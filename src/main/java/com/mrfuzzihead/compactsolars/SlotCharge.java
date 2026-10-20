/*
 * CompactSolars-Legacy
 * Copyright (c) 2026 Duncan Campbell
 * Derived from CompactSolars, Copyright (c) 2012 cpw and contributors.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
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
