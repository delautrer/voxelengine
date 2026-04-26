package de.delautrer.game.ui.gui.screens;

import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.states.PlayScene;
import de.delautrer.game.ui.elements.UIButton;
import de.delautrer.game.ui.elements.UIElement;
import de.delautrer.game.ui.UIMeshBuilder;
import org.joml.Vector3f;

public class DeathScreen extends MenuScreen {
    private final LocalPlayer player;
    private final PlayScene playScene;
    public boolean isSaving = false;

    public DeathScreen(LocalPlayer player, PlayScene playScene) {
        this.player = player;
        this.playScene = playScene;
    }

    @Override
    protected void onInit() {
        elements.clear();
        float btnWidth = 320;
        float btnHeight = 40;
        float centerX = (width - btnWidth) / 2.0f;
        float centerY = height / 2.0f;

        // Respawn Button
        elements.add(new UIButton(centerX, centerY - 20, btnWidth, btnHeight, "Respawn", () -> {
            player.respawn(playScene.getWorld().getWorldSpawnpoint());
            // Danach Maus wieder fangen!
            org.lwjgl.glfw.GLFW.glfwSetInputMode(
                    playScene.getEngine().getWindow().getHandle(),
                    org.lwjgl.glfw.GLFW.GLFW_CURSOR,
                    org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED
            );
            player.getCamera().resetMouseTracking();
        }));

        // Main Menu Button
        elements.add(new UIButton(centerX, centerY - 80, btnWidth, btnHeight, "Main Menu", () -> {
            isSaving = true;
            elements.clear();
            playScene.saveAndQuit();
        }));
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        // Optional: Roter Tint über den Bildschirm
        builder.addAtlasQuad(0, 0, 0, width, height, 6, 0, 1, 1, false);

        if (isSaving) {
            String saveText = "Saving world...";
            float textWidth = builder.getTextWidth(saveText, font);
            float textX = (width / 2.0f) - (textWidth / 2.0f);
            builder.drawText(saveText, textX, height / 2.0f, 0, font);
        } else {
            String deathText = "You died!";
            float textWidth = builder.getTextWidth(deathText, font);
            builder.drawText(deathText, (width - textWidth) / 2.0f, height / 2.0f + 60, 0, font);

            for (UIElement element : elements) {
                element.render(builder, font, mouseX, mouseY);
            }
        }
    }
}