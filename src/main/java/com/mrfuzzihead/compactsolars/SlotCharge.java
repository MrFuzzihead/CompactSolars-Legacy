package com.mrfuzzihead.compactsolars;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Charge slot for a compact solar array. Delegates validity to the tile entity so the GUI, hoppers,
 * and shift-click all use the same IC2-compatible tier/fullness check.
 */
public class SlotCharge extends Slot {

    private final TileEntityCompactSolar tile;

    public SlotCharge(TileEntityCompactSolar tile, int index, int x, int y) {
        super(tile, index, x, y);
        this.tile = tile;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return this.tile.isValidCharge(stack);
    }
}
