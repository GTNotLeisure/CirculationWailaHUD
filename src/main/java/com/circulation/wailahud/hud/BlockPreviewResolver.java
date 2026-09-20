package com.circulation.wailahud.hud;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.client.renderer.tileentity.TileEntityChestRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockPreviewResolver {

    public Preview resolve(World world, int blockX, int blockY, int blockZ) {
        TileEntity tileEntity = world.getTileEntity(blockX, blockY, blockZ);
        AxisAlignedBB bounds = AxisAlignedBB
            .getBoundingBox(blockX, blockY, blockZ, blockX + 1.0D, blockY + 1.0D, blockZ + 1.0D);
        if (!(tileEntity instanceof TileEntityChest chest)) return new Preview(tileEntity, bounds);

        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        TileEntitySpecialRenderer renderer = dispatcher.getSpecialRenderer(chest);
        Block block = world.getBlock(blockX, blockY, blockZ);
        if (!(renderer instanceof TileEntityChestRenderer) || !(block instanceof BlockChest chestBlock)) {
            return new Preview(tileEntity, bounds);
        }

        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (direction.offsetY != 0) continue;
            int adjacentX = blockX + direction.offsetX;
            int adjacentZ = blockZ + direction.offsetZ;
            if (!world.blockExists(adjacentX, blockY, adjacentZ)) continue;
            Block adjacentBlock = world.getBlock(adjacentX, blockY, adjacentZ);
            if (!(adjacentBlock instanceof BlockChest adjacentChestBlock)
                || adjacentChestBlock.field_149956_a != chestBlock.field_149956_a) continue;
            TileEntity adjacentTile = world.getTileEntity(adjacentX, blockY, adjacentZ);
            if (!(adjacentTile instanceof TileEntityChest adjacentChest) || adjacentChest.isInvalid()
                || dispatcher.getSpecialRenderer(adjacentChest) != renderer) continue;

            // The vanilla renderer draws both halves only from the negative-coordinate chest.
            TileEntityChest owner = adjacentX < blockX || adjacentZ < blockZ ? adjacentChest : chest;
            AxisAlignedBB combinedBounds = bounds.addCoord(direction.offsetX, 0.0D, direction.offsetZ);
            return new Preview(owner, combinedBounds);
        }
        return new Preview(tileEntity, bounds);
    }

    public record Preview(TileEntity tileEntity, AxisAlignedBB bounds) {}
}
