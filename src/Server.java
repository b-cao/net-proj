import java.net.*;

public class Server
{
	/*
	 * NOTE: need to pass in an argument when running file
	 * 		pass in the port number of the server
	 */
	public static void main(String[] args)
	{
		byte[] sendMessage = new byte[1024], receiveMessage = new byte[1024];
		int serverPort = Integer.parseInt(args[0]), clientPort;
		String str, newstr;
		InetAddress IPAddress;
		DatagramPacket sendPacket, receivePacket; 
		try
		{
			DatagramSocket serverSocket = new DatagramSocket(serverPort);
			while(true)
			{
				receivePacket = new DatagramPacket(receiveMessage, receiveMessage.length);
				receiveMessage = new byte[1024];	//reset the byte array to flush out any old data
				serverSocket.receive(receivePacket);
				str = new String(receivePacket.getData());
				System.out.println("From Client: " + str);
				
				//for testing purposes
				newstr = new String(str.toUpperCase());
				
				sendMessage = newstr.getBytes();
				IPAddress = receivePacket.getAddress();
				clientPort = receivePacket.getPort();
				sendPacket = new DatagramPacket(sendMessage, sendMessage.length, IPAddress, clientPort);
				serverSocket.send(sendPacket);
				sendMessage = new byte[1024];	//reset the byte array to flush out any old data
			}
		}
		catch (java.io.IOException e)
		{
			e.printStackTrace();
		}
		
	}
}
