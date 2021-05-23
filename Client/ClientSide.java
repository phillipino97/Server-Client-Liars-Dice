/*
Phillip Driscoll
Professor Gyawali
COSC-4475.501
2 October 2020
Assignment #2
Multithreaded Server-Client program that allows the clients to play a game of
liars dice against each other (minimum 2 players).
For more information on how to play liars dice, consult this clip from
Pirates of the Caribbean: https://www.youtube.com/watch?v=8yUkTpzXQZc
*/

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ClientSide extends Thread {
	//global variables for sockets, output stream, and the other classes for client
	private Socket socket = null;
	private DataOutputStream output = null;
	private ClientThread client = null;
	public static boolean isConnected = false;
	private ClientHandler handler = null;
	private Scanner scanner = new Scanner(System.in);

	public static void main(String[] args) {
		//prints info to the terminal then it passes the initial java arguments
		//to the constructor.
		System.out.println("For commands type /help\nFor the rules type /rules\nTo see an example of how to play the game type /example");
		ClientSide client = new ClientSide(args[0], Integer.parseInt(args[1]));
	}

	//this is where it connects to the server using the hostname and port
	//passed to it as parameters. Then it starts the thread and opens the output
	//stream.
	public ClientSide(String hostname, int port) {
		try {
			socket = new Socket(hostname, port);
			System.out.println("Connected: " + socket);
			start();
			open();
			if(client != null)
				handler = new ClientHandler(this, socket);
				Thread.sleep(100);
			} catch(UnknownHostException e) {
				System.out.println("Cannot find Host: " + e);
			} catch(IOException e) {
				System.out.println("Could not find server");
			} catch (InterruptedException e) {
				System.out.println("Error sleeping thread :: ClientSide : " + e);
			}
	}

	//method for stopping the client.
	public void stopClient() {
		isConnected = false;
		client.close();
		try {
			output.close();
			socket.close();
		} catch(Exception e) {
			System.out.println("Error closing...");
			client.close();
			client.stop();
			System.exit(0);
		}
		handler.close();
		Thread.stop();
		try {
			output.close();
		} catch(Exception e) {
		System.exit(0);
		}
		System.out.println("Disconnected");
		System.exit(0);
	}

	//opens the output stream and gives info to the ClientThread and sets the boolean
	//isConnected to true;
	public void open() throws IOException {
		isConnected = true;
		output = new DataOutputStream(socket.getOutputStream());
	 	client = new ClientThread(this, socket);
	}

	//thread that checks for user input into the terminal and then sends it to
	//the send queue for sending to the server.
	public void run() {
		while(isConnected) {
			String a = scanner.nextLine();
			ClientHandler.addToSendQueue(a);
		}
	}
}
