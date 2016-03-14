import java.io.InvalidObjectException;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.Random;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;
import java.util.Hashtable;

public class MasterThread implements Runnable {
    
    private PriorityBlockingQueue<MPacket> masterOrderQueue = null;
    private BlockingQueue<MPacket>[] masterHoldingList  = null;
    private int sequenceNumber;
    private Hashtable<String, Client> clientTable = null;
    private Comparator<MPacket> comparator = null;
	private PriorityBlockingQueue<MPacket> sequencerHoldbackQueue = null;
	private BlockingQueue outgoingOrderRetransmitQueue = null;
	private double timer;
	private String name;
    private BlockingQueue outgoingRetransmitQueue = null;
    private int[] localSequenceNumbers;

	
    //private PriorityBlockingQueue<MPacket> processingQueue = null;
    
    public MasterThread(PriorityBlockingQueue<MPacket> masterOrderQueue, BlockingQueue<MPacket> masterHoldingList[], Hashtable<String, Client> clientTable, PriorityBlockingQueue<MPacket> sequencerHoldbackQueue, String name, BlockingQueue outgoingOrderRetransmitQueue, BlockingQueue outgoingRetransmitQueue ){
        this.masterOrderQueue = masterOrderQueue;
        this.masterHoldingList = masterHoldingList;
        this.sequenceNumber = 0;
        this.clientTable = clientTable;
		this.sequencerHoldbackQueue = sequencerHoldbackQueue;
		this.name = name;
		this.localSequenceNumbers = new int[masterHoldingList.length];
		for(int i=0; i<masterHoldingList.length; i++){
			this.localSequenceNumbers[i]= 0;
		}
		this.outgoingOrderRetransmitQueue = outgoingOrderRetransmitQueue;
        this.outgoingRetransmitQueue = outgoingRetransmitQueue;
		
        //this.comparator = new PacketComparator();
        //this. processingQueue = new PriorityBlockingQueue<MPacket>(10, comparator);
    }
    
    public void run(){
        MPacket order = null;
        MPacket received = null;
        Client client  = null;
        if(Debug.debug) System.out.println("Starting MasterThread");
        while(true){
			try {
                
                int timeouts = 0, empty = 0, timeout_time = 300;
                this.timer = System.currentTimeMillis();
                while(masterOrderQueue.peek() == null && timeouts < 3) {
                    //System.out.println("MasterThread: waiting...");
                    empty = 0;
                    for(BlockingQueue queue: masterHoldingList) {
                        if(queue.peek() == null) empty++;
                    }
                    //if(empty < masterHoldingList.length) System.out.println("MasterThread: waiting for order packet");
                    if(empty < masterHoldingList.length && System.currentTimeMillis() - this.timer > timeout_time*(timeouts+1)) {
                        System.out.println("MasterThread: NULL: asking for order packet " + this.timer + " " + System.currentTimeMillis());
                        timeouts++;
                        this.timer = System.currentTimeMillis();
                        int myPID = clientTable.get(name).pid;
                        String send = myPID + "," + this.sequenceNumber + "," + this.localSequenceNumbers[myPID];
                        MPacket retransmit = new MPacket(send, 400, 402);
                        outgoingOrderRetransmitQueue.add(retransmit);
                    } else if(empty == masterHoldingList.length){
                        //System.out.println("MasterThread: everthing is empty");
                        this.timer = System.currentTimeMillis();
                    }
                }

            	//wait for next order packet
				this.timer = System.currentTimeMillis();
		        while(masterOrderQueue.peek() != null && masterOrderQueue.peek().sequenceNumber != this.sequenceNumber && timeouts < 3) {
		            //do nothing
					if(masterOrderQueue.peek().type == 300 && masterOrderQueue.peek().sequenceNumber < this.sequenceNumber) masterOrderQueue.poll();
                    //System.out.println("MasterThread: waiting for order packet ");

					if(System.currentTimeMillis() - this.timer > timeout_time*(timeouts+1)) {
                        System.out.println("MasterThread: asking for order packet " + this.timer + " " + System.currentTimeMillis());
						timeouts++;
						this.timer = System.currentTimeMillis();
						int myPID = clientTable.get(name).pid;
						String send = myPID + "," + this.sequenceNumber + "," + this.localSequenceNumbers[myPID];
						MPacket retransmit = new MPacket(send, 400, 402);
						outgoingOrderRetransmitQueue.add(retransmit);
				    }
				}
			
                if(timeouts == 3) System.out.println("MasterThread: FAILURE");
                
		        //process packets with new sequence number
		        while((masterOrderQueue.peek() != null) && (masterOrderQueue.peek().sequenceNumber == this.sequenceNumber)) {
					this.sequenceNumber++;
		        
					order = masterOrderQueue.poll();
		            String[] info = order.name.split(",");
		            
		            int pid = Integer.parseInt(info[0]);
		            int localSeqNum = Integer.parseInt(info[1]);
		            
		            received = masterHoldingList[pid].take();
                    //int timeouts = 0;
                    //this.timer = System.currentTimeMillis();
		            while(received.sequenceNumber != localSeqNum) {
		                masterHoldingList[pid].put(received);
//                        if(System.currentTimeMillis() - this.timer > 300) {
//                            System.out.println("ClientListenerThread: asking for event packet " + this.timer + " " + System.currentTimeMillis());
//                            timeouts++;
//                            this.timer = System.currentTimeMillis();
//                            int myPID = clientTable.get(name).pid;
//                            String send = myPID + "," + pid + "," + this.sequenceNumber;
//                            MPacket retransmit = new MPacket(send, 400, 401);
//                            outgoingRetransmitQueue.add(retransmit);
//                        }
		                received = masterHoldingList[pid].take();
		                
		            }
		            
		            this.localSequenceNumbers[pid] = received.sequenceNumber+1;
		            System.out.println("MasterThread: Received " + received);
		            
		            
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
            }catch(InterruptedException e){
                e.printStackTrace();
                Thread.currentThread().interrupt();    
            }
        }
        
    }
}
