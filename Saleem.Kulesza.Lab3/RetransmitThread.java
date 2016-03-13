import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Hashtable;

public class RetransmitThread implements Runnable {


    private MSocket[] socketList = null;
	private BlockingQueue outgoingRetransmitQueue = null;
	private BlockingQueue incomingRetransmitQueue = null;
	private Hashtable<Integer, MSocket> pidtoMSocketMap = null;
	private PriorityBlockingQueue<MPacket> holdbackQueue = null;
	private PriorityBlockingQueue<MPacket> sequencerHoldbackQueue = null;
	private BlockingQueue outgoingOrderRetransmitQueue = null;

    public RetransmitThread(MSocket[] socketList, BlockingQueue outgoingRetransmitQueue, BlockingQueue incomingRetransmitQueue, Hashtable<Integer, MSocket> pidtoMSocketMap, PriorityBlockingQueue<MPacket> holdbackQueue, BlockingQueue outgoingOrderRetransmitQueue, PriorityBlockingQueue<MPacket> sequencerHoldbackQueue){
        this.socketList = socketList;
		this.outgoingRetransmitQueue = outgoingRetransmitQueue;
		this.incomingRetransmitQueue = incomingRetransmitQueue;
		this.pidtoMSocketMap = pidtoMSocketMap;
		this.holdbackQueue = holdbackQueue;
		this.outgoingOrderRetransmitQueue = outgoingOrderRetransmitQueue;
		this.sequencerHoldbackQueue = sequencerHoldbackQueue;
    }
    
    public void run() {
        MPacket toServer = null;
        if(Debug.debug) System.out.println("Starting RetransmitThread");
        while(true){
            try{                
				
				if(outgoingRetransmitQueue.peek() != null) {
					if(Debug.debug) System.out.println("asking for event packet");
					MPacket retransmit = (MPacket) outgoingRetransmitQueue.take();					
					String[] info = retransmit.name.split(",");
					int pid = Integer.parseInt(info[1]);
					MSocket mSocket = pidtoMSocketMap.get(pid);
					mSocket.writeObject(retransmit);
				}
		    	
				if(outgoingOrderRetransmitQueue.peek() != null) {
					if(Debug.debug) System.out.println("asking for order packet");
					MPacket retransmit = (MPacket) outgoingOrderRetransmitQueue.take();
					socketList[0].writeObject(retransmit);
				}

				if(incomingRetransmitQueue.peek() != null) {
					MPacket retransmit = (MPacket) incomingRetransmitQueue.take();
					if(retransmit.event == 401) {
                        if(Debug.debug) System.out.println("retransmitting for event packet");
						String[] info = retransmit.name.split(",");
						int pid = Integer.parseInt(info[0]);
						int seqNum = Integer.parseInt(info[2]);
                        Object[] mPackets = holdbackQueue.toArray();
                        boolean found = false;
                        for(int i = 0; i < mPackets.length; i++) {
                            MPacket packet = (MPacket) mPackets[i];
                            if(Debug.debug) System.out.println(packet);
							if(packet.sequenceNumber == seqNum) {
                                packet.timestamp = System.currentTimeMillis();
								MSocket mSocket = pidtoMSocketMap.get(pid);
								mSocket.writeObject(packet);
								found = true;
								break;
							}
						}
						if(!found)  System.out.println("FAILURE: Empty queue");
					} else {
                        if(Debug.debug) System.out.println("retransmitting for order packet");
						String[] info = retransmit.name.split(",");
						int pid = Integer.parseInt(info[0]);
						int seqNum = Integer.parseInt(info[1]);
						Object[] mPackets = sequencerHoldbackQueue.toArray();
						boolean found = false;
                        for(int i = 0; i < mPackets.length; i++) {
                            MPacket packet = (MPacket) mPackets[i];
                            if(Debug.debug) System.out.println(packet);
							if(packet.sequenceNumber == seqNum) {
                                packet.timestamp = System.currentTimeMillis();
								MSocket mSocket = pidtoMSocketMap.get(pid);
								mSocket.writeObject(packet);
								found = true;
								break;
							}
						}
						if(!found) System.out.println("FAILURE: Empty ueue");
					}
				}

            }catch(InterruptedException e){
                e.printStackTrace();
                Thread.currentThread().interrupt();    
            }
            
        }
    }
}
