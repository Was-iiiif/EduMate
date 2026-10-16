package com.edumate.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.edumate.app.repository.AIRepository;

import org.junit.Test;

import java.util.List;

public class TopicChecklistTest {

    @Test
    public void testPresetTopics_returnRichChecklists() {
        // Instantiate without Android Context for pure checklist logic test
        List<String> photosynthesisList = AIRepository.getInstance(
                // Context is not touched in getTopicChecklist
                null
        ).getTopicChecklist("Photosynthesis");

        assertNotNull(photosynthesisList);
        assertTrue(photosynthesisList.size() >= 5);
        assertTrue(photosynthesisList.get(0).toLowerCase().contains("chloroplast")
                || photosynthesisList.get(0).toLowerCase().contains("chlorophyll"));

        List<String> quicksortList = AIRepository.getInstance(null).getTopicChecklist("QuickSort");
        assertNotNull(quicksortList);
        assertTrue(quicksortList.size() >= 5);

        List<String> newtonList = AIRepository.getInstance(null).getTopicChecklist("Newton's Laws of Motion");
        assertNotNull(newtonList);
        assertTrue(newtonList.size() >= 5);

        List<String> customList = AIRepository.getInstance(null).getTopicChecklist("Quantum Entanglement");
        assertNotNull(customList);
        assertTrue(customList.size() >= 5);
    }
}
