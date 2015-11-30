import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane; 

/** 
 * Main class for the game 
 */ 
@SuppressWarnings("serial")
public class PitchClient extends JFrame implements KeyListener, MouseListener
{        
		public class Bounds
		{
			int topLeftX;
			int topLeftY;
			int bottomRightX;
			int bottomRightY;
			int width;
			int height;
			
			public Bounds(int x1, int y1, int x2, int y2)
			{
				topLeftX = x1;
				topLeftY = y1;
				bottomRightX = x2;
				bottomRightY = y2;
				width = Math.abs(x2-x1);
				height = Math.abs(y2-y1);
			}
			
			public boolean contains(int x, int y)
			{
				if (x > topLeftX && x < bottomRightX && y > topLeftY && y < bottomRightY)
				{
					return true;
				}
				return false;
			}
			
			public boolean contains(Point p)
			{
				return contains(p.x, p.y);
			}
		}

		boolean isRunning = true; 
        int cardWidth = 500/5;
        //int cardHeight = 726/5;
        int cardHeight = 500/5;
        int fps = 30; 
        int windowWidth = 1200; 
        int windowHeight = 700;
        static int PORT = 8901;
        Socket socket;
        BufferedReader in;
        PrintWriter out;
        int playerNum;
        Hashtable<String, Image> cardImages;
        
        Hashtable<String, Bounds> cardClickBoxes = new Hashtable<String, Bounds>();
          
        BufferedImage backBuffer; 
        Insets insets; 
        Image background;
        Image cardBack; 
        String selectedCard = "";
        
        ArrayList<String> hand = new ArrayList<String>();
        ArrayList<String> tableCards = new ArrayList<String>();
        HashSet<String> visibleCards = new HashSet<String>();
        String[] handCounts;
        
        // sets up connection
        public PitchClient(String serverAddress)
        {
        	try {
				socket = new Socket(serverAddress, PORT);
				JOptionPane.showMessageDialog(this, "Connection Success");
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	        	out = new PrintWriter(socket.getOutputStream(), true);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				JOptionPane.showMessageDialog(this, "Network Connection Error: " + e.getMessage());
			}
        	
        	initializeUI();
        	readInCardImages();
        	this.addKeyListener(this);
        	this.addMouseListener(this);
        }
        
        public static void main(String[] args) throws Exception
        { 
    		String serverAddress = (args.length == 0) ? "localhost" : args[1];
    		serverAddress = JOptionPane.showInputDialog("Enter IP or LAN Name");
    		
            PitchClient game = new PitchClient(serverAddress);
            game.run();
            System.exit(0); 
        } 
        
        /** 
         * Sets up GUI
         */ 
        void initializeUI() 
        { 
            setTitle("Pitch"); 
            setSize(windowWidth, windowHeight); 
            setResizable(false); 
            setDefaultCloseOperation(EXIT_ON_CLOSE); 
            setVisible(true); 
            
            insets = getInsets(); 
            setSize(insets.left + windowWidth + insets.right, 
                            insets.top + windowHeight + insets.bottom); 
            
            backBuffer = new BufferedImage(windowWidth, windowHeight, BufferedImage.TYPE_INT_ARGB); 
            try {
            	// read in background
    		    background = ImageIO.read(new File("Resources\\Background-Green.jpg"));
    		    background = background.getScaledInstance(windowWidth, windowHeight, Image.SCALE_SMOOTH);
    		    
    		    // read in card back
    		    cardBack = ImageIO.read(new File("Resources\\playing-card-back.jpg"));
    		    cardBack = cardBack.getScaledInstance(cardWidth, cardHeight, Image.SCALE_SMOOTH);
    		    
    		} catch (IOException e) 
    		{
    			System.out.println("ErrorOnReadInResources");
    			JOptionPane.showMessageDialog(this, "ErrorOnReadInResources");
    			System.exit(1);
    		}
        } 

        
        /** 
         * This method will check for input, move things 
         * around and check for win conditions, etc 
         */ 
        void run() throws Exception
        { 
        	updateGraphics();
        	String response;
        	try 
        	{
        		response = in.readLine();
        		if (response.startsWith("PLAYER"))
        		{
        			playerNum = response.charAt(7) - '0';
        			setTitle("Pitch - Player " + playerNum);
        		}
        		
        		response = in.readLine();
        		if (response.startsWith("TABLE:") && response.length() > 6)
        		{
        			System.out.println(response);
        			// pattern in info is suit, power, x, y, vis
        			String[] info = response.substring(7).split(" ");
        			System.out.println(info.length + " " + info[0]);
        			for (int i = 0; i + 4 < info.length; i += 5)
        			{
        				String card = info[i] + " " + info[i+1];
        				int x = Integer.parseInt(info[i+2]);
        				int y = Integer.parseInt(info[i+3]);
        				y = windowHeight - y - cardHeight;
        				x = windowWidth/2 + (windowWidth/2 - x) - cardWidth;
        				Bounds bounds = new Bounds(x, y, x + cardWidth, y + cardHeight);
        				tableCards.add(card);
        				cardClickBoxes.put(card, bounds);  	
        				if (info[i+4].equals("1"))
        				{
        					visibleCards.add(card);
        				}
        			}
        		}	
        		while (true) 
        		{        			     			
        			// if no input, sleep for a bit and re-draw
        			if (!in.ready())
        			{
        				updateGraphics();
        				Thread.sleep(50);
        				continue;
        			}
        			
        			// check for input from server
        			response = in.readLine();
        			
        			// if null, sleep for a bit
        			if (response == null)
        			{
        				Thread.sleep(100);
        				continue;
        			}
        			
        			System.out.println(response);
        			// check cases for input
        			if (response.startsWith("DREW"))
        			{
        				hand.add(response.substring(5));
        			}
        			else if (response.startsWith("HAND COUNTS: " ))
        			{
        				handCounts = response.substring(13).split(" ");
        			}
        			else if (response.startsWith("PLACE"))
        			{	
        				// info is suit, power, x, y, player number, visible
        				String[] info = response.substring(6).split(" ");
        				String card = info[0] + " " + info[1];
        				
        				if (info[5].equals("1"))
        				{
        					visibleCards.add(card);
        				}
        				
        				if (Integer.parseInt(info[4]) == playerNum)
        				{
        					hand.remove(card);
        				}
        				
        				tableCards.add(card);
        				
        				// logic for rotating placed cards)	
        				int x = Integer.parseInt(info[2]);
        				int y = Integer.parseInt(info[3]);
        				
        				if (Integer.parseInt(info[4]) != playerNum)
        				{
        					y = windowHeight - y - cardHeight;
        					x = windowWidth/2 + (windowWidth/2 - x) - cardWidth;
        				}
        				
        				Bounds bound = new Bounds(x, y, x + cardWidth, y + cardHeight);
        				cardClickBoxes.put(card, bound);
        			}
        			else if (response.startsWith("MOVE"))
        			{
        				// info is suit, power, x, y, player number
        				String[] info = response.substring(5).split(" ");
        				String card = info[0] + " " + info[1];
        				
        				// logic for rotating placed cards based on player number
        				int x = Integer.parseInt(info[2]);
        				int y = Integer.parseInt(info[3]);
        				
        				if (Integer.parseInt(info[4]) != playerNum)
        				{
        					y = windowHeight - y - cardHeight;
        					x = windowWidth/2 + (windowWidth/2 - x) - cardWidth;
        				}
        				
        				Bounds bound = new Bounds(x, y, x + cardWidth, y + cardHeight);
        				cardClickBoxes.replace(card, bound);
        			}
        			else if (response.startsWith("FLIP"))
        			{
        				String card = response.substring(5);
        				// ensure card is in play
        				if (hand.contains(card) || tableCards.contains(card))
        				{
        					if (visibleCards.contains(card))
        					{
        						visibleCards.remove(card);
        					}
        					else
        					{
        						visibleCards.add(card);
        					}
        				}
        			}
        			else if (response.startsWith("PICKUP"))
        			{
        				// info is suit, power, player who picked up
        				String info[] = response.substring(7).split(" ");
        				String card = info[0] + " " + info[1];
        				int playerWhoPickedUp = Integer.parseInt(info[2]);
        				
        				if (playerWhoPickedUp == playerNum)
        				{
        					hand.add(card);
        				}
        				tableCards.remove(card);
        			}
        		}    		
        	}
        	finally
        	{
        		socket.close();
        	}
        } 
        
        /** 
         * This method will draw everything 
         */ 
        public void updateGraphics() 
        {               
            Graphics g = getGraphics(); 
            
            Graphics2D bbg = (Graphics2D) backBuffer.getGraphics(); 
            
            bbg.drawImage(background, 0, 0, null); 
            
            // draw hand cards
            if (!hand.isEmpty())
            {
            	int totalW = (cardWidth * (hand.size()+1))/2;
            	int firstX = windowWidth/2 - totalW/2;
                for (int i = 0; i < hand.size(); i++)
                {
                	// draw the cards in hand
                	boolean overlapped = (i != hand.size()-1);
                	int imgY = windowHeight - cardHeight; 
                	int imgX = firstX + cardWidth*i/2;
                	String card = hand.get(i);
                	
                	
                	if (visibleCards.contains(card))
                	{
	                	bbg.drawImage(cardImages.get(card), imgX, imgY, null);	                		
                	}
                	else
                	{
	                	bbg.drawImage(cardBack, imgX, imgY, null);
                	}

                	
                	// update click boxes of hand cards
                	int offset = overlapped ? cardWidth/2 : cardWidth;
                	if (cardClickBoxes.containsKey(card))
                	{
                		cardClickBoxes.replace(card, new Bounds(imgX, imgY, imgX + offset, windowHeight));
                	}
                	else
                	{ 
                		cardClickBoxes.put(card, new Bounds(imgX, imgY, imgX + offset, windowHeight));
                	}
                }
            }
                
            // paint table cards
            for (int i = 0; i < tableCards.size(); i++)
            {
            	String card = tableCards.get(i);
            	Bounds cardB = cardClickBoxes.get(card);
            	if (visibleCards.contains(card))
            	{
            		bbg.drawImage(cardImages.get(card), cardB.topLeftX, cardB.topLeftY, null); 
            	}
            	else
            	{
            		bbg.drawImage(cardBack, cardB.topLeftX, cardB.topLeftY, null); 
            	}
            	            	
            }
            
            // draw opponents cards
            if (handCounts != null && handCounts.length > 1)
            {
            	int oppHandCount = Integer.parseInt(handCounts[((playerNum + 1) %2)]);
            	int totalW = (cardWidth * (oppHandCount+1))/2;
            	int firstX = windowWidth/2 - totalW/2;
            	for (int i = 0; i < oppHandCount; i++)
            	{
            		bbg.drawImage(cardBack, firstX + cardWidth*i/2, 0, null);
            	}
            }
            
            // paint box around selected card
            if (!selectedCard.isEmpty())
            {
                Bounds box = cardClickBoxes.get(selectedCard);              
                bbg.setColor(Color.BLACK);
                bbg.setStroke(new BasicStroke(2));
                bbg.drawRect(box.topLeftX, box.topLeftY, box.width, box.height);
            }
                     
            // paint ghost card
            if (!selectedCard.isEmpty())
            {
                float opacity = 0.5f;
                bbg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));	                
               
                Point ghostCard = getGhostCardPoint();
                if (visibleCards.contains(selectedCard))
            	{
                	bbg.drawImage(cardImages.get(selectedCard), ghostCard.x, ghostCard.y, null);                		
            	}
            	else
            	{
                	bbg.drawImage(cardBack, ghostCard.x, ghostCard.y, null);
            	}
                
                // if the player would pick up the card, draw border around hand
                if (ghostCard.y > windowHeight - cardWidth)
                {
                	bbg.setColor(Color.YELLOW);
 	                bbg.setStroke(new BasicStroke(2));
 	                int handWidth = (hand.size() + 1)*cardWidth/2;
 	                int handX = windowWidth/2 - handWidth/2;
 	                bbg.drawRect(handX, windowHeight - cardHeight, handWidth, cardHeight);
                }
            }
            
            g.drawImage(backBuffer, insets.left, insets.top, this); 
        } 

        
        public void readInCardImages()
        {
        	cardImages = new Hashtable<String, Image>();
        	File resources = new File("Resources\\Cards");
    		for (File f : resources.listFiles())
        	{
        		Image image = null;
        		int power = 0;
        		String suit = "";
        		try {
        		    image = ImageIO.read(f);
        		    image = image.getScaledInstance(cardWidth, cardHeight, Image.SCALE_SMOOTH);
        		} catch (IOException e) 
        		{
        			System.out.println("ErrorOnReadInCards");
        			JOptionPane.showMessageDialog(this, "ErrorOnReadInCards");
        			System.exit(1);
        		}
        		String fileName = f.getName();
        		String[] splitName = fileName.split("_");
        		if (splitName.length == 2)
        		{
        			power = -1;
        			suit = splitName[0];
        		}
        		else
        		{
        			suit = splitName[2];
        			suit = suit.substring(0, suit.lastIndexOf('.'));
        			char first = fileName.charAt(0);
        			if (first >= '0' && first <= '9')
        			{
        				power = Integer.parseInt(splitName[0]);   
        			}
        			else if (splitName[0].startsWith("ace"))
        			{
        				power = 14;
        			}
        			else if (splitName[0].startsWith("king"))
        			{
        				power = 13;
        			}
        			else if (splitName[0].startsWith("queen"))
        			{
        				power = 12;
        			}
        			else if (splitName[0].startsWith("jack"))
        			{
        				power = 11;
        			}
        		}
        		
        		cardImages.put(suit + " " + power, image);
        	}
        }

		public Point getGhostCardPoint()
		{
			Point mouse = MouseInfo.getPointerInfo().getLocation();
            Point frame = getLocationOnScreen();
            int x = mouse.x - frame.x - cardWidth/2;
            int y = mouse.y - frame.y - cardHeight/2;
            
            // create boundaries for the card so it won't overlap with opponent's hand
            if (y < cardHeight)
            {
            	y = cardHeight;
            }
            return new Point(x, y);
		}
        
        @Override
		public void keyPressed(KeyEvent e) {
			// TODO Auto-generated method stub
        	
			
		}

		@Override
		public void keyReleased(KeyEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void keyTyped(KeyEvent e) {
			// TODO Auto-generated method stub
			if (e.getKeyChar() == 'd' || e.getKeyChar() == 'D')
			{
				out.println("DRAW FROM DECK");
			}		
		}

		@Override
		public void mouseClicked(MouseEvent e) 
		{
			Point clickPoint = new Point(e.getPoint().x, e.getPoint().y - 26);
			
			// if right click and a card is selected, unselect card
			if (e.getButton() == MouseEvent.BUTTON3 && !selectedCard.isEmpty())
			{
				if (!selectedCard.isEmpty())
				{
					selectedCard = "";
					updateGraphics();
					return;
				}
			}
			// see if a card was clicked
			String clickedCard = "";
			for (String s : cardClickBoxes.keySet())
			{
				if (cardClickBoxes.get(s).contains(clickPoint))
				{
					clickedCard = s;
					break;
				}
			}	
			
			// if no card was selected and right mouse was clicked, flip clicked card
			if (e.getButton() == MouseEvent.BUTTON3 && selectedCard.isEmpty() && !clickedCard.isEmpty())
			{
				out.println("FLIP " + clickedCard);
				System.out.println("Request: FLIP " + clickedCard);
				return;
			}
			
			// if a card was clicked and no card is selected, select the card
			if (!clickedCard.isEmpty() && selectedCard.isEmpty())
			{
				selectedCard = clickedCard;
				if (tableCards.contains(selectedCard))
				{
					// move the selected card to the end of the list
					tableCards.remove(selectedCard);
					tableCards.add(selectedCard);
				}
			}
			// if empty space was clicked and a card was selected
			else if (!selectedCard.isEmpty())
			{
				if (hand.contains(selectedCard))
				{
					// place card and set selected card to empty
					Point playSpot = getGhostCardPoint();
					
					String request;
					if (playSpot.y + cardHeight > windowHeight)
					{
						// do nothing
						selectedCard = "";
						return;
					}
					else
					{
						request = "PLACE " + selectedCard + " " + playSpot.x + " " + playSpot.y;
					}
					
					System.out.println("Request: " + request);
					out.println(request);
					selectedCard = "";
				}
				
				if (tableCards.contains(selectedCard))
				{
					Point playSpot = getGhostCardPoint();
					
					// if card placed in hand, pickup instead of move
					String request;					
					if (playSpot.y + cardHeight > windowHeight)
					{
						request = "PICKUP " + selectedCard;
					}
					else
					{
						request = "MOVE " + selectedCard + " " + playSpot.x + " " + playSpot.y;
					}
					
					System.out.println("Request: " + request);
					out.println(request);
					selectedCard = "";
				}
			}
			
			updateGraphics();
		}

		@Override
		public void mouseEntered(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseExited(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mousePressed(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseReleased(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}
}
