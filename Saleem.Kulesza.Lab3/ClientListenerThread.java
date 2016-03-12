import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Hashtable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;

public class ClientListenerThread implements Runnable {
    
    private Comparator<MPacket> comparator = null; 
    private PriorityBlockingQueue<MPacket> eventQueue = null;
    private MSocket mSocket  =  null;
    private Hashtable<String, Client> clientTable = null;
	private int sequenceNumber;
	private boolean self = false;
	private BlockingQueue incomingQueue = null;
	private boolean sequencer;
	private PriorityBlockingQueue<MPacket> masterOrderQueue = null;
	private BlockingQueue<MPacket>[] masterHoldingList = null;
	private int pid = -1;
	private double timer = 0;
	private BlockingQueue outgoingRetransmitQueue = null;
	private BlockingQueue incomingRetransmitQueue = null;
	private String name;

    public ClientListenerThread( String name, MSocket mSocket,
                                Hashtable<String, Client> clientTable, BlockingQueue incomingQueue, boolean sequencer, PriorityBlockingQueue<MPacket> masterOrderQueue, BlockingQueue<MPacket>[] masterHoldingList, BlockingQueue outgoingRetransmitQueue, BlockingQueue incomingRetransmitQueue){
		this.name = name;
        this.comparator = new PacketComparator();
        this.eventQueue = new PriorityBlockingQueue<MPacket>(10, comparator);
        this.mSocket = mSocket;
        this.sequenceNumber = 0;
        this.clientTable = clientTable;
		this.incomingQueue = incomingQueue;
		this.sequencer = sequencer;
		this.masterOrderQueue = masterOrderQueue;
		this.masterHoldingList = masterHoldingList;
		this.outgoingRetransmitQueue = outgoingRetransmitQueue;
		this.incomingRetransmitQueue = incomingRetransmitQueue;
        if(Debug.debug) System.out.println("Instatiating ClientListenerThread");
    }

	public ClientListenerThread( Hashtable<String, Client> clientTable, PriorityBlockingQueue<MPacket> selfEventQueue, BlockingQueue<MPacket> incomingQueue, boolean sequencer, PriorityBlockingQueue<MPacket> masterOrderQueue, BlockingQueue<MPacket>[] masterHoldingList){
        this.comparator = new PacketComparator();
        this.eventQueue = selfEventQueue;
        this.self = true;
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
		orderPacket.sequenceNumber = Integer.parseInt(info[2]); 
		
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
			boolean retransmitting = true;
			while(receivedOrder || retransmitting) {
			
				received = (MPacket) this.mSocket.readObject();

				if(received.type == 300) {
					MPacket mpacket = processOrderPacket(received);					
					masterOrderQueue.add(mpacket);
					receivedOrder = true;
				} else if(received.type == 400) {
					incomingRetransmitQueue.add(received);
					retransmitting = true;
				}	
				else {
					retransmitting = false;
					receivedOrder = false;
					eventQueue.add(received);
				}
			}		

			int timeouts = 0;
			this.timer = System.currentTimeMillis();
			while(eventQueue.peek().sequenceNumber != this.sequenceNumber && timeouts <= 3) {
				System.out.println("waiting for event packet " + this.timer + " " + System.currentTimeMillis());
				if(System.currentTimeMillis() - this.timer > 300) {
					timeouts++;
					this.timer = System.currentTimeMillis();
					int myPID = clientTable.get(name).pid;
					String send = myPID + "," + pid + "," + this.sequenceNumber;
					MPacket retransmit = new MPacket(send, 400, 401);
					outgoingRetransmitQueue.add(retransmit);
				}
				System.out.println("looking at event queue");
				receivedOrder = true;
				retransmitting = true;
				while(receivedOrder || retransmitting) {
			
					received = (MPacket) this.mSocket.readObject();

					if(received.type == 300) {
						MPacket mpacket = processOrderPacket(received);					
						masterOrderQueue.add(mpacket);
						receivedOrder = true;
					} else if(received.type == 400) {
						incomingRetransmitQueue.add(received);
						retransmitting = true;
					}	
					else {
						retransmitting = false;
						receivedOrder = false;
						eventQueue.add(received);
					}
				}
			}

			if(timeouts == 4) System.out.println("FAILURE");
		} else {
			boolean receivedOrder = true;
			while(receivedOrder) {
				try {		
					received = eventQueue.take();

					if(received.type == 300) {
						MPacket mpacket = processOrderPacket(received);					
						masterOrderQueue.add(mpacket);
						receivedOrder = true;
					} else {
						eventQueue.add(received);
						receivedOrder = false;
					}
				}catch(InterruptedException e){
		            e.printStackTrace();
		            Thread.currentThread().interrupt();    
		        }
			}
		}


		while((eventQueue.peek() != null) && (eventQueue.peek().sequenceNumber == this.sequenceNumber)) {
			this.sequenceNumber++;
			received = eventQueue.poll();

			if(sequencer) incomingQueue.add(received);
		
	        //set the pid to waht client we're listening to
			if(pid == -1) pid = clientTable.get(received.name).pid;
	        
			masterHoldingList[pid].add(received);


			System.out.println("holding " + received);
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




