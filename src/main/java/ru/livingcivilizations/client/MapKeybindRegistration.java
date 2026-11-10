package ru.livingcivilizations.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import ru.livingcivilizations.LivingCivilizations;

@Mod.EventBusSubscriber(modid = LivingCivilizations.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class MapKeybindRegistration {
    public static final KeyMapping OPEN_MAP = new KeyMapping(
            "key.livingcivilizations.open_map",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.livingcivilizations");

    @SubscribeEvent
    public static void registerKeyMapping(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MAP);
    }
}
