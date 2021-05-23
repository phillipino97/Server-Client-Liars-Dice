/*
Phillip Driscoll
Multithreaded Server-Client program that allows the clients to play a game of
liars dice against each other (minimum 2 players).
For more information on how to play liars dice, consult this clip from
Pirates of the Caribbean: https://www.youtube.com/watch?v=8yUkTpzXQZc
*/

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ServerSide extends Thread implements Runnable {
	//global varibales that need to be accessed by ServerThread and ServerHandler
	//through their various methods
	private ServerSocket serverSocket;
	private Thread thread = null;
	public static int dice_number = 5;
	public static int player_number = 5;
	public ServerThread[] clients = new ServerThread[player_number];
	public int clientCount = 0;
	public static int port = 0;
	public ServerHandler handle = null;

	public static void main(String[] args) {
		//retrieves the number of dice and the number of players for the server
		//then uses an argument passed during program initialization and uses it as the Port
		//number and starts the server
		Scanner scanner = new Scanner(System.in);
		System.out.println("Enter number of dice for each person to start with (default 5): ");
		dice_number = scanner.nextInt();
		System.out.println("Enter number of players (default 5): ");
		player_number = scanner.nextInt();
		ServerSide server = new ServerSide(Integer.parseInt(args[0]));
	}

	//thread for retrieving input to the console from the server.
	private Thread thread2 = new Thread() {
		public void run() {
			Scanner input = new Scanner(System.in);
			while(thread2 != null) {
				String msg = input.nextLine();
				ServerHandler.addToQueue(0, msg);
			}
		}
	};

	//constructor with port as a parameter
	public ServerSide(int port) {
		//sets the player cap on the ServerThread array
		this.clients = new ServerThread[player_number];
		try {
			//starts the server handler with ServerSide as a parameter
			handle = new ServerHandler(this);
			this.port = port;
			System.out.println("Connecting to port " + port + "...");
			//starts the server socket
			serverSocket = new ServerSocket(port);
			System.out.println("Starting " + serverSocket);
			//starts the two threads for this class
			start();
			thread2.start();
		} catch(Exception e) {
			System.out.println("Port already in use");
		}
	}

	//returns this class.
	public ServerSide getServerSide() {
		return this;
	}

	//thread for accepting clients to the server
	public void run() {
		System.out.println("Waiting for clients...");
		while(thread != null) {
			try {
				addThread(serverSocket.accept());
				thread.sleep(500);
			} catch(Exception e) {
				System.out.println("Acceptance Error: " + e);
				stopThread();
			}
		}
	}

	//method for accepting the client, starting the I/O processes
	//and adding them to the clients array
	public void addThread(Socket socket) {
			if(clientCount < clients.length) {
				System.out.println("Accepting Client");
				clients[clientCount] = new ServerThread(this, socket);
				try {
					clients[clientCount].open();
					clients[clientCount].startThreads();
					clients[clientCount].send("Enter your username: ");
					clientCount++;
				} catch(Exception e) {
					System.out.println("Cannot open thread: " + e);
					e.printStackTrace();
				}
			}
			else {
				System.out.println("Maximum client count achieved, could not accept client");
			}
	}

	//method for starting the initial thread for the class
	public void start() {
		if (thread == null) {
			thread = new Thread(this);
	        thread.start();
	      }
	}

	//stops both threads in the class
	public void stopThread() {
		if(thread != null) {
			thread.stop();
			thread = null;
		}
		if(thread2 != null) {
			thread2.stop();
			thread2 = null;
		}

	}

	//method for removing a client from the clients array
	public synchronized void remove(int ID) {
		int position = findClient(ID);
		if(position >= 0) {
			ServerThread toTerminate = clients[position];
			System.out.println("Removing client: " + ID);
			ServerHandler.addToQueue(ID, "disconnected");
			if(position < clientCount-1) {
				for(int i = position+1; i < clientCount; i++) {
					clients[i-1] = clients[i];
				}
			}
			clientCount--;
			try {
				toTerminate.close();
			} catch(Exception e) {
				System.out.println("Cannot close thread for " + ID + " : " + e);
			}
			toTerminate.stopThreads();
		}
	}

	//removes all clients from the client array
	public synchronized void removeAll() {
		for(int i = 0; i < clientCount; i++) {
			remove(clients[i].getID());
		}
		stopThread();
	}

	//method for finding the location of a client in the clients array
	//based on the ID given
	public int findClient(int ID) {
		for(int i = 0; i < clientCount; i++) {
			if(clients[i].getID() == ID) {
				return i;
			}
		}
		return -1;
	}
}
