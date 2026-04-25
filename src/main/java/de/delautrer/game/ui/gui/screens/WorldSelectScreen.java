package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.Engine;
import de.delautrer.game.states.PlayScene;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.ui.elements.UIButton;
import de.delautrer.game.ui.elements.UIConfirmButton;
import de.delautrer.game.ui.elements.UIInputField;
import de.delautrer.game.ui.elements.UIScrollableList;
import de.delautrer.game.world.WorldStorageManager;
import de.delautrer.game.world.persistence.WorldData;

import java.io.File;

public class WorldSelectScreen extends MenuScreen {
    private final Engine engine;
    private final Runnable onBackAction;

    // Status: Befinden wir uns im Popup für eine neue Welt?
    private boolean isCreatingWorld = false;

    private final int PANEL_GRID_X = 3;
    private final int PANEL_GRID_Y = 0;

    public WorldSelectScreen(Engine engine, Runnable onBackAction) {
        this.engine = engine;
        this.onBackAction = onBackAction;
    }

    @Override
    protected void onInit() {
        // Beim ersten Laden bauen wir das Standard-Layout (die Liste)
        buildLayout();
    }

    /**
     * Diese Methode baut die UI-Elemente je nach aktuellem Zustand neu auf!
     */
    private void buildLayout() {
        elements.clear();
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
        float listWidth = 600.0f;
        float listHeight = height - 180.0f;
        float listY = 90.0f; // Etwas Platz unten für die Main-Buttons

        UIScrollableList worldList = new UIScrollableList(centerX - (listWidth / 2.0f), listY, listWidth, listHeight);
        float itemY = listY + listHeight - 60.0f;

        File savesDir = new File("saves");
        if (!savesDir.exists()) savesDir.mkdirs();

        File[] saveFiles = savesDir.listFiles(File::isDirectory);
        if (saveFiles != null) {
            for (File saveFolder : saveFiles) {
                String safeFolderName = saveFolder.getName();
                String displayName = safeFolderName;
                WorldData data = WorldStorageManager.readMetadataForUI(saveFolder);
                if (data != null && data.worldName != null) {
                    displayName = data.worldName;
                }
                String wN = displayName;

                UIButton loadBtn = new UIButton(worldList.getX() + 20, itemY, listWidth - 140, 40, "Play: " + displayName, () -> {
                    engine.getSceneManager().changeScene(new PlayScene(engine, wN, safeFolderName));
                });

                UIConfirmButton deleteBtn = new UIConfirmButton(worldList.getX() + listWidth - 110, itemY, 90, 40, "Del", "Sure?", () -> {
                    deleteDirectory(saveFolder);
                    buildLayout(); // Liste nach dem Löschen sofort neu aufbauen!
                });

                worldList.addItem(loadBtn);
                worldList.addItem(deleteBtn);
                itemY -= 50.0f;
            }
        }
        elements.add(worldList);

        // --- BOTTOM BAR BUTTONS ---
        UIButton backBtn = new UIButton(centerX - 300, 30.0f, 290, 40, "Back to Title", () -> {
            if (onBackAction != null) onBackAction.run();
        });

        UIButton newWorldBtn = new UIButton(centerX + 10, 30.0f, 290, 40, "Create New World", () -> {
            isCreatingWorld = true; // Status ändern
            buildLayout();          // UI neu zeichnen lassen (öffnet das Popup)
        });

        elements.add(backBtn);
        elements.add(newWorldBtn);
    }

    // ==========================================
    // ANSICHT 2: DAS "NEUE WELT" POPUP
    // ==========================================
    private void buildCreationPopup(float centerX) {
        // Wir setzen die Felder in die Mitte des Bildschirms
        float centerY = height / 2.0f;

        UIInputField nameInput = new UIInputField(centerX - 200, centerY + 20.0f, 400, 40, "Enter World Name...", 20);
        UIInputField seedInput = new UIInputField(centerX - 200, centerY - 40.0f, 400, 40, "Seed (leave empty for random)", 20);

        UIButton createBtn = new UIButton(centerX + 10, centerY - 110.0f, 190, 40, "Create", () -> {
            String worldName = nameInput.getText().isEmpty() ? "New World" : nameInput.getText();
            String safeFolderName = WorldStorageManager.getUniqueValidFolderName(worldName);
            String seedStr = seedInput.getText().replaceAll("[^0-9]", "");;
            long seed = seedStr.isEmpty() ? (long)(Math.random() * Long.MAX_VALUE) : Long.valueOf(seedStr);

            engine.getSceneManager().changeScene(new PlayScene(engine, worldName, seed));
        });

        UIButton cancelBtn = new UIButton(centerX - 200, centerY - 110.0f, 190, 40, "Cancel", () -> {
            isCreatingWorld = false; // Status ändern
            buildLayout();           // UI neu zeichnen lassen (schließt das Popup)
        });

        elements.add(nameInput);
        elements.add(seedInput);
        elements.add(createBtn);
        elements.add(cancelBtn);
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
        // 1. Hintergrund (Schmutz-Textur, Z = 0.0f)
        builder.addAtlasQuad(0, 0, 0.0f, width, height, 15, 15, 1, 1, false);

        // 2. Wenn das Popup aktiv ist, dimmen wir den Hintergrund und zeichnen ein Panel
        if (isCreatingWorld) {
            // Dunkler Schleier über dem ganzen Bildschirm (Z = 0.02f)
            // builder.addRect(0, 0, 0.02f, width, height, 0.0f, 0.0f, 0.0f, 0.7f);

            // Modernes Panel-Rechteck in der Mitte (Z = 0.03f)
            float popupWidth = 460.0f;
            float popupHeight = 260.0f;
            float pX = (width / 2.0f) - (popupWidth / 2.0f);
            float pY = (height / 2.0f) - (popupHeight / 2.0f);

            // Nutzt dein neues Panel aus Grid X=0, Y=2
            builder.add9Slice(pX, pY, 0.03f, popupWidth, popupHeight, PANEL_GRID_X, PANEL_GRID_Y, 12.0f);

            if (font != null) {
                String title = "Create New World";
                float titleWidth = builder.getTextWidth(title, font);
                builder.drawText(title, (width / 2.0f) - (titleWidth / 2.0f), pY + popupHeight - 30.0f, 0.05f, font);
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