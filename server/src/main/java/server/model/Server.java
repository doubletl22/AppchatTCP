package server.model;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
	// Arraylist to store the online users
	public static ArrayList<ServerReceiver> cLists = new ArrayList<ServerReceiver>();
	private static ServerSocket sSocket;

	public static void main(String[] args) {
		try {
			// Create Server Socket.
			sSocket = new ServerSocket(4445);
			// This thread will always keep checking
			ServerSender obj = new ServerSender();
			obj.start();

			System.out.println("Server started");
			// Continuously accept new client if get one.
			while (true) {
				Socket connectionSocket = sSocket.accept();
				AuthenticationHandler newlogIn = new AuthenticationHandler(connectionSocket);
				newlogIn.start();
			}

		} catch (IOException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			closeServerSocket();
		}
	}

	public static void closeServerSocket() {
		try {
			sSocket.close();
		} catch (IOException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
