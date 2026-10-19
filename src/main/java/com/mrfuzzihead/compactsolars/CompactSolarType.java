package com.mrfuzzihead.compactsolars;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.registry.GameRegistry;
import ic2.api.item.IC2Items;

public enum CompactSolarType {

    LV(8, 32, "Low Voltage Solar Array", "lvTransformer", TileEntityCompactSolar.class, "lvHat"),
    MV(64, 128, "Medium Voltage Solar Array", "mvTransformer", TileEntityCompactSolarMV.class, "mvHat"),
    HV(512, 512, "High Voltage Solar Array", "hvTransformer", TileEntityCompactSolarHV.class, "hvHat");

    private static final CompactSolarType[] VALUES = values();
    private static final Logger LOG = LogManager.getLogger("CompactSolars");

    /** Ore dictionary names used by the array recipes. Other mods may register alternatives. */
    public static final String ORE_SOLAR_PANEL = "solarPanel";
    public static final String ORE_HELMET_IRON = "helmetIron";

    private int output;
    public Class<? extends TileEntityCompactSolar> clazz;
    public String friendlyName;
    public String transformerName;
    public final ResourceLocation hatTexture;
    public final String hatName;
    private ItemSolarHat item;
    public final ResourceLocation hatItemTexture;
    public final int outputPacketSize;
    public final int maxStorage;
    private final String unlocalizedName;

    private CompactSolarType(int output, int outputPacketSize, String friendlyName, String transformerName,
        Class<? extends TileEntityCompactSolar> clazz, String hatTexture) {
        this.output = output;
        this.outputPacketSize = outputPacketSize;
        this.friendlyName = friendlyName;
        this.transformerName = transformerName;
        this.clazz = clazz;
        this.hatName = "solarHat" + this.name();
        this.hatTexture = new ResourceLocation("compactsolars", "textures/armor/" + hatTexture + ".png");
        this.hatItemTexture = new ResourceLocation("compactsolars", hatTexture);
        this.maxStorage = outputPacketSize << 1;
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

    public static void generateRecipes(BlockCompactSolar block) {
        if (block == null) {
            LOG.error("Cannot register CompactSolars recipes: the compact solar block is null.");
            return;
        }

        ItemStack solar = IC2Items.getItem("solarPanel");
        if (solar == null) {
            LOG.error("Cannot register CompactSolars recipes: IC2 solarPanel is missing.");
            return;
        }

        OreDictionary.registerOre(ORE_SOLAR_PANEL, solar);
        ItemStack parent = solar;

        for (CompactSolarType typ : VALUES) {
            ItemStack transformer = IC2Items.getItem(typ.transformerName);
            if (transformer == null) {
                LOG.error(
                    "Cannot register the {} compact solar recipe: IC2 {} is missing.",
                    typ.name(),
                    typ.transformerName);
                return;
            }

            String transformerOre = typ.transformerOreName();
            OreDictionary.registerOre(transformerOre, transformer);

            ItemStack target = new ItemStack(block, 1, typ.ordinal());
            Object source = typ == LV ? ORE_SOLAR_PANEL : parent;
            GameRegistry.addRecipe(new ShapedOreRecipe(target, "SSS", "SXS", "SSS", 'S', source, 'X', transformerOre));

            parent = target;
        }
    }

    public int getOutput() {
        return this.output;
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

        try {
            return byMetadata(metadata).clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create a CompactSolars tile entity for metadata " + metadata, e);
        }
    }

    public int getTextureRow() {
        return this.ordinal();
    }

    public String tileEntityName() {
        return "CompactSolarType." + this.name();
    }

    public ItemSolarHat buildHat() {
        this.item = new ItemSolarHat(this);
        GameRegistry.registerItem(this.item, this.hatName);
        return this.item;
    }

    public static void buildHats() {
        for (CompactSolarType typ : VALUES) {
            typ.buildHat();
        }
    }

    public static void generateHatRecipes(BlockCompactSolar block) {
        if (block == null) {
            LOG.error("Cannot register CompactSolars hat recipes: the compact solar block is null.");
            return;
        }

        OreDictionary.registerOre(ORE_HELMET_IRON, new ItemStack(Items.iron_helmet));

        for (CompactSolarType typ : VALUES) {
            if (typ.item == null) {
                LOG.error("Cannot register the {} solar hat recipe: the hat item is not registered.", typ.name());
                continue;
            }

            GameRegistry.addRecipe(
                new ShapelessOreRecipe(
                    new ItemStack(typ.item),
                    new ItemStack(block, 1, typ.ordinal()),
                    ORE_HELMET_IRON));
        }
    }
}
