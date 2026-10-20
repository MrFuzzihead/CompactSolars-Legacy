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
import ic2.core.init.MainConfig;
import ic2.core.util.ConfigUtil;

@Mod(
    modid = "CompactSolars",
    name = "CompactSolars",
    version = Tags.VERSION,
    acceptedMinecraftVersions = "1.7.10",
    dependencies = "required-after:IC2")
public class CompactSolars {

    private static final Logger LOG = LogManager.getLogger("CompactSolars");

    @SidedProxy(
        clientSide = "com.mrfuzzihead.compactsolars.client.ClientProxy",
        serverSide = "com.mrfuzzihead.compactsolars.CommonProxy")
    public static CommonProxy proxy;
    public static BlockCompactSolar compactSolarBlock;
    public static int productionRate = 1;
    public static double solarBaseValue = 1.0D;
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

            double ic2SolarBase = readIc2SolarBase();
            Property solarBase = cfg.get("general", "solarBaseValue", ic2SolarBase);
            solarBase.comment = "EU/t per solar panel at full sky light (15). Defaults to IC2's "
                + "balance/energy/generator/solar, usually 1.0.";
            double configuredBase = solarBase.getDouble(ic2SolarBase);
            solarBaseValue = sanitizeSolarBase(configuredBase);
            if (configuredBase != solarBaseValue) {
                LOG.warn("solarBaseValue {} is invalid; using {} instead.", configuredBase, solarBaseValue);
                solarBase.setValue(solarBaseValue);
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

    /** Clamps the per-panel solar base to a finite, non-negative value. */
    public static double sanitizeSolarBase(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0D) {
            return 1.0D;
        }
        return value;
    }

    /** Reads IC2's balance/energy/generator/solar value, falling back to 1.0 if unavailable. */
    private static double readIc2SolarBase() {
        try {
            if (MainConfig.get() != null) {
                return sanitizeSolarBase(ConfigUtil.getDouble(MainConfig.get(), "balance/energy/generator/solar"));
            }
        } catch (Exception e) {
            LOG.warn("Unable to read IC2's solar balance; using 1.0.", e);
        }
        return 1.0D;
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
