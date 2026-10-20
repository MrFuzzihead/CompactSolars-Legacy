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
