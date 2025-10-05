package server.model;

import static server.model.Server.cLists;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerReceiver extends Thread {
	public Socket cSocket;
	public String cName;
	public String uname;

	public ServerReceiver(Socket cSocket, String cName, String uname) {
		this.cSocket = cSocket;
		this.cName = cName;
		this.uname = uname;
	}


	void addFriends(String frdname) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("friends.txt", true));
		writer.write(uname + ":" + frdname);
		writer.newLine();
		writer.flush();
		writer.close();
	}

	void addFriendRequest(String frdname) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("requests.txt", true));
		writer.write("send:" + uname + ":" + frdname);
		writer.newLine();
		writer.flush();
		writer.close();
	}

	void requestConfirmation(String name) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("notifications.txt", true));
		writer.write("send:" + name + ":" + uname + " is your friend now.");
		writer.newLine();
		writer.flush();
		writer.close();
	}

	void giveMSG(String name, String msg, DataOutputStream outToClient, String cmd) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("message.txt", true));
		StringTokenizer tokens = new StringTokenizer(name, ":");
		while (tokens.hasMoreTokens()) {
			String next = tokens.nextToken();
			writer.write("send:" + uname + ":" + next + ":" + msg);
			writer.newLine();
		}
		writer.flush();
		writer.close();
	}


	boolean checkFriendship(String user1, String user2) throws IOException {
		FileReader inputFile = null;
		inputFile = new FileReader("friends.txt");
		Scanner parser = new Scanner(inputFile);

		while (parser.hasNextLine()) {
			String line = parser.nextLine();
			if ("".equals(line))
				continue;
			StringTokenizer tokens = new StringTokenizer(line, ":");
			String f = tokens.nextToken();
			String s = tokens.nextToken();
			if (f.equals(user1) && s.equals(user2)) {

			} else if (s.equals(user1) && f.equals(user2)) {
				parser.close();
				return true;
			}
		}
		parser.close();
		return false;
	}

	void broadcasting(String msg) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("message.txt", true));
		FileReader inputFile = null;
		inputFile = new FileReader("friends.txt");
		Scanner parser = new Scanner(inputFile);

		while (parser.hasNextLine()) {
			String line = parser.nextLine();
			if ("".equals(line))
				continue;
			StringTokenizer tokens = new StringTokenizer(line, ":");
			String f = tokens.nextToken();
			String s = tokens.nextToken();
			if (f.equals(uname)) {
				writer.write("send:" + f + ":" + s + ":" + msg);
				writer.newLine();
			} else if (s.equals(uname)) {
				writer.write("send:" + s + ":" + f + ":" + msg);
				writer.newLine();
			}
		}
		writer.flush();
		writer.close();
		parser.close();
	}

	public void delUser() {
		for (ServerReceiver i : cLists) {
			if (i.uname.equals(uname)) {
				cLists.remove(i);
				break;
			}
		}
	}
	
	private void addBlogPost(String owner, String title, String content, String cmd) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("blogs.txt", true));
		writer.write(owner + ":" + title + ":" + content);
		writer.newLine();
		writer.flush();
		writer.close();
	}
	
	private void saveAvatar(String owner, String path) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("avatars.txt", true));
		writer.append(owner + ":::" + path);
		writer.append('\n');
		writer.flush();
		writer.close();
	}
	
	public void run() {
		try {
			while (true) {
				DataOutputStream outToClient = new DataOutputStream(cSocket.getOutputStream());
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
				String cmd = inFromClient.readLine();

				if ("Online User Lists".equals(cmd)) {
					outToClient.writeBytes(cmd + '\n');
					ArrayList<String> allUsers = new ArrayList<String>();
					for (ServerReceiver i : cLists) {
						String names = i.uname;
						if (!allUsers.contains(names))
							allUsers.add(names);
					}
					ObjectOutputStream outputObject = new ObjectOutputStream(cSocket.getOutputStream());
					outputObject.writeObject(allUsers);
					// NOTE: closing ObjectOutputStream will also close 
					// the associated socket (i,e cSocket). So don't close it
				} else if ("Accept Request".equals(cmd)) {
					String name = inFromClient.readLine();
					addFriends(name);
					outToClient.writeBytes("New Friendship with " + name + '\n');
					requestConfirmation(name);
				} else if ("Send Request".equals(cmd)) {
					String name = inFromClient.readLine();
					addFriendRequest(name);
				} else if ("Unicast".equals(cmd)) {
					String user = inFromClient.readLine();
					String msg = inFromClient.readLine();
					giveMSG(user, msg, outToClient, cmd);
				} else if ("Multicast".equals(cmd)) {
					String user = inFromClient.readLine();
					String msg = inFromClient.readLine();
					giveMSG(user, msg, outToClient, cmd);
				} else if ("Broadcast".equals(cmd)) {
					String msg = inFromClient.readLine();
					broadcasting(msg);
				} else if ("Log out".equals(cmd)) {
					delUser();
					outToClient.writeBytes("Logged out of the system." + '\n');
					break;
				} else if("Blog post".equals(cmd)) {		
					String owner = inFromClient.readLine();
					String title = inFromClient.readLine();
					String content = inFromClient.readLine();
					addBlogPost(owner, title, content, cmd);
				} else if("Load posts".equals(cmd)) {
					outToClient.writeBytes(cmd + '\n');
					
					ArrayList<String> allBlogPosts = new ArrayList<String>();
					FileReader inputFile = new FileReader("blogs.txt");
					Scanner parser = new Scanner(inputFile);
					while (parser.hasNextLine()) {
						String line = parser.nextLine();
						if ("".equals(line))
							continue;
						allBlogPosts.add(line);
					}
					ObjectOutputStream outputObject = new ObjectOutputStream(cSocket.getOutputStream());
					outputObject.writeObject(allBlogPosts);
					outToClient.writeBytes("Blog posts loaded." + '\n');
					parser.close();
					
				} else if("Avatar changed".equals(cmd)){
					String owner = inFromClient.readLine();
					String path = inFromClient.readLine();
					saveAvatar(owner, path);
					
				} else if("Get avatar".equals(cmd)){
					outToClient.writeBytes(cmd + '\n');
					
					String owner = inFromClient.readLine();
					FileReader inputFile = new FileReader("avatars.txt");
					Scanner parser = new Scanner(inputFile);
					while (parser.hasNextLine()) {
						String line = parser.nextLine();
						if ("".equals(line))
							continue;
						if(line.split(":::")[0].equals(owner)){
							ObjectOutputStream outputObject = new ObjectOutputStream(cSocket.getOutputStream());
							String pathToAvatar = line.split(":::")[1];
							outputObject.writeObject(pathToAvatar);
							outToClient.writeBytes("Avatar loaded. " + '\n');
						}
					}
					parser.close();
				}
			}
		} catch (IOException ex) {
			Logger.getLogger(ServerReceiver.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				cSocket.close();
			} catch (IOException ex) {
				Logger.getLogger(ServerReceiver.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}