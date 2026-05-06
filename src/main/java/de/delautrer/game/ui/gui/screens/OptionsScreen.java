package de.delautrer.game.ui.gui.screens;

import de.delautrer.game.settings.GameSettings;
import de.delautrer.game.settings.SettingsManager;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.ui.elements.*;
import org.lwjgl.glfw.GLFW;
import java.util.Map;

public class OptionsScreen extends MenuScreen {
    private final Runnable onBackAction;
    private final GameSettings tempSettings;

    public OptionsScreen(Runnable onBackAction) {
        this.onBackAction = onBackAction;
        this.tempSettings = SettingsManager.get();
    }

    @Override
    protected void onInit() {
        elements.clear();

        float listWidth = 460.0f; // Etwas breiter gemacht für die beiden Spalten
        float listHeight = height - 160.0f;

        UIVBox mainBox = new UIVBox(0, 0, 20.0f);
        UIScrollableList scrollList = new UIScrollableList(0, 0, listWidth, listHeight);

        mainBox.addChild(scrollList);
        mainBox.addChild(new UIButton(0, 0, 300.0f, 40.0f, "Save & Back", () -> {
            SettingsManager.save();
            if (onBackAction != null) onBackAction.run();
        }));

        mainBox.setPosition((width - mainBox.getWidth()) / 2.0f, (height - mainBox.getHeight()) / 2.0f);
        elements.add(mainBox);

        // ========================================================
        // Responsive Breiten berechnen
        // ========================================================
        UIVBox contentBox = new UIVBox(0, 0, 10.0f);

        float btnHeight = 40.0f;
        float spacingHBox = 10.0f; // Abstand zwischen Slidern, die nebeneinander sind

        // Die verfügbare Breite in der Liste (abzgl. Scrollbar & Padding)
        float fullWidth = listWidth - 40.0f - 2f * pixelScale;

        // Die halbe Breite für 2 Elemente nebeneinander (wir ziehen den Abstand ab!)
        float halfWidth = (fullWidth - spacingHBox) / 2.0f;

        // --- BEREICH: GRAFIK ---
        contentBox.addChild(new UISeparator(fullWidth, 30.0f, "Graphics", 2.0f, 0.5f, 0.5f, 0.5f, 1.0f));

        UIHBox graphicsBox = new UIHBox(0, 0, spacingHBox);

        graphicsBox.addChild(new UISlider(0, 0, halfWidth, btnHeight, "FOV",
                GameSettings.MIN_FOV, GameSettings.MAX_FOV, 1f, tempSettings.fov,
                val -> tempSettings.fov = val));

        graphicsBox.addChild(new UISlider(0, 0, halfWidth, btnHeight, "Render Dist",
                GameSettings.MIN_RENDER_DISTANCE, GameSettings.MAX_RENDER_DISTANCE, 1f, tempSettings.renderDistance,
                val -> tempSettings.renderDistance = Math.round(val)));

        contentBox.addChild(graphicsBox);

        // --- BEREICH: STEUERUNG ---
        contentBox.addChild(new UISeparator(fullWidth, 30.0f, "Mouse", 2.0f, 0.5f, 0.5f, 0.5f, 1.0f));

        UIHBox mouseBox = new UIHBox(0, 0, spacingHBox);

        mouseBox.addChild(new UISlider(0, 0, halfWidth, btnHeight, "Sensitivity",
                GameSettings.MIN_SENSITIVITY, GameSettings.MAX_SENSITIVITY, 0.1f, tempSettings.mouseSensitivity,
                val -> tempSettings.mouseSensitivity = val));
        mouseBox.addChild(new UIButton(0, 0, halfWidth, btnHeight, "Invert Y: " + (tempSettings.invertY ? "ON" : "OFF"), () -> {
            tempSettings.invertY = !tempSettings.invertY;
            onInit();
        }));
        contentBox.addChild(mouseBox);

        // --- BEREICH: KEYBINDS ---
        contentBox.addChild(new UISeparator(fullWidth, 30.0f, "Keybinds", 2.0f, 0.5f, 0.5f, 0.5f, 1.0f));

        for (Map.Entry<String, Integer> bind : tempSettings.keyBinds.entrySet()) {
            contentBox.addChild(new UIKeybindButton(fullWidth, btnHeight, bind.getKey(), bind.getValue(), newKey -> {
                if (newKey == GLFW.GLFW_KEY_ESCAPE) {
                    tempSettings.keyBinds.put(bind.getKey(), -1);
                    return;
                }
                tempSettings.keyBinds.put(bind.getKey(), newKey);
            }));
        }

        // Alles verpackt ab in die ScrollList
        float spacingX = 8.0f * pixelScale;
        contentBox.setPosition(scrollList.getX() + spacingX, scrollList.getY() + listHeight - contentBox.getHeight() - 20.0f);
        scrollList.addItem(contentBox);
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        builder.addAtlasQuad(0, 0, 0.0f, width, height, 15, 15, 1, 1, false);
        super.render(builder, mouseX, mouseY);

        if (font != null) {
            String title = "Options & Controls";
            float titleWidth = builder.getTextWidth(title, font);
            builder.drawText(title, (width / 2.0f) - (titleWidth / 2.0f), height - 40.0f, 0.2f, font);
        }
    }
}