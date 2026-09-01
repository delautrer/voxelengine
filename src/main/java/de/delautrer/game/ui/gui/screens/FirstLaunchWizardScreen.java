package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.graphics.IFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.settings.GameSettings;
import de.delautrer.game.settings.SettingsManager;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.ui.elements.*;

public class FirstLaunchWizardScreen extends MenuScreen {
    private final Runnable onCompleteAction;

    private int step = 0;
    private static final int TOTAL_STEPS = 4;

    private UIInputField nameInput;
    private UISlider skinSlider;

    public FirstLaunchWizardScreen(Runnable onCompleteAction) {
        this.onCompleteAction = onCompleteAction;
    }

    @Override
    protected void onInit() {
        elements.clear();

        float cardW = 580.0f;
        float cardH = 400.0f;
        float cardX = (width - cardW) / 2.0f;
        float cardY = (height - cardH) / 2.0f;

        switch (step) {
            case 0 -> {
                // Step 1: Welcome & Early Dev
                UIButton nextBtn = new UIButton((width - 220.0f) / 2.0f, cardY + 40.0f, 220.0f, 44.0f, "Next >", () -> {
                    step = 1;
                    onInit();
                });
                elements.add(nextBtn);
            }
            case 1 -> {
                // Step 2: Player Name
                nameInput = new UIInputField((width - 440.0f) / 2.0f, cardY + cardH - 180.0f, 440.0f, 42.0f, "Your Name...", 20);
                String name = SettingsManager.get().playerName;
                if (name != null && !name.isEmpty()) {
                    nameInput.setText(name);
                }
                nameInput.setFocused(true);
                elements.add(nameInput);

                float navW = 420.0f;
                float navX = (width - navW) / 2.0f;
                UIButton backBtn = new UIButton(navX, cardY + 40.0f, 195.0f, 44.0f, "< Back", () -> {
                    step = 0;
                    onInit();
                });
                UIButton nextBtn = new UIButton(navX + 225.0f, cardY + 40.0f, 195.0f, 44.0f, "Next >", () -> {
                    String entered = nameInput.getText().trim();
                    if (!entered.isEmpty()) {
                        SettingsManager.get().playerName = entered;
                    }
                    step = 2;
                    onInit();
                });
                elements.add(backBtn);
                elements.add(nextBtn);
            }
            case 2 -> {
                // Step 3: Skin Color Slider
                skinSlider = new UISlider((width - 440.0f) / 2.0f, cardY + cardH - 170.0f, 440.0f, 40.0f, "Skin Tone", 0.0f, 1.0f, 0.01f,
                        SettingsManager.get().skinToneFactor, (val) -> {
                            SettingsManager.get().skinToneFactor = val;
                        });
                elements.add(skinSlider);

                float navW = 420.0f;
                float navX = (width - navW) / 2.0f;
                UIButton backBtn = new UIButton(navX, cardY + 40.0f, 195.0f, 44.0f, "< Back", () -> {
                    step = 1;
                    onInit();
                });
                UIButton nextBtn = new UIButton(navX + 225.0f, cardY + 40.0f, 195.0f, 44.0f, "Next >", () -> {
                    step = 3;
                    onInit();
                });
                elements.add(backBtn);
                elements.add(nextBtn);
            }
            case 3 -> {
                // Step 4: All set
                UIButton finishBtn = new UIButton((width - 260.0f) / 2.0f, cardY + 40.0f, 260.0f, 46.0f, "Start Game!", () -> {
                    GameSettings s = SettingsManager.get();
                    s.firstLaunch = false;
                    SettingsManager.save();
                    if (onCompleteAction != null) {
                        onCompleteAction.run();
                    }
                });
                elements.add(finishBtn);
            }
        }
    }

    @Override
    public void handleMenuInput(InputManager input, float mouseX, float mouseY) {
        if (step == 1 && nameInput != null) {
            if (input.isActionJustPressed("UI_CLICK")) {
                boolean clickedInput = mouseX >= nameInput.getX() && mouseX <= nameInput.getX() + nameInput.getWidth() &&
                                       mouseY >= nameInput.getY() && mouseY <= nameInput.getY() + nameInput.getHeight();
                nameInput.setFocused(clickedInput);
            }
            nameInput.handleInput(input);
        } else if (step == 2 && skinSlider != null) {
            skinSlider.handleInput(input, mouseX, mouseY);
        }

        super.handleMenuInput(input, mouseX, mouseY);
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        // Darkened overlay background
        builder.addRect(0, 0, 0.05f, width, height, 0.03f, 0.03f, 0.05f, 0.94f);

        float cardW = 580.0f;
        float cardH = 400.0f;
        float cardX = (width - cardW) / 2.0f;
        float cardY = (height - cardH) / 2.0f;
        builder.add9Slice(cardX, cardY, 0.08f, cardW, cardH, 4, 0, 12.0f);

        if (font != null) {
            String stepStr = "Step " + (step + 1) + " of " + TOTAL_STEPS;
            float stepScale = 0.75f;
            float stepW = builder.getTextWidth(stepStr, font) * stepScale;

            switch (step) {
                case 0 -> {
                    String title = "Welcome to Veinstride!";
                    float tW = builder.getTextWidth(title, font);
                    builder.drawText(title, (width - tW) / 2.0f, cardY + cardH - 38.0f, 0.15f, font);
                    builder.drawText(stepStr, (width - stepW) / 2.0f, cardY + cardH - 68.0f, 0.15f, font, 0.6f, 0.6f, 0.7f, 1.0f, stepScale);

                    String text1 = "Veinstride is currently in";
                    String text2 = "early active development.";
                    String text3 = "Thank you for testing and playing!";
                    float sScale = 0.8f;
                    float w1 = builder.getTextWidth(text1, font) * sScale;
                    float w2 = builder.getTextWidth(text2, font) * sScale;
                    float w3 = builder.getTextWidth(text3, font) * sScale;

                    builder.drawText(text1, (width - w1) / 2.0f, cardY + cardH - 120.0f, 0.15f, font, 0.85f, 0.85f, 0.95f, 1.0f, sScale);
                    builder.drawText(text2, (width - w2) / 2.0f, cardY + cardH - 145.0f, 0.15f, font, 0.85f, 0.85f, 0.95f, 1.0f, sScale);
                    builder.drawText(text3, (width - w3) / 2.0f, cardY + cardH - 180.0f, 0.15f, font, 0.85f, 0.85f, 0.95f, 1.0f, sScale);
                }
                case 1 -> {
                    String title = "What Should We Call You?";
                    float tW = builder.getTextWidth(title, font);
                    builder.drawText(title, (width - tW) / 2.0f, cardY + cardH - 38.0f, 0.15f, font);
                    builder.drawText(stepStr, (width - stepW) / 2.0f, cardY + cardH - 68.0f, 0.15f, font, 0.6f, 0.6f, 0.7f, 1.0f, stepScale);

                    String sub = "Enter your preferred player name:";
                    float sScale = 0.8f;
                    float sW = builder.getTextWidth(sub, font) * sScale;
                    builder.drawText(sub, (width - sW) / 2.0f, cardY + cardH - 120.0f, 0.15f, font, 0.85f, 0.85f, 0.95f, 1.0f, sScale);
                }
                case 2 -> {
                    String title = "Hand Skin Tone";
                    float tW = builder.getTextWidth(title, font);
                    builder.drawText(title, (width - tW) / 2.0f, cardY + cardH - 38.0f, 0.15f, font);
                    builder.drawText(stepStr, (width - stepW) / 2.0f, cardY + cardH - 68.0f, 0.15f, font, 0.6f, 0.6f, 0.7f, 1.0f, stepScale);

                    String sub = "Adjust your hand skin tone with the slider:";
                    float sScale = 0.8f;
                    float sW = builder.getTextWidth(sub, font) * sScale;
                    builder.drawText(sub, (width - sW) / 2.0f, cardY + cardH - 110.0f, 0.15f, font, 0.85f, 0.85f, 0.95f, 1.0f, sScale);

                    // Hand Color Preview Rect
                    float[] rgb = SettingsManager.get().getSkinToneColorRGB();
                    String prevLabel = "Hand Preview:";
                    float labelScale = 0.8f;
                    float labelW = builder.getTextWidth(prevLabel, font) * labelScale;
                    float pW = 120.0f;
                    float pH = 26.0f;
                    
                    float totalW = labelW + 15.0f + pW;
                    float startX = (width - totalW) / 2.0f;
                    float pY = cardY + cardH - 235.0f;

                    builder.drawText(prevLabel, startX, pY + 4.0f, 0.15f, font, 0.85f, 0.85f, 0.9f, 1.0f, labelScale);

                    float rectX = startX + labelW + 15.0f;
                    // Frame container
                    builder.add9Slice(rectX - 3.0f, pY - 3.0f, 0.12f, pW + 6.0f, pH + 6.0f, 1, 0, 4.0f);
                    // Live colored preview fill
                    builder.addRect(rectX, pY, 0.22f, pW, pH, rgb[0], rgb[1], rgb[2], 1.0f);
                }
                case 3 -> {
                    String title = "All Set!";
                    float tW = builder.getTextWidth(title, font);
                    builder.drawText(title, (width - tW) / 2.0f, cardY + cardH - 38.0f, 0.15f, font);
                    builder.drawText(stepStr, (width - stepW) / 2.0f, cardY + cardH - 68.0f, 0.15f, font, 0.6f, 0.6f, 0.7f, 1.0f, stepScale);

                    String text1 = "You are ready to explore the world!";
                    String text2 = "You can change these settings anytime";
                    String text3 = "in the main menu via 'Customize'.";
                    float sScale = 0.8f;
                    float w1 = builder.getTextWidth(text1, font) * sScale;
                    float w2 = builder.getTextWidth(text2, font) * sScale;
                    float w3 = builder.getTextWidth(text3, font) * sScale;

                    builder.drawText(text1, (width - w1) / 2.0f, cardY + cardH - 120.0f, 0.15f, font, 0.85f, 0.85f, 0.95f, 1.0f, sScale);
                    builder.drawText(text2, (width - w2) / 2.0f, cardY + cardH - 145.0f, 0.15f, font, 0.85f, 0.85f, 0.95f, 1.0f, sScale);
                    builder.drawText(text3, (width - w3) / 2.0f, cardY + cardH - 170.0f, 0.15f, font, 0.85f, 0.85f, 0.95f, 1.0f, sScale);
                }
            }
        }

        super.render(builder, mouseX, mouseY);
    }
}
