package ru.livingcivilizations.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Road tile that can be either a thin 1/8-height path or a full cube. */
public class RoadBlock extends Block {
    private static final VoxelShape HALF_SHAPE =
            Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D);
    private final VoxelShape shape;

    public RoadBlock(Properties properties, boolean fullBlock) {
        super(properties);
        this.shape = fullBlock ? Shapes.block() : HALF_SHAPE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }
}
