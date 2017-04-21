import javax.crypto.*;
import javax.crypto.spec.*;
import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServer
{
    ServerSocket server;
    Socket client;
    BufferedReader in;
    PrintWriter out;
    int clientIndex;
	public void begin(int tcp, int clientindex)
	{
		int tcpPort = tcp;
        clientIndex = clientindex;
		byte[] data = null;
		String rec = null;

		//id, port, availability
        ClientObject c = new ClientObject(Main.clientIDs.get(clientIndex), tcp, true);
        Main.clientObjects.put(c.getID(), c);
		try
		{
			server = new java.net.ServerSocket(tcpPort);
			client = server.accept();

			//in will read messages from the client
			in = new java.io.BufferedReader(
					new java.io.InputStreamReader(client.getInputStream()));

			//out will send messages back to client
			out = new java.io.PrintWriter(client.getOutputStream(), true);

			String messageIn = null, messageOut = null;
			String[] tokens;
			
			while(true)
			{
				messageIn = in.readLine();
				
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

					//****
//					System.out.println("TCP disconnecting");
//					client.close();
//					server.close();
//					break;
				}
				else if(tokens[0].equals("CHAT_REQUEST")){
					//messageOut = new String("CHAT_REQUEST ACCEPTED");
                    rec = tokens[1]; //recipient

                    if(checkClient(rec)){
                        //can proceed with contact
                        Main.clientObjects.get(Main.clientIDs.get(clientIndex)).partner = rec;
                        Main.clientObjects.get(rec).partner = Main.clientIDs.get(clientIndex);
                        sendMessage("CHAT_STARTED", rec);
                        messageOut = prepareOutMessage(data, "CHAT_STARTED", clientIndex);
                        out.println(messageOut);
//                        while(true){
//                            //get A's message to B
//                            messageIn = in.readLine();
//                            //decrypt
//                            messageIn = prepareInMessage(data, messageIn, clientIndex);
//                            //System.out.println("received first messsage client");
//                            //sending message from TCPA to TCPB
//                            sendMessage(messageIn, rec);
//                        }
                    }
//					messageOut = prepareOutMessage(data, messageOut, clientIndex);
//					out.println(messageOut);
////					System.out.println("TCP disconnecting under CHAT_REQUEST");
//					client.close();
//					server.close();
//					break;
				}
				else if(tokens[0].equals("CHAT_STATE")){
                    while(true){
                        messageIn = in.readLine();
                        messageIn = prepareInMessage(data, messageIn, clientindex);
                        sendMessage(messageIn, Main.clientObjects.get(Main.clientIDs.get(clientIndex)).partner);
                    }

                }
				else
				{
					messageOut = "ERROR Message not recognized from TCP";
					messageOut = prepareOutMessage(data, messageOut, clientIndex);
					out.println(messageOut);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	boolean checkClient(String id){
        return Main.clientObjects.get(id).isAvailable();
    }

	public void sendMessage(String message, String rec){
        Main.tcpConns.get(rec).giveClientMessage(message);
    }

    public void giveClientMessage(String message){
	    message = prepareOutMessage(null, message, clientIndex);
        System.out.println("SENDING RECEIVED MESSAGE TO CLIENT");
        out.println(message);
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
