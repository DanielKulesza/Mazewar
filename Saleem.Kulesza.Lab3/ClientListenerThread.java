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
	private boolean self = false;
	private BlockingQueue<MPacket> incomingQueue = null;
	private boolean sequencer = null;
	private PriorityBlockingQueue<MPacket> masterOrderQueue = null;
	private BlockingQueue<MPacket> masterHoldingList = null;
	private int pid = -1;

    public ClientListenerThread( MSocket mSocket,
                                Hashtable<String, Client> clientTable, PriorityBlockingQueue<MPacket> selfEventQueue, incomingQueue, sequencer, PriorityBlockingQueue<MPacket> masterOrderQueue, BlockingQueue<MPacket> masterHoldingList[]){
        this.comparator = new PacketComparator();
        if(selfEventQueue == null) {
            this.eventQueue = new PriorityBlockingQueue<MPacket>(10, comparator);
            this.mSocket = mSocket;
        } else {
            this.eventQueue = selfEventQueue;
            self = true;
        }
        this.sequenceNumber = 0;
        this.clientTable = clientTable;
		this.incomingQueue = incomingQueue;
		this.sequencer = sequencer;
		this.masterOrderQueue = masterOrderQueue;
		this.masterHoldingList = masterHoldingList;
        if(Debug.debug) System.out.println("Instatiating ClientListenerThread");
    }

	MPacket processOrderPacket(MPacket orderPacket) {
		String name = orderPacket.name;
		String[] info = name.split(",");
		orderPacket.sequenceNumber = info[2].parseInt(); 
		
		return orderPacket;
	}

    public void run() {
        MPacket received = null;
        Client client = null;
        if(Debug.debug) System.out.println("Starting ClientListenerThread");
        while(true){
            try{
               	
		if(!self) {
			boolean receivedOrder = true;
			while(receivedOrder) {			
				received = (MPacket) this.mSocket.readObject();

				if(received.type == 300) {
					MPacket mpacket = processOrderPacket(received);					
					masterOrderQueue.add(mpacket);
					receivedOrder = true;
				}
			}

	 		eventQueue.put(received);

			while(eventQueue.peek().sequenceNumber != this.sequenceNumber) {
				System.out.println("looking at event queue");
				boolean receivedOrder = true;
				while(receivedOrder) {			
					received = (MPacket) this.mSocket.readObject();

					if(received.type == 300) {
					 	MPacket mpacket = processOrderPacket(received);					
						masterOrderQueue.add(mpacket);
						receivedOrder = true;
					}
				}

				eventQueue.put(received);
			}
		}


		while((eventQueue.peek() != null) && (eventQueue.peek().sequenceNumber == this.sequenceNumber)) {
			this.sequenceNumber++;
			received = eventQueue.poll();
			
			if(sequencer) incomingQueue.add(received);
			
            //set the pid to waht client we're listening to
			if(pid == -1) pid = clientTable.find(received.name).pid;
            
			masterHoldingList[pid].add(received);

//			System.out.println("Received " + received);
//			
//			
//			if(received.name.equals("everyone")) {
////			System.out.println("moving bullets");
//				for(String client_name : clientTable.keySet()) {
//					clientTable.get(client_name).moveProjectile();
//					break;
//				}
//			}
//			else {
//System.out.println("not moving bullets" + received.name);
//				client = clientTable.get(received.name);
//				if(received.event == MPacket.UP){
//					client.forward();
//				}else if(received.event == MPacket.DOWN){
//					client.backup();
//				}else if(received.event == MPacket.LEFT){
//					client.turnLeft();
//				}else if(received.event == MPacket.RIGHT){
//					client.turnRight();
//				}else if(received.event == MPacket.FIRE){
//					client.fire();
//				}else{
//					throw new UnsupportedOperationException();
//				}
//			}
		}
		}catch(IOException e){
		    e.printStackTrace();
		}catch(ClassNotFoundException e){
		    e.printStackTrace();
		  	} 
					
		}
		
        }
 }




