package server.model;

import static server.model.Server.cLists;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerSender extends Thread {
	public void resetFriendRequest(ArrayList<String> resetFR_LIST) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("requests.txt", false));
		for (String i : resetFR_LIST) {
			System.out.println(i);
			writer.write(i);
			writer.newLine();
		}
		writer.flush();
		writer.close();
	}

	public void handleSendFriendRequest() throws IOException {
		ArrayList<String> FR_LIST = new ArrayList<String>();
		boolean check = false;
		try {
			FileReader inputFile = null;
			inputFile = new FileReader("requests.txt");
			Scanner parser = new Scanner(inputFile);
			while (parser.hasNextLine()) {
				String line = parser.nextLine();
				if ("".equals(line))
					continue;
				StringTokenizer tokens = new StringTokenizer(line, ":");
				String type = tokens.nextToken();
				String from = tokens.nextToken();
				String to = tokens.nextToken();
				if ("send".equals(type)) {
					for (ServerReceiver i : cLists) {
						if (i.uname.equals(to)) {
							type = "receive";
							try {
								DataOutputStream outToClient = new DataOutputStream(i.cSocket.getOutputStream());
								outToClient.writeBytes("One friend request from : " + from + '\n');
							} catch (IOException ex) {
								Logger.getLogger(ServerSender.class.getName()).log(Level.SEVERE, null, ex);
							}
							check = true;
						}
					}
				}
				FR_LIST.add(type + ":" + from + ":" + to);
			}
			parser.close();
		} catch (FileNotFoundException ex) {
			Logger.getLogger(ServerSender.class.getName()).log(Level.SEVERE, null, ex);
		}
		if (check == true)
			resetFriendRequest(FR_LIST);
	}

	public void resetNotifications(ArrayList<String> resetN_LIST) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("notifications.txt", false));
		for (String i : resetN_LIST) {
			writer.write(i);
			writer.newLine();
		}
		writer.flush();
		writer.close();
	}
	

	public void handleSendNotifications() throws IOException {
		ArrayList<String> NOT_LIST = new ArrayList<String>();
		boolean check = false;
		try {
			FileReader inputFile = null;
			inputFile = new FileReader("notifications.txt");
			Scanner parser = new Scanner(inputFile);
			while (parser.hasNextLine()) {
				String line = parser.nextLine();
				if ("".equals(line))
					continue;
				StringTokenizer tokens = new StringTokenizer(line, ":");
				String type = tokens.nextToken();
				String to = tokens.nextToken();
				String notification = tokens.nextToken();
				if ("send".equals(type)) {
					for (ServerReceiver i : cLists) {
						if (i.uname.equals(to)) {
							type = "receive";
							try {
								DataOutputStream outToClient = new DataOutputStream(i.cSocket.getOutputStream());
								outToClient.writeBytes("Notifications: " + notification + '\n');
							} catch (IOException ex) {
								Logger.getLogger(ServerSender.class.getName()).log(Level.SEVERE, null, ex);
							}
							check = true;
						}
					}
				}
				NOT_LIST.add(type + ":" + to + ":" + notification);
			}
		} catch (FileNotFoundException ex) {
			Logger.getLogger(ServerSender.class.getName()).log(Level.SEVERE, null, ex);
		}
		if (check == true)
			resetNotifications(NOT_LIST);
	}

	public void resetMessages(ArrayList<String> resetMSG_LIST) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("message.txt", false));
		for (String i : resetMSG_LIST) {
			writer.write(i);
			writer.newLine();
		}
		writer.flush();
		writer.close();
	}

	public void handleSendMessages() throws IOException {
		ArrayList<String> MESEG_LIST = new ArrayList<String>();
		boolean check = false;
		try {
			FileReader inputFile = null;
			inputFile = new FileReader("message.txt");
			Scanner parser = new Scanner(inputFile);
			while (parser.hasNextLine()) {
				String line = parser.nextLine();
				if ("".equals(line))
					continue;
				StringTokenizer tokens = new StringTokenizer(line, ":");
				String type = tokens.nextToken();
				String from = tokens.nextToken();
				String to = tokens.nextToken();
				String msg = tokens.nextToken();
				if ("send".equals(type)) {
					for (ServerReceiver i : cLists) {
						if (i.uname.equals(to)) {
							type = "receive";
							try {
								DataOutputStream outToClient = new DataOutputStream(i.cSocket.getOutputStream());
								outToClient.writeBytes("Message from " + from + " : " + msg + '\n');
							} catch (IOException ex) {
								Logger.getLogger(ServerSender.class.getName()).log(Level.SEVERE, null, ex);
							}
							check = true;
						}
					}
				}
				MESEG_LIST.add(type + ":" + from + ":" + to + ":" + msg);
			}
		} catch (FileNotFoundException ex) {
			Logger.getLogger(ServerSender.class.getName()).log(Level.SEVERE, null, ex);
		}
		if (check == true) {
			resetMessages(MESEG_LIST);
		}
	}

	public void run() {
		while (true) {
			try {
				handleSendFriendRequest();
				handleSendMessages();
				handleSendNotifications();
			} catch (IOException ex) {
				Logger.getLogger(ServerSender.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}
