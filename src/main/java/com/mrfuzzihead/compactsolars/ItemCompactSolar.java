package com.mrfuzzihead.compactsolars;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlockWithMetadata;
import net.minecraft.item.ItemStack;

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
}
