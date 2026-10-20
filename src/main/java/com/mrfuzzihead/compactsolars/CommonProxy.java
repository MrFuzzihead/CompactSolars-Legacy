package com.mrfuzzihead.compactsolars;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.IGuiHandler;

public class CommonProxy implements IGuiHandler {

    public void registerTileEntityRenderers() {}

    public void registerRenderInformation() {}

    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int X, int Y, int Z) {
        TileEntity te = world.getTileEntity(X, Y, Z);
        if (te != null && te instanceof TileEntityCompactSolar) {
            TileEntityCompactSolar tecs = (TileEntityCompactSolar) te;
            return new ContainerCompactSolar(player.inventory, tecs);
        } else {
            return null;
        }
    }

    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int X, int Y, int Z) {
        return null;
    }
}
