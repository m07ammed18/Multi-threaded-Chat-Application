package com.chatapp.client;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.chatapp.ui.ChatFrame;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;

public class ClientMain {
  public static void main(String[] args) {
    // Flatlaf theme setup
    try {
      UIManager.setLookAndFeel(new FlatMacDarkLaf());
    } catch (Exception ex) {
      System.err.println("Failed to initialize LaF");
    }

    SwingUtilities.invokeLater(() -> new ChatFrame("localhost", 5000));
  }
}
