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
				MPacket packet = holdbackQueue.poll();
				if(System.currentTimeMillis() - packet.timestamp <= 1000) holdbackQueue.add(packet);
			}
			if(sequencerHoldbackQueue.peek() != null) {
				MPacket packet = sequencerHoldbackQueue.poll();
				if(System.currentTimeMillis() - packet.timestamp <= 1000) sequencerHoldbackQueue.add(packet);
			}
		}
	}

}

