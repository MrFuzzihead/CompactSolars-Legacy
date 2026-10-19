package cpw.mods.compactsolars.client;

import cpw.mods.compactsolars.CommonProxy;
import cpw.mods.compactsolars.TileEntityCompactSolar;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class ClientProxy extends CommonProxy {
   @Override
   public void registerTileEntityRenderers() {
   }

   @Override
   public void registerRenderInformation() {
   }

   @Override
   public Object getClientGuiElement(int ID, EntityPlayer player, World world, int X, int Y, int Z) {
      TileEntity te = world.func_147438_o(X, Y, Z);
      if (te != null && te instanceof TileEntityCompactSolar) {
         TileEntityCompactSolar tecs = (TileEntityCompactSolar)te;
         return GUISolar.GUI.buildGUI(tecs.getType(), player.field_71071_by, tecs);
      } else {
         return null;
      }
   }
}
