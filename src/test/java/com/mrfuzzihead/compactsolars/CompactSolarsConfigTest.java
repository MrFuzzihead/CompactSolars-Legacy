package com.mrfuzzihead.compactsolars;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CompactSolarsConfigTest {

    @Test
    void solarBaseIsClampedToFiniteNonNegative() {
        assertEquals(1.0D, CompactSolars.sanitizeSolarBase(1.0D), 0.0D);
        assertEquals(0.5D, CompactSolars.sanitizeSolarBase(0.5D), 0.0D);
        assertEquals(0.0D, CompactSolars.sanitizeSolarBase(0.0D), 0.0D);
        assertEquals(1.0D, CompactSolars.sanitizeSolarBase(-1.0D), 0.0D);
        assertEquals(1.0D, CompactSolars.sanitizeSolarBase(Double.NaN), 0.0D);
        assertEquals(1.0D, CompactSolars.sanitizeSolarBase(Double.POSITIVE_INFINITY), 0.0D);
    }

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
