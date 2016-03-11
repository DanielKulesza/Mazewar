import java.io.InvalidObjectException;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.Random;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;

public class MasterThread implements Runnable {
    
    private PriorityBlockingQueue<MPacket> masterOrderQueue = null;
    private BlockingQueue<MPacket>[] masterHoldingList  = null;
    private int sequenceNumber = null;
    private Hashtable<String, Client> clientTable = null;
    private Comparator<MPacket> comparator = null;
    //private PriorityBlockingQueue<MPacket> processingQueue = null;
    
    public SequencerThread(PriorityBlockingQueue<MPacket> masterOrderQueue, BlockingQueue<MPacket> masterHoldingList[], Hashtable<String, Client> clientTable){
        this.masterOrderQueue = masterOrderQueue;
        this.masterHoldingList = masterHoldingList;
        this.sequenceNumber = 0;
        this.clientTable = clientTable;
        //this.comparator = new PacketComparator();
        //this. processingQueue = new PriorityBlockingQueue<MPacket>(10, comparator);
    }
    
    public void run(){
        MPacket order = null;
        MPacket received = null;
        Client client  = null;
        if(Debud.debug) System.out.println("Starting MasterThread");
        while(true){
            //wait for next order packet
            while(masterOrderQueue.peek() == null || masterOrderQueue.peek().sequenceNumber != this.sequenceNumber) {
                //do nothing
            }
            
            
            //process packets with new sequence number
            while((masterOrderQueue.peek() != null) && (masterOrderQueue.peek().sequenceNumber == this.sequenceNumber) {
                order = masterOrderQueue.poll();
                String[] info = order.name.spit(",");
                
                int pid = info[0].parseInt();
                int localSeqNum = info[1].parseInt();
                
                received = masterHoldingList[pid].take();
                
                while(event.sequenceNumber != localSeqNum) {
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
            
        }
        
    }
}