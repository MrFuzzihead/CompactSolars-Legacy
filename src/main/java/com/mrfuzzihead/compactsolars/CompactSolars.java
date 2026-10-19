package com.mrfuzzihead.compactsolars;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(
    modid = "CompactSolars",
    name = "CompactSolars",
    version = Tags.VERSION,
    acceptedMinecraftVersions = "1.7.10",
    dependencies = "required-after:Forge@[10.12,);required-after:IC2@[2.2.828,)")
public class CompactSolars {

    private static final Logger LOG = LogManager.getLogger("CompactSolars");

    @SidedProxy(
        clientSide = "com.mrfuzzihead.compactsolars.client.ClientProxy",
        serverSide = "com.mrfuzzihead.compactsolars.CommonProxy")
    public static CommonProxy proxy;
    public static BlockCompactSolar compactSolarBlock;
    public static int productionRate = 1;
    @Instance("CompactSolars")
    public static CompactSolars instance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent preinit) {
        Configuration cfg = new Configuration(preinit.getSuggestedConfigurationFile());

        try {
            cfg.load();
            compactSolarBlock = new BlockCompactSolar();
            CompactSolarType.buildHats();

            Property scale = cfg.get("general", "scaleFactor", 1);
            scale.comment = "Generation interval. 1 = generate every tick, 2 = generate on average every other tick, etc. "
                + "Values below 1 are clamped to 1. This does not change the per-generation EU amount (8, 64 or 512 EU).";
            int configuredRate = scale.getInt(1);
            productionRate = sanitizeScaleFactor(configuredRate);
            if (configuredRate < 1) {
                LOG.warn("scaleFactor {} is invalid; using 1 instead.", configuredRate);
                scale.setValue(productionRate);
            }

            cfg.save();
        } catch (Exception e) {
            LOG.error("CompactSolars was unable to load its configuration successfully", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Clamps the configurable generation interval to a usable value. A value below 1 would make
     * {@link java.util.Random#nextInt(int)} throw, so all call sites go through this helper.
     */
    public static int sanitizeScaleFactor(int value) {
        return Math.max(1, value);
    }

    @EventHandler
    public void load(FMLInitializationEvent init) {
        GameRegistry.registerBlock(compactSolarBlock, ItemCompactSolar.class, "CompactSolarBlock");

        for (CompactSolarType typ : CompactSolarType.values()) {
            GameRegistry.registerTileEntity(typ.clazz, typ.tileEntityName());
        }

        proxy.registerTileEntityRenderers();
        proxy.registerRenderInformation();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
    }

    @EventHandler
    public void modsLoaded(FMLPostInitializationEvent postinit) {
        CompactSolarType.generateRecipes(compactSolarBlock);
        CompactSolarType.generateHatRecipes(compactSolarBlock);
    }

    @EventHandler
    public void resetMap(FMLServerStoppingEvent evt) {
        ItemSolarHat.clearRaining();
    }
}
