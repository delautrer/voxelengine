package de.delautrer.game.particle;

import de.delautrer.game.world.World;
import org.joml.Vector3f;
import java.util.Random;

public class ParticleSpawner {
    private static final Random random = new Random();

    public static void spawnBreak(World world, float x, float y, float z, de.delautrer.game.blocks.Block block) {
        de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion reg = block.getModel() != null ? block.getModel().top : null;
        if (reg == null) return;
        
        for (int i = 0; i < 40; i++) {
            float uSize = reg.u1 - reg.u0;
            float vSize = reg.v1 - reg.v0;
            float fragmentU = reg.u0 + random.nextFloat() * uSize * 0.75f;
            float fragmentV = reg.v0 + random.nextFloat() * vSize * 0.75f;
            float fragmentSize = 0.25f; // 1/4 of texture
            
            new ParticleBuilder(world, x + random.nextFloat(), y + random.nextFloat(), z + random.nextFloat())
                .velocity((random.nextFloat() - 0.5f) * 4.0f, (random.nextFloat() - 0.5f) * 4.0f, (random.nextFloat() - 0.5f) * 4.0f)
                .life(0.5f + random.nextFloat() * 0.5f)
                .size(0.06f + random.nextFloat() * 0.04f)
                .texture(fragmentU, fragmentV, fragmentU + uSize * fragmentSize, fragmentV + vSize * fragmentSize, reg.layer)
                .rotation((float)(random.nextFloat() * Math.PI * 2), (random.nextFloat() - 0.5f) * 10.0f)
                .gravity(-20.0f)
                .collision(true, 0.3f)
                .drag(2.0f)
                .spawn();
        }
    }

    public static void spawnBreaking(World world, float x, float y, float z, de.delautrer.game.blocks.Block block) {
        de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion reg = block.getModel() != null ? block.getModel().top : null;
        if (reg == null) return;
        
        for (int i = 0; i < 4; i++) {
            float uSize = reg.u1 - reg.u0;
            float vSize = reg.v1 - reg.v0;
            float fragmentU = reg.u0 + random.nextFloat() * uSize * 0.75f;
            float fragmentV = reg.v0 + random.nextFloat() * vSize * 0.75f;
            float fragmentSize = 0.25f;
            
            new ParticleBuilder(world, x + random.nextFloat(), y + random.nextFloat(), z + random.nextFloat())
                .velocity((random.nextFloat() - 0.5f) * 2.0f, (random.nextFloat() - 0.5f) * 2.0f, (random.nextFloat() - 0.5f) * 2.0f)
                .life(0.3f + random.nextFloat() * 0.2f)
                .size(0.04f + random.nextFloat() * 0.03f)
                .texture(fragmentU, fragmentV, fragmentU + uSize * fragmentSize, fragmentV + vSize * fragmentSize, reg.layer)
                .rotation((float)(random.nextFloat() * Math.PI * 2), (random.nextFloat() - 0.5f) * 10.0f)
                .gravity(-20.0f)
                .collision(true, 0.3f)
                .drag(2.0f)
                .spawn();
        }
    }

    public static void spawnDrop(World world, float x, float y, float z) {
        new ParticleBuilder(world, x, y, z)
            .velocity(0, 0, 0) // Startet bei null
            .life(2.0f)
            .size(0.02f)
            .color(0.2f, 0.4f, 1.0f) // winziges blaues Quadrat
            .gravity(-9.8f) // beschleunigt durch Gravitation
            .collision(true, 0.0f) // stirbt bei Kollision
            .onDeath(() -> spawnSplash(world, x, y - 0.05f, z, new Vector3f(0.2f, 0.4f, 1.0f))) // Triggert Splash
            .spawn();
    }

    public static void spawnSplash(World world, float x, float y, float z, Vector3f color) {
        for (int i = 0; i < 15; i++) {
            new ParticleBuilder(world, x + (random.nextFloat()-0.5f)*0.2f, y, z + (random.nextFloat()-0.5f)*0.2f)
                .velocity((random.nextFloat()-0.5f)*2.0f, random.nextFloat()*2.0f + 1.0f, (random.nextFloat()-0.5f)*2.0f) // Impuls nach oben und außen
                .life(0.2f + random.nextFloat()*0.2f) // Extrem kurze TTL
                .size(0.02f + random.nextFloat()*0.02f)
                .color(color) // blau-weiße bis hellblaue Quadrate
                .gravity(-18.0f) // zieht sie sofort wieder nach unten (Parabel)
                .drag(0.5f)
                .spawn();
        }
    }
    
    public static void spawnLargeSplash(World world, float x, float y, float z, Vector3f color) {
        for (int i = 0; i < 50; i++) {
            new ParticleBuilder(world, x + (random.nextFloat()-0.5f)*0.8f, y, z + (random.nextFloat()-0.5f)*0.8f)
                .velocity((random.nextFloat()-0.5f)*3.0f, random.nextFloat()*4.0f + 1.5f, (random.nextFloat()-0.5f)*3.0f)
                .life(0.3f + random.nextFloat()*0.3f)
                .size(0.03f + random.nextFloat()*0.03f)
                .color(color)
                .gravity(-18.0f)
                .drag(0.5f)
                .spawn();
        }
    }

    public static void spawnSmoke(World world, float x, float y, float z) {
        new ParticleBuilder(world, x + (random.nextFloat()-0.5f)*0.05f, y, z + (random.nextFloat()-0.5f)*0.05f)
            .velocity((random.nextFloat()-0.5f)*0.1f, 0.4f + random.nextFloat()*0.2f, (random.nextFloat()-0.5f)*0.1f) // neg. Schwerkraft / Auftrieb
            .life(1.5f + random.nextFloat()*1.0f)
            .size(0.08f, 0.22f) // dehnen sich aus
            .color(new Vector3f(1.0f, 0.45f, 0.1f), new Vector3f(0.12f, 0.12f, 0.12f)) // Glut-Orange -> Rauch-Schwarz
            .alpha(0.85f, 0.0f) // verblassen fließend
            .gravity(0.0f) // Kein echter Fall, sondern Velocity macht Auftrieb
            .drag(0.5f)
            .spawn();
    }

    public static void spawnFire(World world, float x, float y, float z) {
        Particle p = new ParticleBuilder(world, x + (random.nextFloat()-0.5f)*0.05f, y, z + (random.nextFloat()-0.5f)*0.05f)
            .velocity((random.nextFloat()-0.5f)*0.1f, 0.3f + random.nextFloat()*0.2f, (random.nextFloat()-0.5f)*0.1f) // Drift + Auftrieb
            .life(0.4f + random.nextFloat()*0.2f)
            .size(0.08f, 0.0f) // schrumpfen leicht bevor sie sich auflösen
            .color(new Vector3f(1.0f, 0.9f, 0.0f), new Vector3f(1.0f, 0.3f, 0.0f)) // gelb-orange
            .alpha(1.0f, 0.0f) // verblassen
            .gravity(0.0f) // ignoriert Schwerkraft
            .spawn();
            
        // Fullbright erzwingen (ignoriert dynamische Beleuchtung)
        p.setLight(15.0f, 15.0f);
    }
}
