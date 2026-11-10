package ru.livingcivilizations.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.Comparator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import ru.livingcivilizations.registry.ModItems;
import ru.livingcivilizations.economy.PlayerEconomy;
import ru.livingcivilizations.world.SettlementSavedData;
import ru.livingcivilizations.kingdom.KingdomSavedData;

public class CivilizationNpc extends PathfinderMob {
    private final NpcRole role;
    private BlockPos travelDestination;
    private int restTicks;

    public CivilizationNpc(EntityType<? extends PathfinderMob> type, Level level, NpcRole role) {
        super(type, level);
        this.role = role;
        this.setCanPickUpLoot(role == NpcRole.BANDIT || role == NpcRole.WARRIOR);
        registerRoleCombatGoals();
        switch (role) {
            case GUARD -> equipTiered(false, true);
            case WARRIOR -> equipTiered(false, false);
            case WORKER -> equipWorkerTools();
            case BANDIT -> equipTiered(true, true);
            case LORD, KING -> {
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
            }
            case TRAVELER -> {
                if (this.random.nextInt(4) == 0)
                    this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.TRAVELERS_STAFF.get()));
            }
            default -> { }
        }
        if (!level.isClientSide) {
            String[] names = {"Альрик", "Эдгар", "Роланд", "Гуго", "Матвей", "Бруно", "Освальд", "Томас", "Герда", "Эльза"};
            this.setCustomName(Component.literal(names[this.random.nextInt(names.length)] + " — " + role.displayName()));
            this.setCustomNameVisible(false);
        }
    }

    private void equipWorkerTools() {
        Item[] picks = {Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE};
        Item[] axes = {Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE};
        int tier = this.random.nextInt(4);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(picks[tier]));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(axes[tier]));
        if (tier >= 2 && this.random.nextBoolean()) this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
    }

    private void equipTiered(boolean bandit, boolean shield) {
        Item[] swords = {Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD};
        Item[] axes = {Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE};
        Item[] helmets = {Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.DIAMOND_HELMET};
        Item[] chestplates = {Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE};
        Item[] leggings = {Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS};
        Item[] boots = {Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.DIAMOND_BOOTS};
        int tier = this.random.nextInt(4);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(bandit ? axes[tier] : swords[tier]));
        if (shield && this.random.nextBoolean()) this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(helmets[tier]));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chestplates[tier]));
        if (this.random.nextBoolean()) this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(leggings[tier]));
        if (this.random.nextBoolean()) this.setItemSlot(EquipmentSlot.FEET, new ItemStack(boots[tier]));
    }

    public NpcRole getRole() {
        return role;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.05D);
    }

    private void registerRoleCombatGoals() {
        if (role == NpcRole.BANDIT) {
            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.18D, true));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, CivilizationNpc.class, 10,
                    true, false, target -> target instanceof CivilizationNpc npc && npc.getRole() != NpcRole.BANDIT));
            this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10,
                    true, false, target -> target instanceof Player player && !player.isCreative() && !player.isSpectator()));
            this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, true));
        } else if (role == NpcRole.GUARD || role == NpcRole.WARRIOR) {
            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.12D, true));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, CivilizationNpc.class, 10,
                    true, false, target -> target instanceof CivilizationNpc npc && npc.getRole() == NpcRole.BANDIT));
            this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, true));
        }
    }

    @Override
    protected void registerGoals() {
        // Role can still be null while the Mob constructor invokes this override.
        // Add only role-independent goals here.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && (role == NpcRole.TRAVELER || role == NpcRole.CARAVAN_MERCHANT)) tickTravelRoute();
        if (this.level().isClientSide || this.tickCount % 15 != 0 || (this.getTarget() != null && this.getTarget().isAlive())) return;

        if (role == NpcRole.BANDIT) {
            CivilizationNpc civilian = this.level().getEntitiesOfClass(CivilizationNpc.class,
                    this.getBoundingBox().inflate(22.0D),
                    other -> other != this && other.isAlive() && other.getRole() != NpcRole.BANDIT)
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            Player player = this.level().getNearestPlayer(this, 20.0D);
            if (player != null && (player.isCreative() || player.isSpectator())) player = null;
            if (civilian != null && (player == null || this.distanceToSqr(civilian) <= this.distanceToSqr(player))) {
                this.setTarget(civilian);
            } else if (player != null) {
                this.setTarget(player);
            } else {
                Monster monster = this.level().getEntitiesOfClass(Monster.class,
                        this.getBoundingBox().inflate(16.0D))
                        .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
                if (monster != null) this.setTarget(monster);
            }
        } else if (role == NpcRole.GUARD || role == NpcRole.WARRIOR) {
            CivilizationNpc bandit = this.level().getEntitiesOfClass(CivilizationNpc.class,
                    this.getBoundingBox().inflate(24.0D),
                    other -> other != this && other.isAlive() && other.getRole() == NpcRole.BANDIT)
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if (bandit != null) {
                this.setTarget(bandit);
            } else if (this.level() instanceof ServerLevel server) {
                String homeName = this.getPersistentData().getString("LC_Kingdom");
                KingdomSavedData.Kingdom home = homeName.isBlank() ? null
                        : KingdomSavedData.get(server.getServer()).findByName(homeName);
                if (home != null && !home.wars().isEmpty()) {
                    CivilizationNpc enemy = this.level().getEntitiesOfClass(CivilizationNpc.class,
                            this.getBoundingBox().inflate(32.0D),
                            other -> other != this && other.isAlive()
                                    && (other.getRole() == NpcRole.GUARD || other.getRole() == NpcRole.WARRIOR)
                                    && home.wars().contains(other.getPersistentData().getString("LC_Kingdom")))
                            .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
                    if (enemy != null) {
                        this.setTarget(enemy);
                    } else {
                        // If the enemy army is not yet visible, move toward its settlement when loaded.
                        KingdomSavedData.Kingdom enemyKingdom = home.wars().stream()
                                .map(KingdomSavedData.get(server.getServer())::findByName)
                                .filter(java.util.Objects::nonNull)
                                .min(Comparator.comparingLong(k -> {
                                    long dx = (long) k.x() - this.blockPosition().getX();
                                    long dz = (long) k.z() - this.blockPosition().getZ();
                                    return dx * dx + dz * dz;
                                })).orElse(null);
                        if (enemyKingdom != null && this.tickCount % 80 == 0) {
                            BlockPos rally = new BlockPos(enemyKingdom.x(), 0, enemyKingdom.z());
                            if (server.hasChunkAt(rally)) {
                                int y = server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, enemyKingdom.x(), enemyKingdom.z());
                                this.getNavigation().moveTo(enemyKingdom.x() + 0.5D, y, enemyKingdom.z() + 0.5D,
                                        role == NpcRole.WARRIOR ? 1.05D : 0.9D);
                            }
                        }
                    }
                }
            }
        }
    }

    /** Travelers and caravan merchants move between registered settlements, taking breaks after arrival. */
    private void tickTravelRoute() {
        if (!(this.level() instanceof ServerLevel server)) return;
        if (restTicks > 0) {
            restTicks--;
            this.getNavigation().stop();
            return;
        }
        if (travelDestination != null) {
            double dx = this.getX() - (travelDestination.getX() + 0.5D);
            double dz = this.getZ() - (travelDestination.getZ() + 0.5D);
            if (dx * dx + dz * dz < 100.0D) {
                travelDestination = null;
                restTicks = 300 + this.random.nextInt(500); // 15–40 seconds at a settlement
                this.getNavigation().stop();
                return;
            }
            if (this.tickCount % 40 == 0) {
                this.getNavigation().moveTo(travelDestination.getX() + 0.5D,
                        travelDestination.getY() + 1.0D, travelDestination.getZ() + 0.5D,
                        role == NpcRole.CARAVAN_MERCHANT ? 1.0D : 0.92D);
            }
            return;
        }
        if (this.tickCount % 100 == 0) {
            BlockPos next = SettlementSavedData.get(server).findNearestBeyond(this.blockPosition(), 36, 420);
            if (next != null) travelDestination = next.immutable();
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (role == NpcRole.CARAVAN_MERCHANT || role == NpcRole.VILLAGE_MERCHANT) {
            if (!this.level().isClientSide) PlayerEconomy.tradeWith(player, role);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (!this.level().isClientSide) {
            player.displayClientMessage(Component.literal(role.displayName() + ": здравствуй, путник!"), true);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }
}
