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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane; 

/** 
 * Main class for the game 
 */ 
@SuppressWarnings("serial")
public class PitchClient extends JFrame implements KeyListener, MouseListener
{       
		// card hitbox class
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

		String tableName;
        int cardWidth = 500/7;
        // cardHeight = 726/5; this is the image height
        int cardHeight = 500/7;
        int fps = 30; 
        int windowWidth = 1200; 
        int windowHeight = 700;
        int handThresholdY;
        
        static int PORT = 36636;
        Socket socket;
        BufferedReader in;
        PrintWriter out;
        ObjectInputStream objectIn;
        ObjectOutputStream objectOut;
        
        int playerNum;
        HashMap<Card, Image> cardImages;
        
        HashMap<Card, Bounds> cardClickBoxes = new HashMap<Card, Bounds>();
          
        BufferedImage backBuffer; 
        Insets insets; 
        Image background;
        Image cardBack; 
        Card selectedCard;
        
        ArrayList<Card> hand = new ArrayList<Card>();
        ArrayList<Card> tableCards = new ArrayList<Card>();
        String[] handCounts;
        
        Point initialClick;
        boolean leftMouseDown = false;
        
        /**
         * sets up connection
         */
        public PitchClient()
        {
        	// loop to retry if user messes up the IP address
        	while (true)
        	{
	        	String serverAddress = JOptionPane.showInputDialog("Enter Host IP");
	        	
	        	if (serverAddress == null)
	        	{
	        		System.exit(0);
	        	}
	    		if (serverAddress.isEmpty())
	    		{
	    			serverAddress = "localhost";
	    		}
	    		
	    		// display loading window
	    		JOptionPane pane = new JOptionPane();
        		JDialog loadingDialog = pane.createDialog(this, "Message:");
        		pane.setMessage("Connecting...");
        		loadingDialog.setModal(false);
        		loadingDialog.setVisible(true);
	    		
	        	try 
	        	{       		
	        		// try connection
					socket = new Socket(serverAddress, PORT);
					
					// success, remove loading window and show success
					loadingDialog.dispose();
					tableName = JOptionPane.showInputDialog("Name of table to create or join:");
					
					// string io
					in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		        	out = new PrintWriter(socket.getOutputStream(), true);

		        	// object io
					// objectIn = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
					// objectOut = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
					
		        	out.println(tableName);
		        	String reply = in.readLine();
		        	if (reply.equals("FOUND"))
		        	{
		        		JOptionPane.showMessageDialog(this, "Joined Table: " + tableName);
		        	}
		        	else
		        	{
		        		JOptionPane.showMessageDialog(this, "Table name not found, creating table: " + tableName);
		        	}	        	
		        	break;
				} catch (Exception e) 
	        	{				
					// display error and remove loading window
					loadingDialog.dispose();
					JOptionPane.showMessageDialog(this, "Network Connection Error: " + e.getMessage());
					
					// prompt for retry
					int tryAgain = JOptionPane.showConfirmDialog(this, "Try again?");
					if (tryAgain != JOptionPane.OK_OPTION)
					{
						System.exit(0);
					}
				}
        	}
        	
        	// set up UI and load resources
        	initializeUI();
        	readInCardImages();
        	this.addKeyListener(this);
        	this.addMouseListener(this);
        }
        
        public static void main(String[] args) throws Exception
        { 		
            PitchClient game = new PitchClient();
            game.run();
            System.exit(0); 
        } 
        
        /** 
         * Sets up GUI
         */ 
        void initializeUI() 
        { 
            setTitle("Table: " + tableName); 
            setSize(windowWidth, windowHeight); 
            setResizable(false); 
            setDefaultCloseOperation(EXIT_ON_CLOSE); 
            setVisible(true); 
            
            insets = getInsets(); 
            setSize(insets.left + windowWidth + insets.right, 
                            insets.top + windowHeight + insets.bottom); 
            handThresholdY = windowHeight - cardWidth - getInsets().top;
            
            backBuffer = new BufferedImage(windowWidth, windowHeight, BufferedImage.TYPE_INT_ARGB); 
            try {
            	// read in background
    		    background = ImageIO.read(new File("Resources" + File.separatorChar + "Background-Green.jpg"));
    		    background = background.getScaledInstance(windowWidth, windowHeight, Image.SCALE_SMOOTH);
    		    
    		    // read in card back
    		    cardBack = ImageIO.read(new File("Resources" + File.separatorChar + "playing-card-back.jpg"));
    		    cardBack = cardBack.getScaledInstance(cardWidth, cardHeight, Image.SCALE_SMOOTH);
    		    
    		} catch (IOException e) 
    		{
    			System.out.println("ErrorOnReadInResources");
    			String s = "ErrorOnReadInResources: ";
    			for (StackTraceElement element : e.getStackTrace())
    			{
    				s += element.toString() + "\n";
    			}
    			JOptionPane.showMessageDialog(this, s);
    			System.exit(1);
    		}
        } 
      
        /** 
         * This method will check for input, move things 
         * around and check for win conditions, ect
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
        				Card card = new Card(info[i],  info[i+1], info[i+4]);
        				int x = Integer.parseInt(info[i+2]);
        				int y = Integer.parseInt(info[i+3]);
        				
        				// info is from player 0 perspective so alter it
        				Point transLocation = fitToPerspective(x, y, 0, playerNum);
        				x = transLocation.x;
        				y = transLocation.y;

        				Bounds bounds = new Bounds(x, y, x + cardWidth, y + cardHeight);
        				tableCards.add(card);
        				cardClickBoxes.put(card, bounds);
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
        			else
        			{
        				// check for input from server
            			response = in.readLine();
            			
            			// if null, sleep for a bit
            			if (response == null)
            			{
            				Thread.sleep(50);
            				continue;
            			}
            			
            			System.out.println(response);
            			// check cases for input
            			if (response.startsWith("DREW"))
            			{
            				String[] info = response.substring(5).split(" ");
            				hand.add(new Card(info[0], info[1]));
            			}
            			else if (response.startsWith("HAND COUNTS: " ))
            			{
            				handCounts = response.substring(13).split(" ");
            			}
            			else if (response.startsWith("PLACE"))
            			{	
            				// info is suit, power, x, y, player number, visible
            				String[] info = response.substring(6).split(" ");
            				Card card = new Card(info[0], info[1], info[5]);
            				
            				if (Integer.parseInt(info[4]) == playerNum)
            				{
            					hand.remove(card);
            				}
            				
            				tableCards.add(card);
            				
            				// logic for rotating placed cards)	
            				int x = Integer.parseInt(info[2]);
            				int y = Integer.parseInt(info[3]);
            				
            				// translate point
            				Point newPoint = fitToPerspective(x,y,0, playerNum);
            				
            				x = newPoint.x;
            				y = newPoint.y;
            				
            				Bounds bound = new Bounds(x, y, x + cardWidth, y + cardHeight);
            				cardClickBoxes.put(card, bound);
            			}
            			else if (response.startsWith("MOVE"))
            			{
            				// info is suit, power, x, y, player number
            				String[] info = response.substring(5).split(" ");
            				Card card = new Card(info[0], info[1]);
            				
            				// logic for rotating placed cards based on player number
            				int x = Integer.parseInt(info[2]);
            				int y = Integer.parseInt(info[3]);
            				
            				// translate point
            				Point newPoint = fitToPerspective(x,y,0, playerNum);
            				
            				x = newPoint.x;
            				y = newPoint.y;
            				
            				Bounds bound = new Bounds(x, y, x + cardWidth, y + cardHeight);
            				cardClickBoxes.replace(card, bound);
            			}
            			else if (response.startsWith("FLIP"))
            			{
            				// info is suit, power, vis
            				String[] info = response.substring(5).split(" ");
            				Card cardToFlip = new Card(info[0], info[1], info[2]);
            				
            				// check hand and flip
            				for (Card c : hand)
            				{
            					if (c.equals(cardToFlip))
            					{
            						c.visible = cardToFlip.visible;
            					}
            				}
            				
            				// check table and flip
            				for (Card c : tableCards)
            				{
            					if (c.equals(cardToFlip))
            					{
            						c.visible = cardToFlip.visible;
            					}
            				}			
            			}
            			else if (response.startsWith("PICKUP"))
            			{
            				// info is suit, power, vis, player who picked up
            				String info[] = response.substring(7).split(" ");
            				Card card = new Card(info[0], info[1], info[2]);
            				int playerWhoPickedUp = Integer.parseInt(info[3]);
            				
            				if (playerWhoPickedUp == playerNum)
            				{
            					hand.add(card);
            				}
            				tableCards.remove(card);
            				cardClickBoxes.remove(card);
            				
            				// clear selected card
            				if (card.equals(selectedCard))
            				{
            					selectedCard = null;
            				}
            			}
            			else if (response.startsWith("DISCARD"))
            			{
            				// info is suit, power
            				String info[] = response.substring(8).split(" ");
            				Card card = new Card(info[0], info[1]);
            				
            				// remove from hand or table
            				if (tableCards.contains(card))
            				{
            					tableCards.remove(card);
            				}
            				else if (hand.contains(card))
            				{
            					hand.remove(card);
            				}
            				
            				// remove the click box
            				cardClickBoxes.remove(card);
            				
            				if (card.equals(selectedCard))
            				{
            					selectedCard = null;
            				}
            				
            			}
            			else if (response.startsWith("RESET"))
            			{
            				resetState();
            			}
            			else if (response.startsWith("CLEAR TABLE"))
            			{
            				clearTable();
            			}
            			else
            			{
            				// if we get an unintended sever call, just sleep a bit
            				Thread.sleep(50);
            			}
            			updateGraphics();
            		}
        		}
        	}
        	catch (Exception e)
        	{
        		String s = "Run Loop Error: " + e.toString() + "\n";
    			for (StackTraceElement element : e.getStackTrace())
    			{
    				s += element.toString() + "\n";
    			}
    			JOptionPane.showMessageDialog(this, s);
        		System.exit(1);
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
                	Card card = hand.get(i);              	
                	
                	if (card.visible)
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
            	Card card = tableCards.get(i);
            	Bounds cardB = cardClickBoxes.get(card);
            	if (card.visible)
            	{
            		bbg.drawImage(cardImages.get(card), cardB.topLeftX, cardB.topLeftY, null); 
            	}
            	else
            	{
            		bbg.drawImage(cardBack, cardB.topLeftX, cardB.topLeftY, null); 
            	}
            	            	
            }
            
            // paint opponents cards
            if (handCounts != null && handCounts.length > 1)
            {
            	for (int c = 0; c < handCounts.length; c++)
            	{
            		// if it's you, don't do anything
            		if (c == playerNum)
            		{
            			continue;
            		}
            		
            		// compute offset
            		int offset = c - playerNum;
            		int oppHandCount = Integer.parseInt(handCounts[c]);
            		int totalW = (cardWidth * (oppHandCount+1))/2;
            		
            		// adjust to positive
            		if (offset < 0)
            		{
            			offset += 4;
            		}
            		
            		if (offset == 1) // left player
            		{
            			int firstY = windowHeight/2 - totalW/2;
            			for (int i = 0; i < oppHandCount; i++)
		            	{
		            		bbg.drawImage(cardBack, 0, firstY + cardHeight*i/2, null);
		            	}
            		}
            		else if (offset == 2) // across player
            		{
		            	int firstX = windowWidth/2 - totalW/2;
		            	for (int i = 0; i < oppHandCount; i++)
		            	{
		            		bbg.drawImage(cardBack, firstX + cardWidth*i/2, 0, null);
		            	}
            		}
            		else if (offset == 3) // right player
            		{
            			int firstY = windowHeight/2 - totalW/2;
            			for (int i = 0; i < oppHandCount; i++)
		            	{
		            		bbg.drawImage(cardBack, windowWidth-cardHeight, firstY + cardHeight*i/2, null);
		            	}
            		}
            	}
            }        
                     
            // paint box around selected card and ghost card
            if (selectedCard != null && cardClickBoxes.containsKey(selectedCard))
            {
            	Bounds box = cardClickBoxes.get(selectedCard);              
                bbg.setColor(Color.BLACK);
                bbg.setStroke(new BasicStroke(2));
                bbg.drawRect(box.topLeftX, box.topLeftY, box.width, box.height);
            	
                float opacity = 0.5f;
                bbg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));	                
               
                Point ghostCard = getGhostCardPoint();
                if (selectedCard.visible)
            	{
                	bbg.drawImage(cardImages.get(selectedCard), ghostCard.x, ghostCard.y, null);                		
            	}
            	else
            	{
                	bbg.drawImage(cardBack, ghostCard.x, ghostCard.y, null);
            	}
                
                // change opacity back
                opacity = 1f;
                bbg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
                
                // if the player would pick up the card, draw border around hand
                int handWidth = (hand.size() + 1)*cardWidth/2;
	            int handX = windowWidth/2 - handWidth/2;
                if (ghostCard.y > handThresholdY && ghostCard.x > handX - cardWidth 
                		&& ghostCard.x < handX + handWidth)
                {
                	bbg.setColor(Color.YELLOW);
 	                bbg.setStroke(new BasicStroke(2));
 	                bbg.drawRect(handX, windowHeight - cardHeight, handWidth, cardHeight);
                }
            }
            
            // draw the click and drag box
            if (leftMouseDown)
            {
            	int topLeftX;
            	int topLeftY;
            	int width;
            	int height;
            	Point current = getMousePoint();
            	
            	// find coords for X
            	if (current.x < initialClick.x)
            	{
            		topLeftX = current.x;
            		width = initialClick.x - current.x;
            	}
            	else
            	{
            		topLeftX = initialClick.x;
            		width = current.x - initialClick.x;
            	}
            	
            	// find the coords for y
            	if (current.y < initialClick.y)
            	{
            		topLeftY = current.y;
            		height = initialClick.y - current.y;
            	}
            	else
            	{
            		topLeftY = initialClick.y;
            		height = current.y - initialClick.y;
            	}
            	
            	
            	bbg.setColor(Color.BLACK);
	            bbg.setStroke(new BasicStroke(2));
            	bbg.drawRect(topLeftX, topLeftY, width, height);
            }
            
            g.drawImage(backBuffer, insets.left, insets.top, this); 
        } 

        /**
         * Reset game to initial state
         */
        public void resetState()
        {
        	tableCards.clear();
        	hand.clear();
        	cardClickBoxes.clear();
        	selectedCard = null;
        	for (int i = 0; i < handCounts.length; i++)
        	{
        		handCounts[i] = "0";
        	}
        }
        
        /**
         * Clears the table of cards
         */
        public void clearTable()
        {
        	tableCards.clear();
        	cardClickBoxes.clear();
        	if (tableCards.contains(selectedCard))
        	{
        		selectedCard = null;
        	}
        }
        
        /**
         * Orders the hand
         */
        public void orderHand()
        {
        	for (int i = 0; i < hand.size(); i++) // for each hand index
        	{
        		// insertion sort
        		for (int j = 0; j < i; j++)
        		{
        			if (!hand.get(j).isBefore(hand.get(i)))
        			{
        				Card value = hand.get(i);
        				hand.remove(i);
        				hand.add(j, value);
        				break;
        			}
        		}
        	}
        }
        
        /**
         * Flips all Cards in hand face up
         */
        public void flipHandFaceUp()
        {
        	// go through hand and request flip for all non visible cards
        	for (Card c : hand)
        	{
        		if (!c.visible)
        		{
        			out.println("FLIP " + c.suit + " " + c.power + " 1");
        		}
        	}
        }
        
        /**
         * Reads in card images from resources
         */
        public void readInCardImages()
        {
        	cardImages = new HashMap<Card, Image>();
        	File resources = new File("Resources" + File.separatorChar + "Cards");
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
        		
        		cardImages.put(new Card(suit, power), image);
        	}
        }

        /**
         * Get the location of the mouse in the frame
         * @return point of mouse location
         */
        public Point getMousePoint()
        {
        	Point mouse = MouseInfo.getPointerInfo().getLocation();
        	Point frame = getLocationOnScreen();
        	frame.y += getInsets().top;
        	frame.x += getInsets().left;
        	return new Point(mouse.x - frame.x, mouse.y - frame.y);
        }
        
        /**
         * Gets the location of the upper left of the ghost card
         * @return the point of the upper left of the ghost card
         */
		public Point getGhostCardPoint()
		{
			Point mouse = getMousePoint();
            int x = mouse.x - cardWidth/2;
            int y = mouse.y - cardHeight/2;
            
            // create boundaries for the card so it won't overlap with opponent's hand
            if (y < cardHeight)
            {
            	y = cardHeight;
            }
            if (x < cardWidth)
            {
            	x = cardWidth;
            }
            if (x > windowWidth - 2 * cardWidth)
            {
            	x = windowWidth - 2 * cardWidth;
            }
            return new Point(x, y);
		}
		
		/**
		 * Converts a point to a different player's perspective
		 * @param x from x
		 * @param y from y
		 * @param fromPlayer player number to convert from
		 * @param toPlayer	player number to convert to
		 * @return the converted point
		 */
		public Point fitToPerspective(int x, int y, int fromPlayer, int toPlayer)
		{
			int tempHeight = windowHeight - 2*cardHeight;
			int tempWidth = windowWidth - 2*cardWidth;
			
			int offset = (fromPlayer - toPlayer);
			if (offset < 0)
			{
				offset += 4;
			}
			
			// System.out.println("Offset: " + offset);
			// System.out.println("In Coords: " + x + " " + y);
			
			// get the center of the card and take off the boundaries
			x = x + cardWidth/2 - cardWidth;
			y = y + cardHeight/2 -  cardHeight;
			
			int newX = x;
			int newY = y;
			
			// go one to the right ex 1 to 0
			if (offset == 1)
			{
				newY = (x*tempHeight)/tempWidth;
				newX = ((tempHeight - y)*tempWidth)/tempHeight;
			}
			// go across
			else if (offset == 2)
			{
				newY = tempHeight - y;
				newX = tempWidth - x;
			}
			// rotate to the left ex. 0 to 1
			else if (offset ==  3)
			{
				newX = (y*tempWidth)/tempHeight;
				newY = ((tempWidth - x)*tempHeight)/tempWidth; 
			}			
			// get the upper left
			newX = newX - cardWidth/2 + cardWidth;
			newY = newY - cardHeight/2 +  cardHeight;
			
			// System.out.println("Out Coords: " + newX + " " + newY);
			return new Point(newX, newY);
		}
		
		/**
		 * Called when a key is pressed
		 */
        @Override
		public void keyPressed(KeyEvent e) {
			// TODO Auto-generated method stub
        	
			
		}

        /**
         * Called when a key is released
         */
		@Override
		public void keyReleased(KeyEvent e) 
		{
			// shortcut commands
			if (e.getKeyCode() == KeyEvent.VK_D)
			{
				out.println("DRAW FROM DECK");
			}		
			else if (e.getKeyCode() == KeyEvent.VK_T)
			{
				// if a card is selected, discard that card
				if (selectedCard != null)
				{
					out.println("DISCARD " + selectedCard.suit + " " + selectedCard.power);
					selectedCard = null;
				}
			}
			else if (e.getKeyCode() == KeyEvent.VK_R)
			{
				// get confirm for reset
				int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to shuffle everyting into the deck?");
				if (confirm == JOptionPane.OK_OPTION)
				{
					out.println("RESET");
				}
			}
			else if (e.getKeyCode() == KeyEvent.VK_O)
			{
				orderHand();
			}
			else if (e.getKeyCode() == KeyEvent.VK_C)
			{
				// get confirm for reset
				int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear the table?");
				if (confirm == JOptionPane.OK_OPTION)
				{
					out.println("CLEAR TABLE");
				}
			}
			else if (e.getKeyCode() == KeyEvent.VK_F)
			{
				flipHandFaceUp();
			}
			
		}

		/**
		 * Called when a key is fully typed
		 */
		@Override
		public void keyTyped(KeyEvent e) {
				
		}

		/**
		 * Called when any of the mouse buttons are fully clicked
		 */
		@Override
		public void mouseClicked(MouseEvent e) 
		{
			
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
		public void mousePressed(MouseEvent e) 
		{
			// record mouse down
			if (e.getButton() == MouseEvent.BUTTON1)
			{
				leftMouseDown = true;
				initialClick = getMousePoint();
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) 
		{			
			// say mouse up
			if (e.getButton() == MouseEvent.BUTTON1)
			{
				leftMouseDown = false;
			}
			
			Point clickPoint = new Point(e.getPoint().x, e.getPoint().y - 26);
			
			// if right click and a card is selected, unselect card
			if (e.getButton() == MouseEvent.BUTTON3 && selectedCard != null)
			{
				selectedCard = null;
				updateGraphics();
				return;
			}
			// see if a card was clicked
			Card clickedCard = null;
			for (Card s : cardClickBoxes.keySet())
			{
				if (cardClickBoxes.get(s).contains(clickPoint))
				{
					clickedCard = s;
					break;
				}
			}	
			
			// if no card was selected and right mouse was clicked, flip clicked card
			if (e.getButton() == MouseEvent.BUTTON3 && selectedCard == null && clickedCard != null)
			{
				String command = "FLIP " + clickedCard.suit + " " + clickedCard.power + " " + (clickedCard.visible ? "0" : "1");
				out.println(command);
				System.out.println(command);
				return;
			}
			
			// if a card was clicked and no card is selected, select the card
			if (clickedCard != null && selectedCard == null)
			{
				selectedCard = clickedCard;
				if (tableCards.contains(selectedCard))
				{
					// move the selected card to the end of the list
					tableCards.remove(selectedCard);
					tableCards.add(selectedCard);
				}
			}
			// if table space was clicked and a card was selected
			else if (selectedCard != null)
			{
				// from hand
				if (hand.contains(selectedCard))
				{
					// place card and set selected card to empty
					Point playSpot = getGhostCardPoint();
					
					String request = "";
					if (playSpot.y  > handThresholdY)
					{
						// if no card was clicked
						if (clickedCard == null)
						{
							// place in front of hand
							if (playSpot.x < windowWidth/2)
							{
								hand.remove(selectedCard);
								hand.add(0, selectedCard);
							}
							else
							{
								hand.remove(selectedCard);
								hand.add(selectedCard);
							}
						}
						// if a card in your hand was clicked, then swap
						else if (hand.contains(clickedCard))
						{
							// swap cards in hand
							hand.remove(selectedCard);
							int cIndex = hand.indexOf(clickedCard);
							hand.add(cIndex+1, selectedCard);
						}
					}
					else
					{
						// convert to player 0's view
						Point p = fitToPerspective(playSpot.x, playSpot.y, playerNum, 0);						
						request = "PLACE " + selectedCard.suit + " " + selectedCard.power + " " + p.x + " " + p.y;
						System.out.println("Request: " + request);
						out.println(request);
					}					
					selectedCard = null;
				}
				
				// from table
				if (tableCards.contains(selectedCard))
				{
					Point playSpot = getGhostCardPoint();
					
					
					// if card placed in hand, pickup instead of move
					String request;					
					int handWidth = (hand.size() + 1)*cardWidth/2;
		            int handX = windowWidth/2 - handWidth/2;
					if (playSpot.y > handThresholdY && playSpot.x > handX - cardWidth 
	                		&& playSpot.x < handX + handWidth)
					{
						request = "PICKUP " + selectedCard.suit + " " + selectedCard.power;
					}
					else
					{
						// convert to player 0's view
						Point p = fitToPerspective(playSpot.x, playSpot.y, playerNum, 0);	
						request = "MOVE " + selectedCard.suit + " " + selectedCard.power + " " + p.x + " " + p.y;
					}
					
					System.out.println("Request: " + request);
					out.println(request);
					selectedCard = null;
				}
			}			
			updateGraphics();		
		}
}
