package com.mrfuzzihead.compactsolars;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlockWithMetadata;
import net.minecraft.item.ItemStack;

public class ItemCompactSolar extends ItemBlockWithMetadata {

    public ItemCompactSolar(Block b) {
        super(b, CompactSolars.compactSolarBlock);
    }

    public int getMetadata(int i) {
        return i < CompactSolarType.values().length ? i : 0;
    }

    public String getUnlocalizedName(ItemStack itemstack) {
        return "tile.compactsolars:" + CompactSolarType.values()[itemstack.getItemDamage()].name() + "_block";
    }
}
