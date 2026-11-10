package ru.livingcivilizations.kingdom;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class KingdomSavedData extends SavedData {
    private static final String DATA_NAME = "living_civilizations_kingdoms";
    private final List<Kingdom> kingdoms = new ArrayList<>();

    public static KingdomSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                KingdomSavedData::load, KingdomSavedData::new, DATA_NAME);
    }

    public Kingdom create(String name, int x, int z, UUID owner) {
        Kingdom kingdom = new Kingdom(name, x, z, 512, owner.toString());
        kingdoms.add(kingdom);
        setDirty();
        return kingdom;
    }

    public Kingdom createNeutral(String name, int x, int z, int radius) {
        Kingdom kingdom = new Kingdom(name, x, z, radius, "npc:" + name, "");
        kingdoms.add(kingdom);
        setDirty();
        return kingdom;
    }

    /** Creates a player lordship purchased inside an NPC kingdom's territory. */
    public Kingdom createLordship(String name, int x, int z, UUID owner, String liegeName) {
        if (name == null || name.isBlank() || findByName(name) != null || findByOwner(owner) != null) return null;
        Kingdom kingdom = new Kingdom(name.trim(), x, z, 160, owner.toString(), liegeName);
        kingdoms.add(kingdom);
        setDirty();
        return kingdom;
    }

    public boolean conquer(String name, UUID newOwner) {
        Kingdom target = findByName(name);
        if (target == null) return false;
        target.setOwner(newOwner.toString());
        setDirty();
        return true;
    }

    public Kingdom findByName(String name) {
        return kingdoms.stream().filter(k -> k.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public Kingdom findByOwner(UUID owner) {
        return kingdoms.stream().filter(k -> k.owner().equals(owner.toString())).findFirst().orElse(null);
    }

    public Kingdom findAt(int x, int z) {
        return kingdoms.stream().filter(k -> {
            long dx = (long) x - k.x();
            long dz = (long) z - k.z();
            return dx * dx + dz * dz <= (long) k.radius() * k.radius();
        }).findFirst().orElse(null);
    }

    public List<Kingdom> all() { return List.copyOf(kingdoms); }

    /** Expands a player's kingdom after a paid land purchase. */
    public boolean expand(Kingdom kingdom, int amount) {
        if (kingdom == null || amount <= 0 || kingdom.radius() >= 2048) return false;
        kingdom.setRadius(Math.min(2048, kingdom.radius() + amount));
        setDirty();
        return true;
    }

    public boolean declareWar(Kingdom first, Kingdom second) {
        if (first == null || second == null || first == second) return false;
        first.wars().add(second.name());
        second.wars().add(first.name());
        setDirty();
        return true;
    }

    public boolean makePeace(Kingdom first, Kingdom second) {
        if (first == null || second == null) return false;
        first.wars().remove(second.name());
        second.wars().remove(first.name());
        setDirty();
        return true;
    }

    public static KingdomSavedData load(CompoundTag tag) {
        KingdomSavedData data = new KingdomSavedData();
        ListTag list = tag.getList("Kingdoms", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag k = list.getCompound(i);
            Kingdom kingdom = new Kingdom(k.getString("Name"), k.getInt("X"), k.getInt("Z"),
                    k.getInt("Radius"), k.getString("Owner"), k.getString("Liege"));
            ListTag wars = k.getList("Wars", 8);
            for (int j = 0; j < wars.size(); j++) kingdom.wars().add(wars.getString(j));
            data.kingdoms.add(kingdom);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Kingdom kingdom : kingdoms) {
            CompoundTag k = new CompoundTag();
            k.putString("Name", kingdom.name());
            k.putInt("X", kingdom.x());
            k.putInt("Z", kingdom.z());
            k.putInt("Radius", kingdom.radius());
            k.putString("Owner", kingdom.owner());
            k.putString("Liege", kingdom.liege());
            ListTag wars = new ListTag();
            for (String war : kingdom.wars()) wars.add(net.minecraft.nbt.StringTag.valueOf(war));
            k.put("Wars", wars);
            list.add(k);
        }
        tag.put("Kingdoms", list);
        return tag;
    }

    public static class Kingdom {
        private final String name;
        private final int x, z;
        private int radius;
        private String owner;
        private final String liege;
        private final List<String> wars = new ArrayList<>();

        public Kingdom(String name, int x, int z, int radius, String owner) {
            this(name, x, z, radius, owner, "");
        }
        public Kingdom(String name, int x, int z, int radius, String owner, String liege) {
            this.name = name; this.x = x; this.z = z; this.radius = radius; this.owner = owner;
            this.liege = liege == null ? "" : liege;
        }
        public String name() { return name; }
        public int x() { return x; }
        public int z() { return z; }
        public int radius() { return radius; }
        public String owner() { return owner; }
        public String liege() { return liege; }
        public void setOwner(String owner) { this.owner = owner; }
        public void setRadius(int radius) { this.radius = Math.max(1, radius); }
        public List<String> wars() { return wars; }
    }
}
