package cpw.mods.compactsolars;

import ic2.api.energy.prefab.BasicSource;
import ic2.api.item.IElectricItem;
import ic2.api.tile.IWrenchable;
import java.util.Random;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

public class TileEntityCompactSolar extends TileEntity implements IInventory, IWrenchable {
   private BasicSource energySource;
   private static Random random = new Random();
   private CompactSolarType type;
   private ItemStack[] inventory;
   private boolean initialized;
   public boolean theSunIsVisible;
   private int tick;
   private boolean canRain;
   private boolean noSunlight;

   public TileEntityCompactSolar() {
      this(CompactSolarType.LV);
   }

   public TileEntityCompactSolar(CompactSolarType type) {
      this.type = type;
      this.inventory = new ItemStack[1];
      this.tick = random.nextInt(64);
      this.energySource = new BasicSource(this, type.maxStorage, type.ordinal() + 1);
   }

   public void func_145845_h() {
      this.energySource.onUpdateEntity();
      if (!this.initialized && this.field_145850_b != null) {
         this.canRain = this.field_145850_b.func_72959_q().func_76935_a(this.field_145851_c, this.field_145849_e).func_76744_g() > 0;
         this.noSunlight = this.field_145850_b.field_73011_w.field_76576_e;
         this.initialized = true;
      }

      if (!this.noSunlight) {
         if (this.tick-- == 0) {
            this.updateSunState();
            this.tick = 64;
         }

         int energyProduction = 0;
         if (this.theSunIsVisible && (CompactSolars.productionRate == 1 || random.nextInt(CompactSolars.productionRate) == 0)) {
            energyProduction = this.generateEnergy();
         }

         this.energySource.addEnergy(energyProduction);
         if (this.inventory[0] != null && this.inventory[0].func_77973_b() instanceof IElectricItem) {
            this.energySource.charge(this.inventory[0]);
         }
      }
   }

   private void updateSunState() {
      boolean isRaining = this.canRain && (this.field_145850_b.func_72896_J() || this.field_145850_b.func_72911_I());
      this.theSunIsVisible = this.field_145850_b.func_72935_r()
         && !isRaining
         && this.field_145850_b.func_72937_j(this.field_145851_c, this.field_145848_d + 1, this.field_145849_e);
   }

   private int generateEnergy() {
      return this.type.getOutput();
   }

   public ItemStack[] getContents() {
      return this.inventory;
   }

   public int func_70302_i_() {
      return 1;
   }

   public ItemStack func_70301_a(int i) {
      return this.inventory[i];
   }

   public ItemStack func_70298_a(int i, int j) {
      if (this.inventory[i] != null) {
         if (this.inventory[i].field_77994_a <= j) {
            ItemStack itemstack = this.inventory[i];
            this.inventory[i] = null;
            this.func_70296_d();
            return itemstack;
         } else {
            ItemStack itemstack1 = this.inventory[i].func_77979_a(j);
            if (this.inventory[i].field_77994_a == 0) {
               this.inventory[i] = null;
            }

            this.func_70296_d();
            return itemstack1;
         }
      } else {
         return null;
      }
   }

   public void func_70299_a(int i, ItemStack itemstack) {
      this.inventory[i] = itemstack;
      if (itemstack != null && itemstack.field_77994_a > this.func_70297_j_()) {
         itemstack.field_77994_a = this.func_70297_j_();
      }

      this.func_70296_d();
   }

   public String func_145825_b() {
      return this.type.name();
   }

   public int func_70297_j_() {
      return 64;
   }

   public boolean func_70300_a(EntityPlayer entityplayer) {
      if (this.field_145850_b == null) {
         return true;
      } else {
         return this.field_145850_b.func_147438_o(this.field_145851_c, this.field_145848_d, this.field_145849_e) != this
            ? false
            : entityplayer.func_70092_e(this.field_145851_c + 0.5, this.field_145848_d + 0.5, this.field_145849_e + 0.5) <= 64.0;
      }
   }

   public void func_70295_k_() {
   }

   public void func_70305_f() {
   }

   public boolean wrenchCanSetFacing(EntityPlayer entityPlayer, int side) {
      return false;
   }

   public short getFacing() {
      return 0;
   }

   public void setFacing(short facing) {
   }

   public boolean wrenchCanRemove(EntityPlayer entityPlayer) {
      return true;
   }

   public float getWrenchDropRate() {
      return 1.0F;
   }

   public void func_145841_b(NBTTagCompound nbttagcompound) {
      super.func_145841_b(nbttagcompound);
      NBTTagList nbttaglist = new NBTTagList();

      for (int i = 0; i < this.inventory.length; i++) {
         if (this.inventory[i] != null) {
            NBTTagCompound nbttagcompound1 = new NBTTagCompound();
            nbttagcompound1.func_74774_a("Slot", (byte)i);
            this.inventory[i].func_77955_b(nbttagcompound1);
            nbttaglist.func_74742_a(nbttagcompound1);
         }
      }

      nbttagcompound.func_74782_a("Items", nbttaglist);
      this.energySource.onWriteToNbt(nbttagcompound);
   }

   public void func_145839_a(NBTTagCompound nbttagcompound) {
      super.func_145839_a(nbttagcompound);
      this.energySource.onReadFromNbt(nbttagcompound);
      NBTTagList nbttaglist = nbttagcompound.func_150295_c("Items", 9);
      this.inventory = new ItemStack[this.func_70302_i_()];

      for (int i = 0; i < nbttaglist.func_74745_c(); i++) {
         NBTTagCompound nbttagcompound1 = nbttaglist.func_150305_b(i);
         int j = nbttagcompound1.func_74771_c("Slot") & 255;
         if (j >= 0 && j < this.inventory.length) {
            this.inventory[j] = ItemStack.func_77949_a(nbttagcompound1);
         }
      }
   }

   public CompactSolarType getType() {
      return this.type;
   }

   public void onChunkUnload() {
      this.energySource.onChunkUnload();
   }

   public void func_145843_s() {
      this.energySource.onInvalidate();
      super.func_145843_s();
   }

   public ItemStack func_70304_b(int var1) {
      if (this.inventory[var1] != null) {
         ItemStack var2 = this.inventory[var1];
         this.inventory[var1] = null;
         return var2;
      } else {
         return null;
      }
   }

   public ItemStack getWrenchDrop(EntityPlayer entityPlayer) {
      return new ItemStack(CompactSolars.compactSolarBlock, 1, this.getType().ordinal());
   }

   public boolean func_145818_k_() {
      return false;
   }

   public boolean func_94041_b(int i, ItemStack itemstack) {
      return itemstack != null && itemstack.func_77973_b() instanceof IElectricItem;
   }
}
