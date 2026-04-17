package de.delautrer.game.world;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.graphics.VulkanContext;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.events.BlockChangeEvent;
import de.delautrer.game.events.BlockNeighborUpdateEvent;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.player.Inventory;
import de.delautrer.game.world.persistence.PlayerData;
import de.delautrer.game.world.persistence.WorldData;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class World {
    private final EventBus eventBus;
    private final ChunkManager chunkManager;
    private final TickScheduler tickScheduler;
    private final CloudSystem cloudSystem;
    private final WorldStorageManager storageManager;
    private final Environment environment;

    private Vector3f worldSpawnpoint;
    private final long seed;
    private boolean isCleanedUp = false;
    private final String worldName;
    private final String worldSave;

    public World(VulkanContext context, LocalPlayer localPlayer, EventBus eventBus, long defaultSeed, String worldName, String worldSave) {
        this.eventBus = eventBus;
        this.worldName = worldName;
        this.worldSave = worldSave;
        this.storageManager = new WorldStorageManager(worldSave);
        this.environment = new Environment();

        WorldData wData = storageManager.loadLevelMetadata();
        if (wData != null) {
            this.seed = wData.seed;
            this.environment.setTimeOfDay(wData.timeOfDay);
            this.worldSpawnpoint = wData.worldSpawnpoint;
        } else {
            this.seed = defaultSeed;

            WorldData saveWData = new WorldData();
            saveWData.worldName = worldName;
            saveWData.seed = this.seed;
            saveWData.timeOfDay = environment.getTimeOfDay();
            storageManager.saveLevelMetadata(saveWData);
        }
        this.chunkManager = new ChunkManager(this, context);

        /*if(worldSpawnpoint == null)
            this.worldSpawnpoint = findSafeSpawn(0, 0);*/

        this.tickScheduler = new TickScheduler(this);
        this.cloudSystem = new CloudSystem();

        PlayerData pData = storageManager.loadPlayerData("lokaler-spieler");
        if (pData != null) {
            localPlayer.position.set(pData.x, pData.y, pData.z);
            localPlayer.getCamera().setPitch(pData.pitch);
            localPlayer.getCamera().setYaw(pData.yaw);
            if (localPlayer.getInventory() != null) {
                localPlayer.getInventory().importFromSavedData(pData.inventory);
                localPlayer.getInventory().setSelectedSlot(pData.selectedHotbarSlot);
            }
        } else {
            //localPlayer.position.set(worldSpawnpoint);
            int i = 0;
            for (de.delautrer.game.items.Item item : ItemRegistry.getAll().values()) {
                localPlayer.getInventory().setStack(i++, new ItemStack(item, 64));
                if (i >= Inventory.TOTAL_SIZE) break;
            }
        }
        chunkManager.update(localPlayer.position.x, localPlayer.position.z);
    }

    public void update(float deltaTime, LocalPlayer localPlayer) {
        chunkManager.update(localPlayer.position.x, localPlayer.position.z);
        tickScheduler.update(deltaTime);
        cloudSystem.update(deltaTime);

        if (localPlayer.position.y < -50) {
            localPlayer.position.set(findSafeSpawn((int)localPlayer.position.x, (int)localPlayer.position.z));
            localPlayer.velocity.set(0);
        }

        chunkManager.getAsyncBuilder().uploadReadyMeshes(chunkManager);
    }

    public void setBlock(int x, int y, int z, byte newBlockId) {
        if (y < 0 || y >= Chunk.HEIGHT) return;
        Chunk targetChunk = chunkManager.getChunkAtBlock(x, y, z);
        if (targetChunk == null) return;

        int localX = Math.floorMod(x, Chunk.SIZE);
        int localZ = Math.floorMod(z, Chunk.SIZE);

        byte oldBlockId = targetChunk.getBlock(localX, y, localZ);
        if (oldBlockId == newBlockId) return;

        targetChunk.setBlock(localX, y, localZ, newBlockId);

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

    public void setBlock(Vector3i pos, byte newBlockId) {
        if (pos != null) setBlock(pos.x, pos.y, pos.z, newBlockId);
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
        if (placedBlock != null) placedBlock.onNeighborChanged(this, x, y, z, new Vector3i(x, y - 1, z), oldBlockId);
    }

    public void setBlockState(int x, int y, int z, BlockState state) {
        setBlockWithState(x, y, z, state.getBlock().getId(), state.getStateId());
    }

    public BlockState getBlockState(int x, int y, int z) {
        Chunk chunk = chunkManager.getChunkAtBlock(x, y, z);
        if (chunk == null) return BlockRegistry.AIR.getDefaultState();
        return chunk.getBlockState(Math.floorMod(x, Chunk.SIZE), y, Math.floorMod(z, Chunk.SIZE));
    }
    public BlockState getBlockState(Vector3i pos) {
        return getBlockState(pos.x, pos.y, pos.z);
    }

    public byte getBlockAt(int x, int y, int z) {
        Chunk c = chunkManager.getChunkAtBlock(x, y, z);
        if (c == null) return 0;
        return c.getBlock(Math.floorMod(x, Chunk.SIZE), y, Math.floorMod(z, Chunk.SIZE));
    }

    public byte getBlockAt(Vector3i pos) { return getBlockAt(pos.x, pos.y, pos.z); }

    public void saveWorld(LocalPlayer localPlayer) {
        WorldData wData = new WorldData();
        wData.worldName = worldName;
        wData.seed = this.seed;
        wData.timeOfDay = environment.getTimeOfDay();
        storageManager.saveLevelMetadata(wData);

        PlayerData pData = new PlayerData();
        pData.x = localPlayer.position.x;
        pData.y = localPlayer.position.y;
        pData.z = localPlayer.position.z;
        pData.yaw = localPlayer.getCamera().getYaw();
        pData.pitch = localPlayer.getCamera().getPitch();
        pData.selectedHotbarSlot = localPlayer.getInventory().getSelectedSlot();
        pData.inventory = localPlayer.getInventory().exportToSavedData();

        storageManager.savePlayerData("lokaler-spieler", pData);

        for (Chunk c : chunkManager.getLoadedChunks()) {
            if (c.isDirty()) storageManager.queueChunkForSaving(c);
        }
    }

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

    public void calcWorldspawnAndTeleportPlayer(LocalPlayer player) {
        if (worldSpawnpoint == null) {
            worldSpawnpoint = findSafeSpawn(0, 0);
            player.position.set(worldSpawnpoint);
        }
    }

    public long getSeed() { return seed; }
    public CloudSystem getCloudSystem() { return cloudSystem; }
    public TickScheduler getTickScheduler() { return tickScheduler; }
    public ChunkManager getChunkManager() { return chunkManager; }
    public WorldStorageManager getStorageManager() { return storageManager; }
    public Environment getEnvironment() { return environment; }

    public void cleanup(LocalPlayer localPlayer) {
        if (isCleanedUp) return;
        isCleanedUp = true;
        saveWorld(localPlayer);
        if (storageManager != null) storageManager.shutdown();
        if (chunkManager != null) chunkManager.cleanup();
    }
}