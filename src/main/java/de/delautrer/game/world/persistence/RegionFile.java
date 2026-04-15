package de.delautrer.game.world.persistence;

import java.io.*;

public class RegionFile {
    private static final int CHUNK_COUNT = 32 * 32;
    private static final int SECTOR_SIZE = 4096;
    private final File file;
    private final int[] offsets = new int[CHUNK_COUNT];

    public RegionFile(File file) {
        this.file = file;
        loadOffsets();
    }

    private void loadOffsets() {
        if (!file.exists() || file.length() < SECTOR_SIZE) return;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            for (int i = 0; i < CHUNK_COUNT; i++) {
                offsets[i] = raf.readInt();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized byte[] readChunk(int cx, int cz) {
        int index = (cx & 31) + (cz & 31) * 32;
        int offsetData = offsets[index];
        if (offsetData == 0) return null;

        int sectorOffset = offsetData >> 8;
        int sectorCount = offsetData & 0xFF;

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek((long) sectorOffset * SECTOR_SIZE);
            int length = raf.readInt();
            byte[] data = new byte[length];
            raf.readFully(data);
            return data;
        } catch (IOException e) {
            return null;
        }
    }

    public synchronized void writeChunk(int cx, int cz, byte[] data) {
        int index = (cx & 31) + (cz & 31) * 32;
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            int sectorOffset = (int) (raf.length() + SECTOR_SIZE - 1) / SECTOR_SIZE;
            if (sectorOffset < 1) sectorOffset = 1;

            raf.seek((long) sectorOffset * SECTOR_SIZE);
            raf.writeInt(data.length);
            raf.write(data);

            // Header aktualisieren
            int sectorCount = (data.length + 4 + SECTOR_SIZE - 1) / SECTOR_SIZE;
            int offsetData = (sectorOffset << 8) | (sectorCount & 0xFF);
            offsets[index] = offsetData;

            raf.seek((long) index * 4);
            raf.writeInt(offsetData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void close() {

    }
}