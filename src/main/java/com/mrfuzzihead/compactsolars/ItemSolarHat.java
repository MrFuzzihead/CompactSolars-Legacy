package com.mrfuzzihead.compactsolars;

import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
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

    private static final ArmorMaterial HAT_MATERIAL = EnumHelper
        .addArmorMaterial("COMPACTSOLARS_SOLAR_HAT", 0, new int[] { 0, 0, 0, 0 }, 0);
    private static final Random random = new Random();
    private static final Map<EntityPlayer, ItemSolarHat.PlayerState> playerState = new MapMaker().weakKeys()
        .makeMap();

    private final CompactSolarType type;

    public ItemSolarHat(CompactSolarType type) {
        super(HAT_MATERIAL, 0, 0);
        this.type = type;
        this.setUnlocalizedName("compactsolars:" + type.hatName);
        this.setMaxDamage(0);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, int slot, String layerType) {
        return this.type.hatTexture.toString();
    }

    @Override
    public void onArmorTick(World worldObj, EntityPlayer player, ItemStack itemStack) {
        if (worldObj.isRemote || worldObj.provider.hasNoSky) {
            return;
        }

        int interval = CompactSolars.sanitizeScaleFactor(CompactSolars.productionRate);
        if (interval != 1 && random.nextInt(interval) != 0) {
            return;
        }

        int xCoord = MathHelper.floor_double(player.posX);
        int yCoord = MathHelper.floor_double(player.posY);
        int zCoord = MathHelper.floor_double(player.posZ);

        ItemSolarHat.PlayerState state = playerState.get(player);
        if (state == null) {
            state = new ItemSolarHat.PlayerState();
            playerState.put(player, state);
        }

        long now = worldObj.getTotalWorldTime();
        long elapsed = Math.max(0L, now - state.lastTick);
        state.lastTick = now;

        if (SolarMath.isSunVisible(worldObj, xCoord, yCoord, zCoord)) {
            double available = this.type.getOutput();

            for (ItemStack is : player.inventory.armorInventory) {
                if (is != itemStack && is != null && is.getItem() instanceof IElectricItem) {
                    available -= ElectricItem.manager.charge(is, available, this.type.getTier(), false, false);
                }
            }

            if (available <= 0.0D) {
                state.buildUp += IntMath.pow(2, this.type.ordinal());
            } else {
                state.buildUp = Math.max(state.buildUp - elapsed, 0L);
            }

            int dose = IntMath.pow(10, this.type.ordinal()) * 5;
            if (state.buildUp > dose) {
                player.addPotionEffect(new PotionEffect(Potion.confusion.id, dose >> 2, 0));
                state.buildUp -= dose;
            }
        } else {
            state.buildUp = Math.max(state.buildUp - elapsed, 0L);
        }
    }

    public static void clearRaining() {
        playerState.clear();
    }

    @Override
    public ArmorProperties getProperties(EntityLivingBase player, ItemStack armor, DamageSource source, double damage,
        int slot) {
        return new ArmorProperties(0, 0.0, 0);
    }

    @Override
    public int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot) {
        return 0;
    }

    @Override
    public void damageArmor(EntityLivingBase entity, ItemStack stack, DamageSource source, int damage, int slot) {}

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister par1IconRegister) {
        this.itemIcon = par1IconRegister.registerIcon(this.type.hatItemTexture.toString());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack itemstack, EntityPlayer player, List list, boolean advanced) {
        list.add(
            StatCollector.translateToLocal("ic2.item.tooltip.PowerOutput") + " "
                + this.type.getOutput()
                + " EU/t "
                + StatCollector.translateToLocal("ic2.item.tooltip.max"));
        list.add(StatCollector.translateToLocal("ic2.item.tooltip.PowerTier") + " " + this.type.getTier());
        String descKey = "item.compactsolars:solarHat.desc";
        String description = StatCollector.translateToLocal(descKey);
        list.add(description.equals(descKey) ? "Charges equipped electric armor while in sunlight." : description);
    }

    private static class PlayerState {

        private long buildUp;
        private long lastTick;
    }
}
