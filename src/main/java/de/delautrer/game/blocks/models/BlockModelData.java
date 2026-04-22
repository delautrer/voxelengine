package de.delautrer.game.blocks.models;

import de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion;

public class BlockModelData {
    public AtlasRegion top;
    public AtlasRegion bottom;
    public AtlasRegion north;
    public AtlasRegion south;
    public AtlasRegion east;
    public AtlasRegion west;

    public boolean directional_textures = true;

    public void fillMissing(AtlasRegion missing) {
        if (top == null) top = missing;
        if (bottom == null) bottom = missing;
        if (north == null) north = missing;
        if (south == null) south = missing;
        if (east == null) east = missing;
        if (west == null) west = missing;
    }
}