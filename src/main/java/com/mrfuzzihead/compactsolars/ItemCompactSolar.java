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
