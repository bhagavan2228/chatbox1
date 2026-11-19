package com.aichat.assistant.service;

import com.aichat.assistant.client.GeminiClient;
import com.aichat.assistant.client.dto.GeminiContent;
import com.aichat.assistant.client.dto.GeminiRequest;
import com.aichat.assistant.client.dto.GeminiSystemInstruction;
import com.aichat.assistant.dto.SendMessageRequest;
import com.aichat.assistant.model.ChatMessage;
import com.aichat.assistant.model.Conversation;
import com.aichat.assistant.model.Tone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ChatStreamService {

    private static final Logger log = LoggerFactory.getLogger(ChatStreamService.class);

    private final GeminiClient geminiClient;
    private final ConversationService conversationService;
    private final ToneInstructionProvider toneInstructionProvider;
    private final TitleGenerationService titleGenerationService;

    public ChatStreamService(
            GeminiClient geminiClient,
            ConversationService conversationService,
            ToneInstructionProvider toneInstructionProvider,
            TitleGenerationService titleGenerationService) {
        this.geminiClient = geminiClient;
        this.conversationService = conversationService;
        this.toneInstructionProvider = toneInstructionProvider;
        this.titleGenerationService = titleGenerationService;
    }

    /**
     * Streams an AI response for the conversation via Server-Sent Events.
     * Persists the user message immediately, streams tokens from Gemini,
     * and persists the complete (or partial on stop) response to MongoDB.
     */
    public Flux<ServerSentEvent<String>> streamMessage(String conversationId, SendMessageRequest request) {
        // 1. Validate & load conversation
        Conversation conversation = conversationService.getConversationById(conversationId);

        Tone effectiveTone = request.getTone() != null ? request.getTone() : conversation.getTone();
        conversation.setTone(effectiveTone);

        // 2. Persist user prompt immediately
        ChatMessage userMsg = new ChatMessage("user", request.getContent());
        conversation.addMessage(userMsg);
        conversationService.save(conversation);

        // Check if this was the first user message (conversation has 1 message so far)
        boolean isFirstExchange = conversation.getMessages().size() == 1;

        // 3. Build Gemini payload: tone system instruction + full chat history
        String systemPrompt = toneInstructionProvider.getInstruction(effectiveTone);
        GeminiSystemInstruction systemInstruction = new GeminiSystemInstruction(systemPrompt);

        List<GeminiContent> geminiContents = new ArrayList<>();
        for (ChatMessage msg : conversation.getMessages()) {
            // Gemini uses "model" instead of "assistant"
            String role = "assistant".equalsIgnoreCase(msg.getRole()) ? "model" : msg.getRole();
            geminiContents.add(new GeminiContent(role, msg.getContent()));
        }

        GeminiRequest geminiRequest = new GeminiRequest(systemInstruction, geminiContents);

        // State holders for the stream lifecycle
        StringBuilder accumulatedAiResponse = new StringBuilder();
        AtomicBoolean saved = new AtomicBoolean(false);

        // 4. Stream from Gemini and map to Server-Sent Events
        return geminiClient.streamGenerateContent(geminiRequest)
                .map(token -> {
                    accumulatedAiResponse.append(token);
                    return ServerSentEvent.<String>builder()
                            .event("token")
                            .data(token)
                            .build();
                })
                .concatWith(Flux.defer(() -> {
                    // Normal completion: persist full AI message and emit "done" event
                    String fullResponse = accumulatedAiResponse.toString();
                    if (!saved.getAndSet(true) && !fullResponse.isEmpty()) {
                        persistAiResponse(conversationId, fullResponse, false);

                        if (isFirstExchange) {
                            titleGenerationService.generateAndSetTitle(conversationId, request.getContent());
                        }
                    }

                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("done")
                            .data(fullResponse)
                            .build());
                }))
                .doOnCancel(() -> {
                    // Stream cancelled by client (e.g. "Stop generating" button)
                    String partial = accumulatedAiResponse.toString();
                    log.info("Stream cancelled for conversation {}. Persisting {} characters.", conversationId, partial.length());
                    if (!saved.getAndSet(true) && !partial.isEmpty()) {
                        persistAiResponse(conversationId, partial, true);
                    }
                })
                .onErrorResume(ex -> {
                    log.error("Error during streaming for conversation {}: {}", conversationId, ex.getMessage());
                    String partial = accumulatedAiResponse.toString();
                    if (!saved.getAndSet(true) && !partial.isEmpty()) {
                        persistAiResponse(conversationId, partial, true);
                    }

                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(ex.getMessage() != null ? ex.getMessage() : "An error occurred during generation.")
                            .build());
                });
    }

    private void persistAiResponse(String conversationId, String content, boolean stopped) {
        try {
            String finalContent = stopped ? content + " [stopped]" : content;
            ChatMessage aiMsg = new ChatMessage("assistant", finalContent);
            conversationService.addMessage(conversationId, aiMsg);
        } catch (Exception e) {
            log.error("Failed to persist AI message for conversation {}: {}", conversationId, e.getMessage());
        }
    }
}
