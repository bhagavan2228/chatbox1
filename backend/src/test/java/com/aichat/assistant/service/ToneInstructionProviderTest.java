package com.aichat.assistant.service;

import com.aichat.assistant.model.Tone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToneInstructionProviderTest {

    private ToneInstructionProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ToneInstructionProvider();
    }

    @Test
    void testProfessionalToneInstruction() {
        String instruction = provider.getInstruction(Tone.PROFESSIONAL);
        assertNotNull(instruction);
        assertTrue(instruction.toLowerCase().contains("professional"));
    }

    @Test
    void testCasualToneInstruction() {
        String instruction = provider.getInstruction(Tone.CASUAL);
        assertNotNull(instruction);
        assertTrue(instruction.toLowerCase().contains("friendly") || instruction.toLowerCase().contains("casual"));
    }

    @Test
    void testConciseToneInstruction() {
        String instruction = provider.getInstruction(Tone.CONCISE);
        assertNotNull(instruction);
        assertTrue(instruction.toLowerCase().contains("concise"));
    }

    @Test
    void testNullToneFallbackToProfessional() {
        String instruction = provider.getInstruction(null);
        assertNotNull(instruction);
        assertEquals(provider.getInstruction(Tone.PROFESSIONAL), instruction);
    }
}
