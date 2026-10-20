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

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlockWithMetadata;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public class ItemCompactSolar extends ItemBlockWithMetadata {

    public ItemCompactSolar(Block b) {
        super(b, b);
    }

    public int getMetadata(int metadata) {
        return CompactSolarType.clampMetadata(metadata);
    }

    public String getUnlocalizedName(ItemStack itemstack) {
        return CompactSolarType.byMetadata(itemstack.getItemDamage())
            .getUnlocalizedName();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack itemstack, EntityPlayer player, List list, boolean advanced) {
        CompactSolarType type = CompactSolarType.byMetadata(itemstack.getItemDamage());
        list.add(
            StatCollector.translateToLocal("ic2.item.tooltip.PowerOutput") + " "
                + type.getOutput()
                + " EU/t "
                + StatCollector.translateToLocal("ic2.item.tooltip.max"));
    }
}
