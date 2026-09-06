package de.delautrer.game.worldgen.jigsaw;

import de.delautrer.engine.physics.AABB;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.JigsawBlock;
import de.delautrer.game.nbt.CompoundTag;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;
import de.delautrer.game.world.WorldGenerator;
import de.delautrer.game.world.generation.structure.StructureRegistry;
import de.delautrer.game.world.generation.structure.StructureTemplate;
import de.delautrer.game.worldgen.pool.TemplatePool;
import de.delautrer.game.worldgen.pool.TemplatePoolRegistry;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;

public class JigsawPlacer {

    public static class JigsawData {
        public final int dx, dy, dz;
        public final String name;
        public final String target;
        public final String pool;
        public final String joint;
        public final String orientation;
        public final String turnsInto;

        public JigsawData(int dx, int dy, int dz, CompoundTag tag) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.name = tag != null && tag.contains("name") ? tag.getString("name") : "veinstride:entrance";
            this.target = tag != null && tag.contains("target") ? tag.getString("target") : "veinstride:building";
            this.pool = tag != null && tag.contains("pool") ? tag.getString("pool") : "veinstride:desert_camp/tents";
            this.joint = tag != null && tag.contains("joint") ? tag.getString("joint") : "rollable";
            this.orientation = tag != null && tag.contains("orientation") ? tag.getString("orientation") : "south";
            this.turnsInto = tag != null && tag.contains("turns_into") ? tag.getString("turns_into") : "veinstride:air";
        }
    }

    public static class PlacedPiece {
        public final NamespacedKey templateKey;
        public final StructureTemplate template;
        public final int originX, originY, originZ;
        public final int rotY;
        public final int depth;
        public final AABB boundingBox;

        public PlacedPiece(NamespacedKey templateKey, StructureTemplate template, int originX, int originY, int originZ, int rotY, int depth) {
            this.templateKey = templateKey;
            this.template = template;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.rotY = rotY;
            this.depth = depth;

            Vector3i rotSize = getRotatedSize(template.getSizeX(), template.getSizeY(), template.getSizeZ(), rotY);
            this.boundingBox = new AABB(
                    new Vector3f(originX, originY, originZ),
                    new Vector3f(originX + rotSize.x, originY + rotSize.y, originZ + rotSize.z)
            );
        }
    }

    public static class PendingJigsaw {
        public final Vector3i worldPos;
        public final JigsawData data;
        public final int depth;

        public PendingJigsaw(Vector3i worldPos, JigsawData data, int depth) {
            this.worldPos = worldPos;
            this.data = data;
            this.depth = depth;
        }
    }

    public static int generate(World world, Chunk currentChunk, WorldGenerator wg, int originX, int originY, int originZ, NamespacedKey startPoolKey, int maxDepth, long seed) {
        Random random = new Random(seed);
        TemplatePool startPool = TemplatePoolRegistry.getPool(startPoolKey);
        if (startPool == null) return 0;

        TemplatePool.PoolElement startElem = startPool.pickWeighted(random);
        if (startElem == null) return 0;

        StructureTemplate startTemplate = StructureRegistry.getTemplate(startElem.getTemplateKey());
        if (startTemplate == null) return 0;

        List<PlacedPiece> placedPieces = new ArrayList<>();
        Queue<PendingJigsaw> queue = new ArrayDeque<>();

        // Place start piece
        placeTemplateRotated(world, currentChunk, wg, startTemplate, originX, originY, originZ, 0);
        PlacedPiece startPiece = new PlacedPiece(startElem.getTemplateKey(), startTemplate, originX, originY, originZ, 0, 0);
        placedPieces.add(startPiece);

        collectPendingJigsaws(startPiece, queue, 0, null);

        int totalPieces = 1;
        int maxPieces = 32;

        while (!queue.isEmpty() && totalPieces < maxPieces) {
            PendingJigsaw pending = queue.poll();
            if (pending.depth >= maxDepth) continue;

            NamespacedKey poolKey = NamespacedKey.fromString(pending.data.pool.contains(":") ? pending.data.pool : "veinstride:" + pending.data.pool);
            TemplatePool pool = TemplatePoolRegistry.getPool(poolKey);
            if (pool == null || pool.getElements().isEmpty()) {
                NamespacedKey fbKey = pool != null ? pool.getFallbackKey() : NamespacedKey.fromString("veinstride:empty");
                pool = TemplatePoolRegistry.getPool(fbKey);
            }
            if (pool == null || pool.getElements().isEmpty()) {
                if (pending.data.pool == null || !pending.data.pool.endsWith("empty")) {
                    sendDebug(world, "§eJigsaw skip: pool=" + pending.data.pool + " target=" + pending.data.target + " reason=no_pool");
                }
                continue;
            }

            List<TemplatePool.PoolElement> candidates = new ArrayList<>(pool.getElements());
            Collections.shuffle(candidates, random);

            boolean attached = false;
            boolean hadMatch = false;
            boolean hadOverlap = false;

            for (TemplatePool.PoolElement candElem : candidates) {
                StructureTemplate candTemplate = StructureRegistry.getTemplate(candElem.getTemplateKey());
                if (candTemplate == null) continue;

                // Find matching jigsaw in candidate piece
                List<JigsawData> candJigsaws = getJigsawsFromTemplate(candTemplate);
                for (JigsawData candJigsaw : candJigsaws) {
                    if (!candJigsaw.name.equals(pending.data.target)) continue;
                    hadMatch = true;

                    int[] rotations = "aligned".equalsIgnoreCase(pending.data.joint) ? new int[]{0} : new int[]{0, 90, 180, 270};

                    for (int rotY : rotations) {
                        Vector3i rotCandJigsawOffset = rotateOffset(candJigsaw.dx, candJigsaw.dy, candJigsaw.dz, candTemplate.getSizeX(), candTemplate.getSizeZ(), rotY);
                        Vector3i facingOffset = getFacingOffset(pending.data.orientation);

                        int candOriginX = pending.worldPos.x + facingOffset.x - rotCandJigsawOffset.x;
                        int candOriginY = pending.worldPos.y + facingOffset.y - rotCandJigsawOffset.y;
                        int candOriginZ = pending.worldPos.z + facingOffset.z - rotCandJigsawOffset.z;

                        Vector3i candJigsawWorldPos = new Vector3i(pending.worldPos.x + facingOffset.x, pending.worldPos.y + facingOffset.y, pending.worldPos.z + facingOffset.z);

                        Vector3i rotSize = getRotatedSize(candTemplate.getSizeX(), candTemplate.getSizeY(), candTemplate.getSizeZ(), rotY);
                        AABB candBox = new AABB(
                                new Vector3f(candOriginX, candOriginY, candOriginZ),
                                new Vector3f(candOriginX + rotSize.x, candOriginY + rotSize.y, candOriginZ + rotSize.z)
                        );

                        if (hasOverlap(candBox, pending.worldPos, candJigsawWorldPos, placedPieces)) {
                            hadOverlap = true;
                            continue;
                        }

                        // Place candidate piece
                        placeTemplateRotated(world, currentChunk, wg, candTemplate, candOriginX, candOriginY, candOriginZ, rotY);

                        // Transform connecting Jigsaw blocks into their turns_into blocks
                        transformJigsawBlock(world, currentChunk, wg, pending.worldPos, pending.data.turnsInto);
                        transformJigsawBlock(world, currentChunk, wg, candJigsawWorldPos, candJigsaw.turnsInto);

                        PlacedPiece newPiece = new PlacedPiece(candElem.getTemplateKey(), candTemplate, candOriginX, candOriginY, candOriginZ, rotY, pending.depth + 1);
                        placedPieces.add(newPiece);
                        totalPieces++;

                        // Ignore the connecting seam Jigsaw of the new piece
                        collectPendingJigsaws(newPiece, queue, pending.depth + 1, candJigsawWorldPos);

                        attached = true;
                        break;
                    }
                    if (attached) break;
                }
                if (attached) break;
            }

            if (!attached) {
                if (pending.data.pool == null || !pending.data.pool.endsWith("empty")) {
                    String reason = hadOverlap ? "overlap" : (hadMatch ? "no_match" : "no_match");
                    sendDebug(world, "§eJigsaw skip: pool=" + pending.data.pool + " target=" + pending.data.target + " reason=" + reason);
                }
            }
        }

        return totalPieces;
    }

    private static void sendDebug(World world, String message) {
        System.out.println(message);
        if (world != null && world.getEventBus() != null) {
            world.getEventBus().publish(new de.delautrer.game.events.ChatMessageEvent(message));
        }
    }

    private static void collectPendingJigsaws(PlacedPiece piece, Queue<PendingJigsaw> queue, int depth, Vector3i ignoreWorldPos) {
        for (StructureTemplate.StructureBlock sb : piece.template.getBlocks()) {
            if (sb.block instanceof JigsawBlock || (sb.nbt != null && sb.nbt.contains("name"))) {
                JigsawData data = new JigsawData(sb.dx, sb.dy, sb.dz, sb.nbt);
                Vector3i rotOffset = rotateOffset(sb.dx, sb.dy, sb.dz, piece.template.getSizeX(), piece.template.getSizeZ(), piece.rotY);
                Vector3i worldPos = new Vector3i(piece.originX + rotOffset.x, piece.originY + rotOffset.y, piece.originZ + rotOffset.z);
                if (ignoreWorldPos != null && worldPos.equals(ignoreWorldPos)) {
                    continue;
                }
                queue.add(new PendingJigsaw(worldPos, data, depth));
            }
        }
    }

    private static List<JigsawData> getJigsawsFromTemplate(StructureTemplate template) {
        List<JigsawData> list = new ArrayList<>();
        for (StructureTemplate.StructureBlock sb : template.getBlocks()) {
            if (sb.block instanceof JigsawBlock || (sb.nbt != null && sb.nbt.contains("name"))) {
                list.add(new JigsawData(sb.dx, sb.dy, sb.dz, sb.nbt));
            }
        }
        return list;
    }

    private static boolean hasOverlap(AABB box, Vector3i pendingPos, Vector3i candJigsawPos, List<PlacedPiece> placedPieces) {
        float inset = 0.001f;
        float bMinX = box.min.x + inset;
        float bMinY = box.min.y + inset;
        float bMinZ = box.min.z + inset;
        float bMaxX = box.max.x - inset;
        float bMaxY = box.max.y - inset;
        float bMaxZ = box.max.z - inset;

        if (bMinX >= bMaxX || bMinY >= bMaxY || bMinZ >= bMaxZ) return false;

        for (PlacedPiece piece : placedPieces) {
            AABB pBox = piece.boundingBox;
            float pMinX = pBox.min.x + inset;
            float pMinY = pBox.min.y + inset;
            float pMinZ = pBox.min.z + inset;
            float pMaxX = pBox.max.x - inset;
            float pMaxY = pBox.max.y - inset;
            float pMaxZ = pBox.max.z - inset;

            if (pMinX >= pMaxX || pMinY >= pMaxY || pMinZ >= pMaxZ) continue;

            if (bMinX < pMaxX && bMaxX > pMinX &&
                bMinY < pMaxY && bMaxY > pMinY &&
                bMinZ < pMaxZ && bMaxZ > pMinZ) {

                int iMinX = Math.max((int) Math.floor(box.min.x), (int) Math.floor(pBox.min.x));
                int iMaxX = Math.min((int) Math.ceil(box.max.x), (int) Math.ceil(pBox.max.x));
                int iMinY = Math.max((int) Math.floor(box.min.y), (int) Math.floor(pBox.min.y));
                int iMaxY = Math.min((int) Math.ceil(box.max.y), (int) Math.ceil(pBox.max.y));
                int iMinZ = Math.max((int) Math.floor(box.min.z), (int) Math.floor(pBox.min.z));
                int iMaxZ = Math.min((int) Math.ceil(box.max.z), (int) Math.ceil(pBox.max.z));

                boolean isSeamOnly = false;
                if ((iMaxX - iMinX <= 1) && (iMaxY - iMinY <= 1) && (iMaxZ - iMinZ <= 1)) {
                    if ((iMinX == pendingPos.x && iMinY == pendingPos.y && iMinZ == pendingPos.z) ||
                        (iMinX == candJigsawPos.x && iMinY == candJigsawPos.y && iMinZ == candJigsawPos.z)) {
                        isSeamOnly = true;
                    }
                }

                if (!isSeamOnly) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Vector3i rotateOffset(int dx, int dy, int dz, int sizeX, int sizeZ, int rotY) {
        switch (rotY) {
            case 90:
                return new Vector3i(sizeZ - 1 - dz, dy, dx);
            case 180:
                return new Vector3i(sizeX - 1 - dx, dy, sizeZ - 1 - dz);
            case 270:
                return new Vector3i(dz, dy, sizeX - 1 - dx);
            case 0:
            default:
                return new Vector3i(dx, dy, dz);
        }
    }

    public static Vector3i getRotatedSize(int sizeX, int sizeY, int sizeZ, int rotY) {
        if (rotY == 90 || rotY == 270) {
            return new Vector3i(sizeZ, sizeY, sizeX);
        }
        return new Vector3i(sizeX, sizeY, sizeZ);
    }

    public static Vector3i getFacingOffset(String orientation) {
        if (orientation == null) return new Vector3i(0, 0, -1);
        switch (orientation.toLowerCase()) {
            case "north": return new Vector3i(0, 0, -1);
            case "south": return new Vector3i(0, 0, 1);
            case "west":  return new Vector3i(-1, 0, 0);
            case "east":  return new Vector3i(1, 0, 0);
            case "up":    return new Vector3i(0, 1, 0);
            case "down":  return new Vector3i(0, -1, 0);
            default:      return new Vector3i(0, 0, -1);
        }
    }

    private static void placeTemplateRotated(World world, Chunk currentChunk, WorldGenerator wg, StructureTemplate template, int originX, int originY, int originZ, int rotY) {
        for (StructureTemplate.StructureBlock sb : template.getBlocks()) {
            Vector3i rotOffset = rotateOffset(sb.dx, sb.dy, sb.dz, template.getSizeX(), template.getSizeZ(), rotY);
            int wx = originX + rotOffset.x;
            int wy = originY + rotOffset.y;
            int wz = originZ + rotOffset.z;

            setBlock(world, currentChunk, wg, wx, wy, wz, sb.block, sb.state, sb.nbt);
        }
    }

    private static void transformJigsawBlock(World world, Chunk currentChunk, WorldGenerator wg, Vector3i pos, String turnsIntoKeyStr) {
        Block turnsIntoBlock = Registries.BLOCKS.get(turnsIntoKeyStr != null && turnsIntoKeyStr.contains(":") ? turnsIntoKeyStr : "veinstride:" + (turnsIntoKeyStr != null ? turnsIntoKeyStr : "air"));
        if (turnsIntoBlock == null) {
            turnsIntoBlock = Registries.BLOCKS.get("veinstride:air");
        }
        setBlock(world, currentChunk, wg, pos.x, pos.y, pos.z, turnsIntoBlock, (byte) 0, null);
    }

    private static void setBlock(World world, Chunk currentChunk, WorldGenerator wg, int wx, int wy, int wz, Block block, byte state, CompoundTag nbt) {
        if (block != null && block.isStructureVoid()) {
            block = Registries.BLOCKS.get("veinstride:air");
            state = 0;
            nbt = null;
        }
        if (world != null) {
            world.setBlockWithState(wx, wy, wz, block, state, false);
            if (nbt != null) {
                int cx = wx >> 4;
                int cz = wz >> 4;
                Chunk chunk = world.getChunkManager() != null ? world.getChunkManager().getChunk(cx, cz) : null;
                if (chunk != null) {
                    chunk.setBlockEntityTag(wx & 15, wy, wz & 15, nbt);
                }
                world.setBlockEntity(new org.joml.Vector3i(wx, wy, wz), null);
            }
        } else if (currentChunk != null) {
            StructureRegistry.setBlockIfInChunk(currentChunk, wg, wx, wy, wz, block, state, nbt);
        }
    }
}
