/*
Phillip Driscoll
Multithreaded Server-Client program that allows the clients to play a game of
liars dice against each other (minimum 2 players).
For more information on how to play liars dice, consult this clip from
Pirates of the Caribbean: https://www.youtube.com/watch?v=8yUkTpzXQZc
*/

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

//object class for holding client objects for use in an array in ServerSide
public class ServerThread {
	//all info prevalent to the client
	private Socket socket = null;
	private ServerSide server = null;
	private int ID = -1;
	private String username = "";
	private int score = 0;
	public int dice = ServerSide.dice_number;
	public int[] nums = new int[dice];
	public int messages = 0;
	private DataInputStream streamIn = null;
	private DataOutputStream streamOut = null;

	//constructor taking ServerSide and a Socket as parameters
	public ServerThread(ServerSide server, Socket socket) {
		//sets the server and socket then sets the ID
		this.server = server;
		this.socket = socket;
		ID = socket.getPort();
	}

	//method for initializing the dice in the array
	public void initializeDice() {
		for(int i = 0; i < nums.length; i++) {
			nums[i] = (int) Math.floor(1 + (Math.random() * 6));
		}
		Arrays.sort(nums);
	}

	//method for sending a message to the client
	public void send(String msg) {
		try {
			streamOut.writeUTF(msg);
			streamOut.flush();
		} catch(Exception e) {
			System.out.println("Problem sending message from " + ID);
			ServerHandler.addToQueue(ID, "disconnected");
			server.remove(ID);
			messageThread.stop();
			return;
		}
	}

	//starts the message thread for retrieving and sending messages
	public void startThreads() {
		messageThread.start();
	}

	//stops the message thread
	public void stopThreads() {
		messageThread.stop();
	}

	//thread for checking whether the client sent a message and if they did then
	//it calls to add it to the ServerHandler Queue to check the messages
	//and send responses
	Thread messageThread = new Thread() {
		public void run() {
			try {
				ServerHandler.addToQueue(ID, "connected");
				while(true) {
					try {
						String clientResponse = streamIn.readUTF();
						ServerHandler.addToQueue(ID, clientResponse);
					} catch(Exception e) {
						ServerHandler.addToQueue(ID, "disconnected");
						server.remove(ID);
						messageThread.stop();
					}
				}
			} catch(Exception e) {

			}
	}
	};

	//starts the I/O streams
	public void open() throws IOException {
		streamOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		streamIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
	}

	//closes the I/O streams, socket, and thread
	public void close() throws IOException {
		if(socket != null) socket.close();
		if(streamIn != null) streamIn.close();
		if(streamOut != null) streamOut.close();
		stopThreads();
	}

	//returns the client ID
	public int getID() {
		return ID;
	}

	//returns the current ServerThread
	public ServerThread getServerThread() {
		return this;
	}

	//removes a single die from the clients dice
	public void removeDice() {
		dice -= 1;
		nums = new int[dice];
	}

	//returns the client's current dice roll as a string
	public String diceToString() {
		String a = "[";
		for(int i = 0; i < nums.length; i++) {
			if(i != nums.length - 1) {
				a += nums[i] + ", ";
			} else {
				a += nums[i] + "";
			}
		}
		a += "]";
		return a;
	}

	//method for setting the client's username
	public void setName(String name) {
		username = name;
	}

	//method for retrieving the username
	public String getName() {
		return username;
	}

	//method for adding 1 to the client's score
	public void addScore() {
		score += 1;
	}

	//method for retrieving the client's score
	public int getScore() {
		return score;
	}

}
