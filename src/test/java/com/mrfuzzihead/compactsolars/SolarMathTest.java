/*
 * CompactSolars-Legacy
 * Copyright (c) 2026 Duncan Campbell
 * Derived from CompactSolars, Copyright (c) 2012 cpw and contributors.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
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
