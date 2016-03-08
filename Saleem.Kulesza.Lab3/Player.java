import java.io.Serializable;

public class Player implements Serializable {
    //Need these because direction is not serializable
    public final static int North = 0;
    public final static int South = 1;
    public final static int East  = 2;
    public final static int West  = 3;
    
    public Point point = null;
    public int direction;
    public String name;
    public int port;
    public String IPaddress;
    public int pid;
    
    public Player(String name, Point point, int direction){
        this.point = point;
        this.name = name;
        this.direction = direction;
    }
    public String toString(){
    	return "[" + name + ": (" + point.getX() + "," + point.getY() + ")]"; 
    }
    


    public void setIP(String IPAddress){
	this.IPaddress = IPAddress;
    }
    
    public void setPort(int Port){
	this.port = Port;
    }

}
