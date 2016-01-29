import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;

public class ServerListenerThread implements Runnable {

    private MSocket mSocket =  null;
    private BlockingQueue eventQueue = null;
    private Comparator<MPacket> comparator = null; 
    private PriorityBlockingQueue<MPacket> incomingEventQueue = null;
    private int sequenceNumber = 0;

    public ServerListenerThread( MSocket mSocket, BlockingQueue eventQueue){
        this.mSocket = mSocket;
        this.eventQueue = eventQueue;
	this.comparator = new PacketComparator();
	this.incomingEventQueue = new PriorityBlockingQueue<MPacket>(10, comparator);
    } 

    public void run() {
        MPacket received = null;
        if(Debug.debug) System.out.println("Starting a listener");
        while(true){
            try{
                received = (MPacket) mSocket.readObject();
                if(Debug.debug) System.out.println("Received: " + received);
		
		incomingEventQueue.put(received);
	
		System.out.println("QUEUE PEEK: " + incomingEventQueue.peek());
		while(incomingEventQueue.peek().sequenceNumber != this.sequenceNumber) {
			received = (MPacket) mSocket.readObject();
			System.out.println("SEQ NUM : " + received.sequenceNumber);
			System.out.println("Listener SEQ NUM :" + this.sequenceNumber);
			incomingEventQueue.put(received);
		}

		while((incomingEventQueue.peek() != null) && (incomingEventQueue.peek().sequenceNumber == this.sequenceNumber)) {
			this.sequenceNumber++;
			received = incomingEventQueue.poll();                
			eventQueue.put(received);   
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
}
