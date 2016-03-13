/*
Copyright (C) 2004 Geoffrey Alan Washburn
   
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
   
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
   
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
USA.
*/
  
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JOptionPane;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.BorderFactory;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.Hashtable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;

/**
 * The entry point and glue code for the game.  It also contains some helpful
 * global utility methods.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Mazewar.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class Mazewar extends JFrame {

        /**
         * The default width of the {@link Maze}.
         */
        private final int mazeWidth = 20;

        /**
         * The default height of the {@link Maze}.
         */
        private final int mazeHeight = 10;

        /**
         * The default random seed for the {@link Maze}.
         * All implementations of the same protocol must use 
         * the same seed value, or your mazes will be different.
         */
        private final int mazeSeed = 42;

        /**
         * The {@link Maze} that the game uses.
         */
        private Maze maze = null;

        /**
         * The Mazewar instance itself. 
         */
        private Mazewar mazewar = null;
        private MSocket mSocket = null;
        private ObjectOutputStream out = null;
        private ObjectInputStream in = null;

        /**
         * The {@link GUIClient} for the game.
         */
        private GUIClient guiClient = null;
        
        
        /**
         * A map of {@link Client} clients to client name.
         */
        private Hashtable<String, Client> clientTable = null;


	private Hashtable<Integer, String> pidClientMap = null;
	
	private Player[] players = null;

	private MServerSocket mServerSocket = null;
        /**
         * A queue of events.
         */
        private BlockingQueue eventQueue = null;
        
        /**
         * The panel that displays the {@link Maze}.
         */
        private OverheadMazePanel overheadPanel = null;

        /**
         * The table the displays the scores.
         */
        private JTable scoreTable = null;
        
        /** 
         * Create the textpane statically so that we can 
         * write to it globally using
         * the static consolePrint methods  
         */
        private static final JTextPane console = new JTextPane();
      
        /** 
         * Write a message to the console followed by a newline.
         * @param msg The {@link String} to print.
         */ 
        public static synchronized void consolePrintLn(String msg) {
                console.setText(console.getText()+msg+"\n");
        }
        
        /** 
         * Write a message to the console.
         * @param msg The {@link String} to print.
         */ 
        public static synchronized void consolePrint(String msg) {
                console.setText(console.getText()+msg);
        }
        
        /** 
         * Clear the console. 
         */
        public static synchronized void clearConsole() {
           console.setText("");
        }
        
        /**
         * Static method for performing cleanup before exiting the game.
         */
        public static void quit() {
                // Put any network clean-up code you might have here.
                // (inform other implementations on the network that you have 
                //  left, etc.)
                

                System.exit(0);
        }
       
        /** 
         * The place where all the pieces are put together. 
         */
        public Mazewar(String serverHost, int serverPort) throws IOException,
                                                ClassNotFoundException {
                super("ECE419 Mazewar");
                consolePrintLn("ECE419 Mazewar started!");
                
                // Create the maze
                maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed);
                assert(maze != null);
                
                // Have the ScoreTableModel listen to the maze to find
                // out how to adjust scores.
                ScoreTableModel scoreModel = new ScoreTableModel();
                assert(scoreModel != null);
                maze.addMazeListener(scoreModel);
                
                // Throw up a dialog to get the GUIClient name.
                String name = JOptionPane.showInputDialog("Enter your name");
                if((name == null) || (name.length() == 0)) {
                  Mazewar.quit();
                }
                
                Socket socket = new Socket(serverHost, serverPort);
				if (Debug.debug) System.out.println("setting up output stream");
				out = new ObjectOutputStream(socket.getOutputStream());
                
				//Send hello packet to server
				if (Debug.debug) System.out.println("creating Hello Packet");
                MPacket hello = new MPacket(name, MPacket.HELLO, MPacket.HELLO_INIT);
                hello.mazeWidth = mazeWidth;
                hello.mazeHeight = mazeHeight;
                
                if(Debug.debug) System.out.println("Sending hello");
                out.writeObject(hello);
                if(Debug.debug) System.out.println("hello sent");
                //Receive response from server
				if (Debug.debug) System.out.println("setting up input stream");
				in = new ObjectInputStream(socket.getInputStream());
                
				MPacket resp = (MPacket)in.readObject();
                if(Debug.debug) System.out.println("Received response from server");
                //Initialize queue of events
                eventQueue = new LinkedBlockingQueue<MPacket>();
                //Initialize hash table of clients to client name 
                clientTable = new Hashtable<String, Client>(); 
                pidClientMap = new Hashtable<Integer, String>();
                // Create the GUIClient and connect it to the KeyListener queue
                //RemoteClient remoteClient = null;
				players = resp.players;
                for(Player player: resp.players){  
					System.out.println(player.name + " " + player.IPaddress + " " + player.port + " " + player.pid);
		    		if(player.name.equals(name)){
		
		            	if(Debug.debug)System.out.println("Adding guiClient: " + player);
		                    guiClient = new GUIClient(name, eventQueue);
							guiClient.IPaddress = player.IPaddress;
							guiClient.port = player.port;
							guiClient.pid = player.pid;
		                    maze.addClientAt(guiClient, player.point, player.direction);
		                    this.addKeyListener(guiClient);
		                    clientTable.put(player.name, guiClient);
		            }else{
		            	if(Debug.debug)System.out.println("Adding remoteClient: " + player);
		                    RemoteClient remoteClient = new RemoteClient(player.name);
							remoteClient.IPaddress = player.IPaddress;
							remoteClient.port = player.port;
							remoteClient.pid = player.pid;
		                    maze.addClientAt(remoteClient, player.point, player.direction);
		                    clientTable.put(player.name, remoteClient);
		            }
					pidClientMap.put(player.pid,player.name);
                }
                
                // Use braces to force constructors not to be called at the beginning of the
                // constructor.
                /*
                {
                        maze.addClient(new RobotClient("Norby"));
                        maze.addClient(new RobotClient("Robbie"));
                        maze.addClient(new RobotClient("Clango"));
                        maze.addClient(new RobotClient("Marvin"));
                }
                */

                
                // Create the panel that will display the maze.
                overheadPanel = new OverheadMazePanel(maze, guiClient);
                assert(overheadPanel != null);
                maze.addMazeListener(overheadPanel);
                
                // Don't allow editing the console from the GUI
                console.setEditable(false);
                console.setFocusable(false);
                console.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
               
                // Allow the console to scroll by putting it in a scrollpane
                JScrollPane consoleScrollPane = new JScrollPane(console);
                assert(consoleScrollPane != null);
                consoleScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Console"));
                
                // Create the score table
                scoreTable = new JTable(scoreModel);
                assert(scoreTable != null);
                scoreTable.setFocusable(false);
                scoreTable.setRowSelectionAllowed(false);

                // Allow the score table to scroll too.
                JScrollPane scoreScrollPane = new JScrollPane(scoreTable);
                assert(scoreScrollPane != null);
                scoreScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Scores"));
                
                // Create the layout manager
                GridBagLayout layout = new GridBagLayout();
                GridBagConstraints c = new GridBagConstraints();
                getContentPane().setLayout(layout);
                
                // Define the constraints on the components.
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 1.0;
                c.weighty = 3.0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                layout.setConstraints(overheadPanel, c);
                c.gridwidth = GridBagConstraints.RELATIVE;
                c.weightx = 2.0;
                c.weighty = 1.0;
                layout.setConstraints(consoleScrollPane, c);
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1.0;
                layout.setConstraints(scoreScrollPane, c);
                                
                // Add the components
                getContentPane().add(overheadPanel);
                getContentPane().add(consoleScrollPane);
                getContentPane().add(scoreScrollPane);
                
                // Pack everything neatly.
                pack();

                // Let the magic begin.
                setVisible(true);
                overheadPanel.repaint();
                this.requestFocusInWindow();
        }

        /*
        *Starts the ClientSenderThread, which is 
         responsible for sending events
         and the ClientListenerThread which is responsible for 
         listening for events
        */
        private void startThreads(String host, int port){
                //Start a new sender thread
if(Debug.debug) System.out.println("starting threads");
		//3 sender threads
		try {
		
		Comparator<MPacket> comparator = new PacketComparator(); 
		
		BlockingQueue outgoingRetransmitQueue = new LinkedBlockingQueue<MPacket>();
		BlockingQueue incomingRetransmitQueue = new LinkedBlockingQueue<MPacket>();
		
		BlockingQueue outgoingOrderRetransmitQueue = new LinkedBlockingQueue<MPacket>();
		//BlockingQueue incomingOrderRetransmitQueue = new LinkedBlockingQueue<MPacket>();

		BlockingQueue incomingQueue = new LinkedBlockingQueue<MPacket>();
		BlockingQueue outgoingQueue = new LinkedBlockingQueue<MPacket>();
		PriorityBlockingQueue<MPacket> masterOrderQueue = new PriorityBlockingQueue<MPacket>(10, comparator);
		BlockingQueue[] masterHoldingList = new BlockingQueue[players.length];
		for(int i = 0; i < players.length; i++) {
			masterHoldingList[i] = new LinkedBlockingQueue<MPacket>();
		}

		PriorityBlockingQueue<MPacket> holdbackQueue = new PriorityBlockingQueue<MPacket>(10, comparator);

		PriorityBlockingQueue<MPacket> sequencerHoldbackQueue = new PriorityBlockingQueue<MPacket>(10, comparator);
		
		Hashtable<Integer, MSocket> pidtoMSocketMap = new Hashtable<Integer, MSocket>();
		
		mServerSocket = new MServerSocket(port + guiClient.pid);
		
		int i = 0, j=0, k = 0;
		MSocket[] socketList = new MSocket[players.length - 1];
        
        //Start a new listener thread - 4 threads that receives all packets and puts them in main receiver queue
		for(Player player: players){
			System.out.println(player.name);
			j=0;			
			if (guiClient.pid == i){
				Integer x = i;
				MPacket ping = new MPacket(x.toString(), 100, 102);
				this.out.writeObject(ping);
				String name = guiClient.getName();
				while(j < players.length-1){
                    
                    if(Debug.debug) System.out.println("accepting connections");
					boolean sequencer = (i == 0);					
			
					MSocket mSocketReceive = mServerSocket.accept();					
					
                    if(Debug.debug) System.out.println("ping");
					new Thread(new ClientListenerThread(name, mSocketReceive, clientTable, incomingQueue, sequencer, masterOrderQueue, masterHoldingList, outgoingRetransmitQueue, incomingRetransmitQueue)).start();
					
//					if(Debug.debug && guiClient.pid == i) System.out.println(socketList[j].socket.getPort());
					j++;	
				}
				this.in.readObject();
			} else {
					this.in.readObject();
                    if(Debug.debug) System.out.println("ping2 " + player.IPaddress + " " + (port + i));

					MSocket mSocketSend = new MSocket(player.IPaddress, port + i);				            
					socketList[k] = mSocketSend;
					
					pidtoMSocketMap.put(player.pid, mSocketSend);

					k++;
			}
			

			i++;
			
		}
		
		PriorityBlockingQueue<MPacket> selfEventQueue = new PriorityBlockingQueue<MPacket>(10, comparator);
            
		new Thread(new ClientListenerThread(clientTable, selfEventQueue, incomingQueue, guiClient.pid == 0, masterOrderQueue, masterHoldingList)).start();
            
            
		new Thread(new ClientSenderThread(socketList, eventQueue, selfEventQueue, outgoingRetransmitQueue, incomingRetransmitQueue, pidtoMSocketMap, holdbackQueue, outgoingOrderRetransmitQueue, sequencerHoldbackQueue)).start();
        
        new Thread(new RetransmitThread(socketList, outgoingRetransmitQueue, incomingRetransmitQueue, pidtoMSocketMap, holdbackQueue, outgoingOrderRetransmitQueue, sequencerHoldbackQueue)).start();

		if(guiClient.pid == 0) new Thread(new SequencerThread(incomingQueue, eventQueue, clientTable)).start();
            
        new Thread(new MasterThread(masterOrderQueue, masterHoldingList, clientTable,sequencerHoldbackQueue, guiClient.getName(), outgoingOrderRetransmitQueue, outgoingRetransmitQueue)).start();

		new Thread(new CleanupThread(holdbackQueue, sequencerHoldbackQueue)).start();         

		//New thread to process receiver queue- pop the packets sequentially and place in respective process queues - If the process is the sequencer, then assign seq no for event packets as they come
		//If the packet is a order packet then we can then search within the desired queue for packet, and send it to the event queue of the process
		}catch(IOException e){
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }catch(ClassNotFoundException e){
            e.printStackTrace();
        }

		}
		
        
        /**
         * Entry point for the game.  
         * @param args Command-line arguments.
         */
        public static void main(String args[]) throws IOException,
                                        ClassNotFoundException{

             String host = args[0];
             int port = Integer.parseInt(args[1]);
             /* Create the GUI */
             Mazewar mazewar = new Mazewar(host, port);
             mazewar.startThreads(host, port + 1);
        }
}
