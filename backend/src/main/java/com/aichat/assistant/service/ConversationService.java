package com.aichat.assistant.service;

import com.aichat.assistant.dto.ConversationSummaryResponse;
import com.aichat.assistant.dto.CreateConversationRequest;
import com.aichat.assistant.exception.ResourceNotFoundException;
import com.aichat.assistant.model.ChatMessage;
import com.aichat.assistant.model.Conversation;
import com.aichat.assistant.model.Tone;
import com.aichat.assistant.repository.ConversationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

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
        return conversationRepository.save(conversation);
    }

    public Conversation getConversationById(String id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + id));
    }

    public List<ConversationSummaryResponse> listConversations(String search) {
        List<Conversation> list;
        if (search != null && !search.trim().isEmpty()) {
            list = conversationRepository.findByTitleContainingIgnoreCaseOrderByUpdatedAtDesc(search.trim());
        } else {
            list = conversationRepository.findAllByOrderByUpdatedAtDesc();
        }
        return list.stream()
                .map(ConversationSummaryResponse::fromConversation)
                .collect(Collectors.toList());
    }

    public void deleteConversation(String id) {
        if (!conversationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Conversation not found with id: " + id);
        }
        conversationRepository.deleteById(id);
    }

    public Conversation updateTone(String id, Tone tone) {
        Conversation conversation = getConversationById(id);
        conversation.setTone(tone != null ? tone : Tone.PROFESSIONAL);
        conversation.setUpdatedAt(Instant.now());
        return conversationRepository.save(conversation);
    }

    public Conversation addMessage(String conversationId, ChatMessage message) {
        Conversation conversation = getConversationById(conversationId);
        conversation.addMessage(message);
        return conversationRepository.save(conversation);
    }

    public Conversation updateTitle(String id, String newTitle) {
        Conversation conversation = getConversationById(id);
        if (newTitle != null && !newTitle.isBlank()) {
            conversation.setTitle(newTitle.trim());
            conversation.setUpdatedAt(Instant.now());
            return conversationRepository.save(conversation);
        }
        return conversation;
    }

    public Conversation save(Conversation conversation) {
        conversation.setUpdatedAt(Instant.now());
        return conversationRepository.save(conversation);
    }
}
