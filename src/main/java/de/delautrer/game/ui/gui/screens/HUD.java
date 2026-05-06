package de.delautrer.game.ui.gui.screens;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.IFont;
import de.delautrer.engine.graphics.utils.TextureStitcher;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.entity.player.GameMode;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.inventory.PlayerInventory;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.ui.ChatOverlay;
import de.delautrer.game.ui.DebugOverlay;
import de.delautrer.game.ui.UIUtils;
import de.delautrer.game.ui.UIMeshBuilder;
import java.util.List;
import de.delautrer.Constants;
import de.delautrer.game.registry.Registries;

public class HUD {

    private long lastSlotChangeTime = 0;
    private int lastSelectedSlot = -1;

    public void render(UIMeshBuilder builder, int width, int height, PlayerInteraction interaction, int hoveredSlot, DebugOverlay debugOverlay, ChatOverlay chatOverlay, IFont font, int blockAtlasWidth) {
        float pixelScale = 2.0f;
        if (height >= 1080) pixelScale = 3.0f;
        if (height >= 1440) pixelScale = 4.0f;

        PlayerInventory inventory = interaction.getInventory();
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
        if (interaction.getPlayer().getGameMode() != GameMode.SPECTATOR && headBlock != Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "air")) {

            TextureStitcher.AtlasRegion reg = headBlock.getModel().north;

            if (reg != null) {
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

        builder.addAtlasQuad(hx, hotbarY, 0.0f, hotbarWidth, hotbarHeight, 1, 1, 9, 1, false);

        // Überprüfe Slot-Wechsel für das Namens-Popup
        int currentSlot = inventory.getSelectedSlot();
        if (currentSlot != lastSelectedSlot) {
            lastSelectedSlot = currentSlot;
            lastSlotChangeTime = System.currentTimeMillis();
        }

        for (int col = 0; col < 9; col++) {
            float slotX = hx + (col * 24.0f) * pixelScale;
            float selectorW = 24.0f * pixelScale;

            if (!isScreenOpen && currentSlot == col) {
                builder.addAtlasQuad(slotX, hotbarY, 0.1f, selectorW, hotbarHeight, 10, 1, 1, 1, false);
            } else if (isScreenOpen && hoveredSlot == col) {
                builder.addAtlasQuad(slotX, hotbarY, 0.1f, selectorW, hotbarHeight, 10, 1, 1, 1, false);
            }

            ItemStack stack = inventory.getStack(col);
            builder.drawItem(stack, slotX + 3 * pixelScale, hotbarY + 3 * pixelScale, 0.2f, selectorW - 6 * pixelScale);

            if (stack != null && stack.amount > 1) {
                if(stack.amount > 9) builder.drawText(String.valueOf(stack.amount), slotX + selectorW - (12.0f * pixelScale), hotbarY + (2.0f * pixelScale), 0.25f, font);
                else builder.drawText(String.valueOf(stack.amount), slotX + selectorW - (8 * pixelScale), hotbarY + (2.0f * pixelScale), 0.25f, font);
            }
        }

        // ==========================================
        // 4. HEALTH BAR (HERZEN)
        // ==========================================
        if (interaction.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            float health = interaction.getPlayer().getHealth();
            float maxHealth = interaction.getPlayer().getMaxHealth();

            int heartCount = (int) Math.ceil(maxHealth / 2.0f); // 20 Leben = 10 Herzen
            float heartSize = 9.0f * pixelScale; // Minecraft-Größe
            float heartSpacing = 8.0f * pixelScale; // Leicht überlappend (8 Pixel Abstand)

            // Start-Position: Linksbündig mit der Hotbar, etwas drüber
            float heartsX = hx;
            float heartsY = hotbarY + hotbarHeight + (5.0f * pixelScale);

            for (int i = 0; i < heartCount; i++) {
                float currentHeartX = heartsX + (i * heartSpacing);

                // Hintergrund: Leeres Herz zeichnen (GridX=0, GridY=2)
                builder.addAtlasQuad(currentHeartX, heartsY, 0.05f, heartSize, heartSize, 0, 2, 1, 1, false);

                float heartValue = (i + 1) * 2.0f;

                if (health >= heartValue) {
                    // Volles Herz drüberzeichnen (GridX=2, GridY=2)
                    builder.addAtlasQuad(currentHeartX, heartsY, 0.06f, heartSize, heartSize, 2, 2, 1, 1, false);
                } else if (health >= heartValue - 1.0f) {
                    // Halbes Herz drüberzeichnen (GridX=1, GridY=2)
                    builder.addAtlasQuad(currentHeartX, heartsY, 0.06f, heartSize, heartSize, 1, 2, 1, 1, false);
                }
            }
        }

        // --- 5. ITEM NAME POPUP ---
        long elapsed = System.currentTimeMillis() - lastSlotChangeTime;
        if (elapsed < 2000 && inventory.getStack(currentSlot) != null) {
            String rawId = ItemRegistry.getId(inventory.getStack(currentSlot).type);
            String itemName = UIUtils.formatItemName(rawId);

            float textScale = 0.3f;
            float textWidth = builder.getTextWidth(itemName, font);

            // NEU: Wenn wir im Survival sind, muss das Popup wegen der Herzen höher liegen
            float popupBaseY = hotbarY + hotbarHeight + (16.0f * pixelScale);
            if (interaction.getPlayer().getGameMode() == GameMode.SURVIVAL) {
                popupBaseY += 12.0f * pixelScale; // Platz für Herzen machen
            }

            float textX = (width / 2.0f) - (textWidth / 2);
            builder.drawText(itemName, textX, popupBaseY, textScale, font);
        }
    }
}
