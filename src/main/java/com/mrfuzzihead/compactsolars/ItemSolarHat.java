package cpw.mods.compactsolars;

import com.google.common.collect.MapMaker;
import com.google.common.math.IntMath;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.ISpecialArmor;
import net.minecraftforge.common.ISpecialArmor.ArmorProperties;
import net.minecraftforge.common.util.EnumHelper;

public class ItemSolarHat extends ItemArmor implements ISpecialArmor {
   private static Random random = new Random();
   private static Map<EntityPlayer, ItemSolarHat.PlayerState> playerState = new MapMaker().weakKeys().makeMap();
   private CompactSolarType type;

   public ItemSolarHat(CompactSolarType type) {
      super(EnumHelper.addArmorMaterial("COMPACTSOLARHAT", 1, new int[]{1, 1, 1, 1}, 1), 0, 0);
      this.type = type;
      this.func_77655_b("compactsolars:" + type.hatName);
   }

   public String getArmorTexture(ItemStack stack, Entity entity, int slot, String layerType) {
      return this.type.hatTexture.toString();
   }

   public void onArmorTick(World worldObj, EntityPlayer player, ItemStack itemStack) {
      if (!worldObj.field_72995_K && !worldObj.field_73011_w.field_76576_e) {
         if (CompactSolars.productionRate == 1 || random.nextInt(CompactSolars.productionRate) == 0) {
            int xCoord = MathHelper.func_76128_c(player.field_70165_t);
            int zCoord = MathHelper.func_76128_c(player.field_70161_v);
            boolean isRaining = false;
            if (!playerState.containsKey(player)) {
               playerState.put(player, new ItemSolarHat.PlayerState());
            }

            ItemSolarHat.PlayerState state = playerState.get(player);
            if (worldObj.func_82737_E() % 20L == 0L) {
               boolean canRain = worldObj.func_72959_q().func_76935_a(xCoord, zCoord).func_76744_g() > 0;
               state.canRain = canRain;
            }

            isRaining = state.canRain && (worldObj.func_72896_J() || worldObj.func_72911_I());
            boolean theSunIsVisible = worldObj.func_72935_r()
               && !isRaining
               && worldObj.func_72937_j(xCoord, MathHelper.func_76128_c(player.field_70163_u) + 1, zCoord);
            if (theSunIsVisible) {
               int available = this.type.getOutput();

               for (ItemStack is : player.field_71071_by.field_70460_b) {
                  if (is != itemStack && is != null && is.func_77973_b() instanceof IElectricItem) {
                     IElectricItem electricItem = (IElectricItem)is.func_77973_b();
                     available = (int)(available - ElectricItem.manager.charge(is, available, this.type.ordinal() + 1, false, false));
                  }
               }

               if (available <= 0) {
                  state.buildUp = state.buildUp + IntMath.pow(2, this.type.ordinal());
               } else {
                  state.buildUp = Math.max(state.buildUp - (worldObj.func_82737_E() - state.lastTick), 0L);
               }

               state.lastTick = worldObj.func_82737_E();
               int dose = IntMath.pow(10, this.type.ordinal()) * 5;
               if (state.buildUp > dose) {
                  player.func_70690_d(new PotionEffect(Potion.field_76431_k.field_76415_H, dose >> 2, 0));
                  state.buildUp -= dose;
               }
            }
         }
      }
   }

   public static void clearRaining() {
      playerState.clear();
   }

   public ArmorProperties getProperties(EntityLivingBase player, ItemStack armor, DamageSource source, double damage, int slot) {
      return new ArmorProperties(0, 0.0, 0);
   }

   public int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot) {
      return 0;
   }

   public void damageArmor(EntityLivingBase entity, ItemStack stack, DamageSource source, int damage, int slot) {
   }

   @SideOnly(Side.CLIENT)
   public void func_94581_a(IIconRegister par1IconRegister) {
      this.field_77791_bV = par1IconRegister.func_94245_a(this.type.hatItemTexture.toString());
   }

   private class PlayerState {
      boolean canRain;
      public long buildUp;
      public long lastTick;

      private PlayerState() {
      }
   }
}
