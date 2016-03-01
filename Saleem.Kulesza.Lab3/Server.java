import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.ServerSocket
import java.net.Socket

public class Server {
    
	//The maximum of clients that will join
	//Server waits until the max number of clients to join 
    private static final int MAX_CLIENTS = 4;
    private ServerSocket serverSocket = null;
    private int clientCount; //The number of clients before game starts
    private Socket[] socketList = null; //A list of MSockets
    private BlockingQueue eventQueue = null; //A list of events
	private HashMap<String, ClientData> clientsHashMap= null;
	private BufferedReader in = null;
    /*
    * Constructor
    */
    public Server(int port) throws IOException{
        clientCount = 0; 
        serverSocket = new ServerSocket(port);
        if(Debug.debug) System.out.println("Listening on port: " + port);
        socketList = new Socket[MAX_CLIENTS];
        eventQueue = new LinkedBlockingQueue<MPacket>();
		clientsHashMap = new HashMap();
		this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }
    
    /*
    *Starts the listener and sender threads 
    */
    public void startThreads() throws IOException{
        //Listen for new clients
        while(clientCount < MAX_CLIENTS){
            //Start a new listener thread for each new client connection
            Socket socket = serverSocket.accept();

            new Thread(new ServerListenerThread(socket, eventQueue)).start();
            
            socketList[clientCount] = socket;                            
            
            clientCount++;
        }
        
        //Start a new sender thread 
        new Thread(new ServerSenderThread(socketList, eventQueue, clientsHashMap)).start();
		new Thread(new MoveProjectileThread(eventQueue)).start();
    }

        
    /*
    * Entry point for server
    */
    public static void main(String args[]) throws IOException {
        if(Debug.debug) System.out.println("Starting the server");
        int port = Integer.parseInt(args[0]);
        Server server = new Server(port);
                
        server.startThreads();    

    }
}
