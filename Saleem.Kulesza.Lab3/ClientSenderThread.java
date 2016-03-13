import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Hashtable;

public class ClientSenderThread implements Runnable {


    private MSocket[] socketList = null;
    private BlockingQueue<MPacket> eventQueue = null;
    private int sequenceNumber = 0; //Start at 1 for hello packets
    private PriorityBlockingQueue<MPacket> selfEventQueue = null;
	private BlockingQueue outgoingRetransmitQueue = null;
	private BlockingQueue incomingRetransmitQueue = null;
	private Hashtable<Integer, MSocket> pidtoMSocketMap = null;
	private PriorityBlockingQueue<MPacket> holdbackQueue = null;
	private PriorityBlockingQueue<MPacket> sequencerHoldbackQueue = null;
	private BlockingQueue outgoingOrderRetransmitQueue = null;
	private BlockingQueue incomingOrderRetransmitQueue = null;

    public ClientSenderThread(MSocket[] socketList,
                              BlockingQueue eventQueue, PriorityBlockingQueue<MPacket> selfEventQueue, BlockingQueue outgoingRetransmitQueue, BlockingQueue incomingRetransmitQueue, Hashtable<Integer, MSocket> pidtoMSocketMap, PriorityBlockingQueue<MPacket> holdbackQueue, BlockingQueue outgoingOrderRetransmitQueue, PriorityBlockingQueue<MPacket> sequencerHoldbackQueue){
        this.socketList = socketList;
        this.eventQueue = eventQueue;
		this.selfEventQueue = selfEventQueue;		
		this.outgoingRetransmitQueue = outgoingRetransmitQueue;
		this.incomingRetransmitQueue = incomingRetransmitQueue;
		this.pidtoMSocketMap = pidtoMSocketMap;
		this.holdbackQueue = holdbackQueue;
		this.outgoingOrderRetransmitQueue = outgoingOrderRetransmitQueue;
		this.sequencerHoldbackQueue = sequencerHoldbackQueue;
    }
    
    public void run() {
        MPacket toServer = null;
        if(Debug.debug) System.out.println("Starting ClientSenderThread");
        while(true){
            try{                
                //Take packet from queue
                toServer = (MPacket)eventQueue.take();
				if(toServer.type != 300) toServer.sequenceNumber = this.sequenceNumber;
                if(Debug.debug) System.out.println("Sending " + toServer);
                for(MSocket mSocket: this.socketList) {
					mSocket.writeObject(toServer);
				}
				selfEventQueue.add(toServer);
				toServer.timestamp = System.currentTimeMillis();

				if(toServer.type != 300) {
					this.sequenceNumber++;
					holdbackQueue.add(toServer);
				} else {
					sequencerHoldbackQueue.add(toServer);
				}
//				
//				System.out.println("check fo retransmits");
//				if(outgoingRetransmitQueue.peek() != null) {
//					System.out.println("retransmitting for event packet");
//					MPacket retransmit = (MPacket) outgoingRetransmitQueue.take();					
//					String[] info = retransmit.name.split(",");
//					int pid = Integer.parseInt(info[1]);
//					MSocket mSocket = pidtoMSocketMap.get(pid);
//					mSocket.writeObject(retransmit);
//				}
//		    	
//				if(outgoingOrderRetransmitQueue.peek() != null) {
//					System.out.println("retransmitting for order packet");
//					MPacket retransmit = (MPacket) outgoingOrderRetransmitQueue.take();
//					socketList[0].writeObject(retransmit);
//				}
//
//				if(incomingRetransmitQueue.peek() != null) {
//					MPacket retransmit = (MPacket) incomingRetransmitQueue.take();
//					if(retransmit.event == 401) {					
//						String[] info = retransmit.name.split(",");
//						int pid = Integer.parseInt(info[0]);
//						int seqNum = Integer.parseInt(info[2]);
//						MPacket[] mPackets = (MPacket[]) holdbackQueue.toArray();
//						boolean found = false;
//						for(MPacket packet: mPackets) {
//							if(packet.sequenceNumber == seqNum) {
//								MSocket mSocket = pidtoMSocketMap.get(pid);
//								mSocket.writeObject(packet);
//								found = true;
//								break;
//							}
//						}
//						if(!found)  System.out.println("FAILURE");
//					} else {
//						String[] info = retransmit.name.split(",");
//						int pid = Integer.parseInt(info[0]);
//						int seqNum = Integer.parseInt(info[1]);
//						MPacket[] mPackets = (MPacket[]) sequencerHoldbackQueue.toArray();
//						boolean found = false;
//						for(MPacket packet: mPackets) {
//							if(packet.sequenceNumber == seqNum) {
//								MSocket mSocket = pidtoMSocketMap.get(pid);
//								mSocket.writeObject(packet);
//								found = true;
//								break;
//							}
//						}
//						if(!found) System.out.println("FAILURE");
//					}
//				}

            }catch(InterruptedException e){
                e.printStackTrace();
                Thread.currentThread().interrupt();    
            }
            
        }
    }
}
