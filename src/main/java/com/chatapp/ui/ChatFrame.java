package com.chatapp.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.text.*;
import com.chatapp.server.ChatMessage;

public class ChatFrame extends JFrame {
  private JTextPane chatPane;
  private StyledDocument doc;
  private JTextArea inputField;
  private JButton sendBtn, fileBtn;
  private JList<String> userList;
  private DefaultListModel<String> listModel;
  private JLabel statusLabel;
  private ObjectOutputStream out;
  private ObjectInputStream in;
  private String username;
  private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

  // Colors for Dark Mode
  private final Color BG_COLOR = new Color(30, 30, 30);
  private final Color INPUT_BG = new Color(45, 45, 45);
  private final Color TEXT_COLOR = new Color(220, 220, 220);
  private final Color MY_MSG_COLOR = new Color(75, 181, 67);
  private final Color OTHER_MSG_COLOR = new Color(66, 135, 245);
  private final Color SYSTEM_MSG_COLOR = new Color(180, 180, 180);

  public ChatFrame(String host, int port) {
    this.username = JOptionPane.showInputDialog(this, "Enter your username:", "Login", JOptionPane.PLAIN_MESSAGE);
    if (username == null || username.trim().isEmpty())
      System.exit(0);

    setupUI();
    connectToServer(host, port);
  }

  private void setupUI() {
    setTitle("Chat Application - " + username);
    setSize(900, 650);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setLocationRelativeTo(null);
    getContentPane().setBackground(BG_COLOR);

    // Chat Area
    chatPane = new JTextPane();
    chatPane.setEditable(false);
    chatPane.setBackground(BG_COLOR);
    chatPane.setForeground(TEXT_COLOR);
    chatPane.setBorder(new EmptyBorder(10, 10, 10, 10));
    doc = chatPane.getStyledDocument();

    // Users List
    listModel = new DefaultListModel<>();
    userList = new JList<>(listModel);
    userList.setBackground(INPUT_BG);
    userList.setForeground(TEXT_COLOR);
    userList.setSelectionBackground(OTHER_MSG_COLOR);
    userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    userList.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(SYSTEM_MSG_COLOR),
        "Online Users", 0, 0, null, SYSTEM_MSG_COLOR));
    userList.setPreferredSize(new Dimension(180, 0));

    // Input Field
    inputField = new JTextArea(1, 25);
    inputField.setLineWrap(true);
    inputField.setWrapStyleWord(true);
    inputField.setBackground(INPUT_BG);
    inputField.setForeground(TEXT_COLOR);
    inputField.setCaretColor(TEXT_COLOR);
    inputField.setBorder(new EmptyBorder(5, 5, 5, 5));

    // Input Behavior: Enter to send, Shift+Enter for new line
    inputField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          if (e.isShiftDown()) {
            inputField.append("\n");
          } else {
            e.consume();
            sendMessage();
          }
        }
      }
    });

    // Buttons Logic
    Icon msgIcon = new FlatSVGIcon("icons/msgIcon.svg", 16, 16);
    sendBtn = new JButton("Send", msgIcon);

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

    inputField.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent e) {
        checkInput();
      }

      public void removeUpdate(DocumentEvent e) {
        checkInput();
      }

      public void changedUpdate(DocumentEvent e) {
        checkInput();
      }

      private void checkInput() {
        SwingUtilities.invokeLater(() -> sendBtn.setEnabled(!inputField.getText().trim().isEmpty()));
      }
    });

    // Status Bar
    statusLabel = new JLabel("Connecting...");
    statusLabel.setForeground(SYSTEM_MSG_COLOR);
    statusLabel.setBorder(new EmptyBorder(5, 10, 5, 10));

    // Layout
    JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
    bottomPanel.setBackground(BG_COLOR);
    bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
    buttonPanel.setBackground(BG_COLOR);
    buttonPanel.add(fileBtn);
    buttonPanel.add(sendBtn);

    bottomPanel.add(new JScrollPane(inputField), BorderLayout.CENTER);
    bottomPanel.add(buttonPanel, BorderLayout.EAST);

    add(new JScrollPane(chatPane), BorderLayout.CENTER);
    add(userList, BorderLayout.EAST);
    add(bottomPanel, BorderLayout.SOUTH);
    add(statusLabel, BorderLayout.NORTH);

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

    // Focus management
    addWindowListener(new WindowAdapter() {
      public void windowOpened(WindowEvent e) {
        inputField.requestFocus();
      }
    });

    setVisible(true);
  }

  private void appendMessage(String sender, String content, Color color, int alignment) {
    SwingUtilities.invokeLater(() -> {
      try {
        String timestamp = "[" + timeFormat.format(new Date()) + "] ";
        SimpleAttributeSet alignAttr = new SimpleAttributeSet();
        StyleConstants.setAlignment(alignAttr, alignment);
        int start = doc.getLength();

        // Timestamp
        SimpleAttributeSet timeAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(timeAttr, SYSTEM_MSG_COLOR);
        StyleConstants.setItalic(timeAttr, true);
        doc.insertString(doc.getLength(), timestamp, timeAttr);

        // Sender
        SimpleAttributeSet senderAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(senderAttr, color);
        StyleConstants.setBold(senderAttr, true);
        doc.insertString(doc.getLength(), sender + ": ", senderAttr);

        // Content
        SimpleAttributeSet contentAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(contentAttr, TEXT_COLOR);
        doc.insertString(doc.getLength(), content + "\n", contentAttr);

        doc.setParagraphAttributes(start, doc.getLength() - start, alignAttr, false);
        chatPane.setCaretPosition(doc.getLength());
      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    });
  }

  private void appendSystemMessage(String content) {
    SwingUtilities.invokeLater(() -> {
      try {
        SimpleAttributeSet alignAttr = new SimpleAttributeSet();
        StyleConstants.setAlignment(alignAttr, StyleConstants.ALIGN_CENTER);
        int start = doc.getLength();

        SimpleAttributeSet sysAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(sysAttr, SYSTEM_MSG_COLOR);
        StyleConstants.setItalic(sysAttr, true);
        doc.insertString(doc.getLength(), "— " + content + " —\n", sysAttr);

        doc.setParagraphAttributes(start, doc.getLength() - start, alignAttr, false);
      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    });
  }

  private void appendImage(byte[] imageData, String sender, int alignment, Color senderColor) {
    SwingUtilities.invokeLater(() -> {
      try {
        String timestamp = "[" + timeFormat.format(new Date()) + "] ";
        SimpleAttributeSet alignAttr = new SimpleAttributeSet();
        StyleConstants.setAlignment(alignAttr, alignment);
        int start = doc.getLength();

        // Timestamp
        SimpleAttributeSet timeAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(timeAttr, SYSTEM_MSG_COLOR);
        StyleConstants.setItalic(timeAttr, true);
        doc.insertString(doc.getLength(), timestamp, timeAttr);

        // Sender
        SimpleAttributeSet senderAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(senderAttr, senderColor);
        StyleConstants.setBold(senderAttr, true);
        doc.insertString(doc.getLength(), sender + " sent an image:\n", senderAttr);

        // Image
        ImageIcon icon = new ImageIcon(imageData);
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
        doc.insertString(doc.getLength(), "\n", null);

        doc.setParagraphAttributes(start, doc.getLength() - start, alignAttr, false);
        chatPane.setCaretPosition(doc.getLength());
      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    });
  }

  private void appendFileDownload(ChatMessage msg, int alignment, Color senderColor) {
    SwingUtilities.invokeLater(() -> {
      try {
        String timestamp = "[" + timeFormat.format(new Date()) + "] ";
        SimpleAttributeSet alignAttr = new SimpleAttributeSet();
        StyleConstants.setAlignment(alignAttr, alignment);
        int start = doc.getLength();

        // Timestamp
        SimpleAttributeSet timeAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(timeAttr, SYSTEM_MSG_COLOR);
        StyleConstants.setItalic(timeAttr, true);
        doc.insertString(doc.getLength(), timestamp, timeAttr);

        // Sender
        SimpleAttributeSet senderAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(senderAttr, senderColor);
        StyleConstants.setBold(senderAttr, true);
        doc.insertString(doc.getLength(), msg.getSender() + " sent a file: " + msg.getFileName() + " ", senderAttr);

        // Download Button
        Icon downloadIcon = new FlatSVGIcon("icons/downloadBtn.svg", 12, 12);
        JButton downloadBtn = new JButton(downloadIcon);
        downloadBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        downloadBtn.addActionListener(e -> saveFile(msg));

        chatPane.setCaretPosition(doc.getLength());
        chatPane.insertComponent(downloadBtn);

        doc.insertString(doc.getLength(), "\n", null);
        doc.setParagraphAttributes(start, doc.getLength() - start, alignAttr, false);
        chatPane.setCaretPosition(doc.getLength());
      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    });
  }

  private void connectToServer(String host, int port) {
    new Thread(() -> {
      try {
        Socket socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        out.writeObject(new ChatMessage(ChatMessage.MessageType.TEXT, username, "Login"));

        SwingUtilities.invokeLater(() -> {
          statusLabel.setText("Connected as " + username);
          statusLabel.setForeground(MY_MSG_COLOR);
        });

        while (true) {
          ChatMessage msg = (ChatMessage) in.readObject();
          handleIncomingMessage(msg);
        }
      } catch (Exception e) {
        SwingUtilities.invokeLater(() -> {
          statusLabel.setText("Disconnected from server.");
          statusLabel.setForeground(Color.RED);
        });
        appendSystemMessage("Connection lost.");
      }
    }).start();
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
        appendImage(msg.getFileData(), msg.getSender(), StyleConstants.ALIGN_LEFT, OTHER_MSG_COLOR);
      } else {
        appendFileDownload(msg, StyleConstants.ALIGN_LEFT, OTHER_MSG_COLOR);
      }
    } else {
      if (msg.getSender().equals("Server")) {
        appendSystemMessage(msg.getContent());
      } else {
        appendMessage(msg.getSender(), msg.getContent(), OTHER_MSG_COLOR, StyleConstants.ALIGN_LEFT);
      }
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
      appendMessage("Me", text, MY_MSG_COLOR, StyleConstants.ALIGN_RIGHT);
      inputField.setText("");
      inputField.requestFocus();
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
          appendImage(data, "Me", StyleConstants.ALIGN_RIGHT, MY_MSG_COLOR);
        } else {
          appendFileDownload(msg, StyleConstants.ALIGN_RIGHT, MY_MSG_COLOR);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void saveFile(ChatMessage msg) {
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new File(msg.getFileName()));
    if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      try (FileOutputStream fos = new FileOutputStream(chooser.getSelectedFile())) {
        fos.write(msg.getFileData());
        appendSystemMessage("File '" + msg.getFileName() + "' saved successfully.");
      } catch (IOException e) {
        appendSystemMessage("Error saving file: " + e.getMessage());
      }
    }
  }
}
