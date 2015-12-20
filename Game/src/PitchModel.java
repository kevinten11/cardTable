import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

public class PitchModel {

	ConcurrentArrayList<Card> deck;
	ConcurrentArrayList<Card> discardPile;
	ConcurrentArrayList<Card> tableCards;
	ConcurrentArrayList<Player> players;
	HashMap<String, Card> stringToRef;
	
	
	public PitchModel()
	{
		deck = new ConcurrentArrayList<Card>();
		discardPile = new ConcurrentArrayList<Card>();
		players = new ConcurrentArrayList<Player>();
		tableCards = new ConcurrentArrayList<Card>();
		stringToRef = new HashMap<String, Card>();
		try {
			addCardsToDeck();
			deck.shuffle();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void addPlayer(Socket socket)
	{
		int numPlayers = players.size();
		players.add(new Player(socket, numPlayers));	
		players.get(numPlayers).start();
		sendHandCounts();
	}
	
	/**
	 * Moves all cards to deck and shuffles
	 */
	public void resetState()
	{
		// move all table cards to discard pile
		clearTable();
		
		// move discard to deck
		drainToDeck(discardPile);
		
		// move hands to deck
		for (int i = 0; i < players.size(); i++)
		{
			drainToDeck(players.get(i).hand);
		}	
		deck.shuffle();
		sendOutCommand("RESET");
	}
	
	/**
	 * Clears out table cards and moves them to the discard pile
	 */
	public void clearTable()
	{
		// go through all cards on table and put them in the discardPile
		for (int i = 0; i < tableCards.size(); i++)
		{
			discardPile.add(tableCards.get(i));	
		}
		
		// clear the table list
		tableCards.clear();
	}
	
	/**
	 * Takes all cards from a list and puts them in deck
	 * @param from    list to drain
	 */
	public void drainToDeck(ConcurrentArrayList<Card> from)
	{
		while (from.size() > 0)
		{
			Card last = from.get(from.size() - 1);
			from.remove(last);
			last.visible = false;
			deck.add(last);
		}
	}
	
	/**
	 * Sends out the counts of all players' hands
	 */
	public void sendHandCounts()
	{
		String countString = "HAND COUNTS: ";
		
		// build counts string
		for (int i = 0; i < players.size(); i++)
		{
			countString += players.get(i).hand.size() + " ";
		}
		
		// send out counts to all players
		for (int i = 0; i < players.size(); i++)
		{
			players.get(i).output.println(countString);
		}
	}
	
	/**
	 * Sends out a command to all players
	 * @param s String of the command to send
	 */
	public void sendOutCommand(String s)
	{
		for (int i = 0; i < players.size(); i++)
		{
			players.get(i).output.println(s);
		}
	}
	
	/**
	 * Initializes the deck with a 54 cards
	 * @throws InterruptedException
	 */
	public void addCardsToDeck() throws InterruptedException
	{
		String[] suits = new String[]{"clubs", "diamonds", "hearts", "spades"};
		for (String s : suits)
		{
			for (int i = 2; i < 15; i++)
			{
				Card card = new Card(s, i);
				deck.add(card);
				stringToRef.put(s + " " + i, card);
			}
		}
		Card redJ = new Card("red", -1);
		deck.add(redJ);
		stringToRef.put("red -1", redJ);
		Card blackJ = new Card("black", -1);
		deck.add(blackJ);
		stringToRef.put("black -1", blackJ);
	}
	
	/**
	 * Thread of a player
	 * @author Kevin
	 *
	 */
	class Player extends Thread 
	{
		int number;
		Socket socket;
		BufferedReader input;
		PrintWriter output;
		ConcurrentArrayList<Card> hand;
		
		/**
		 * Creates a player based on the number and socket
		 * @param socket
		 * @param number
		 */
		public Player(Socket socket, int number)
		{
			this.socket = socket;
			this.number = number;
			hand = new ConcurrentArrayList<Card>();
			try 
			{
				input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				output = new PrintWriter(socket.getOutputStream(), true);
			}
			catch (IOException e)
			{
				System.out.println("Player lost: " + e);
			}
			output.println("PLAYER " + number);
			
			// send out table state
			String tableString = "TABLE:";
			for (int i = 0; i < tableCards.size(); i++)
			{
				Card card = tableCards.get(i);
				Point point = card.location;
				tableString += " " + card.suit + " " + card.power + " " + point.x + " " + point.y + " " + (card.visible ? "1" : "0");
			}
			output.println(tableString);
			
			
			System.out.println("Hello to Player " + number);
		}
		
		/**
		 * Main run loop reading in requests from the client
		 */
		public void run()
		{
			try
			{
				while (true)
				{					
					// get client commands
					String request = input.readLine();
					if (request == null)
					{
						// do stuff?
					}
					else if (request.startsWith("DRAW FROM DECK") && deck.size() > 0)
					{
						Card drew = deck.get(0);
						deck.remove(drew);
						
						if (drew != null)
						{							
							hand.add(drew);
							output.println("DREW " + drew.suit + " " + drew.power);
							sendHandCounts();
						}
					}
					else if (request.startsWith("PLACE "))
					{
						// info is suit, power, x, y, vis
						String[] info = request.substring(6).split(" ");
						Card card = stringToRef.get(info[0] + " " + info[1]);
						card.visible = info[4].equals("1");
						// if valid card, then place it
						if (hand.contains(card))
						{
							hand.remove(card);
							Point cardPoint = new Point(Integer.parseInt(info[2]), Integer.parseInt(info[3]));
							card.location = cardPoint;
							tableCards.add(card);
							sendOutCommand(request + " " + number);
							sendHandCounts();
						}
								
					}
					else if (request.startsWith("MOVE "))
					{
						// info is suit, power, x, y
						String[] info = request.substring(5).split(" ");
						Card card = stringToRef.get(info[0] + " " + info[1]);
						
						// if card is a valid table card, move it
						Card foundCard = tableCards.tryGet(card);
						if (foundCard != null)
						{
							Point cardPoint = new Point(Integer.parseInt(info[2]), Integer.parseInt(info[3]));
							foundCard.location = cardPoint;
							sendOutCommand(request + " " + number);
						}
					}
					else if (request.startsWith("FLIP"))
					{
						// info is suit power vis
						String[] info = request.substring(5).split(" ");
						Card card = stringToRef.get(info[0] + " " + info[1]);		
						boolean vis = info[2].equals("1");
						
						// check hand				
						if (hand.contains(card))
						{
							// if found in hand send the command to just the player
							card.visible = vis;
							output.println(request);
						}
						else if (tableCards.contains(card))
						{
							// if on the field send the command to everyone
							card.visible = vis;
							sendOutCommand(request);			
						}
					}
					else if (request.startsWith("PICKUP"))
					{
						// info is suit, power
						String[] info = request.substring(7).split(" ");
						String cardString = info[0] + " " + info[1];
						Card card = stringToRef.get(cardString);
						
						// if found, process and send out request
						if (tableCards.contains(card))
						{
							tableCards.remove(card);
							hand.add(card);								
							sendOutCommand(request + " " + (card.visible ? "1" : "0") + " " + number);
							sendHandCounts();	
						}
					}
					else if (request.startsWith("DISCARD"))
					{
						String[] info = request.substring(8).split(" ");
						String cardString = info[0] + " " + info[1];
						Card card = stringToRef.get(cardString);
						
						// if in hand remove it and send to that player, send out counts
						if (hand.contains(card))
						{
							hand.remove(card);
							discardPile.add(card);
							output.println(request);
							sendHandCounts();
						}
						// if card is on table, send out to all players
						else if (tableCards.contains(card))
						{
							tableCards.remove(card);
							discardPile.add(card);
							sendOutCommand(request);
						}
					}
					else if (request.startsWith("RESET"))
					{
						resetState();
					}
					else if (request.startsWith("CLEAR TABLE"))
					{
						clearTable();
						sendOutCommand(request);
					}
					sleep(10);
				}
			}
			catch (Exception e)
			{
				players.remove(this);
				e.printStackTrace();
				System.out.println("Player dropped: " + e);
			}
		}
	}
}
