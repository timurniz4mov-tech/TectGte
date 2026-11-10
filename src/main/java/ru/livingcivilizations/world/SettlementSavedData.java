package ru.livingcivilizations.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Remembers generated villages so exploration does not create one every few minutes. */
public class SettlementSavedData extends SavedData {
    private static final String DATA_NAME = "living_civilizations_settlements";
    private final List<BlockPos> centers = new ArrayList<>();

    public static SettlementSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                SettlementSavedData::load, SettlementSavedData::new, DATA_NAME);
    }

    public boolean hasNearby(BlockPos pos, int radius) {
        long radiusSquared = (long) radius * radius;
        for (BlockPos center : centers) {
            long dx = (long) pos.getX() - center.getX();
            long dz = (long) pos.getZ() - center.getZ();
            if (dx * dx + dz * dz < radiusSquared) return true;
        }
        return false;
    }

    public void add(BlockPos center) {
        centers.add(center.immutable());
        setDirty();
    }

    public int count() { return centers.size(); }

    /** Returns the nearest known settlement within a reasonable pathfinding range. */
    public BlockPos findNearestBeyond(BlockPos origin, int minimumDistance, int maximumDistance) {
        long min2 = (long) minimumDistance * minimumDistance;
        long max2 = (long) maximumDistance * maximumDistance;
        BlockPos best = null;
        long bestDistance = Long.MAX_VALUE;
        for (BlockPos center : centers) {
            long dx = (long) center.getX() - origin.getX();
            long dz = (long) center.getZ() - origin.getZ();
            long distance = dx * dx + dz * dz;
            if (distance >= min2 && distance <= max2 && distance < bestDistance) {
                best = center;
                bestDistance = distance;
            }
        }
        return best;
    }

    public static SettlementSavedData load(CompoundTag tag) {
        SettlementSavedData data = new SettlementSavedData();
        ListTag list = tag.getList("Centers", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            data.centers.add(new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BlockPos pos : centers) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", pos.getX());
            entry.putInt("Y", pos.getY());
            entry.putInt("Z", pos.getZ());
            list.add(entry);
        }
        tag.put("Centers", list);
        return tag;
    }
}
