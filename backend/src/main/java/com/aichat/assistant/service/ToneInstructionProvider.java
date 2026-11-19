package com.aichat.assistant.service;

import com.aichat.assistant.model.Tone;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ToneInstructionProvider {

    private final Map<Tone, String> toneInstructions = new EnumMap<>(Tone.class);

    public ToneInstructionProvider() {
        toneInstructions.put(Tone.PROFESSIONAL,
                "You are a professional, helpful, and courteous AI assistant. Provide well-structured, accurate, and articulate responses with clear explanations.");

        toneInstructions.put(Tone.CASUAL,
                "You are a friendly, relaxed, and conversational AI assistant. Use everyday, approachable language, feel free to use emojis where appropriate, and keep the vibe easygoing and engaging.");

        toneInstructions.put(Tone.CONCISE,
                "You are an ultra-concise, direct AI assistant. Provide the most direct, succinct answer possible with zero fluff or filler. Prioritize bullet points and brief sentences.");
    }

    public String getInstruction(Tone tone) {
        Tone effectiveTone = (tone != null) ? tone : Tone.PROFESSIONAL;
        return toneInstructions.getOrDefault(effectiveTone, toneInstructions.get(Tone.PROFESSIONAL));
    }
}
