import java.io.InvalidObjectException;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.Random;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Hashtable;

public class SequencerThread implements Runnable {
		
		private BlockingQueue<MPacket> incomingQueue = null;
		private BlockingQueue<MPacket> outgoingQueue  = null;
		private int sequenceNumber;
		private Hashtable<String, Client> clientTable = null;

		public SequencerThread(BlockingQueue<MPacket> incomingQueue, BlockingQueue<MPacket> outgoingQueue, Hashtable<String, Client> clientTable){
			this.incomingQueue = incomingQueue;
			this.outgoingQueue = outgoingQueue;
			this.sequenceNumber = 0;
			this.clientTable = clientTable;
		}

		public void run(){
			while(true){
				try{
					MPacket received = (MPacket)this.incomingQueue.take();

					if(Debug.debug) System.out.println("Giving sequence number");	
				
					String name = received.name;
					int pid = clientTable.get(name).pid;
					String send = pid + "," + received.sequenceNumber + "," + this.sequenceNumber;

					MPacket mpacket = new MPacket(send, 300, 301);
					mpacket.sequenceNumber = -1;
					outgoingQueue.add(mpacket);

					this.sequenceNumber++;

				}catch(InterruptedException e){
		            e.printStackTrace();
		            Thread.currentThread().interrupt();    
            	}
			}

        }
}
