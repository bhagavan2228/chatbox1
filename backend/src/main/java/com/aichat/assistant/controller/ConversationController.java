package com.aichat.assistant.controller;

import com.aichat.assistant.dto.ConversationSummaryResponse;
import com.aichat.assistant.dto.CreateConversationRequest;
import com.aichat.assistant.model.Conversation;
import com.aichat.assistant.service.ConversationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public ResponseEntity<List<ConversationSummaryResponse>> listConversations(
            @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(conversationService.listConversations(search));
    }

    @PostMapping
    public ResponseEntity<Conversation> createConversation(
            @RequestBody(required = false) CreateConversationRequest request) {
        Conversation conversation = conversationService.createConversation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Conversation> getConversation(@PathVariable String id) {
        return ResponseEntity.ok(conversationService.getConversationById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable String id) {
        conversationService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }
}
