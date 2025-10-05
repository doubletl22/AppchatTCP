package client.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import client.view.ClientShell;

public class ClientSender extends Thread {
	public Socket cSocket;
	private ClientShell clientShell;
	private DataOutputStream outToServer;

	public ClientSender(Socket cSocket) {
		this.cSocket = cSocket;
		try {
			outToServer = new DataOutputStream(cSocket.getOutputStream());
		} catch (IOException ex) {
			Logger.getLogger(ClientReceiver.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void run() {

	}

	public void sendMessage(String... args) {
		try {
			String command = args[0];
			String k = "";
			if (command.equals("Online User Lists")) {
				k = "Online User Lists" + '\n';
				outToServer.writeBytes(k);
			} else if (command.equals("Accept Request")) {
				k = "Accept Request" + '\n';
				outToServer.writeBytes(k);
				System.out.print("Accept Request of the user: ");
				String name = args[1];
				System.out.println();
				outToServer.writeBytes(name + '\n');

			} else if (command.equals("Send Request")) {
				k = "Send Request" + '\n';
				outToServer.writeBytes(k); // send command to the
											// server.

				System.out.print("Send Request to the user: ");
				String name = args[1];
				System.out.println();
				outToServer.writeBytes(name + '\n');

			} else if (command.equals("Unicast")) {
				outToServer.writeBytes(command + '\n');
				String name = args[1];

				outToServer.writeBytes(name + '\n');
				String msg = args[2];
				outToServer.writeBytes(msg + '\n');

				clientShell.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						clientShell.updateChatHistory("Me >> " + msg);
					}
				});
				
			} else if (command.equals("Multicast")) {
				k = "Multicast" + '\n';
				outToServer.writeBytes(k);

				System.out.print("Recipient: ");
				String name = args[1];
				System.out.println("");
				outToServer.writeBytes(name + '\n');

				System.out.print("Message: ");
				String msg = args[2];
				System.out.println("");
				outToServer.writeBytes(msg + '\n');

			} else if (command.equals("Broadcast")) {
				k = "Broadcast" + '\n';
				outToServer.writeBytes(k);

				System.out.print("Message: ");
				String msg = args[1];
				System.out.println("");
				
				outToServer.writeBytes(msg + '\n');
				
			} else if ("Log out".equals(command)) {
				k = "Log out" + '\n';
				outToServer.writeBytes(k);
				
			} else if("Blog post".equals(command)) {
				k = "Blog post" + '\n';
				outToServer.writeBytes(k);
				
				System.out.print("owner: ");
				String owner = args[1];
				System.out.println("");
				outToServer.writeBytes(owner + '\n');

				System.out.print("title: ");
				String title = args[2];
				System.out.println("");
				outToServer.writeBytes(title + '\n');
				
				System.out.print("content: ");
				String content = args[3];
				System.out.println("");
				outToServer.writeBytes(content + '\n');
				
			} else if("Load posts".equals(command)){
				k = "Load posts" + '\n';
				outToServer.writeBytes(k);
			} else if("Avatar changed".equals(command)){
				k = "Avatar changed" + '\n';
				outToServer.writeBytes(k);
				
				System.out.print("owner: ");
				String owner = args[1];
				System.out.println("");
				outToServer.writeBytes(owner + '\n');

				System.out.print("path: ");
				String path = args[2];
				System.out.println("");
				outToServer.writeBytes(path + '\n');
				
			} else if("Get avatar".equals(command)){
				k = "Get avatar" + '\n';
				outToServer.writeBytes(k);
				
				System.out.print("owner: ");
				String owner = args[1];
				System.out.println("");
				outToServer.writeBytes(owner + '\n');
			}
			
			outToServer.flush();
			
		} catch (IOException ex) {
			Logger.getLogger(ClientSender.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	public void setClientShell(ClientShell clientShell) {
		this.clientShell = clientShell;
	}
}
