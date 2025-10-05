package client.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import client.view.ClientShell;

public class ClientReceiver extends Thread {
	public Socket cSocket;
	private ClientShell clientShell;
	private BufferedReader inFromServer;

	// Constructor for the class.
	public ClientReceiver(Socket cSocket) {
		this.cSocket = cSocket;
		try {
			inFromServer = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
		} catch (IOException ex) {
			Logger.getLogger(ClientReceiver.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	// Run method for the thread.
	@SuppressWarnings("unchecked")
	public void run() {
		while (true) {
			try {
				inFromServer = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));

				String sentence = inFromServer.readLine();
				// Time to show the online user lists.
				if (sentence == null)
					continue;
				if (sentence.equals("Online User Lists")) {
					ObjectInputStream objectInput = new ObjectInputStream(cSocket.getInputStream());
					ArrayList<String> objecAsArrayList = new ArrayList<String>();
					objecAsArrayList = (ArrayList<String>) objectInput.readObject();
					String[] onlineUsers = objecAsArrayList.toArray(new String[0]);
					clientShell.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							clientShell.updateOnlineUsersList(onlineUsers);
						}
					});

				} else if (sentence.startsWith("Message from ")) {
					System.out.println(cSocket.getInetAddress() + " Received a message");
					StringTokenizer tokens = new StringTokenizer(sentence, ":");
					ArrayList<String> fromAndMessage = new ArrayList<String>();
					while (tokens.hasMoreTokens()) {
						String token = tokens.nextToken();
						fromAndMessage.add(token);
					}

					if (fromAndMessage.size() != 2) {
						continue;
					}

					String[] messages = fromAndMessage.toArray(new String[0]);
					clientShell.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							clientShell.updateChatHistory(
									messages[0].replaceAll("Message from ", "") + ">>" + messages[1]);
						}
					});
				} else if(sentence.equals("Load posts")){
					System.out.println("Loading blog posts ... ");
					ObjectInputStream objectInput = new ObjectInputStream(cSocket.getInputStream());
					ArrayList<String> objecAsArrayList = new ArrayList<String>();
					objecAsArrayList = (ArrayList<String>) objectInput.readObject();
					String[] blogPosts = objecAsArrayList.toArray(new String[0]);
					clientShell.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							clientShell.updateBlogPostsList(blogPosts);
						}
					});
				} else if(sentence.equals("Get avatar")){
					ObjectInputStream objectInput = new ObjectInputStream(cSocket.getInputStream());
					String avatarPath = (String) objectInput.readObject();
					clientShell.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							clientShell.updateAvatar(avatarPath);
						}
					});
				} else
					System.out.println(sentence);
			} catch (ClassNotFoundException ex) {
				Logger.getLogger(ClientReceiver.class.getName()).log(Level.SEVERE, null, ex);
			} catch (IOException ex) {
				Logger.getLogger(ClientReceiver.class.getName()).log(Level.SEVERE, null, ex);
			}

		}
	}

	public void setClientShell(ClientShell clientShell) {
		this.clientShell = clientShell;
	}
}
