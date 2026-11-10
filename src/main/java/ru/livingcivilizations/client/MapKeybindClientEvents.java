package ru.livingcivilizations.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.livingcivilizations.LivingCivilizations;
import ru.livingcivilizations.network.ModNetwork;
import ru.livingcivilizations.network.RequestMapPacket;

@Mod.EventBusSubscriber(modid = LivingCivilizations.MOD_ID, value = Dist.CLIENT)
public class MapKeybindClientEvents {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().player == null) return;
        if (MapKeybindRegistration.OPEN_MAP.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new RequestMapPacket());
        }
    }
}
