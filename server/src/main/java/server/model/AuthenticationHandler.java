package server.model;

import static server.model.Server.cLists;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthenticationHandler extends Thread {
	public Socket cSocket;
	private BufferedReader inFromClient;
	private DataOutputStream outToClient;

	// Constructor of the class
	public AuthenticationHandler(Socket cSocket) {
		this.cSocket = cSocket;
	}

	// Method to save all the details
	void saveDetails(String name, String uname, String pass) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("detail.txt", true));
		writer.newLine();
		writer.append(uname + ":" + pass + ":" + name);
		writer.close();
	}

	// Method to verify an user with username & password.
	public boolean validity1(String uname, String pass) {
		FileReader inputFile = null;
		Scanner parser = null;
		try {
			inputFile = new FileReader("detail.txt");
			parser = new Scanner(inputFile);
			while (parser.hasNextLine()) {
				String line = parser.nextLine();
				if ("".equals(line))
					continue;
				StringTokenizer tokens = new StringTokenizer(line, ":");
				String u = tokens.nextToken();
				String p = tokens.nextToken();
				if (uname.equals(u) && pass.equals(p))
					return true;
			}
			return false;
		} catch (FileNotFoundException ex) {
			Logger.getLogger(AuthenticationHandler.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				parser.close();
				inputFile.close();
			} catch (IOException ex) {
				Logger.getLogger(AuthenticationHandler.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		return false;
	}

	// Method to check if the registered username already exists
	public boolean usernameNotTaken(String uname) {
		FileReader inputFile = null;
		Scanner parser = null;
		try {
			inputFile = new FileReader("detail.txt");
			parser = new Scanner(inputFile);
			while (parser.hasNextLine()) {
				String line = parser.nextLine();
				if ("".equals(line))
					continue;
				StringTokenizer tokens = new StringTokenizer(line, ":");
				String u = tokens.nextToken();
				if ((u.equals(uname)))
					return true;
			}
			return false;
		} catch (FileNotFoundException ex) {
			Logger.getLogger(AuthenticationHandler.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				parser.close();
				inputFile.close();
			} catch (IOException ex) {
				Logger.getLogger(AuthenticationHandler.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		return false;
	}

	// Method to get the Full name of corresponding username.
	String getName(String uname) {
		String name = "";
		FileReader inputFile = null;
		Scanner parser = null;
		try {
			inputFile = new FileReader("detail.txt");
			parser = new Scanner(inputFile);
			while (parser.hasNextLine()) {
				String line = parser.nextLine();
				if ("".equals(line))
					continue;
				StringTokenizer tokens = new StringTokenizer(line, ":");
				String u = tokens.nextToken();
				name = tokens.nextToken();
				if (u.equals(uname))
					return name;
			}
		} catch (FileNotFoundException ex) {
			Logger.getLogger(AuthenticationHandler.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				parser.close();
				inputFile.close();
			} catch (IOException ex) {
				Logger.getLogger(AuthenticationHandler.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		return name;
	}

	// Run Method for this thread.
	public void run() {
		try {
			inFromClient = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
			outToClient = new DataOutputStream(cSocket.getOutputStream());
			String cmd = inFromClient.readLine();

			if ("login".equals(cmd)) {
				String data = inFromClient.readLine();
				StringTokenizer tokens = new StringTokenizer(data, ":");
				String uname = tokens.nextToken();
				String pass = tokens.nextToken();

				if (validity1(uname, pass)) {
					outToClient.writeBytes("ok" + '\n');
					String name = getName(uname);
					ServerReceiver CH = new ServerReceiver(cSocket, name, uname);
					CH.start();
					cLists.add(CH);
				} else
					outToClient.writeBytes("not ok" + '\n');
			} else if ("signup".equals(cmd)) {

				String data = inFromClient.readLine();
				StringTokenizer tokens = new StringTokenizer(data, ":");
				String uname = tokens.nextToken();
				String pass = tokens.nextToken();
				if (usernameNotTaken(uname))
					outToClient.writeBytes("not ok" + '\n');
				else {
					saveDetails("", uname, pass);
					outToClient.writeBytes("ok" + '\n');
					ServerReceiver CH = new ServerReceiver(cSocket, "", uname);
					cLists.add(CH);
					CH.start();
				}
			}
		} catch (IOException ex) {
			Logger.getLogger(AuthenticationHandler.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
