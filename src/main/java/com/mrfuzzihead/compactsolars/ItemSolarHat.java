package com.mrfuzzihead.compactsolars;

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
import net.minecraftforge.common.util.EnumHelper;

import com.google.common.collect.MapMaker;
import com.google.common.math.IntMath;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;

public class ItemSolarHat extends ItemArmor implements ISpecialArmor {

    private static Random random = new Random();
    private static Map<EntityPlayer, ItemSolarHat.PlayerState> playerState = new MapMaker().weakKeys()
        .makeMap();
    private CompactSolarType type;

    public ItemSolarHat(CompactSolarType type) {
        super(EnumHelper.addArmorMaterial("COMPACTSOLARHAT", 1, new int[] { 1, 1, 1, 1 }, 1), 0, 0);
        this.type = type;
        this.setUnlocalizedName("compactsolars:" + type.hatName);
    }

    public String getArmorTexture(ItemStack stack, Entity entity, int slot, String layerType) {
        return this.type.hatTexture.toString();
    }

    public void onArmorTick(World worldObj, EntityPlayer player, ItemStack itemStack) {
        if (!worldObj.isRemote && !worldObj.provider.hasNoSky) {
            if (CompactSolars.productionRate == 1 || random.nextInt(CompactSolars.productionRate) == 0) {
                int xCoord = MathHelper.floor_double(player.posX);
                int zCoord = MathHelper.floor_double(player.posZ);
                boolean isRaining = false;
                if (!playerState.containsKey(player)) {
                    playerState.put(player, new ItemSolarHat.PlayerState());
                }

                ItemSolarHat.PlayerState state = playerState.get(player);
                if (worldObj.getTotalWorldTime() % 20L == 0L) {
                    boolean canRain = worldObj.getWorldChunkManager()
                        .getBiomeGenAt(xCoord, zCoord)
                        .getIntRainfall() > 0;
                    state.canRain = canRain;
                }

                isRaining = state.canRain && (worldObj.isRaining() || worldObj.isThundering());
                boolean theSunIsVisible = worldObj.isDaytime() && !isRaining
                    && worldObj.canBlockSeeTheSky(xCoord, MathHelper.floor_double(player.posY) + 1, zCoord);
                if (theSunIsVisible) {
                    int available = this.type.getOutput();

                    for (ItemStack is : player.inventory.armorInventory) {
                        if (is != itemStack && is != null && is.getItem() instanceof IElectricItem) {
                            IElectricItem electricItem = (IElectricItem) is.getItem();
                            available = (int) (available
                                - ElectricItem.manager.charge(is, available, this.type.ordinal() + 1, false, false));
                        }
                    }

                    if (available <= 0) {
                        state.buildUp = state.buildUp + IntMath.pow(2, this.type.ordinal());
                    } else {
                        state.buildUp = Math.max(state.buildUp - (worldObj.getTotalWorldTime() - state.lastTick), 0L);
                    }

                    state.lastTick = worldObj.getTotalWorldTime();
                    int dose = IntMath.pow(10, this.type.ordinal()) * 5;
                    if (state.buildUp > dose) {
                        player.addPotionEffect(new PotionEffect(Potion.confusion.id, dose >> 2, 0));
                        state.buildUp -= dose;
                    }
                }
            }
        }
    }

    public static void clearRaining() {
        playerState.clear();
    }

    public ArmorProperties getProperties(EntityLivingBase player, ItemStack armor, DamageSource source, double damage,
        int slot) {
        return new ArmorProperties(0, 0.0, 0);
    }

    public int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot) {
        return 0;
    }

    public void damageArmor(EntityLivingBase entity, ItemStack stack, DamageSource source, int damage, int slot) {}

    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister par1IconRegister) {
        this.itemIcon = par1IconRegister.registerIcon(this.type.hatItemTexture.toString());
    }

    private class PlayerState {

        boolean canRain;
        public long buildUp;
        public long lastTick;

        private PlayerState() {}
    }
}
