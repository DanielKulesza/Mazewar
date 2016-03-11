import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

public class ClientSenderThread implements Runnable {


    private MSocket[] socketList = null;
    private BlockingQueue<MPacket> eventQueue = null;
    private int sequenceNumber = 0; //Start at 1 for hello packets
    private PriorityBlockingQueue<MPacket> selfEventQueue = null;
    public ClientSenderThread(MSocket[] socketList,
                              BlockingQueue eventQueue, PriorityBlockingQueue<MPacket> selfEventQueue){
        this.socketList = socketList;
        this.eventQueue = eventQueue;
		this.selfEventQueue = selfEventQueue;

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

				if(toServer.type != 300) this.sequenceNumber++;

		    	
            }catch(InterruptedException e){
                e.printStackTrace();
                Thread.currentThread().interrupt();    
            }
            
        }
    }
}
