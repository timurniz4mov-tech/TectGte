package ru.livingcivilizations.registry;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.livingcivilizations.LivingCivilizations;
import ru.livingcivilizations.item.KingdomMapItem;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, LivingCivilizations.MOD_ID);

    public static final RegistryObject<Item> TRAVELERS_STAFF = ITEMS.register("travelers_staff",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> KINGDOM_MAP = ITEMS.register("kingdom_map",
            () -> new KingdomMapItem(new Item.Properties()));

    // Road blocks are registered separately from their block items to avoid
    // DeferredRegister class-initialization cycles.
    public static final RegistryObject<Item> DIRT_ROAD_ITEM = ITEMS.register("dirt_road",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.DIRT_ROAD.get(), new Item.Properties()));
    public static final RegistryObject<Item> GRAVEL_ROAD_ITEM = ITEMS.register("gravel_road",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.GRAVEL_ROAD.get(), new Item.Properties()));
    public static final RegistryObject<Item> PAVED_ROAD_ITEM = ITEMS.register("paved_road",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.PAVED_ROAD.get(), new Item.Properties()));
    public static final RegistryObject<Item> DIRT_ROAD_FULL_ITEM = ITEMS.register("dirt_road_full",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.DIRT_ROAD_FULL.get(), new Item.Properties()));
    public static final RegistryObject<Item> GRAVEL_ROAD_FULL_ITEM = ITEMS.register("gravel_road_full",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.GRAVEL_ROAD_FULL.get(), new Item.Properties()));
    public static final RegistryObject<Item> PAVED_ROAD_FULL_ITEM = ITEMS.register("paved_road_full",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.PAVED_ROAD_FULL.get(), new Item.Properties()));
    public static final RegistryObject<Item> DIRT_ROAD_HALF_ITEM = ITEMS.register("dirt_road_half",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.DIRT_ROAD_HALF.get(), new Item.Properties()));
    public static final RegistryObject<Item> GRAVEL_ROAD_HALF_ITEM = ITEMS.register("gravel_road_half",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.GRAVEL_ROAD_HALF.get(), new Item.Properties()));
    public static final RegistryObject<Item> PAVED_ROAD_HALF_ITEM = ITEMS.register("paved_road_half",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.PAVED_ROAD_HALF.get(), new Item.Properties()));

    public static final RegistryObject<Item> TRAVELER_EGG = egg("traveler_spawn_egg", ModEntities.TRAVELER, 0x725039, 0xD8B77B);
    public static final RegistryObject<Item> CARAVAN_MERCHANT_EGG = egg("caravan_merchant_spawn_egg", ModEntities.CARAVAN_MERCHANT, 0x6A3F27, 0xD5A94E);
    public static final RegistryObject<Item> VILLAGE_MERCHANT_EGG = egg("village_merchant_spawn_egg", ModEntities.VILLAGE_MERCHANT, 0x8A5B32, 0xE7C77A);
    public static final RegistryObject<Item> WORKER_EGG = egg("worker_spawn_egg", ModEntities.WORKER, 0x4C5961, 0xB78A53);
    public static final RegistryObject<Item> GUARD_EGG = egg("guard_spawn_egg", ModEntities.GUARD, 0x354C69, 0xC7C9CC);
    public static final RegistryObject<Item> WARRIOR_EGG = egg("warrior_spawn_egg", ModEntities.WARRIOR, 0x632D29, 0xAFAFAF);
    public static final RegistryObject<Item> LORD_EGG = egg("lord_spawn_egg", ModEntities.LORD, 0x49345E, 0xD6B75B);
    public static final RegistryObject<Item> KING_EGG = egg("king_spawn_egg", ModEntities.KING, 0x7B5C16, 0xE6C94B);
    public static final RegistryObject<Item> BANDIT_EGG = egg("bandit_spawn_egg", ModEntities.BANDIT, 0x292929, 0x8A3830);

    private static RegistryObject<Item> egg(String id,
            RegistryObject<net.minecraft.world.entity.EntityType<ru.livingcivilizations.entity.CivilizationNpc>> type,
            int primary, int secondary) {
        return ITEMS.register(id, () -> new ForgeSpawnEggItem(type, primary, secondary, new Item.Properties()));
    }

    public static List<RegistryObject<Item>> spawnEggs() {
        return List.of(TRAVELER_EGG, CARAVAN_MERCHANT_EGG, VILLAGE_MERCHANT_EGG, WORKER_EGG,
                GUARD_EGG, WARRIOR_EGG, LORD_EGG, KING_EGG, BANDIT_EGG);
    }
}
