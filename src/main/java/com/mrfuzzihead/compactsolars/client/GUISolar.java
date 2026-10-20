package com.mrfuzzihead.compactsolars.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.compactsolars.CompactSolarType;
import com.mrfuzzihead.compactsolars.ContainerCompactSolar;
import com.mrfuzzihead.compactsolars.TileEntityCompactSolar;

public class GUISolar extends GuiContainer {

    public static final ResourceLocation gui = new ResourceLocation("ic2", "textures/gui/GUISolarGenerator.png");
    private GUISolar.GUI type;
    private final String title;
    private ContainerCompactSolar container = (ContainerCompactSolar) this.inventorySlots;

    private GUISolar(GUISolar.GUI type, IInventory player, TileEntityCompactSolar chest) {
        super(type.makeContainer(player, chest));
        this.type = type;
        this.allowUserInput = false;
        this.title = StatCollector.translateToLocal(type.mainType.getUnlocalizedName() + ".name");
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

    public static enum GUI {

        LV(CompactSolarType.LV),
        MV(CompactSolarType.MV),
        HV(CompactSolarType.HV);

        private CompactSolarType mainType;

        private GUI(CompactSolarType mainType) {
            this.mainType = mainType;
        }

        protected Container makeContainer(IInventory player, TileEntityCompactSolar solarTile) {
            return new ContainerCompactSolar(player, solarTile, this.mainType);
        }

        public static GUISolar buildGUI(CompactSolarType type, IInventory playerInventory,
            TileEntityCompactSolar solarTile) {
            for (GUISolar.GUI gui : values()) {
                if (solarTile.getType() == gui.mainType) {
                    return new GUISolar(gui, playerInventory, solarTile);
                }
            }

            return null;
        }
    }
}
