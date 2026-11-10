package ru.livingcivilizations.economy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.livingcivilizations.LivingCivilizations;
import ru.livingcivilizations.entity.CivilizationNpc;
import ru.livingcivilizations.entity.NpcRole;

@Mod.EventBusSubscriber(modid = LivingCivilizations.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EconomyEvents {
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            PlayerEconomy.rewardForBlockBreak(player, event.getState().getBlock());
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerEconomy.rewardForPlacement(player);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        PlayerEconomy.copyFrom(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof CivilizationNpc npc && npc.getRole() == NpcRole.BANDIT
                && event.getSource().getEntity() instanceof ServerPlayer player) {
            PlayerEconomy.rewardForBanditKill(player);
        }
    }

    private EconomyEvents() { }
}
