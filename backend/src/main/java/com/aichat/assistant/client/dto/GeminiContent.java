package com.aichat.assistant.client.dto;

import java.util.Collections;
import java.util.List;

public class GeminiContent {
    private String role; // "user" or "model"
    private List<GeminiPart> parts;

    public GeminiContent() {
    }

    public GeminiContent(String role, String text) {
        this.role = role;
        this.parts = Collections.singletonList(new GeminiPart(text));
    }

    public GeminiContent(String role, List<GeminiPart> parts) {
        this.role = role;
        this.parts = parts;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<GeminiPart> getParts() {
        return parts;
    }

    public void setParts(List<GeminiPart> parts) {
        this.parts = parts;
    }
}
