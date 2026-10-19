package cpw.mods.compactsolars.client;

import cpw.mods.compactsolars.CompactSolarType;
import cpw.mods.compactsolars.ContainerCompactSolar;
import cpw.mods.compactsolars.TileEntityCompactSolar;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class GUISolar extends GuiContainer {
   public static final ResourceLocation gui = new ResourceLocation("ic2", "textures/gui/GUISolarGenerator.png");
   private GUISolar.GUI type;
   private ContainerCompactSolar container = (ContainerCompactSolar)this.field_147002_h;

   private GUISolar(GUISolar.GUI type, IInventory player, TileEntityCompactSolar chest) {
      super(type.makeContainer(player, chest));
      this.type = type;
      this.field_146291_p = false;
   }

   protected void func_146979_b(int p1, int p2) {
      this.field_146289_q.func_78276_b(this.type.mainType.friendlyName, 8, 6, 4210752);
      this.field_146289_q.func_78276_b("Inventory", 8, this.field_147000_g - 96 + 2, 4210752);
   }

   protected void func_146976_a(float f, int i, int j) {
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.field_146297_k.field_71446_o.func_110577_a(gui);
      int l = (this.field_146294_l - this.field_146999_f) / 2;
      int i1 = (this.field_146295_m - this.field_147000_g) / 2;
      this.func_73729_b(l, i1, 0, 0, this.field_146999_f, this.field_147000_g);
      if (this.container.tile.theSunIsVisible) {
         this.func_73729_b(l + 80, i1 + 45, 176, 0, 14, 14);
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

      public static GUISolar buildGUI(CompactSolarType type, IInventory playerInventory, TileEntityCompactSolar solarTile) {
         for (GUISolar.GUI gui : values()) {
            if (solarTile.getType() == gui.mainType) {
               return new GUISolar(gui, playerInventory, solarTile);
            }
         }

         return null;
      }
   }
}
