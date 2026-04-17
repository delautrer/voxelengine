package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.Engine;
import de.delautrer.engine.states.Scene;
import de.delautrer.game.states.PlayScene;
import de.delautrer.game.ui.gui.*;
import de.delautrer.game.world.WorldStorageManager;
import de.delautrer.game.world.persistence.WorldData;

import java.io.File;

public class WorldSelectScreen extends MenuScreen {
    private final Engine engine;
    private final Runnable onBackAction;

    private UIInputField nameInput;
    private UIInputField seedInput;

    public WorldSelectScreen(Engine engine, Runnable onBackAction) {
        this.engine = engine;
        this.onBackAction = onBackAction;
    }

    @Override
    protected void onInit() {
        elements.clear();

        float centerX = width / 2.0f;

        // Da HEIGHT oben ist, starten wir weit oben und ziehen für jede neue Zeile Y ab!
        float startY = height - 120.0f;

        // ==========================================
        // 1. NEUE WELT ERSTELLEN BEREICH
        // ==========================================
        nameInput = new UIInputField(centerX - 210, startY, 200, 40, "Worldname", 20);
        seedInput = new UIInputField(centerX + 10, startY, 200, 40, "Seed (empty = random)", 20);

        // Y - 60.0f (Ein Stück nach unten rutschen)
        UIButton createBtn = new UIButton(centerX - 100, startY - 60.0f, 200, 40, "Create new world", () -> {
            String worldName = nameInput.getText().isEmpty() ? "New World" : nameInput.getText();
            String safeFolderName = WorldStorageManager.getUniqueValidFolderName(worldName);
            String seedStr = seedInput.getText();

            long seed = seedStr.isEmpty() ? (long)(Math.random() * Long.MAX_VALUE) : seedStr.hashCode();

            System.out.println("Erstelle Welt: " + worldName + " | Seed: " + seed);

            // NEU: Wir rufen den PlayScene-Konstruktor FÜR NEUE WELTEN auf
            engine.getSceneManager().changeScene(new PlayScene(engine, worldName, seed));
        });

        elements.add(nameInput);
        elements.add(seedInput);
        elements.add(createBtn);


        // ==========================================
        // 2. EXISTIERENDE WELTEN LADEN / LÖSCHEN
        // ==========================================
        float worldY = startY - 140.0f;

        File savesDir = new File("saves");
        if (!savesDir.exists()) savesDir.mkdirs();

        File[] saveFiles = savesDir.listFiles(File::isDirectory);
        if (saveFiles != null) {
            for (File saveFolder : saveFiles) {
                String wS = saveFolder.getName();
                String displayName = wS;
                WorldData data = WorldStorageManager.readMetadataForUI(saveFolder);
                if (data != null && data.worldName != null) {
                    displayName = data.worldName;
                }
                String wN = displayName;

                // LADE-BUTTON (Fehlerhafte Höhe repariert: ist jetzt fest 40!)
                UIButton loadBtn = new UIButton(centerX - 210, worldY, 300, 40, "Play: " + displayName, () -> {
                    engine.getSceneManager().changeScene(new PlayScene(engine, wN, wS));
                });

                // LÖSCHEN-BUTTON
                UIConfirmButton deleteBtn = new UIConfirmButton(centerX + 110, worldY, 100, 40, "Delete", "You sure?", () -> {
                    deleteDirectory(saveFolder);
                    init(width, height);
                });

                elements.add(loadBtn);
                elements.add(deleteBtn);

                worldY -= 50.0f;
            }
        }

        // ==========================================
        // 3. ZURÜCK ZUM HAUPTMENÜ
        // ==========================================
        // 40.0f ist knapp über dem unteren Bildschirmrand
        UIButton backBtn = new UIButton(centerX - 100, 40.0f, 200, 40, "Back", () -> {
            if (onBackAction != null) onBackAction.run();
        });
        elements.add(backBtn);
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
        // Hintergrund (Schmutz-Textur, Z = 0.0f)
        builder.addAtlasQuad(0, 0, 0.0f, width, height, 15, 15, 1, 1, UIElement.MENU_GRID_SIZE, false);

        super.render(builder, mouseX, mouseY);

        // Titel ganz oben zentriert (Z = 0.2f)
        if (font != null) {
            String title = "Select world";
            float titleWidth = builder.getTextWidth(title, font);
            builder.drawText(title, (width / 2.0f) - (titleWidth / 2.0f), height - 40.0f, 0.2f, font);
        }
    }
}