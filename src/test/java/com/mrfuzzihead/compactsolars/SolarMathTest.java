package com.mrfuzzihead.compactsolars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SolarMathTest {

    @Test
    void perPanelOutputUsesSkyLightAndBase() {
        assertEquals(0.0D, SolarMath.perPanelOutput(0, 1.0D), 1.0E-9D);
        assertEquals(0.0D, SolarMath.perPanelOutput(4, 1.0D), 1.0E-9D);
        assertEquals(1.0D / 3.0D, SolarMath.perPanelOutput(5, 1.0D), 1.0E-9D);
        assertEquals(0.8D, SolarMath.perPanelOutput(12, 1.0D), 1.0E-9D);
        assertEquals(1.0D, SolarMath.perPanelOutput(15, 1.0D), 1.0E-9D);
        assertEquals(0.4D, SolarMath.perPanelOutput(12, 0.5D), 1.0E-9D);
    }

    @Test
    void perPanelOutputRejectsInvalidBase() {
        assertEquals(0.0D, SolarMath.perPanelOutput(15, 0.0D), 1.0E-9D);
        assertEquals(0.0D, SolarMath.perPanelOutput(15, -1.0D), 1.0E-9D);
        assertEquals(0.0D, SolarMath.perPanelOutput(15, Double.NaN), 1.0E-9D);
    }

    @Test
    void sunVisibilityRequiresSkyLightAndClearSky() {
        assertFalse(SolarMath.isSunVisible(4, false, true, false, false, false));
        assertTrue(SolarMath.isSunVisible(5, false, true, false, false, false));
        assertFalse(SolarMath.isSunVisible(15, true, true, false, false, false));
        assertFalse(SolarMath.isSunVisible(15, false, false, false, false, false));
    }

    @Test
    void sunVisibilityUsesIc2WeatherRules() {
        assertFalse(SolarMath.isSunVisible(15, false, true, true, false, false));
        assertFalse(SolarMath.isSunVisible(15, false, true, false, true, false));
        assertTrue(SolarMath.isSunVisible(15, false, true, true, true, true));
        assertTrue(SolarMath.isSunVisible(15, false, true, true, false, true));
    }
}
