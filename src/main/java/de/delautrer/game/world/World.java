package de.delautrer.game.world;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.engine.graphics.VulkanMesh;
import de.delautrer.engine.input.InputManager;
import de.delautrer.engine.player.Player;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.events.BlockChangeEvent;
import de.delautrer.game.events.BlockNeighborUpdateEvent;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.player.Inventory;
import de.delautrer.game.world.persistence.PlayerData;
import de.delautrer.game.world.persistence.WorldData;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class World {
    private final EventBus eventBus;
    private final ChunkManager chunkManager;
    private final TickScheduler tickScheduler;
    private final Player player;
    private final CloudSystem cloudSystem;
    private final WorldStorageManager storageManager;
    private final Environment environment;

    private final Vector3f worldSpawnpoint;

    private final long seed;

    private float autosaveTimer = 0;
    private final float AUTOSAVE_INTERVAL = 300.0f;
    private boolean isCleanedUp = false;

    public World(VulkanContext context, Player player, EventBus eventBus, long defaultSeed) {
        this.eventBus = eventBus;
        this.player = player;
        this.environment = new Environment();

        // 1. Storage Manager initialisieren
        this.storageManager = new WorldStorageManager("MeineErsteWelt");

        // 2. WELTDATEN LADEN (Seed & Zeit)
        WorldData wData = storageManager.loadLevelMetadata();
        if (wData != null) {
            this.seed = wData.seed;
            this.environment.setTimeOfDay(wData.timeOfDay);
            this.worldSpawnpoint = wData.worldSpawnpoint;
            System.out.println("Welt geladen! Seed: " + this.seed);
        } else {
            this.seed = defaultSeed;
            this.worldSpawnpoint = findSafeSpawn(0, 0);
            System.out.println("Neue Welt erstellt! Seed: " + this.seed);
        }

        this.chunkManager = new ChunkManager(this, context);
        this.tickScheduler = new TickScheduler(this);
        this.cloudSystem = new CloudSystem();

        // 3. SPIELERDATEN LADEN (Position, Rotation & Inventar)
        PlayerData pData = storageManager.loadPlayerData("lokaler-spieler");

        if (pData != null) {
            // Spieler existiert bereits -> Werte überschreiben
            player.position.set(pData.x, pData.y, pData.z);

            // Rotation
            player.getCamera().setPitch(pData.pitch);
            player.getCamera().setYaw(pData.yaw);

            // Inventar wiederherstellen
            if (player.getInventory() != null) {
                player.getInventory().importFromSavedData(pData.inventory);
                player.getInventory().setSelectedSlot(pData.selectedHotbarSlot);
            }
            System.out.println("Spielerdaten erfolgreich geladen!");

        } else {
            // Neuer Spieler -> Sicheren Spawn suchen
            System.out.println("Neuer Spieler - Suche sicheren Spawn...");
            player.position.set(worldSpawnpoint);

            // Hier könntest du dem Spieler auch Starter-Items geben
            int i = 0;
            for (de.delautrer.game.items.Item item : ItemRegistry.getAll().values()) {
                player.getInventory().setStack(i++, new ItemStack(item, 64));
                if (i >= Inventory.TOTAL_SIZE) break;
            }
        }

        // 4. Update anstoßen, damit die Chunks um den (geladenen) Spieler herum generiert werden
        chunkManager.update(player.position.x, player.position.z);
    }

    // Core logic
    public void update(InputManager input, Vector3f cameraFront, boolean isInventoryOpen, float deltaTime) {
        player.update(input, chunkManager, cameraFront, isInventoryOpen, deltaTime);
        chunkManager.update(player.position.x, player.position.z);
        tickScheduler.update(deltaTime);
        cloudSystem.update(deltaTime);

        if (player.position.y < -50) {
            player.position.set(findSafeSpawn((int)player.position.x, (int)player.position.z));
            player.velocity.set(0);
        }

        chunkManager.getAsyncBuilder().uploadReadyMeshes(chunkManager);

        autosaveTimer += deltaTime;
        if (autosaveTimer >= AUTOSAVE_INTERVAL) {
            saveWorld();
            autosaveTimer = 0;
        }
    }

    // Render Logic
    public List<VulkanMesh> getVisibleMeshes(Matrix4f mvp) {
        FrustumIntersection frustum = new FrustumIntersection(mvp);
        List<VulkanMesh> visibleMeshes = new ArrayList<>();

        for (Map.Entry<Vector2i, VulkanMesh> entry : chunkManager.getMeshes().entrySet()) {
            int cx = entry.getKey().x;
            int cz = entry.getKey().y;

            float minX = cx * Chunk.SIZE;
            float minY = 0.0f;
            float minZ = cz * Chunk.SIZE;
            float maxX = minX + Chunk.SIZE;
            float maxY = Chunk.HEIGHT;
            float maxZ = minZ + Chunk.SIZE;

            if (frustum.testAab(minX, minY, minZ, maxX, maxY, maxZ)) {
                visibleMeshes.add(entry.getValue());
            }
        }
        return visibleMeshes;
    }

    // Raycat Logic
    public static class RaycastResult {
        public final Vector3i hitPos;
        public final Vector3i adjacentPos;

        public RaycastResult(Vector3i hitPos, Vector3i adjacentPos) {
            this.hitPos = hitPos;
            this.adjacentPos = adjacentPos;
        }
    }
    public RaycastResult raycast(Vector3f start, Vector3f dir, float maxDistance) {
        int x = (int) Math.floor(start.x);
        int y = (int) Math.floor(start.y);
        int z = (int) Math.floor(start.z);

        int stepX = Float.compare(dir.x, 0.0f);
        int stepY = Float.compare(dir.y, 0.0f);
        int stepZ = Float.compare(dir.z, 0.0f);

        float tDeltaX = stepX != 0 ? Math.abs(1.0f / dir.x) : Float.MAX_VALUE;
        float tDeltaY = stepY != 0 ? Math.abs(1.0f / dir.y) : Float.MAX_VALUE;
        float tDeltaZ = stepZ != 0 ? Math.abs(1.0f / dir.z) : Float.MAX_VALUE;

        float tMaxX = stepX > 0 ? (x + 1.0f - start.x) * tDeltaX : (start.x - x) * tDeltaX;
        float tMaxY = stepY > 0 ? (y + 1.0f - start.y) * tDeltaY : (start.y - y) * tDeltaY;
        float tMaxZ = stepZ > 0 ? (z + 1.0f - start.z) * tDeltaZ : (start.z - z) * tDeltaZ;

        Vector3i lastPos = new Vector3i(x, y, z);
        float dist = 0.0f;

        byte startBlockId = getBlockAt(x, y, z);
        Block startBlock = BlockRegistry.get(startBlockId);
        if (startBlock.isRaycastable) {
            return new RaycastResult(new Vector3i(x, y, z), new Vector3i(x, y, z));
        }

        while (dist <= maxDistance) {
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    lastPos.set(x, y, z);
                    x += stepX;
                    dist = tMaxX;
                    tMaxX += tDeltaX;
                } else {
                    lastPos.set(x, y, z);
                    z += stepZ;
                    dist = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    lastPos.set(x, y, z);
                    y += stepY;
                    dist = tMaxY;
                    tMaxY += tDeltaY;
                } else {
                    lastPos.set(x, y, z);
                    z += stepZ;
                    dist = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            }

            if (dist > maxDistance) break;

            byte blockId = getBlockAt(x, y, z);
            Block block = BlockRegistry.get(blockId);
            if (block.isRaycastable) {
                return new RaycastResult(new Vector3i(x, y, z), new Vector3i(lastPos));
            }
        }
        return null;
    }

    // Blocks
    public void setBlock(int x, int y, int z, byte newBlockId) {
        if (y < 0 || y >= Chunk.HEIGHT) return;

        Chunk targetChunk = chunkManager.getChunkAtBlock(x, y, z);
        if (targetChunk == null) return;

        int localX = Math.floorMod(x, Chunk.SIZE);
        int localZ = Math.floorMod(z, Chunk.SIZE);

        byte oldBlockId = targetChunk.getBlock(localX, y, localZ);
        if (oldBlockId == newBlockId) return;

        // 1. Blockdaten ändern
        targetChunk.setBlock(localX, y, localZ, newBlockId);

        // 2. Zentrales Change-Event feuern (Licht & Renderer lauschen hier!)
        Vector3i pos = new Vector3i(x, y, z);
        eventBus.publish(new BlockChangeEvent(pos, oldBlockId, newBlockId, targetChunk));

        // 3. Neighbor-Updates feuern (Oben, Unten, Nord, Süd, Ost, West)
        int[][] dirs = {{0,1,0}, {0,-1,0}, {1,0,0}, {-1,0,0}, {0,0,1}, {0,0,-1}};
        for (int[] dir : dirs) {
            org.joml.Vector3i nPos = new org.joml.Vector3i(x + dir[0], y + dir[1], z + dir[2]);
            eventBus.publish(new BlockNeighborUpdateEvent(nPos, pos, newBlockId));
        }

        Block placedBlock = BlockRegistry.get(newBlockId);
        if (placedBlock != null) {
            placedBlock.onNeighborChanged(this, x, y, z, new org.joml.Vector3i(x, y - 1, z), oldBlockId);
        }
    }
    public void setBlock(Vector3i pos, byte newBlockId) {
        if (pos != null) {
            setBlock(pos.x, pos.y, pos.z, newBlockId);
        }
    }
    public void setBlockWithState(int x, int y, int z, byte newBlockId, byte newState) {
        if (y < 0 || y >= Chunk.HEIGHT) return;

        Chunk targetChunk = chunkManager.getChunkAtBlock(x, y, z);
        if (targetChunk == null) return;

        int localX = Math.floorMod(x, Chunk.SIZE);
        int localZ = Math.floorMod(z, Chunk.SIZE);

        byte oldBlockId = targetChunk.getBlock(localX, y, localZ);
        byte oldState = targetChunk.getState(localX, y, localZ);

        if (oldBlockId == newBlockId && oldState == newState) return;

        targetChunk.setBlock(localX, y, localZ, newBlockId, newState);

        Vector3i pos = new Vector3i(x, y, z);
        eventBus.publish(new BlockChangeEvent(pos, oldBlockId, newBlockId, targetChunk));

        int[][] dirs = {{0,1,0}, {0,-1,0}, {1,0,0}, {-1,0,0}, {0,0,1}, {0,0,-1}};
        for (int[] dir : dirs) {
            Vector3i nPos = new Vector3i(x + dir[0], y + dir[1], z + dir[2]);
            eventBus.publish(new BlockNeighborUpdateEvent(nPos, pos, newBlockId));
        }

        Block placedBlock = BlockRegistry.get(newBlockId);
        if (placedBlock != null) {
            placedBlock.onNeighborChanged(this, x, y, z, new Vector3i(x, y - 1, z), oldBlockId);
        }
    }
    public void setBlockState(int x, int y, int z, BlockState state) {
        setBlockWithState(x, y, z, state.getBlock().getId(), state.getStateId());
    }
    public BlockState getBlockState(int x, int y, int z) {
        Chunk chunk = chunkManager.getChunkAtBlock(x, y, z);
        if (chunk == null) return BlockRegistry.AIR.getDefaultState();

        int localX = Math.floorMod(x, Chunk.SIZE);
        int localZ = Math.floorMod(z, Chunk.SIZE);
        return chunk.getBlockState(localX, y, localZ);
    }
    public byte getBlockAt(int x, int y, int z) {
        Chunk c = chunkManager.getChunkAtBlock(x, y, z);
        if (c == null) return 0;
        return c.getBlock(Math.floorMod(x, Chunk.SIZE), y, Math.floorMod(z, Chunk.SIZE));
    }
    public byte getBlockAt(Vector3i pos) {
        return getBlockAt(pos.x, pos.y, pos.z);
    }

    // Persistence
    public void saveWorld() {
        System.out.println("Autosave wird ausgeführt...");

        WorldData wData = new WorldData();
        wData.worldName = "MeineErsteWelt";
        wData.seed = this.seed;
        wData.timeOfDay = environment.getTimeOfDay();
        storageManager.saveLevelMetadata(wData);

        PlayerData pData = new PlayerData();
        pData.x = player.position.x;
        pData.y = player.position.y;
        pData.z = player.position.z;
        pData.yaw = player.getCamera().getYaw();
        pData.pitch = player.getCamera().getPitch();

        pData.selectedHotbarSlot = player.getInventory().getSelectedSlot();
        pData.inventory = player.getInventory().exportToSavedData();

        storageManager.savePlayerData("lokaler-spieler", pData);

        // 3. Alle "Dirty" Chunks in die Speicher-Queue werfen
        for (Chunk c : chunkManager.getLoadedChunks()) {
            if (c.isDirty()) {
                storageManager.queueChunkForSaving(c);
            }
        }
    }

    // Helper
    public Vector3f findSafeSpawn(int x, int z) {
        for (int y = Chunk.HEIGHT - 3; y > 0; y--) {
            if (getBlockAt(x, y, z) != 0) {
                if (getBlockAt(x, y + 1, z) == 0 && getBlockAt(x, y + 2, z) == 0) {
                    return new Vector3f(x + 0.5f, y + 1.0f, z + 0.5f);
                }
            }
        }
        return new Vector3f(x, 30, z);
    }

    // Getter & Setter
    public long getSeed() {
        return seed;
    }
    public CloudSystem getCloudSystem() { return cloudSystem; }
    public TickScheduler getTickScheduler() {
        return tickScheduler;
    }
    public ChunkManager getChunkManager() { return chunkManager; }
    public Player getPlayer() { return player; }
    public WorldStorageManager getStorageManager() {
        return storageManager;
    }
    public Environment getEnvironment() {
        return environment;
    }

    // Cleanup
    public void cleanup() {
        if (isCleanedUp) return;
        isCleanedUp = true;

        saveWorld();

        if (storageManager != null) {
            storageManager.shutdown();
        }

        if (chunkManager != null) {
            chunkManager.cleanup();
        }
    }
}