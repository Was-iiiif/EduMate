package com.edumate.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.edumate.app.model.Message;
import com.edumate.app.model.Persona;
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
        String reply = aiRepository.generateFallbackStudentReply("Photosynthesis", Persona.BEGINNER, history);
        assertNotNull(reply);
        assertTrue(reply.contains("Photosynthesis"));
        assertTrue(reply.contains("simple terms") || reply.contains("explain"));
    }

    @Test
    public void testUserSaysIDontKnow_givesScaffoldingHintAndAdaptiveQuestion() {
        List<Message> history = new ArrayList<>();
        history.add(new Message("s1", Message.SENDER_STUDENT, "What does photosynthesis mean in simple terms?"));
        history.add(new Message("s1", Message.SENDER_LEARNER, "I DON'T KNOW"));

        // Beginner Persona
        String beginnerReply = aiRepository.generateFallbackStudentReply("Photosynthesis", Persona.BEGINNER, history);
        assertNotNull(beginnerReply);
        // Must NOT return the old canned generic switch string
        assertFalse(beginnerReply.contains("Oh, that's interesting! But I got a bit confused about how it works inside"));
        // Must give scaffolding/clue
        assertTrue(beginnerReply.toLowerCase().contains("clue")
                || beginnerReply.toLowerCase().contains("hint")
                || beginnerReply.toLowerCase().contains("leaf")
                || beginnerReply.toLowerCase().contains("kitchen")
                || beginnerReply.toLowerCase().contains("chloroplast"));

        // Skeptical Peer Persona
        String skepticalReply = aiRepository.generateFallbackStudentReply("Photosynthesis", Persona.SKEPTICAL_PEER, history);
        assertNotNull(skepticalReply);
        assertTrue(skepticalReply.contains("teacher") || skepticalReply.contains("guess") || skepticalReply.contains("clue"));

        // Exam Style Persona
        String examReply = aiRepository.generateFallbackStudentReply("Photosynthesis", Persona.EXAM_STYLE, history);
        assertNotNull(examReply);
        assertTrue(examReply.contains("exam") || examReply.contains("marks") || examReply.contains("principle"));
    }

    @Test
    public void testUserSaysIdkOrNoIdea_isRecognizedAsUncertainty() {
        List<Message> history = new ArrayList<>();
        history.add(new Message("s1", Message.SENDER_STUDENT, "Define Quicksort."));
        history.add(new Message("s1", Message.SENDER_LEARNER, "no idea"));

        String reply = aiRepository.generateFallbackStudentReply("Quicksort", Persona.BEGINNER, history);
        assertNotNull(reply);
        assertTrue(reply.toLowerCase().contains("clue") || reply.toLowerCase().contains("pivot") || reply.toLowerCase().contains("benchmark"));
    }

    @Test
    public void testUserProvidesMeaningfulExplanation_quotesLearnerAndProbesNextConcept() {
        List<Message> history = new ArrayList<>();
        history.add(new Message("s1", Message.SENDER_STUDENT, "Could you start from the very beginning and explain what Photosynthesis means in simple terms?"));
        history.add(new Message("s1", Message.SENDER_LEARNER, "Plants use solar energy from the sun to synthesize glucose and chemical energy"));

        String reply = aiRepository.generateFallbackStudentReply("Photosynthesis", Persona.BEGINNER, history);
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

        String reply = aiRepository.generateFallbackStudentReply("Quicksort", Persona.SKEPTICAL_PEER, history);
        assertNotNull(reply);
        assertTrue(reply.contains("'yes'") || reply.contains("Just 'yes'"));
        assertTrue(reply.contains("explain") || reply.contains("convince") || reply.contains("works"));
    }
}
