package networking;

import java.io.Serializable;
import game.Pile;

public class PileRequest implements Serializable{
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 238847426706585912L;
	
	public enum Type implements Serializable
	{
		DRAW("DrawFrom"),
		MOVE("Move"),
		FLIP("Flip"),
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
	public Pile pile;
	public int playerNum;
	
	public PileRequest(int playerNum, Type type)
	{
		this.type = type;
		this.playerNum = playerNum;
	}
	
	public PileRequest(Pile p, int playerNum, Type type)
	{
		this(playerNum, type);
		pile = p;
	}
	
	public String toString()
	{
		return "Card: " + pile.toString() + " Player: " + playerNum + " Type: " + type.value;
	}
}
