package ru.livingcivilizations.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("livingcivilizations", "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static boolean registered;

    private ModNetwork() { }

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, RequestMapPacket.class,
                RequestMapPacket::encode, RequestMapPacket::decode, RequestMapPacket::handle);
        CHANNEL.registerMessage(1, MapDataPacket.class,
                MapDataPacket::encode, MapDataPacket::decode, MapDataPacket::handle);
    }
}
