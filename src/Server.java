/*
 * CS 4390
 */

import java.net.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class Server
{
	private int tcpPort = -22;
	
	public void begin(int p, InetAddress IP, int client)
	{
		byte[] sendMessage = new byte[1024], receiveMessage = new byte[1024];
		int serverPort = MainWorker.udpPortNumbers, tcpPort = -33, clientPort = p, clientIndex = client;
		//use current value and then increment the udp port for next use
		MainWorker.udpPortNumbers++;
		String[] tokens;
		java.util.Random rng = new java.util.Random();
		InetAddress IPAddress = IP;
		DatagramPacket sendPacket, receivePacket;
		//using MD for authentication between client and server and encryption
		MessageDigest md;
		Integer rand, randCookie;
		byte[] data;
		
		try
		{
			DatagramSocket serverSocket = new DatagramSocket(serverPort);
			
			//****
			System.out.println("inside udp server, udp port is " + serverPort + ", clientport is " 
					+ clientPort + ", clientindex is " + clientIndex);
			
			//send a message back to client saying to use this port now
			sendMessage = new String("port " + serverPort).getBytes();
			sendPacket = new DatagramPacket(sendMessage, sendMessage.length, IPAddress, clientPort);
			serverSocket.send(sendPacket);
			
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
				
				if(tokens[0].equals("HELLO"))
				{
					String challenge, xres;
					//MD5 hash for authentications
					md = MessageDigest.getInstance("MD5");
					
					//this means that the client is a subscriber in the arraylist
					if(clientIndex > -1)
					{
						//generate random integers for each CHALLENGE
						rand = new Integer(rng.nextInt(100000));
						//STILL do not know what rand cookie is for *cries*
						randCookie = new Integer(rng.nextInt(100000));
						Main.randoms.set(clientIndex, new Integer[]{rand, randCookie});
						
						//server uses the rand value above and concats with the client's secret key as a string
						//string goes through MD5 hash and stores it
						//server will wait until the client sends back a hash value and then the server will compare
						//	both hashes to see if they match
						challenge = new String(Main.randoms.get(clientIndex)[0].toString() + Main.secretKeys.get(clientIndex));
						md.update(challenge.getBytes(), 0, challenge.length());
						xres = new java.math.BigInteger(1, md.digest()).toString(16);
						
						Main.authenHashes.set(clientIndex, xres);
			
						//issue a challenge protocol to client, sending a rand value
						String protocol = "CHALLENGE " + Main.randoms.get(clientIndex)[0].toString();
						sendMessage = new String(protocol).getBytes();
						sendPacket = new DatagramPacket(sendMessage, sendMessage.length, IPAddress, clientPort);
						serverSocket.send(sendPacket);
						
					}
					
					//client is not in the arraylist; not a subscriber
					else
					{
						sendMessage = new String("SUBSCRIBER_FAIL").getBytes();
						sendPacket = new DatagramPacket(sendMessage, sendMessage.length, IPAddress, clientPort);
						serverSocket.send(sendPacket);
					}
				}
				
				else if(tokens[0].equals("RESPONSE"))
				{
					//compares the hash value the client has sent back with the hash value
					//	that the server previously computed and stored
					if(!tokens[1].equals(Main.authenHashes.get(clientIndex)))
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
						
						byte[] key = (Main.randoms.get(clientIndex)[0].toString() + Main.secretKeys.get(clientIndex)).getBytes();
						md = MessageDigest.getInstance("SHA-1");
						//returns a byte array hash
						key = md.digest(key);
						//truncates the hash to 16 bytes (128 bits)
						key = Arrays.copyOf(key, 16);
						
						//create the encryption key (CK_A) using the hash from our rand and secret key
						SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
						Main.encryptKeys.set(clientIndex, secretKeySpec);
						
						//NOTE: may not be needed
						MainWorker.isTcpRunning[clientIndex] = true;
						
						//assigns a tcp port from the tcp port range we provided
						tcpPort = MainWorker.tcpPortNumbers;
						//increment it so that the next TCP can have its own port
						MainWorker.tcpPortNumbers++;
						setTcpPort(tcpPort);
						String message = "AUTH_SUCCESS " + Main.randoms.get(clientIndex)[1] + " " + tcpPort;
						sendMessage = encrypt(message, secretKeySpec);
						sendPacket = new DatagramPacket(sendMessage, sendMessage.length, IPAddress, clientPort);
						serverSocket.send(sendPacket);
						
						serverSocket.close();
						break;
					}
				}
				
				//something unexpected went wrong
				//e.g. unrecognized text
				else
				{
					//decrypt the message, and then create more conditionals for other protocols
					//have a final else state in here for the unrecognized text
					//	for unrecog. text, output that there is an unexpected error, and then system.exit(-2)
					
					sendMessage = new String("ERROR UDP server received unrecognized protocol").getBytes();
					sendPacket = new DatagramPacket(sendMessage, sendMessage.length, IPAddress, clientPort);
					serverSocket.send(sendPacket);
					serverSocket.close();
				}
			}
		}
		catch (java.io.IOException|java.security.NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
	}
	
	public void setTcpPort(int tcp)
	{
		this.tcpPort = tcp;
	}
	
	public int getTcpPort()
	{
		return tcpPort;
	}
	
	//AES encrypt
	public byte[] encrypt(String message, SecretKeySpec secretKeySpec)
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
	
	//AES decrypt
	public String decrypt(byte[] encrypted, SecretKeySpec secretKeySpec)
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