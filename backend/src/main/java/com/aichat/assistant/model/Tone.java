package com.aichat.assistant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Tone {
    PROFESSIONAL("Professional"),
    CASUAL("Casual"),
    CONCISE("Concise");

    private final String displayName;

    Tone(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static Tone fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PROFESSIONAL;
        }
        for (Tone tone : Tone.values()) {
            if (tone.name().equalsIgnoreCase(value.trim()) ||
                tone.displayName.equalsIgnoreCase(value.trim())) {
                return tone;
            }
        }
        return PROFESSIONAL;
    }
}
