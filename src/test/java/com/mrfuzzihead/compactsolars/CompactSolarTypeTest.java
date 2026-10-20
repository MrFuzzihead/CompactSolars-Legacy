package com.mrfuzzihead.compactsolars;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CompactSolarTypeTest {

    @Test
    void metadataMapsToExpectedTier() {
        assertEquals(CompactSolarType.LV, CompactSolarType.byMetadata(0));
        assertEquals(CompactSolarType.MV, CompactSolarType.byMetadata(1));
        assertEquals(CompactSolarType.HV, CompactSolarType.byMetadata(2));
    }

    @Test
    void invalidMetadataFallsBackToLv() {
        assertEquals(0, CompactSolarType.clampMetadata(-1));
        assertEquals(0, CompactSolarType.clampMetadata(3));
        assertEquals(CompactSolarType.LV, CompactSolarType.byMetadata(-1));
        assertEquals(CompactSolarType.LV, CompactSolarType.byMetadata(3));
    }

    @Test
    void tierDataIsStable() {
        assertEquals(8, CompactSolarType.LV.getOutput());
        assertEquals(64, CompactSolarType.MV.getOutput());
        assertEquals(512, CompactSolarType.HV.getOutput());

        assertEquals(1, CompactSolarType.LV.getTier());
        assertEquals(2, CompactSolarType.MV.getTier());
        assertEquals(3, CompactSolarType.HV.getTier());

        assertEquals(8, CompactSolarType.LV.panels);
        assertEquals(64, CompactSolarType.MV.panels);
        assertEquals(512, CompactSolarType.HV.panels);

        assertEquals(32, CompactSolarType.LV.outputPacketSize);
        assertEquals(128, CompactSolarType.MV.outputPacketSize);
        assertEquals(512, CompactSolarType.HV.outputPacketSize);

        assertEquals(16, CompactSolarType.LV.panelArrayStorage);
        assertEquals(128, CompactSolarType.MV.panelArrayStorage);
        assertEquals(1024, CompactSolarType.HV.panelArrayStorage);

        // BasicSource cannot go below one packet, so LV's 16 EU is raised to 32.
        assertEquals(32, CompactSolarType.LV.maxStorage);
        assertEquals(128, CompactSolarType.MV.maxStorage);
        assertEquals(1024, CompactSolarType.HV.maxStorage);
    }

    @Test
    void legacyTileEntityIdsMapToTiers() {
        assertEquals(CompactSolarType.LV, CompactSolarType.fromLegacyTileEntityId("CompactSolarType.LV"));
        assertEquals(CompactSolarType.MV, CompactSolarType.fromLegacyTileEntityId("CompactSolarType.MV"));
        assertEquals(CompactSolarType.HV, CompactSolarType.fromLegacyTileEntityId("CompactSolarType.HV"));
        assertEquals(CompactSolarType.LV, CompactSolarType.fromLegacyTileEntityId(null));
        assertEquals(CompactSolarType.LV, CompactSolarType.fromLegacyTileEntityId("unknown"));
    }

    @Test
    void transformerOreNamesMatchRecipeInputs() {
        assertEquals("transformerLV", CompactSolarType.LV.transformerOreName());
        assertEquals("transformerMV", CompactSolarType.MV.transformerOreName());
        assertEquals("transformerHV", CompactSolarType.HV.transformerOreName());
    }

    @Test
    void unlocalizedNameMatchesLanguageKey() {
        assertEquals("tile.compactsolars:LV_block", CompactSolarType.LV.getUnlocalizedName());
        assertEquals("tile.compactsolars:MV_block", CompactSolarType.MV.getUnlocalizedName());
        assertEquals("tile.compactsolars:HV_block", CompactSolarType.HV.getUnlocalizedName());
    }
}
