import javax.crypto.*;
import javax.crypto.spec.*;
import javax.xml.bind.DatatypeConverter;

import java.io.ObjectInputStream;
import java.util.ArrayList;

public class TcpServer
{
	public void begin(int tcp, int clientindex)
	{
		int tcpPort = tcp, clientIndex = clientindex;
		byte[] data = null;
		
		try
		{
			java.net.ServerSocket server = new java.net.ServerSocket(tcpPort);
			java.net.Socket clientSocket = server.accept();

			//in will read messages from the client
			java.io.BufferedReader in = new java.io.BufferedReader(
					new java.io.InputStreamReader(clientSocket.getInputStream()));

			//out will send messages back to client
			java.io.PrintWriter out = new java.io.PrintWriter(clientSocket.getOutputStream(), true);

			String messageIn = null, messageOut = null, requestedUser;
			String[] tokens;
			
			while(true)
			{
				messageIn = in.readLine();
				if(messageIn!= null){
					//****
					System.out.println("Received encrypted message " + messageIn);

					messageIn = prepareInMessage(data, messageIn, clientIndex);

					//****
					System.out.println("Received decrypted message " + messageIn);
                                        
					tokens = messageIn.split(" ");

					if(tokens[0].equals("CONNECT"))
					{
						//			Main.cookies.set(clientIndex, Integer.parseInt(tokens[1]));
						messageOut = new String("CONNECTED");
						messageOut = prepareOutMessage(data, messageOut, clientIndex);
						out.println(messageOut);
						
						//Quick and dirty
						ClientObject client = new ClientObject(Main.clientIDs.get(clientIndex),tcp,0);
						Main.clients.add(client);
						
						while(true){
							messageIn = in.readLine();
							if(messageIn!= null){
								System.out.println("Received encrypted message " + messageIn);

								messageIn = prepareInMessage(data, messageIn, clientIndex);

								//****
								System.out.println("Received decrypted message " + messageIn);
								tokens = messageIn.split(" ");

								//reqIDport == -1 means the user is not available
								if(tokens[0].equals("CHAT_REQUEST")){
									int reqIDPort = Main.TCPChatRequest(tokens[1]);
									System.out.println(reqIDPort);
									
									//If a port number was returned, sets up client communication. NOT WORKING!!!!
									if(reqIDPort != -1){
										java.net.ServerSocket sender = new java.net.ServerSocket(5555);
										java.net.Socket receiver = sender.accept();

										//in will read messages from the client
										java.io.BufferedReader in1 = new java.io.BufferedReader(
												new java.io.InputStreamReader(receiver.getInputStream()));

										//out will send messages back to client
										java.io.PrintWriter out2 = new java.io.PrintWriter(receiver.getOutputStream(), true);
										
										
										messageOut = new String("CHAT_STARTED");
										messageOut = prepareOutMessage(data, messageOut, clientIndex);
										out2.println(messageOut);
									}
									if (reqIDPort == -1){
										//messageOut = new String("UNREACHABLE " + tokens[1]);
										//messageOut = prepareOutMessage(data, messageOut, clientIndex);
									}
								}
							}
						}

						//****
						//	System.out.println("TCP disconnecting");
						//	clientSocket.close();
						//		server.close();
						//			break;
					}
					else
					{
						messageOut = "ERROR Message not recognized from TCP";
						messageOut = prepareOutMessage(data, messageOut, clientIndex);
						out.println(messageOut);
					}
                                        
                                        //recives client request to chat with different client
                                        System.out.println("Receiving request to chat...");
                                        requestedUser = in.readLine();
                                        //System.out.println("The user requested is: "+requestedUser);
                                        
                                        /*for(int u = 0; u < threads.length; u++)
                                        {
                                            if()
                                            {
                                                isBusy[clientIndex] = true;
                                            }
                                        }*/
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	//AES encrypt
	public byte[] encrypt(String message, SecretKeySpec secretKeySpec)
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
	
	//Used for TCP decryption
	public String prepareInMessage(byte[] data, String messageIn, int clientIndex)
	{
		System.out.println("messageIn : " +messageIn);
		data = DatatypeConverter.parseBase64Binary(messageIn);
		messageIn = decrypt(data, Main.encryptKeys.get(clientIndex));
		return messageIn;
	}
	
	//Used for TCP encryption
	public String prepareOutMessage(byte[] data, String messageOut, int clientIndex)
	{
		data = encrypt(messageOut, Main.encryptKeys.get(clientIndex));
		messageOut = DatatypeConverter.printBase64Binary(data);
		return messageOut;
	}
}