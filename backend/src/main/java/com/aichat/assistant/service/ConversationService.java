package com.aichat.assistant.service;

import com.aichat.assistant.dto.ConversationSummaryResponse;
import com.aichat.assistant.dto.CreateConversationRequest;
import com.aichat.assistant.exception.ResourceNotFoundException;
import com.aichat.assistant.model.ChatMessage;
import com.aichat.assistant.model.Conversation;
import com.aichat.assistant.model.Tone;
import com.aichat.assistant.repository.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationRepository conversationRepository;
    private final Map<String, Conversation> memoryStore = new ConcurrentHashMap<>();
    private volatile boolean useFallback = false;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public Conversation createConversation(CreateConversationRequest request) {
        String title = (request != null && request.getTitle() != null && !request.getTitle().isBlank())
                ? request.getTitle().trim()
                : "New Chat";
        Tone tone = (request != null && request.getTone() != null)
                ? request.getTone()
                : Tone.PROFESSIONAL;

        Conversation conversation = new Conversation(title, tone);
        if (conversation.getId() == null || conversation.getId().isBlank()) {
            conversation.setId(UUID.randomUUID().toString());
        }

        if (!useFallback) {
            try {
                Conversation saved = conversationRepository.save(conversation);
                memoryStore.put(saved.getId(), saved);
                return saved;
            } catch (Exception e) {
                log.warn("MongoDB unavailable, falling back to in-memory store: {}", e.getMessage());
                useFallback = true;
            }
        }

        memoryStore.put(conversation.getId(), conversation);
        return conversation;
    }

    public Conversation getConversationById(String id) {
        if (!useFallback) {
            try {
                Optional<Conversation> found = conversationRepository.findById(id);
                if (found.isPresent()) {
                    memoryStore.put(id, found.get());
                    return found.get();
                }
            } catch (Exception e) {
                log.warn("MongoDB findById failed, checking memory store: {}", e.getMessage());
                useFallback = true;
            }
        }

        Conversation inMem = memoryStore.get(id);
        if (inMem != null) {
            return inMem;
        }
        throw new ResourceNotFoundException("Conversation not found with id: " + id);
    }

    public List<ConversationSummaryResponse> listConversations(String search) {
        if (!useFallback) {
            try {
                List<Conversation> list;
                if (search != null && !search.trim().isEmpty()) {
                    list = conversationRepository.findByTitleContainingIgnoreCaseOrderByUpdatedAtDesc(search.trim());
                } else {
                    list = conversationRepository.findAllByOrderByUpdatedAtDesc();
                }
                for (Conversation c : list) {
                    memoryStore.put(c.getId(), c);
                }
                return list.stream()
                        .map(ConversationSummaryResponse::fromConversation)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.warn("MongoDB listConversations failed, using memory store: {}", e.getMessage());
                useFallback = true;
            }
        }

        // Return from memory store
        List<Conversation> list = new ArrayList<>(memoryStore.values());
        list.sort((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()));
        if (search != null && !search.trim().isEmpty()) {
            String lower = search.trim().toLowerCase();
            list = list.stream()
                    .filter(c -> c.getTitle() != null && c.getTitle().toLowerCase().contains(lower))
                    .collect(Collectors.toList());
        }

        return list.stream()
                .map(ConversationSummaryResponse::fromConversation)
                .collect(Collectors.toList());
    }

    public void deleteConversation(String id) {
        memoryStore.remove(id);
        if (!useFallback) {
            try {
                if (conversationRepository.existsById(id)) {
                    conversationRepository.deleteById(id);
                }
            } catch (Exception e) {
                log.warn("MongoDB delete failed: {}", e.getMessage());
                useFallback = true;
            }
        }
    }

    public Conversation updateTone(String id, Tone tone) {
        Conversation conversation = getConversationById(id);
        conversation.setTone(tone != null ? tone : Tone.PROFESSIONAL);
        conversation.setUpdatedAt(Instant.now());
        return save(conversation);
    }

    public Conversation addMessage(String conversationId, ChatMessage message) {
        Conversation conversation = getConversationById(conversationId);
        conversation.addMessage(message);
        return save(conversation);
    }

    public Conversation updateTitle(String id, String newTitle) {
        Conversation conversation = getConversationById(id);
        if (newTitle != null && !newTitle.isBlank()) {
            conversation.setTitle(newTitle.trim());
            conversation.setUpdatedAt(Instant.now());
            return save(conversation);
        }
        return conversation;
    }

    public Conversation save(Conversation conversation) {
        conversation.setUpdatedAt(Instant.now());
        if (conversation.getId() == null || conversation.getId().isBlank()) {
            conversation.setId(UUID.randomUUID().toString());
        }
        memoryStore.put(conversation.getId(), conversation);

        if (!useFallback) {
            try {
                return conversationRepository.save(conversation);
            } catch (Exception e) {
                log.warn("MongoDB save failed, retained in memory store: {}", e.getMessage());
                useFallback = true;
            }
        }
        return conversation;
    }
}
