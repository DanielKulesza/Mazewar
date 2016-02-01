import java.io.InvalidObjectException;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.Random;



public class MoveProjectileThread implements Runnable {

	//private ObjectOutputStream[] outputStreamList = null;
    private MSocket[] mSocketList = null;
    private BlockingQueue eventQueue = null;
    private int globalSequenceNumber=0;
	private double time; 
    
    public MoveProjectileThread(BlockingQueue eventQueue){
        this.eventQueue = eventQueue;
		this.time = System.currentTimeMillis();
    }





	public void run() {
		MPacket toBroadcast = null;
        this.time = System.currentTimeMillis();
        
        while(true){
            try{
				
				if(System.currentTimeMillis() - time >= 200) {
					this.time = System.currentTimeMillis();
					MPacket move = new MPacket("everyone", MPacket.ACTION, MPacket.MOVE_PROJECTILE);
			
					move.sequenceNumber = this.globalSequenceNumber++;
            		if(Debug.debug) System.out.println("Sending " + move);
            		//Send it to all clients

					eventQueue.put(move); 
				}
	
			}catch(InterruptedException e){
                System.out.println("Throwing Interrupt");
                Thread.currentThread().interrupt();    
            }
		}
	}
}
