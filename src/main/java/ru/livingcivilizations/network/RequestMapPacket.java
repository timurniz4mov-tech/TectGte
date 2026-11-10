package ru.livingcivilizations.network;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import ru.livingcivilizations.kingdom.KingdomSavedData;

public class RequestMapPacket {
    public static void encode(RequestMapPacket message, FriendlyByteBuf buffer) { }
    public static RequestMapPacket decode(FriendlyByteBuf buffer) { return new RequestMapPacket(); }

    public static void handle(RequestMapPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            List<KingdomMarker> markers = KingdomSavedData.get(player.getServer()).all().stream()
                    .map(k -> new KingdomMarker(k.name(), k.x(), k.z(), k.radius(), !k.wars().isEmpty()))
                    .toList();
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MapDataPacket(markers));
        });
        context.setPacketHandled(true);
    }
}
