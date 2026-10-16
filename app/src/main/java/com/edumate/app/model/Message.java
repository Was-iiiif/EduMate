package com.edumate.app.model;

import java.io.Serializable;

public class Message implements Serializable {
    public static final String SENDER_LEARNER = "LEARNER";
    public static final String SENDER_STUDENT = "STUDENT";

    private long id;
    private String sessionId;
    private String sender;
    private String messageText;
    private long createdAt;

    public Message() {
        this.createdAt = System.currentTimeMillis();
    }

    public Message(String sessionId, String sender, String messageText) {
        this.sessionId = sessionId;
        this.sender = sender;
        this.messageText = messageText;
        this.createdAt = System.currentTimeMillis();
    }

    public Message(long id, String sessionId, String sender, String messageText, long createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.sender = sender;
        this.messageText = messageText;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isFromLearner() {
        return SENDER_LEARNER.equalsIgnoreCase(sender);
    }
}
