package de.delautrer.game.world.persistence;

import java.io.*;

public class RegionFile {
    private static final int CHUNK_COUNT = 32 * 32;
    private static final int SECTOR_SIZE = 4096;

    private RandomAccessFile raf;
    private final int[] offsets = new int[CHUNK_COUNT];

    public RegionFile(File file) {
        try {
            // Datei EINMALIG im Lese-/Schreibmodus öffnen und offen halten!
            this.raf = new RandomAccessFile(file, "rw");
            loadOffsets();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadOffsets() throws IOException {
        if (raf.length() < SECTOR_SIZE)
            return;
        raf.seek(0);
        for (int i = 0; i < CHUNK_COUNT; i++) {
            offsets[i] = raf.readInt();
        }
    }

    // Methoden bleiben synchronized, damit nicht zwei Threads gleichzeitig
    // im selben Dateistream umherspringen (seek).
    public synchronized byte[] readChunk(int cx, int cz) {
        int index = (cx & 31) + (cz & 31) * 32;
        int offsetData = offsets[index];
        if (offsetData == 0)
            return null;

        int sectorOffset = offsetData >> 8;

        try {
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
        try {
            int sectorOffset = (int) (raf.length() + SECTOR_SIZE - 1) / SECTOR_SIZE;
            if (sectorOffset < 1)
                sectorOffset = 1; // Sector 0 ist für den Header (Offsets) reserviert

            raf.seek((long) sectorOffset * SECTOR_SIZE);
            raf.writeInt(data.length);
            raf.write(data);

            // Header (Offsets) aktualisieren
            int sectorCount = (data.length + 4 + SECTOR_SIZE - 1) / SECTOR_SIZE;
            int offsetData = (sectorOffset << 8) | (sectorCount & 0xFF);
            offsets[index] = offsetData;

            // Zurückspringen und Header schreiben
            raf.seek((long) index * 4);
            raf.writeInt(offsetData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void close() {
        try {
            if (raf != null) {
                raf.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}