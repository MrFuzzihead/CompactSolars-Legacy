package com.mrfuzzihead.compactsolars;

import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.tile.IWrenchable;

public class TileEntityCompactSolar extends TileEntity implements IInventory, IWrenchable, IEnergySource {

    private static final int SUN_UPDATE_INTERVAL = 128;
    /** IC2's solar generator hardcodes its charge slot to LV, independent of the output tier. */
    private static final int CHARGE_SLOT_TIER = 1;
    private static final String NBT_TIER = "Tier";
    private static final String NBT_ENERGY = "Energy";
    private static final String LEGACY_ENERGY = "IC2BasicSource";

    private static final Random random = new Random();

    private CompactSolarType type;
    private ItemStack[] inventory;
    private boolean initialized;
    public boolean theSunIsVisible;
    private int tick;
    private boolean noSunlight;

    private double capacity;
    private int tier;
    private double power;
    private double energyStored;
    private boolean addedToEnet;

    public TileEntityCompactSolar() {
        this(CompactSolarType.LV);
    }

    public TileEntityCompactSolar(CompactSolarType type) {
        this.inventory = new ItemStack[1];
        this.tick = random.nextInt(SUN_UPDATE_INTERVAL);
        this.applyType(type);
    }

    private void applyType(CompactSolarType type) {
        this.type = type;
        this.capacity = type.panelArrayStorage;
        this.tier = type.getTier();
        this.power = EnergyNet.instance != null ? EnergyNet.instance.getPowerFromTier(this.tier)
            : type.outputPacketSize;
    }

    @Override
    public void validate() {
        super.validate();
        if (this.worldObj != null && !this.worldObj.isRemote && !this.addedToEnet) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
            this.addedToEnet = true;
        }
    }

    @Override
    public void invalidate() {
        this.unloadFromEnet();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        this.unloadFromEnet();
        super.onChunkUnload();
    }

    private void unloadFromEnet() {
        if (this.addedToEnet) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            this.addedToEnet = false;
        }
    }

    @Override
    public void updateEntity() {
        if (this.worldObj == null || this.worldObj.isRemote) {
            return;
        }

        if (!this.initialized) {
            this.noSunlight = this.worldObj.provider.hasNoSky;
            if (this.noSunlight) {
                this.theSunIsVisible = false;
            } else {
                this.updateSunState();
            }
            this.initialized = true;
        }

        if (!this.noSunlight) {
            if (this.tick-- == 0) {
                this.updateSunState();
                this.tick = SUN_UPDATE_INTERVAL;
            }

            double energyProduction = 0.0D;
            if (this.theSunIsVisible) {
                int interval = CompactSolars.sanitizeScaleFactor(CompactSolars.productionRate);
                if (interval == 1 || random.nextInt(interval) == 0) {
                    energyProduction = this.generateEnergy();
                }
            }

            this.addEnergy(energyProduction);
            if (this.inventory[0] != null && this.inventory[0].getItem() instanceof IElectricItem) {
                if (this.charge(this.inventory[0])) {
                    this.markDirty();
                }
            }
        }
    }

    private void updateSunState() {
        this.theSunIsVisible = SolarMath.isSunVisible(this.worldObj, this.xCoord, this.yCoord, this.zCoord);
    }

    private double generateEnergy() {
        int skyLight = SolarMath.skyLight(this.worldObj, this.xCoord, this.zCoord);
        return this.type.panels * SolarMath.perPanelOutput(skyLight, CompactSolars.solarBaseValue);
    }

    private double addEnergy(double amount) {
        if (amount <= 0.0D) {
            return 0.0D;
        }

        double accepted = Math.min(amount, this.capacity - this.energyStored);
        if (accepted <= 0.0D) {
            return 0.0D;
        }

        this.energyStored += accepted;
        return accepted;
    }

    private boolean charge(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof IElectricItem)) {
            return false;
        }

        double used = ElectricItem.manager.charge(stack, this.energyStored, CHARGE_SLOT_TIER, false, false);
        if (used > 0.0D) {
            this.energyStored -= used;
            return true;
        }

        return false;
    }

    @Override
    public double getOfferedEnergy() {
        return Math.min(this.energyStored, this.power);
    }

    @Override
    public void drawEnergy(double amount) {
        this.energyStored = Math.max(0.0D, this.energyStored - amount);
    }

    @Override
    public int getSourceTier() {
        return this.tier;
    }

    @Override
    public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction) {
        return true;
    }

    public ItemStack[] getContents() {
        return this.inventory;
    }

    public int getSizeInventory() {
        return 1;
    }

    public ItemStack getStackInSlot(int i) {
        return this.inventory[i];
    }

    public ItemStack decrStackSize(int i, int j) {
        if (this.inventory[i] != null) {
            if (this.inventory[i].stackSize <= j) {
                ItemStack itemstack = this.inventory[i];
                this.inventory[i] = null;
                this.markDirty();
                return itemstack;
            } else {
                ItemStack itemstack1 = this.inventory[i].splitStack(j);
                if (this.inventory[i].stackSize == 0) {
                    this.inventory[i] = null;
                }

                this.markDirty();
                return itemstack1;
            }
        } else {
            return null;
        }
    }

    public void setInventorySlotContents(int i, ItemStack itemstack) {
        this.inventory[i] = itemstack;
        if (itemstack != null && itemstack.stackSize > this.getInventoryStackLimit()) {
            itemstack.stackSize = this.getInventoryStackLimit();
        }

        this.markDirty();
    }

    public String getInventoryName() {
        return this.type.name();
    }

    public int getInventoryStackLimit() {
        return 64;
    }

    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        if (this.worldObj == null) {
            return true;
        } else {
            return this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord) != this ? false
                : entityplayer.getDistanceSq(this.xCoord + 0.5, this.yCoord + 0.5, this.zCoord + 0.5) <= 64.0;
        }
    }

    public void openInventory() {}

    public void closeInventory() {}

    public boolean wrenchCanSetFacing(EntityPlayer entityPlayer, int side) {
        return false;
    }

    public short getFacing() {
        return 0;
    }

    public void setFacing(short facing) {}

    public boolean wrenchCanRemove(EntityPlayer entityPlayer) {
        return true;
    }

    public float getWrenchDropRate() {
        return CompactSolars.wrenchParity ? 0.9F : 1.0F;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbttagcompound) {
        super.writeToNBT(nbttagcompound);
        nbttagcompound.setByte(NBT_TIER, (byte) this.type.ordinal());
        nbttagcompound.setDouble(NBT_ENERGY, this.energyStored);
        NBTTagList nbttaglist = new NBTTagList();

        for (int i = 0; i < this.inventory.length; i++) {
            if (this.inventory[i] != null) {
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Slot", (byte) i);
                this.inventory[i].writeToNBT(nbttagcompound1);
                nbttaglist.appendTag(nbttagcompound1);
            }
        }

        nbttagcompound.setTag("Items", nbttaglist);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        super.readFromNBT(nbttagcompound);
        this.applyType(readType(nbttagcompound));
        this.energyStored = Math.min(this.capacity, readEnergy(nbttagcompound));
        NBTTagList nbttaglist = nbttagcompound.getTagList("Items", 9);
        this.inventory = new ItemStack[this.getSizeInventory()];

        for (int i = 0; i < nbttaglist.tagCount(); i++) {
            NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);
            int j = nbttagcompound1.getByte("Slot") & 255;
            if (j >= 0 && j < this.inventory.length) {
                this.inventory[j] = ItemStack.loadItemStackFromNBT(nbttagcompound1);
            }
        }
    }

    private static CompactSolarType readType(NBTTagCompound nbttagcompound) {
        if (nbttagcompound.hasKey(NBT_TIER)) {
            return CompactSolarType.byMetadata(nbttagcompound.getByte(NBT_TIER) & 255);
        }
        return CompactSolarType.fromLegacyTileEntityId(nbttagcompound.getString("id"));
    }

    private static double readEnergy(NBTTagCompound nbttagcompound) {
        if (nbttagcompound.hasKey(NBT_ENERGY)) {
            return Math.max(0.0D, nbttagcompound.getDouble(NBT_ENERGY));
        }
        if (nbttagcompound.hasKey(LEGACY_ENERGY)) {
            return Math.max(
                0.0D,
                nbttagcompound.getCompoundTag(LEGACY_ENERGY)
                    .getDouble("energy"));
        }
        return 0.0D;
    }

    public CompactSolarType getType() {
        return this.type;
    }

    public int getTier() {
        return this.tier;
    }

    /**
     * Mirrors IC2's charge-slot validation: the item must be electric, accept the array's tier, and
     * have room for at least some charge. The simulated call ignores the item's transfer limit.
     */
    public boolean isValidCharge(ItemStack itemstack) {
        return itemstack != null && itemstack.getItem() instanceof IElectricItem
            && ElectricItem.manager.charge(itemstack, Double.POSITIVE_INFINITY, CHARGE_SLOT_TIER, true, true) > 0.0D;
    }

    public ItemStack getStackInSlotOnClosing(int var1) {
        if (this.inventory[var1] != null) {
            ItemStack var2 = this.inventory[var1];
            this.inventory[var1] = null;
            return var2;
        } else {
            return null;
        }
    }

    public ItemStack getWrenchDrop(EntityPlayer entityPlayer) {
        return new ItemStack(
            CompactSolars.compactSolarBlock,
            1,
            this.getType()
                .ordinal());
    }

    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack itemstack) {
        return slot == 0 && this.isValidCharge(itemstack);
    }
}
