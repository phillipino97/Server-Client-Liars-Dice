/*
Phillip Driscoll
Multithreaded Server-Client program that allows the clients to play a game of
liars dice against each other (minimum 2 players).
For more information on how to play liars dice, consult this clip from
Pirates of the Caribbean: https://www.youtube.com/watch?v=8yUkTpzXQZc
*/

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class ClientHandler extends Thread {
	//global variables including for the threat, ClientSide, socket, and queueing arraylists
	private Thread thread = null;
	private ClientSide client = null;
	private Socket socket = null;
	private String currentMsg = null;
	private DataOutputStream streamOut = null;
	private static ArrayList<String> queuedRMessages = new ArrayList<String>();
	private static ArrayList<String> queuedSMessages = new ArrayList<String>();

	//initiates the client and socket to the client and socket passed to it
	//then it opens everything up.
	public ClientHandler(ClientSide client, Socket socket) {
		this.client = client;
		this.socket = socket;
		open();
	}

	//initiates the output stream and the thread.
	public void open() {
		try {
			streamOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		} catch (IOException e) {
			System.out.println("Error opening output stream :: ClientHandler : " + e);
			client.stopClient();
		}
		if(thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}

	//closes the output stream and the thread
	public void close() {
		try {
			thread.stop();
			if(streamOut != null)
				streamOut.close();
			streamOut = null;
		} catch (IOException e) {
			System.out.println("Error closing output stream :: ClientHandler : " + e);
		}
		thread = null;
	}

	//prints messages coming in.
	public void handleMessages(String msg) {
		System.out.println(msg);
	}

	//method for sending the messages to the server, checks whether the ClientSide
	//commands are entered.
	//If they are then the server ignores the message and the client responds.
	public void sendMessages(String msg) {
		try {
			streamOut.writeUTF(msg);
			if(msg.equals("/rules")) {
				System.out.println("Each round, each player rolls a \"hand\" of dice under" +
				 "their cup and looks at their hand while keeping it concealed from the other players." +
				 "The first player begins bidding, announcing any face value and the minimum number of dice" +
				 "that the player believes are showing that value, under all of the cups in the game.");
			} else if(msg.equals("/example")) {
				File file = new File("example.txt");
				Scanner scanner = new Scanner(file);
				System.out.println("\n\n");
				String line = "";
				while(scanner.hasNextLine()) {
					line = scanner.nextLine();
					System.out.println(line);
				}
				System.out.println("\n\n");
			} else if(msg.equals("/help")) {
				System.out.println("/quit -- leave server\n" +
				"/roll -- rolls your dice\n" +
				"/dice -- shows your current dice roll\n" +
				"/show -- shows your dice to the rest of the players\n" +
				"/alldice -- shows the number of dice each person has\n" +
				"/score -- shows the score of everyone currently playing\n" +
				"/newgame -- begins a new game\n" +
				"/liar -- if you believe the person calling the bid is incorrect you can call this\n" +
				"to place your bid you call two numbers seperated by a space, the first being the number of dice\n" +
				"with the second number being the die number (ex: 3 4) meaning three fours"
				);
			}
			streamOut.flush();
		} catch (IOException e) {
			System.out.println("Error writing message to server :: ClientHandler : " + e);
			client.stopClient();
		}
	}

	//method for adding strings the the send queue for sending to the server
	public static void addToSendQueue(String msg) {
		queuedSMessages.add(msg);
	}

	//method for adding the messages to the receive queue for printing
	//to the terminal
	public static void addToReceiveQueue(String msg) {
		queuedRMessages.add(msg);
	}

	//thread that loops and checks the queues for whether there are messages that
	//need to be sent or printed.
	public void run() {
		while(ClientSide.isConnected) {
			try {
				thread.sleep(1);
			} catch (InterruptedException e) {
				System.out.println("Error sleepign thread :: ClientHandler : " + e);
			}
			if(!queuedRMessages.isEmpty()) {
				currentMsg = queuedRMessages.remove(0);
					handleMessages(currentMsg);
			}
			if(!queuedSMessages.isEmpty()) {
				currentMsg = queuedSMessages.remove(0);
				sendMessages(currentMsg);
				if(currentMsg.equals("/quit")) {
					client.stopClient();
				}
			}
			currentMsg = null;
		}
	}

}
