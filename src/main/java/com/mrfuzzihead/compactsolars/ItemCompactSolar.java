package cpw.mods.compactsolars;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlockWithMetadata;
import net.minecraft.item.ItemStack;

public class ItemCompactSolar extends ItemBlockWithMetadata {
   public ItemCompactSolar(Block b) {
      super(b, CompactSolars.compactSolarBlock);
   }

   public int func_77647_b(int i) {
      return i < CompactSolarType.values().length ? i : 0;
   }

   public String func_77667_c(ItemStack itemstack) {
      return "tile.compactsolars:" + CompactSolarType.values()[itemstack.func_77960_j()].name() + "_block";
   }
}
