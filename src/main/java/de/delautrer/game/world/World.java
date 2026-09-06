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
import de.delautrer.game.world.persistence.WorldPalette;
import de.delautrer.game.world.persistence.BiomePalette;
import de.delautrer.game.world.sky.CloudSystem;
import de.delautrer.game.world.sky.SkyManager;
import de.delautrer.game.world.sky.Weather;
import de.delautrer.game.particle.ParticleManager;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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
    private final ParticleManager particleManager;

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
    
    private String generatorType = "DEFAULT";
    private String generatorOptions = "";

    private final List<WorldSystem> systems = new ArrayList<>();
    private final EntitySystem entitySystem;
    private final Map<Vector3i, BlockEntity> blockEntities = new ConcurrentHashMap<>();

    @SuppressWarnings("unused")
    private final IGraphicsFactory graphicsFactory;

    private boolean allowCheats = false;

    @SuppressWarnings("this-escape")
    private final WorldPalette blockPalette;
    private final BiomePalette biomePalette;

    @SuppressWarnings("this-escape")
    public World(IGraphicsFactory graphicsFactory, LocalPlayer localPlayer, EventBus eventBus, long defaultSeed,
            String worldName, String worldSave, String generatorType, String generatorOptions, de.delautrer.game.entity.player.GameMode initialGameMode, boolean allowCheats) {
        this.eventBus = eventBus;
        this.graphicsFactory = graphicsFactory;
        this.worldName = worldName;
        this.worldSave = worldSave;
        this.storageManager = new WorldStorageManager(worldSave, this);
        this.skyManager = new SkyManager();
        this.particleManager = new ParticleManager();

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
            
            this.generatorType = wData.generatorType != null ? wData.generatorType : "DEFAULT";
            this.generatorOptions = wData.generatorOptions != null ? wData.generatorOptions : "";
            this.allowCheats = wData.allowCheats;
            
            if (wData.blockPalette != null) {
                this.blockPalette = WorldPalette.fromKeyList(wData.blockPalette);
            } else {
                this.blockPalette = WorldPalette.createFreshFromRegistry();
            }
            if (wData.biomePalette != null) {
                this.biomePalette = BiomePalette.fromKeyList(wData.biomePalette);
            } else {
                this.biomePalette = BiomePalette.createFreshFromRegistry();
            }

            saveWorldData();
        } else {
            this.seed = defaultSeed;
            Vector3f sp = WorldInitializer.findSpawnPoint(this.seed, generatorType, generatorOptions);
            if (sp != null)
                this.worldSpawnpoint = new Vector3d(sp);
            skyManager.setCurrentWeather(Weather.PARTLY_CLOUDY);
            skyManager.forceWeather(skyManager.getCurrentWeather());
            
            this.creationDate = now;
            this.creationVersion = Constants.VERSION;
            this.lastOpenedDate = now;
            this.lastOpenedVersion = Constants.VERSION;
            
            this.generatorType = generatorType;
            this.generatorOptions = generatorOptions;
            this.allowCheats = allowCheats;
            
            this.blockPalette = WorldPalette.createFreshFromRegistry();
            this.biomePalette = BiomePalette.createFreshFromRegistry();

            saveWorldData();
        }

        this.chunkManager = new ChunkManager(this, graphicsFactory);
        this.tickScheduler = new TickScheduler(this);
        if (wData != null && wData.currentTick > 0) {
            this.tickScheduler.setCurrentTick(wData.currentTick);
        }
        this.cloudSystem = new CloudSystem();

        this.entitySystem = new EntitySystem();
        this.systems.add(this.entitySystem);
        this.systems.add(new TerrainSystem(this.chunkManager));
        this.systems.add(new WeatherSystem(this.cloudSystem, this.skyManager));
        this.systems.add(new de.delautrer.game.world.systems.BlockTickSystem());

        PlayerData pData = storageManager.loadPlayerData("lokaler-spieler");
        if (pData != null && localPlayer != null) {
            localPlayer.position.set(pData.x, pData.y, pData.z);
            localPlayer.getCamera().setPitch(pData.pitch);
            localPlayer.getCamera().setYaw(pData.yaw);
            localPlayer.setGameMode(pData.gamemode != null ? pData.gamemode : de.delautrer.game.entity.player.GameMode.SURVIVAL);
            localPlayer.setDead(pData.isDead);
            localPlayer.setCurrentHealth(pData.currentHealth);
            if (pData.inventory != null) {
                localPlayer.getInventory().importFromSavedData(pData.inventory);
                localPlayer.getInventory().setSelectedSlot(pData.selectedHotbarSlot);
            }
        } else if (localPlayer != null) {
            localPlayer.setGameMode(initialGameMode);
            if (this.worldSpawnpoint != null) {
                localPlayer.position.set(this.worldSpawnpoint);
            }
        }
        
        /*
         * int i = 0;
         * for (Item item : ItemRegistry.getAll().values()) {
         * localPlayer.getInventory().setStack(i++, new ItemStack(item, 64));
         * if (i >= PlayerInventory.TOTAL_SIZE) break;
         * }
         */

        storageManager.loadBlockEntities(this);
        storageManager.loadEntities(this);

        if (localPlayer != null) {
            chunkManager.update(localPlayer.position.x, localPlayer.position.z);
        }
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
            }
        }
    }

    public List<Vector3i> getStructureVoidsNear(double px, double py, double pz, int radius) {
        List<Vector3i> result = new ArrayList<>();
        if (chunkManager == null) return result;

        int minChunkX = (int) Math.floor((px - radius) / 16.0);
        int maxChunkX = (int) Math.floor((px + radius) / 16.0);
        int minChunkZ = (int) Math.floor((pz - radius) / 16.0);
        int maxChunkZ = (int) Math.floor((pz + radius) / 16.0);

        int minX = (int) Math.floor(px - radius);
        int maxX = (int) Math.floor(px + radius);
        int minY = (int) Math.floor(py - radius);
        int maxY = (int) Math.floor(py + radius);
        int minZ = (int) Math.floor(pz - radius);
        int maxZ = (int) Math.floor(pz + radius);

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                Chunk chunk = chunkManager.getChunk(cx, cz);
                if (chunk != null) {
                    for (Vector3i pos : chunk.getStructureVoidPositions()) {
                        if (pos.x >= minX && pos.x <= maxX &&
                            pos.y >= minY && pos.y <= maxY &&
                            pos.z >= minZ && pos.z <= maxZ) {
                            result.add(pos);
                        }
                    }
                }
            }
        }
        return result;
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
        Block block = getBlock(x, y, z);
        if (!block.isAir()) {
            tickScheduler.scheduleTick(new Vector3i(x, y, z), block, 1);
        }
    }

    public void setBlock(Vector3i pos, Block block) {
        if (pos != null)
            setBlock(pos.x, pos.y, pos.z, block);
    }

    public void setBlockWithState(int x, int y, int z, Block newBlock, byte newState, boolean playSound) {
        setBlockWithState(x, y, z, newBlock, newState, playSound, true);
    }

    public void setBlockWithState(int x, int y, int z, Block newBlock, byte newState, boolean playSound, boolean notifyNeighbors) {
        if (y < Chunk.MIN_Y || y >= Chunk.MAX_Y || newBlock == null)
            return;
        Chunk targetChunk = chunkManager.getChunkAtBlock(x, y, z);
        if (targetChunk == null)
            return;

        int localX = x & 15;
        int localZ = z & 15;

        Block oldBlock = targetChunk.getBlock(localX, y, localZ, blockPalette);
        byte oldState = targetChunk.getState(localX, y, localZ);

        if (oldBlock == newBlock && oldState == newState)
            return;

        Vector3i pos = new Vector3i(x, y, z);
        targetChunk.setBlock(localX, y, localZ, newBlock, newState, blockPalette);

        // Altes BlockEntity entfernen (Items droppen)
        BlockEntity oldEntity = getBlockEntity(pos);
        if (oldEntity != null && oldBlock != newBlock) {
            oldEntity.onRemove();
            setBlockEntity(pos, null);
        }

        if (oldBlock != null && !oldBlock.isAir() && oldBlock != newBlock) {
            oldBlock.onBlockRemoved(this, pos, oldBlock.getStateForId(oldState));
            // Break Sound & Partikel abspielen (Leiser), aber NICHT für Wasser!
            if (playSound && !(oldBlock instanceof de.delautrer.game.blocks.WaterBlock) && !(newBlock instanceof de.delautrer.game.blocks.WaterBlock)) {
                de.delautrer.engine.audio.SoundManager.playEvent(oldBlock.getSoundMaterialName(), "jump_land", 0.35f, 0.6f, 1.2f, x + 0.5f, y + 0.5f, z + 0.5f);
                
                // PARTIKEL SPAWNEN
                de.delautrer.game.particle.ParticleSpawner.spawnBreak(this, x, y, z, oldBlock);
            }
        }

        int oldLightEmission = (oldBlock != null) ? oldBlock.getLightEmission(oldBlock.getStateForId(oldState)) : 0;
        int newLightEmission = newBlock.getLightEmission(newBlock.getStateForId(newState));

        if (oldLightEmission > 0) {
            chunkManager.getLightEngine().removeBlockLight(x, y, z, oldLightEmission);
        }

        targetChunk.recalculateSunlightColumn(localX, localZ, chunkManager.getLightEngine());
        chunkManager.getLightEngine().notifyBlockChanged(x, y, z);

        if (newLightEmission > 0) {
            chunkManager.getLightEngine().addBlockLightSource(x, y, z, newLightEmission);
        }

        if (eventBus != null) {
            eventBus.publish(new BlockChangeEvent(pos, oldBlock, newBlock, targetChunk, playSound));

            if (notifyNeighbors) {
                int[][] dirs = { { 0, 1, 0 }, { 0, -1, 0 }, { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
                for (int[] dir : dirs) {
                    Vector3i nPos = new Vector3i(x + dir[0], y + dir[1], z + dir[2]);
                    eventBus.publish(new BlockNeighborUpdateEvent(nPos, pos, newBlock));
                }
            }
        }

        if (newBlock != null) {
            if (newBlock.hasBlockEntity() && (oldBlock != newBlock || getBlockEntity(pos) == null)) {
                setBlockEntity(pos, newBlock.createBlockEntity(this, pos));
            }

            if (notifyNeighbors) {
                Block blockBelow = getBlock(x, y - 1, z);
                newBlock.onNeighborChanged(this, x, y, z, new Vector3i(x, y - 1, z), blockBelow);
            }
        }
    }

    public void setBlockState(int x, int y, int z, BlockState state) {
        if (state != null) {
            setBlockWithState(x, y, z, state.getBlock(), state.getStateId(), true);
        }
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

    public Block getBlock(int x, int y, int z) {
        Chunk chunk = chunkManager.getChunkAtBlock(x, y, z);
        if (chunk == null) return Registries.BLOCKS.get("veinstride:air");
        return chunk.getBlock(x & 15, y, z & 15, blockPalette);
    }

    public Block getBlock(Vector3i pos) {
        return pos != null ? getBlock(pos.x, pos.y, pos.z) : Registries.BLOCKS.get("veinstride:air");
    }

    public void setBlock(int x, int y, int z, Block block) {
        setBlock(x, y, z, block, (byte) 0);
    }

    public void setBlock(int x, int y, int z, Block block, byte state) {
        setBlockWithState(x, y, z, block, state, true);
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
        
        wData.generatorType = this.generatorType;
        wData.generatorOptions = this.generatorOptions;
        wData.allowCheats = this.allowCheats;
        
        if (this.blockPalette != null) wData.blockPalette = this.blockPalette.toKeyList();
        if (this.biomePalette != null) wData.biomePalette = this.biomePalette.toKeyList();
        if (this.tickScheduler != null) wData.currentTick = this.tickScheduler.getCurrentTick();

        storageManager.saveLevelMetadata(wData);
    }

    public WorldPalette getBlockPalette() { return blockPalette; }
    public BiomePalette getBiomePalette() { return biomePalette; }

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
            if (!getBlock(x, y, z).isAir()) {
                if (getBlock(x, y + 1, z).isAir() && getBlock(x, y + 2, z).isAir()) {
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

    public ParticleManager getParticleManager() {
        return particleManager;
    }

    public List<Entity> getEntities() {
        return entitySystem.getEntities();
    }

    public EntitySystem getEntitySystem() {
        return entitySystem;
    }

    public Map<Vector3i, BlockEntity> getBlockEntities() {
        return blockEntities;
    }

    public BlockEntity getBlockEntity(Vector3i pos) {
        if (pos == null) return null;
        BlockEntity be = blockEntities.get(pos);
        if (be == null) {
            de.delautrer.game.blocks.Block block = getBlock(pos.x, pos.y, pos.z);
            if (block != null && block.hasBlockEntity()) {
                be = block.createBlockEntity(this, pos);
                if (be != null) {
                    Chunk chunk = chunkManager != null ? chunkManager.getChunkAtBlock(pos.x, pos.y, pos.z) : null;
                    if (chunk != null) {
                        de.delautrer.game.nbt.CompoundTag tag = chunk.getBlockEntityTag(pos);
                        if (tag != null) {
                            be.readTag(tag);
                            if (be instanceof de.delautrer.game.blocks.entities.ChestBlockEntity chest && tag.contains("LootTable") && isInventoryEmpty(chest.getInventory())) {
                                String lootTablePath = tag.getString("LootTable");
                                if (lootTablePath.startsWith("veinstride:")) {
                                    lootTablePath = lootTablePath.substring("veinstride:".length());
                                }
                                de.delautrer.game.loot.LootTable table = de.delautrer.game.loot.LootTableManager.load(lootTablePath);
                                if (table != null) {
                                    List<de.delautrer.game.items.ItemStack> drops = table.generateLoot();
                                    int invSize = chest.getInventory().getSize();
                                    if (invSize > 0 && !drops.isEmpty()) {
                                        List<Integer> slots = new ArrayList<>(invSize);
                                        for (int s = 0; s < invSize; s++) {
                                            slots.add(s);
                                        }
                                        long posHash = (long) pos.x * 3121L ^ (long) pos.y * 4567L ^ (long) pos.z * 8901L;
                                        Random lootRand = new Random(seed ^ posHash);
                                        Collections.shuffle(slots, lootRand);

                                        for (int i = 0; i < drops.size() && i < slots.size(); i++) {
                                            chest.getInventory().setStack(slots.get(i), drops.get(i));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    blockEntities.put(pos, be);
                }
            }
        }
        return be;
    }

    private boolean isInventoryEmpty(de.delautrer.game.inventory.IInventory inv) {
        if (inv == null) return true;
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getStack(i) != null) return false;
        }
        return true;
    }

    public void setBlockEntity(Vector3i pos, BlockEntity entity) {
        if (entity == null)
            blockEntities.remove(pos);
        else
            blockEntities.put(pos, entity);
    }

    public float getTimeOfDay() {
        return skyManager.getTimeOfDay();
    }
    
    public String getGeneratorType() {
        return generatorType;
    }
    
    public String getGeneratorOptions() {
        return generatorOptions;
    }

    public boolean isCheatsAllowed() {
        return allowCheats;
    }

    public void setCheatsAllowed(boolean allowCheats) {
        this.allowCheats = allowCheats;
    }

    public Vector3d findSafeSpawn(Vector3d preferred) {
        if ("FLAT".equalsIgnoreCase(generatorType)) {
            // Flat world is perfectly flat and always safe at its designated spawn point
            return new Vector3d(preferred);
        }

        int px = (int) Math.floor(preferred.x);
        int pz = (int) Math.floor(preferred.z);
        Block waterBlock = Registries.BLOCKS.get(Constants.NAMESPACE + ":water");

        // Search spiral radius of 5 chunks around the preferred spawn
        int searchRadius = 80; // blocks
        for (int r = 0; r <= searchRadius; r += 2) {
            for (int x = px - r; x <= px + r; x++) {
                for (int z = pz - r; z <= pz + r; z++) {
                    if (Math.abs(x - px) != r && Math.abs(z - pz) != r) continue;

                    Chunk c = chunkManager.getChunkAtBlock(x, 0, z);
                    if (c == null) continue; // Skip unloaded chunks

                    for (int y = Chunk.MAX_Y - 2; y > Chunk.MIN_Y; y--) {
                        Block b = c.getBlock(x & 15, y, z & 15, blockPalette);
                        if (b != null && !b.isAir()) {
                            if (b != waterBlock) {
                                if (b.isSolid && !b.isTransparent && !b.isPassable) {
                                    return new Vector3d(x + 0.5, y + 1.5, z + 0.5);
                                }
                            }
                            break; // Column is not safe (water or not solid), move to next column
                        }
                    }
                }
            }
        }
        return new Vector3d(preferred); // Fallback to original
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
