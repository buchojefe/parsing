/*
 * Decompiled with CFR 0.152.
 */
package zombie.pot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

final class POTChunkData.Chunk {
    public final int[] counts;
    public byte[] bits;
    final int NSQRS;

    POTChunkData.Chunk() {
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
