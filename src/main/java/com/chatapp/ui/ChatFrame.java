package com.chatapp.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.*;
import java.awt.PageAttributes.ColorType;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

import javax.swing.*;
import javax.swing.text.*;

import com.chatapp.server.ChatMessage;

public class ChatFrame extends JFrame {
  private JTextPane chatPane;
  private StyledDocument doc;
  private JTextField inputField;
  private JButton sendBtn, fileBtn;
  private JList<String> userList;
  private DefaultListModel<String> listModel;
  private ObjectOutputStream out;
  private ObjectInputStream in;
  private String username;

  public ChatFrame(String host, int port) {
    this.username = JOptionPane.showInputDialog(this, "Enter your username:", "Login", JOptionPane.PLAIN_MESSAGE);
    if (username == null || username.trim().isEmpty())
      System.exit(0);

    setupUI();
    connectToServer(host, port);
  }

  private void setupUI() {
    setTitle("Chat Application - " + username);
    setSize(800, 600);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setLocationRelativeTo(null);

    chatPane = new JTextPane();
    chatPane.setEditable(false);
    chatPane.setFont(new Font("SansSerif", Font.PLAIN, 14));
    doc = chatPane.getStyledDocument();

    listModel = new DefaultListModel<>();
    userList = new JList<>(listModel);
    userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    userList.setBorder(BorderFactory.createTitledBorder("Online Users"));
    userList.setPreferredSize(new Dimension(150, 0));

    inputField = new JTextField();
    inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));

    Icon msgIcon = new FlatSVGIcon("icons/msgIcon.svg", 16, 16);
    sendBtn = new JButton("Send" , msgIcon);
    
    Icon fileIcon = new FlatSVGIcon("icons/fileIcon.svg", 16, 16);
    fileBtn = new JButton("", fileIcon);

    JPanel bottom = new JPanel(new BorderLayout());
    bottom.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    JPanel btns = new JPanel(new GridLayout(1, 2));
    btns.add(fileBtn);
    btns.add(sendBtn);
    bottom.add(inputField, BorderLayout.CENTER);
    bottom.add(btns, BorderLayout.EAST);

    add(new JScrollPane(chatPane), BorderLayout.CENTER);
    add(new JScrollPane(userList), BorderLayout.EAST);
    add(bottom, BorderLayout.SOUTH);

    sendBtn.addActionListener(e -> sendMessage());
    fileBtn.addActionListener(e -> sendFile());

    // Double-click on the username to send a private message
    userList.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent evt) {
        if (evt.getClickCount() == 2) {
          String selected = userList.getSelectedValue();
          if (selected != null && !selected.equals(username)) {
            inputField.setText("@" + selected + ":");
            inputField.requestFocus();
          }
        }
      }
    });

    setVisible(true);
  }

  private void appendText(String text, Color color, boolean bold) {
    SimpleAttributeSet attrs = new SimpleAttributeSet();
    StyleConstants.setForeground(attrs, color);
    StyleConstants.setBold(attrs, bold);
    try {
      doc.insertString(doc.getLength(), text + "\n", attrs);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }

  private void appendImage(byte[] imageData, String sender) {
    appendText(sender + " sent an image:", Color.BLUE, true);
    ImageIcon icon = new ImageIcon(imageData);
    // Reduce the image size if it is too large.
    Image img = icon.getImage();
    int width = img.getWidth(null);
    int height = img.getHeight(null);
    if (width > 300) {
      double ratio = 300.0 / width;
      img = img.getScaledInstance(300, (int) (height * ratio), Image.SCALE_SMOOTH);
      icon = new ImageIcon(img);
    }

    chatPane.setCaretPosition(doc.getLength());
    chatPane.insertIcon(icon);
    try {
      doc.insertString(doc.getLength(), "\n", null);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }

  private void connectToServer(String host, int port) {
    try {
      Socket socket = new Socket(host, port);
      out = new ObjectOutputStream(socket.getOutputStream());
      in = new ObjectInputStream(socket.getInputStream());

      out.writeObject(new ChatMessage(ChatMessage.MessageType.TEXT, username, "Login"));

      new Thread(() -> {
        try {
          while (true) {
            ChatMessage msg = (ChatMessage) in.readObject();
            handleIncomingMessage(msg);
          }
        } catch (Exception e) {
          appendText("Disconnected from server.", Color.RED, true);
        }
      }).start();
    } catch (IOException e) {
      JOptionPane.showMessageDialog(this, "Could not connect to server: " + e.getMessage(), "Error",
          JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }
  }

  private void handleIncomingMessage(ChatMessage msg) {
    if (msg.getType() == ChatMessage.MessageType.USER_LIST) {
      SwingUtilities.invokeLater(() -> {
        listModel.clear();
        for (String user : msg.getContent().split(",")) {
          if (!user.isEmpty())
            listModel.addElement(user);
        }
      });
    } else if (msg.getType() == ChatMessage.MessageType.FILE) {
      String fileName = msg.getFileName().toLowerCase();
      if (fileName.endsWith(".jpg") || fileName.endsWith(".png") || fileName.endsWith(".gif")
          || fileName.endsWith(".jpeg")) {
        SwingUtilities.invokeLater(() -> appendImage(msg.getFileData(), msg.getSender()));
      } else {
        SwingUtilities.invokeLater(() -> {
          appendText(msg.getSender() + " sent a file: " + msg.getFileName(), Color.MAGENTA, true);
          saveFile(msg);
        });
      }
    } else {
      SwingUtilities.invokeLater(() -> appendText(msg.getSender() + ": " + msg.getContent(), Color.WHITE, false));
    }
  }

  private void sendMessage() {
    String text = inputField.getText().trim();
    if (text.isEmpty())
      return;

    ChatMessage msg = new ChatMessage(ChatMessage.MessageType.TEXT, username, text);
    if (text.startsWith("@")) {
      String[] parts = text.split(":", 2);
      if (parts.length == 2) {
        msg.setTargetUser(parts[0].substring(1));
        msg.setContent(parts[1]);
      }
    }

    try {
      out.writeObject(msg);
      appendText("Me: " + msg.getContent(), Color.WHITE, false);
      inputField.setText("");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void sendFile() {
    JFileChooser chooser = new JFileChooser();
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      try {
        byte[] data = Files.readAllBytes(file.toPath());
        ChatMessage msg = new ChatMessage(ChatMessage.MessageType.FILE, username, "File");
        msg.setFileData(data);
        msg.setFileName(file.getName());

        String selected = userList.getSelectedValue();
        if (selected != null && !selected.equals(username)) {
          msg.setTargetUser(selected);
        }

        out.writeObject(msg);

        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".jpg") || fileName.endsWith(".png") || fileName.endsWith(".gif")
            || fileName.endsWith(".jpeg")) {
          appendImage(data, "Me");
        } else {
          appendText("Me: Sent file " + file.getName(), Color.lightGray, false);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void saveFile(ChatMessage msg) {
    int choice = JOptionPane.showConfirmDialog(this, "Download file " + msg.getFileName() + "?");
    if (choice == JOptionPane.YES_OPTION) {
      JFileChooser chooser = new JFileChooser();
      chooser.setSelectedFile(new File(msg.getFileName()));
      if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        try (FileOutputStream fos = new FileOutputStream(chooser.getSelectedFile())) {
          fos.write(msg.getFileData());
          JOptionPane.showMessageDialog(this, "File saved!");
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
