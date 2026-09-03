package de.delautrer.game.particle;

import org.joml.Vector3f;
import de.delautrer.game.world.World;

public class ParticleBuilder {
    private World world;
    private Vector3f position;
    private Vector3f velocity = new Vector3f(0, 0, 0);
    private float life = 1.0f;
    
    private float startSize = 0.1f;
    private float endSize = 0.1f;
    
    private Vector3f startColor = new Vector3f(1, 1, 1);
    private Vector3f endColor = new Vector3f(1, 1, 1);
    
    private float startAlpha = 1.0f;
    private float endAlpha = 1.0f;
    
    private float gravity = 0.0f;
    private float drag = 2.0f;
    
    private boolean collideWithBlocks = false;
    private float bounciness = 0.0f;
    
    private boolean hasTexture = false;
    private float uMin = 0, vMin = 0, uMax = 1, vMax = 1;
    private float textureLayer = 0;
    
    private float rotation = 0.0f;
    private float angularVelocity = 0.0f;
    
    private Runnable onDeath = null;
    
    public ParticleBuilder(World world, float x, float y, float z) {
        this.world = world;
        this.position = new Vector3f(x, y, z);
    }
    
    public ParticleBuilder velocity(float vx, float vy, float vz) {
        this.velocity.set(vx, vy, vz);
        return this;
    }
    
    public ParticleBuilder velocity(Vector3f vel) {
        this.velocity.set(vel);
        return this;
    }
    
    public ParticleBuilder rotation(float rotation, float angularVelocity) {
        this.rotation = rotation;
        this.angularVelocity = angularVelocity;
        return this;
    }
    
    public ParticleBuilder onDeath(Runnable onDeath) {
        this.onDeath = onDeath;
        return this;
    }
    
    public ParticleBuilder life(float life) {
        this.life = life;
        return this;
    }
    
    public ParticleBuilder size(float size) {
        this.startSize = size;
        this.endSize = size;
        return this;
    }
    
    public ParticleBuilder size(float startSize, float endSize) {
        this.startSize = startSize;
        this.endSize = endSize;
        return this;
    }
    
    public ParticleBuilder color(float r, float g, float b) {
        this.startColor.set(r, g, b);
        this.endColor.set(r, g, b);
        return this;
    }
    
    public ParticleBuilder color(Vector3f color) {
        if (color == null) {
            this.startColor.set(1, 1, 1);
            this.endColor.set(1, 1, 1);
            return this;
        }
        this.startColor.set(color);
        this.endColor.set(color);
        return this;
    }
    
    public ParticleBuilder color(Vector3f start, Vector3f end) {
        this.startColor.set(start);
        this.endColor.set(end);
        return this;
    }
    
    public ParticleBuilder alpha(float alpha) {
        this.startAlpha = alpha;
        this.endAlpha = alpha;
        return this;
    }
    
    public ParticleBuilder alpha(float startAlpha, float endAlpha) {
        this.startAlpha = startAlpha;
        this.endAlpha = endAlpha;
        return this;
    }
    
    public ParticleBuilder gravity(float gravity) {
        this.gravity = gravity;
        return this;
    }
    
    public ParticleBuilder drag(float drag) {
        this.drag = drag;
        return this;
    }
    
    public ParticleBuilder collision(boolean collide, float bounciness) {
        this.collideWithBlocks = collide;
        this.bounciness = bounciness;
        return this;
    }
    
    public ParticleBuilder texture(float uMin, float vMin, float uMax, float vMax, float layer) {
        this.hasTexture = true;
        this.uMin = uMin;
        this.vMin = vMin;
        this.uMax = uMax;
        this.vMax = vMax;
        this.textureLayer = layer;
        return this;
    }
    
    private boolean isEmissive = false;

    public ParticleBuilder emissive(boolean emissive) {
        this.isEmissive = emissive;
        return this;
    }

    public Particle build() {
        Particle p = new Particle(position, velocity, life, startSize, startColor);
        p.endSize = this.endSize;
        p.startColor.set(this.startColor);
        p.endColor.set(this.endColor);
        p.color.set(this.startColor);
        p.startAlpha = this.startAlpha;
        p.endAlpha = this.endAlpha;
        p.alpha = this.startAlpha;
        p.setGravity(this.gravity);
        p.drag = this.drag;
        p.collideWithBlocks = this.collideWithBlocks;
        p.bounciness = this.bounciness;
        p.isEmissive = this.isEmissive;
        
        p.rotation = this.rotation;
        p.angularVelocity = this.angularVelocity;
        p.onDeath = this.onDeath;
        
        if (this.hasTexture) {
            p.setTexture(uMin, vMin, uMax, vMax, textureLayer);
        }

        if (this.isEmissive) {
            p.setLight(1.0f, 1.0f);
        } else if (world != null && world.getChunkManager() != null) {
            int px = (int) Math.floor(position.x);
            int py = (int) Math.floor(position.y);
            int pz = (int) Math.floor(position.z);
            de.delautrer.game.world.Chunk chunk = world.getChunkManager().getChunkAtBlock(px, py, pz);
            if (chunk != null) {
                int lx = Math.floorMod(px, de.delautrer.game.world.Chunk.SIZE);
                int lz = Math.floorMod(pz, de.delautrer.game.world.Chunk.SIZE);
                int sky = chunk.getSkyLightAt(lx, py, lz, world.getChunkManager());
                int block = chunk.getBlockLightAt(lx, py, lz, world.getChunkManager());
                p.skyLightBrightness = sky / 15.0f;
                p.blockLightBrightness = block / 15.0f;
            }
        }
        
        return p;
    }
    
    public Particle spawn() {
        Particle p = build();
        if (world != null) {
            world.spawnEntity(p);
        }
        return p;
    }
}
