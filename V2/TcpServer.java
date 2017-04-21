import javax.crypto.*;
import javax.crypto.spec.*;
import javax.xml.bind.DatatypeConverter;

public class TcpServer
{	
	static java.util.ArrayList<javax.crypto.spec.SecretKeySpec> encryptKeys; 
	
	public static void main(String[] args)
	{
		int serverPort = Integer.parseInt(args[0]);
		java.net.ServerSocket server = null;
		
		try
		{
			server = new java.net.ServerSocket(serverPort);

			//****
			System.out.println("TCP server running");
			
			java.net.Socket socket = server.accept();
			java.io.ObjectInputStream is = new java.io.ObjectInputStream(socket.getInputStream());
			ClientData dc = (ClientData) is.readObject();
			encryptKeys = dc.encryptKs;
			is.close();
			socket.close();
			 
		} catch (java.io.IOException e)
		{
			System.out.println("Error creating welcoming socket.");
			e.printStackTrace();
			System.exit(-1);
		} catch(ClassNotFoundException e)
		{
			System.out.println("Error with reading object from stream");
			System.exit(-2);
		}
		
		while(true)
		{
			TcpServerWorker w;
			try
			{
				w = new TcpServerWorker(server.accept());
				Thread t = new Thread(w);
				t.start();
			}
			catch(java.io.IOException e)
			{
				System.out.println("Accept failed");
				e.printStackTrace();
				System.exit(-3);
			}
		}
	}
}

class TcpServerWorker implements Runnable 
{
	private java.net.Socket client;
	
	TcpServerWorker(java.net.Socket client)
	{
		this.client = client;
	}

	@Override
	public void run()
	{
		byte[] data = null;
		int clientIndex = -5;
        
		try
		{
			//in will read messages from the client
			java.io.BufferedReader in = new java.io.BufferedReader(
					new java.io.InputStreamReader(client.getInputStream()));

			//out will send messages back to client
			java.io.PrintWriter out = new java.io.PrintWriter(client.getOutputStream(), true);

			String messageIn = null, messageOut = null;
			String[] tokens, parsedMessage;
			
			messageIn = in.readLine();
			clientIndex = Integer.parseInt(messageIn);

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
					messageOut = new String("CONNECTED");
					messageOut = prepareOutMessage(data, messageOut, clientIndex);
					out.println(messageOut);

					//****
					System.out.println("TCP disconnecting");
					client.close();
					break;
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
		messageIn = decrypt(data, TcpServer.encryptKeys.get(clientIndex));
		return messageIn;
	}

	//Used for TCP encryption
	public String prepareOutMessage(byte[] data, String messageOut, int clientIndex)
	{
		data = encrypt(messageOut, TcpServer.encryptKeys.get(clientIndex));
		messageOut = DatatypeConverter.printBase64Binary(data);
		return messageOut;
	}
}
