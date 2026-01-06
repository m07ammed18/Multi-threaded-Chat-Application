package com.chatapp.server;

import java.io.Serializable;

public class ChatMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  public enum MessageType {
    TEXT, FILE, USER_LIST
  }

  private MessageType type;
  private String sender;
  private String content;
  private byte[] fileData;
  private String fileName;
  private String targetUser;

  public ChatMessage(MessageType type, String sender, String content) {
    this.type = type;
    this.sender = sender;
    this.content = content;
  }

  // Getters and Setters
  public MessageType getType() {
    return type;
  }

  public void setType(MessageType type) {
    this.type = type;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public byte[] getFileData() {
    return fileData;
  }

  public void setFileData(byte[] fileData) {
    this.fileData = fileData;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getTargetUser() {
    return targetUser;
  }

  public void setTargetUser(String targetUser) {
    this.targetUser = targetUser;
  }
}