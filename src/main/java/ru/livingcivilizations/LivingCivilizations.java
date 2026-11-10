package ru.livingcivilizations;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import ru.livingcivilizations.registry.ModBlocks;
import ru.livingcivilizations.registry.ModEntities;
import ru.livingcivilizations.registry.ModItems;
import ru.livingcivilizations.network.ModNetwork;

@Mod(LivingCivilizations.MOD_ID)
public class LivingCivilizations {
    public static final String MOD_ID = "livingcivilizations";

    public LivingCivilizations() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModEntities.ENTITY_TYPES.register(bus);
        ModItems.ITEMS.register(bus);
        ModNetwork.register();
        bus.addListener(this::addCreativeItems);
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.TRAVELERS_STAFF);
            event.accept(ModItems.KINGDOM_MAP);
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            ModItems.spawnEggs().forEach(event::accept);
        }
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.DIRT_ROAD_ITEM);
            event.accept(ModItems.GRAVEL_ROAD_ITEM);
            event.accept(ModItems.PAVED_ROAD_ITEM);
            event.accept(ModItems.DIRT_ROAD_FULL_ITEM);
            event.accept(ModItems.GRAVEL_ROAD_FULL_ITEM);
            event.accept(ModItems.PAVED_ROAD_FULL_ITEM);
            event.accept(ModItems.DIRT_ROAD_HALF_ITEM);
            event.accept(ModItems.GRAVEL_ROAD_HALF_ITEM);
            event.accept(ModItems.PAVED_ROAD_HALF_ITEM);
        }
    }
}
