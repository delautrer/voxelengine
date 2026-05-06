package de.delautrer.game.world.persistence;

import de.delautrer.game.world.Biome;
import de.delautrer.game.world.Chunk;
import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ChunkSerializer {

    public static byte[] serialize(Chunk chunk) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos);
             DataOutputStream dos = new DataOutputStream(gzip)) {

            dos.writeInt(chunk.getWorldX());
            dos.writeInt(chunk.getWorldZ());

            dos.write(chunk.getBlocks());
            dos.write(chunk.getStates());
            dos.write(chunk.getLightMap());

            byte[] biomeBytes = new byte[Chunk.SIZE * Chunk.SIZE];
            Biome[] biomeMap = chunk.getBiomeMap();
            for (int i = 0; i < biomeMap.length; i++) {
                biomeBytes[i] = biomeMap[i] != null ? (byte) biomeMap[i].ordinal() : 0;
            }
            dos.write(biomeBytes);
        }
        return baos.toByteArray();
    }

    public static void deserialize(Chunk chunk, byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (GZIPInputStream gzip = new GZIPInputStream(bais);
             DataInputStream dis = new DataInputStream(gzip)) {

            int savedX = dis.readInt();
            int savedZ = dis.readInt();
            if (savedX != chunk.getWorldX() || savedZ != chunk.getWorldZ()) {
                throw new IOException("Chunk-Coordinates are not equal!");
            }

            dis.readFully(chunk.getBlocks());
            dis.readFully(chunk.getStates());
            dis.readFully(chunk.getLightMap());

            Biome[] biomeMap = chunk.getBiomeMap();
            try {
                byte[] biomeBytes = new byte[Chunk.SIZE * Chunk.SIZE];
                dis.readFully(biomeBytes);
                Biome[] biomeValues = Biome.values();
                for (int i = 0; i < biomeBytes.length; i++) {
                    int ordinal = biomeBytes[i] & 0xFF;
                    if (ordinal >= 0 && ordinal < biomeValues.length) {
                        biomeMap[i] = biomeValues[ordinal];
                    } else {
                        biomeMap[i] = Biome.PLAINS;
                    }
                }
            } catch (EOFException e) {
                // Backward compatibility
                for (int i = 0; i < biomeMap.length; i++) {
                    biomeMap[i] = Biome.PLAINS;
                }
            }
        }
        chunk.clearDirty();
    }
}
