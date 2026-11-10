package ru.livingcivilizations.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.livingcivilizations.LivingCivilizations;
import ru.livingcivilizations.kingdom.KingdomSavedData;
import ru.livingcivilizations.registry.ModBlocks;
import ru.livingcivilizations.world.SettlementGenerator;
import ru.livingcivilizations.world.RoadBuilder;

@Mod.EventBusSubscriber(modid = LivingCivilizations.MOD_ID)
public class CivilizationCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("civilizations")
            .then(Commands.literal("kingdom")
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        KingdomSavedData data = KingdomSavedData.get(player.getServer());
                        if (data.findByOwner(player.getUUID()) != null) {
                            ctx.getSource().sendFailure(Component.literal("У тебя уже есть королевство."));
                            return 0;
                        }
                        var pos = player.blockPosition();
                        data.create(StringArgumentType.getString(ctx, "name"), pos.getX(), pos.getZ(), player.getUUID());
                        ctx.getSource().sendSuccess(() -> Component.literal("Королевство создано! Столица: " + pos.getX() + ", " + pos.getZ()), false);
                        return 1;
                    })))
                .then(Commands.literal("war")
                    .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        KingdomSavedData data = KingdomSavedData.get(player.getServer());
                        var own = data.findByOwner(player.getUUID());
                        var target = data.findByName(StringArgumentType.getString(ctx, "name"));
                        if (!data.declareWar(own, target)) {
                            ctx.getSource().sendFailure(Component.literal("Не удалось объявить войну. Нужны своё и чужое королевства."));
                            return 0;
                        }
                        ctx.getSource().sendSuccess(() -> Component.literal("Объявлена война королевству " + target.name()), false);
                        return 1;
                    })))
                .then(Commands.literal("peace")
                    .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        KingdomSavedData data = KingdomSavedData.get(player.getServer());
                        var own = data.findByOwner(player.getUUID());
                        var target = data.findByName(StringArgumentType.getString(ctx, "name"));
                        if (!data.makePeace(own, target)) {
                            ctx.getSource().sendFailure(Component.literal("Не удалось заключить мир."));
                            return 0;
                        }
                        ctx.getSource().sendSuccess(() -> Component.literal("Мир заключён с " + target.name()), false);
                        return 1;
                    }))))
            .then(Commands.literal("conquer")
                .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    KingdomSavedData data = KingdomSavedData.get(player.getServer());
                    var own = data.findByOwner(player.getUUID());
                    var target = data.findByName(StringArgumentType.getString(ctx, "name"));
                    if (own == null || target == null || !own.wars().contains(target.name())) {
                        ctx.getSource().sendFailure(Component.literal("Нужно своё королевство и активная война с этой целью."));
                        return 0;
                    }
                    double dx = player.getX() - target.x();
                    double dz = player.getZ() - target.z();
                    if (dx * dx + dz * dz > 10000.0D) {
                        ctx.getSource().sendFailure(Component.literal("Чтобы захватить столицу, подойди к её центру ближе чем на 100 блоков."));
                        return 0;
                    }
                    data.conquer(target.name(), player.getUUID());
                    own.wars().remove(target.name());
                    target.wars().remove(own.name());
                    ctx.getSource().sendSuccess(() -> Component.literal("Земли королевства " + target.name() + " захвачены!"), true);
                    return 1;
                })))
            .then(Commands.literal("map").executes(ctx -> {
                var data = KingdomSavedData.get(ctx.getSource().getServer());
                if (data.all().isEmpty()) {
                    ctx.getSource().sendSuccess(() -> Component.literal("Королевств пока нет. Создай своё: /civilizations kingdom create Название"), false);
                } else {
                    ctx.getSource().sendSuccess(() -> Component.literal("=== Карта королевств ==="), false);
                    for (var kingdom : data.all()) {
                        ctx.getSource().sendSuccess(() -> Component.literal(kingdom.name() + " | центр "
                                + kingdom.x() + ", " + kingdom.z() + " | радиус " + kingdom.radius()
                                + (kingdom.liege().isEmpty() ? "" : " | вассал королевства: " + kingdom.liege())
                                + " | войны: " + String.join(", ", kingdom.wars())), false);
                    }
                }
                return 1;
            }))
            .then(Commands.literal("settlement").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                boolean generated = SettlementGenerator.generateNear(player.serverLevel(), player.blockPosition(), true);
                if (generated) {
                    ctx.getSource().sendSuccess(() -> Component.literal("Поселение построено неподалёку. Осмотрись вокруг!"), true);
                    return 1;
                }
                ctx.getSource().sendFailure(Component.literal("Не нашлось ровной площадки рядом. Попробуй на равнине."));
                return 0;
            }))
            .then(Commands.literal("castle").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                BlockPos base = player.blockPosition().below();
                var level = player.serverLevel();
                for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
                    boolean wall = Math.abs(x) == 4 || Math.abs(z) == 4;
                    if (wall) {
                        for (int y = 0; y < 5; y++) {
                            if (y == 2 && ((x + z) % 3 == 0)) continue;
                            level.setBlock(base.offset(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                        }
                    }
                    if ((Math.abs(x) == 4 && Math.abs(z) == 4)) {
                        for (int y = 5; y < 8; y++)
                            level.setBlock(base.offset(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    }
                }
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
                    level.setBlock(base.offset(x, 0, z), Blocks.POLISHED_ANDESITE.defaultBlockState(), 3);
                ctx.getSource().sendSuccess(() -> Component.literal("Небольшая каменная крепость построена."), true);
                return 1;
            }))
            .then(Commands.literal("roads")
                .then(Commands.argument("type", StringArgumentType.word())
                    .then(Commands.argument("length", IntegerArgumentType.integer(1, 64)).executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String type = StringArgumentType.getString(ctx, "type");
                        var block = switch (type) {
                            case "dirt" -> ModBlocks.DIRT_ROAD.get();
                            case "gravel" -> ModBlocks.GRAVEL_ROAD.get();
                            case "paved" -> ModBlocks.PAVED_ROAD.get();
                            case "dirt_half" -> ModBlocks.DIRT_ROAD_HALF.get();
                            case "gravel_half" -> ModBlocks.GRAVEL_ROAD_HALF.get();
                            case "paved_half" -> ModBlocks.PAVED_ROAD_HALF.get();
                            case "dirt_full" -> ModBlocks.DIRT_ROAD.get();
                            case "gravel_full" -> ModBlocks.GRAVEL_ROAD.get();
                            case "paved_full" -> ModBlocks.PAVED_ROAD.get();
                            default -> null;
                        };
                        if (block == null) {
                            ctx.getSource().sendFailure(Component.literal("Типы: dirt, gravel, paved (полные блоки); dirt_half, gravel_half, paved_half (тонкие)"));
                            return 0;
                        }
                        int length = IntegerArgumentType.getInteger(ctx, "length");
                        BlockPos start = player.blockPosition().below();
                        var dir = player.getDirection();
                        for (int i = 1; i <= length; i++) {
                            BlockPos p = start.relative(dir, i);
                            RoadBuilder.place(player.serverLevel(), p, block);
                        }
                        ctx.getSource().sendSuccess(() -> Component.literal("Проложена дорога: " + type + ", длина " + length), true);
                        return 1;
                    }))))
        );
    }
}
