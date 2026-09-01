package de.delautrer.game.world.persistence;

import java.io.*;
import java.util.BitSet;

public class RegionFile {
    public enum ReadResultType { OK, MISSING, CORRUPT }

    public static final class ReadResult {
        public final ReadResultType type;
        public final byte[] data;

        public ReadResult(ReadResultType type, byte[] data) {
            this.type = type;
            this.data = data;
        }

        public static ReadResult ok(byte[] data) { return new ReadResult(ReadResultType.OK, data); }
        public static ReadResult missing() { return new ReadResult(ReadResultType.MISSING, null); }
        public static ReadResult corrupt() { return new ReadResult(ReadResultType.CORRUPT, null); }
        public static ReadResult corrupt(byte[] data) { return new ReadResult(ReadResultType.CORRUPT, data); }
    }

    private static final int CHUNK_COUNT = 32 * 32;
    private static final int SECTOR_SIZE = 4096;

    private final RandomAccessFile raf;
    private final int[] offsets = new int[CHUNK_COUNT];
    private final BitSet occupiedSectors = new BitSet();

    public RegionFile(File file) throws IOException {
        this.raf = new RandomAccessFile(file, "rw");
        loadOffsetsAndSectors();
    }

    private synchronized void loadOffsetsAndSectors() throws IOException {
        occupiedSectors.set(0); // Sector 0 reserved for header offsets
        int totalSectors = (int) ((raf.length() + SECTOR_SIZE - 1) / SECTOR_SIZE);

        if (raf.length() < SECTOR_SIZE) {
            return;
        }

        raf.seek(0);
        for (int i = 0; i < CHUNK_COUNT; i++) {
            offsets[i] = raf.readInt();
            if (offsets[i] != 0) {
                int sectorOffset = offsets[i] >> 8;
                int sectorCount = offsets[i] & 0xFF;
                for (int s = 0; s < sectorCount; s++) {
                    occupiedSectors.set(sectorOffset + s);
                }
            }
        }
    }

    public synchronized ReadResult readChunk(int cx, int cz) {
        int index = (cx & 31) + (cz & 31) * 32;
        int offsetData = offsets[index];
        if (offsetData == 0) return ReadResult.missing();

        int sectorOffset = offsetData >> 8;
        int sectorCount = offsetData & 0xFF;

        try {
            if ((long) sectorOffset * SECTOR_SIZE >= raf.length()) {
                System.err.println("[RegionFile] Corrupt chunk offset out of bounds: (" + cx + "," + cz + ")");
                return ReadResult.corrupt(null);
            }
            raf.seek((long) sectorOffset * SECTOR_SIZE);
            int length = raf.readInt();
            if (length <= 0 || length > sectorCount * SECTOR_SIZE || length > 1_048_576) {
                System.err.println("[RegionFile] Invalid chunk payload length " + length + " for chunk (" + cx + "," + cz + ")");
                int bytesToRead = (int) Math.min(sectorCount * SECTOR_SIZE, Math.max(0, raf.length() - (long) sectorOffset * SECTOR_SIZE));
                byte[] raw = new byte[bytesToRead];
                raf.seek((long) sectorOffset * SECTOR_SIZE);
                raf.read(raw);
                return ReadResult.corrupt(raw.length > 0 ? raw : null);
            }
            byte[] data = new byte[length];
            raf.readFully(data);
            return ReadResult.ok(data);
        } catch (IOException e) {
            System.err.println("[RegionFile] Error reading chunk (" + cx + "," + cz + "): " + e.getMessage());
            return ReadResult.corrupt(null);
        }
    }

    public synchronized void writeChunk(int cx, int cz, byte[] data) throws IOException {
        int index = (cx & 31) + (cz & 31) * 32;
        int neededSectors = (data.length + 4 + SECTOR_SIZE - 1) / SECTOR_SIZE;

        int oldOffsetData = offsets[index];
        int oldSectorOffset = oldOffsetData >> 8;
        int oldSectorCount = oldOffsetData & 0xFF;

        int sectorOffset;

        if (oldSectorOffset != 0 && neededSectors <= oldSectorCount) {
            sectorOffset = oldSectorOffset;
            for (int s = neededSectors; s < oldSectorCount; s++) {
                occupiedSectors.clear(oldSectorOffset + s);
            }
        } else {
            if (oldSectorOffset != 0) {
                for (int s = 0; s < oldSectorCount; s++) {
                    occupiedSectors.clear(oldSectorOffset + s);
                }
            }
            sectorOffset = findFreeSectors(neededSectors);
            for (int s = 0; s < neededSectors; s++) {
                occupiedSectors.set(sectorOffset + s);
            }
        }

        raf.seek((long) sectorOffset * SECTOR_SIZE);
        raf.writeInt(data.length);
        raf.write(data);

        int offsetData = (sectorOffset << 8) | (neededSectors & 0xFF);
        offsets[index] = offsetData;

        raf.seek((long) index * 4);
        raf.writeInt(offsetData);
    }

    private int findFreeSectors(int count) {
        int start = 1;
        while (true) {
            int found = 0;
            int candidate = start;
            for (int i = start; ; i++) {
                if (!occupiedSectors.get(i)) {
                    if (found == 0) candidate = i;
                    found++;
                    if (found == count) return candidate;
                } else {
                    found = 0;
                    start = i + 1;
                    break;
                }
            }
        }
    }

    public synchronized void close() {
        try {
            if (raf != null) {
                raf.close();
            }
        } catch (IOException e) {
            System.err.println("[RegionFile] Error closing file: " + e.getMessage());
        }
    }
}