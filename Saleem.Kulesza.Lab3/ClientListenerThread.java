import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Hashtable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;

public class ClientListenerThread implements Runnable {
    
    private Comparator<MPacket> comparator = null; 
    private PriorityBlockingQueue<MPacket> eventQueue = null;
    private MSocket mSocket  =  null;
    private Hashtable<String, Client> clientTable = null;
	private int sequenceNumber;

    public ClientListenerThread( MSocket mSocket,
                                Hashtable<String, Client> clientTable){
	this.comparator = new PacketComparator();
	this.eventQueue = new PriorityBlockingQueue<MPacket>(10, comparator);
	this.sequenceNumber = 0;
        this.mSocket = mSocket;
        this.clientTable = clientTable;
        if(Debug.debug) System.out.println("Instatiating ClientListenerThread");
    }

    public void run() {
        MPacket received = null;
        Client client = null;
        if(Debug.debug) System.out.println("Starting ClientListenerThread");
        while(true){
            try{
System.out.println("reading");               	
		received = (MPacket) this.mSocket.readObject();
System.out.println(received);
		eventQueue.put(received);
	
		while(eventQueue.peek().sequenceNumber != this.sequenceNumber) {
			System.out.println("looking at event queue");
			received = (MPacket) mSocket.readObject();
			eventQueue.put(received);
		}


		while((eventQueue.peek() != null) && (eventQueue.peek().sequenceNumber == this.sequenceNumber)) {
			this.sequenceNumber++;
			received = eventQueue.poll();

			 System.out.println("Received " + received);
			if(received.name.equals("everyone")) {
//			System.out.println("moving bullets");
				for(String client_name : clientTable.keySet()) {
					clientTable.get(client_name).moveProjectile();
					break;
				}
			}
			else {
System.out.println("not moving bullets" + received.name);
				client = clientTable.get(received.name);
				if(received.event == MPacket.UP){
					client.forward();
				}else if(received.event == MPacket.DOWN){
					client.backup();
				}else if(received.event == MPacket.LEFT){
					client.turnLeft();
				}else if(received.event == MPacket.RIGHT){
					client.turnRight();
				}else if(received.event == MPacket.FIRE){
					client.fire();
				}else{
					throw new UnsupportedOperationException();
				}
			}
		}    
		}catch(IOException e){
		    e.printStackTrace();
		}catch(ClassNotFoundException e){
		    e.printStackTrace();
		  	} 
					
		}
		
        }
 }




