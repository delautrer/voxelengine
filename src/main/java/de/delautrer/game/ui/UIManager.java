package de.delautrer.game.ui;

import de.delautrer.engine.graphics.IFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.entity.player.GameMode;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.inventory.ChestInventory;
import de.delautrer.game.inventory.IInventory;
import de.delautrer.game.ui.gui.container.CreativeContainer;
import de.delautrer.game.ui.gui.container.ChestContainer;
import de.delautrer.game.ui.gui.container.CraftingTableContainer;
import de.delautrer.game.ui.gui.container.FurnaceContainer;
import de.delautrer.game.ui.gui.container.StonecutterContainer;
import de.delautrer.game.ui.gui.screens.*;
import de.delautrer.game.ui.gui.container.PlayerContainer;
import de.delautrer.game.ui.gui.screens.ContainerScreen;

public class UIManager {
    private final HUD hud;
    private Screen currentScreen;
    private int lastWidth, lastHeight;

    public UIManager() {
        this.hud = new HUD();
    }

    public void update(InputManager input, PlayerInteraction interaction) {
        if (interaction.getPlayer().isDead()) {
            if (currentScreen != null) {
                currentScreen.onClose();
            }
            currentScreen = null;
            return;
        }
        IInventory externalInv = interaction.getPlayer().getOpenedInventory();
        boolean isPlayerInvOpen = interaction.getInventory().isOpen();

        if (externalInv != null) {
            boolean needsNewScreen = false;
            if (currentScreen == null) {
                needsNewScreen = true;
            } else {
                if (externalInv instanceof ChestInventory && !(currentScreen instanceof ChestScreen)) needsNewScreen = true;
                else if (externalInv instanceof de.delautrer.game.inventory.CraftingTableInventory && !(currentScreen instanceof CraftingTableScreen)) needsNewScreen = true;
                else if (externalInv instanceof de.delautrer.game.inventory.StonecutterInventory && !(currentScreen instanceof StonecutterScreen)) needsNewScreen = true;
                else if (externalInv instanceof de.delautrer.game.inventory.FurnaceInventory && !(currentScreen instanceof FurnaceScreen)) needsNewScreen = true;
                else if (externalInv instanceof de.delautrer.game.inventory.StructureBlockInventory && !(currentScreen instanceof StructureBlockScreen)) needsNewScreen = true;
                else if (externalInv instanceof de.delautrer.game.inventory.JigsawInventory && !(currentScreen instanceof de.delautrer.game.ui.gui.screens.JigsawScreen)) needsNewScreen = true;
            }

            if (needsNewScreen) {
                if (currentScreen != null) {
                    currentScreen.onClose();
                }
                if (externalInv instanceof ChestInventory) {
                    currentScreen = new ChestScreen(
                            new ChestContainer(interaction.getInventory(), (ChestInventory) externalInv));
                } else if (externalInv instanceof de.delautrer.game.inventory.CraftingTableInventory) {
                    currentScreen = new CraftingTableScreen(
                            new CraftingTableContainer(interaction.getInventory(), (de.delautrer.game.inventory.CraftingTableInventory) externalInv));
                } else if (externalInv instanceof de.delautrer.game.inventory.StonecutterInventory) {
                    currentScreen = new StonecutterScreen(
                            new StonecutterContainer(interaction.getInventory(), (de.delautrer.game.inventory.StonecutterInventory) externalInv));
                } else if (externalInv instanceof de.delautrer.game.inventory.FurnaceInventory) {
                    currentScreen = new FurnaceScreen(
                            new FurnaceContainer(interaction.getInventory(), (de.delautrer.game.inventory.FurnaceInventory) externalInv));
                } else if (externalInv instanceof de.delautrer.game.inventory.StructureBlockInventory sbi) {
                    StructureBlockScreen sbs = new StructureBlockScreen(sbi.getBlockEntity());
                    sbs.setInteraction(interaction);
                    currentScreen = sbs;
                } else if (externalInv instanceof de.delautrer.game.inventory.JigsawInventory ji) {
                    de.delautrer.game.ui.gui.screens.JigsawScreen js = new de.delautrer.game.ui.gui.screens.JigsawScreen(ji.getBlockEntity());
                    js.setInteraction(interaction);
                    currentScreen = js;
                }
                if (lastWidth > 0 && currentScreen != null) {
                    currentScreen.init(lastWidth, lastHeight);
                }
            } else {
                if (currentScreen != null) {
                    if (currentScreen instanceof ContainerScreen containerScreen) {
                        containerScreen.setInteraction(interaction);
                    }
                    currentScreen.handleInput(input);
                }
            }

        }
        // 2. Hat der Spieler nur sein eigenes Inventar offen?
        else if (isPlayerInvOpen) {
            boolean needsNewScreen = false;
            if (currentScreen == null || currentScreen instanceof ChestScreen || currentScreen instanceof CraftingTableScreen || currentScreen instanceof StonecutterScreen || currentScreen instanceof FurnaceScreen || currentScreen instanceof StructureBlockScreen || currentScreen instanceof de.delautrer.game.ui.gui.screens.JigsawScreen) {
                needsNewScreen = true;
                if (currentScreen != null) {
                    currentScreen.onClose();
                }

                if (interaction.getPlayer().getGameMode() == GameMode.CREATIVE) {
                    currentScreen = new CreativeInventoryScreen(new CreativeContainer(interaction.getInventory()));
                } else {
                    currentScreen = new InventoryScreen(new PlayerContainer(interaction.getInventory()));
                }

                if (lastWidth > 0)
                    currentScreen.init(lastWidth, lastHeight);
            }
            if (!needsNewScreen && currentScreen != null) {
                if (currentScreen instanceof ContainerScreen containerScreen) {
                    containerScreen.setInteraction(interaction);
                } else if (currentScreen instanceof StructureBlockScreen structureBlockScreen) {
                    structureBlockScreen.setInteraction(interaction);
                }
                currentScreen.handleInput(input);
            }

        }
        // 3. Gar kein Inventar offen
        else {
            if (currentScreen != null) {
                currentScreen.onClose();
            }
            currentScreen = null;
        }
    }

    public void buildMeshes(UIMeshBuilder builder, int width, int height, InputManager input,
            PlayerInteraction interaction, float mouseX, float mouseY, DebugOverlay debugOverlay,
            ChatOverlay chatOverlay, IFont font, int blockAtlasWidth) {
        builder.clear();

        if (width != lastWidth || height != lastHeight) {
            lastWidth = width;
            lastHeight = height;
            if (currentScreen != null)
                currentScreen.init(width, height);
        }

        int hoveredSlot = -1;
        boolean showCursor = false;
        boolean isHovering = false;

        if (currentScreen != null && !(currentScreen instanceof ChatScreen)) {
            showCursor = true;
            hoveredSlot = currentScreen.getHoveredSlot(mouseX, mouseY);
            isHovering = (hoveredSlot != -1);

            input.setUICursorState(showCursor, isHovering);
        }

        hud.render(builder, width, height, interaction, hoveredSlot, debugOverlay, chatOverlay, font, blockAtlasWidth);

        if (currentScreen != null) {
            if (currentScreen instanceof MenuScreen) {
                ((MenuScreen) currentScreen).setFont(font);
            } else if (currentScreen instanceof StructureBlockScreen) {
                ((StructureBlockScreen) currentScreen).setFont(font);
            } else if (currentScreen instanceof de.delautrer.game.ui.gui.screens.JigsawScreen js) {
                js.setFont(font);
            }
            currentScreen.render(builder, mouseX, mouseY);
        }
    }
}
