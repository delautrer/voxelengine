package de.delautrer.game.world;

import de.delautrer.engine.graphics.*;
import de.delautrer.engine.events.EventBus;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.entities.BlockEntity;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.Entity;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.events.BlockChangeEvent;
import de.delautrer.game.events.BlockNeighborUpdateEvent;
import de.delautrer.game.world.persistence.PlayerData;
import de.delautrer.game.world.persistence.WorldData;
import de.delautrer.game.world.sky.CloudSystem;
import de.delautrer.game.world.sky.SkyManager;
import de.delautrer.game.world.sky.Weather;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import de.delautrer.game.world.systems.WorldSystem;
import de.delautrer.game.world.systems.EntitySystem;
import de.delautrer.game.world.systems.TerrainSystem;
import de.delautrer.game.world.systems.WeatherSystem;
import de.delautrer.Constants;

import de.delautrer.game.registry.Registries;

public class World {
    private final EventBus eventBus;
    private final ChunkManager chunkManager;
    private final TickScheduler tickScheduler;
    private final CloudSystem cloudSystem;
    private final WorldStorageManager storageManager;
    private final SkyManager skyManager;

    private Vector3d worldSpawnpoint;
    private final long seed;
    private boolean isCleanedUp = false;
    private final String worldName;
    
    // Metadata
    private long creationDate;
    private long lastOpenedDate;
    private String creationVersion;
    private String lastOpenedVersion;

    @SuppressWarnings("unused")
    private final String worldSave;

    private final List<WorldSystem> systems = new ArrayList<>();
    private final EntitySystem entitySystem;
    private final Map<Vector3i, BlockEntity> blockEntities = new ConcurrentHashMap<>();

    @SuppressWarnings("unused")
    private final IGraphicsFactory graphicsFactory;

    @SuppressWarnings("this-escape")
    public World(IGraphicsFactory graphicsFactory, LocalPlayer localPlayer, EventBus eventBus, long defaultSeed,
            String worldName, String worldSave) {
        this.eventBus = eventBus;
        this.graphicsFactory = graphicsFactory;
        this.worldName = worldName;
        this.worldSave = worldSave;
        this.storageManager = new WorldStorageManager(worldSave);
        this.skyManager = new SkyManager();

        WorldData wData = storageManager.loadLevelMetadata();
        long now = System.currentTimeMillis();
        
        if (wData != null) {
            this.seed = wData.seed;
            this.skyManager.setTimeOfDay(wData.timeOfDay);
            if (wData.worldSpawnpoint != null) {
                this.worldSpawnpoint = new Vector3d(wData.worldSpawnpoint);
            }
            try {
                skyManager.setCurrentWeather(Weather.valueOf(wData.weather));
            } catch (Exception e) {
                skyManager.setCurrentWeather(Weather.PARTLY_CLOUDY);
            }
            
            this.creationDate = wData.creationDate == 0 ? now : wData.creationDate;
            this.creationVersion = wData.creationVersion == null ? Constants.VERSION : wData.creationVersion;
            this.lastOpenedDate = now;
            this.lastOpenedVersion = Constants.VERSION;
            
            // Save immediately to update lastOpened
            saveWorldData();
        } else {
            this.seed = defaultSeed;
            Vector3f sp = WorldInitializer.findSpawnPoint(this.seed);
            if (sp != null)
                this.worldSpawnpoint = new Vector3d(sp);
            skyManager.setCurrentWeather(Weather.PARTLY_CLOUDY);
            skyManager.forceWeather(skyManager.getCurrentWeather());
            
            this.creationDate = now;
            this.creationVersion = Constants.VERSION;
            this.lastOpenedDate = now;
            this.lastOpenedVersion = Constants.VERSION;
            
            saveWorldData();
        }

        // ChunkManager und Threads erst DANACH starten!
        this.chunkManager = new ChunkManager(this, graphicsFactory);
        this.tickScheduler = new TickScheduler(this);
        this.cloudSystem = new CloudSystem();

        this.entitySystem = new EntitySystem();
        this.systems.add(this.entitySystem);
        this.systems.add(new TerrainSystem(this.chunkManager));
        this.systems.add(new WeatherSystem(this.cloudSystem, this.skyManager));
        this.systems.add(new de.delautrer.game.world.systems.BlockTickSystem());

        PlayerData pData = storageManager.loadPlayerData("lokaler-spieler");
        if (pData != null) {
            localPlayer.position.set(pData.x, pData.y, pData.z);
            localPlayer.getCamera().setPitch(pData.pitch);
            localPlayer.getCamera().setYaw(pData.yaw);
            localPlayer.setGameMode(pData.gamemode);
            localPlayer.setDead(pData.isDead);
            localPlayer.setCurrentHealth(pData.currentHealth);
            if (localPlayer.getInventory() != null) {
                localPlayer.getInventory().importFromSavedData(pData.inventory);
                localPlayer.getInventory().setSelectedSlot(pData.selectedHotbarSlot);
            }
        } else {
            if (this.worldSpawnpoint != null) {
                localPlayer.position.set(this.worldSpawnpoint);
            }
            /*
             * int i = 0;
             * for (Item item : ItemRegistry.getAll().values()) {
             * localPlayer.getInventory().setStack(i++, new ItemStack(item, 64));
             * if (i >= PlayerInventory.TOTAL_SIZE) break;
             * }
             */
        }

        storageManager.loadBlockEntities(this);
        storageManager.loadEntities(this);

        chunkManager.update(localPlayer.position.x, localPlayer.position.z);
    }

    public void update(float deltaTime, LocalPlayer localPlayer) {
        for (WorldSystem system : systems) {
            system.update(this, deltaTime, localPlayer);
        }

        tickScheduler.update(deltaTime, localPlayer);

        if (localPlayer.position.y < Chunk.MIN_Y - 50) {
            if (worldSpawnpoint != null) {
                localPlayer.position.set(worldSpawnpoint);
                localPlayer.velocity.set(0);
            } else {
                localPlayer.position.y = Chunk.MIN_Y - 49.0;
                localPlayer.velocity.y = 0.0f;
            }
        }
    }

    public void onTick(LocalPlayer localPlayer) {
        for (WorldSystem system : systems) {
            system.onTick(this, localPlayer);
        }
        for (BlockEntity entity : blockEntities.values()) {
            entity.tick();
        }
    }

    public void scheduleBlockUpdate(int x, int y, int z) {
        byte blockId = getBlockAt(x, y, z);
        if (blockId != 0) {
            tickScheduler.scheduleTick(new Vector3i(x, y, z), BlockRegistry.get(blockId), 1);
        }
    }

    public void setBlock(int x, int y, int z, byte newBlockId) {
        setBlock(x, y, z, newBlockId, true);
    }

    public void setBlock(int x, int y, int z, byte newBlockId, boolean playSound) {
        if (y < Chunk.MIN_Y || y >= Chunk.MAX_Y)
            return;
        Chunk targetChunk = chunkManager.getChunkAtBlock(x, y, z);
        if (targetChunk == null)
            return;

        int localX = x & 15;
        int localZ = z & 15;

        byte oldBlockId = targetChunk.getBlock(localX, y, localZ);
        if (oldBlockId == newBlockId)
            return;

        Vector3i pos = new Vector3i(x, y, z);
        byte oldState = targetChunk.getState(localX, y, localZ);
        targetChunk.setBlock(localX, y, localZ, newBlockId);

        // Altes BlockEntity entfernen (Items droppen)
        BlockEntity oldEntity = getBlockEntity(pos);
        if (oldEntity != null && oldBlockId != newBlockId) {
            oldEntity.onRemove();
            setBlockEntity(pos, null);
        }

        if (oldBlockId != 0 && oldBlockId != newBlockId) {
            Block oldBlock = BlockRegistry.get(oldBlockId);
            oldBlock.onBlockRemoved(this, pos, oldBlock.getStateForId(oldState));
            // NEU: Break Sound abspielen (Leiser)
            if (playSound) {
                de.delautrer.engine.audio.SoundManager.playEvent(oldBlock.getSoundMaterialName(), "jump_land", 0.35f, 0.6f, 1.2f, x + 0.5f, y + 0.5f, z + 0.5f);
            }
        }

        Block oldBlock = BlockRegistry.get(oldBlockId);
        int oldLightEmission = oldBlock.getLightEmission(oldBlock.getStateForId(oldState));
        Block newBlock = BlockRegistry.get(newBlockId);
        int newLightEmission = newBlock.getLightEmission(newBlock.getDefaultState());

        if (oldLightEmission > 0) {
            chunkManager.getLightEngine().removeBlockLight(x, y, z, oldLightEmission);
        }

        targetChunk.recalculateSunlightColumn(localX, localZ, chunkManager.getLightEngine());
        chunkManager.getLightEngine().notifyBlockChanged(x, y, z);

        if (newLightEmission > 0) {
            chunkManager.getLightEngine().addBlockLightSource(x, y, z, newLightEmission);
        }

        chunkManager.getLightEngine().processLightUpdates();

        eventBus.publish(new BlockChangeEvent(pos, oldBlockId, newBlockId, targetChunk));

        int[][] dirs = { { 0, 1, 0 }, { 0, -1, 0 }, { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
        for (int[] dir : dirs) {
            Vector3i nPos = new Vector3i(x + dir[0], y + dir[1], z + dir[2]);
            eventBus.publish(new BlockNeighborUpdateEvent(nPos, pos, newBlockId));
        }

        Block placedBlock = BlockRegistry.get(newBlockId);
        if (placedBlock != null) {
            if (placedBlock.hasBlockEntity() && (oldBlockId != newBlockId || getBlockEntity(pos) == null)) {
                setBlockEntity(pos, placedBlock.createBlockEntity(this, pos));
            }

            byte blockBelow = getBlockAt(x, y - 1, z);
            placedBlock.onNeighborChanged(this, x, y, z, new Vector3i(x, y - 1, z), blockBelow);
        }
    }

    public void setBlock(Vector3i pos, byte newBlockId) {
        if (pos != null)
            setBlock(pos.x, pos.y, pos.z, newBlockId);
    }

    public void setBlockWithState(int x, int y, int z, byte newBlockId, byte newState) {
        setBlockWithState(x, y, z, newBlockId, newState, true);
    }

    public void setBlockWithState(int x, int y, int z, byte newBlockId, byte newState, boolean playSound) {
        if (y < Chunk.MIN_Y || y >= Chunk.MAX_Y)
            return;
        Chunk targetChunk = chunkManager.getChunkAtBlock(x, y, z);
        if (targetChunk == null)
            return;

        int localX = x & 15;
        int localZ = z & 15;

        byte oldBlockId = targetChunk.getBlock(localX, y, localZ);
        byte oldState = targetChunk.getState(localX, y, localZ);

        if (oldBlockId == newBlockId && oldState == newState)
            return;

        Vector3i pos = new Vector3i(x, y, z);
        targetChunk.setBlock(localX, y, localZ, newBlockId, newState);

        // Altes BlockEntity entfernen
        BlockEntity oldEntity = getBlockEntity(pos);
        if (oldEntity != null && oldBlockId != newBlockId) {
            oldEntity.onRemove();
            setBlockEntity(pos, null);
        }

        if (oldBlockId != 0 && oldBlockId != newBlockId) {
            Block oldBlock = BlockRegistry.get(oldBlockId);
            oldBlock.onBlockRemoved(this, pos, oldBlock.getStateForId(oldState));
            // NEU: Break Sound abspielen (Leiser)
            if (playSound) {
                de.delautrer.engine.audio.SoundManager.playEvent(oldBlock.getSoundMaterialName(), "jump_land", 0.35f, 0.6f, 1.2f, x + 0.5f, y + 0.5f, z + 0.5f);
            }
        }

        Block oldBlock = BlockRegistry.get(oldBlockId);
        int oldLightEmission = oldBlock.getLightEmission(oldBlock.getStateForId(oldState));
        Block newBlock = BlockRegistry.get(newBlockId);
        int newLightEmission = newBlock.getLightEmission(newBlock.getStateForId(newState));

        if (oldLightEmission > 0) {
            chunkManager.getLightEngine().removeBlockLight(x, y, z, oldLightEmission);
        }

        // Licht-Updates
        targetChunk.recalculateSunlightColumn(localX, localZ, chunkManager.getLightEngine());
        chunkManager.getLightEngine().notifyBlockChanged(x, y, z);

        if (newLightEmission > 0) {
            chunkManager.getLightEngine().addBlockLightSource(x, y, z, newLightEmission);
        }

        chunkManager.getLightEngine().processLightUpdates();

        // Events
        eventBus.publish(new BlockChangeEvent(pos, oldBlockId, newBlockId, targetChunk));

        int[][] dirs = { { 0, 1, 0 }, { 0, -1, 0 }, { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
        for (int[] dir : dirs) {
            Vector3i nPos = new Vector3i(x + dir[0], y + dir[1], z + dir[2]);
            eventBus.publish(new BlockNeighborUpdateEvent(nPos, pos, newBlockId));
        }

        Block placedBlock = BlockRegistry.get(newBlockId);
        if (placedBlock != null) {
            if (placedBlock.hasBlockEntity() && (oldBlockId != newBlockId || getBlockEntity(pos) == null)) {
                setBlockEntity(pos, placedBlock.createBlockEntity(this, pos));
            }

            byte blockBelow = getBlockAt(x, y - 1, z);
            placedBlock.onNeighborChanged(this, x, y, z, new Vector3i(x, y - 1, z), blockBelow);
        }
    }

    public void setBlockState(int x, int y, int z, BlockState state) {
        setBlockWithState(x, y, z, state.getBlock().getId(), state.getStateId());
    }

    public BlockState getBlockState(int x, int y, int z) {
        Chunk chunk = chunkManager.getChunkAtBlock(x, y, z);
        if (chunk == null)
            return Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "air").getDefaultState();
        return chunk.getBlockState(x & 15, y, z & 15);
    }

    public BlockState getBlockState(Vector3i pos) {
        return getBlockState(pos.x, pos.y, pos.z);
    }

    public byte getBlockAt(int x, int y, int z) {
        Chunk c = chunkManager.getChunkAtBlock(x, y, z);
        if (c == null)
            return 0;
        return c.getBlock(x & 15, y, z & 15);
    }

    public byte getBlockAt(Vector3i pos) {
        return getBlockAt(pos.x, pos.y, pos.z);
    }

    public final void saveWorldData() {
        WorldData wData = new WorldData();
        wData.worldName = worldName;
        wData.seed = this.seed;
        wData.timeOfDay = skyManager.getTimeOfDay();
        if (worldSpawnpoint != null) {
            wData.worldSpawnpoint = new Vector3f((float) worldSpawnpoint.x, (float) worldSpawnpoint.y,
                    (float) worldSpawnpoint.z);
        }
        wData.weather = skyManager.getCurrentWeather().name();
        
        wData.creationDate = this.creationDate;
        wData.creationVersion = this.creationVersion;
        wData.lastOpenedDate = this.lastOpenedDate;
        wData.lastOpenedVersion = this.lastOpenedVersion;
        wData.lastSavedDate = System.currentTimeMillis();
        
        storageManager.saveLevelMetadata(wData);
    }

    public void saveWorld(LocalPlayer localPlayer) {
        saveWorldData();

        PlayerData pData = new PlayerData();
        pData.x = (float) localPlayer.position.x;
        pData.y = (float) localPlayer.position.y;
        pData.z = (float) localPlayer.position.z;
        pData.yaw = localPlayer.getCamera().getYaw();
        pData.pitch = localPlayer.getCamera().getPitch();
        pData.selectedHotbarSlot = localPlayer.getInventory().getSelectedSlot();
        pData.inventory = localPlayer.getInventory().exportToSavedData();
        pData.gamemode = localPlayer.getGameMode();
        pData.currentHealth = localPlayer.getCurrentHealth();
        pData.isDead = localPlayer.isDead();

        storageManager.savePlayerData("lokaler-spieler", pData);

        storageManager.saveBlockEntities(this.blockEntities);
        storageManager.saveEntities(entitySystem.getEntities());

        for (Chunk c : chunkManager.getLoadedChunks()) {
            if (c.isDirty())
                storageManager.queueChunkForSaving(c);
        }
    }

    public Vector3f findSafeSpawn(int x, int z) {
        if (chunkManager.getChunkAtBlock(x, 0, z) == null) {
            return null;
        }

        for (int y = Chunk.MAX_Y - 3; y > Chunk.MIN_Y; y--) {
            if (getBlockAt(x, y, z) != 0) {
                if (getBlockAt(x, y + 1, z) == 0 && getBlockAt(x, y + 2, z) == 0) {
                    return new Vector3f(x + 0.5f, y + 1.0f, z + 0.5f);
                }
            }
        }
        return new Vector3f(x + 0.5f, 30, z + 0.5f);
    }

    public void calcWorldspawnAndTeleportPlayer(LocalPlayer player) {
        if (worldSpawnpoint != null) {
            player.position.set(worldSpawnpoint);
        }
    }

    public void spawnEntity(Entity entity) {
        entitySystem.spawnEntity(entity);
    }

    public void removeEntity(Entity entity) {
        entitySystem.removeEntity(entity);
    }

    public List<Entity> getEntities() {
        return entitySystem.getEntities();
    }

    public BlockEntity getBlockEntity(Vector3i pos) {
        return blockEntities.get(pos);
    }

    public void setBlockEntity(Vector3i pos, BlockEntity entity) {
        if (entity == null)
            blockEntities.remove(pos);
        else
            blockEntities.put(pos, entity);
    }

    public long getSeed() {
        return seed;
    }

    public CloudSystem getCloudSystem() {
        return cloudSystem;
    }

    public TickScheduler getTickScheduler() {
        return tickScheduler;
    }

    public ChunkManager getChunkManager() {
        return chunkManager;
    }

    public WorldStorageManager getStorageManager() {
        return storageManager;
    }

    public SkyManager getEnvironment() {
        return skyManager;
    }

    public SkyManager getSkyManager() {
        return skyManager;
    }

    public Vector3d getWorldSpawnpoint() {
        return worldSpawnpoint;
    }

    public String getSafeFolderName() {
        return worldSave;
    }

    public void cleanup(LocalPlayer localPlayer) {
        if (isCleanedUp)
            return;
        isCleanedUp = true;
        saveWorld(localPlayer);
        if (storageManager != null)
            storageManager.shutdown();
        if (chunkManager != null)
            chunkManager.cleanup();
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
