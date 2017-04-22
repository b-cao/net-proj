import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by danirubio on 4/21/17.
 */
//object to store clients communicating, sessionID, and maintain chat history
public class chatSession {
    int sessionID;
    ArrayList<String> messages = new ArrayList<>();
    String clientA;
    String clientB;

    chatSession(int s, String a, String b){
        this.sessionID = s;
        this.clientA = a;
        this.clientB = b;
    }

    public ArrayList<String> getMessages() {
        return messages;
    }

    /*
    <session_id> <from: sending client> <chat message>
     */
    public void addMessage(String msg, String src){
        String entry = sessionID + " from: " +  src + " " + msg;
        messages.add(entry);
    }
}
