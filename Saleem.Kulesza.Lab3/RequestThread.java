import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Hashtable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;

public class RequestThread implements Runnable {
    
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
    
    public RequestThread( String name, MSocket mSocket,
                                Hashtable<String, Client> clientTable, BlockingQueue incomingQueue, boolean sequencer, PriorityBlockingQueue<MPacket> masterOrderQueue, BlockingQueue<MPacket>[] masterHoldingList, BlockingQueue outgoingRetransmitQueue, BlockingQueue incomingRetransmitQueue, int sequenceNumber){
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
        this.sequenceNumber = sequenceNumber;
        if(Debug.debug) System.out.println("Instatiating ClientListenerThread");
    }
    
    public void run() {
        MPacket received = null;
        Client client = null;
        if(Debug.debug) System.out.println("Starting ClientListenerThread");
        while(true){
                    
                    int timeouts = 0, timeout_time = 300;
                    this.timer = System.currentTimeMillis();
                    while(eventQueue.peek() != null && eventQueue.peek().sequenceNumber != this.sequenceNumber && timeouts < 3) {
                        if(eventQueue.peek().type == 300 && eventQueue.peek().sequenceNumber < this.sequenceNumber) eventQueue.poll();
                        if(System.currentTimeMillis() - this.timer > timeout_time*(timeouts+1)) {
                            System.out.println("asking for event packet " + this.timer + " " + System.currentTimeMillis());
                            timeouts++;
                            this.timer = System.currentTimeMillis();
                            int myPID = clientTable.get(name).pid;
                            String send = myPID + "," + pid + "," + this.sequenceNumber;
                            MPacket retransmit = new MPacket(send, 400, 401);
                            outgoingRetransmitQueue.add(retransmit);
                        }

                    }
                    
                    if(timeouts == 3) System.out.println("FAILURE");
                
        }
    }
}




