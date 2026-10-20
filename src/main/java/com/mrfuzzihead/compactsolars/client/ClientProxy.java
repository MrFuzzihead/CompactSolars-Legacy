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
            return new GUISolar(tecs.getType(), player.inventory, tecs);
        } else {
            return null;
        }
    }
}
