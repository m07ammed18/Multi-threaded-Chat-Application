package com.chatapp.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
  private Socket socket;
  private ChatServer server;
  private ObjectOutputStream out;
  private ObjectInputStream in;
  private String username;

  public ClientHandler(Socket socket, ChatServer server) {
    this.socket = socket;
    this.server = server;
  }

  @Override
  public void run() {
    try {
      out = new ObjectOutputStream(socket.getOutputStream());
      in = new ObjectInputStream(socket.getInputStream());

      // Receive username (first text message)
      ChatMessage loginMsg = (ChatMessage) in.readObject();
      this.username= loginMsg.getSender();

      server.updateUsersList();
      server.broadcast(new ChatMessage(ChatMessage.MessageType.TEXT, "Server", "User " + username + " joined!"), this);

      ChatMessage msg;
      while ((msg = (ChatMessage) in.readObject()) != null) {
        if (msg.getTargetUser() != null) {
                    server.privateMessage(msg.getTargetUser(), msg, this);
                } else {
                    server.broadcast(msg, this);
                }
      }
    } catch (Exception e) {
      System.out.println("Client disconnected.");
    } finally {
      server.removeClient(this);
      try { socket.close(); } catch (IOException e) {}
    }
  }

  public void sendMessage(ChatMessage msg) {
    try {
      out.writeObject(msg);
      out.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String getUsername() { return username; }
}
