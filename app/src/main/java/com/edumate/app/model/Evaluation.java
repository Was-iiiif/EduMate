package com.edumate.app.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Evaluation implements Serializable {
    public static final double WEIGHT_ACCURACY = 0.40;
    public static final double WEIGHT_COMPLETENESS = 0.25;
    public static final double WEIGHT_CLARITY = 0.20;
    public static final double WEIGHT_REASONING = 0.15;

    private long id;
    private String sessionId;
    private double accuracy;
    private double completeness;
    private double clarity;
    private double reasoning;
    private double overallScore;
    private String feedback;
    private List<String> strengths;
    private List<String> improvements;
    private List<Misconception> misconceptions;
    private long createdAt;

    public Evaluation() {
        this.strengths = new ArrayList<>();
        this.improvements = new ArrayList<>();
        this.misconceptions = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public Evaluation(String sessionId, double accuracy, double completeness, double clarity,
                      double reasoning, String feedback, List<String> strengths,
                      List<String> improvements, List<Misconception> misconceptions) {
        this.sessionId = sessionId;
        this.accuracy = clamp(accuracy);
        this.completeness = clamp(completeness);
        this.clarity = clamp(clarity);
        this.reasoning = clamp(reasoning);
        this.overallScore = calculateOverallScore(this.accuracy, this.completeness, this.clarity, this.reasoning);
        this.feedback = feedback;
        this.strengths = strengths != null ? strengths : new ArrayList<>();
        this.improvements = improvements != null ? improvements : new ArrayList<>();
        this.misconceptions = misconceptions != null ? misconceptions : new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public static double calculateOverallScore(double accuracy, double completeness, double clarity, double reasoning) {
        double acc = clamp(accuracy);
        double comp = clamp(completeness);
        double clar = clamp(clarity);
        double reas = clamp(reasoning);
        double score = (acc * WEIGHT_ACCURACY)
                + (comp * WEIGHT_COMPLETENESS)
                + (clar * WEIGHT_CLARITY)
                + (reas * WEIGHT_REASONING);
        return Math.round(score * 10.0) / 10.0;
    }

    public static double clamp(double value) {
        if (value < 0.0) return 0.0;
        if (value > 100.0) return 100.0;
        return value;
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

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = clamp(accuracy);
        this.overallScore = calculateOverallScore(this.accuracy, this.completeness, this.clarity, this.reasoning);
    }

    public double getCompleteness() {
        return completeness;
    }

    public void setCompleteness(double completeness) {
        this.completeness = clamp(completeness);
        this.overallScore = calculateOverallScore(this.accuracy, this.completeness, this.clarity, this.reasoning);
    }

    public double getClarity() {
        return clarity;
    }

    public void setClarity(double clarity) {
        this.clarity = clamp(clarity);
        this.overallScore = calculateOverallScore(this.accuracy, this.completeness, this.clarity, this.reasoning);
    }

    public double getReasoning() {
        return reasoning;
    }

    public void setReasoning(double reasoning) {
        this.reasoning = clamp(reasoning);
        this.overallScore = calculateOverallScore(this.accuracy, this.completeness, this.clarity, this.reasoning);
    }

    public double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths != null ? strengths : new ArrayList<>();
    }

    public List<String> getImprovements() {
        return improvements;
    }

    public void setImprovements(List<String> improvements) {
        this.improvements = improvements != null ? improvements : new ArrayList<>();
    }

    public List<Misconception> getMisconceptions() {
        return misconceptions;
    }

    public void setMisconceptions(List<Misconception> misconceptions) {
        this.misconceptions = misconceptions != null ? misconceptions : new ArrayList<>();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
