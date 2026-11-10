package ru.livingcivilizations.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import ru.livingcivilizations.network.KingdomMarker;

public class KingdomMapScreen extends Screen {
    private static final int GRID = 17;
    private static final int BLOCKS_PER_CELL = 16;
    private final List<KingdomMarker> kingdoms;

    public KingdomMapScreen(List<KingdomMarker> kingdoms) {
        super(Component.literal("Карта королевств"));
        this.kingdoms = List.copyOf(kingdoms);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        graphics.drawCenteredString(this.font, "Карта королевств", this.width / 2, 18, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "Местность вокруг игрока • M или ESC — закрыть",
                this.width / 2, 34, 0xD5D5D5);

        int cell = Math.max(12, Math.min(22, (Math.min(this.width - 100, this.height - 150)) / GRID));
        int mapW = GRID * cell;
        int left = (this.width - mapW) / 2 - 65;
        int top = Math.max(58, (this.height - mapW) / 2);
        BlockPos player = minecraft.player.blockPosition();

        graphics.fill(left - 5, top - 5, left + mapW + 5, top + mapW + 5, 0xFF35291D);
        for (int gz = 0; gz < GRID; gz++) {
            for (int gx = 0; gx < GRID; gx++) {
                int wx = player.getX() + (gx - GRID / 2) * BLOCKS_PER_CELL;
                int wz = player.getZ() + (gz - GRID / 2) * BLOCKS_PER_CELL;
                int color = terrainColor(level, wx, wz);
                KingdomMarker owner = ownerAt(wx, wz);
                if (owner != null) color = blend(color,
                        owner.atWar() ? 0xFFD34C42 : 0xFF4C83C5, 0.42F);

                int x = left + gx * cell;
                int y = top + gz * cell;
                graphics.fill(x, y, x + cell - 1, y + cell - 1, color);
                if (owner != null && isTerritoryEdge(wx, wz, owner)) {
                    graphics.fill(x, y, x + cell - 1, y + 2, 0xFFEEE1B0);
                    graphics.fill(x, y, x + 2, y + cell - 1, 0xFFEEE1B0);
                }
            }
        }

        // Draw settlement/kingdom centers.
        for (KingdomMarker kingdom : kingdoms) {
            int dx = kingdom.x() - player.getX();
            int dz = kingdom.z() - player.getZ();
            int gx = GRID / 2 + Math.round((float) dx / BLOCKS_PER_CELL);
            int gz = GRID / 2 + Math.round((float) dz / BLOCKS_PER_CELL);
            if (gx < 0 || gz < 0 || gx >= GRID || gz >= GRID) continue;
            int cx = left + gx * cell + cell / 2;
            int cy = top + gz * cell + cell / 2;
            int markerColor = kingdom.atWar() ? 0xFFFF5148 : 0xFFFFD45C;
            graphics.fill(cx - 4, cy - 4, cx + 5, cy + 5, 0xFF241C13);
            graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, markerColor);
        }

        // Player marker in the middle.
        int pcx = left + GRID / 2 * cell + cell / 2;
        int pcy = top + GRID / 2 * cell + cell / 2;
        graphics.fill(pcx - 5, pcy - 1, pcx + 6, pcy + 2, 0xFFFFFFFF);
        graphics.fill(pcx - 1, pcy - 5, pcx + 2, pcy + 6, 0xFFFFFFFF);
        graphics.fill(pcx - 2, pcy - 2, pcx + 3, pcy + 3, 0xFFDA3333);

        int listX = left + mapW + 15;
        int listY = top + 2;
        graphics.drawString(this.font, "Поселения / королевства:", listX, listY, 0xFFFFFF, false);
        for (int i = 0; i < Math.min(kingdoms.size(), 10); i++) {
            KingdomMarker k = kingdoms.get(i);
            String line = (k.atWar() ? "⚔ " : "♜ ") + k.name();
            graphics.drawString(this.font, line, listX, listY + 14 + i * 12,
                    k.atWar() ? 0xFFFF7770 : 0xFFFFD98A, false);
        }

        int infoY = top + mapW + 12;
        graphics.drawCenteredString(this.font,
                "Твоя позиция: X " + player.getX() + "  Z " + player.getZ(),
                this.width / 2, infoY, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                "Синий — территория королевства • Красный — территория в состоянии войны",
                this.width / 2, infoY + 14, 0xD5D5D5);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private KingdomMarker ownerAt(int x, int z) {
        for (KingdomMarker kingdom : kingdoms) {
            long dx = (long) x - kingdom.x();
            long dz = (long) z - kingdom.z();
            if (dx * dx + dz * dz <= (long) kingdom.radius() * kingdom.radius()) return kingdom;
        }
        return null;
    }

    private boolean isTerritoryEdge(int x, int z, KingdomMarker kingdom) {
        long r2 = (long) kingdom.radius() * kingdom.radius();
        long dx = (long) x - kingdom.x();
        long dz = (long) z - kingdom.z();
        if (dx * dx + dz * dz > r2) return false;
        int step = BLOCKS_PER_CELL;
        long east = (long) (x + step - kingdom.x()) * (x + step - kingdom.x()) + dz * dz;
        long west = (long) (x - step - kingdom.x()) * (x - step - kingdom.x()) + dz * dz;
        long south = dx * dx + (long) (z + step - kingdom.z()) * (z + step - kingdom.z());
        long north = dx * dx + (long) (z - step - kingdom.z()) * (z - step - kingdom.z());
        return east > r2 || west > r2 || south > r2 || north > r2;
    }

    private int terrainColor(ClientLevel level, int x, int z) {
        BlockPos top = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(x, 0, z));
        BlockPos ground = top.below();
        BlockState state = level.getBlockState(ground);
        if (!level.getFluidState(ground).isEmpty()) return 0xFF315F91;
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MOSS_BLOCK)) return 0xFF557D45;
        if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) return 0xFFD8C18A;
        if (state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.SNOW)) return 0xFFE6EDF1;
        if (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)) return 0xFF777777;
        if (state.is(Blocks.OAK_LEAVES) || state.is(Blocks.SPRUCE_LEAVES)
                || state.is(Blocks.BIRCH_LEAVES)) return 0xFF315C35;
        if (state.is(Blocks.COARSE_DIRT) || state.is(Blocks.DIRT) || state.is(Blocks.PODZOL))
            return 0xFF896A4A;
        return 0xFF6F795D;
    }

    private int blend(int base, int overlay, float amount) {
        int r = (int) (((base >> 16) & 255) * (1 - amount) + ((overlay >> 16) & 255) * amount);
        int g = (int) (((base >> 8) & 255) * (1 - amount) + ((overlay >> 8) & 255) * amount);
        int b = (int) ((base & 255) * (1 - amount) + (overlay & 255) * amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
