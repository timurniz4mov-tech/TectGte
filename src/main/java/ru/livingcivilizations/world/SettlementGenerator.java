package ru.livingcivilizations.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import ru.livingcivilizations.entity.CivilizationNpc;
import ru.livingcivilizations.kingdom.KingdomSavedData;
import ru.livingcivilizations.registry.ModBlocks;
import ru.livingcivilizations.registry.ModEntities;

/** Builds persistent, visible medieval settlements on suitable flat land near explored areas. */
public final class SettlementGenerator {
    private SettlementGenerator() { }

    public static boolean generateNear(ServerLevel level, BlockPos origin, boolean force) {
        if (level.dimension() != Level.OVERWORLD) return false;
        SettlementSavedData saved = SettlementSavedData.get(level);
        if (!force && saved.hasNearby(origin, 320)) return false;

        BlockPos center = findSite(level, origin, force);
        if (center == null) return false;
        // Last safety check: all chunks modified by the build must still be loaded.
        for (int x = -18; x <= 18; x += 3) for (int z = -18; z <= 18; z += 3) {
            if (!level.hasChunkAt(new BlockPos(center.getX() + x, 0, center.getZ() + z))) return false;
        }

        int number = saved.count() + 1;
        String kingdomName = "Королевство_" + number;
        KingdomSavedData.get(level.getServer()).createNeutral(kingdomName,
                center.getX(), center.getZ(), 160);
        prepareClearing(level, center);
        buildVillageSquare(level, center);

        buildHouse(level, center.offset(-10, 0, -10), Direction.SOUTH);
        buildHouse(level, center.offset(10, 0, -10), Direction.SOUTH);
        buildHouse(level, center.offset(-10, 0, 10), Direction.NORTH);
        buildHouse(level, center.offset(10, 0, 10), Direction.NORTH);

        // Main crossroad and four branches to the houses. Road blocks occupy the ground block,
        // not the air block above it (the old placement was one block too high).
        for (int i = -18; i <= 18; i++) {
            for (int width = -1; width <= 1; width++) {
                placeRoad(level, center, i, width, ModBlocks.GRAVEL_ROAD.get());
                placeRoad(level, center, width, i, ModBlocks.GRAVEL_ROAD.get());
            }
        }
        connectHouse(level, center, -10, -10);
        connectHouse(level, center, 10, -10);
        connectHouse(level, center, -10, 10);
        connectHouse(level, center, 10, 10);

        // Paved central square, dirt side trails, farm and market.
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            placeRoad(level, center, x, z, ModBlocks.PAVED_ROAD.get());
        for (int i = 1; i <= 10; i++) {
            placeRoad(level, center, -i, -7, ModBlocks.DIRT_ROAD.get());
            placeRoad(level, center, i, -7, ModBlocks.DIRT_ROAD.get());
            placeRoad(level, center, -i, 7, ModBlocks.DIRT_ROAD.get());
            placeRoad(level, center, i, 7, ModBlocks.DIRT_ROAD.get());
        }
        buildWell(level, center);
        buildMarket(level, center.offset(4, 0, 2));
        buildFarm(level, center.offset(17, 0, -2));

        spawn(level, ModEntities.VILLAGE_MERCHANT.get(), center.offset(3, 1, 2), kingdomName);
        spawn(level, ModEntities.CARAVAN_MERCHANT.get(), center.offset(5, 1, 2), kingdomName);
        spawn(level, ModEntities.WORKER.get(), center.offset(17, 1, -2), kingdomName);
        spawn(level, ModEntities.WORKER.get(), center.offset(-16, 1, 3), kingdomName);
        spawn(level, ModEntities.GUARD.get(), center.offset(-3, 1, -14), kingdomName);
        spawn(level, ModEntities.GUARD.get(), center.offset(3, 1, 14), kingdomName);
        spawn(level, ModEntities.TRAVELER.get(), center.offset(0, 1, -21), kingdomName);

        // Ordinary villages have a local lord; every third settlement is also a royal seat.
        if (number % 3 != 0) spawn(level, ModEntities.LORD.get(), center.offset(-3, 1, 4), kingdomName);
        if (number % 3 == 0) {
            BlockPos castleCandidate = findTerrainGround(level, center.getX() + 29, center.getZ());
            BlockPos castle = castleCandidate == null ? center.offset(29, 0, 0) : castleCandidate;
            if (castleCandidate != null) prepareSmallClearing(level, castle, 8);
            buildCastle(level, castle);
            for (int z = -1; z <= 1; z++) for (int x = 18; x <= 29; x++)
                placeRoad(level, center, x, z, ModBlocks.PAVED_ROAD.get());
            spawn(level, ModEntities.KING.get(), castle.offset(0, 1, 0), kingdomName);
            spawn(level, ModEntities.LORD.get(), castle.offset(2, 1, 0), kingdomName);
            spawn(level, ModEntities.GUARD.get(), castle.offset(-4, 1, 0), kingdomName);
            spawn(level, ModEntities.GUARD.get(), castle.offset(4, 1, 0), kingdomName);
            spawn(level, ModEntities.WARRIOR.get(), castle.offset(0, 1, 4), kingdomName);
        }

        saved.add(center);
        return true;
    }

    /**
     * Lets a worker expand a settlement with one small timber house.
     * Each worker completes at most one house; the saved entity tag prevents repeats
     * after the world is reloaded.
     */
    public static boolean tryWorkerBuildHouse(ServerLevel level, CivilizationNpc worker) {
        if (worker.getRole() != ru.livingcivilizations.entity.NpcRole.WORKER) return false;
        net.minecraft.nbt.CompoundTag data = worker.getPersistentData();
        if (data.getBoolean("LC_HouseBuilt")) return false;
        long now = level.getGameTime();
        if (now < data.getLong("LC_NextHouseTry")) return false;
        data.putLong("LC_NextHouseTry", now + 2400L);

        // Builders only expand a known village, not random wilderness.
        if (!SettlementSavedData.get(level).hasNearby(worker.blockPosition(), 150)) return false;

        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            int distance = 9 + level.random.nextInt(7);
            int x = worker.blockPosition().getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = worker.blockPosition().getZ() + (int) Math.round(Math.sin(angle) * distance);
            BlockPos base = findTerrainGround(level, x, z);
            if (base == null || !isWorkerHouseSiteClear(level, base)) continue;

            prepareSmallClearing(level, base, 4);
            Direction front = Direction.SOUTH;
            buildHouse(level, base, front);
            // Add a short dirt path from the doorway to the village route.
            for (int i = 1; i <= 5; i++) {
                BlockPos path = base.relative(front, 3 + i);
                placeRoad(level, path, 0, 0, ModBlocks.DIRT_ROAD.get());
            }
            worker.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            data.putBoolean("LC_HouseBuilt", true);
            worker.setCustomName(net.minecraft.network.chat.Component.literal("Строитель"));
            worker.setCustomNameVisible(false);
            return true;
        }
        return false;
    }

    private static boolean isWorkerHouseSiteClear(ServerLevel level, BlockPos center) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int x = -4; x <= 4; x += 2) {
            for (int z = -4; z <= 4; z += 2) {
                BlockPos ground = findTerrainGround(level, center.getX() + x, center.getZ() + z);
                if (ground == null || !level.hasChunkAt(ground)) return false;
                if (!level.getFluidState(ground).isEmpty()) return false;
                minY = Math.min(minY, ground.getY());
                maxY = Math.max(maxY, ground.getY());
                if (maxY - minY > 2) return false;
                for (int y = 1; y <= 5; y++) {
                    BlockPos above = new BlockPos(ground.getX(), center.getY() + y, ground.getZ());
                    BlockState state = level.getBlockState(above);
                    if (!state.isAir() && !state.canBeReplaced()
                            && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)) return false;
                }
            }
        }
        return true;
    }

    private static BlockPos findSite(ServerLevel level, BlockPos origin, boolean force) {
        // Keep the complete 37x37 build area inside already-loaded chunks. The previous
        // 84-168 block search often landed outside the simulation distance, where the
        // heightmap returned no usable terrain and the village was never generated.
        for (int attempt = 0; attempt < 96; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            int distance = force
                    ? 12 + level.random.nextInt(13)  // command: 12..24, close and reliable
                    : 20 + level.random.nextInt(15); // automatic: 20..34, still in loaded chunks
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * distance);
            BlockPos ground = findTerrainGround(level, x, z);
            if (ground != null && isFlatClearSite(level, ground)) return ground;
        }
        return null;
    }

    private static BlockPos findTerrainGround(ServerLevel level, int x, int z) {
        BlockPos column = new BlockPos(x, 0, z);
        if (!level.hasChunkAt(column)) return null;
        BlockPos height = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        for (int y = height.getY(); y > level.getMinBuildHeight() + 5; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(Blocks.SNOW)) continue;
            if (!level.getFluidState(pos).isEmpty()) continue;
            if (state.isSolidRender(level, pos)) return pos;
        }
        return null;
    }

    private static boolean isFlatClearSite(ServerLevel level, BlockPos center) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        // Sample the whole build footprint. A little slope is accepted because the
        // clearing routine levels it with dirt/air before placing houses and road tiles.
        for (int x = -18; x <= 18; x += 6) {
            for (int z = -18; z <= 18; z += 6) {
                BlockPos column = new BlockPos(center.getX() + x, 0, center.getZ() + z);
                if (!level.hasChunkAt(column)) return false;
                BlockPos ground = findTerrainGround(level, column.getX(), column.getZ());
                if (ground == null || !level.getFluidState(ground).isEmpty()) return false;
                minY = Math.min(minY, ground.getY());
                maxY = Math.max(maxY, ground.getY());
                if (maxY - minY > 7) return false;
            }
        }

        // Do not overwrite existing houses/large constructions. Trees, leaves and
        // plants are deliberately allowed: the village clears a 37x37 forest patch.
        for (int x = -15; x <= 15; x += 5) {
            for (int z = -15; z <= 15; z += 5) {
                BlockPos ground = findTerrainGround(level, center.getX() + x, center.getZ() + z);
                if (ground == null) return false;
                for (int y = 1; y <= 10; y++) {
                    BlockPos p = ground.above(y);
                    BlockState state = level.getBlockState(p);
                    if (!state.isAir() && !state.canBeReplaced()
                            && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)) return false;
                    if (!level.getFluidState(p).isEmpty()) return false;
                }
            }
        }
        return true;
    }

    private static void prepareClearing(ServerLevel level, BlockPos center) {
        for (int x = -18; x <= 18; x++) for (int z = -18; z <= 18; z++) {
            int worldX = center.getX() + x;
            int worldZ = center.getZ() + z;
            BlockPos currentSurface = findTerrainGround(level, worldX, worldZ);
            if (currentSurface != null) {
                if (currentSurface.getY() < center.getY()) {
                    for (int y = currentSurface.getY() + 1; y <= center.getY(); y++)
                        level.setBlock(new BlockPos(worldX, y, worldZ), Blocks.DIRT.defaultBlockState(), 3);
                } else if (currentSurface.getY() > center.getY()) {
                    for (int y = center.getY() + 1; y <= currentSurface.getY(); y++)
                        level.setBlock(new BlockPos(worldX, y, worldZ), Blocks.AIR.defaultBlockState(), 3);
                }
            }
            level.setBlock(new BlockPos(worldX, center.getY(), worldZ), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            for (int y = 1; y <= 16; y++) {
                BlockPos above = center.offset(x, y, z);
                BlockState state = level.getBlockState(above);
                if (!state.isAir() && (state.canBeReplaced() || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)))
                    level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void prepareSmallClearing(ServerLevel level, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            int worldX = center.getX() + x;
            int worldZ = center.getZ() + z;
            BlockPos ground = findTerrainGround(level, worldX, worldZ);
            if (ground != null && ground.getY() < center.getY()) {
                for (int y = ground.getY() + 1; y <= center.getY(); y++)
                    level.setBlock(new BlockPos(worldX, y, worldZ), Blocks.DIRT.defaultBlockState(), 3);
            } else if (ground != null && ground.getY() > center.getY()) {
                for (int y = center.getY() + 1; y <= ground.getY(); y++)
                    level.setBlock(new BlockPos(worldX, y, worldZ), Blocks.AIR.defaultBlockState(), 3);
            }
            level.setBlock(new BlockPos(worldX, center.getY(), worldZ), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            for (int y = 1; y <= 16; y++) {
                BlockPos above = center.offset(x, y, z);
                BlockState state = level.getBlockState(above);
                if (!state.isAir() && (state.canBeReplaced() || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)))
                    level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void buildVillageSquare(ServerLevel level, BlockPos center) {
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++)
            placeRoad(level, center, x, z, ModBlocks.PAVED_ROAD.get());
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            level.setBlock(center.offset(x, 0, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        // Lamp post
        level.setBlock(center.offset(0, 1, 0), Blocks.OAK_FENCE.defaultBlockState(), 3);
        level.setBlock(center.offset(0, 2, 0), Blocks.LANTERN.defaultBlockState(), 3);
    }

    private static void buildHouse(ServerLevel level, BlockPos base, Direction front) {
        BlockState floor = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState wall = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            level.setBlock(base.offset(x, 0, z), floor, 3);
            boolean edge = Math.abs(x) == 3 || Math.abs(z) == 3;
            if (!edge) continue;
            boolean corner = Math.abs(x) == 3 && Math.abs(z) == 3;
            for (int y = 1; y <= 3; y++) {
                BlockState state = corner ? log : wall;
                boolean doorCell = (front == Direction.SOUTH && x == 0 && z == 3)
                        || (front == Direction.NORTH && x == 0 && z == -3)
                        || (front == Direction.WEST && x == -3 && z == 0)
                        || (front == Direction.EAST && x == 3 && z == 0);
                if (doorCell) continue;
                boolean window = y == 2 && !corner && ((Math.abs(x) == 3 && z == 0) || (Math.abs(z) == 3 && x == 0));
                if (window) state = Blocks.GLASS_PANE.defaultBlockState();
                level.setBlock(base.offset(x, y, z), state, 3);
            }
        }
        BlockState lowerDoor = Blocks.SPRUCE_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, front).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upperDoor = lowerDoor.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        BlockPos door = switch (front) {
            case SOUTH -> base.offset(0, 1, 3);
            case NORTH -> base.offset(0, 1, -3);
            case WEST -> base.offset(-3, 1, 0);
            default -> base.offset(3, 1, 0);
        };
        level.setBlock(door, lowerDoor, 3);
        level.setBlock(door.above(), upperDoor, 3);
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++)
            level.setBlock(base.offset(x, 4, z), Blocks.SPRUCE_SLAB.defaultBlockState(), 3);
        level.setBlock(base.offset(-1, 1, -1), Blocks.BARREL.defaultBlockState(), 3);
        level.setBlock(base.offset(1, 1, -1), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        level.setBlock(base.offset(0, 1, 1), Blocks.RED_BED.defaultBlockState(), 3);
        level.setBlock(base.offset(2, 1, 1), Blocks.CHEST.defaultBlockState(), 3);
    }

    private static void connectHouse(ServerLevel level, BlockPos center, int houseX, int houseZ) {
        // L-shaped dirt path that meets the crossroad and the village-house entrance.
        int doorZ = houseZ < 0 ? houseZ + 7 : houseZ - 7;
        int doorX = houseX;
        for (int x = Math.min(0, doorX); x <= Math.max(0, doorX); x++)
            placeRoad(level, center, x, doorZ, ModBlocks.DIRT_ROAD.get());
        for (int z = Math.min(0, doorZ); z <= Math.max(0, doorZ); z++)
            placeRoad(level, center, 0, z, ModBlocks.DIRT_ROAD.get());
    }

    private static void buildWell(ServerLevel level, BlockPos center) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (Math.abs(x) == 1 || Math.abs(z) == 1)
                level.setBlock(center.offset(x, 1, z), Blocks.COBBLESTONE.defaultBlockState(), 3);
        }
        level.setBlock(center.offset(0, 1, 0), Blocks.WATER.defaultBlockState(), 3);
        for (int x : new int[]{-1, 1}) for (int z : new int[]{-1, 1})
            level.setBlock(center.offset(x, 2, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
        level.setBlock(center.offset(0, 3, 0), Blocks.OAK_FENCE.defaultBlockState(), 3);
        level.setBlock(center.offset(0, 4, 0), Blocks.SPRUCE_SLAB.defaultBlockState(), 3);
    }

    private static void buildMarket(ServerLevel level, BlockPos base) {
        for (int x = -2; x <= 2; x++) for (int z = -1; z <= 1; z++)
            level.setBlock(base.offset(x, 0, z), Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), 3);
        for (int x : new int[]{-2, 2}) for (int z : new int[]{-1, 1}) {
            level.setBlock(base.offset(x, 1, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
            level.setBlock(base.offset(x, 2, z), Blocks.RED_WOOL.defaultBlockState(), 3);
        }
        for (int x = -2; x <= 2; x++) for (int z = -1; z <= 1; z++)
            level.setBlock(base.offset(x, 3, z), Blocks.RED_WOOL.defaultBlockState(), 3);
        level.setBlock(base.offset(0, 1, 0), Blocks.BARREL.defaultBlockState(), 3);
    }

    private static void buildFarm(ServerLevel level, BlockPos base) {
        for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) {
            if (x == 3) {
                level.setBlock(base.offset(x, 0, z), Blocks.WATER.defaultBlockState(), 3);
            } else {
                BlockPos crop = base.offset(x, 0, z);
                level.setBlock(crop, Blocks.FARMLAND.defaultBlockState(), 3);
                level.setBlock(crop.above(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7), 3);
            }
        }
        for (int x = -1; x <= 7; x++) {
            level.setBlock(base.offset(x, 0, -1), Blocks.OAK_FENCE.defaultBlockState(), 3);
            level.setBlock(base.offset(x, 0, 7), Blocks.OAK_FENCE.defaultBlockState(), 3);
        }
        for (int z = 0; z < 7; z++) {
            level.setBlock(base.offset(-1, 0, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
            level.setBlock(base.offset(7, 0, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
        }
    }

    private static void buildCastle(ServerLevel level, BlockPos base) {
        for (int x = -7; x <= 7; x++) for (int z = -7; z <= 7; z++) {
            boolean wall = Math.abs(x) == 7 || Math.abs(z) == 7;
            if (wall) for (int y = 1; y <= 4; y++) {
                boolean opening = z == 7 && x == 0 && y <= 2;
                if (!opening) level.setBlock(base.offset(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
            }
            if (Math.abs(x) == 7 && Math.abs(z) == 7) {
                for (int y = 5; y <= 8; y++)
                    level.setBlock(base.offset(x, y, z), Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 3);
            }
        }
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            level.setBlock(base.offset(x, 0, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
            boolean edge = Math.abs(x) == 3 || Math.abs(z) == 3;
            if (edge) for (int y = 1; y <= 5; y++)
                level.setBlock(base.offset(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++)
            level.setBlock(base.offset(x, 6, z), Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3);
        level.setBlock(base.offset(0, 1, -3), Blocks.IRON_DOOR.defaultBlockState(), 3);
    }

    private static void placeRoad(ServerLevel level, BlockPos center, int dx, int dz, net.minecraft.world.level.block.Block road) {
        RoadBuilder.place(level, center.offset(dx, 0, dz), road);
    }

    private static void spawn(ServerLevel level, EntityType<CivilizationNpc> type, BlockPos feetPos) {
        spawn(level, type, feetPos, "");
    }

    private static void spawn(ServerLevel level, EntityType<CivilizationNpc> type, BlockPos feetPos, String kingdomName) {
        CivilizationNpc npc = type.create(level);
        if (npc == null) return;
        if (kingdomName != null && !kingdomName.isBlank()) npc.getPersistentData().putString("LC_Kingdom", kingdomName);
        BlockPos support = feetPos.below();
        double supportHeight = level.getBlockState(support)
                .getCollisionShape(level, support, net.minecraft.world.phys.shapes.CollisionContext.empty())
                .max(Direction.Axis.Y);
        if (supportHeight <= 0.0D) supportHeight = 1.0D;
        npc.moveTo(feetPos.getX() + 0.5D, support.getY() + supportHeight, feetPos.getZ() + 0.5D,
                level.random.nextFloat() * 360F, 0F);
        npc.setPersistenceRequired();
        level.addFreshEntity(npc);
    }
}
