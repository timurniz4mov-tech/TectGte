package ru.livingcivilizations.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import ru.livingcivilizations.entity.NpcRole;

/** Persistent player jobs and a simple coin economy. */
public final class PlayerEconomy {
    private static final String MONEY = "lc_money";
    private static final String JOB = "lc_job";
    private static final String LAST_REWARD = "lc_last_reward";

    public static int getMoney(Player player) {
        return Math.max(0, player.getPersistentData().getInt(MONEY));
    }

    public static String getJob(Player player) {
        return player.getPersistentData().getString(JOB);
    }

    public static void setJob(Player player, String job) {
        player.getPersistentData().putString(JOB, job);
    }

    public static void addMoney(Player player, int amount) {
        if (amount <= 0) return;
        long total = (long) getMoney(player) + amount;
        player.getPersistentData().putInt(MONEY, (int) Math.min(Integer.MAX_VALUE, total));
    }

    public static boolean spend(Player player, int amount) {
        if (amount < 0 || getMoney(player) < amount) return false;
        player.getPersistentData().putInt(MONEY, getMoney(player) - amount);
        return true;
    }

    public static void rewardForBlockBreak(ServerPlayer player, Block block) {
        String job = getJob(player);
        boolean matched = switch (job) {
            case "miner" -> block.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE);
            case "lumberjack" -> block.defaultBlockState().is(BlockTags.LOGS);
            case "farmer" -> block instanceof CropBlock;
            default -> false;
        };
        if (matched) rewardWithCooldown(player, 2 + player.getRandom().nextInt(3));
    }

    public static void rewardForPlacement(ServerPlayer player) {
        if ("builder".equals(getJob(player))) rewardWithCooldown(player, 2);
    }

    public static void rewardForBanditKill(ServerPlayer player) {
        if ("guard".equals(getJob(player)) || "warrior".equals(getJob(player))) rewardWithCooldown(player, 12);
    }

    private static void rewardWithCooldown(ServerPlayer player, int amount) {
        long now = player.level().getGameTime();
        long last = player.getPersistentData().getLong(LAST_REWARD);
        // Limits automated farms and rapid block placement from generating endless coins.
        if (now - last < 20) return;
        player.getPersistentData().putLong(LAST_REWARD, now);
        addMoney(player, amount);
        player.displayClientMessage(Component.literal("§6+" + amount + " монет §7(баланс: " + getMoney(player) + ")"), true);
    }

    public static boolean sellHeldItem(Player player) {
        if (!"trader".equals(getJob(player))) return false;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return false;
        int unitPrice;
        if (held.is(Items.DIAMOND)) unitPrice = 30;
        else if (held.is(Items.EMERALD)) unitPrice = 18;
        else if (held.is(Items.IRON_INGOT)) unitPrice = 8;
        else if (held.is(Items.GOLD_INGOT)) unitPrice = 10;
        else if (held.is(Items.COPPER_INGOT)) unitPrice = 4;
        else if (Block.byItem(held.getItem()).defaultBlockState().is(BlockTags.LOGS)) unitPrice = 2;
        else if (held.is(Items.COBBLESTONE) || held.is(Items.DIRT) || held.is(Items.SAND)) unitPrice = 1;
        else {
            player.displayClientMessage(Component.literal("§cЭтот предмет торговцы не покупают."), true);
            return true;
        }
        int earnings = unitPrice * held.getCount();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        addMoney(player, earnings);
        player.sendSystemMessage(Component.literal("§aПродано за " + earnings + " монет. Баланс: " + getMoney(player)));
        return true;
    }

    public static void tradeWith(Player player, NpcRole role) {
        boolean caravan = role == NpcRole.CARAVAN_MERCHANT;
        int price = caravan ? 18 : 10;
        ItemStack goods = caravan ? new ItemStack(Items.IRON_INGOT, 2) : new ItemStack(Items.BREAD, 8);
        if (!player.getAbilities().instabuild && !spend(player, price)) {
            player.displayClientMessage(Component.literal("§cНе хватает монет. Нужно: " + price + ", баланс: " + getMoney(player)), true);
            return;
        }
        if (!player.getInventory().add(goods)) {
            if (!player.getAbilities().instabuild) addMoney(player, price);
            player.displayClientMessage(Component.literal("§cОсвободи место в инвентаре. Деньги возвращены."), true);
            return;
        }
        player.displayClientMessage(Component.literal("§aПокупка совершена: " + (caravan ? "2 железных слитка" : "8 хлебов")
                + ". Остаток: " + getMoney(player) + " монет."), true);
    }

    public static void copyFrom(Player original, Player replacement) {
        replacement.getPersistentData().putInt(MONEY, original.getPersistentData().getInt(MONEY));
        replacement.getPersistentData().putString(JOB, original.getPersistentData().getString(JOB));
        replacement.getPersistentData().putLong(LAST_REWARD, original.getPersistentData().getLong(LAST_REWARD));
    }

    private PlayerEconomy() { }
}
