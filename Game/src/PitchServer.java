import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class PitchServer {

	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub

		ServerSocket listener = new ServerSocket(36636);
		System.out.println("Pitch Server is Running");
		HashMap<String, PitchModel> tableMap = new HashMap<String, PitchModel>();
		try
		{
			while (true)
			{			
				Socket playerSocket = listener.accept();
				ObjectOutputStream output = new ObjectOutputStream(playerSocket.getOutputStream());
				ObjectInputStream input = new ObjectInputStream(playerSocket.getInputStream());	
				
				String tableName = (String)input.readObject();
				if (tableMap.containsKey(tableName))
				{
					System.out.println("Found table");
					output.writeObject(true);
					tableMap.get(tableName).addPlayer(input, output);
				}
				else
				{
					System.out.println("Creating table: " + tableName);
					output.writeObject(false);
					PitchModel model = new PitchModel();
					tableMap.put(tableName, model);
					model.addPlayer(input, output);
				}
			}				
		}
		catch (Exception e)
		{
			System.out.println("MAIN");
			e.printStackTrace();
		}
		listener.close();
		System.exit(0);
	}

}
