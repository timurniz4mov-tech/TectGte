package ru.livingcivilizations.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.livingcivilizations.LivingCivilizations;
import ru.livingcivilizations.block.RoadBlock;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, LivingCivilizations.MOD_ID);

    // Default road IDs are now full cube blocks.
    public static final RegistryObject<Block> DIRT_ROAD = BLOCKS.register("dirt_road",
            () -> new RoadBlock(BlockBehaviour.Properties.copy(Blocks.COARSE_DIRT).strength(0.65F).sound(SoundType.GRAVEL), true));
    public static final RegistryObject<Block> GRAVEL_ROAD = BLOCKS.register("gravel_road",
            () -> new RoadBlock(BlockBehaviour.Properties.copy(Blocks.GRAVEL).strength(0.65F).sound(SoundType.GRAVEL), true));
    public static final RegistryObject<Block> PAVED_ROAD = BLOCKS.register("paved_road",
            () -> new RoadBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS).strength(1.8F).sound(SoundType.STONE), true));

    // Optional thin path blocks.
    public static final RegistryObject<Block> DIRT_ROAD_HALF = BLOCKS.register("dirt_road_half",
            () -> new RoadBlock(BlockBehaviour.Properties.copy(Blocks.DIRT).strength(0.55F).sound(SoundType.GRAVEL).noOcclusion(), false));
    public static final RegistryObject<Block> GRAVEL_ROAD_HALF = BLOCKS.register("gravel_road_half",
            () -> new RoadBlock(BlockBehaviour.Properties.copy(Blocks.GRAVEL).strength(0.65F).sound(SoundType.GRAVEL).noOcclusion(), false));
    public static final RegistryObject<Block> PAVED_ROAD_HALF = BLOCKS.register("paved_road_half",
            () -> new RoadBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS).strength(1.8F).sound(SoundType.STONE).noOcclusion(), false));

    // Keep previous full-road IDs for worlds/items already using them.
    public static final RegistryObject<Block> DIRT_ROAD_FULL = BLOCKS.register("dirt_road_full",
            () -> new RoadBlock(BlockBehaviour.Properties.copy(Blocks.COARSE_DIRT).strength(0.65F).sound(SoundType.GRAVEL), true));
    public static final RegistryObject<Block> GRAVEL_ROAD_FULL = BLOCKS.register("gravel_road_full",
            () -> new RoadBlock(BlockBehaviour.Properties.copy(Blocks.GRAVEL).strength(0.65F).sound(SoundType.GRAVEL), true));
    public static final RegistryObject<Block> PAVED_ROAD_FULL = BLOCKS.register("paved_road_full",
            () -> new RoadBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS).strength(1.8F).sound(SoundType.STONE), true));
}
