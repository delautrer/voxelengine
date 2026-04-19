package de.delautrer.game.ui.gui.screens;

import de.delautrer.game.ui.gui.UIElement;
import de.delautrer.game.ui.gui.UIMeshBuilder;
import de.delautrer.game.ui.gui.UIProgressBar;

public class LoadingScreen extends MenuScreen {
    private UIProgressBar progressBar;

    @Override
    protected void onInit() {
        float barWidth = 400.0f;
        float barHeight = 40.0f;
        float startX = (width / 2.0f) - (barWidth / 2.0f);
        float startY = (height / 2.0f) - (barHeight / 2.0f);

        progressBar = new UIProgressBar(startX, startY, barWidth, barHeight);
        elements.add(progressBar);
    }

    public void setProgress(float progress) {
        if (progressBar != null) {
            progressBar.setProgress(progress);
        }
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        builder.addAtlasQuad(0, 0, 0.0f, width, height, 15, 15, 1, 1, false);

        super.render(builder, mouseX, mouseY);

        if (progressBar != null && font != null) {
            int percent = Math.min(100, (int)(progressBar.getProgress() * 100));
            String loadText = "World is loading... " + percent + "%";
            float textWidth = builder.getTextWidth(loadText, font);
            float textX = (width / 2.0f) - (textWidth / 2.0f);

            builder.drawText(loadText, textX, progressBar.getY() - 30.0f, 0.4f, font);
        }
    }
}