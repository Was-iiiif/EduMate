package com.edumate.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.edumate.app.model.Evaluation;
import com.edumate.app.repository.AIRepository;

import org.junit.Test;

public class EvaluationJsonValidationTest {

    @Test
    public void testValidJsonSchema_parsesCorrectly() {
        String json = "{\n"
                + "  \"accuracy\": 85.0,\n"
                + "  \"completeness\": 70.0,\n"
                + "  \"clarity\": 90.0,\n"
                + "  \"reasoning\": 75.0,\n"
                + "  \"feedback\": \"Clear explanation of Calvin cycle.\",\n"
                + "  \"strengths\": [\"Well-structured intro\", \"Clear terminology\"],\n"
                + "  \"improvements\": [\"Explain light-independent ATP generation\"],\n"
                + "  \"misconceptions\": [\n"
                + "    {\"statement\": \"Water is split in dark reaction\", \"messageId\": 3, \"citation\": \"Learner said water splits in Calvin cycle\"}\n"
                + "  ]\n"
                + "}";

        Evaluation eval = AIRepository.parseAndValidateEvaluationJson(json);
        assertNotNull(eval);
        assertEquals(85.0, eval.getAccuracy(), 0.001);
        assertEquals(70.0, eval.getCompleteness(), 0.001);
        assertEquals(90.0, eval.getClarity(), 0.001);
        assertEquals(75.0, eval.getReasoning(), 0.001);
        // Overall score: (85*0.4)+(70*0.25)+(90*0.2)+(75*0.15) = 34 + 17.5 + 18 + 11.25 = 80.8
        assertEquals(80.8, eval.getOverallScore(), 0.1);
        assertEquals("Clear explanation of Calvin cycle.", eval.getFeedback());
        assertEquals(2, eval.getStrengths().size());
        assertEquals(1, eval.getImprovements().size());
        assertEquals(1, eval.getMisconceptions().size());
        assertEquals(3, eval.getMisconceptions().get(0).getMessageId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingAccuracyField_throwsException() {
        String invalidJson = "{\n"
                + "  \"completeness\": 70.0,\n"
                + "  \"clarity\": 90.0,\n"
                + "  \"reasoning\": 75.0\n"
                + "}";
        AIRepository.parseAndValidateEvaluationJson(invalidJson);
    }

    @Test
    public void testOutOfRangeScores_areClamped() {
        String jsonWithOutOfRange = "{\n"
                + "  \"accuracy\": 130.0,\n"
                + "  \"completeness\": -20.0,\n"
                + "  \"clarity\": 100.0,\n"
                + "  \"reasoning\": 80.0\n"
                + "}";

        Evaluation eval = AIRepository.parseAndValidateEvaluationJson(jsonWithOutOfRange);
        assertEquals(100.0, eval.getAccuracy(), 0.001);
        assertEquals(0.0, eval.getCompleteness(), 0.001);
        assertEquals(100.0, eval.getClarity(), 0.001);
        assertEquals(80.0, eval.getReasoning(), 0.001);
    }
}
