package de.delautrer.game.ui.gui;

import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.entity.player.GameMode;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.ui.ChatOverlay;
import de.delautrer.game.ui.DebugOverlay;
import de.delautrer.game.player.Inventory;

import java.util.List;
import java.util.Map;

public class HUD {

    public void render(UIMeshBuilder builder, int width, int height, PlayerInteraction interaction, int hoveredSlot, DebugOverlay debugOverlay, ChatOverlay chatOverlay, VulkanFont font, int blockAtlasWidth) {
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
            List<ChatOverlay.ChatMessage> messages = chatOverlay.getMessages();

            int maxVisible = 10;
            int startIndex = messages.size() - 1 - chatOverlay.getScrollOffset();
            int endIndex = Math.max(0, startIndex - maxVisible + 1);

            for (int i = startIndex; i >= endIndex; i--) {
                ChatOverlay.ChatMessage msg = messages.get(i);

                if (isChatOpen || msg.timeRemaining > 0) {
                    builder.drawText(msg.text, 10.0f, textY, 0.4f, font);
                    textY += 20.0f;
                }
            }
        }

        // --- 1.5 SUFFOCATION (KOPF IM BLOCK) ---
        Block headBlock = interaction.getPlayer().getHeadBlock();
        if (interaction.getPlayer().getGameMode() != GameMode.SPECTATOR && headBlock != BlockRegistry.AIR) {

            // Wir nehmen direkt das Modell des Blocks (Vorderseite)
            de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion reg = headBlock.getModel().north;

            if (reg != null) {
                // --- Die Mathematik: Layer-Index in 2D UV-Koordinaten umrechnen ---
                int gridSize = blockAtlasWidth / 16;
                int layer = (int) reg.layer;

                int gridX = layer % gridSize;
                int gridY = layer / gridSize;

                float epsilon = 0.0005f;
                float uStep = 1.0f / gridSize;

                float u0 = gridX * uStep + epsilon;
                float v0 = gridY * uStep + epsilon;
                float u1 = u0 + uStep - (2 * epsilon);
                float v1 = v0 + uStep - (2 * epsilon);

                float maxDim = Math.max(width, height) * 1.5f;
                float offX = (width - maxDim) / 2.0f;
                float offY = (height - maxDim) / 2.0f;

                builder.addOverlayQuad(offX, offY, maxDim, maxDim, u0, v0, u1, v1);
            }
        }

        // --- 2. CROSSHAIR ---
        if (!isScreenOpen) {
            float crosshairSize = 16.0f * pixelScale;
            float cx = (float) Math.floor((width - crosshairSize) / 2.0f);
            float cy = (float) Math.floor((height - crosshairSize) / 2.0f);
            builder.addAtlasQuad(cx, cy, 0.0f, crosshairSize, crosshairSize, 0, 1, 1, 1, false);
        }

        // --- 3. HOTBAR ---
        if (interaction.getPlayer().getGameMode() == GameMode.SPECTATOR) return;
        if (isScreenOpen) return;
        if (isChatOpen) return;

        float hotbarWidth = 24f * 9f * pixelScale;
        float hotbarHeight = 24f * pixelScale;
        float hx = (float) Math.floor((width - hotbarWidth) / 2.0f);
        float hotbarY = (float) Math.floor(10.0f * pixelScale);

        builder.addAtlasQuad(hx, hotbarY, 0.2f, hotbarWidth, hotbarHeight, 1, 1, 9, 1, false);

        for (int col = 0; col < 9; col++) {
            float slotX = hx + (col * 24.0f) * pixelScale;
            float selectorW = 24.0f * pixelScale;

            if (!isScreenOpen && inventory.getSelectedSlot() == col) {
                builder.addAtlasQuad(slotX, hotbarY, 0.1f, selectorW, hotbarHeight, 10, 1, 1, 1, false);
            } else if (isScreenOpen && hoveredSlot == col) {
                builder.addAtlasQuad(slotX, hotbarY, 0.1f, selectorW, hotbarHeight, 10, 1, 1, 1, false);
            }

            builder.drawItem(inventory.getStack(col), slotX + 2, hotbarY + 2, 0.0f, selectorW - 4);
        }
    }
}