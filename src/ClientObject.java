
public class ClientObject {
	public String ID;
	public int port;
	public int sessionID;
	
	ClientObject(String id, int p, int s){
		ID = id;
		port = p;
		sessionID = s;
	}
	
	String getID(){
		return ID;
	}
	
	void setID(String id){
		ID = id;
	}
	
	int getPort(){
		return port;
	}
	
	void setPort(int p){
		port = p;
	}
	
	int getSessionID(){
		return sessionID;
	}
	
	void setSessionID(int s){
		sessionID = s;
	}
}
