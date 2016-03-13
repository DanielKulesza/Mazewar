import java.io.InvalidObjectException;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.Random;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

public class CleanupThread implements Runnable {

	private PriorityBlockingQueue<MPacket> holdbackQueue = null;
	private PriorityBlockingQueue<MPacket> sequencerHoldbackQueue = null;
	
	public CleanupThread(PriorityBlockingQueue<MPacket> holdbackQueue, PriorityBlockingQueue<MPacket> sequencerHoldbackQueue) {
		this.holdbackQueue = holdbackQueue;
		this.sequencerHoldbackQueue = sequencerHoldbackQueue;
	}

	public void run() {
		while(true) {
			if(holdbackQueue.peek() != null) {
				MPacket packet = holdbackQueue.peek();
                if(packet != null && System.currentTimeMillis() - packet.timestamp > 10000) {
                    holdbackQueue.poll();
                    if(Debug.debug) System.out.println("removing event packet");
                }
			}
			if(sequencerHoldbackQueue.peek() != null) {
				MPacket packet = sequencerHoldbackQueue.peek();
                if(packet != null && System.currentTimeMillis() - packet.timestamp > 60000) {
                    sequencerHoldbackQueue.poll();
                    if(Debug.debug) System.out.println("removing order packet");
                }
			}
		}
	}

}

