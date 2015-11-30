import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class PitchModel {

	ConcurrentArrayList<Card> deck;
	static ConcurrentArrayList<Card> discardPile;
	ConcurrentHashMap<Card, Point> tableCards;
	ConcurrentArrayList<Player> players;
	HashMap<String, Card> stringToRef;
	
	
	public PitchModel()
	{
		deck = new ConcurrentArrayList<Card>();
		discardPile = new ConcurrentArrayList<Card>();
		players = new ConcurrentArrayList<Player>();
		tableCards = new ConcurrentHashMap<Card, Point>();
		stringToRef = new HashMap<String, Card>();
		try {
			addCardsToDeck();
			deck.shuffle();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void addPlayer(Socket socket, int number)
	{
		players.add(new Player(socket, number));
		sendHandCounts();
	}
	
	public static void discard(ArrayList<Card> from, int index)
	{
		discardPile.add(from.remove(index));
	}
	
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
	
	public void sendOutCommand(String s)
	{
		for (int i = 0; i < players.size(); i++)
		{
			players.get(i).output.println(s);
		}
	}
	
	public void addCardsToDeck() throws InterruptedException
	{
		String[] suits = new String[]{"clubs", "diamonds", "hearts", "spades"};
		for (String s : suits)
		{
			for (int i = 2; i < 15; i++)
			{
				Card card = new Card(i, s);
				deck.add(card);
				stringToRef.put(s + " " + i, card);
			}
		}
		Card redJ = new Card(-1, "red");
		deck.add(redJ);
		stringToRef.put("red -1", redJ);
		Card blackJ = new Card(-1, "black");
		deck.add(blackJ);
		stringToRef.put("black -1", blackJ);
	}
	
	class Player extends Thread 
	{
		int number;
		Socket socket;
		BufferedReader input;
		PrintWriter output;
		ConcurrentArrayList<Card> hand;
		
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
			Enumeration<Card> cardEnum = tableCards.keys();
			while (cardEnum.hasMoreElements())
			{
				Card card = cardEnum.nextElement();
				Point point = tableCards.get(card);
				tableString += " " + card.suit + " " + card.power + " " + point.x + " " + point.y + " " + (card.visible ? "1" : "0");
			}
			output.println(tableString);
			
			
			System.out.println("Hello to Player " + number);
		}
		
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
						// info is suit, power, x, y, visible
						String[] info = request.substring(6).split(" ");
						Card card = stringToRef.get(info[0] + " " + info[1]);
						
						// if valid card, then place it
						if (hand.contains(card))
						{
							hand.remove(card);
							Point cardPoint = new Point(Integer.parseInt(info[2]), Integer.parseInt(info[3]));
							tableCards.put(card, cardPoint);
							sendOutCommand(request + " " + number + " " +  (card.visible ? "1" : "0"));
							sendHandCounts();
						}
								
					}
					else if (request.startsWith("MOVE "))
					{
						// info is suit, power, x, y
						String[] info = request.substring(5).split(" ");
						Card card = stringToRef.get(info[0] + " " + info[1]);
						
						// if card is a valid table card, move it
						if (tableCards.containsKey(card))
						{
							Point cardPoint = new Point(Integer.parseInt(info[2]), Integer.parseInt(info[3]));
							Point oldPoint = tableCards.get(card);
							tableCards.replace(card, oldPoint, cardPoint);
							sendOutCommand(request + " " + number);
						}
					}
					else if (request.startsWith("FLIP"))
					{
						// info is suit power
						String cardString = request.substring(5);
						Card card = stringToRef.get(cardString);						
						
						// check hand				
						if (hand.contains(card))
						{
							// if found in hand send the command to just the player
							card.visible = !card.visible;
							output.println(request);
						}
						else if (tableCards.containsKey(card))
						{
							// if on the field send the command to everyone
							card.visible = !card.visible;
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
						if (tableCards.containsKey(card))
						{
							tableCards.remove(card);
							hand.add(card);								
							sendOutCommand(request + " " + number);
							sendHandCounts();	
						}
					}
					sleep(10);
				}
			}
			catch (Exception e)
			{
				players.remove(this);
				System.out.println("Player lost: " + e);
			}
		}
	}
}
