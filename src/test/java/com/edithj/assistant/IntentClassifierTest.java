package com.edithj.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.edithj.integration.llm.LlmClient;

class IntentClassifierTest {

    @Test
    void classify_blankInputFallsBackToGeneralChatWithoutLlmRefinement() {
        IntentClassifier classifier = new IntentClassifier(prompt -> {
            throw new AssertionError("LLM should not be called for blank input");
        });

        IntentClassifier.Classification classification = classifier.classify("   ");

        assertEquals(Intent.GENERAL_CHAT, classification.intent());
        assertTrue(classification.targets().isEmpty());
        assertFalse(classification.llmRefined());
        assertTrue(classification.confidenceScore() <= 0.50d);
    }

    @Test
    void classify_openCommandExtractsLaunchTarget() {
        IntentClassifier classifier = new IntentClassifier(prompt -> "unused");

        IntentClassifier.Classification classification = classifier.classify("Open calculator");

        assertEquals(Intent.OPEN_APP, classification.intent());
        assertEquals("calculator", classification.targets().get(0));
        assertFalse(classification.llmRefined());
        assertTrue(classification.confidenceScore() >= 0.80d);
    }

    @Test
    void classify_ambiguousInputUsesLlmRefinementWhenAvailable() {
        LlmClient llmClient = prompt -> """
                {
                  "intent": "ASK_LOCAL_KB",
                  "targets": ["architecture notes"]
                }
                """;
        IntentClassifier classifier = new IntentClassifier(llmClient);

        IntentClassifier.Classification classification = classifier.classify("open notes from our documents");

        assertEquals(Intent.ASK_LOCAL_KB, classification.intent());
        assertEquals("architecture notes", classification.targets().get(0));
        assertTrue(classification.llmRefined());
        assertTrue(classification.candidates().size() > 1);
        assertTrue(classification.confidenceScore() >= 0.80d);
    }
}
