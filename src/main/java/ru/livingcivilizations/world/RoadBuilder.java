package ru.livingcivilizations.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;

/** Places full-cube road blocks and builds a stone foundation through water or air. */
public final class RoadBuilder {
    private RoadBuilder() {}

    public static void place(ServerLevel level, BlockPos roadPos, Block road) {
        if (!level.hasChunkAt(roadPos)) return;
        BlockPos below = roadPos.below();
        boolean unsupported = !level.getBlockState(below).isSolidRender(level, below)
                || !level.getFluidState(roadPos).isEmpty()
                || !level.getFluidState(below).isEmpty();
        if (unsupported) {
            int y = below.getY();
            while (y > level.getMinBuildHeight() + 1) {
                BlockPos scan = new BlockPos(roadPos.getX(), y, roadPos.getZ());
                if (level.getBlockState(scan).isSolidRender(level, scan)
                        && level.getFluidState(scan).isEmpty()) break;
                y--;
            }
            for (int fillY = y + 1; fillY < roadPos.getY(); fillY++) {
                BlockPos fill = new BlockPos(roadPos.getX(), fillY, roadPos.getZ());
                level.setBlock(fill, Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
        }
        level.setBlock(roadPos, road.defaultBlockState(), 3);
    }
}
