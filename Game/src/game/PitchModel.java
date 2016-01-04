package game;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import networking.CardRequest;
import utilities.ConcurrentArrayList;

public class PitchModel {

	Pile deck;
	Pile discardPile;
	ConcurrentArrayList<Card> tableCards;
	ConcurrentArrayList<Player> players;
	ConcurrentArrayList<Pile> freePiles;
	
	public PitchModel()
	{
		deck = new Pile(false);
		discardPile = new Pile(true);
		players = new ConcurrentArrayList<Player>();
		tableCards = new ConcurrentArrayList<Card>();
		freePiles = new ConcurrentArrayList<Pile>();
		try {
			addCardsToDeck();
			deck.shuffle();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void addPlayer(ObjectInputStream in, ObjectOutputStream out) throws IOException
	{
		int numPlayers = players.size();
		players.add(new Player(numPlayers, in, out));	
		players.get(numPlayers).start();
		sendHandCounts();
	}
	
	/**
	 * Moves all cards to deck and shuffles
	 * @throws IOException 
	 */
	public void resetState() throws IOException
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
	 * Takes all cards from a list and puts them in deck
	 * @param from    list to drain
	 */
	public void drainToDeck(Pile from)
	{
		while (from.size() > 0)
		{
			Card last = from.pop();
			last.visible = false;
			deck.add(last);
		}
	}
	
	/**
	 * Sends out the counts of all players' hands
	 * @throws IOException 
	 */
	public void sendHandCounts() throws IOException
	{
		ArrayList<Integer> counts = new ArrayList<Integer>();
		
		// build counts string
		for (int i = 0; i < players.size(); i++)
		{
			counts.add(players.get(i).hand.size());
		}
		
		// send out counts to all players
		for (int i = 0; i < players.size(); i++)
		{
			players.get(i).output.writeObject(counts);
		}
	}
	
	/**
	 * Sends out a command to all players
	 * @param s String of the command to send
	 * @throws IOException 
	 */
	public void sendOutCommand(Object s) throws IOException
	{
		for (int i = 0; i < players.size(); i++)
		{
			players.get(i).output.writeObject(s);
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
			}
		}
		Card redJ = new Card("red", -1);
		deck.add(redJ);
		Card blackJ = new Card("black", -1);
		deck.add(blackJ);
	}
	
	/**
	 * Thread of a player
	 * @author Kevin
	 *
	 */
	class Player extends Thread 
	{
		int number;
		ObjectInputStream input;
		ObjectOutputStream output;
		ConcurrentArrayList<Card> hand;
		
		/**
		 * Creates a player based on the number and socket
		 * @param socket
		 * @param number
		 * @throws IOException 
		 */
		public Player(int number, ObjectInputStream in, ObjectOutputStream out) throws IOException
		{
			this.number = number;
			hand = new ConcurrentArrayList<Card>();
			output = out;
			input = in;
			output.writeObject(number);
			
			// send out table cards
			output.writeObject(tableCards.listCopy());
			
			
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
					Object request = input.readObject();
					if (request == null)
					{
						// do stuff?
					}
					else if (request instanceof CardRequest)
					{
						CardRequest cReq = (CardRequest) request;
        				Card card = cReq.card;
        				switch(cReq.type)
        				{
							case DISCARD:
								// if in hand remove it and send to that player, send out counts
								if (hand.contains(card))
								{
									hand.remove(card);
									discardPile.add(card);
									output.writeObject(request);
									sendHandCounts();
								}
								// if card is on table, send out to all players
								else if (tableCards.contains(card))
								{
									tableCards.remove(card);
									discardPile.add(card);
									sendOutCommand(request);
								}
								break;
								
							case DRAW:
								if (deck.size() > 0)
								{
									Card drew = deck.pop();
									
									if (drew != null)
									{						
										cReq.card = drew;
										drew.visible = false;
										hand.add(drew);
										output.writeObject(cReq);
										sendHandCounts();
									}
								}
								break;
								
							case FLIP:
								// check hand				
								if (hand.contains(card))
								{
									// if found in hand send the command to just the player
									hand.tryGet(card).visible = card.visible;
									output.writeObject(cReq);
								}
								else if (tableCards.contains(card))
								{
									// if on the field send the command to everyone
									tableCards.tryGet(card).visible = card.visible;
									sendOutCommand(cReq);			
								}
								break;
								
							case MOVE:
								Card foundCard = tableCards.tryGet(card);
								if (foundCard != null)
								{
									foundCard.location = card.location;
									sendOutCommand(cReq);
								}
								break;
								
							case PICKUP:
								// if found, process and send out request
								if (tableCards.contains(card))
								{
									tableCards.remove(card);
									hand.add(card);								
									sendOutCommand(request);
									sendHandCounts();	
								}
								break;
								
							case PLACE:
								// if valid card, then place it
								if (hand.contains(card))
								{
									hand.remove(card);
									tableCards.add(card);
									sendOutCommand(cReq);
									sendHandCounts();
								}
								break;
								
							default:
								break;		        				
        				}
					}
					else if (request instanceof String)
					{
						String res = (String) request;
        				switch (res)
        				{
        					case "RESET":
        						resetState();
        						break;
        					
        					case "CLEAR TABLE":
        						clearTable();
        						sendOutCommand(request);
        						break;
        						
        					default:
        						break;
        				}
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
