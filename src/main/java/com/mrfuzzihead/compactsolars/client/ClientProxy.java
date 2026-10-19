package com.mrfuzzihead.compactsolars.client;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.mrfuzzihead.compactsolars.CommonProxy;
import com.mrfuzzihead.compactsolars.TileEntityCompactSolar;

public class ClientProxy extends CommonProxy {

    @Override
    public void registerTileEntityRenderers() {}

    @Override
    public void registerRenderInformation() {}

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int X, int Y, int Z) {
        TileEntity te = world.getTileEntity(X, Y, Z);
        if (te != null && te instanceof TileEntityCompactSolar) {
            TileEntityCompactSolar tecs = (TileEntityCompactSolar) te;
            return GUISolar.GUI.buildGUI(tecs.getType(), player.inventory, tecs);
        } else {
            return null;
        }
    }
}
