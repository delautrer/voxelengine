package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.graphics.IFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.blocks.entities.StructureBlockEntity;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.game.ui.elements.UIButton;
import de.delautrer.game.ui.elements.UIInputField;
import de.delautrer.game.world.World;
import de.delautrer.game.world.generation.structure.StructureRegistry;
import de.delautrer.game.world.generation.structure.StructureTemplate;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StructureBlockScreen extends Screen {

    private final StructureBlockEntity blockEntity;
    private PlayerInteraction interaction;
    private IFont font;

    private float panelX, panelY, panelW, panelH;

    private UIButton modeButton;
    private UIButton actionButton;

    private UIInputField nameField;
    private UIInputField sizeXField, sizeYField, sizeZField;
    private UIInputField offXField, offYField, offZField;

    private final List<UIInputField> fields = new ArrayList<>();
    private UIInputField focusedField = null;

    private String statusMessage = "";
    private long statusMessageTime = 0;

    public StructureBlockScreen(StructureBlockEntity blockEntity) {
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
        nameField = new UIInputField(panelX + 20 * pixelScale, panelY + panelH - 75 * pixelScale, 380 * pixelScale, fieldH, "Structure Name", 32);
        nameField.setText(blockEntity.getName());
        fields.add(nameField);

        // Size Fields
        float thirdW = 115 * pixelScale;
        sizeXField = new UIInputField(panelX + 20 * pixelScale, panelY + panelH - 145 * pixelScale, thirdW, fieldH, "Size X", 4);
        sizeXField.setText(String.valueOf(blockEntity.getSizeX()));
        fields.add(sizeXField);

        sizeYField = new UIInputField(panelX + 145 * pixelScale, panelY + panelH - 145 * pixelScale, thirdW, fieldH, "Size Y", 4);
        sizeYField.setText(String.valueOf(blockEntity.getSizeY()));
        fields.add(sizeYField);

        sizeZField = new UIInputField(panelX + 270 * pixelScale, panelY + panelH - 145 * pixelScale, thirdW, fieldH, "Size Z", 4);
        sizeZField.setText(String.valueOf(blockEntity.getSizeZ()));
        fields.add(sizeZField);

        // Offset Fields
        offXField = new UIInputField(panelX + 20 * pixelScale, panelY + panelH - 215 * pixelScale, thirdW, fieldH, "Offset X", 5);
        offXField.setText(String.valueOf(blockEntity.getOffX()));
        fields.add(offXField);

        offYField = new UIInputField(panelX + 145 * pixelScale, panelY + panelH - 215 * pixelScale, thirdW, fieldH, "Offset Y", 5);
        offYField.setText(String.valueOf(blockEntity.getOffY()));
        fields.add(offYField);

        offZField = new UIInputField(panelX + 270 * pixelScale, panelY + panelH - 215 * pixelScale, thirdW, fieldH, "Offset Z", 5);
        offZField.setText(String.valueOf(blockEntity.getOffZ()));
        fields.add(offZField);

        // Mode Button (bottom left next to action button)
        modeButton = new UIButton(panelX + 20 * pixelScale, panelY + 20 * pixelScale, 175 * pixelScale, 36 * pixelScale,
                "Mode: " + blockEntity.getMode().toUpperCase(), () -> {
            String newMode = "save".equalsIgnoreCase(blockEntity.getMode()) ? "load" : "save";
            blockEntity.setMode(newMode);
            modeButton.setText("Mode: " + newMode.toUpperCase());
            actionButton.setText(newMode.toUpperCase());
        });

        // Action Button (SAVE / LOAD, bottom right)
        actionButton = new UIButton(panelX + 225 * pixelScale, panelY + 20 * pixelScale, 175 * pixelScale, 36 * pixelScale,
                blockEntity.getMode().toUpperCase(), () -> {
            syncBEValues();
            World w = interaction != null ? interaction.getWorld() : blockEntity.getWorld();
            if ("save".equalsIgnoreCase(blockEntity.getMode())) {
                statusMessage = blockEntity.executeSave(w);
            } else {
                statusMessage = blockEntity.executeLoad(w);
            }
            statusMessageTime = System.currentTimeMillis();
            if (interaction != null && interaction.getEventBus() != null) {
                interaction.getEventBus().publish(new de.delautrer.game.events.ChatMessageEvent(statusMessage));
            }
        });
    }

    private List<String> getAvailableTemplateNames() {
        List<String> list = new ArrayList<>();
        Set<NamespacedKey> keys = StructureRegistry.getTemplateKeys();
        for (NamespacedKey key : keys) {
            list.add(key.getKey());
        }
        return list;
    }

    private void updateFieldsFromTemplate(String templateName) {
        if (templateName == null || templateName.trim().isEmpty()) return;
        String cleanName = templateName.trim().toLowerCase();
        NamespacedKey key = cleanName.contains(":") ? NamespacedKey.fromString(cleanName) : NamespacedKey.fromString("veinstride:" + cleanName);
        StructureTemplate template = StructureRegistry.getTemplate(key);
        if (template != null) {
            blockEntity.setSize(template.getSizeX(), template.getSizeY(), template.getSizeZ());
            sizeXField.setText(String.valueOf(template.getSizeX()));
            sizeYField.setText(String.valueOf(template.getSizeY()));
            sizeZField.setText(String.valueOf(template.getSizeZ()));
        }
    }

    private void syncBEValues() {
        String name = nameField.getText();
        blockEntity.setName(name);
        updateFieldsFromTemplate(name);

        try {
            int sx = Integer.parseInt(sizeXField.getText());
            int sy = Integer.parseInt(sizeYField.getText());
            int sz = Integer.parseInt(sizeZField.getText());
            blockEntity.setSize(sx, sy, sz);
        } catch (NumberFormatException ignored) {}

        try {
            int ox = Integer.parseInt(offXField.getText());
            int oy = Integer.parseInt(offYField.getText());
            int oz = Integer.parseInt(offZField.getText());
            blockEntity.setOffset(ox, oy, oz);
        } catch (NumberFormatException ignored) {}
    }

    @Override
    public int getHoveredSlot(float mouseX, float mouseY) {
        return -1;
    }

    @Override
    protected void mouseClicked(float mouseX, float mouseY, int button) {
        float invertedMouseY = height - mouseY;

        // Check buttons
        if (modeButton != null && modeButton.isHovered(mouseX, invertedMouseY)) {
            modeButton.click();
            return;
        }
        if (actionButton != null && actionButton.isHovered(mouseX, invertedMouseY)) {
            actionButton.click();
            return;
        }

        // Check fields
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
            if (focusedField == nameField && input.isActionJustPressed("UI_TAB")) {
                List<String> names = getAvailableTemplateNames();
                if (!names.isEmpty()) {
                    int currentIdx = names.indexOf(nameField.getText().trim().toLowerCase());
                    int nextIdx;
                    if (input.isShiftDown()) {
                        nextIdx = currentIdx <= 0 ? names.size() - 1 : currentIdx - 1;
                    } else {
                        nextIdx = (currentIdx + 1) % names.size();
                    }
                    nameField.setText(names.get(nextIdx));
                    syncBEValues();
                }
                return;
            }

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

        // Title & Field Labels
        if (font != null) {
            builder.drawText("Structure Block", panelX + 20 * pixelScale, panelY + panelH - 22 * pixelScale, 0.1f, font);
            builder.drawText("Structure Name (Press TAB to cycle):", panelX + 20 * pixelScale, panelY + panelH - 45 * pixelScale, 0.1f, font);

            builder.drawText("Size (X, Y, Z):", panelX + 20 * pixelScale, panelY + panelH - 115 * pixelScale, 0.1f, font);
            builder.drawText("Offset (X, Y, Z):", panelX + 20 * pixelScale, panelY + panelH - 185 * pixelScale, 0.1f, font);

            if (!statusMessage.isEmpty() && System.currentTimeMillis() - statusMessageTime < 3000) {
                builder.drawText(statusMessage, panelX + 20 * pixelScale, panelY + 68 * pixelScale, 0.1f, font);
            }
        }

        // Render Buttons
        if (modeButton != null) modeButton.render(builder, font, mouseX, invertedMouseY);
        if (actionButton != null) actionButton.render(builder, font, mouseX, invertedMouseY);

        // Render Fields
        for (UIInputField field : fields) {
            field.render(builder, font, mouseX, invertedMouseY);
        }
    }
}
