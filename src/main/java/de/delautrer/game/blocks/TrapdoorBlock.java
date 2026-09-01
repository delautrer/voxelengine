package de.delautrer.game.blocks;

import de.delautrer.engine.graphics.utils.TextureStitcher;
import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.state.BlockProperties.*;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.BooleanProperty;
import de.delautrer.game.blocks.state.EnumProperty;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.items.BlockItem;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.List;

public class TrapdoorBlock extends CubeBlock implements IInteractable {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final EnumProperty<Half> HALF = EnumProperty.create("half", Half.class);
    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);

    @SuppressWarnings("this-escape")
    public TrapdoorBlock() {
        super(false, true); // nicht voll-solid, transparent (da Löcher in der Textur sein könnten)
        setSoundMaterialName("wood");
        setHardness(2.0f);
        this.mesher = new de.delautrer.engine.graphics.meshing.TrapdoorMesher(this);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockItem item, Vector3i hitFace, Vector3f exactHit) {
        return false;
    }

    @Override
    protected void appendProperties(List<Property<?>> properties) {
        properties.add(OPEN);
        properties.add(HALF);
        properties.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit) {
        BlockState state = getDefaultState().with(OPEN, false);

        // 1. HALF bestimmen (TOP oder BOTTOM)
        if (hitFace.y == 1) {
            // Auf Top eines Blocks geklickt -> Unten im neuen Block-Slot platzieren
            state = state.with(HALF, Half.BOTTOM);
        } else if (hitFace.y == -1) {
            // An Unterseite eines Blocks geklickt -> Oben im neuen Block-Slot platzieren (Hängt an Decke)
            state = state.with(HALF, Half.TOP);
        } else {
            // Seitlich geklickt -> Je nach Klickhöhe (obere oder untere Hälfte)
            float relativeY = exactHit.y - (float) Math.floor(exactHit.y);
            state = state.with(HALF, relativeY > 0.5f ? Half.TOP : Half.BOTTOM);
        }

        // 2. FACING bestimmen (Hinge-Seite / Anker-Seite)
        Direction facing;
        if (hitFace.y == 0) {
            // Seitlich platziert -> Die "Hinge" ist an der Fläche, an der wir sie befestigen
            if (hitFace.x == 1) facing = Direction.WEST;
            else if (hitFace.x == -1) facing = Direction.EAST;
            else if (hitFace.z == 1) facing = Direction.NORTH;
            else facing = Direction.SOUTH;
        } else {
            // Oben/Unten platziert -> Hinge wird durch die Blickrichtung des Spielers bestimmt (Hinge "gegenüber")
            float yaw = ((LocalPlayer) player).getCamera().getYaw();
            yaw = (yaw % 360 + 360) % 360;
            if (yaw >= 45 && yaw < 135) facing = Direction.SOUTH;
            else if (yaw >= 135 && yaw < 225) facing = Direction.WEST;
            else if (yaw >= 225 && yaw < 315) facing = Direction.NORTH;
            else facing = Direction.EAST;
        }

        return state.with(FACING, facing);
    }

    @Override
    public boolean onInteract(World world, Vector3i pos, LocalPlayer player) {
        BlockState state = world.getBlockState(pos.x, pos.y, pos.z);
        world.setBlockState(pos.x, pos.y, pos.z, state.with(OPEN, !state.getValue(OPEN)));
        // Hier könntest du noch ein Sound-Event abspielen
        return true;
    }

    @Override
    public List<AABB> getBoundingBoxes(BlockState state) {
        boolean open = state.getValue(OPEN);
        Half half = state.getValue(HALF);
        Direction facing = state.getValue(FACING);
        float t = 0.1875f; // Dicke (3/16)

        if (!open) {
            return List.of(new AABB(new Vector3f(0, half == Half.TOP ? 1-t : 0, 0), new Vector3f(1, half == Half.TOP ? 1 : t, 1)));
        } else {
            if (facing == Direction.NORTH) return List.of(new AABB(new Vector3f(0, 0, 0), new Vector3f(1, 1, t)));
            if (facing == Direction.SOUTH) return List.of(new AABB(new Vector3f(0, 0, 1-t), new Vector3f(1, 1, 1)));
            if (facing == Direction.WEST)  return List.of(new AABB(new Vector3f(0, 0, 0), new Vector3f(t, 1, 1)));
            return List.of(new AABB(new Vector3f(1-t, 0, 0), new Vector3f(1, 1, 1)));
        }
    }

    @Override
    public boolean shouldRenderFaceAgainstState(BlockState myState, BlockState neighborState, BlockFace face) {
        // Immer rendern, um Lücken zu vermeiden, wenn zwei Falltüren nebeneinander sind
        return true;
    }

    @Override
    public TextureStitcher.AtlasRegion getTextureForFace(BlockState state, BlockFace face) {
        boolean open = state.getValue(OPEN);
        Direction facing = state.getValue(FACING);
        Half half = state.getValue(HALF);

        // Bestimme, welche Welt-Flächen die "breiten" Flächen der Falltür sind
        BlockFace wideFace1;
        BlockFace wideFace2;

        if (!open) {
            // Geschlossen: Oben und Unten sind breit
            wideFace1 = BlockFace.UP;
            wideFace2 = BlockFace.DOWN;
        } else {
            // Offen: Die Flächen gegenüber/an der Hinge-Seite sind breit
            if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                wideFace1 = BlockFace.NORTH;
                wideFace2 = BlockFace.SOUTH;
            } else {
                wideFace1 = BlockFace.EAST;
                wideFace2 = BlockFace.WEST;
            }
        }

        if (face == wideFace1) {
            TextureStitcher.AtlasRegion tex = getModel().side_top;
            if (tex != null) return tex;
        }
        if (face == wideFace2) {
            TextureStitcher.AtlasRegion tex = getModel().side_bottom;
            if (tex != null) return tex;
        }

        return super.getTextureForFace(state, face);
    }
}