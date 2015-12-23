package networking;
import java.io.Serializable;

import game.Card;

public class CardRequest implements Serializable{
	

	/**
	 * 
	 */
	private static final long serialVersionUID = -6231002210435639987L;

	public enum Type implements Serializable
	{
		DRAW("Draw"),
		MOVE("Move"),
		FLIP("Flip"),
		PLACE("Place"),
		PICKUP("Pickup"),
		DISCARD("Discard");	
		
		
		private String value;
		private Type(String val)
		{
			value = val;
		}
		
		public String getValue()
		{
			return value;
		}
		
	}
	
	public Type type;
	public Card card;
	public int playerNum;
	
	public CardRequest(int playerNum, Type type)
	{
		this.type = type;
		this.playerNum = playerNum;
	}
	
	public CardRequest(Card c, int playerNum, Type type)
	{
		this(playerNum, type);
		card = c;
	}
	
	public String toString()
	{
		return "Card: " + card.toString() + " Player: " + playerNum + " Type: " + type.value;
	}
}
