package ru.livingcivilizations.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import ru.livingcivilizations.client.ClientMapHandler;

public class MapDataPacket {
    private final List<KingdomMarker> markers;

    public MapDataPacket(List<KingdomMarker> markers) {
        this.markers = List.copyOf(markers);
    }

    public static void encode(MapDataPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.markers.size());
        for (KingdomMarker marker : message.markers) {
            buffer.writeUtf(marker.name(), 64);
            buffer.writeInt(marker.x());
            buffer.writeInt(marker.z());
            buffer.writeInt(marker.radius());
            buffer.writeBoolean(marker.atWar());
        }
    }

    public static MapDataPacket decode(FriendlyByteBuf buffer) {
        int count = Math.min(buffer.readVarInt(), 256);
        List<KingdomMarker> markers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            markers.add(new KingdomMarker(buffer.readUtf(64), buffer.readInt(), buffer.readInt(),
                    buffer.readInt(), buffer.readBoolean()));
        }
        return new MapDataPacket(markers);
    }

    public static void handle(MapDataPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientMapHandler.open(message.markers)));
        context.setPacketHandled(true);
    }
}
