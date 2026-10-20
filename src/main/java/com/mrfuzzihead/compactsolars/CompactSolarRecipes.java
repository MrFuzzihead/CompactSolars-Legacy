package com.mrfuzzihead.compactsolars;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.registry.GameRegistry;
import ic2.api.item.IC2Items;

/** Array and hat recipe registration for CompactSolars. */
public final class CompactSolarRecipes {

    public static final String ORE_SOLAR_PANEL = "solarPanel";
    public static final String ORE_HELMET_IRON = "helmetIron";

    private static final Logger LOG = LogManager.getLogger("CompactSolars");

    private CompactSolarRecipes() {}

    public static void register(BlockCompactSolar block) {
        generateArrayRecipes(block);
        generateHatRecipes(block);
    }

    public static void generateArrayRecipes(BlockCompactSolar block) {
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

        for (CompactSolarType typ : CompactSolarType.values()) {
            ItemStack transformer = IC2Items.getItem(typ.getTransformerName());
            if (transformer == null) {
                LOG.error(
                    "Cannot register the {} compact solar recipe: IC2 {} is missing.",
                    typ.name(),
                    typ.getTransformerName());
                return;
            }

            String transformerOre = typ.transformerOreName();
            OreDictionary.registerOre(transformerOre, transformer);

            ItemStack target = new ItemStack(block, 1, typ.ordinal());
            Object source = typ == CompactSolarType.LV ? ORE_SOLAR_PANEL : parent;
            GameRegistry.addRecipe(new ShapedOreRecipe(target, "SSS", "SXS", "SSS", 'S', source, 'X', transformerOre));

            parent = target;
        }
    }

    public static void generateHatRecipes(BlockCompactSolar block) {
        if (block == null) {
            LOG.error("Cannot register CompactSolars hat recipes: the compact solar block is null.");
            return;
        }

        OreDictionary.registerOre(ORE_HELMET_IRON, new ItemStack(Items.iron_helmet));

        for (CompactSolarType typ : CompactSolarType.values()) {
            ItemSolarHat hat = typ.getHatItem();
            if (hat == null) {
                LOG.error("Cannot register the {} solar hat recipe: the hat item is not registered.", typ.name());
                continue;
            }

            GameRegistry.addRecipe(
                new ShapelessOreRecipe(new ItemStack(hat), new ItemStack(block, 1, typ.ordinal()), ORE_HELMET_IRON));
        }
    }
}
