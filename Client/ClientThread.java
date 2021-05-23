/*
Phillip Driscoll
Multithreaded Server-Client program that allows the clients to play a game of
liars dice against each other (minimum 2 players).
For more information on how to play liars dice, consult this clip from
Pirates of the Caribbean: https://www.youtube.com/watch?v=8yUkTpzXQZc
*/

import java.io.DataInputStream;
import java.net.Socket;

public class ClientThread extends Thread {
	//global variables containing socket info, a ClientSide variable, and the input stream.
	private Socket socket = null;
	private ClientSide client = null;
	private DataInputStream streamIn = null;

	//constructor with the ClientSide object and socket as parameters
	//initializes the global client and socket. then opens the input stream
	//then starts the thread.
	public ClientThread(ClientSide client, Socket socket) {
		this.client = client;
		this.socket = socket;
		open();
		start();
	}

	//method for opening the input stream.
	public void open() {
		try {
			streamIn = new DataInputStream(socket.getInputStream());
		} catch(Exception e) {
			System.out.println("Could not get input stream :: ClientThread : " + e);
			client.stopClient();
		}
	}

	//method for stopping the thread then closing the input stream.
	public void close() {
		try {
			Thread.stop();
			if(streamIn != null) {
				streamIn.close();
			}
		} catch(Exception e) {
			System.out.println("Could not close input stream :: ClientThread : " + e);
		}
	}

	//thread that runs while ClientSide.isConnected is true. While it is true
	//it checks if there are any messages sent to the client from the server and if
	//there is it makes sure that the message isn't "Server Closed" and if it is
	//then it stops the client. Else, it adds it to the receive queue in the ClientHandler
	//to be printed to the screen.
	public void run() {
		try {
			Thread.sleep(200);
		} catch (InterruptedException e1) {
			System.out.println("Thread interrrupt :: ClientThread : " + e1);
		}
		while(ClientSide.isConnected) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e1) {
				System.out.println("Thread interrrupt :: ClientThread : " + e1);
			}
			try {
				String serverResponse = streamIn.readUTF();
				if(serverResponse.equalsIgnoreCase("Server Closed")) {
					ClientHandler.addToReceiveQueue("Server -> " + serverResponse);
					client.stopClient();
				} else {
					ClientHandler.addToReceiveQueue(serverResponse);
				}
			} catch(Exception e) {
				client.stopClient();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					System.out.println("Thread interrrupt :: ClientThread : " + e1);
				}
				return;
			}
		}
	}

 }
