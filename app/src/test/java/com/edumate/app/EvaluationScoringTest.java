package com.edumate.app;

import static org.junit.Assert.assertEquals;

import com.edumate.app.model.Evaluation;

import org.junit.Test;

public class EvaluationScoringTest {

    @Test
    public void testExactRubricCalculation_nominalScores() {
        // Accuracy 80 * 0.40 = 32.0
        // Completeness 70 * 0.25 = 17.5
        // Clarity 90 * 0.20 = 18.0
        // Reasoning 60 * 0.15 = 9.0
        // Total expected = 32.0 + 17.5 + 18.0 + 9.0 = 76.5
        double score = Evaluation.calculateOverallScore(80, 70, 90, 60);
        assertEquals(76.5, score, 0.001);
    }

    @Test
    public void testPerfectScores_equals100() {
        double score = Evaluation.calculateOverallScore(100, 100, 100, 100);
        assertEquals(100.0, score, 0.001);
    }

    @Test
    public void testZeroScores_equalsZero() {
        double score = Evaluation.calculateOverallScore(0, 0, 0, 0);
        assertEquals(0.0, score, 0.001);
    }

    @Test
    public void testOverMaxClamping_clampedTo100() {
        double score = Evaluation.calculateOverallScore(150, 120, 200, 105);
        assertEquals(100.0, score, 0.001);
    }

    @Test
    public void testUnderMinClamping_clampedToZero() {
        double score = Evaluation.calculateOverallScore(-50, -10, -5, -100);
        assertEquals(0.0, score, 0.001);
    }

    @Test
    public void testRoundingToTenth() {
        // Accuracy 85 * 0.40 = 34.0
        // Completeness 73 * 0.25 = 18.25
        // Clarity 91 * 0.20 = 18.2
        // Reasoning 77 * 0.15 = 11.55
        // Total = 34.0 + 18.25 + 18.2 + 11.55 = 82.0
        double score = Evaluation.calculateOverallScore(85, 73, 91, 77);
        assertEquals(82.0, score, 0.001);
    }
}
