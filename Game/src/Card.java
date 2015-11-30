public class Card {
	
	public int power;
	public String suit;
	public boolean visible;
	
	public Card(int num, String suit)
	{
		power = num;
		this.suit = suit;
		visible = false;
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
}
