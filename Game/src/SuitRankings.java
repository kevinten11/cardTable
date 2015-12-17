import java.util.HashMap;

public class SuitRankings {
	private static final HashMap<String, Integer> suitRankings;
	static {
		suitRankings = new HashMap<>();
		suitRankings.put("clubs", 1);
    	suitRankings.put("diamonds", 2);
    	suitRankings.put("spades", 3);
    	suitRankings.put("hearts", 4);
    	
    	// red and black for jokers
    	suitRankings.put("red", 5);
    	suitRankings.put("black", 6);
	}
	
	public static boolean isBefore( Card c1, Card c2 ) {
		int c1r = suitRankings.get( c1.suit );
		int c2r = suitRankings.get( c2.suit );
		return ( c1r < c2r ? true : (c1r > c2r ? false : ( c1.power < c2.power ? true : false)));
	}
} 
