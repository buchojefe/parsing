/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  gnu.trove.list.array.TIntArrayList
 *  zombie.iso.IsoLot
 *  zombie.iso.LotHeader
 *  zombie.iso.SliceY
 *  zombie.util.BufferedRandomAccessFile
 */
package zombie.pot;

import gnu.trove.list.array.TIntArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import zombie.iso.IsoLot;
import zombie.iso.LotHeader;
import zombie.iso.SliceY;
import zombie.pot.POTLotHeader;
import zombie.util.BufferedRandomAccessFile;

public final class POTLotPack {
    static File m_lastFile = null;
    static RandomAccessFile m_in = null;
    public final POTLotHeader lotHeader;
    public final boolean pot;
    public final int CHUNK_DIM;
    public final int CHUNKS_PER_CELL;
    public final int CELL_DIM;
    public final int x;
    public final int y;
    int m_version;
    final boolean[] m_loadedChunks;
    final int[] m_offsetInData;
    final TIntArrayList m_data = new TIntArrayList();

    POTLotPack(POTLotHeader pOTLotHeader) {
        this.lotHeader = pOTLotHeader;
        this.pot = pOTLotHeader.pot;
        this.x = pOTLotHeader.x;
        this.y = pOTLotHeader.y;
        this.CHUNK_DIM = this.pot ? 8 : 10;
        this.CHUNKS_PER_CELL = this.pot ? 32 : 30;
        this.CELL_DIM = this.pot ? 256 : 300;
        this.m_loadedChunks = new boolean[this.CHUNKS_PER_CELL * this.CHUNKS_PER_CELL];
        this.m_offsetInData = new int[this.CELL_DIM * this.CELL_DIM * (pOTLotHeader.maxLevel - pOTLotHeader.minLevel + 1)];
        Arrays.fill(this.m_offsetInData, -1);
    }

    void clear() {
        this.m_data.clear();
    }

    void load(File file) throws IOException {
        if (m_in == null || m_lastFile != file) {
            if (m_in != null) {
                m_in.close();
            }
            System.out.println(file.getPath());
            m_in = new BufferedRandomAccessFile(file, "r", 4096);
            m_lastFile = file;
        }
        m_in.seek(0L);
        byte[] byArray = new byte[4];
        m_in.read(byArray, 0, 4);
        boolean bl = Arrays.equals(byArray, LotHeader.LOTPACK_MAGIC);
        if (bl) {
            this.m_version = IsoLot.readInt((RandomAccessFile)m_in);
            if (this.m_version < 0 || this.m_version > 1) {
                throw new IOException("Unsupported version " + this.m_version);
            }
        } else {
            m_in.seek(0L);
            this.m_version = 0;
        }
        for (int i = 0; i < this.CHUNKS_PER_CELL; ++i) {
            for (int j = 0; j < this.CHUNKS_PER_CELL; ++j) {
                this.loadChunk(this.x * this.CHUNKS_PER_CELL + i, this.y * this.CHUNKS_PER_CELL + j);
            }
        }
    }

    void loadChunk(int n, int n2) throws IOException {
        int n3 = 0;
        int n4 = n - this.x * this.CHUNKS_PER_CELL;
        int n5 = n2 - this.y * this.CHUNKS_PER_CELL;
        int n6 = n4 * this.CHUNKS_PER_CELL + n5;
        m_in.seek((long)((this.m_version >= 1 ? 8 : 0) + 4) + (long)n6 * 8L);
        int n7 = IsoLot.readInt((RandomAccessFile)m_in);
        m_in.seek(n7);
        int n8 = Math.max(this.lotHeader.minLevel, -32);
        int n9 = Math.min(this.lotHeader.maxLevel, 31);
        if (this.m_version == 0) {
            --n9;
        }
        for (int i = n8; i <= n9; ++i) {
            for (int j = 0; j < this.CHUNK_DIM; ++j) {
                for (int k = 0; k < this.CHUNK_DIM; ++k) {
                    int n10 = j + k * this.CELL_DIM;
                    this.m_offsetInData[n10 += n4 * this.CHUNK_DIM + n5 * this.CHUNK_DIM * this.CELL_DIM + (i - this.lotHeader.minLevel) * this.CELL_DIM * this.CELL_DIM] = -1;
                    if (n3 > 0) {
                        --n3;
                        continue;
                    }
                    int n11 = IsoLot.readInt((RandomAccessFile)m_in);
                    if (n11 == -1 && (n3 = IsoLot.readInt((RandomAccessFile)m_in)) > 0) {
                        --n3;
                        continue;
                    }
                    if (n11 <= 1) continue;
                    this.m_offsetInData[n10] = this.m_data.size();
                    this.m_data.add(n11 - 1);
                    int n12 = IsoLot.readInt((RandomAccessFile)m_in);
                    for (int i2 = 1; i2 < n11; ++i2) {
                        int n13 = IsoLot.readInt((RandomAccessFile)m_in);
                        this.m_data.add(n13);
                    }
                }
            }
        }
    }

    void save(String string) throws IOException {
        int n = this.CHUNKS_PER_CELL * this.CHUNKS_PER_CELL;
        ByteBuffer byteBuffer = SliceY.SliceBuffer;
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.clear();
        byteBuffer.put(LotHeader.LOTPACK_MAGIC);
        byteBuffer.putInt(1);
        byteBuffer.putInt(this.CHUNK_DIM);
        int n2 = byteBuffer.position();
        byteBuffer.position(n2 + n * 8);
        for (int i = 0; i < this.CHUNKS_PER_CELL; ++i) {
            for (int j = 0; j < this.CHUNKS_PER_CELL; ++j) {
                this.saveChunk(byteBuffer, n2, i, j);
            }
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(string);){
            fileOutputStream.write(byteBuffer.array(), 0, byteBuffer.position());
        }
    }

    void saveChunk(ByteBuffer byteBuffer, int n, int n2, int n3) {
        byteBuffer.putLong(n + (n2 * this.CHUNKS_PER_CELL + n3) * 8, byteBuffer.position());
        int n4 = 0;
        for (int i = this.lotHeader.minLevelNotEmpty; i <= this.lotHeader.maxLevelNotEmpty; ++i) {
            for (int j = 0; j < this.CHUNK_DIM; ++j) {
                for (int k = 0; k < this.CHUNK_DIM; ++k) {
                    int n5 = j + k * this.CELL_DIM + (i - this.lotHeader.minLevel) * this.CELL_DIM * this.CELL_DIM;
                    int n6 = this.m_offsetInData[n5 += n2 * this.CHUNK_DIM + n3 * this.CHUNK_DIM * this.CELL_DIM];
                    if (n6 == -1) {
                        ++n4;
                        continue;
                    }
                    if (n4 > 0) {
                        byteBuffer.putInt(-1);
                        byteBuffer.putInt(n4);
                        n4 = 0;
                    }
                    int n7 = this.m_data.getQuick(n6);
                    byteBuffer.putInt(n7 + 1);
                    int n8 = -1;
                    byteBuffer.putInt(n8);
                    for (int i2 = 0; i2 < n7; ++i2) {
                        byteBuffer.putInt(this.m_data.getQuick(n6 + 1 + i2));
                    }
                }
            }
        }
        if (n4 > 0) {
            byteBuffer.putInt(-1);
            byteBuffer.putInt(n4);
        }
    }

    String[] getSquareData(int n, int n2, int n3) {
        int n4 = (n -= this.lotHeader.getMinSquareX()) + (n2 -= this.lotHeader.getMinSquareY()) * this.CELL_DIM + (n3 - this.lotHeader.minLevel) * this.CELL_DIM * this.CELL_DIM;
        int n5 = this.m_offsetInData[n4];
        if (n5 == -1) {
            return null;
        }
        int n6 = this.m_data.getQuick(n5);
        String[] stringArray = new String[n6];
        for (int i = 0; i < n6; ++i) {
            stringArray[i] = this.lotHeader.tilesUsed.get(this.m_data.getQuick(n5 + 1 + i));
        }
        return stringArray;
    }

    void setSquareData(int n, int n2, int n3, String[] stringArray) {
        if (n3 < this.lotHeader.minLevel || n3 > this.lotHeader.maxLevel) {
            return;
        }
        int n4 = (n -= this.lotHeader.getMinSquareX()) + (n2 -= this.lotHeader.getMinSquareY()) * this.CELL_DIM + (n3 - this.lotHeader.minLevel) * this.CELL_DIM * this.CELL_DIM;
        if (stringArray == null || stringArray.length == 0) {
            this.m_offsetInData[n4] = -1;
            return;
        }
        this.m_offsetInData[n4] = this.m_data.size();
        this.m_data.add(stringArray.length);
        for (String string : stringArray) {
            this.m_data.add(this.lotHeader.getTileIndex(string));
        }
        this.lotHeader.minLevelNotEmpty = Math.min(this.lotHeader.minLevelNotEmpty, n3);
        this.lotHeader.maxLevelNotEmpty = Math.max(this.lotHeader.maxLevelNotEmpty, n3);
    }
}
