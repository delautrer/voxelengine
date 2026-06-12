package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.Engine;
import de.delautrer.engine.utils.GamePaths;
import de.delautrer.game.states.PlayScene;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.ui.elements.UIButton;
import de.delautrer.game.ui.elements.UIConfirmButton;
import de.delautrer.game.ui.elements.UIInputField;
import de.delautrer.game.ui.elements.UIScrollableList;
import de.delautrer.game.ui.elements.UIWorldRow;
import de.delautrer.game.world.WorldStorageManager;
import de.delautrer.game.world.persistence.WorldData;
import java.io.File;

public class WorldSelectScreen extends MenuScreen {
    private final Engine engine;
    private final Runnable onBackAction;

    private boolean isCreatingWorld = false;
    private boolean isSuperFlat = false;
    private boolean isCreativeMode = false;
    private boolean allowCheats = false;

    private UIInputField nameInput;
    private UIInputField seedInput;

    private final int PANEL_GRID_X = 3;
    private final int PANEL_GRID_Y = 0;

    private final java.util.List<de.delautrer.engine.graphics.ITexture> thumbnails = new java.util.ArrayList<>();
    private UIWorldRow selectedRow = null;

    public WorldSelectScreen(Engine engine, Runnable onBackAction) {
        this.engine = engine;
        this.onBackAction = onBackAction;
    }

    @Override
    protected void onInit() {
        if (nameInput == null) {
            nameInput = new UIInputField(0, 0, 400, 40, "Enter World Name...", 20);
        }
        if (seedInput == null) {
            seedInput = new UIInputField(0, 0, 400, 40, "Seed (leave empty for random)", 20);
        }
        buildLayout();
    }

    @Override
    public void onClose() {
        for (de.delautrer.engine.graphics.ITexture tex : thumbnails) {
            tex.cleanup();
        }
        thumbnails.clear();
    }

    private void buildLayout() {
        elements.clear();
        for (de.delautrer.engine.graphics.ITexture tex : thumbnails) {
            tex.cleanup();
        }
        thumbnails.clear();

        float centerX = width / 2.0f;

        if (isCreatingWorld) {
            buildCreationPopup(centerX);
        } else {
            buildWorldBrowser(centerX);
        }
    }

    // ==========================================
    // ANSICHT 1: DER WELTEN-BROWSER (Liste)
    // ==========================================
    private void buildWorldBrowser(float centerX) {
        float listWidth = 800.0f;
        // Make list shorter so it doesn't overlap the newWorldBtn
        float listHeight = height - 260.0f;
        float listY = 150.0f; 

        UIScrollableList worldList = new UIScrollableList(centerX - (listWidth / 2.0f), listY, listWidth, listHeight);
        float itemY = listY + listHeight - 110.0f; // More padding at the top

        File savesDir = GamePaths.SAVES_DIR.toFile();
        if (!savesDir.exists())
            savesDir.mkdirs();

        File[] saveFiles = savesDir.listFiles(File::isDirectory);
        java.util.List<File> sortedFiles = new java.util.ArrayList<>();
        if (saveFiles != null) {
            sortedFiles.addAll(java.util.Arrays.asList(saveFiles));
            sortedFiles.sort((f1, f2) -> {
                WorldData d1 = WorldStorageManager.readMetadataForUI(f1);
                WorldData d2 = WorldStorageManager.readMetadataForUI(f2);
                long d1Date = (d1 != null && d1.lastOpenedDate > 0) ? d1.lastOpenedDate : ((d1 != null && d1.creationDate > 0) ? d1.creationDate : 0);
                long d2Date = (d2 != null && d2.lastOpenedDate > 0) ? d2.lastOpenedDate : ((d2 != null && d2.creationDate > 0) ? d2.creationDate : 0);
                return Long.compare(d2Date, d1Date); // Descending
            });
        }

        if (sortedFiles.isEmpty()) {
            isCreatingWorld = true;
            buildLayout();
            return;
        }

        for (File saveFolder : sortedFiles) {
            String safeFolderName = saveFolder.getName();
            String displayName = safeFolderName;
            WorldData data = WorldStorageManager.readMetadataForUI(saveFolder);
            String versionText = "Unknown Version";
            String dateText = "Unknown Date";

            if (data != null) {
                if (data.worldName != null && !data.worldName.isEmpty())
                    displayName = data.worldName;
            }

            de.delautrer.engine.graphics.ITexture thumb = null;
            File thumbFile = new File(saveFolder, "level.png");
            if (thumbFile.exists()) {
                thumb = engine.getGraphicsFactory().createTexture(thumbFile.getAbsolutePath());
                if (thumb != null) {
                    thumbnails.add(thumb);
                }
            }

            UIWorldRow row = new UIWorldRow(worldList.getX() + 20, itemY, listWidth - 40, 80, displayName, safeFolderName, data, thumb, null);
            
            if (selectedRow != null && selectedRow.getSafeFolderName().equals(safeFolderName)) {
                row.setSelected(true);
                selectedRow = row; 
            }

            final UIWorldRow finalRow = row;
            
            float thumbSize = 70.0f;
            float thumbX = row.getX() + 5.0f;
            float thumbY = row.getY() + 5.0f;
            UIButton directPlayBtn = new UIButton(thumbX, thumbY, thumbSize, thumbSize, "", () -> {
                onClose();
                engine.getSceneManager().changeScene(new PlayScene(engine, finalRow.getData() != null ? finalRow.getData().worldName : finalRow.getSafeFolderName(), finalRow.getSafeFolderName()));
            }) {
                @Override
                public void render(de.delautrer.game.ui.UIMeshBuilder builder, de.delautrer.engine.graphics.IFont font, float mouseX, float mouseY) {}
            };
            
            UIButton invisibleBtn = new UIButton(worldList.getX() + 20, itemY, listWidth - 40, 80, "", () -> {
                if (selectedRow != null) selectedRow.setSelected(false);
                selectedRow = finalRow;
                selectedRow.setSelected(true);
                buildLayout();
            }) {
                @Override
                public void render(de.delautrer.game.ui.UIMeshBuilder builder, de.delautrer.engine.graphics.IFont font, float mouseX, float mouseY) {}
            };
            
            worldList.addItem(row);
            worldList.addItem(directPlayBtn); 
            worldList.addItem(invisibleBtn);
            itemY -= 90.0f;
        }

        elements.add(worldList);

        // --- BOTTOM BAR BUTTONS ---
        float btnY = 30.0f;
        UIButton backBtn = new UIButton(centerX - 390, btnY, 180, 40, "Back", () -> {
            onClose();
            if (onBackAction != null) onBackAction.run();
        });

        UIButton playBtn = new UIButton(centerX - 190, btnY, 180, 40, "Play", () -> {
            if (selectedRow != null) {
                onClose();
                engine.getSceneManager().changeScene(new PlayScene(engine, selectedRow.getData() != null ? selectedRow.getData().worldName : selectedRow.getSafeFolderName(), selectedRow.getSafeFolderName()));
            }
        });
        playBtn.setDisabled(selectedRow == null);

        UIButton recreateBtn = new UIButton(centerX + 10, btnY, 180, 40, "Recreate", () -> {
            if (selectedRow != null && selectedRow.getData() != null) {
                onClose();
                engine.getSceneManager().changeScene(new PlayScene(engine, selectedRow.getData().worldName, selectedRow.getData().seed));
            }
        });
        recreateBtn.setDisabled(selectedRow == null);

        UIConfirmButton deleteBtn = new UIConfirmButton(centerX + 210, btnY, 180, 40, "Delete", "Sure?", () -> {
            if (selectedRow != null) {
                deleteDirectory(new File(GamePaths.SAVES_DIR.toFile(), selectedRow.getSafeFolderName()));
                selectedRow = null;
                buildLayout();
            }
        });
        deleteBtn.setDisabled(selectedRow == null);

        UIButton newWorldBtn = new UIButton(centerX - 390, btnY + 50, 780, 40, "Create New World", () -> {
            isCreatingWorld = true;
            buildLayout();
        });

        elements.add(backBtn);
        elements.add(playBtn);
        elements.add(recreateBtn);
        elements.add(deleteBtn);
        elements.add(newWorldBtn);
    }

    @Override
    protected void onBackgroundClicked() {
        if (selectedRow != null) {
            selectedRow.setSelected(false);
            selectedRow = null;
            buildLayout();
        }
    }

    // ==========================================
    // ANSICHT 2: DAS "NEUE WELT" POPUP
    // ==========================================
    private void buildCreationPopup(float centerX) {
        float centerY = height / 2.0f;

        de.delautrer.game.ui.elements.UIVBox layoutBox = new de.delautrer.game.ui.elements.UIVBox(0, 0, 16.0f);

        String typeStr = isSuperFlat ? "World Type: Super Flat" : "World Type: Normal";
        UIButton typeToggleBtn = new UIButton(0, 0, 400, 40, typeStr, () -> {
            isSuperFlat = !isSuperFlat;
            buildLayout();
        });

        String modeStr = isCreativeMode ? "GameMode: Creative" : "GameMode: Survival";
        UIButton modeToggleBtn = new UIButton(0, 0, 400, 40, modeStr, () -> {
            isCreativeMode = !isCreativeMode;
            buildLayout();
        });

        String cheatsStr = allowCheats ? "Allow Cheats: On" : "Allow Cheats: Off";
        UIButton cheatsToggleBtn = new UIButton(0, 0, 400, 40, cheatsStr, () -> {
            allowCheats = !allowCheats;
            buildLayout();
        });

        de.delautrer.game.ui.elements.UIHBox btnBox = new de.delautrer.game.ui.elements.UIHBox(0, 0, 20.0f);
        UIButton cancelBtn = new UIButton(0, 0, 190, 40, "Cancel", () -> {
            File savesDir = GamePaths.SAVES_DIR.toFile();
            File[] saveFiles = savesDir.exists() ? savesDir.listFiles(File::isDirectory) : new File[0];
            if (saveFiles == null || saveFiles.length == 0) {
                if (onBackAction != null) onBackAction.run();
            } else {
                isCreatingWorld = false;
                buildLayout();
            }
        });
        UIButton createBtn = new UIButton(0, 0, 190, 40, "Create", () -> {
            String worldName = nameInput.getText().isEmpty() ? "New World" : nameInput.getText();
            String seedStr = seedInput.getText().replaceAll("[^\\-0-9]", "");
            if (seedStr.equals("-")) seedStr = "";
            long seed = seedStr.isEmpty() ? (long) (Math.random() * Long.MAX_VALUE) : Long.valueOf(seedStr);
            
            String genType = isSuperFlat ? "FLAT" : "DEFAULT";
            String genOpts = isSuperFlat ? "1xbedrock;3xstone;2xdirt;1xgrass_block" : "";
            de.delautrer.game.entity.player.GameMode gm = isCreativeMode ? de.delautrer.game.entity.player.GameMode.CREATIVE : de.delautrer.game.entity.player.GameMode.SURVIVAL;
            engine.getSceneManager().changeScene(new PlayScene(engine, worldName, seed, genType, genOpts, gm, allowCheats));
        });
        
        btnBox.addChild(cancelBtn);
        btnBox.addChild(createBtn);
        btnBox.pack();

        layoutBox.addChild(nameInput);
        layoutBox.addChild(seedInput);
        layoutBox.addChild(typeToggleBtn);
        layoutBox.addChild(modeToggleBtn);
        layoutBox.addChild(cheatsToggleBtn);
        // Add extra space before buttons
        de.delautrer.game.ui.elements.UIElement spacer = new de.delautrer.game.ui.elements.UIElement(0, 0, 400, 10) {
            @Override
            public void render(UIMeshBuilder builder, de.delautrer.engine.graphics.IFont font, float mouseX, float mouseY) {}
        };
        layoutBox.addChild(spacer);
        layoutBox.addChild(btnBox);
        layoutBox.pack();

        float titleSpace = 50.0f;
        layoutBox.setPosition(centerX - layoutBox.getWidth() / 2.0f, centerY - layoutBox.getHeight() / 2.0f - titleSpace / 2.0f);

        elements.add(layoutBox);

    }

    private void deleteDirectory(File dir) {
        File[] allContents = dir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        dir.delete();
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        // 1. Hintergrund
        long time = System.currentTimeMillis();
        builder.addRect(0, 0, 0.0f, width, height, 0.06f, 0.06f, 0.08f, 1.0f);

        for (int i = 0; i < 60; i++) {
            float xBase = (i * 137.5f) % width;
            float yBase = (i * 93.1f) % height;
            float speed = 30.0f + (i % 40);
            float xPos = (xBase + (time / speed)) % width;
            float yPos = (yBase - (time / (speed * 1.5f))) % height;
            if (yPos < 0) yPos += height;
            
            float size = 2.0f + (i % 5);
            float alpha = 0.1f + 0.4f * (float)Math.sin((time + i * 1000) / 600.0);
            if (alpha > 0) {
                builder.addRect(xPos, yPos, 0.005f, size, size, 0.5f * alpha, 0.3f * alpha, 0.9f * alpha, 1.0f);
            }
        }

        float gridSpacing = 50.0f;
        int lines = (int)(height / gridSpacing) + 2;
        float yOffset = (time / 40.0f) % gridSpacing;
        for (int i = -1; i < lines; i++) {
            float yPos = (i * gridSpacing) + yOffset;
            if (yPos < 0 || yPos > height) continue;
            float intensity = 0.08f + 0.08f * (yPos / height);
            builder.addRect(0, yPos, 0.01f, width, 1.0f, intensity, intensity, intensity + 0.03f, 1.0f);
        }

        float lineY = height * 0.85f;
        float glow = 0.5f + 0.2f * (float)Math.sin(time / 500.0);
        builder.addRect(0, lineY, 0.02f, width, 2.0f, glow, glow * 0.4f, glow * 1.8f, 1.0f);
        builder.addRect(0, lineY + 2.0f, 0.02f, width, 1.0f, glow * 0.4f, glow * 0.1f, glow * 0.8f, 1.0f);

        // 2. Wenn das Popup aktiv ist, dimmen wir den Hintergrund und zeichnen ein Panel
        if (isCreatingWorld) {
            // Dunkler Schleier über dem ganzen Bildschirm (Z = 0.02f)
            // builder.addRect(0, 0, 0.02f, width, height, 0.01f, 0.01f, 0.015f, 0.85f);

            // Finde die layoutBox, falls vorhanden
            de.delautrer.game.ui.elements.UILayout layoutBox = null;
            for (de.delautrer.game.ui.elements.UIElement el : elements) {
                if (el instanceof de.delautrer.game.ui.elements.UILayout) {
                    layoutBox = (de.delautrer.game.ui.elements.UILayout) el;
                    break;
                }
            }

            if (layoutBox != null) {
                float titleHeight = 50.0f;
                float padding = 30.0f;
                float popupWidth = layoutBox.getWidth() + padding * 2;
                float popupHeight = layoutBox.getHeight() + padding * 2 + titleHeight;
                float pX = (width / 2.0f) - (popupWidth / 2.0f);
                float pY = layoutBox.getY() - padding;

                // Modernes Panel-Rechteck in der Mitte (Z = 0.03f)
                builder.add9Slice(pX, pY, 0.03f, popupWidth, popupHeight, PANEL_GRID_X, PANEL_GRID_Y, 12.0f);

                if (font != null) {
                    String title = "Create New World";
                    float titleWidth = builder.getTextWidth(title, font);
                    builder.drawText(title, (width / 2.0f) - (titleWidth / 2.0f), pY + popupHeight - 40.0f, 0.05f, font);
                }
            }
        }

        // 3. UI Elemente rendern lassen (die Inputs und Buttons haben intern Z = 0.1f oder höher)
        super.render(builder, mouseX, mouseY);

        // 4. Haupt-Titel rendern (Nur anzeigen, wenn wir NICHT im Popup sind)
        if (!isCreatingWorld && font != null) {
            String title = "Select World";
            float titleWidth = builder.getTextWidth(title, font);
            builder.drawText(title, (width / 2.0f) - (titleWidth / 2.0f), height - 50.0f, 0.2f, font);
        }
    }
}