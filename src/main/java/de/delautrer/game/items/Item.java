package de.delautrer.game.items;

import de.delautrer.engine.graphics.utils.TextureStitcher;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import de.delautrer.game.entity.player.PlayerInteraction;
import org.joml.Vector3i;

public abstract class Item {
    public final String name;
    public final String textureName;
    private TextureStitcher.AtlasRegion iconRegion;

    protected int maxStackSize = 64;
    protected String category = "misc";
    protected boolean renderAsItem = false;

    public Item(String name, String textureName) {
        this.name = name;
        this.textureName = textureName;
    }

    public void setIconRegion(TextureStitcher.AtlasRegion iconRegion) {
        this.iconRegion = iconRegion;
    }

    public TextureStitcher.AtlasRegion getIconRegion() {
        return iconRegion;
    }

    public Item setMaxStackSize(int size) {
        this.maxStackSize = size;
        return this;
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public String getName() {
        return name;
    }

    public void setRenderAsItem(boolean renderAsItem) {
        this.renderAsItem = renderAsItem;
    }

    public boolean isRenderAsItem() {
        return renderAsItem;
    }

    public abstract boolean onUseRightClick(World world, LocalPlayer localPlayer, Vector3i targetBlock,
            Vector3i adjacentBlock, PlayerInteraction interaction);

    public Item setCategory(String category) {
        this.category = category;
        return this;
    }

    public String getCategory() {
        return category;
    }
}
