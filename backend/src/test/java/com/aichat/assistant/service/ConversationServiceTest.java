package com.aichat.assistant.service;

import com.aichat.assistant.dto.ConversationSummaryResponse;
import com.aichat.assistant.dto.CreateConversationRequest;
import com.aichat.assistant.exception.ResourceNotFoundException;
import com.aichat.assistant.model.ChatMessage;
import com.aichat.assistant.model.Conversation;
import com.aichat.assistant.model.Tone;
import com.aichat.assistant.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private ConversationService conversationService;

    private Conversation sampleConversation;

    @BeforeEach
    void setUp() {
        sampleConversation = new Conversation("Test Chat", Tone.CASUAL);
        sampleConversation.setId("conv-123");
    }

    @Test
    void testCreateConversationWithDefaults() {
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

        Conversation created = conversationService.createConversation(new CreateConversationRequest(null, null));
        assertNotNull(created);
        assertEquals("New Chat", created.getTitle());
        assertEquals(Tone.PROFESSIONAL, created.getTone());
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void testGetConversationByIdSuccess() {
        when(conversationRepository.findById("conv-123")).thenReturn(Optional.of(sampleConversation));

        Conversation found = conversationService.getConversationById("conv-123");
        assertEquals("conv-123", found.getId());
        assertEquals("Test Chat", found.getTitle());
    }

    @Test
    void testGetConversationByIdNotFound() {
        when(conversationRepository.findById("invalid")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> conversationService.getConversationById("invalid"));
    }

    @Test
    void testListConversationsWithSearch() {
        when(conversationRepository.findByTitleContainingIgnoreCaseOrderByUpdatedAtDesc("spring"))
                .thenReturn(Collections.singletonList(sampleConversation));

        List<ConversationSummaryResponse> result = conversationService.listConversations("spring");
        assertEquals(1, result.size());
        assertEquals("conv-123", result.get(0).getId());
    }

    @Test
    void testAddMessage() {
        when(conversationRepository.findById("conv-123")).thenReturn(Optional.of(sampleConversation));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

        ChatMessage message = new ChatMessage("user", "Hello AI", Instant.now());
        Conversation updated = conversationService.addMessage("conv-123", message);

        assertEquals(1, updated.getMessages().size());
        assertEquals("Hello AI", updated.getMessages().get(0).getContent());
    }
}
