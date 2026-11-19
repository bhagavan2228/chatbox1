package com.aichat.assistant.service;

import com.aichat.assistant.client.GeminiClient;
import com.aichat.assistant.client.dto.GeminiContent;
import com.aichat.assistant.client.dto.GeminiRequest;
import com.aichat.assistant.client.dto.GeminiSystemInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class TitleGenerationService {

    private static final Logger log = LoggerFactory.getLogger(TitleGenerationService.class);

    private final GeminiClient geminiClient;
    private final ConversationService conversationService;

    public TitleGenerationService(GeminiClient geminiClient, ConversationService conversationService) {
        this.geminiClient = geminiClient;
        this.conversationService = conversationService;
    }

    /**
     * Generates a 3-5 word concise title for the conversation based on the first prompt.
     * Falls back to a snippet of the user prompt if Gemini call fails.
     */
    public void generateAndSetTitle(String conversationId, String firstPrompt) {
        if (firstPrompt == null || firstPrompt.isBlank()) {
            return;
        }

        String fallbackTitle = buildFallbackTitle(firstPrompt);

        GeminiSystemInstruction instruction = new GeminiSystemInstruction(
                "You are an assistant that summarizes user prompts into a concise chat thread title of 3 to 6 words. " +
                        "Do not use quotes, punctuation, or preamble. Return ONLY the title text.");

        GeminiContent promptContent = new GeminiContent("user", "Create a short title for a chat starting with: " + firstPrompt);
        GeminiRequest request = new GeminiRequest(instruction, Collections.singletonList(promptContent));

        geminiClient.generateContent(request)
                .subscribe(
                        aiTitle -> {
                            String cleaned = cleanTitle(aiTitle, fallbackTitle);
                            log.info("Generated title for {}: '{}'", conversationId, cleaned);
                            conversationService.updateTitle(conversationId, cleaned);
                        },
                        error -> {
                            log.warn("Title generation failed, using fallback: {}", error.getMessage());
                            conversationService.updateTitle(conversationId, fallbackTitle);
                        }
                );
    }

    private String buildFallbackTitle(String prompt) {
        String trimmed = prompt.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= 35) {
            return trimmed;
        }
        return trimmed.substring(0, 32) + "...";
    }

    private String cleanTitle(String rawTitle, String fallback) {
        if (rawTitle == null || rawTitle.isBlank()) {
            return fallback;
        }
        String cleaned = rawTitle.replace("\"", "").replace("'", "").replace("\n", " ").trim();
        if (cleaned.isEmpty()) {
            return fallback;
        }
        if (cleaned.length() > 50) {
            cleaned = cleaned.substring(0, 47) + "...";
        }
        return cleaned;
    }
}
