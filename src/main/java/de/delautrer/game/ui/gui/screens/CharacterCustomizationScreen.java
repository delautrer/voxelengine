package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.graphics.IFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.settings.GameSettings;
import de.delautrer.game.settings.SettingsManager;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.ui.elements.*;

public class CharacterCustomizationScreen extends MenuScreen {
    private final Runnable onCloseAction;

    private UIInputField nameInput;
    private UISlider skinSlider;

    public CharacterCustomizationScreen(Runnable onCloseAction) {
        this.onCloseAction = onCloseAction;
    }

    @Override
    protected void onInit() {
        elements.clear();

        float cardW = 540.0f;
        float cardH = 380.0f;
        float cardX = (width - cardW) / 2.0f;
        float cardY = (height - cardH) / 2.0f;

        float boxW = 440.0f;
        float boxX = (width - boxW) / 2.0f;

        // Name Input
        nameInput = new UIInputField(boxX, cardY + cardH - 145.0f, boxW, 40.0f, "Enter Player Name", 20);
        String currentName = SettingsManager.get().playerName;
        if (currentName != null && !currentName.isEmpty()) {
            nameInput.setText(currentName);
        }
        elements.add(nameInput);

        // Skin Tone Slider
        skinSlider = new UISlider(boxX, cardY + cardH - 225.0f, boxW, 40.0f, "Skin Tone", 0.0f, 1.0f, 0.01f,
                SettingsManager.get().skinToneFactor, (val) -> {
                    SettingsManager.get().skinToneFactor = val;
                });
        elements.add(skinSlider);

        // Save & Back Button at bottom of card
        UIButton saveBtn = new UIButton((width - 240.0f) / 2.0f, cardY + 30.0f, 240.0f, 44.0f, "Save & Back", () -> {
            GameSettings settings = SettingsManager.get();
            String enteredName = nameInput.getText().trim();
            if (!enteredName.isEmpty()) {
                settings.playerName = enteredName;
            }
            SettingsManager.save();

            if (onCloseAction != null) {
                onCloseAction.run();
            }
        });
        elements.add(saveBtn);
    }

    @Override
    public void handleMenuInput(InputManager input, float mouseX, float mouseY) {
        if (nameInput != null) {
            if (input.isActionJustPressed("UI_CLICK")) {
                boolean clickedInput = mouseX >= nameInput.getX() && mouseX <= nameInput.getX() + nameInput.getWidth() &&
                                       mouseY >= nameInput.getY() && mouseY <= nameInput.getY() + nameInput.getHeight();
                nameInput.setFocused(clickedInput);
            }
            nameInput.handleInput(input);
        }

        if (skinSlider != null) {
            skinSlider.handleInput(input, mouseX, mouseY);
        }

        super.handleMenuInput(input, mouseX, mouseY);
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        // Darkened overlay background
        builder.addRect(0, 0, 0.05f, width, height, 0.04f, 0.04f, 0.06f, 0.92f);

        // Card Panel
        float cardW = 540.0f;
        float cardH = 380.0f;
        float cardX = (width - cardW) / 2.0f;
        float cardY = (height - cardH) / 2.0f;
        builder.add9Slice(cardX, cardY, 0.08f, cardW, cardH, 4, 0, 12.0f);

        if (font != null) {
            String title = "Character Customization";
            float titleW = builder.getTextWidth(title, font);
            builder.drawText(title, (width - titleW) / 2.0f, cardY + cardH - 38.0f, 0.15f, font);

            String sub = "Customize your player name and hand skin tone:";
            float subScale = 0.75f;
            float subW = builder.getTextWidth(sub, font) * subScale;
            builder.drawText(sub, (width - subW) / 2.0f, cardY + cardH - 68.0f, 0.15f, font, 0.85f, 0.85f, 0.95f, 1.0f, subScale);

            // Labels for inputs
            String nameLabel = "Player Name:";
            float labelScale = 0.80f;
            builder.drawText(nameLabel, cardX + 50.0f, cardY + cardH - 100.0f, 0.15f, font, 0.9f, 0.9f, 0.95f, 1.0f, labelScale);

            String skinLabel = "Hand Skin Tone:";
            builder.drawText(skinLabel, cardX + 50.0f, cardY + cardH - 178.0f, 0.15f, font, 0.9f, 0.9f, 0.95f, 1.0f, labelScale);

            // Hand Skin Tone Live Preview Box
            String prevLabel = "Hand Preview:";
            float prevLabelScale = 0.80f;
            float prevLabelW = builder.getTextWidth(prevLabel, font) * prevLabelScale;
            float[] rgb = SettingsManager.get().getSkinToneColorRGB();
            float pW = 120.0f;
            float pH = 26.0f;

            float totalW = prevLabelW + 15.0f + pW;
            float startX = (width - totalW) / 2.0f;
            float pY = cardY + 92.0f;

            builder.drawText(prevLabel, startX, pY + 4.0f, 0.15f, font, 0.85f, 0.85f, 0.9f, 1.0f, prevLabelScale);

            float rectX = startX + prevLabelW + 15.0f;
            // Frame container at Z = 0.12f
            builder.add9Slice(rectX - 3.0f, pY - 3.0f, 0.12f, pW + 6.0f, pH + 6.0f, 1, 0, 4.0f);
            // Live colored preview fill at Z = 0.22f
            builder.addRect(rectX, pY, 0.22f, pW, pH, rgb[0], rgb[1], rgb[2], 1.0f);
        }

        super.render(builder, mouseX, mouseY);
    }
}
