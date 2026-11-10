package ru.livingcivilizations.economy;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.livingcivilizations.LivingCivilizations;
import ru.livingcivilizations.kingdom.KingdomSavedData;

import java.util.List;

@Mod.EventBusSubscriber(modid = LivingCivilizations.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EconomyCommands {
    private static final List<String> JOBS = List.of("servant", "miner", "farmer", "builder", "guard", "warrior", "trader", "lumberjack");

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("civilizations")
            .then(Commands.literal("money").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                ctx.getSource().sendSuccess(() -> Component.literal("Баланс: " + PlayerEconomy.getMoney(player) + " монет. Профессия: "
                        + (PlayerEconomy.getJob(player).isEmpty() ? "не выбрана" : PlayerEconomy.getJob(player))), false);
                return 1;
            }))
            .then(Commands.literal("job")
                .then(Commands.literal("list").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("Профессии: servant, miner, farmer, builder, guard, warrior, trader, lumberjack. Выбери: /civilizations job choose <профессия>"), false);
                    return 1;
                }))
                .then(Commands.literal("choose")
                    .then(Commands.argument("job", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(JOBS, builder))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String job = StringArgumentType.getString(ctx, "job").toLowerCase(java.util.Locale.ROOT);
                            if (!JOBS.contains(job)) {
                                ctx.getSource().sendFailure(Component.literal("Неизвестная профессия. Список: /civilizations job list"));
                                return 0;
                            }
                            PlayerEconomy.setJob(player, job);
                            ctx.getSource().sendSuccess(() -> Component.literal("Теперь твоя профессия: " + job + ". Работай и получай монеты!"), false);
                            return 1;
                        }))))
            .then(Commands.literal("sell").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                if (!"trader".equals(PlayerEconomy.getJob(player))) {
                    ctx.getSource().sendFailure(Component.literal("Чтобы продавать предметы, выбери профессию: /civilizations job choose trader"));
                    return 0;
                }
                if (!PlayerEconomy.sellHeldItem(player)) {
                    ctx.getSource().sendFailure(Component.literal("Возьми предмет в главную руку, затем используй /civilizations sell"));
                    return 0;
                }
                return 1;
            }))
            .then(Commands.literal("buy")
                .then(Commands.argument("building", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("house", "farm"), builder))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String building = StringArgumentType.getString(ctx, "building").toLowerCase(java.util.Locale.ROOT);
                        int price = switch (building) { case "house" -> 250; case "farm" -> 180; default -> -1; };
                        if (price < 0) {
                            ctx.getSource().sendFailure(Component.literal("Доступно: /civilizations buy house или /civilizations buy farm"));
                            return 0;
                        }
                        if (PlayerEconomy.getMoney(player) < price) {
                            ctx.getSource().sendFailure(Component.literal("Не хватает денег. Стоимость: " + price + ", баланс: " + PlayerEconomy.getMoney(player)));
                            return 0;
                        }
                        ServerLevel level = player.serverLevel();
                        net.minecraft.core.BlockPos placed = building.equals("house")
                                ? PurchasedBuildings.buildHouseNear(level, player.blockPosition())
                                : PurchasedBuildings.buildFarmNear(level, player.blockPosition());
                        if (placed == null) {
                            ctx.getSource().sendFailure(Component.literal("Рядом не нашлось ровного и свободного места. Деньги не списаны."));
                            return 0;
                        }
                        PlayerEconomy.spend(player, price);
                        ctx.getSource().sendSuccess(() -> Component.literal("Постройка куплена за " + price + " монет: " + building
                                + " (" + placed.getX() + ", " + placed.getY() + ", " + placed.getZ() + "). Остаток: " + PlayerEconomy.getMoney(player)), true);
                        return 1;
                    })))
            .then(Commands.literal("land")
                .then(Commands.literal("buy")
                    .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        KingdomSavedData data = KingdomSavedData.get(player.getServer());
                        if (data.findByOwner(player.getUUID()) != null) {
                            ctx.getSource().sendFailure(Component.literal("У тебя уже есть собственное владение."));
                            return 0;
                        }
                        KingdomSavedData.Kingdom liege = data.findAt(player.blockPosition().getX(), player.blockPosition().getZ());
                        if (liege == null || !liege.owner().startsWith("npc:")) {
                            ctx.getSource().sendFailure(Component.literal("Чтобы купить титул лорда, подойди к земле NPC-королевства и находись внутри его границ."));
                            return 0;
                        }
                        String title = StringArgumentType.getString(ctx, "name");
                        if (data.findByName(title) != null) {
                            ctx.getSource().sendFailure(Component.literal("Такое название владения уже занято."));
                            return 0;
                        }
                        int price = 1000;
                        if (PlayerEconomy.getMoney(player) < price) {
                            ctx.getSource().sendFailure(Component.literal("Покупка владения стоит " + price + " монет. Твой баланс: " + PlayerEconomy.getMoney(player)));
                            return 0;
                        }
                        KingdomSavedData.Kingdom lordship = data.createLordship(title, player.blockPosition().getX(),
                                player.blockPosition().getZ(), player.getUUID(), liege.name());
                        if (lordship == null) {
                            ctx.getSource().sendFailure(Component.literal("Не удалось оформить владение."));
                            return 0;
                        }
                        PlayerEconomy.spend(player, price);
                        ctx.getSource().sendSuccess(() -> Component.literal("Ты заплатил королю " + liege.name()
                                + " 1000 монет и стал лордом владения «" + lordship.name() + "». Теперь можно расширять земли: /civilizations kingdom expand"), true);
                        return 1;
                    }))))
            .then(Commands.literal("kingdom")
                .then(Commands.literal("expand").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    KingdomSavedData data = KingdomSavedData.get(player.getServer());
                    KingdomSavedData.Kingdom own = data.findByOwner(player.getUUID());
                    if (own == null) {
                        ctx.getSource().sendFailure(Component.literal("Сначала создай королевство: /civilizations kingdom create <название>"));
                        return 0;
                    }
                    if (own.radius() >= 2048) {
                        ctx.getSource().sendFailure(Component.literal("Территория уже достигла максимального размера."));
                        return 0;
                    }
                    int price = 500;
                    if (!PlayerEconomy.spend(player, price)) {
                        ctx.getSource().sendFailure(Component.literal("Покупка земли стоит " + price + " монет. Твой баланс: " + PlayerEconomy.getMoney(player)));
                        return 0;
                    }
                    data.expand(own, 64);
                    ctx.getSource().sendSuccess(() -> Component.literal("Ты приобрёл новые земли за " + price + " монет. Радиус владений: " + own.radius() + " блоков. Ты признан лордом этих земель."), true);
                    return 1;
                })))
        );
    }

    private EconomyCommands() { }
}
