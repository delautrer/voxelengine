package de.delautrer.game.blocks;

import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.EnumProperty;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.entity.player.Player;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;
import de.delautrer.game.world.World;
import de.delautrer.engine.physics.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.List;

public class TorchBlock extends CubeBlock {

    public enum TorchAttach { FLOOR, NORTH, SOUTH, EAST, WEST }
    public static final EnumProperty<TorchAttach> ATTACH = EnumProperty.create("attach", TorchAttach.class);

    // Speicher für die vorberechneten rotierten Eckpunkte (Maximale Performance!)
    private static final Vector3f[][] VERTS = new Vector3f[5][8];

    static {
        float w = 1f / 16f;  // Fackel ist 2 Pixel breit
        float h = 10f / 16f; // Fackel ist 10 Pixel hoch

        // Die 8 Eckpunkte einer unrotierten Fackel im Ursprung (0,0,0)
        Vector3f[] base = new Vector3f[]{
                new Vector3f(-w, 0, -w), new Vector3f(w, 0, -w), new Vector3f(w, 0, w), new Vector3f(-w, 0, w), // Unten
                new Vector3f(-w, h, -w), new Vector3f(w, h, -w), new Vector3f(w, h, w), new Vector3f(-w, h, w)  // Oben
        };

        // Wir berechnen die Schrägen für alle 5 Zustände
        for (TorchAttach attach : TorchAttach.values()) {
            Matrix4f mat = new Matrix4f();

            if (attach == TorchAttach.FLOOR) {
                mat.translate(0.5f, 0.0f, 0.5f);
            } else if (attach == TorchAttach.NORTH) { // Hängt an Z=0 Wand (Lehnt nach Süd)
                mat.translate(0.5f, 0.2f, 0.0f).rotateX((float) Math.toRadians(25));
            } else if (attach == TorchAttach.SOUTH) { // Hängt an Z=1 Wand (Lehnt nach Nord)
                mat.translate(0.5f, 0.2f, 1.0f).rotateX((float) Math.toRadians(-25));
            } else if (attach == TorchAttach.WEST) {  // Hängt an X=0 Wand (Lehnt nach Ost)
                mat.translate(0.0f, 0.2f, 0.5f).rotateZ((float) Math.toRadians(-25));
            } else if (attach == TorchAttach.EAST) {  // Hängt an X=1 Wand (Lehnt nach West)
                mat.translate(1.0f, 0.2f, 0.5f).rotateZ((float) Math.toRadians(25));
            }

            VERTS[attach.ordinal()] = new Vector3f[8];
            for (int i = 0; i < 8; i++) {
                VERTS[attach.ordinal()][i] = mat.transformPosition(new Vector3f(base[i]));
            }
        }
    }

    public TorchBlock(int tex) {
        super(false, true, tex, tex, tex);
    }

    @Override
    protected void appendProperties(List<Property<?>> properties) {
        properties.add(ATTACH);
    }

    @Override
    public boolean canWaterFlowInto() {
        return true;
    }

    // ==========================================
    // 1. PLATZIERUNG UND WAND-LOGIK
    // ==========================================
    @Override
    public BlockState getStateForPlacement(World world, Player player, Vector3i hitPos, Vector3i hitFace, Vector3f exactHit) {
        if (hitFace.y == -1) return null;

        Vector3i wallPos = new Vector3i(hitPos).sub(hitFace);
        Block wallBlock = world.getBlockState(wallPos.x, wallPos.y, wallPos.z).getBlock();

        if (!wallBlock.isSolid) return null;

        if (hitFace.y == 1) return getDefaultState().with(ATTACH, TorchAttach.FLOOR);
        if (hitFace.z == 1) return getDefaultState().with(ATTACH, TorchAttach.NORTH);
        if (hitFace.z == -1) return getDefaultState().with(ATTACH, TorchAttach.SOUTH);
        if (hitFace.x == 1) return getDefaultState().with(ATTACH, TorchAttach.WEST);
        if (hitFace.x == -1) return getDefaultState().with(ATTACH, TorchAttach.EAST);

        return null;
    }

    @Override
    public void onNeighborChanged(World world, int x, int y, int z, Vector3i neighborPos, byte newNeighborId) {
        BlockState state = world.getBlockState(x, y, z);
        TorchAttach attach = state.getValue(ATTACH);

        Vector3i wallPos = new Vector3i(x, y, z);
        if (attach == TorchAttach.FLOOR) wallPos.y--;
        else if (attach == TorchAttach.NORTH) wallPos.z--;
        else if (attach == TorchAttach.SOUTH) wallPos.z++;
        else if (attach == TorchAttach.WEST) wallPos.x--;
        else if (attach == TorchAttach.EAST) wallPos.x++;

        Block wallBlock = world.getBlockState(wallPos.x, wallPos.y, wallPos.z).getBlock();
        if (!wallBlock.isSolid) {
            world.setBlockState(x, y, z, BlockRegistry.AIR.getDefaultState());
        }
    }

    // ==========================================
    // 2. HIGHLIGHTER UND KOLLISION (Perfekte kleine Hitboxen!)
    // ==========================================

    @Override
    public List<AABB> getBoundingBoxes(BlockState state) {
        TorchAttach attach = state.getValue(ATTACH);
        if (attach == TorchAttach.FLOOR) return List.of(new AABB(new Vector3f(0.4f, 0.0f, 0.4f), new Vector3f(0.6f, 0.6f, 0.6f)));
        if (attach == TorchAttach.NORTH) return List.of(new AABB(new Vector3f(0.4f, 0.2f, 0.0f), new Vector3f(0.6f, 0.8f, 0.3f)));
        if (attach == TorchAttach.SOUTH) return List.of(new AABB(new Vector3f(0.4f, 0.2f, 0.7f), new Vector3f(0.6f, 0.8f, 1.0f)));
        if (attach == TorchAttach.WEST)  return List.of(new AABB(new Vector3f(0.0f, 0.2f, 0.4f), new Vector3f(0.3f, 0.8f, 0.6f)));
        if (attach == TorchAttach.EAST)  return List.of(new AABB(new Vector3f(0.7f, 0.2f, 0.4f), new Vector3f(1.0f, 0.8f, 0.6f)));
        return super.getBoundingBoxes(state);
    }

    // ==========================================
    // 3. EIGENER 3D-MESHER FÜR DIE SCHRÄGE
    // ==========================================

    @Override
    public void generateMesh(int x, int y, int z, Chunk chunk, ChunkManager cm) {
        BlockState state = chunk.getBlockState(x, y, z);
        TorchAttach attach = state.getValue(ATTACH);

        // Wir holen uns die 8 vorausberechneten Eckpunkte der schrägen Fackel
        Vector3f[] v = VERTS[attach.ordinal()];

        // Fackeln leuchten von selbst, haben also keine weichen Schatten
        float light = 1.0f;

        // U-Koordinaten (Die Breite der Fackel: Pixel 7 bis 9)
        float u0 = 7f / 16f;
        float u1 = 9f / 16f;

        // V-Koordinaten für die SEITEN (Die volle Höhe: Pixel 6 bis 16)
        float vSideTop = 6f / 16f;
        float vSideBot = 1.0f;

        // V-Koordinaten für OBEN (Die 2x2 gelben Flammen-Pixel: Pixel 6 bis 8)
        float vTop0 = 6f / 16f;
        float vTop1 = 8f / 16f;

        // V-Koordinaten für UNTEN (Die 2x2 Holz-Pixel: Pixel 14 bis 16)
        float vBot0 = 14f / 16f;
        float vBot1 = 1.0f;

        // Oben (v4, v7, v6, v5) -> Nutzt die gelben Pixel (vTop)
        addQuad(chunk, x, y, z, v[4], v[7], v[6], v[5], u0, vTop0, u1, vTop1, texTop, light);

        // Unten (v3, v0, v1, v2) -> Nutzt die Holz-Pixel (vBot)
        addQuad(chunk, x, y, z, v[3], v[0], v[1], v[2], u0, vBot0, u1, vBot1, texBottom, light);

        // Süd / Z+ (v3, v2, v6, v7) -> Nutzt die volle Seite
        addQuad(chunk, x, y, z, v[3], v[2], v[6], v[7], u0, vSideTop, u1, vSideBot, texSide, light);

        // Nord / Z- (v1, v0, v4, v5) -> Nutzt die volle Seite
        addQuad(chunk, x, y, z, v[1], v[0], v[4], v[5], u0, vSideTop, u1, vSideBot, texSide, light);

        // West / X- (v0, v3, v7, v4) -> Nutzt die volle Seite
        addQuad(chunk, x, y, z, v[0], v[3], v[7], v[4], u0, vSideTop, u1, vSideBot, texSide, light);

        // Ost / X+ (v2, v1, v5, v6) -> Nutzt die volle Seite
        addQuad(chunk, x, y, z, v[2], v[1], v[5], v[6], u0, vSideTop, u1, vSideBot, texSide, light);
    }

    private void addQuad(Chunk chunk, int x, int y, int z, Vector3f vec0, Vector3f vec1, Vector3f vec2, Vector3f vec3, float u0, float v0, float u1, float v1, int tex, float light) {
        chunk.addFace(
                x + vec0.x, y + vec0.y, z + vec0.z, 1.0f,
                x + vec1.x, y + vec1.y, z + vec1.z, 1.0f,
                x + vec2.x, y + vec2.y, z + vec2.z, 1.0f,
                x + vec3.x, y + vec3.y, z + vec3.z, 1.0f,
                u0, v0, u1, v1,
                tex, light, this,
                1.0f, 1.0f, 1.0f, 1.0f, // Sky Light
                1.0f, 1.0f, 1.0f, 1.0f  // Block Light
        );
    }
}