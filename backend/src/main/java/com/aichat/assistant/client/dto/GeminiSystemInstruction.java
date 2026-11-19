package com.aichat.assistant.client.dto;

import java.util.Collections;
import java.util.List;

public class GeminiSystemInstruction {
    private List<GeminiPart> parts;

    public GeminiSystemInstruction() {
    }

    public GeminiSystemInstruction(String instruction) {
        this.parts = Collections.singletonList(new GeminiPart(instruction));
    }

    public GeminiSystemInstruction(List<GeminiPart> parts) {
        this.parts = parts;
    }

    public List<GeminiPart> getParts() {
        return parts;
    }

    public void setParts(List<GeminiPart> parts) {
        this.parts = parts;
    }
}
