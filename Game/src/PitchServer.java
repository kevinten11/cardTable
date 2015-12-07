import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class PitchServer {

	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub

		ServerSocket listener = new ServerSocket(8901);
		System.out.println("Pitch Server is Running");
		HashMap<String, PitchModel> tableMap = new HashMap<String, PitchModel>();
		try
		{
			while (true)
			{
				Socket playerSocket = listener.accept();
				BufferedReader input = new BufferedReader(new InputStreamReader(playerSocket.getInputStream()));
				PrintWriter output = new PrintWriter(playerSocket.getOutputStream(), true);
				String tableName = input.readLine();
				
				if (tableMap.containsKey(tableName))
				{
					output.println("FOUND");
					tableMap.get(tableName).addPlayer(playerSocket);
				}
				else
				{
					output.println("NOT FOUND");
					PitchModel model = new PitchModel();
					tableMap.put(tableName, model);
					model.addPlayer(playerSocket);
				}
			}				
		}
		catch (Exception e)
		{
			System.out.println("MAIN");
			e.printStackTrace();
		}
		finally 
		{
			listener.close();
			System.exit(0);
		}
	}

}
