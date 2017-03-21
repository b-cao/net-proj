/*
 * CS 4390
 */

import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class Server
{
	/*
	 * NOTE: need to pass in an argument when running file
	 * 		pass in the port number of the server
	 * 
	 * (?) Will we need mutual exclusion when multiple clients are trying to log on?
	 * (?) What will happen if client A logs on and while handshaking, client B tries to log on?
	 * 		(?) should the handshaking be mutually exclusive, or could the server be handling multiple handshaking protocols at once?
	 * (?) we are expected to encrypt our messages, but how would we decrypt our messages when md5 and sha are one-way encrypts
	 * 		Or would we use our own simpler hash function (e.g. ceasar cipher)
	 * 		(?) based on the example, do we encrypt the rand with the client secret key separately and then encrypt our message?
	 * 			Or we do only encrypt the rand plus secret key to determine if this is our message before it can read our normal message
	 * (?) do we use the same port for tcp connection or different ports?
	 */
	public static void main(String[] args)
	{
		byte[] sendMessage = new byte[1024], receiveMessage = new byte[1024];
		int serverPort = Integer.parseInt(args[0]), clientPort;
		String str;
		String[] tokens;
		ArrayList<String> clientIDs = new ArrayList<String>();
		ArrayList<String> secretKeys = new ArrayList<String>();
		//for storing the random for CHALLENGE and rand_cookie for encryption 
		ArrayList<Integer[]> randoms = new ArrayList<Integer[]>();
		ArrayList<String> authenHashes = new ArrayList<String>();
		ArrayList<SecretKeySpec> encryptKeys = new ArrayList<SecretKeySpec>();
		java.util.Random rng = new java.util.Random();
		
		InetAddress IPAddress;
		DatagramPacket sendPacket, receivePacket;
		int clientIndex = -1;
		
		//read text file and store ids in an array and secret keys in another array
		try
		{
			java.util.Scanner input = new java.util.Scanner(new java.io.File("subscriptions.txt"));
			
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
		
		try
		{
			DatagramSocket serverSocket = new DatagramSocket(serverPort);
			//using MD for authentication between client and server and encryption
			MessageDigest md;
			Integer rand, randCookie;
			byte[] data;
			
			//might have to get rid of loop and process each handshaking step in order
			while(true)
			{
				receivePacket = new DatagramPacket(receiveMessage, receiveMessage.length);
				//reset the byte array to flush out any old data
				receiveMessage = new byte[1024];
				serverSocket.receive(receivePacket);
				
				//trims any padded junk-text from the 1024 byte receive packet
				data = new byte[receivePacket.getLength()];
				System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), data, 0, receivePacket.getLength());
				tokens = new String(data).split(" ");
				
				IPAddress = receivePacket.getAddress();
				clientPort = receivePacket.getPort();
				
				if(tokens[0].equals("HELLO"))
				{
					boolean flag = false;
					String challenge, xres;
					//MD5 hash for authentications
					md = MessageDigest.getInstance("MD5");
					
					for(int i = 0; i < clientIDs.size(); i++)
					{
						//generate random integers for each CHALLENGE
						rand = new Integer(rng.nextInt(100000));
						//STILL do not know what rand cookie is for *cries*
						randCookie = new Integer(rng.nextInt(100000));
						randoms.add(new Integer[]{rand, randCookie});
						
						//server uses the rand value above and concats with the client's secret key as a string
						//string goes through MD5 hash and stores it
						//server will wait until the client sends back a hash value and then the server will compare
						//	both hashes to see if they match
						challenge = new String(randoms.get(i)[0].toString() + secretKeys.get(i));
						md.update(challenge.getBytes(), 0, challenge.length());
						xres = new java.math.BigInteger(1, md.digest()).toString(16);
						
						authenHashes.add(xres);
						
						if(tokens[1].equals(clientIDs.get(i)))
						{
							flag = true;
							clientIndex = i;
						}
					}
					
					if(flag)
					{
						//issue a challenge protocol to client, sending a rand value
						String protocol = "CHALLENGE " + randoms.get(clientIndex)[0].toString();
						sendMessage = new String(protocol).getBytes();
						sendPacket = new DatagramPacket(sendMessage, sendMessage.length, IPAddress, clientPort);
						serverSocket.send(sendPacket);
						
					}
					//in the future, just send back a message saying ERROR, this ID is not subscribed
					else
					{
						sendMessage = new String("FAIL").getBytes();
					}
				}
				
				else if(tokens[0].equals("RESPONSE"))
				{
					//compares the hash value the client has sent back with the hash value
					//	that the server previously computed and stored
					if(!tokens[1].equals(authenHashes.get(clientIndex)))
					{
						sendMessage = new String("AUTH_FAIL").getBytes();
						sendPacket = new DatagramPacket(sendMessage, sendMessage.length, IPAddress, clientPort);
						serverSocket.send(sendPacket);
					}
					
					else
					{
						//hashes match, meaning that the client is a subscriber
						//begin to encrypt messages using AES
						//encrypt the AUTH_SUCCESS protocol and send it back to client
						
						byte[] key = (randoms.get(clientIndex)[0].toString() + secretKeys.get(clientIndex)).getBytes();
						md = MessageDigest.getInstance("SHA-1");
						//returns a byte array hash
						key = md.digest(key);
						//truncates the hash to 16 bytes (128 bits)
						key = Arrays.copyOf(key, 16);
						
						//create the encryption key (CK_A) using the hash from our rand and secret key
						SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
						encryptKeys.set(clientIndex, secretKeySpec);
						
						
						//string contains the protocol AUTH_SUCCESS with rand cookie and port number
						//(?) is the port number different from each client?
						String message = "";
						
						//******TESTING PURPOSES ONLY******
						sendMessage = encrypt("hello world!", secretKeySpec);
						//this is important for displaying it as a hex ONLY
//						sendMessage = new java.math.BigInteger(1, sendMessage).toString(16).getBytes();
						sendPacket = new DatagramPacket(sendMessage, sendMessage.length, IPAddress, clientPort);
						serverSocket.send(sendPacket);
					}
				}
				
				else if(tokens[0].equals("CONNECT"))
				{
					
				}
				
				//something unexpected went wrong
				//e.g. unrecognized text
				//OR
				//it is an ecnrypted message
				else
				{
					//decrypt the message, and then create more conditionals for other protocols
					//have a final else state in here for the unrecognized text
					//	for unrecog. text, output that there is an unexpected error, and then system.exit(-2)
				}
			}
		}
		catch (java.io.IOException|java.security.NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		
	}
	
	//AES encrypt
	public static byte[] encrypt(String message, SecretKeySpec secretKeySpec)
	{
		byte[] encrypted = null;
		try
		{
			Cipher aesCipher = Cipher.getInstance("AES");
			aesCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
			encrypted = aesCipher.doFinal(message.getBytes());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
        return encrypted;
	}
	
	//AES decrypt
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
}
