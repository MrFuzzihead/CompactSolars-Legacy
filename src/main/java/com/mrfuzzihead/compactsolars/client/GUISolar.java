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
package com.mrfuzzihead.compactsolars.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.compactsolars.CompactSolarType;
import com.mrfuzzihead.compactsolars.ContainerCompactSolar;
import com.mrfuzzihead.compactsolars.TileEntityCompactSolar;

public class GUISolar extends GuiContainer {

    public static final ResourceLocation gui = new ResourceLocation("ic2", "textures/gui/GUISolarGenerator.png");
    private final ContainerCompactSolar container;
    private final String title;

    public GUISolar(CompactSolarType type, IInventory player, TileEntityCompactSolar chest) {
        super(new ContainerCompactSolar(player, chest));
        this.container = (ContainerCompactSolar) this.inventorySlots;
        this.allowUserInput = false;
        this.title = StatCollector.translateToLocal(type.getUnlocalizedName() + ".name");
    }

    protected void drawGuiContainerForegroundLayer(int p1, int p2) {
        this.fontRendererObj.drawString(this.title, 8, 6, 4210752);
        this.fontRendererObj
            .drawString(StatCollector.translateToLocal("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
    }

    protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture(gui);
        int l = (this.width - this.xSize) / 2;
        int i1 = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(l, i1, 0, 0, this.xSize, this.ySize);
        if (this.container.tile.theSunIsVisible) {
            this.drawTexturedModalRect(l + 80, i1 + 45, 176, 0, 14, 14);
        }
    }
}
