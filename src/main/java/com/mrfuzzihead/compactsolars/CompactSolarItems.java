package com.mrfuzzihead.compactsolars;

/** Registration of the three solar-hat items. */
public final class CompactSolarItems {

    private CompactSolarItems() {}

    public static void registerHats() {
        for (CompactSolarType typ : CompactSolarType.values()) {
            typ.buildHat();
        }
    }
}
