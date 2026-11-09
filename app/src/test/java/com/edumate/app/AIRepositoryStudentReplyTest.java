package com.edumate.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.edumate.app.model.Message;
import com.edumate.app.repository.AIRepository;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AIRepositoryStudentReplyTest {
    private AIRepository aiRepository;

    @Before
    public void setUp() {
        aiRepository = AIRepository.getInstance(null);
    }

    @Test
    public void testOpeningTurn_generatesIntroductionQuestion() {
        List<Message> history = new ArrayList<>();
        String reply = aiRepository.generateFallbackStudentReply("Photosynthesis", history);
        assertNotNull(reply);
        assertTrue(reply.contains("Photosynthesis"));
        assertTrue(reply.contains("simple terms") || reply.contains("explain"));
    }

    @Test
    public void testUserSaysIDontKnow_givesScaffoldingHintAndAdaptiveQuestion() {
        List<Message> history = new ArrayList<>();
        history.add(new Message("s1", Message.SENDER_STUDENT, "What does photosynthesis mean in simple terms?"));
        history.add(new Message("s1", Message.SENDER_LEARNER, "I DON'T KNOW"));

        String reply = aiRepository.generateFallbackStudentReply("Photosynthesis", history);
        assertNotNull(reply);
        // Must NOT return the old canned generic switch string
        assertFalse(reply.contains("Oh, that's interesting! But I got a bit confused about how it works inside"));
        // Must give scaffolding/clue
        assertTrue(reply.toLowerCase().contains("clue")
                || reply.toLowerCase().contains("hint")
                || reply.toLowerCase().contains("leaf")
                || reply.toLowerCase().contains("kitchen")
                || reply.toLowerCase().contains("chloroplast"));
    }

    @Test
    public void testUserSaysIdkOrNoIdea_isRecognizedAsUncertainty() {
        List<Message> history = new ArrayList<>();
        history.add(new Message("s1", Message.SENDER_STUDENT, "Define Quicksort."));
        history.add(new Message("s1", Message.SENDER_LEARNER, "no idea"));

        String reply = aiRepository.generateFallbackStudentReply("Quicksort", history);
        assertNotNull(reply);
        assertTrue(reply.toLowerCase().contains("clue") || reply.toLowerCase().contains("pivot") || reply.toLowerCase().contains("benchmark"));
    }

    @Test
    public void testUserProvidesMeaningfulExplanation_quotesLearnerAndProbesNextConcept() {
        List<Message> history = new ArrayList<>();
        history.add(new Message("s1", Message.SENDER_STUDENT, "Could you start from the very beginning and explain what Photosynthesis means in simple terms?"));
        history.add(new Message("s1", Message.SENDER_LEARNER, "Plants use solar energy from the sun to synthesize glucose and chemical energy"));

        String reply = aiRepository.generateFallbackStudentReply("Photosynthesis", history);
        assertNotNull(reply);
        // Should quote or acknowledge user's phrase
        assertTrue(reply.contains("Plants use solar energy") || reply.contains("glucose") || reply.contains("synthesize"));
        // Should probe deeper or next concept
        assertTrue(reply.contains("?") && reply.length() > 20);
    }

    @Test
    public void testUserGivesOneWordAnswer_promptsToElaborate() {
        List<Message> history = new ArrayList<>();
        history.add(new Message("s1", Message.SENDER_STUDENT, "How does the pivot work?"));
        history.add(new Message("s1", Message.SENDER_LEARNER, "yes"));

        String reply = aiRepository.generateFallbackStudentReply("Quicksort", history);
        assertNotNull(reply);
        assertTrue(reply.contains("'yes'"));
        assertTrue(reply.contains("explain") || reply.contains("picture") || reply.contains("why"));
    }

    @Test
    public void testSanitizeApiKey_stripsQuotesAndWhitespace() {
        assertEquals("AIzaSyTest123", AIRepository.sanitizeApiKey("  \"AIzaSyTest123\"  "));
        assertEquals("AIzaSyTest123", AIRepository.sanitizeApiKey("'AIzaSyTest123'"));
        assertEquals("AIzaSyTest123", AIRepository.sanitizeApiKey("AIzaSy\nTest123\r "));
        assertEquals("", AIRepository.sanitizeApiKey(null));
        assertEquals("", AIRepository.sanitizeApiKey("\"\""));
    }

    @Test
    public void testFormatApiError_decodes403Correctly() {
        String invalidKeyJson = "{\"error\":{\"code\":403,\"message\":\"API key not valid. Please pass a valid API key.\",\"status\":\"PERMISSION_DENIED\"}}";
        String formatted = AIRepository.formatApiError(403, invalidKeyJson);
        assertTrue(formatted.contains("Invalid API Key"));

        String blockedJson = "{\"error\":{\"code\":403,\"message\":\"Requests from this Android client application <empty> are blocked.\",\"status\":\"PERMISSION_DENIED\"}}";
        String formattedBlocked = AIRepository.formatApiError(403, blockedJson);
        assertTrue(formattedBlocked.contains("restriction"));
    }

    @Test
    public void testFormatApiError_decodes503Correctly() {
        String overloadedJson = "{\"error\":{\"code\":503,\"message\":\"The model is overloaded. Please try again later.\",\"status\":\"UNAVAILABLE\"}}";
        String formatted = AIRepository.formatApiError(503, overloadedJson);
        assertTrue(formatted.contains("503"));
        assertTrue(formatted.contains("overloaded"));
    }

    @Test
    public void testFormatNetworkException_handlesTimeoutAndOffline() {
        String timeoutMsg = AIRepository.formatNetworkException(new java.net.SocketTimeoutException("timeout"));
        assertTrue(timeoutMsg.contains("Connection Timed Out"));
        assertTrue(timeoutMsg.contains("Troubleshooting:"));

        String dnsMsg = AIRepository.formatNetworkException(new java.net.UnknownHostException("Unable to resolve host"));
        assertTrue(dnsMsg.contains("No Internet Connection"));
    }
}
