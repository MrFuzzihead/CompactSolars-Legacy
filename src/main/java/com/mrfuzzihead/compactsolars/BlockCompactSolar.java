package cpw.mods.compactsolars;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public class BlockCompactSolar extends BlockContainer {
   private Random random;
   @SideOnly(Side.CLIENT)
   private IIcon[][] textures;

   public BlockCompactSolar() {
      super(Material.field_151573_f);
      this.func_149663_c("CompactSolar");
      this.func_149711_c(3.0F);
      this.random = new Random();
      this.func_149647_a(CreativeTabs.field_78028_d);
   }

   public TileEntity func_149915_a(World var1, int i) {
      return null;
   }

   public TileEntity createTileEntity(World world, int metadata) {
      return CompactSolarType.makeEntity(metadata);
   }

   public IIcon func_149691_a(int i, int j) {
      return j >= CompactSolarType.values().length ? null : this.textures[j][i > 2 ? 2 : i];
   }

   public boolean func_149727_a(World world, int i, int j, int k, EntityPlayer player, int s, float f1, float f2, float f3) {
      if (player.func_70093_af()) {
         return false;
      } else if (world.field_72995_K) {
         return true;
      } else {
         TileEntity te = world.func_147438_o(i, j, k);
         if (te != null && te instanceof TileEntityCompactSolar) {
            TileEntityCompactSolar tecs = (TileEntityCompactSolar)te;
            player.openGui(CompactSolars.instance, tecs.getType().ordinal(), world, i, j, k);
         }

         return true;
      }
   }

   public int func_149692_a(int i) {
      return i;
   }

   public void func_149749_a(World world, int i, int j, int k, Block par5, int par6) {
      TileEntityCompactSolar tileSolar = (TileEntityCompactSolar)world.func_147438_o(i, j, k);
      if (tileSolar != null) {
         this.dropContent(0, tileSolar, world);
      }

      super.func_149749_a(world, i, j, k, par5, par6);
   }

   public void dropContent(int newSize, TileEntityCompactSolar tileSolar, World world) {
      for (int l = newSize; l < tileSolar.func_70302_i_(); l++) {
         ItemStack itemstack = tileSolar.func_70301_a(l);
         if (itemstack != null) {
            float f = this.random.nextFloat() * 0.8F + 0.1F;
            float f1 = this.random.nextFloat() * 0.8F + 0.1F;
            float f2 = this.random.nextFloat() * 0.8F + 0.1F;

            while (itemstack.field_77994_a > 0) {
               int i1 = this.random.nextInt(21) + 10;
               if (i1 > itemstack.field_77994_a) {
                  i1 = itemstack.field_77994_a;
               }

               itemstack.field_77994_a -= i1;
               EntityItem entityitem = new EntityItem(
                  world,
                  tileSolar.field_145851_c + f,
                  (float)tileSolar.field_145848_d + (newSize > 0 ? 1 : 0) + f1,
                  tileSolar.field_145849_e + f2,
                  new ItemStack(itemstack.func_77973_b(), i1, itemstack.func_77960_j())
               );
               float f3 = 0.05F;
               entityitem.field_70159_w = (float)this.random.nextGaussian() * f3;
               entityitem.field_70181_x = (float)this.random.nextGaussian() * f3 + 0.2F;
               entityitem.field_70179_y = (float)this.random.nextGaussian() * f3;
               if (itemstack.func_77942_o()) {
                  entityitem.func_92059_d().func_77982_d((NBTTagCompound)itemstack.func_77978_p().func_74737_b());
               }

               world.func_72838_d(entityitem);
            }
         }
      }
   }

   @SideOnly(Side.CLIENT)
   public void func_149666_a(Item par1, CreativeTabs par2CreativeTabs, List itemList) {
      for (CompactSolarType type : CompactSolarType.values()) {
         itemList.add(new ItemStack(this, 1, type.ordinal()));
      }
   }

   @SideOnly(Side.CLIENT)
   public void func_149651_a(IIconRegister par1IconRegister) {
      this.textures = new IIcon[CompactSolarType.values().length][3];

      for (CompactSolarType typ : CompactSolarType.values()) {
         for (int i = 0; i < 3; i++) {
            String side = i == 0 ? "Bottom" : (i == 1 ? "Top" : "Side");
            String texName = String.format("compactsolars:%s%s", typ.name().toLowerCase(), side);
            this.textures[typ.ordinal()][i] = par1IconRegister.func_94245_a(texName);
         }
      }
   }
}
