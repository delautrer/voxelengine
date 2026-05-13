package de.delautrer.game.world.persistence;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkSection;
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

            ChunkSection[] sections = chunk.getSections();
            for (int i = 0; i < sections.length; i++) {
                if (sections[i] != null && !sections[i].isAir()) {
                    dos.writeBoolean(true);
                    dos.write(sections[i].getBlocks());
                    dos.write(sections[i].getStates());
                    dos.write(sections[i].getLightMap());
                } else {
                    dos.writeBoolean(false);
                }
            }

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

            ChunkSection[] sections = chunk.getSections();
            for (int i = 0; i < sections.length; i++) {
                boolean hasData = dis.readBoolean();
                if (hasData) {
                    sections[i] = new ChunkSection();
                    dis.readFully(sections[i].getBlocks());
                    dis.readFully(sections[i].getStates());
                    dis.readFully(sections[i].getLightMap());
                    sections[i].recalculateAir();
                } else {
                    sections[i] = null;
                }
            }

            try {
                byte[] biomeBytes = new byte[Chunk.SIZE * Chunk.SIZE];
                dis.readFully(biomeBytes);
            } catch (EOFException e) {}
        }
        chunk.clearDirty();
    }
}