package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.graphics.IFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.blocks.JigsawBlock;
import de.delautrer.game.blocks.entities.JigsawBlockEntity;
import de.delautrer.game.blocks.state.BlockProperties.Direction;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.ui.elements.UIButton;
import de.delautrer.game.ui.elements.UIInputField;
import de.delautrer.game.world.World;

import java.util.ArrayList;
import java.util.List;

public class JigsawScreen extends Screen {

    private final JigsawBlockEntity blockEntity;
    private PlayerInteraction interaction;
    private IFont font;

    private float panelX, panelY, panelW, panelH;

    private UIInputField nameField;
    private UIInputField targetField;
    private UIInputField poolField;
    private UIInputField turnsIntoField;

    private UIButton jointButton;
    private UIButton orientationButton;
    private UIButton doneButton;

    private final List<UIInputField> fields = new ArrayList<>();
    private UIInputField focusedField = null;

    public JigsawScreen(JigsawBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    public void setInteraction(PlayerInteraction interaction) {
        this.interaction = interaction;
    }

    public void setFont(IFont font) {
        this.font = font;
    }

    @Override
    protected void onInit() {
        fields.clear();

        panelW = 420 * pixelScale;
        panelH = 340 * pixelScale;

        panelX = (width - panelW) / 2.0f;
        panelY = (height - panelH) / 2.0f;

        float fieldH = 28 * pixelScale;

        // Name Field
        nameField = new UIInputField(panelX + 20 * pixelScale, panelY + panelH - 70 * pixelScale, 380 * pixelScale, fieldH, "Name", 32);
        nameField.setText(blockEntity.getName());
        fields.add(nameField);

        // Target Field
        targetField = new UIInputField(panelX + 20 * pixelScale, panelY + panelH - 130 * pixelScale, 380 * pixelScale, fieldH, "Target Name", 32);
        targetField.setText(blockEntity.getTarget());
        fields.add(targetField);

        // Pool Field
        poolField = new UIInputField(panelX + 20 * pixelScale, panelY + panelH - 190 * pixelScale, 380 * pixelScale, fieldH, "Target Pool", 48);
        poolField.setText(blockEntity.getPool());
        fields.add(poolField);

        // Turns Into Field
        turnsIntoField = new UIInputField(panelX + 20 * pixelScale, panelY + panelH - 250 * pixelScale, 380 * pixelScale, fieldH, "Turns Into", 32);
        turnsIntoField.setText(blockEntity.getTurnsInto());
        fields.add(turnsIntoField);

        // Joint Button
        jointButton = new UIButton(panelX + 20 * pixelScale, panelY + 20 * pixelScale, 120 * pixelScale, 32 * pixelScale,
                "Joint: " + blockEntity.getJoint().toUpperCase(), () -> {
            String newJoint = "rollable".equalsIgnoreCase(blockEntity.getJoint()) ? "aligned" : "rollable";
            blockEntity.setJoint(newJoint);
            jointButton.setText("Joint: " + newJoint.toUpperCase());
        });

        // Orientation Button
        orientationButton = new UIButton(panelX + 150 * pixelScale, panelY + 20 * pixelScale, 140 * pixelScale, 32 * pixelScale,
                "Facing: " + blockEntity.getOrientation().toUpperCase(), () -> {
            cycleOrientation();
        });

        // Done Button
        doneButton = new UIButton(panelX + 300 * pixelScale, panelY + 20 * pixelScale, 100 * pixelScale, 32 * pixelScale,
                "DONE", this::onClose);
    }

    private void cycleOrientation() {
        String current = blockEntity.getOrientation().toLowerCase();
        Direction nextDir;
        switch (current) {
            case "north" -> nextDir = Direction.EAST;
            case "east" -> nextDir = Direction.SOUTH;
            case "south" -> nextDir = Direction.WEST;
            case "west" -> nextDir = Direction.UP;
            case "up" -> nextDir = Direction.DOWN;
            default -> nextDir = Direction.NORTH;
        }

        blockEntity.setOrientation(nextDir.name().toLowerCase());
        orientationButton.setText("Facing: " + nextDir.name().toUpperCase());

        World w = interaction != null ? interaction.getWorld() : blockEntity.getWorld();
        if (w != null && blockEntity.getPos() != null) {
            BlockState currentState = w.getBlockState(blockEntity.getPos());
            if (currentState != null && currentState.contains(JigsawBlock.FACING)) {
                w.setBlockState(blockEntity.getPos().x, blockEntity.getPos().y, blockEntity.getPos().z, currentState.with(JigsawBlock.FACING, nextDir));
            }
        }
    }

    private void syncBEValues() {
        blockEntity.setName(nameField.getText());
        blockEntity.setTarget(targetField.getText());
        blockEntity.setPool(poolField.getText());
        blockEntity.setTurnsInto(turnsIntoField.getText());
    }

    @Override
    public int getHoveredSlot(float mouseX, float mouseY) {
        return -1;
    }

    @Override
    protected void mouseClicked(float mouseX, float mouseY, int button) {
        float invertedMouseY = height - mouseY;

        if (jointButton != null && jointButton.isHovered(mouseX, invertedMouseY)) {
            jointButton.click();
            return;
        }
        if (orientationButton != null && orientationButton.isHovered(mouseX, invertedMouseY)) {
            orientationButton.click();
            return;
        }
        if (doneButton != null && doneButton.isHovered(mouseX, invertedMouseY)) {
            doneButton.click();
            return;
        }

        UIInputField newlyFocused = null;
        for (UIInputField field : fields) {
            if (field.isHovered(mouseX, invertedMouseY)) {
                newlyFocused = field;
                field.onMouseDown(mouseX, invertedMouseY, font, false);
                break;
            }
        }

        if (focusedField != null && focusedField != newlyFocused) {
            focusedField.setFocused(false);
        }
        focusedField = newlyFocused;
        if (focusedField != null) {
            focusedField.setFocused(true);
        }
    }

    @Override
    protected void onKeyPressed(InputManager input) {
        if (input.isActionJustPressed("UI_CANCEL")) {
            onClose();
            return;
        }

        if (focusedField != null) {
            focusedField.handleInput(input);
            syncBEValues();
        }
    }

    @Override
    protected void onCharTyped(char c) {
        if (focusedField != null) {
            focusedField.typeChar(c);
            syncBEValues();
        }
    }

    @Override
    public void onClose() {
        syncBEValues();
        if (focusedField != null) {
            focusedField.setFocused(false);
            focusedField = null;
        }
        if (interaction != null && interaction.getPlayer() != null) {
            interaction.getPlayer().closeInventory();
        }
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        float invertedMouseY = height - mouseY;

        // 9-Slice Panel
        builder.add9Slice(panelX, panelY, 0.0f, panelW, panelH, 4, 0, 8.0f * pixelScale);

        if (font != null) {
            builder.drawText("Jigsaw Block", panelX + 20 * pixelScale, panelY + panelH - 20 * pixelScale, 0.1f, font);
            builder.drawText("Name (Identifier):", panelX + 20 * pixelScale, panelY + panelH - 40 * pixelScale, 0.1f, font);
            builder.drawText("Target Name:", panelX + 20 * pixelScale, panelY + panelH - 100 * pixelScale, 0.1f, font);
            builder.drawText("Target Pool:", panelX + 20 * pixelScale, panelY + panelH - 160 * pixelScale, 0.1f, font);
            builder.drawText("Turns Into:", panelX + 20 * pixelScale, panelY + panelH - 220 * pixelScale, 0.1f, font);
        }

        if (jointButton != null) jointButton.render(builder, font, mouseX, invertedMouseY);
        if (orientationButton != null) orientationButton.render(builder, font, mouseX, invertedMouseY);
        if (doneButton != null) doneButton.render(builder, font, mouseX, invertedMouseY);

        for (UIInputField field : fields) {
            field.render(builder, font, mouseX, invertedMouseY);
        }
    }
}
