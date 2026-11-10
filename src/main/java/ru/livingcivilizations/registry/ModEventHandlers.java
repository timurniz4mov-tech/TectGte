package ru.livingcivilizations.registry;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.livingcivilizations.LivingCivilizations;
import ru.livingcivilizations.entity.CivilizationNpc;

@Mod.EventBusSubscriber(modid = LivingCivilizations.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventHandlers {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        var attributes = CivilizationNpc.createAttributes().build();
        event.put(ModEntities.TRAVELER.get(), attributes);
        event.put(ModEntities.CARAVAN_MERCHANT.get(), attributes);
        event.put(ModEntities.VILLAGE_MERCHANT.get(), attributes);
        event.put(ModEntities.WORKER.get(), attributes);
        event.put(ModEntities.GUARD.get(), attributes);
        event.put(ModEntities.WARRIOR.get(), attributes);
        event.put(ModEntities.LORD.get(), attributes);
        event.put(ModEntities.KING.get(), attributes);
        event.put(ModEntities.BANDIT.get(), attributes);
    }
}
