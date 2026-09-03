package de.delautrer.game.particle;

import de.delautrer.game.entity.Entity;
import de.delautrer.game.world.ChunkManager;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Particle extends Entity {
    public float life;
    public float maxLife;
    
    public float startSize;
    public float endSize;
    public float size;
    
    public Vector3f startColor;
    public Vector3f endColor;
    public Vector3f color;
    
    public float startAlpha;
    public float endAlpha;
    public float alpha;
    
    public boolean collideWithBlocks = false;
    public float bounciness = 0.0f;
    public float drag = 2.0f;
    
    public float rotation = 0.0f;
    public float angularVelocity = 0.0f;
    
    public Runnable onDeath = null;
    
    public boolean hasTexture;
    public Vector4f uvRect; // uMin, vMin, uMax, vMax
    public float textureLayer;

    public Particle(Vector3f spawnPosition, Vector3f velocity, float maxLife, float size, Vector3f color) {
        super(new Vector3d(spawnPosition.x, spawnPosition.y, spawnPosition.z));
        this.velocity.set(velocity);
        this.life = maxLife;
        this.maxLife = maxLife;
        
        this.startSize = size;
        this.endSize = size;
        this.size = size;
        
        this.startColor = new Vector3f(color);
        this.endColor = new Vector3f(color);
        this.color = new Vector3f(color);
        
        this.startAlpha = 1.0f;
        this.endAlpha = 1.0f;
        this.alpha = 1.0f;
        
        this.hasTexture = false;
        this.uvRect = new Vector4f(0, 0, 1, 1);
        this.textureLayer = 0;
        
        // Physics settings: NO GRAVITY by default, NO COLLISION Hitbox
        this.width = 0.0f;
        this.height = 0.0f;
        this.gravity = 0.0f;
        this.skyLightBrightness = 1.0f;
        this.blockLightBrightness = 1.0f;
    }

    public boolean isEmissive = false;

    public Particle setTexture(float uMin, float vMin, float uMax, float vMax, float layer) {
        this.hasTexture = true;
        this.uvRect = new Vector4f(uMin, vMin, uMax, vMax);
        this.textureLayer = layer;
        return this;
    }
    
    public Particle setLight(float skyLight, float blockLight) {
        this.skyLightBrightness = Math.min(1.0f, skyLight > 1.0f ? skyLight / 15.0f : skyLight);
        this.blockLightBrightness = Math.min(1.0f, blockLight > 1.0f ? blockLight / 15.0f : blockLight);
        this.isEmissive = true;
        return this;
    }

    @Override
    public void update(float deltaTime, ChunkManager chunkManager) {
        if (!isEmissive && chunkManager != null) {
            int px = (int) Math.floor(position.x);
            int py = (int) Math.floor(position.y);
            int pz = (int) Math.floor(position.z);
            de.delautrer.game.world.Chunk chunk = chunkManager.getChunkAtBlock(px, py, pz);
            if (chunk != null) {
                int lx = Math.floorMod(px, de.delautrer.game.world.Chunk.SIZE);
                int lz = Math.floorMod(pz, de.delautrer.game.world.Chunk.SIZE);
                int sky = chunk.getSkyLightAt(lx, py, lz, chunkManager);
                int block = chunk.getBlockLightAt(lx, py, lz, chunkManager);
                this.skyLightBrightness = sky / 15.0f;
                this.blockLightBrightness = block / 15.0f;
            }
        }

        life -= deltaTime;
        if (life < 0) {
            life = 0;
            this.setDead(true);
        }

        // Lerp factor
        float t = 1.0f - (life / maxLife);
        
        this.size = startSize + (endSize - startSize) * t;
        this.alpha = startAlpha + (endAlpha - startAlpha) * t;
        this.color.set(startColor).lerp(endColor, t);

        // Simple Gravity & Drag
        velocity.y += this.gravity * deltaTime; // Gravity (gravity is a negative value e.g. -28)
        velocity.x *= Math.max(0, 1.0f - drag * deltaTime); // Drag X
        velocity.z *= Math.max(0, 1.0f - drag * deltaTime); // Drag Z
        
        float dx = velocity.x * deltaTime;
        float dy = velocity.y * deltaTime;
        float dz = velocity.z * deltaTime;
        
        this.rotation += this.angularVelocity * deltaTime;
        
        if (collideWithBlocks && chunkManager.getWorld() != null) {
            // Very simple point collision
            de.delautrer.game.blocks.Block b = chunkManager.getWorld().getBlock((int) Math.floor(position.x + dx), (int) Math.floor(position.y + dy), (int) Math.floor(position.z + dz));
            if (b != null && !b.isAir() && !b.isPassable) {
                if (bounciness > 0.0f) {
                    // Simple bounce
                    velocity.y = -velocity.y * bounciness;
                    velocity.x *= bounciness;
                    velocity.z *= bounciness;
                    dy = velocity.y * deltaTime;
                    dx = velocity.x * deltaTime;
                    dz = velocity.z * deltaTime;
                    
                    this.angularVelocity *= bounciness; // damp rotation on bounce
                    
                    // If it barely moves, stop it
                    if (velocity.lengthSquared() < 0.1f) {
                        velocity.set(0);
                        this.angularVelocity = 0.0f;
                    }
                } else {
                    // No bounce -> die on impact
                    this.setDead(true);
                    if (onDeath != null) onDeath.run();
                }
            }
        }
        
        this.position.add(dx, dy, dz);
    }
    
    @Override
    public void setDead(boolean dead) {
        boolean wasDead = this.isDead();
        super.setDead(dead);
        if (dead && !wasDead && onDeath != null) {
            onDeath.run();
        }
    }
}
