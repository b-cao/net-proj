
public class ClientObject {
    public String ID;
    public int port;
    public boolean available;
    public String partner;
    public int sessionID;

    ClientObject(String id, int p, boolean a){
        ID = id;
        port = p;
        this.available = a;
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

    public boolean isAvailable() {
        return available;
    }


    //    int getSessionID(){
//        return sessionID;
//    }
//
//    void setSessionID(int s){
//        sessionID = s;
//    }
}