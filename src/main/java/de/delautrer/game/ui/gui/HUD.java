package de.delautrer.game.ui.gui;

import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.game.entity.player.GameMode;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.ui.ChatOverlay;
import de.delautrer.game.ui.DebugOverlay;
import de.delautrer.game.player.Inventory;

import java.util.List;

public class HUD {

    public void render(UIMeshBuilder builder, int width, int height, PlayerInteraction interaction, int hoveredSlot, DebugOverlay debugOverlay, ChatOverlay chatOverlay, VulkanFont font) {
        float pixelScale = 2.0f;
        if (height >= 1080) pixelScale = 3.0f;
        if (height >= 1440) pixelScale = 4.0f;

        Inventory inventory = interaction.getInventory();
        boolean isScreenOpen = inventory.isOpen();

        boolean isChatOpen = interaction.getPlayer().isChatOpen();

        // --- 1. DEBUG OVERLAY ---
        if (debugOverlay != null && debugOverlay.isVisible() && !isChatOpen) {
            float textY = height - 25.0f;
            float textX = 10.0f;
            for (String line : debugOverlay.getLinesToRender()) {
                builder.drawText(line, textX, textY, 0.5f, font);
                textY -= 24.0f;
            }
        }

        // --- CHAT ---
        if (chatOverlay != null) {
            float textY = 60.0f;
            List<String> messages = chatOverlay.getMessages();

            for (int i = messages.size() - 1; i >= 0; i--) {
                builder.drawText(messages.get(i), 10.0f, textY, 0.4f, font);
                textY += 20.0f;
            }
        }

        // --- 2. CROSSHAIR ---
        if (!isScreenOpen) {
            float crosshairSize = 23.0f * pixelScale;
            float cx = (float) Math.floor((width - crosshairSize) / 2.0f);
            float cy = (float) Math.floor((height - crosshairSize) / 2.0f);
            builder.addAtlasQuad(cx, cy, 0.0f, crosshairSize, crosshairSize, 0, 0, 1, 1, false);
        }

        // --- 3. HOTBAR ---
        if (interaction.getPlayer().getGameMode() == GameMode.SPECTATOR) return;

        float hotbarWidth = 207.0f * pixelScale;
        float hotbarHeight = 23.0f * pixelScale;
        float hx = (float) Math.floor((width - hotbarWidth) / 2.0f);
        float hotbarY = (float) Math.floor(10.0f * pixelScale);

        builder.addAtlasQuad(hx, hotbarY, 0.2f, hotbarWidth, hotbarHeight, 0, 1, 9, 1, false);

        for (int col = 0; col < 9; col++) {
            float slotX = hx + (4.0f + col * 22.0f) * pixelScale;
            float selectorW = 23.0f * pixelScale;

            if (!isScreenOpen && inventory.getSelectedSlot() == col) {
                builder.addAtlasQuad(slotX, hotbarY, 0.1f, selectorW, hotbarHeight, 0, 2, 1, 1, false);
            } else if (isScreenOpen && hoveredSlot == col) {
                builder.addAtlasQuad(slotX, hotbarY, 0.1f, selectorW, hotbarHeight, 0, 2, 1, 1, false);
            }

            builder.drawItem(inventory.getStack(col), slotX, hotbarY, 0.0f, selectorW);
        }
    }
}