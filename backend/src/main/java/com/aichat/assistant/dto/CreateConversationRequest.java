package com.aichat.assistant.dto;

import com.aichat.assistant.model.Tone;

public class CreateConversationRequest {
    private String title;
    private Tone tone = Tone.PROFESSIONAL;

    public CreateConversationRequest() {
    }

    public CreateConversationRequest(String title, Tone tone) {
        this.title = title;
        this.tone = tone;
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
}
