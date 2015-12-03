import java.net.ServerSocket;

public class PitchServer {

	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub

		ServerSocket listener = new ServerSocket(8901);
		System.out.println("Pitch Server is Running");
		PitchModel model = new PitchModel();
		try
		{
			for (int i = 0; i < 4; i++)
			{
				System.out.println("Call for Plr:" + i);
				model.addPlayer(listener.accept(), i);
				model.players.get(i).start();
			}				
		}
		finally 
		{
			while (model.players.size() > 0)
			{
				Thread.sleep(1000);
			}
			listener.close();
			System.exit(0);
		}
	}

}
