package cpw.mods.compactsolars;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerCompactSolar extends Container {
   public TileEntityCompactSolar tile;
   private boolean theSunIsVisible;
   private boolean initialized;
   private EntityPlayer myPlayer;

   public ContainerCompactSolar(IInventory playerInventory, TileEntityCompactSolar inventory, CompactSolarType type) {
      this.tile = inventory;
      this.myPlayer = ((InventoryPlayer)playerInventory).field_70458_d;
      this.layoutContainer(playerInventory, inventory, type);
   }

   private void layoutContainer(IInventory playerInventory, IInventory inventory, CompactSolarType type) {
      this.func_75146_a(new Slot(inventory, 0, 80, 26));

      for (int inventoryRow = 0; inventoryRow < 3; inventoryRow++) {
         for (int inventoryColumn = 0; inventoryColumn < 9; inventoryColumn++) {
            this.func_75146_a(new Slot(playerInventory, inventoryColumn + inventoryRow * 9 + 9, 8 + inventoryColumn * 18, 84 + inventoryRow * 18));
         }
      }

      for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
         this.func_75146_a(new Slot(playerInventory, hotbarSlot, 8 + hotbarSlot * 18, 142));
      }
   }

   public void func_75142_b() {
      super.func_75142_b();

      for (ICrafting crafter : this.field_75149_d) {
         if (this.theSunIsVisible != this.tile.theSunIsVisible || !this.initialized) {
            crafter.func_71112_a(this, 0, this.tile.theSunIsVisible ? 1 : 0);
         }
      }

      this.initialized = true;
      this.theSunIsVisible = this.tile.theSunIsVisible;
   }

   public void func_75137_b(int i, int j) {
      if (i == 0) {
         this.tile.theSunIsVisible = j == 1;
      }
   }

   public boolean func_75145_c(EntityPlayer entityplayer) {
      return this.tile.func_70300_a(entityplayer);
   }

   public ItemStack func_82846_b(EntityPlayer par1EntityPlayer, int i) {
      ItemStack itemstack = null;
      Slot slot = (Slot)this.field_75151_b.get(i);
      if (slot != null && slot.func_75216_d()) {
         ItemStack itemstack1 = slot.func_75211_c();
         itemstack = itemstack1.func_77946_l();
         if (i == 0) {
            if (!this.func_75135_a(itemstack1, 1, 37, true)) {
               return null;
            }
         } else if (i >= 1 && i < 28) {
            if (!this.func_75135_a(itemstack1, 28, 37, false)) {
               return null;
            }
         } else if (i >= 28 && i < 37) {
            if (!this.func_75135_a(itemstack1, 1, 27, false)) {
               return null;
            }
         } else if (!this.func_75135_a(itemstack1, 1, 37, false)) {
            return null;
         }

         if (itemstack1.field_77994_a == 0) {
            slot.func_75215_d(null);
         } else {
            slot.func_75218_e();
         }

         if (itemstack1.field_77994_a == itemstack.field_77994_a) {
            return null;
         }

         slot.func_82870_a(par1EntityPlayer, itemstack1);
      }

      return itemstack;
   }

   public EntityPlayer getPlayer() {
      return this.myPlayer;
   }
}
