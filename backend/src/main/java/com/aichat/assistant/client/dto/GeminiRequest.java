package com.aichat.assistant.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeminiRequest {
    private GeminiSystemInstruction systemInstruction;
    private List<GeminiContent> contents = new ArrayList<>();

    public GeminiRequest() {
    }

    public GeminiRequest(GeminiSystemInstruction systemInstruction, List<GeminiContent> contents) {
        this.systemInstruction = systemInstruction;
        this.contents = contents != null ? contents : new ArrayList<>();
    }

    public GeminiSystemInstruction getSystemInstruction() {
        return systemInstruction;
    }

    public void setSystemInstruction(GeminiSystemInstruction systemInstruction) {
        this.systemInstruction = systemInstruction;
    }

    public List<GeminiContent> getContents() {
        return contents;
    }

    public void setContents(List<GeminiContent> contents) {
        this.contents = contents != null ? contents : new ArrayList<>();
    }
}
