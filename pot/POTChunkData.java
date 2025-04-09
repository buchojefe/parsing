/*
 * Decompiled with CFR 0.152.
 */
package zombie.pot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public final class POTChunkData {
    static final int FILE_VERSION = 1;
    static final int BIT_SOLID = 1;
    static final int BIT_WALLN = 2;
    static final int BIT_WALLW = 4;
    static final int BIT_WATER = 8;
    static final int BIT_ROOM = 16;
    static final int EMPTY_CHUNK = 0;
    static final int SOLID_CHUNK = 1;
    static final int REGULAR_CHUNK = 2;
    static final int WATER_CHUNK = 3;
    static final int ROOM_CHUNK = 4;
    static final int NUM_CHUNK_TYPES = 5;
    public final boolean pot;
    public final int CHUNK_DIM;
    public final int CHUNKS_PER_CELL;
    public final int CELL_DIM;
    public final int x;
    public final int y;
    public final Chunk[] chunks;

    POTChunkData(int n, int n2, boolean bl) {
        this.CHUNK_DIM = bl ? 8 : 10;
        this.CHUNKS_PER_CELL = bl ? 32 : 30;
        this.CELL_DIM = bl ? 256 : 300;
        this.pot = bl;
        this.x = n;
        this.y = n2;
        this.chunks = new Chunk[this.CHUNKS_PER_CELL * this.CHUNKS_PER_CELL];
        for (int i = 0; i < this.chunks.length; ++i) {
            this.chunks[i] = new Chunk();
        }
    }

    void load(File file) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file);
             DataInputStream dataInputStream = new DataInputStream(fileInputStream);){
            short s = dataInputStream.readShort();
            assert (s == 1);
            for (int i = 0; i < this.CHUNKS_PER_CELL; ++i) {
                for (int j = 0; j < this.CHUNKS_PER_CELL; ++j) {
                    this.chunks[j + i * this.CHUNKS_PER_CELL].load(dataInputStream);
                }
            }
        }
    }

    void save(String string) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(string);
             DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);){
            dataOutputStream.writeShort(1);
            for (int i = 0; i < this.CHUNKS_PER_CELL; ++i) {
                for (int j = 0; j < this.CHUNKS_PER_CELL; ++j) {
                    this.chunks[j + i * this.CHUNKS_PER_CELL].save(dataOutputStream);
                }
            }
        }
    }

    int getMinSquareX() {
        return this.x * this.CELL_DIM;
    }

    int getMinSquareY() {
        return this.y * this.CELL_DIM;
    }

    int getMaxSquareX() {
        return (this.x + 1) * this.CELL_DIM - 1;
    }

    int getMaxSquareY() {
        return (this.y + 1) * this.CELL_DIM - 1;
    }

    boolean containsSquare(int n, int n2) {
        return n >= this.getMinSquareX() && n <= this.getMaxSquareX() && n2 >= this.getMinSquareY() && n2 <= this.getMaxSquareY();
    }

    byte getSquareBits(int n, int n2) {
        if (!this.containsSquare(n, n2)) {
            return 0;
        }
        int n3 = (n - this.getMinSquareX()) / this.CHUNK_DIM;
        int n4 = (n2 - this.getMinSquareY()) / this.CHUNK_DIM;
        Chunk chunk = this.chunks[n3 + n4 * this.CHUNKS_PER_CELL];
        return chunk.getBits((n - this.getMinSquareX()) % this.CHUNK_DIM, (n2 - this.getMinSquareY()) % this.CHUNK_DIM);
    }

    void setSquareBits(int n, int n2, byte by) {
        int n3 = (n - this.getMinSquareX()) / this.CHUNK_DIM;
        int n4 = (n2 - this.getMinSquareY()) / this.CHUNK_DIM;
        Chunk chunk = this.chunks[n3 + n4 * this.CHUNKS_PER_CELL];
        chunk.setBits((n - this.getMinSquareX()) % this.CHUNK_DIM, (n2 - this.getMinSquareY()) % this.CHUNK_DIM, by);
    }

    final class Chunk {
        public final int[] counts;
        public byte[] bits;
        final int NSQRS;

        Chunk() {
            this.NSQRS = POTChunkData.this.CHUNK_DIM * POTChunkData.this.CHUNK_DIM;
            this.counts = new int[5];
            this.counts[0] = this.NSQRS;
        }

        void load(DataInputStream dataInputStream) throws IOException {
            Arrays.fill(this.counts, 0);
            byte by = dataInputStream.readByte();
            if (by == 0 || by == 1 || by == 3 || by == 4) {
                this.counts[by] = this.NSQRS;
            } else {
                assert (by == 2);
                this.bits = new byte[this.NSQRS];
                for (int i = 0; i < this.NSQRS; ++i) {
                    this.bits[i] = dataInputStream.readByte();
                    int n = this.getTypeOf(this.bits[i]);
                    this.counts[n] = this.counts[n] + 1;
                }
            }
        }

        void save(DataOutputStream dataOutputStream) throws IOException {
            int n = this.getType();
            dataOutputStream.writeByte(n);
            if (n == 2) {
                dataOutputStream.write(this.bits);
            }
        }

        byte getBits(int n, int n2) {
            if (this.counts[0] == this.NSQRS) {
                return 0;
            }
            if (this.counts[1] == this.NSQRS) {
                return 1;
            }
            if (this.counts[3] == this.NSQRS) {
                return 8;
            }
            if (this.counts[4] == this.NSQRS) {
                return 16;
            }
            return this.bits[n + n2 * POTChunkData.this.CHUNK_DIM];
        }

        byte setBits(int n, int n2, byte by) {
            int n3;
            byte by2 = this.getBits(n, n2);
            int n4 = this.getTypeOf(by2);
            if (n4 == (n3 = this.getTypeOf(by)) && n4 != 2) {
                return by;
            }
            assert (this.counts[n4] > 0);
            assert (this.counts[n3] < this.NSQRS);
            int n5 = n4;
            this.counts[n5] = this.counts[n5] - 1;
            int n6 = n3;
            this.counts[n6] = this.counts[n6] + 1;
            if (this.getType() == 2) {
                if (this.bits == null) {
                    this.bits = new byte[this.NSQRS];
                    Arrays.fill(this.bits, by2);
                }
                this.bits[n + n2 * POTChunkData.this.CHUNK_DIM] = by;
            } else {
                this.bits = null;
            }
            return by2;
        }

        int getType() {
            if (this.counts[0] == this.NSQRS) {
                return 0;
            }
            if (this.counts[1] == this.NSQRS) {
                return 1;
            }
            if (this.counts[3] == this.NSQRS) {
                return 3;
            }
            if (this.counts[4] == this.NSQRS) {
                return 4;
            }
            return 2;
        }

        int getTypeOf(byte by) {
            if (by == 0) {
                return 0;
            }
            if (by == 1) {
                return 1;
            }
            if (by == 8) {
                return 3;
            }
            if (by == 16) {
                return 4;
            }
            return 2;
        }
    }
}
