package com.aichat.assistant.client;

import com.aichat.assistant.client.dto.GeminiRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String model;

    public GeminiClient(WebClient geminiWebClient, ObjectMapper objectMapper) {
        this.webClient = geminiWebClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Streams generative content using Gemini SSE endpoint.
     * Returns a Flux emitting parsed text tokens.
     */
    public Flux<String> streamGenerateContent(GeminiRequest request) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("your-api-key")) {
            return Flux.error(new IllegalStateException(
                    "GEMINI_API_KEY is not configured. Please set the GEMINI_API_KEY environment variable."));
        }

        String uri = String.format("/models/%s:streamGenerateContent?alt=sse", model);

        return webClient.post()
                .uri(uri)
                .header("x-goog-api-key", apiKey.trim())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .mapNotNull(this::extractTextFromEvent)
                .filter(text -> !text.isEmpty())
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("Gemini API error [{}]: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                    String errorMsg = parseErrorMessage(ex.getResponseBodyAsString(), ex.getStatusCode().value());
                    return Flux.error(new RuntimeException(errorMsg));
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Gemini streaming error: {}", ex.getMessage());
                    return Flux.error(ex);
                });
    }

    /**
     * Non-streaming call for title generation.
     */
    public Mono<String> generateContent(GeminiRequest request) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("your-api-key")) {
            return Mono.error(new IllegalStateException(
                    "GEMINI_API_KEY is not configured."));
        }

        String uri = String.format("/models/%s:generateContent", model);

        return webClient.post()
                .uri(uri)
                .header("x-goog-api-key", apiKey.trim())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractTextFromRawJson)
                .onErrorResume(ex -> {
                    log.warn("Gemini non-streaming call failed: {}", ex.getMessage());
                    return Mono.empty();
                });
    }

    private String extractTextFromEvent(ServerSentEvent<String> event) {
        if (event == null || event.data() == null || event.data().trim().isEmpty()) {
            return null;
        }
        return extractTextFromRawJson(event.data());
    }

    private String extractTextFromRawJson(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);

            // Check for prompt feedback blocks
            JsonNode promptFeedback = root.path("promptFeedback");
            if (!promptFeedback.isMissingNode()) {
                JsonNode blockReason = promptFeedback.path("blockReason");
                if (!blockReason.isMissingNode() && !blockReason.asText().isEmpty()) {
                    log.warn("Gemini prompt feedback block: {}", blockReason.asText());
                    return " [Blocked by safety filters: " + blockReason.asText() + "] ";
                }
            }

            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode candidate = candidates.get(0);

                // Check finish reason
                String finishReason = candidate.path("finishReason").asText("");
                if ("SAFETY".equalsIgnoreCase(finishReason)) {
                    log.warn("Candidate finished due to SAFETY filter");
                    return " [Response stopped due to safety policies] ";
                }

                JsonNode parts = candidate.path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText("");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Gemini chunk: {}", e.getMessage());
        }
        return "";
    }

    private String parseErrorMessage(String responseBody, int statusCode) {
        try {
            if (responseBody != null && !responseBody.isBlank()) {
                JsonNode root = objectMapper.readTree(responseBody);
                String msg = root.path("error").path("message").asText("");
                if (!msg.isEmpty()) {
                    return "Gemini API Error (" + statusCode + "): " + msg;
                }
            }
        } catch (Exception ignored) {
        }
        return "Gemini API request failed with status " + statusCode;
    }
}
