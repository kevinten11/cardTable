package game;
import java.awt.Point;
import java.io.Serializable;

public class Card implements Serializable
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3854417171042829951L;
	public int power;
	public String suit;
	public boolean visible;
	
	// the location on the table
	public Point location;
	
	public Card(String suit, int num)
	{
		power = num;
		this.suit = suit;
		visible = false;
	}
	
	public Card(String suit, String num)
	{
		this(suit, Integer.parseInt(num));
	}
	
	public Card(String suit, int num, boolean vis, Point location)
	{
		this(suit, num);
		this.visible = vis;
		this.location = location;
	}
	
	public Card(Card c)
	{
		this(c.suit, c.power, c.visible, c.location);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + power;
		result = prime * result + ((suit == null) ? 0 : suit.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Card other = (Card) obj;
		if (power != other.power)
			return false;
		if (suit == null) {
			if (other.suit != null)
				return false;
		} else if (!suit.equals(other.suit))
			return false;
		return true;
	}
	
	public boolean equals(String s)
	{
		// assume string is "{suit} {power}"
		String[] info = s.split(" ");
		if (info.length != 2)
		{
			return false;
		}
		return suit.equals(info[0]) && Integer.parseInt(info[1]) == power;
	}

    /**
     * Takes in a cards and compares with this
     * @param c2
     * @return true true if c2 comes after this
     */
    public boolean isBefore(Card c2)
    {
    	return SuitRankings.isBefore( this, c2 );
    }
    
    public String toString()
    {
    	return suit + " " + power + location.toString();
    }
}
