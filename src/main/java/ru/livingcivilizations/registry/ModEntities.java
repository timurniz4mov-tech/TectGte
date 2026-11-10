package ru.livingcivilizations.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.livingcivilizations.LivingCivilizations;
import ru.livingcivilizations.entity.CivilizationNpc;
import ru.livingcivilizations.entity.NpcRole;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LivingCivilizations.MOD_ID);

    public static final RegistryObject<EntityType<CivilizationNpc>> TRAVELER = register("traveler", NpcRole.TRAVELER);
    public static final RegistryObject<EntityType<CivilizationNpc>> CARAVAN_MERCHANT = register("caravan_merchant", NpcRole.CARAVAN_MERCHANT);
    public static final RegistryObject<EntityType<CivilizationNpc>> VILLAGE_MERCHANT = register("village_merchant", NpcRole.VILLAGE_MERCHANT);
    public static final RegistryObject<EntityType<CivilizationNpc>> WORKER = register("worker", NpcRole.WORKER);
    public static final RegistryObject<EntityType<CivilizationNpc>> GUARD = register("guard", NpcRole.GUARD);
    public static final RegistryObject<EntityType<CivilizationNpc>> WARRIOR = register("warrior", NpcRole.WARRIOR);
    public static final RegistryObject<EntityType<CivilizationNpc>> LORD = register("lord", NpcRole.LORD);
    public static final RegistryObject<EntityType<CivilizationNpc>> KING = register("king", NpcRole.KING);
    public static final RegistryObject<EntityType<CivilizationNpc>> BANDIT = register("bandit", NpcRole.BANDIT);

    private static RegistryObject<EntityType<CivilizationNpc>> register(String id, NpcRole role) {
        return ENTITY_TYPES.register(id, () -> EntityType.Builder
                .of((EntityType<CivilizationNpc> type, net.minecraft.world.level.Level level) ->
                        new CivilizationNpc(type, level, role), MobCategory.CREATURE)
                .sized(0.6F, 1.95F)
                .clientTrackingRange(10)
                .build(id));
    }
}
