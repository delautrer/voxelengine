package de.delautrer.game.world;

import java.util.Random;

public class NoiseGenerator {

    private final int[] P = new int[512];

    public NoiseGenerator(long seed) {
        Random random = new Random(seed);
        int[] permutation = new int[256];

        for (int i = 0; i < 256; i++) {
            permutation[i] = i;
        }

        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256);
            int swap = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = swap;
        }

        for (int i = 0; i < 256; i++) {
            P[256 + i] = P[i] = permutation[i];
        }
    }

    public float getFractalNoise2D(float x, float z, int octaves, float persistence, float lacunarity) {
        float total = 0;
        float frequency = 1;
        float amplitude = 1;
        float maxValue = 0;

        for (int i = 0; i < octaves; i++) {
            total += getNoise(x * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        return total / maxValue;
    }

    public float getFractalNoise3D(float x, float y, float z, int octaves, float persistence, float lacunarity) {
        float total = 0;
        float frequency = 1;
        float amplitude = 1;
        float maxValue = 0;

        for (int i = 0; i < octaves; i++) {
            total += getNoise(x * frequency, y * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        return total / maxValue;
    }

    public float getNoise(float x, float z) {
        int X = (int)Math.floor(x) & 255;
        int Z = (int)Math.floor(z) & 255;
        x -= (float) Math.floor(x);
        z -= (float) Math.floor(z);
        float u = fade(x), v = fade(z);
        int A = P[X]+Z, AA = P[A], AB = P[A+1];
        int B = P[X+1]+Z, BA = P[B], BB = P[B+1];
        return lerp(v, lerp(u, grad(P[AA], x, z), grad(P[BA], x-1, z)),
                lerp(u, grad(P[AB], x, z-1), grad(P[BB], x-1, z-1)));
    }

    public float getNoise(float x, float y, float z) {
        int X = (int)Math.floor(x) & 255, Y = (int)Math.floor(y) & 255, Z = (int)Math.floor(z) & 255;
        x -= (float) Math.floor(x); y -= (float) Math.floor(y); z -= (float) Math.floor(z);
        float u = fade(x), v = fade(y), w = fade(z);
        int A = P[X]+Y, AA = P[A]+Z, AB = P[A+1]+Z;
        int B = P[X+1]+Y, BA = P[B]+Z, BB = P[B+1]+Z;
        return lerp(w, lerp(v, lerp(u, grad(P[AA], x, y, z), grad(P[BA], x-1, y, z)),
                        lerp(u, grad(P[AB], x, y-1, z), grad(P[BB], x-1, y-1, z))),
                lerp(v, lerp(u, grad(P[AA+1], x, y, z-1), grad(P[BA+1], x-1, y, z-1)),
                        lerp(u, grad(P[AB+1], x, y-1, z-1), grad(P[BB+1], x-1, y-1, z-1))));
    }

    private float fade(float t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private float lerp(float t, float a, float b) { return a + t * (b - a); }
    private float grad(int hash, float x, float z) {
        int h = hash & 15; float u = h < 8 ? x : z; float v = h < 4 ? z : h == 12 || h == 14 ? x : 0;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
    private float grad(int hash, float x, float y, float z) {
        int h = hash & 15; float u = h < 8 ? x : y; float v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}