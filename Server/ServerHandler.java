/*
Phillip Driscoll
Multithreaded Server-Client program that allows the clients to play a game of
liars dice against each other (minimum 2 players).
For more information on how to play liars dice, consult this clip from
Pirates of the Caribbean: https://www.youtube.com/watch?v=8yUkTpzXQZc
*/

import java.util.ArrayList;
import java.util.Scanner;

public class ServerHandler extends Thread {
	//global variables that hold info pertinent to this class and needing
	//to be accessed between methods within the class.
	private ServerSide server = null;
	private Thread thread = null;
	private int total_dice = 0;
	private int[] total_dice_values = new int[6];
	private int current_player_id = 0;
	private String current_player_name = "";
	private int[] current_player_guess = {0, 0};
	private int[] players_with_dice = {0, -1};
	public static ArrayList<String> queuedMessages = new ArrayList<String>();
	public static ArrayList<Integer> queuedUsers = new ArrayList<Integer>();

	public ServerHandler(ServerSide server) {
		//initializes the ServerSide reference to server
		//initializes a new thread and starts it
		this.server = server;
		thread = new Thread(this);
		thread.start();
	}

	//stops this class' thread
	public void close() {
		if(thread != null) {
			thread.stop();
			thread = null;
		}
	}

	//where the magic for the liars dice game happens,
	//this method accepts an ID from a ServerThread object and the message sent
	//from a client.
	public void HandleMessages(int ID, String msg) {
		//initializes a to being the location of the ServerThread object within
		//the ServerSide.clients ServerThread array.
		int a = server.findClient(ID);
		//checks whether the message is coming from the server.
		if(ID == 0) {
			//closes the server if /close is called from server.
			if(msg.equals("/close")) {
				for(int i = 0; i < server.clientCount; i++) {
					server.clients[i].send("Server -> Server is closing!\n");
				}
				server.removeAll();
				System.exit(0);
				//shows the dice of all players to the server when /show is given
				//from the server.
			} else if(msg.equals("/show")) {
				for(int i = 0; i < server.clientCount; i++) {
					System.out.println(server.clients[i].getName() + ": " + server.clients[i].diceToString());
				}
			} else {
				for(int i = 0; i < server.clientCount; i++) {
					server.clients[i].send("Server -> " + msg);
				}
			}
			//makes sure that there is an ID and a is initialized
		} else if(a != -1 && (ID > 0)) {
			String name = "";
			//initializes the name to be the username of the client sending the
			//message to the server. This is used to be able to send the message to
			//other clients and allow them to readily recognize who is typing it.
			try {
				name = server.clients[a].getName();
				server.clients[a].messages += 1;
			} catch(Exception e) {

			}
			//if the client sends /quit then the disconnect from the server
			if(msg.equals("/quit")) {
				server.remove(ID);
			} else if(server.clients[a].messages == 2) {
				server.clients[a].setName(msg);
				//if /roll is sent from the client then they roll their dice
			}  else if(msg.equals("/roll")) {
				server.clients[a].initializeDice();
				server.clients[a].send("Server -> " + server.clients[a].diceToString());
				System.out.println(name + " rolled!");
				updateDice();
				//if /dice is sent, the client is shown their current dice roll again
			} else if(msg.equals("/dice")) {
				server.clients[a].send("Server -> " + server.clients[a].diceToString());
				//if /show is sent, all clients are shown the dice roll of the client
				//who sent the /show request
			} else if(msg.equals("/show")) {
				for(int i = 0; i < server.clientCount; i++) {
					server.clients[i].send(name + " -> " + server.clients[a].diceToString());
				}
				//shows the number of dice currently on the table to the player sending
				//the /alldice request
			} else if(msg.equals("/alldice")) {
				server.clients[a].send("Server ->");
				for(int i = 0; i < server.clientCount; i++) {
					server.clients[a].send(server.clients[i].getName() + ": " + server.clients[i].dice);
				}
				//if /score is sent, it shows the client the scores of everyone who is
				//playing
			} else if(msg.equals("/score")) {
				server.clients[a].send("Server ->");
				for(int i = 0; i < server.clientCount; i++) {
					server.clients[a].send(server.clients[i].getName() + ": " + server.clients[i].getScore());
				}
				//if /newgame is sent, it starts a new game
			} else if(msg.equals("/newgame")) {
				total_dice = 0;
				for(int i = 0; i < server.clientCount; i++) {
					server.clients[i].dice = server.dice_number;
					server.clients[i].nums = new int[server.dice_number];
					server.clients[i].initializeDice();
					server.clients[i].send(server.clients[i].diceToString());
					total_dice += server.dice_number;
				}
				updateDice();
				//if /liar is sent, it means that the player sending it does not believe
				//that the previous players bid is accurate. This method checks the
				//accuracy of the previous players bid by checking with
				//current_player_guess.
			} else if(msg.equals("/liar")) {
				for(int i = 0; i < server.clientCount; i++) {
					if(server.clients[i].getID() != ID) {
						server.clients[i].send(name + " -> " + msg);
					}
				}
				boolean is_liar = checkGuess();

				if(is_liar) {
					for(int i = 0; i < server.clientCount; i++) {
						server.clients[i].send("Server -> " + current_player_name + " was lying and lost a die!");
						server.clients[i].send("There were " + total_dice_values[current_player_guess[1]-1] + " " + current_player_guess[1] + "\'s");
					}
					server.clients[server.findClient(current_player_id)].removeDice();
				} else {
					for(int i = 0; i < server.clientCount; i++) {
						server.clients[i].send("Server -> " + current_player_name + " was correct and " + name + " lost a die!");
						server.clients[i].send("There were " + total_dice_values[current_player_guess[1]-1] + " " + current_player_guess[1] + "\'s");
					}
					server.clients[a].removeDice();
				}
				players_with_dice = getPlayersWithDice();
				if(players_with_dice[0] == 1) {
					for(int i = 0; i < server.clientCount; i++) {
						server.clients[i].send(server.clients[players_with_dice[1]].getName() + " -> Won the game!");
					}
					server.clients[players_with_dice[1]].addScore();
				}

				//initializes current_player_id, current_player_name, and current_player_guess
				//with the person sending a bid i.e. 4 4
			} else if(msg.matches("\\d{1,2} \\d")) {
				current_player_id = ID;
				current_player_name = name;
				Scanner scanner = new Scanner(msg);
				current_player_guess[0] = scanner.nextInt();
				current_player_guess[1] = scanner.nextInt();

				for(int i = 0; i < server.clientCount; i++) {
					if(server.clients[i].getID() != ID) {
						server.clients[i].send(name + " -> " + msg);
					}
				}
				//if /list is sent, it grabs the names of all players in the game
				//and sends their names to the client requesting them
			} else if(msg.equals("/list")) {
				server.clients[a].send("Server ->");
				for(int i = 0; i < server.clientCount; i++) {
					server.clients[a].send(server.clients[i].getName());
				}
				//if the message is /help, /example, or /rules then the server ignores
				//the message as those are ClientSide requests
			} else if(msg.equals("/help") || msg.equals("/example") || msg.equals("/rules")) {

				//if the message is "connected" then it has a special usecase
				//where it uses the user ID rather than their username as it has
				//not been set yet
			} else if(msg.equals("connected")) {
				for(int i = 0; i < server.clientCount; i++) {
					if(server.clients[i].getID() != ID) {
						server.clients[i].send(ID + " -> " + msg);
					}
				}
				//this case means that the message is a normal message and there are
				//no special cases in the message. It sends the message to all users
				//except the user sending the messages.
			} else {
				for(int i = 0; i < server.clientCount; i++) {
					if(server.clients[i].getID() != ID) {
						server.clients[i].send(name + " -> " + msg);
					}
				}
				System.out.println(name + " -> " + msg);
			}
		} else {
			for(int i = 0; i < server.clientCount; i++) {
				if(server.clients[i].getID() != ID) {
					server.clients[i].send(ID + " -> " + msg);
				}
			}
		}
	}

	//method to check whether the current_player_guess given by the previous
	//players bid is correct with what is currently on the table.
	//called from the /liar case in HandleMessages
	private boolean checkGuess() {
		if(total_dice_values[current_player_guess[1]-1] != current_player_guess[0]) {
			return true;
		} else {
			return false;
		}
	}

	//This checks how many players have dice after each round to see if a player
	//has won or not, called from the /liar case in HandleMessages
	private int[] getPlayersWithDice() {
		int a = 0;
		int[] h = new int[2];
		for(int i = 0; i < server.clientCount; i++) {
			if(server.clients[i].dice > 0) {
				a += 1;
			}
		}
		if(a == 1) {
			for(int i = 0; i < server.clientCount; i++) {
				if(server.clients[i].dice > 0) {
					h[0] = 1;
					h[1] = i;
					return h;
				}
			}
		}
		h[0] = a;
		h[1] = -1;
		return h;
	}

	//updates the total_dice_values for checking with the bid.
	//called after /liar condition goes through the rest.
	private void updateDice() {
		total_dice = 0;
		for(int i = 0; i < total_dice_values.length; i++) {
			total_dice_values[i] = 0;
		}

		for(int i = 0; i < server.clientCount; i++) {
			for(int j = 0; j < server.clients[i].nums.length; j++) {
				total_dice += 1;
				if(server.clients[i].nums[j] == 1) {
					total_dice_values[0] += 1;
				} else if(server.clients[i].nums[j] == 2) {
					total_dice_values[1] += 1;
				} else if(server.clients[i].nums[j] == 3) {
					total_dice_values[2] += 1;
				} else if(server.clients[i].nums[j] == 4) {
					total_dice_values[3] += 1;
				} else if(server.clients[i].nums[j] == 5) {
					total_dice_values[4] += 1;
				} else if(server.clients[i].nums[j] == 6) {
					total_dice_values[5] += 1;
				}
			}
		}
	}

	//I decided to go with a Queueing method for messages for handling large quatities
	//of messages all at once.
	public static void addToQueue(int ID, String msg) {
		queuedUsers.add(ID);
		queuedMessages.add(msg);
	}

	//the thread constantly checks whether the Queue is empty and if it isn't
	//then it sends the queued message to the HandleMessages method to send them
	//off then removes the messages from queue.
	public void run() {
		while(thread != null) {
			try {
				thread.sleep(1);
			} catch (InterruptedException e) {
				System.out.println("Error sleeping thread :: ServerHandler : " + e);
			}
			while(!queuedMessages.isEmpty()) {
				HandleMessages(queuedUsers.remove(0), queuedMessages.remove(0));
			}
		}
	}
}
