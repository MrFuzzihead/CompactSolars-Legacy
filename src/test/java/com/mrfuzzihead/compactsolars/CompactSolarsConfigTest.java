package com.mrfuzzihead.compactsolars;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CompactSolarsConfigTest {

    @Test
    void scaleFactorIsClampedToAtLeastOne() {
        assertEquals(1, CompactSolars.sanitizeScaleFactor(0));
        assertEquals(1, CompactSolars.sanitizeScaleFactor(-1));
        assertEquals(1, CompactSolars.sanitizeScaleFactor(-100));
        assertEquals(1, CompactSolars.sanitizeScaleFactor(1));
        assertEquals(2, CompactSolars.sanitizeScaleFactor(2));
        assertEquals(100, CompactSolars.sanitizeScaleFactor(100));
    }
}
