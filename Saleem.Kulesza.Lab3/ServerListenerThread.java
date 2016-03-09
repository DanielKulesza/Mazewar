import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;
import java.io.BufferedReader;
import java.net.Socket;
import java.io.ObjectInputStream;


public class ServerListenerThread implements Runnable {

    private Socket socket =  null;
    private BlockingQueue eventQueue = null;
    private Comparator<MPacket> comparator = null; 
    private PriorityBlockingQueue<MPacket> incomingEventQueue = null;
    private int sequenceNumber = 0;

    public ServerListenerThread( Socket socket, BlockingQueue eventQueue){
        this.socket = socket;
        this.eventQueue = eventQueue;
	this.comparator = new PacketComparator();
	this.incomingEventQueue = new PriorityBlockingQueue<MPacket>(10, comparator);

    } 

    public void run() {
        MPacket received = null;
        if(Debug.debug) System.out.println("Starting a listener");		
		try{

		ObjectInputStream in = new ObjectInputStream(this.socket.getInputStream());
        
		while(true){
            
System.out.println("listening");
		
                received = (MPacket) in.readObject();
                if(Debug.debug) System.out.println("Received: " + received);
		
		eventQueue.put(received);
	
		//System.out.println("QUEUE PEEK: " + incomingEventQueue.peek());
//		while(incomingEventQueue.peek().sequenceNumber != this.sequenceNumber) {
//			received = (MPacket) in.readObject();
			//System.out.println("SEQ NUM : " + received.sequenceNumber);
			//System.out.println("Listener SEQ NUM :" + this.sequenceNumber);
//			incomingEventQueue.put(received);
//		}

//		while((incomingEventQueue.peek() != null) && (incomingEventQueue.peek().sequenceNumber == this.sequenceNumber)) {
//			this.sequenceNumber++;
//			received = incomingEventQueue.poll();                
//			eventQueue.put(received);   
//		} 
		}

        }catch(InterruptedException e){
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }catch(ClassNotFoundException e){
            e.printStackTrace();
        }
    }
}
