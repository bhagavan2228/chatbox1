package com.aichat.assistant.dto;

import com.aichat.assistant.model.Conversation;
import com.aichat.assistant.model.Tone;

import java.time.Instant;

public class ConversationSummaryResponse {
    private String id;
    private String title;
    private Tone tone;
    private Instant createdAt;
    private Instant updatedAt;
    private int messageCount;
    private String lastMessageSnippet;

    public ConversationSummaryResponse() {
    }

    public static ConversationSummaryResponse fromConversation(Conversation c) {
        ConversationSummaryResponse dto = new ConversationSummaryResponse();
        dto.setId(c.getId());
        dto.setTitle(c.getTitle());
        dto.setTone(c.getTone());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        int count = c.getMessages() != null ? c.getMessages().size() : 0;
        dto.setMessageCount(count);
        if (count > 0) {
            String lastText = c.getMessages().get(count - 1).getContent();
            if (lastText != null) {
                dto.setLastMessageSnippet(lastText.length() > 60 ? lastText.substring(0, 57) + "..." : lastText);
            }
        }
        return dto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Tone getTone() {
        return tone;
    }

    public void setTone(Tone tone) {
        this.tone = tone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public String getLastMessageSnippet() {
        return lastMessageSnippet;
    }

    public void setLastMessageSnippet(String lastMessageSnippet) {
        this.lastMessageSnippet = lastMessageSnippet;
    }
}
