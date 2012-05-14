import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * 
 * @author John Linford
 * 
 * A simpler server that listens to port 7890 for update of scores
 * for the "Shooter" game. Sits in a loop to listen, and spawns a 
 * seperate thread to handle any events if a connection is made
 *
 */
public final class SimpleServer extends Thread
{
	private static final int PORT = 7890;
	private static final int NUM_CONNECT = 1;

	private HashMap<String, String> hashMap;
	private PrintWriter writer;
	private BufferedReader reader;

	private static int userNum = 0;

	private SimpleServer(){}

	public static void main(String args[])
	{
		SimpleServer myServer = new SimpleServer();

		if(myServer !=null) 
		{
			myServer.start();
		}
	}

	public void run()
	{
		// create HashMap to store scores
		hashMap = new HashMap<String, String>();
		
		ServerSocket socket = null;
		boolean listen = true;
		// Try to listen to port number and establish connection if possible
		try
		{
			socket = new ServerSocket(PORT, NUM_CONNECT);

			// spawn a new thread every time we need a new connection
			while (listen)
			{
				new ScoreServerThread(socket.accept()).start();
			}
		}
		catch(IOException ie)
		{
			System.err.println("Could not listen on port: " + PORT);
			ie.printStackTrace();
			System.exit(-1);
		}
		finally
		{
			try
			{
				socket.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}


	
	/**
	 * 
	 * A sepearte thread of the server to handle a single request.
	 * Exits upon success, and the server then returns to listen again
	 *
	 */
	private class ScoreServerThread extends Thread
	{
		private Socket client = null;

		public ScoreServerThread(Socket socket)
		{
			super("ScoreServerThread");
			this.client = socket;
		}

		public void run()
		{
			try
			{
				System.out.println("Connection Established: " + client.toString());

				// reader for input
				reader = new BufferedReader(new InputStreamReader(client.getInputStream()));

				// write to output
				writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())));

				// Write Mood Server to output Stream
				writer.write("Score Server\n");
				writer.flush();

				String line;

				line = reader.readLine();
				System.out.println("Input read: " + line);
				
				// process the input
				process(line);
			}
			catch (IOException ie)
			{
				ie.printStackTrace();
			}
			finally
			{
				try
				{
					reader.close();
					writer.close();
					client.close();
				}
				catch (IOException ie)
				{
					ie.printStackTrace();
				}
			}
		}

		public void process(String input)
		{
			if (input == null)
				return;
			
			System.out.println("Processing: " + input);

			// register the name to hashMap
			if (input.contains("Score"))
			{
				String score = null;

				// extract the name (happens directly after ':')
				for (int i = 0; i < input.length(); i++)
				{
					if (input.charAt(i) == ':')
					{
						score = input.substring(++i);
						break;
					}
				}

				// store the score in our hashmap
				hashMap.put(score, "" + userNum++);

				// output the rank of our score
				writer.write("" + getRank(score));
				writer.flush();

				System.out.println("Score added: " + score);
			}
		}

		// get the rank of the value we just added
		public int getRank(String s)
		{
			int rank = 0;
			// Get a set of the entries 
			Set<Entry<String, String>> set = hashMap.entrySet();

			// Get an iterator 
			Iterator<Entry<String, String>> i = set.iterator();

			while(i.hasNext()) 
			{ 
				Map.Entry me = (Map.Entry)i.next(); 

				if (Integer.parseInt((String)me.getKey()) >= Integer.parseInt(s))
					rank++;
			}

			return rank;
		}

		// reset all scores to zero
		public void reset()
		{
			// Get a set of the entries 
			Set<Entry<String, String>> set = hashMap.entrySet();

			// Get an iterator 
			Iterator<Entry<String, String>> i = set.iterator();

			while(i.hasNext()) 
			{ 
				Map.Entry me = (Map.Entry)i.next(); 
				me.setValue(0);
			}
		}
	}
}