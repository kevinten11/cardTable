import java.util.HashMap;

public class Card {
	
	public int power;
	public String suit;
	public boolean visible;
	
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
	
	public Card(String suit, String num, String vis)
	{
		this(suit, num);
		visible = (vis.equals("0") ? false : true);
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
    	HashMap<String, Integer> suitRanking = new HashMap<String, Integer>();
    	suitRanking.put("clubs", 1);
    	suitRanking.put("diamonds", 2);
    	suitRanking.put("spades", 3);
    	suitRanking.put("hearts", 4);
    	
    	// red and black for jokers
    	suitRanking.put("red", 5);
    	suitRanking.put("black", 6);
    	
    	if (suitRanking.get(suit) < suitRanking.get(c2.suit))
    	{
    		return true;
    	}
    	else if (suitRanking.get(suit) > suitRanking.get(c2.suit))
    	{
    		return false;
    	}
    	else // tied suit
    	{
    		if (power < c2.power)
    		{
    			return true; 
    		}
    		return false;
    	}  	
    }
}
