/*
 * CompactSolars-Legacy
 * Copyright (c) 2026 Duncan Campbell
 * Derived from CompactSolars, Copyright (c) 2012 cpw and contributors.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.mrfuzzihead.compactsolars;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerCompactSolar extends Container {

    private static final int MACHINE_SLOT = 0;
    private static final int INV_START = 1;
    private static final int INV_END = 28;
    private static final int HOTBAR_START = 28;
    private static final int HOTBAR_END = 37;

    public TileEntityCompactSolar tile;
    private boolean theSunIsVisible;
    private boolean initialized;

    public ContainerCompactSolar(IInventory playerInventory, TileEntityCompactSolar inventory) {
        this.tile = inventory;
        this.layoutContainer(playerInventory, inventory);
    }

    private void layoutContainer(IInventory playerInventory, TileEntityCompactSolar inventory) {
        this.addSlotToContainer(new SlotCharge(inventory, MACHINE_SLOT, 80, 26));

        for (int inventoryRow = 0; inventoryRow < 3; inventoryRow++) {
            for (int inventoryColumn = 0; inventoryColumn < 9; inventoryColumn++) {
                this.addSlotToContainer(
                    new Slot(
                        playerInventory,
                        inventoryColumn + inventoryRow * 9 + 9,
                        8 + inventoryColumn * 18,
                        84 + inventoryRow * 18));
            }
        }

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            this.addSlotToContainer(new Slot(playerInventory, hotbarSlot, 8 + hotbarSlot * 18, 142));
        }
    }

    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        for (ICrafting crafter : this.crafters) {
            if (this.theSunIsVisible != this.tile.theSunIsVisible || !this.initialized) {
                crafter.sendProgressBarUpdate(this, 0, this.tile.theSunIsVisible ? 1 : 0);
            }
        }

        this.initialized = true;
        this.theSunIsVisible = this.tile.theSunIsVisible;
    }

    public void updateProgressBar(int i, int j) {
        if (i == 0) {
            this.tile.theSunIsVisible = j == 1;
        }
    }

    public boolean canInteractWith(EntityPlayer entityplayer) {
        return this.tile.isUseableByPlayer(entityplayer);
    }

    public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int i) {
        ItemStack itemstack = null;
        Slot slot = (Slot) this.inventorySlots.get(i);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (i == MACHINE_SLOT) {
                if (!this.mergeItemStack(itemstack1, INV_START, HOTBAR_END, true)) {
                    return null;
                }
            } else if (i >= INV_START && i < INV_END) {
                if (!this.mergeIntoMachine(itemstack1)
                    && !this.mergeItemStack(itemstack1, HOTBAR_START, HOTBAR_END, false)) {
                    return null;
                }
            } else if (i >= HOTBAR_START && i < HOTBAR_END) {
                if (!this.mergeIntoMachine(itemstack1) && !this.mergeItemStack(itemstack1, INV_START, INV_END, false)) {
                    return null;
                }
            } else if (!this.mergeItemStack(itemstack1, INV_START, HOTBAR_END, false)) {
                return null;
            }

            if (itemstack1.stackSize == 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.stackSize == itemstack.stackSize) {
                return null;
            }

            slot.onPickupFromSlot(par1EntityPlayer, itemstack1);
        }

        return itemstack;
    }

    /**
     * Container.mergeItemStack does not consult Slot.isItemValid, so shift-click has to check the
     * target slot explicitly before trying to move a stack into the charge slot.
     */
    private boolean mergeIntoMachine(ItemStack stack) {
        Slot machine = (Slot) this.inventorySlots.get(MACHINE_SLOT);
        return machine != null && machine.isItemValid(stack)
            && this.mergeItemStack(stack, MACHINE_SLOT, MACHINE_SLOT + 1, false);
    }

}
