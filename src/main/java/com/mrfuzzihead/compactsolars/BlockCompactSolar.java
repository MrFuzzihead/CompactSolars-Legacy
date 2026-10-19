package com.mrfuzzihead.compactsolars;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockCompactSolar extends BlockContainer {

    private Random random;
    @SideOnly(Side.CLIENT)
    private IIcon[][] textures;

    public BlockCompactSolar() {
        super(Material.iron);
        this.setBlockName("CompactSolar");
        this.setHardness(3.0F);
        this.random = new Random();
        this.setCreativeTab(CreativeTabs.tabRedstone);
    }

    public TileEntity createNewTileEntity(World var1, int i) {
        return null;
    }

    public TileEntity createTileEntity(World world, int metadata) {
        return CompactSolarType.makeEntity(metadata);
    }

    public IIcon getIcon(int i, int j) {
        return j >= CompactSolarType.values().length ? null : this.textures[j][i > 2 ? 2 : i];
    }

    public boolean onBlockActivated(World world, int i, int j, int k, EntityPlayer player, int s, float f1, float f2,
        float f3) {
        if (player.isSneaking()) {
            return false;
        } else if (world.isRemote) {
            return true;
        } else {
            TileEntity te = world.getTileEntity(i, j, k);
            if (te != null && te instanceof TileEntityCompactSolar) {
                TileEntityCompactSolar tecs = (TileEntityCompactSolar) te;
                player.openGui(
                    CompactSolars.instance,
                    tecs.getType()
                        .ordinal(),
                    world,
                    i,
                    j,
                    k);
            }

            return true;
        }
    }

    public int damageDropped(int i) {
        return i;
    }

    public void breakBlock(World world, int i, int j, int k, Block par5, int par6) {
        TileEntityCompactSolar tileSolar = (TileEntityCompactSolar) world.getTileEntity(i, j, k);
        if (tileSolar != null) {
            this.dropContent(0, tileSolar, world);
        }

        super.breakBlock(world, i, j, k, par5, par6);
    }

    public void dropContent(int newSize, TileEntityCompactSolar tileSolar, World world) {
        for (int l = newSize; l < tileSolar.getSizeInventory(); l++) {
            ItemStack itemstack = tileSolar.getStackInSlot(l);
            if (itemstack != null) {
                float f = this.random.nextFloat() * 0.8F + 0.1F;
                float f1 = this.random.nextFloat() * 0.8F + 0.1F;
                float f2 = this.random.nextFloat() * 0.8F + 0.1F;

                while (itemstack.stackSize > 0) {
                    int i1 = this.random.nextInt(21) + 10;
                    if (i1 > itemstack.stackSize) {
                        i1 = itemstack.stackSize;
                    }

                    itemstack.stackSize -= i1;
                    EntityItem entityitem = new EntityItem(
                        world,
                        tileSolar.xCoord + f,
                        (float) tileSolar.yCoord + (newSize > 0 ? 1 : 0) + f1,
                        tileSolar.zCoord + f2,
                        new ItemStack(itemstack.getItem(), i1, itemstack.getItemDamage()));
                    float f3 = 0.05F;
                    entityitem.motionX = (float) this.random.nextGaussian() * f3;
                    entityitem.motionY = (float) this.random.nextGaussian() * f3 + 0.2F;
                    entityitem.motionZ = (float) this.random.nextGaussian() * f3;
                    if (itemstack.hasTagCompound()) {
                        entityitem.getEntityItem()
                            .setTagCompound(
                                (NBTTagCompound) itemstack.getTagCompound()
                                    .copy());
                    }

                    world.spawnEntityInWorld(entityitem);
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item par1, CreativeTabs par2CreativeTabs, List itemList) {
        for (CompactSolarType type : CompactSolarType.values()) {
            itemList.add(new ItemStack(this, 1, type.ordinal()));
        }
    }

    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister par1IconRegister) {
        this.textures = new IIcon[CompactSolarType.values().length][3];

        for (CompactSolarType typ : CompactSolarType.values()) {
            for (int i = 0; i < 3; i++) {
                String side = i == 0 ? "Bottom" : (i == 1 ? "Top" : "Side");
                String texName = String.format(
                    "compactsolars:%s%s",
                    typ.name()
                        .toLowerCase(),
                    side);
                this.textures[typ.ordinal()][i] = par1IconRegister.registerIcon(texName);
            }
        }
    }
}
