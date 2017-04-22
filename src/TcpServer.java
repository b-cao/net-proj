import javax.crypto.*;
import javax.crypto.spec.*;
import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class TcpServer {
    ServerSocket server;
    Socket client;
    BufferedReader in;
    PrintWriter out;
    int clientIndex;
    static int session = 0;
    //String is other client, the map is all sessions of chats we have had, identified by sessionID
    //HashMap<String, HashMap<Integer, chatSession>> chatHistory = new HashMap<>();

    //Change boolean available whenever user logs off
    //check if object exists before creating in TCPserver
    HashMap<String, HashMap<Integer, chatSession>> chatHistory = new HashMap<>();

    public void begin(int tcp, int clientindex) {
        int tcpPort = tcp;
        clientIndex = clientindex;
        String clientID;
        byte[] data = null;
        String rec = null;
        int sessionID;

        //id, port, availability
        clientID = Main.clientIDs.get(clientIndex);
        //check if object exists, if not create a new one
        if(!Main.clientObjects.containsKey(clientID)){
            ClientObject c = new ClientObject(clientID, tcp, true);
            Main.clientObjects.put(c.getID(), c);
            //clientID = Main.clientIDs.get(clientIndex);
        }

        try {
            server = new java.net.ServerSocket(tcpPort);
            client = server.accept();

            //in will read messages from the client
            in = new java.io.BufferedReader(
                    new java.io.InputStreamReader(client.getInputStream()));

            //out will send messages back to client
            out = new java.io.PrintWriter(client.getOutputStream(), true);

            String messageIn = null, messageOut = null;
            String[] tokens;

            while (true) {
                messageIn = in.readLine();

                //****
                System.out.println("Received encrypted message " + messageIn);

                messageIn = prepareInMessage(data, messageIn, clientIndex);

                //****
                System.out.println("Received decrypted message " + messageIn);

                tokens = messageIn.split(" ");

                if (tokens[0].equals("CONNECT")) {
                    //			Main.cookies.set(clientIndex, Integer.parseInt(tokens[1]));
                    messageOut = new String("CONNECTED");
                    messageOut = prepareOutMessage(data, messageOut, clientIndex);
                    out.println(messageOut);

                } else if (tokens[0].equals("CHAT_REQUEST")) {
                    rec = tokens[1]; //recipient

                    if (checkClient(rec)) {
                        //can proceed with contact
                        Main.clientObjects.get(clientID).partner = rec;
                        Main.clientObjects.get(rec).partner = clientID;
                        //set up chatSession object
                        Main.clientObjects.get(clientID).sessionID = session;
                        Main.clientObjects.get(rec).sessionID = session;
                        // when increment?

                        //chatSession currChat = new chatSession(session, clientID, rec);

                        sendMessage("CHAT_STARTED ", rec, clientID);
                        messageOut = prepareOutMessage(data, "CHAT_STARTED ", clientIndex);
                        out.println(messageOut);
                        session++;
                    } else {
                        messageOut = "UNREACHABLE";
                        messageOut = prepareOutMessage(data, messageOut, clientIndex);
                        out.println(messageOut);
                    }
                } else if (tokens[0].equals("CHAT_STATE")) {
                    Main.clientObjects.get(clientID).available = false;
                    String otherClient = Main.clientObjects.get(clientID).partner;
                    if (chatHistory.containsKey(otherClient)) {
                        chatHistory.get(otherClient).put(session, new chatSession(Main.clientObjects.get(clientID).sessionID, clientID, otherClient));
                    }
                    else{
                        chatHistory.put(otherClient, new HashMap<>());
                        chatHistory.get(otherClient).put(Main.clientObjects.get(clientID).sessionID,
                                new chatSession(Main.clientObjects.get(clientID).sessionID, clientID, otherClient));
                    }
                    //begin chatSession saving
                    //chatSession currChat = new chatSession(Main.clientObjects.get(clientID).sessionID, clientID, Main.clientObjects.get(clientID).partner);
                    while(true) {
                        messageIn = in.readLine();
                        messageIn = prepareInMessage(data, messageIn, clientindex);
                        tokens = messageIn.split(" ");
                        if(tokens[0].equals("END_REQUEST")){
                            //time to stop chat
                            //need to send message to other client
                            System.out.println("CLIENT " + clientID + "WANTS TO END");
                            sendMessage("END_NOTIF", otherClient, clientID);
                            Main.clientObjects.get(clientID).available = true;
                            break;
                        }
                        else if(tokens[0].equals("END_NOTIF")){
                            //we have been disconnected
                            System.out.println("OTHER CLIENT ENDED CHAT");
                            Main.clientObjects.get(clientID).available = true;
                            break;
                        }
                        //what we are sending
                        //if we have entry for other client
                        if(chatHistory.containsKey(otherClient)){
                            //if we have entry in hashmap for other client message sessions
                            if(chatHistory.get(otherClient).containsKey(Main.clientObjects.get(clientID).sessionID)){
                                chatHistory.get(otherClient).get(Main.clientObjects.get(clientID).sessionID).addMessage(messageIn, clientID);
                            }
                            else{
                                chatHistory.get(otherClient).put(Main.clientObjects.get(clientID).sessionID,
                                        new chatSession(Main.clientObjects.get(clientID).sessionID, clientID, otherClient));
                                chatHistory.get(otherClient).get(Main.clientObjects.get(clientID).sessionID).addMessage(messageIn, clientID);
                            }
                        }else{
                            chatHistory.put(otherClient, new HashMap<Integer, chatSession>());
                            chatHistory.get(otherClient).put(Main.clientObjects.get(clientID).sessionID,
                                    new chatSession(Main.clientObjects.get(clientID).sessionID, clientID, otherClient));
                            chatHistory.get(otherClient).get(Main.clientObjects.get(clientID).sessionID).addMessage(messageIn, clientID);
                        }
                        //chatHistory.get(otherClient).get(Main.clientObjects.get(clientID).sessionID).addMessage(messageIn,clientID);
                        sendMessage(messageIn, Main.clientObjects.get(Main.clientIDs.get(clientIndex)).partner, clientID);
                    }


                }
                else if (tokens[0].equals("LOG_OFF")) {
                    System.out.println("CLIENT ON PORT " + tcp + " IS DISCONNECTING");
                    Main.clientObjects.get(clientID).available = false;
                    client.close();
                    server.close();
                }
                else if (tokens[0].equals("HISTORY_REQ")) {
                    //request history
                    String user = tokens[1];
                    //need to get chatSession, and relay back infomation
                    //how to store chatsession?
                    HashMap<Integer, chatSession> list = chatHistory.get(user);
                    Iterator it = list.entrySet().iterator();
                    //for ()
                    for(chatSession val: list.values()){
                        ArrayList<String> msgs = val.messages;
                        for(String x: msgs){
                            messageOut = x;
                            messageOut = prepareOutMessage(null, messageOut, clientIndex);
                            out.println(messageOut);
                        }
                    }
                    messageOut = "END";
                    messageOut = prepareOutMessage(null, messageOut, clientIndex);
                    out.println(messageOut);
                } else {
                    messageOut = "ERROR Message not recognized from TCP";
                    messageOut = prepareOutMessage(data, messageOut, clientIndex);
                    out.println(messageOut);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean checkClient(String id) {

        if (Main.clientObjects.containsKey(id)) {
            return Main.clientObjects.get(id).isAvailable();
        }
        return false;
    }

    public void sendMessage(String message, String rec, String src) {
        Main.tcpConns.get(rec).giveClientMessage(message, src);
    }

    public void giveClientMessage(String message, String src) {
        String clientID = Main.clientIDs.get(clientIndex);
        String rec = src;
        int sess = Main.clientObjects.get(clientID).sessionID;
        if(message.equals("END_NOTIF")){
            //end
            message = prepareOutMessage(null, message, clientIndex);
            out.println(message);
            return;
        }
        if(message.equals("CHAT_STARTED ")){
            message = prepareOutMessage(null, message, clientIndex);
            out.println(message);
            return;
        }
        if(chatHistory.containsKey(rec)){
            if(chatHistory.get(rec).containsKey(sess)){
                chatHistory.get(rec).get(sess).addMessage(message, src);
            }
            else{
                //contains client, but not this specific session id thing
                chatHistory.get(rec).put(sess, new chatSession(sess, clientID, rec));
                chatHistory.get(rec).get(sess).addMessage(message, src);
            }

        }
        else{
            chatHistory.put(src, new HashMap<Integer, chatSession>());
            chatHistory.get(src).put(sess, new chatSession(sess, clientID, rec));

        }

            //chatHistory.get(src).get(Main.clientObjects.get(Main.clientIDs.get(clientIndex))).addMessage(message, src);
            message = prepareOutMessage(null, message, clientIndex);
            System.out.println("SENDING RECEIVED MESSAGE TO CLIENT");
            out.println(message);

    }

    //AES encrypt
    public byte[] encrypt(String message, SecretKeySpec secretKeySpec) {
        byte[] encrypted = null;

        try {
            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            encrypted = aesCipher.doFinal(message.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return encrypted;
    }

    //AES decrypt
    public String decrypt(byte[] encrypted, SecretKeySpec secretKeySpec) {
        byte[] decrypted = null;

        try {
            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            decrypted = aesCipher.doFinal(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String(decrypted);
    }

    //Used for TCP decryption
    public String prepareInMessage(byte[] data, String messageIn, int clientIndex) {
        data = DatatypeConverter.parseBase64Binary(messageIn);
        messageIn = decrypt(data, Main.encryptKeys.get(clientIndex));
        return messageIn;
    }

    //Used for TCP encryption
    public String prepareOutMessage(byte[] data, String messageOut, int clientIndex) {
        data = encrypt(messageOut, Main.encryptKeys.get(clientIndex));
        messageOut = DatatypeConverter.printBase64Binary(data);
        return messageOut;
    }
}
