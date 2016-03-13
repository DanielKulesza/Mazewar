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
	private BlockingQueue outgoingRetransmitQueue = null;
	private double timer;
	private String name;

	
    //private PriorityBlockingQueue<MPacket> processingQueue = null;
    
    public MasterThread(PriorityBlockingQueue<MPacket> masterOrderQueue, BlockingQueue<MPacket> masterHoldingList[], Hashtable<String, Client> clientTable, PriorityBlockingQueue<MPacket> sequencerHoldbackQueue, String name, BlockingQueue outgoingRetransmitQueue ){
        this.masterOrderQueue = masterOrderQueue;
        this.masterHoldingList = masterHoldingList;
        this.sequenceNumber = 0;
        this.clientTable = clientTable;
		this.sequencerHoldbackQueue = sequencerHoldbackQueue;
		this.name = name;
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
                
                int timeouts = 0, empty = 0;
                this.timer = System.currentTimeMillis();
                while(masterOrderQueue.peek() == null && timeouts < 3) {
                    empty = 0;
                    for(BlockingQueue queue: masterHoldingList) {
                        if(queue.peek() == null) empty++;
                    }
                    if(empty < masterHoldingList.length && System.currentTimeMillis() - this.timer > 300) {
                        System.out.println("asking for order packet " + this.timer + " " + System.currentTimeMillis());
                        timeouts++;
                        this.timer = System.currentTimeMillis();
                        int myPID = clientTable.get(name).pid;
                        String send = myPID + "," + this.sequenceNumber;
                        MPacket retransmit = new MPacket(send, 400, 402);
                        outgoingRetransmitQueue.add(retransmit);
                    }
                }

            	//wait for next order packet
				timeouts = 0;
				this.timer = System.currentTimeMillis();
		        while(masterOrderQueue.peek() != null && masterOrderQueue.peek().sequenceNumber != this.sequenceNumber && timeouts < 3) {
		            //do nothing
					if(masterOrderQueue.peek().type == 300 && masterOrderQueue.peek().sequenceNumber < this.sequenceNumber) masterOrderQueue.poll();
                    System.out.println("waiting for order packet ");

					if(System.currentTimeMillis() - this.timer > 300) {
                        System.out.println("asking for order packet " + this.timer + " " + System.currentTimeMillis());
						timeouts++;
						this.timer = System.currentTimeMillis();
						int myPID = clientTable.get(name).pid;
						String send = myPID + "," + this.sequenceNumber;
						MPacket retransmit = new MPacket(send, 400, 402);
						outgoingRetransmitQueue.add(retransmit);
				    }
				}
			
                if(timeouts == 3) System.out.println("FAILURE");
                
		        //process packets with new sequence number
		        while((masterOrderQueue.peek() != null) && (masterOrderQueue.peek().sequenceNumber == this.sequenceNumber)) {
					this.sequenceNumber++;
		        
					order = masterOrderQueue.poll();
		            String[] info = order.name.split(",");
		            
		            int pid = Integer.parseInt(info[0]);
		            int localSeqNum = Integer.parseInt(info[1]);
		            
		            received = masterHoldingList[pid].take();
		            
		            while(received.sequenceNumber != localSeqNum) {
		                masterHoldingList[pid].put(received);
		                masterHoldingList[pid].take();
		            }
		            
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
            }catch(InterruptedException e){
                e.printStackTrace();
                Thread.currentThread().interrupt();    
            }
        }
        
    }
}
