/*
 * CS 4390
 * 
 * Questions:
 * A) Can we use same port number for all clients, since UDP is stateless?
 * 		If not, how can we implement multiple clients for UDP?
 * B) Would we need multithreading?
 */

import java.net.*;
import java.util.*;
import java.security.*;
import javax.crypto.spec.*;
import javax.crypto.*;
import javax.xml.bind.DatatypeConverter;

public class Client
{
	/*NOTE: run this file with a port number argument
	 * 		this is the port number of the server
	*/
	public static void main(String[] args)
	{
		//may need to change it so that a client can only know its own
		//	clientID, secretKey, and encryptionKey instead of all of them
		byte[] sendMessage = new byte[1024], receiveMessage = new byte[1024];
		Scanner input;
		String str = null, clientID = null; 
		String[] tokens;
		ArrayList<String> clientIDs = new ArrayList<String>();
		ArrayList<String> secretKeys = new ArrayList<String>();
		ArrayList<SecretKeySpec> encryptKeys = new ArrayList<SecretKeySpec>();
		int port = Integer.valueOf(args[0]);
		int clientIndex = -1, randCookie = -1, tcpPort = -1;
		
		//read text file and store ids in an array and secret keys in another array
		try
		{
			input = new Scanner(new java.io.File("subscriptions.txt"));
			
			while(input.hasNext())
			{
				str = input.nextLine();
				tokens = str.split(" ");
				clientIDs.add(tokens[0]);
				secretKeys.add(tokens[1]);
				//creates the encryptHashes arraylist to be the same size
				encryptKeys.add(null);
			}
			input.close();
		}
		catch(java.io.FileNotFoundException e)
		{
			e.printStackTrace();
		}
		
		input = new Scanner(System.in);
		System.out.print("Hello. Please log on with your client ID: ");
		str = input.nextLine();
		if(!(str.startsWith("log on") || str.startsWith("LOG ON")))
		{
			System.out.println("ERROR. You must enter \"log on <YOUR_CLIENTID>\"");
			input.close();
			return;
		}
		
		try
		{
			DatagramSocket clientSocket = new DatagramSocket();
			DatagramPacket receivePacket, sendPacket;
			InetAddress IPAddress = InetAddress.getByName("localhost");
			byte[] data;
			MessageDigest md;
			
			//tokenize the "log on CLIENTID"
			tokens = str.split(" ");
			clientID = tokens[2];
			sendMessage = new String("log on " + clientID).getBytes();
			System.out.println("sending message to main server, Message: "+ sendMessage + "IPAddress: "+IPAddress+" port: "+port);
			sendPacket = new DatagramPacket(sendMessage, sendMessage.length, IPAddress, 4444);
			//send the "log on <clientID>" to the initial main UDP server
			clientSocket.send(sendPacket);
			
			//****
			System.out.println("Message send to main server");
			
			receivePacket = new DatagramPacket(receiveMessage, receiveMessage.length);
			//reset the byte array to flush out any old data
			receiveMessage = new byte[1024];
			clientSocket.receive(receivePacket);
			
			//****
			System.out.println("received message from main server");
			
			//trims any padded junk-text from the 1024 byte receive packet
			data = new byte[receivePacket.getLength()];
			System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), data, 0, receivePacket.getLength());
			tokens = new String(data).split(" ");
			
			//the port number that the main UDP server sends back to us will be
			//	the UDP connection we use from now on
			if(tokens[0].equals("port"))
			{
				port = Integer.parseInt(tokens[1]);
			}
			else
			{
				System.out.println("ERROR Unexpected message from Main UDP server");
				System.exit(-100);
			}
			
			//send the HELLO protocol
			sendMessage = new String("HELLO " + clientID).getBytes();
			sendPacket = new DatagramPacket(sendMessage, sendMessage.length, IPAddress, port);
			clientSocket.send(sendPacket);
			
			while(true)
			{
				//receive the message from server
				receivePacket = new DatagramPacket(receiveMessage, receiveMessage.length);
				//reset the byte array to flush out any old data
				receiveMessage = new byte[1024];
				clientSocket.receive(receivePacket);
				
				//trims any padded junk-text from the 1024 byte receive packet
				data = new byte[receivePacket.getLength()];
				System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), data, 0, receivePacket.getLength());
				tokens = new String(data).split(" ");
				
				if(tokens[0].equals("SUBSCRIBER_FAIL"))
				{
					System.out.println("ERROR The client ID you used was not on our subscriber's list");
					System.exit(-200);
				}
				
				else if(tokens[0].equals("CHALLENGE"))
				{
					/*grab the 2nd token (rand)
					iterate through the clientID arraylist
					check if clientID is in there
					if so, set it to clientIndex and then exit loop
					access the clientIndex's secret key and concat w/ rand
					get the hash and send back the hash*/
					
					boolean flag = false;
					for(int i = 0; i < clientIDs.size(); i++)
					{
						if(clientIDs.get(i).equals(clientID))
						{
							clientIndex = i;
							flag = true;
							break;
						}
					}
					
					if(flag)
					{
						//creates RES for authentication
						//RES is the hash1 function using the rand+secretkey as the string to hash
						//hash1 uses MD5
						String challenge = tokens[1] + secretKeys.get(clientIndex);
						md = MessageDigest.getInstance("MD5"); 
						md.update(challenge.getBytes(), 0, challenge.length());
						//initialize a BigInteger object to utilize the class's ability to output a string in hex
						String res = new java.math.BigInteger(1, md.digest()).toString(16);
						
						//**NOTE: may need to move this snippet in the AUTH_SUCCESS section
						//generates the CK_A key for encryption
						//another hash2 function is used using the rand+secretkey string
						//this string turns into a hash which is then used as the secret key for the AES encryption
						byte[] key = challenge.getBytes();
						md = MessageDigest.getInstance("SHA-1");
						key = md.digest(key);
						//key needs to be 128 bits, so we trim it to 16 bytes (128 bits)
						key = Arrays.copyOf(key, 16);
						SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
						encryptKeys.set(clientIndex, secretKeySpec);
						
						//send back a response protocol 
						sendMessage = new String("RESPONSE " + res).getBytes();
						sendPacket = new DatagramPacket(sendMessage, sendMessage.length, IPAddress, port);
						clientSocket.send(sendPacket);
					}
					//Unexpected error
					else
					{
						System.out.println("ERROR encountered: " + new String(data));
						clientSocket.close();
						input.close();
						System.exit(-3);
					}
					
				}
				
				//Authentication failed
				else if(tokens[0].equals("AUTH_FAIL"))
				{
					System.out.println("Authentication has failed with client ID " + clientID);
					input.close();
					//This might stop the UDP server (most likely not)
					clientSocket.close();
					return;
				}
				
				//something expected went wrong that I might be aware of
				else if(tokens[0].equals("ERROR"))
				{
					//output the error message, and then system.exit(-1)
					System.out.println(new String(data));
					input.close();
					clientSocket.close();
					System.exit(-1);
				}
				
				//something unexpected went wrong
				//e.g. unrecognized text
				//OR
				//it is an ecnrypted message
				else
				{
					//****
					//using data because it is the trimmed version of receivePacket
					String mess = decrypt(data, encryptKeys.get(clientIndex));
					System.out.println("decrypt attempt: " + mess);
					
					tokens = decrypt(data, encryptKeys.get(clientIndex)).split(" ");
					
					
					if(tokens[0].equals("AUTH_SUCCESS"))
					{
						randCookie = Integer.parseInt(tokens[1]);
						tcpPort = Integer.parseInt(tokens[2]);
						//break out of loop to join a new loop for TCP
						clientSocket.close();
						input.close();
						break;
						
					}
					//Unexpected error, display data string and exit with code -2
					else
					{
						System.out.println("Unexpected error: " + new String(data));
						input.close();
						clientSocket.close();
						System.exit(-2);
					}
					
				}
			}
		} 
		catch (java.io.IOException|java.security.NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		
		//NOTE: for TCP, we will encryt/decrypt messages using the prepareOutMessage/prepareInMessages respectively
		try
		{
			//****
			System.out.println("client " + clientID + " starting TCP connection with port " + tcpPort);
			
			//NOTE: May need to thread.sleep for a second to make sure the tcp server is running first
			InetAddress IPAddress = InetAddress.getByName("localhost");
			Socket clientTCP = new Socket(IPAddress, tcpPort);
			java.io.PrintWriter out = new java.io.PrintWriter(clientTCP.getOutputStream(), true);
			java.io.BufferedReader in = new java.io.BufferedReader(
					new java.io.InputStreamReader(clientTCP.getInputStream()));
			String messageOut = null, messageIn = null;
			byte[] data = null;
			
			
			//****
			System.out.println("Attempting to connect to tcp server with clientindex " + clientIndex);
			
			//Now, we encrypt a CONNECT message and send it to the TCP server
			//TCP server should receive it and send back a CONNECTED message
			messageOut = "CONNECT " + randCookie;
			messageOut = prepareOutMessage(data, messageOut, clientIndex, encryptKeys);
			
			//****
			System.out.println("CONNECT encrypted is " + messageOut);
			System.out.println("CONNECT decrypted is " + prepareInMessage(data, messageOut, clientIndex, encryptKeys));
			
			out.println(messageOut);
			
			while(true)
			{
				messageIn = in.readLine();
				messageIn = prepareInMessage(data, messageIn, clientIndex, encryptKeys);
				tokens = messageIn.split(" ");

				if(tokens[0].equals("CONNECTED"))
				{
					//****
					System.out.println("Client is connected to TCP server");
				}
				
				else if(tokens[0].equals("ERROR"))
				{
					System.out.println(messageIn);
					out.close();
					in.close();
					clientTCP.close();
					break;
				}
				
				//Unrecognized error
				else
				{
					System.out.println(messageIn);
					out.close();
					in.close();
					clientTCP.close();
					break;
				}
			}
		}
		catch(java.io.IOException e)
		{
			e.printStackTrace();
		}

	}
	
	//AES encryption
	public static byte[] encrypt(String message, SecretKeySpec secretKeySpec)
	{
		byte[] encrypted = null;
		try
		{
			Cipher aesCipher = Cipher.getInstance("AES");
			aesCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
			encrypted = aesCipher.doFinal(message.getBytes("UTF-8"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
        return encrypted;
	}
	
	//AES decryption
	public static String decrypt(byte[] encrypted, SecretKeySpec secretKeySpec)
	{
		byte[] decrypted = null;
		
		try
		{
			Cipher aesCipher = Cipher.getInstance("AES");
			aesCipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
			decrypted = aesCipher.doFinal(encrypted);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return new String(decrypted);
	}
	
	//Used for TCP decryption
	public static String prepareInMessage(byte[] data, String messageIn, int clientIndex, ArrayList<SecretKeySpec> encryptKeys)
	{
		data = DatatypeConverter.parseBase64Binary(messageIn);
		messageIn = decrypt(data, encryptKeys.get(clientIndex));
		return messageIn;
	}
	
	//Used for TCP encryption
	public static String prepareOutMessage(byte[] data, String messageOut, int clientIndex, ArrayList<SecretKeySpec> encryptKeys)
	{
		data = encrypt(messageOut, encryptKeys.get(clientIndex));
		messageOut = DatatypeConverter.printBase64Binary(data);
		return messageOut;
	}
}
