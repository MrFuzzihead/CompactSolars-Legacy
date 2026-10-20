package com.mrfuzzihead.compactsolars;

import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.registry.GameRegistry;

public enum CompactSolarType {

    LV(8, 32, "lvTransformer", "lvHat"),
    MV(64, 128, "mvTransformer", "mvHat"),
    HV(512, 512, "hvTransformer", "hvHat");

    private static final CompactSolarType[] VALUES = values();
    private static final Logger LOG = LogManager.getLogger("CompactSolars");

    private final int output;
    private final String transformerName;
    public final ResourceLocation hatTexture;
    public final String hatName;
    private ItemSolarHat item;
    public final ResourceLocation hatItemTexture;
    public final int outputPacketSize;
    public final int panels;
    public final int panelArrayStorage;
    private final String unlocalizedName;

    private CompactSolarType(int output, int outputPacketSize, String transformerName, String hatTexture) {
        this.output = output;
        this.outputPacketSize = outputPacketSize;
        this.transformerName = transformerName;
        this.hatName = "solarHat" + this.name();
        this.hatTexture = new ResourceLocation("compactsolars", "textures/armor/" + hatTexture + ".png");
        this.hatItemTexture = new ResourceLocation("compactsolars", hatTexture);
        this.panels = output;
        this.panelArrayStorage = output * 2;
        this.unlocalizedName = "tile.compactsolars:" + this.name() + "_block";
    }

    /** Returns the valid metadata for a tier, clamping bad values to LV. */
    public static int clampMetadata(int metadata) {
        return metadata >= 0 && metadata < VALUES.length ? metadata : 0;
    }

    /** Returns the tier for a metadata value, with a safe LV fallback. */
    public static CompactSolarType byMetadata(int metadata) {
        return VALUES[clampMetadata(metadata)];
    }

    public int getOutput() {
        return this.output;
    }

    public String getTransformerName() {
        return this.transformerName;
    }

    public int getTier() {
        return this.ordinal() + 1;
    }

    public String getUnlocalizedName() {
        return this.unlocalizedName;
    }

    public String transformerOreName() {
        return "transformer" + this.name();
    }

    public static TileEntityCompactSolar makeEntity(int metadata) {
        if (metadata < 0 || metadata >= VALUES.length) {
            LOG.warn("Invalid CompactSolars metadata {}; falling back to LV.", metadata);
        }

        return new TileEntityCompactSolar(byMetadata(metadata));
    }

    /** Maps the old per-tier tile entity IDs to the unified tile entity's tier. */
    public static CompactSolarType fromLegacyTileEntityId(String id) {
        if (id != null) {
            for (CompactSolarType typ : VALUES) {
                if (typ.tileEntityName()
                    .equals(id)) {
                    return typ;
                }
            }
        }
        return LV;
    }

    public String tileEntityName() {
        return "CompactSolarType." + this.name();
    }

    public ItemSolarHat buildHat() {
        this.item = new ItemSolarHat(this);
        GameRegistry.registerItem(this.item, this.hatName);
        return this.item;
    }

    public ItemSolarHat getHatItem() {
        return this.item;
    }

}
