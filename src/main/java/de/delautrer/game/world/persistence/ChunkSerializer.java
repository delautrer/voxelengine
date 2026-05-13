package de.delautrer.game.world.persistence;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.generation.biome.Biome;
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

            // Biome werden nicht mehr gespeichert (spart Platz!).
            // Wir schreiben Nullen, um das Dateiformat für alte Versionen beizubehalten.
            byte[] biomeBytes = new byte[Chunk.SIZE * Chunk.SIZE];
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

            try {
                // Wir lesen die Biome-Bytes aus der Datei, damit der Stream nicht kaputt geht.
                // Wir werfen sie danach aber weg, da Biome jetzt dynamisch aus dem Multi-Noise-Seed berechnet werden.
                byte[] biomeBytes = new byte[Chunk.SIZE * Chunk.SIZE];
                dis.readFully(biomeBytes);
            } catch (EOFException e) {
                // Rückwärtskompatibilität, falls die Datei unerwartet endet
            }
        }
        chunk.clearDirty();
    }
}