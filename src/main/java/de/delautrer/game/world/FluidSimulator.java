package de.delautrer.game.world;

import de.delautrer.engine.graphics.VulkanContext;
import org.joml.Vector2i;
import org.lwjgl.vulkan.VK10;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FluidSimulator {

    private final ChunkManager chunkManager;
    private final VulkanContext context;

    // N, S, O, W
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public FluidSimulator(ChunkManager chunkManager, VulkanContext context) {
        this.chunkManager = chunkManager;
        this.context = context;
    }

    public void tick() {
        if (chunkManager.getLoadedChunks().isEmpty()) return;

        List<int[]> updates = new ArrayList<>();
        Set<Chunk> activeChunks = new HashSet<>();

        // 1. Chunks mit Wasser und ihre direkten Nachbarn aktivieren
        for (Chunk chunk : chunkManager.getLoadedChunks()) {
            boolean hasWater = false;
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                for (int x = 0; x < Chunk.SIZE; x++) {
                    for (int z = 0; z < Chunk.SIZE; z++) {
                        if (chunk.getBlock(x, y, z) == 4) {
                            hasWater = true;
                            break;
                        }
                    }
                    if (hasWater) break;
                }
                if (hasWater) break;
            }

            if (hasWater) {
                activeChunks.add(chunk);
                int cx = chunk.getWorldX();
                int cz = chunk.getWorldZ();
                activeChunks.add(chunkManager.getChunkAtBlock((cx + 1) * Chunk.SIZE, 0, cz * Chunk.SIZE));
                activeChunks.add(chunkManager.getChunkAtBlock((cx - 1) * Chunk.SIZE, 0, cz * Chunk.SIZE));
                activeChunks.add(chunkManager.getChunkAtBlock(cx * Chunk.SIZE, 0, (cz + 1) * Chunk.SIZE));
                activeChunks.add(chunkManager.getChunkAtBlock(cx * Chunk.SIZE, 0, (cz - 1) * Chunk.SIZE));
            }
        }

        activeChunks.remove(null);

        // 2. Zustand evaluieren
        for (Chunk chunk : activeChunks) {
            int cx = chunk.getWorldX() * Chunk.SIZE;
            int cz = chunk.getWorldZ() * Chunk.SIZE;

            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    for (int z = 0; z < Chunk.SIZE; z++) {
                        int gX = cx + x;
                        int gZ = cz + z;

                        byte currentBlock = chunk.getBlock(x, y, z);
                        byte currentState = chunk.getState(x, y, z);

                        if (currentBlock != 0 && currentBlock != 4) continue;

                        byte expected = getExpectedWaterState(gX, y, gZ);

                        if (expected != currentState || (expected > 0 && currentBlock == 0) || (expected == 0 && currentBlock == 4)) {
                            updates.add(new int[]{gX, y, gZ, expected > 0 ? 4 : 0, expected});
                        }
                    }
                }
            }
        }

        // 3. Updates anwenden
        if (!updates.isEmpty()) {
            Set<Chunk> dirtyChunks = new HashSet<>();
            for (int[] u : updates) {
                Chunk c = chunkManager.getChunkAtBlock(u[0], u[1], u[2]);
                if (c != null) {
                    c.setBlock(Math.floorMod(u[0], Chunk.SIZE), u[1], Math.floorMod(u[2], Chunk.SIZE), (byte) u[3], (byte) u[4]);
                    dirtyChunks.add(c);
                    addNeighborsToDirty(dirtyChunks, u[0], u[1], u[2]);
                }
            }

            if (!dirtyChunks.isEmpty()) {
                VK10.vkDeviceWaitIdle(context.getDevice());
                for (Chunk c : dirtyChunks) {
                    if (c != null) {
                        c.rebuildMesh(chunkManager);
                        Vector2i pos = new Vector2i(c.getWorldX(), c.getWorldZ());
                        if (chunkManager.getMeshes().containsKey(pos)) {
                            chunkManager.getMeshes().get(pos).updateMesh(c);
                        }
                    }
                }
            }
        }
    }

    private void addNeighborsToDirty(Set<Chunk> set, int x, int y, int z) {
        int lx = Math.floorMod(x, Chunk.SIZE);
        int lz = Math.floorMod(z, Chunk.SIZE);
        if (lx == 0) set.add(chunkManager.getChunkAtBlock(x - 1, y, z));
        if (lx == Chunk.SIZE - 1) set.add(chunkManager.getChunkAtBlock(x + 1, y, z));
        if (lz == 0) set.add(chunkManager.getChunkAtBlock(x, y, z - 1));
        if (lz == Chunk.SIZE - 1) set.add(chunkManager.getChunkAtBlock(x, y, z + 1));
    }

    // --- SICHERE HILFSMETHODEN ---

    private byte getBlock(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) return 0;
        Chunk c = chunkManager.getChunkAtBlock(x, y, z);
        if (c == null) return 0;
        return c.getBlock(Math.floorMod(x, Chunk.SIZE), y, Math.floorMod(z, Chunk.SIZE));
    }

    private byte getState(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) return 0;
        Chunk c = chunkManager.getChunkAtBlock(x, y, z);
        if (c == null) return 0;
        return c.getState(Math.floorMod(x, Chunk.SIZE), y, Math.floorMod(z, Chunk.SIZE));
    }

    private boolean isSolid(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) return true;
        Chunk c = chunkManager.getChunkAtBlock(x, y, z);
        if (c == null) return true;

        byte b = c.getBlock(Math.floorMod(x, Chunk.SIZE), y, Math.floorMod(z, Chunk.SIZE));
        return b != 0 && b != 4;
    }

    private int opposite(int dir) {
        if (dir == 0) return 1;
        if (dir == 1) return 0;
        if (dir == 2) return 3;
        return 2;
    }

    // --- DER PERFEKTIONIERTE ALGORITHMUS ---

    private byte getExpectedWaterState(int x, int y, int z) {
        byte currentB = getBlock(x, y, z);
        byte currentS = getState(x, y, z);

        // 1. Eine gesetzte Quelle bleibt immer eine Quelle
        if (currentB == 4 && currentS == 8) {
            return 8;
        }

        // 2. Fallendes Wasser
        if (getBlock(x, y + 1, z) == 4) {
            return 7;
        }

        // 3. Die "Unendliche Wasserquelle"
        int sources = 0;
        for (int i = 0; i < 4; i++) {
            int nx = x + DIRS[i][0];
            int nz = z + DIRS[i][1];
            if (getBlock(nx, y, nz) == 4 && getState(nx, y, nz) == 8) {
                sources++;
            }
        }
        if (sources >= 2) {
            byte below = getBlock(x, y - 1, z);
            if (below != 0 && below != 4) {
                return 8;
            }
        }

        // 4. Die Ausbreitungs-Logik
        int maxFlowLevel = 0;

        for (int i = 0; i < 4; i++) {
            int nx = x + DIRS[i][0];
            int nz = z + DIRS[i][1];

            if (getBlock(nx, y, nz) == 4) {
                byte ns = getState(nx, y, nz);

                byte neighborBelow = getBlock(nx, y - 1, nz);
                byte neighborBelowState = getState(nx, y - 1, nz);

                // Ein Nachbar fällt, wenn unter ihm Luft ist.
                boolean neighborIsFalling = (neighborBelow == 0) || (neighborBelow == 4 && neighborBelowState < 8);

                // WICHTIG: Wasser darf sich horizontal NUR ausbreiten, wenn der Nachbar
                // auf einem festen Block liegt. Fallendes Wasser (oder Quellen in der Luft)
                // breiten sich strikt nicht zur Seite aus, sondern fallen nur!
                if (!neighborIsFalling) {
                    if (ns > 1 && canFlowInto(nx, y, nz, opposite(i))) {
                        if (ns > maxFlowLevel) {
                            maxFlowLevel = ns;
                        }
                    }
                }
            }
        }

        // Natürlicher Decay: Keine Hacks mehr. Das Wasser schmilzt langsam Block für Block ab.
        return maxFlowLevel > 1 ? (byte) (maxFlowLevel - 1) : 0;
    }

    // --- DER LOCH-SUCH-ALGORITHMUS ---

    private boolean canFlowInto(int wx, int wy, int wz, int dirToUs) {
        int[] costs = new int[4];
        int minCost = 999;

        for (int i = 0; i < 4; i++) {
            int nx = wx + DIRS[i][0];
            int nz = wz + DIRS[i][1];

            if (isSolid(nx, wy, nz)) {
                costs[i] = 999;
            } else if (!isSolid(nx, wy - 1, nz)) {
                costs[i] = 0;
            } else {
                costs[i] = calculateDropCost(nx, wy, nz, 1, opposite(i));
            }

            if (costs[i] < minCost) {
                minCost = costs[i];
            }
        }

        if (minCost > 4) {
            return !isSolid(wx + DIRS[dirToUs][0], wy, wz + DIRS[dirToUs][1]);
        }

        return costs[dirToUs] == minCost;
    }

    private int calculateDropCost(int x, int y, int z, int distance, int incomingDir) {
        if (distance > 4) return 999;
        if (isSolid(x, y, z)) return 999;

        if (!isSolid(x, y - 1, z)) {
            return distance;
        }

        int minCost = 999;
        for (int i = 0; i < 4; i++) {
            if (i == incomingDir) continue;

            int nx = x + DIRS[i][0];
            int nz = z + DIRS[i][1];

            int cost = calculateDropCost(nx, y, nz, distance + 1, opposite(i));
            if (cost < minCost) minCost = cost;
        }

        return minCost;
    }
}