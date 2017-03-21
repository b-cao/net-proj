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
		String str, clientID;
		String[] tokens;
		ArrayList<String> clientIDs = new ArrayList<String>();
		ArrayList<String> secretKeys = new ArrayList<String>();
		ArrayList<SecretKeySpec> encryptKeys = new ArrayList<SecretKeySpec>();
		int port = Integer.valueOf(args[0]);
		int clientIndex = -1;
		
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
		
		/*
		 * user will type in "log on (clientID)" [caps sensitive]
		 * string will be parsed to get the clientID
		 * send a udp message as "HELLO (clientID)"
		 * server should either send back "CHALLENGE (rand)" or "AUTH_FAIL"
		 * ...etc 
		 */
		
		input = new Scanner(System.in);
		System.out.print("Hello. Please log on with your client ID (case sensitive): ");
		str = input.nextLine();
		if(!str.startsWith("log on"))
		{
			System.out.println("ERROR. You must enter \"log on YOUR_CLIENTID\"");
			input.close();
			return;
		}
		
		try
		{
			DatagramSocket clientSocket = new DatagramSocket();
			DatagramPacket receivePacket;
			InetAddress IPAddress = InetAddress.getByName("localhost");
			byte[] data;
			MessageDigest md;
			
			//tokenize the "log on CLIENTID"
			tokens = str.split(" ");
			clientID = tokens[2];
			//prepare to send back the protocol "HELLO CLIENTID"
			sendMessage = new String("HELLO " + clientID).getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendMessage, sendMessage.length, IPAddress, port);
			clientSocket.send(sendPacket);
			
			//may need to get rid of the while loop here
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
				
				if(tokens[0].equals("CHALLENGE"))
				{
					//grab the 2nd token (rand)
					//iterate through the clientID arraylist
					//check if clientID is in there
					//if so, set it to clientIndex and then exit loop
					//access the clientIndex's secret key and concat w/ rand
					//get the hash and send back the hash
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
						System.out.println("ERROR encountered");
						System.exit(-3);
					}
					
				}
				
				//NOTE: might have to relocated into the else conditional
//				//	because it might be encrypted first
				else if(tokens[0].equals("AUTH_SUCCESS"))
				{
					
				}
				
				else if(tokens[0].equals("AUTH_FAIL"))
				{
					
				}
				
				//NOTE: might have to relocate into the else conditional
				//	because it might be encrypted first
				else if(tokens[0].equals("CONNECTED"))
				{
					//Might need to close UDP socket and start establishing TCP connection
					//input.close();
					//clientSocket.close();
					
					//exit loop and then open a TCP connection
				}
				
				//something expected went wrong that I might be aware of
				else if(tokens[0].equals("ERROR"))
				{
					//output the error, and then system.exit(-1)
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
					
					//*****TESTING PURPOSES*****
					//using data because it is the trimmed version of receivePacket
					String mess = decrypt(data, encryptKeys.get(clientIndex));
					System.out.println("decrypt attempt: " + mess);
				}
			}
		} 
		catch (java.io.IOException|java.security.NoSuchAlgorithmException e)
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
			encrypted = aesCipher.doFinal(message.getBytes());
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
}
