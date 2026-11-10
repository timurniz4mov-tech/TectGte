package ru.livingcivilizations.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ru.livingcivilizations.kingdom.KingdomSavedData;
import ru.livingcivilizations.network.KingdomMarker;
import ru.livingcivilizations.network.MapDataPacket;
import ru.livingcivilizations.network.ModNetwork;
import net.minecraftforge.network.PacketDistributor;

public class KingdomMapItem extends Item {
    public KingdomMapItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && level.getServer() != null) {
            KingdomSavedData data = KingdomSavedData.get(level.getServer());
            var here = data.findAt(player.blockPosition().getX(), player.blockPosition().getZ());
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                var markers = data.all().stream().map(k -> new KingdomMarker(k.name(), k.x(), k.z(),
                        k.radius(), !k.wars().isEmpty())).toList();
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new MapDataPacket(markers));
            }
            if (here == null) {
                player.displayClientMessage(Component.literal("На этой земле пока нет зарегистрированного королевства."), false);
            } else {
                player.displayClientMessage(Component.literal("Королевство " + here.name()
                        + " | столица: " + here.x() + ", " + here.z()
                        + " | радиус земель: " + here.radius()
                        + " блоков | войны: " + String.join(", ", here.wars())), false);
            }
            player.displayClientMessage(Component.literal("Карта королевств открыта. Нажми M, чтобы открыть её снова."), false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
