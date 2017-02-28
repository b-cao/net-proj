/* 
 * Questions:
 * A) Would we need multithreading?
 */

import java.net.*;

public class Client
{
	/*NOTE: run this file with a port number argument
	 * 		this is the port number of the server
	*/
	public static void main(String[] args)
	{
		byte[] sendMessage = new byte[1024], receiveMessage = new byte[1024];
		java.util.Scanner input = new java.util.Scanner(System.in);
		String str;
		int port = Integer.valueOf(args[0]);
		
		try
		{
			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress IPAddress = InetAddress.getByName("localhost");
			
			System.out.print("Hello. How may I help you? :");
			str = input.nextLine();
			sendMessage = str.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendMessage, sendMessage.length, IPAddress, port);
			clientSocket.send(sendPacket);
			
			DatagramPacket receivePacket = new DatagramPacket(receiveMessage, receiveMessage.length);
			clientSocket.receive(receivePacket);
			String newstr = new String(receivePacket.getData());
			System.out.println("From Server: " + newstr);
			
			input.close();
			clientSocket.close();
		} 
		catch (java.io.IOException e)
		{
			e.printStackTrace();
		}
	}
}
