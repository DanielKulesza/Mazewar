import java.io.InvalidObjectException;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.Random;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerSenderThread implements Runnable {

    //private ObjectOutputStream[] outputStreamList = null;
    private Socket[] socketList = null;
    private BlockingQueue eventQueue = null;
    private int globalSequenceNumber;
    private double time;
	
	private ObjectOutputStream[] outputStreams = null;
    
    public ServerSenderThread(Socket[] socketList,
                              BlockingQueue eventQueue){
        this.socketList = socketList;
        this.eventQueue = eventQueue;
        this.globalSequenceNumber = 0;
		this.time = System.currentTimeMillis();
		this.outputStreams = new ObjectOutputStream[socketList.length];
    }

    /*
     *Handle the initial joining of players including 
      position initialization
     */
    public void handleHello(){
        
        //The number of players
        int playerCount = socketList.length;
        Random randomGen = null;
        Player[] players = new Player[playerCount];
        if(Debug.debug) System.out.println("In handleHello");
        MPacket hello = null;
        try{        
            for(int i=0; i<playerCount; i++){
                hello = (MPacket)eventQueue.take();
                //Sanity check 
                if(hello.type != MPacket.HELLO){
                    throw new InvalidObjectException("Expecting HELLO Packet");
                }
                if(randomGen == null){
                   randomGen = new Random(hello.mazeSeed); 
                }
                //Get a random location for player
                Point point =
                    new Point(randomGen.nextInt(hello.mazeWidth),
                          randomGen.nextInt(hello.mazeHeight));
                
                //Start them all facing North
                Player player = new Player(hello.name, point, Player.North);
				player.name = hello.name;
				player.pid = i;
                players[i] = player;
            }
            
            hello.event = MPacket.HELLO_RESP;
            hello.players = players;
            //Now broadcast the HELLO
            if(Debug.debug) System.out.println("Sending " + hello);
			int i = 0;

	    for(Socket socket: socketList){
			System.out.println("remote: " + socket.getRemoteSocketAddress() + " Inet: " + socket.getInetAddress());
			String str = socket.getRemoteSocketAddress().toString().substring(1);
			String[] Ipaddress = str.split(":"); 
			players[i].setIP(Ipaddress[0]);
			players[i].setPort(socket.getPort());
			i++;
	    }
	    i=0;
        for(Socket socket: socketList){
			
			outputStreams[i] = new ObjectOutputStream(socket.getOutputStream());			               
			outputStreams[i].writeObject(hello);
			System.out.println("Sent: " + players[i].name + " " + players[i].port + " " + players[i].IPaddress + " " + players[i].pid);
			i++;  
        }
        }catch(InterruptedException e){
            e.printStackTrace();
            Thread.currentThread().interrupt();    
        }catch(IOException e){
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

	
    
    public void run() {
        MPacket toBroadcast = null;
        this.time = System.currentTimeMillis();
        handleHello();
		
		try{
		
		int j = 0;
		for(int i = 0; i < this.socketList.length; i++) {
			toBroadcast = (MPacket)eventQueue.take();
			j = 0;
			for(Socket socket : this.socketList) {
			if(Debug.debug)System.out.println("sending ping " + outputStreams[j]);
				outputStreams[j].writeObject(toBroadcast);
				j++;
			}
			
		}		
        }catch(InterruptedException e){
                //System.out.println("Throwing Interrupt");
            Thread.currentThread().interrupt();    
        }catch(IOException e){
        	e.printStackTrace();
        	Thread.currentThread().interrupt();
        }

        while(true){
            try{
		
				//if(System.currentTimeMillis() - time >= 200) {
				//	this.time = System.currentTimeMillis();
				//	moveProjectile();
				//}
                //Take packet from queue to broadcast
                //to all clients
								

	                toBroadcast = (MPacket)eventQueue.take();
	                //Tag packet with sequence number and increment sequence number
		            toBroadcast.sequenceNumber = this.globalSequenceNumber++;
                	if(Debug.debug) System.out.println("Sending " + toBroadcast);
                //Send it to all clients
 //               	for(Socket socket: socketList){
 //                  		 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());                
//				out.writeObject(toBroadcast);
//                	}
            }catch(InterruptedException e){
                //System.out.println("Throwing Interrupt");
                Thread.currentThread().interrupt();    
            }
//catch(IOException e){
//            e.printStackTrace();
//            Thread.currentThread().interrupt();
//            }
            
        }
    }
}




