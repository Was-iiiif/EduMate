package com.edumate.app.model;

import java.io.Serializable;

public class Misconception implements Serializable {
    private String statement;
    private long messageId;
    private String citation;

    public Misconception() {}

    public Misconception(String statement, long messageId, String citation) {
        this.statement = statement;
        this.messageId = messageId;
        this.citation = citation;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public String getCitation() {
        return citation;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }
}
