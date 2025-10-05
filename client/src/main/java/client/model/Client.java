package client.model;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
	private ClientSender sendClientRequests;
	private ClientReceiver receiveClientRequests;

	// Method for user registration
	public boolean signUp(String name, String uname, String pass) {
		try {
			// Create Client Socket.
			Socket cSocket = new Socket("localhost", 4445);
			// input & output stream with wrapper.
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
			DataOutputStream outToServer = new DataOutputStream(cSocket.getOutputStream());
			// Send to Server the Command for Registration.
			outToServer.writeBytes("signup" + '\n');
			// Convert the name,username & password into a string & send to
			// server.
			String full_msg = uname + ":" + pass + ":" + name;
			outToServer.writeBytes(full_msg + '\n');
			// Acknowledgement from Server.
			String ack = inFromServer.readLine();

			// If acknowledgement matched.
			if (ack.equals("ok")) {
				// Print Success message.
				System.out.println("Registration Successful.\n");
				// Two thread operations: Send & Receive for one user.
				sendClientRequests = new ClientSender(cSocket);
				sendClientRequests.start();
				receiveClientRequests = new ClientReceiver(cSocket);
				receiveClientRequests.start();

				return true;
			} else {
				cSocket.close();
				System.out.println("Unsuccessful. Please Try Again.\n");
				return false;
			}
		} catch (IOException ex) {
			Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
		}
		return false;
	}

	// Method for user Login.
	public boolean signIn(String username, String pass) {
		try {
			// Create Client Socket.
			Socket cSocket = new Socket("localhost", 4445);
			// input & output stream with wrapper.
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
			DataOutputStream outToServer = new DataOutputStream(cSocket.getOutputStream());
			// Send to Server the command for Login.
			outToServer.writeBytes("login" + '\n');
			// Convert user name & password into a String and send to server.
			String full_msg = username + ":" + pass;
			outToServer.writeBytes(full_msg + '\n');
			// Acknowledgement from Server.
			String ack = inFromServer.readLine();

			if (ack.equals("ok")) {
				// Print Success message.
				System.out.println("Login Successful.\n");
				sendClientRequests = new ClientSender(cSocket);
				sendClientRequests.start();
				receiveClientRequests = new ClientReceiver(cSocket);
				receiveClientRequests.start();
				return true;
			} else {
				// Print Unsuccessful Message.
				System.out.println("Login Unsuccessful. Try Again\n");
				cSocket.close();
				return false;
			}
		} catch (IOException ex) {
			Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
		}
		return false;
	}

	public ClientSender getClientSendHandler() {
		return sendClientRequests;
	}

	public ClientReceiver getClientReceiveHandler() {
		return receiveClientRequests;
	}

}