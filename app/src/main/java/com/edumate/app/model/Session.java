package com.edumate.app.model;

import java.io.Serializable;
import java.util.UUID;

public class Session implements Serializable {
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";

    public static final String SYNC_PENDING = "PENDING";
    public static final String SYNC_SYNCED = "SYNCED";

    private String id;
    private String topic;
    private String persona;
    private long startedAt;
    private long endedAt;
    private String status;
    private double overallScore;
    private String syncStatus;
    private Long deletedAt; // Nullable for soft deletes

    public Session() {
        this.id = UUID.randomUUID().toString();
        this.startedAt = System.currentTimeMillis();
        this.status = STATUS_IN_PROGRESS;
        this.overallScore = 0.0;
        this.syncStatus = SYNC_PENDING;
        this.persona = Persona.BEGINNER.name();
    }

    public Session(String id, String topic, String persona, long startedAt, long endedAt,
                   String status, double overallScore, String syncStatus, Long deletedAt) {
        this.id = id;
        this.topic = topic;
        this.persona = persona;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.status = status;
        this.overallScore = overallScore;
        this.syncStatus = syncStatus;
        this.deletedAt = deletedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getPersona() {
        return persona != null ? persona : Persona.BEGINNER.name();
    }

    public void setPersona(String persona) {
        this.persona = persona;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(long endedAt) {
        this.endedAt = endedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public Long getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Long deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null && deletedAt > 0;
    }
}
