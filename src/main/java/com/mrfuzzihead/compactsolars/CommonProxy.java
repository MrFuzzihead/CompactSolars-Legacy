package cpw.mods.compactsolars;

import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CommonProxy implements IGuiHandler {
   public void registerTileEntityRenderers() {
   }

   public void registerRenderInformation() {
   }

   public Object getServerGuiElement(int ID, EntityPlayer player, World world, int X, int Y, int Z) {
      TileEntity te = world.func_147438_o(X, Y, Z);
      if (te != null && te instanceof TileEntityCompactSolar) {
         TileEntityCompactSolar tecs = (TileEntityCompactSolar)te;
         return new ContainerCompactSolar(player.field_71071_by, tecs, tecs.getType());
      } else {
         return null;
      }
   }

   public Object getClientGuiElement(int ID, EntityPlayer player, World world, int X, int Y, int Z) {
      return null;
   }
}
