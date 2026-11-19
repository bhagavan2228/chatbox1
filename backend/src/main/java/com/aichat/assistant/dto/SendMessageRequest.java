package com.aichat.assistant.dto;

import com.aichat.assistant.model.Tone;
import jakarta.validation.constraints.NotBlank;

public class SendMessageRequest {
    @NotBlank(message = "Message content must not be blank")
    private String content;
    private Tone tone = Tone.PROFESSIONAL;

    public SendMessageRequest() {
    }

    public SendMessageRequest(String content, Tone tone) {
        this.content = content;
        this.tone = tone != null ? tone : Tone.PROFESSIONAL;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Tone getTone() {
        return tone;
    }

    public void setTone(Tone tone) {
        this.tone = tone;
    }
}
