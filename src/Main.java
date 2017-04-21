/*
 * The starting File to run. Main contains a UDP connection that all clients will connect to initially.
 * Every time this Main server receives a string "log on <clientID>", it will assign a new MainWorker thread
 * 	that will create a specific instance of a UDP server connection and TCP server connection for each client.
 * That way multiple clients can be running simultaneously and can communicate through their own special UDP
 * 	and TCP connections.
 * Both UDP and TCP instances will close their connections and should terminate once the client is done communicating
 *	with them.
 * So if there were 5 clients communicating, there would be 5 pairs of UDP and TCP instances for each client, where
 * 	each connection has a unique socket (i.e. unique port).
 */

import sun.rmi.transport.tcp.TCPConnection;

import java.util.*;
import java.net.*;
import javax.crypto.spec.*;

//RUN THIS FILE FIRST, NEEDS AN ARGUMENT THAT IS THE PORT NUMBER FOR THE UDP SERVER
public class Main
{
	//Static arrays so that both the UDP and TCP instances can access/update them
	static ArrayList<String> clientIDs = new ArrayList<String>();
	static ArrayList<String> secretKeys = new ArrayList<String>();
	//for storing the random for CHALLENGE and rand_cookie for encryption
	//	Size of 2, [0] is the rand, and [1] is the rand_cookie
	static ArrayList<Integer[]> randoms = new ArrayList<Integer[]>();
	//Contains the hash strings for authentication
	static ArrayList<String> authenHashes = new ArrayList<String>();
	//Contains the secretkeys used for encryption
	static ArrayList<SecretKeySpec> encryptKeys = new ArrayList<SecretKeySpec>();

	static HashMap<String, TcpServer> tcpConns = new HashMap<>();
	static HashMap<String, ClientObject> clientObjects = new HashMap<>();

	
	public static void main(String[] args)
	{
		String[] tokens;
		byte[] sendMessage = new byte[1024], receiveMessage = new byte[1024], data;
		int serverPort = Integer.parseInt(args[0]), clientIndex = -1, clientPort;
		InetAddress IPAddress;
		DatagramPacket sendPacket, receivePacket;
		DatagramSocket serverSocket;
		
		//Reads a subscriptions.txt file where each row contains "<clientID> <secretkey>"
		try
		{
			java.util.Scanner input = new java.util.Scanner(new java.io.File("subscriptions.txt"));
			String str;
			
			//Stores text information into the arraylists
			while(input.hasNext())
			{
				str = input.nextLine();
				tokens = str.split(" ");
				clientIDs.add(tokens[0]);
				secretKeys.add(tokens[1]);
				//creates the these arraylists to be the same size
				randoms.add(null);
				authenHashes.add(null);
				encryptKeys.add(null);
			}
			input.close();
			//NOTE: may not be needed; see the MainWorker class for explanation
			MainWorker.isTcpRunning = new boolean[clientIDs.size()];
		}
		catch(java.io.FileNotFoundException e)
		{
			e.printStackTrace();
		}
		
		//Creates the initial UDP server that all initial messages
		//	will be sent to
		try
		{
			serverSocket = new DatagramSocket(serverPort);
			
			while(true)
			{
				receivePacket = new DatagramPacket(receiveMessage, receiveMessage.length);
				//reset the byte array to flush out any old data
				receiveMessage = new byte[1024];
				serverSocket.receive(receivePacket);
				
				//****
				System.out.println("received message from main");
				
				//trims any padded junk-text from the 1024 byte receive packet
				data = new byte[receivePacket.getLength()];
				System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), data, 0, receivePacket.getLength());
				tokens = new String(data).split(" ");
				
				//Retrieve the client's IP and port
				IPAddress = receivePacket.getAddress();
				clientPort = receivePacket.getPort();
				
				//Sees if a client wanted to log on
				if(tokens[0].equalsIgnoreCase("log") && tokens[1].equalsIgnoreCase("on"))
				{
					//checks the array of clientIDs and sees which index they are in the array
					for(int i = 0; i < clientIDs.size(); i++)
					{
						if(tokens[2].equals(clientIDs.get(i)))
						{
							clientIndex = i;
							break;
						}
					}
					
					//Creates an object of MainWorker that will service each client
					//	and then calls it as a thread
					MainWorker mw = new MainWorker(clientPort, IPAddress, clientIndex); 
					Thread t = new Thread(mw);
					t.start();
				}
				//Client did not send "log on <clientID>"; send back error 
				else
				{
					sendMessage = new String("ERROR Please log on with a"
							+ " username using \"log on <YOUR_CLIENTID>\"").getBytes();
					sendPacket = new DatagramPacket(sendMessage, sendMessage.length, IPAddress, clientPort);
					serverSocket.send(sendPacket);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
}

class MainWorker implements Runnable
{
	//NOTE: may not be needed
	static boolean[] isTcpRunning;
	//Values of udp and tcp ports start here
	//	the values will increment and a unique value
	//	will be used for each udp and tcp connection
	static int udpPortNumbers = 6556;
	static int tcpPortNumbers = 9000;
	int tcpPort = -3, udpPort = -2, clientIndex = -1;
	InetAddress IPAddress;
	
	MainWorker(int p, InetAddress IP, int client)
	{
		udpPort = p;
		IPAddress = IP;
		clientIndex = client;
	}
	
	public void run()
	{
		//****
		System.out.println("in main worker with port " + udpPort + ", IP " + IPAddress + ", clientindex " + clientIndex);
		
		//creates an instance of a UDP server specifically
		//	for this MainWorker thread
		Server s = new Server();
		s.begin(udpPort, IPAddress, clientIndex);
		tcpPort = s.getTcpPort();
		
		//****
		System.out.println("tcpport is " + tcpPort);
		
		//NOTE: may not be needed
		//	s.begin will run until the udp socket closes
		//	so the boolean will already be true by the time we get here
		//	Feel free to comment out and see if it still runs correctly
		while(!isTcpRunning[clientIndex])
		{
			//I thought this was needed because I wanted this thread
			//	to wait until the UDP instance signals that the client will be
			//	wanting to connect to a TCP server soon
			//If possible, can maybe swap with semaphores
		}
		
		//creates a specific instance of TCP
		
		//**NOTE: there is a possibility that a client may try to connect to
		//	this TCP port before the TCP server has even started running

		//TCP server is the only thing now,
		TcpServer ts = new TcpServer();
		Main.tcpConns.put(Main.clientIDs.get(clientIndex), ts);
		ts.begin(tcpPort, clientIndex);
	}
}
