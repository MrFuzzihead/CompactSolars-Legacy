package com.mrfuzzihead.compactsolars;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import com.google.common.base.Throwables;

import cpw.mods.fml.common.registry.GameRegistry;
import ic2.api.item.IC2Items;

public enum CompactSolarType {

    LV(8, 32, "Low Voltage Solar Array", "lvTransformer", TileEntityCompactSolar.class, "lvHat"),
    MV(64, 128, "Medium Voltage Solar Array", "mvTransformer", TileEntityCompactSolarMV.class, "mvHat"),
    HV(512, 512, "High Voltage Solar Array", "hvTransformer", TileEntityCompactSolarHV.class, "hvHat");

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
    }

    public static void generateRecipes(BlockCompactSolar block) {
        ItemStack solar = IC2Items.getItem("solarPanel");
        ItemStack parent = solar;

        for (CompactSolarType typ : values()) {
            ItemStack targ = new ItemStack(block, 1, typ.ordinal());
            ItemStack transformer = IC2Items.getItem(typ.transformerName);
            addRecipe(targ, "SSS", "SXS", "SSS", 'S', parent, 'X', transformer);
            parent = targ;
        }
    }

    private static void addRecipe(ItemStack target, Object... args) {
        GameRegistry.addRecipe(target, args);
    }

    public int getOutput() {
        return this.output;
    }

    public static TileEntityCompactSolar makeEntity(int metadata) {
        int solartype = metadata;

        try {
            return values()[solartype].clazz.newInstance();
        } catch (Exception var3) {
            throw Throwables.propagate(var3);
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
        for (CompactSolarType typ : values()) {
            typ.buildHat();
        }
    }

    public static void generateHatRecipes(BlockCompactSolar block) {
        Item ironHat = Items.iron_helmet;

        for (CompactSolarType typ : values()) {
            ItemStack solarBlock = new ItemStack(block, 0, typ.ordinal());
            GameRegistry.addShapelessRecipe(new ItemStack(typ.item), new Object[] { solarBlock, ironHat });
        }
    }
}
