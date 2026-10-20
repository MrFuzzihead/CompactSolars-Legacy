package com.mrfuzzihead.compactsolars;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenDesert;

/**
 * IC2-compatible sun and sky-light math. Keeping this separate from the tile entity makes the
 * parity rules explicit and unit-testable.
 */
public final class SolarMath {

    /** IC2 solar panels require at least this top-of-world sky light value. */
    public static final int MIN_SKY_LIGHT = 5;

    private SolarMath() {}

    /** Top-of-world sky light used by IC2's solar generator (includes time of day and weather). */
    public static int skyLight(World world, int x, int z) {
        return world.getBlockLightValue(x, 255, z);
    }

    /**
     * IC2's {@code TileEntitySolarGenerator.isSunVisible}. The block coordinates are the solar's own
     * coordinates; the sky check uses the block above, matching IC2's call convention.
     */
    public static boolean isSunVisible(World world, int x, int y, int z) {
        return isSunVisible(
            skyLight(world, x, z),
            world.provider.hasNoSky,
            world.canBlockSeeTheSky(x, y + 1, z),
            world.isRaining(),
            world.isThundering(),
            world.getWorldChunkManager()
                .getBiomeGenAt(x, z) instanceof BiomeGenDesert);
    }

    /** Pure predicate for tests and for callers that already have the individual conditions. */
    public static boolean isSunVisible(int skyLight, boolean hasNoSky, boolean canSeeSky, boolean raining,
        boolean thundering, boolean desert) {
        return skyLight >= MIN_SKY_LIGHT && !hasNoSky && canSeeSky && (desert || (!raining && !thundering));
    }

    /** IC2 panel output for one panel at the given sky light and configured solar base value. */
    public static double perPanelOutput(int skyLight, double solarBaseValue) {
        if (skyLight < MIN_SKY_LIGHT || solarBaseValue <= 0.0D || Double.isNaN(solarBaseValue)) {
            return 0.0D;
        }
        return solarBaseValue * skyLight / 15.0D;
    }
}
