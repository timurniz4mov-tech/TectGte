package ru.livingcivilizations.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import ru.livingcivilizations.network.KingdomMarker;

public final class ClientMapHandler {
    private ClientMapHandler() { }

    public static void open(List<KingdomMarker> markers) {
        Minecraft.getInstance().setScreen(new KingdomMapScreen(markers));
    }
}
