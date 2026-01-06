package com.chatapp.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class ChatServer {
  private ServerSocket serverSocket;
  private final Set<ClientHandler> clients = new HashSet<>();

  public ChatServer(int port) throws IOException {
    serverSocket = new ServerSocket(port);
    System.out.println("Server started on port " + port);
  }

  public void start() {
    while (true) {
      try {
        Socket socket = serverSocket.accept();
        System.out.println("New client connected: " + socket.getInetAddress());
        ClientHandler handler = new ClientHandler(socket, this);
        clients.add(handler);
        new Thread(handler).start();
      } catch (IOException e) {
        System.err.println("Error accepting client connection: " + e.getMessage());
      }
    }
  }

  public synchronized void broadcast(ChatMessage message, ClientHandler sender) {
    for (ClientHandler client : clients) {
      if (client != sender) {
        client.sendMessage(message);
      }
    }
  }

  public synchronized void updateUsersList() {
    StringBuilder sb = new StringBuilder();
    for (ClientHandler client : clients) {
      if (client.getUsername() != null) {
        sb.append(client.getUsername()).append(",");
      }
    }

    ChatMessage listMsg = new ChatMessage(ChatMessage.MessageType.USER_LIST, "Server", sb.toString());
    for (ClientHandler client : clients) {
      client.sendMessage(listMsg);
    }
  }

  public synchronized void privateMessage(String targetUser, ChatMessage message, ClientHandler sender) {
    boolean found = false;
    for (ClientHandler client : clients) {
      if (client.getUsername() != null && client.getUsername().equals(targetUser)) {
        client.sendMessage(message);
        found = true;
        break;
      }
    }
    if (!found) {
      // sender.sendMessage("User not found.");
    }
  }

  public synchronized void privateMessage(String targetUser, String message, ClientHandler sender) {
    boolean found = false;
    for (ClientHandler client : clients) {
        if (client.getUsername() != null && client.getUsername().equals(targetUser)) {
            ChatMessage privateMsg = new ChatMessage(ChatMessage.MessageType.TEXT, "Server", "[Private from " + sender.getUsername() + "]: " + message);
            client.sendMessage(privateMsg);
            found = true;
            break;
        }
    }
    if (!found) {
        ChatMessage notFoundMsg = new ChatMessage(ChatMessage.MessageType.TEXT, "Server", "User " + targetUser + " not found.");
        sender.sendMessage(notFoundMsg);
    }
}

  public synchronized void removeClient(ClientHandler client) {
    clients.remove(client);
    if (client.getUsername() != null) {
      broadcast(new ChatMessage(ChatMessage.MessageType.TEXT, "Server", "User " + client.getUsername() + " left."), null);
      updateUsersList();
    }
  }

  public static void main(String[] args) {
    try {
      ChatServer server = new ChatServer(5000);
      server.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
