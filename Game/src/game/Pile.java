package game;

import java.awt.Point;
import java.util.ArrayList;

import utilities.ConcurrentArrayList;

/**
 * Pile of cards.  If face up, end of arrayList is bottom card
 * @author Kevin
 *
 */
public class Pile {

	Boolean faceUp;
	ConcurrentArrayList<Card> cards;
	Point location;
	
	public Pile(Boolean up)
	{
		faceUp = up;
		cards = new ConcurrentArrayList<Card>();
		location = new Point(0, 0);
	}
	
	/**
	 * Adds a card to the top of the pile
	 * @param c card to add
	 * @return Number of cards now in pile
	 */
	public int add(Card c)
	{
		if (faceUp)
		{
			cards.add(0, c);
		}
		else
		{
			cards.add(c);
		}
		return cards.size();
	}
	
	/**
	 * Takes the top card of the pile
	 * @return The card removed from the pile
	 */
	public Card pop()
	{
		int index;
		if (faceUp)
		{
			index = 0;
		}
		else
		{
			index = cards.size() - 1;
		}
		Card c = cards.get(index);
		cards.remove(c);
		return c;
	}
	
	/**
	 * Gets the top of the pile
	 * @return top of the pile
	 */
	public Card peek()
	{
		if (faceUp)
		{
			return cards.get(0);
		}
		return cards.get(cards.size() - 1);
	}
	
	/**
	 * Shuffles the Pile
	 */
	public void shuffle()
	{
		for (int i = 0; i < cards.size(); i++)
		{
			cards.get(i).visible = false;
		}
		cards.shuffle();
	}
	
	public int size()
	{
		return cards.size();
	}
	
	public void flip(Boolean up)
	{
		faceUp = up;
	}

	/**
	 * Clears all cards and gives back as a list
	 * @return List of all cards
	 */
	public ArrayList<Card> takeAll()
	{
		ArrayList<Card> list = cards.listCopy();
		cards.clear();
		return list;
	}
}
