package ru.livingcivilizations.economy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;

/** Small paid player constructions; all checks happen before any blocks are changed. */
public final class PurchasedBuildings {
    public static BlockPos buildHouseNear(ServerLevel level, BlockPos origin) {
        BlockPos base = findFlatSite(level, origin, 3, 5);
        if (base == null) return null;

        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            level.setBlock(base.offset(x, 0, z), Blocks.SPRUCE_PLANKS.defaultBlockState(), 3);
            boolean corner = Math.abs(x) == 3 && Math.abs(z) == 3;
            for (int y = 1; y <= 3; y++) {
                boolean wall = Math.abs(x) == 3 || Math.abs(z) == 3;
                if (!wall) continue;
                if (z == 3 && x == 0 && y <= 2) continue; // doorway
                if (y == 2 && ((Math.abs(x) == 3 && z == 0) || (Math.abs(z) == 3 && x == 0))) {
                    level.setBlock(base.offset(x, y, z), Blocks.GLASS_PANE.defaultBlockState(), 3);
                } else {
                    level.setBlock(base.offset(x, y, z), (corner ? Blocks.SPRUCE_LOG : Blocks.SPRUCE_PLANKS).defaultBlockState(), 3);
                }
            }
            level.setBlock(base.offset(x, 4, z), Blocks.SPRUCE_SLAB.defaultBlockState(), 3);
        }
        BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        level.setBlock(base.offset(0, 1, 3), door, 3);
        level.setBlock(base.offset(0, 2, 3), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
        level.setBlock(base.offset(0, 1, 0), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        level.setBlock(base.offset(-1, 1, 0), Blocks.TORCH.defaultBlockState(), 3);
        level.setBlock(base.offset(1, 1, 0), Blocks.CHEST.defaultBlockState(), 3);
        return base;
    }

    public static BlockPos buildFarmNear(ServerLevel level, BlockPos origin) {
        BlockPos base = findFlatSite(level, origin, 3, 5);
        if (base == null) return null;

        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            if (Math.abs(x) == 3 || Math.abs(z) == 3) {
                level.setBlock(base.offset(x, 0, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
            } else if (x == 0 && z == 0) {
                level.setBlock(base.offset(x, 0, z), Blocks.WATER.defaultBlockState(), 3);
            } else {
                level.setBlock(base.offset(x, 0, z), Blocks.FARMLAND.defaultBlockState(), 3);
                level.setBlock(base.offset(x, 1, z), Blocks.WHEAT.defaultBlockState()
                        .setValue(CropBlock.AGE, 0), 3);
            }
        }
        return base;
    }

    private static BlockPos findFlatSite(ServerLevel level, BlockPos origin, int radius, int clearance) {
        for (int attempt = 0; attempt < 32; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            int distance = 7 + level.random.nextInt(9);
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * distance);
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            BlockPos ground = top.below();
            if (!level.getBlockState(ground).isSolidRender(level, ground) || !level.getFluidState(ground).isEmpty()) continue;
            int y = ground.getY();
            boolean valid = true;
            for (int dx = -radius; dx <= radius && valid; dx++) for (int dz = -radius; dz <= radius && valid; dz++) {
                BlockPos support = new BlockPos(x + dx, y, z + dz);
                if (!level.hasChunkAt(support) || !level.getBlockState(support).isSolidRender(level, support)
                        || !level.getFluidState(support).isEmpty()) {
                    valid = false;
                    break;
                }
                for (int dy = 1; dy <= clearance; dy++) {
                    BlockPos air = support.above(dy);
                    if (!level.hasChunkAt(air)) { valid = false; break; }
                    BlockState state = level.getBlockState(air);
                    if (!state.isAir() && !state.canBeReplaced()) { valid = false; break; }
                }
            }
            if (valid) return ground;
        }
        return null;
    }

    private PurchasedBuildings() { }
}
