package de.delautrer.game.items;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.world.World;
import org.joml.Vector3i;

public class ToolItem extends Item {
    public enum ToolType {
        PICKAXE, AXE, SHOVEL
    }

    private final ToolType toolType;
    private final String material;
    private final float efficiency;
    private final int maxDurability;

    public ToolItem(String name, String textureName, ToolType toolType, String material, float efficiency, int maxDurability) {
        super(name, textureName);
        this.toolType = toolType;
        this.material = material;
        this.efficiency = efficiency;
        this.maxDurability = maxDurability;
        this.setMaxStackSize(1);
        this.setCategory("tools");
    }

    public ToolType getToolType() {
        return toolType;
    }

    public String getMaterial() {
        return material;
    }

    public float getEfficiency() {
        return efficiency;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public float getIncorrectToolMultiplier() {
        return 1.0f;
    }

    public boolean isCorrectToolFor(Block block) {
        String name = block.getSoundMaterialName() != null ? block.getSoundMaterialName().toLowerCase() : "";
        String blockName = block.getClass().getSimpleName().toLowerCase();
        
        switch (toolType) {
            case PICKAXE:
                return name.equals("rock") || blockName.contains("ore") || blockName.contains("stone") || blockName.contains("brick") || blockName.contains("furnace") || blockName.contains("crafting");
            case AXE:
                return name.equals("wood") || blockName.contains("log") || blockName.contains("planks") || blockName.contains("chest") || blockName.contains("door") || blockName.contains("trapdoor");
            case SHOVEL:
                return name.equals("dirt") || name.equals("grass") || name.equals("sand") || name.equals("gravel") || blockName.contains("dirt") || blockName.contains("sand") || blockName.contains("gravel") || blockName.contains("grass");
            default:
                return false;
        }
    }

    @Override
    public boolean onUseRightClick(World world, LocalPlayer localPlayer, Vector3i targetBlock,
            Vector3i adjacentBlock, PlayerInteraction interaction) {
        return false;
    }
}
