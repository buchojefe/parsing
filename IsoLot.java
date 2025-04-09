/*
 * Decompiled with CFR.
 */
package zombie.iso;

import gnu.trove.list.array.TIntArrayList;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Math;
import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import zombie.ChunkMapFilenames;
import zombie.core.logger.ExceptionLogger;
import zombie.iso.IsoChunk;
import zombie.iso.LotHeader;
import zombie.popman.ObjectPool;
import zombie.util.BufferedRandomAccessFile;

public class IsoLot {
    public static final HashMap<String, LotHeader> InfoHeaders;
    public static final ArrayList<String> InfoHeaderNames;
    public static final HashMap<String, String> InfoFileNames;
    public static final ObjectPool<IsoLot> pool;
    private String m_lastUsedPath;
    public int wx;
    public int wy;
    final int[] m_offsetInData;
    final TIntArrayList m_data;
    private RandomAccessFile m_in;
    LotHeader info;

    public IsoLot() {
        super();
        this.m_lastUsedPath = "";
        this.wx = 0;
        this.wy = 0;
        this.m_offsetInData = new int[800];
        this.m_data = new TIntArrayList();
        this.m_in = null;
    }

    public static void Dispose() {
        for (LotHeader lotHeader : InfoHeaders.values()) {
            lotHeader.Dispose();
        }
        InfoHeaders.clear();
        InfoHeaderNames.clear();
        InfoFileNames.clear();
        pool.forEach(isoLot -> {
            RandomAccessFile randomAccessFile = isoLot.m_in;
            if (randomAccessFile != null) {
                isoLot.m_in = null;
                try {
                    randomAccessFile.close();
                }
                catch (IOException iOException) {
                    ExceptionLogger.logException((Throwable)iOException);
                }
            }
        });
    }

    public static String readString(BufferedRandomAccessFile bufferedRandomAccessFile) throws EOFException, IOException {
        String string = bufferedRandomAccessFile.getNextLine();
        return string;
    }

    public static int readInt(RandomAccessFile randomAccessFile) throws EOFException, IOException {
        int n;
        int n2;
        int n3;
        int n4 = randomAccessFile.read();
        if ((n4 | (n3 = randomAccessFile.read()) | (n2 = randomAccessFile.read()) | (n = randomAccessFile.read())) < 0) {
            throw new EOFException();
        }
        return (n4 << 0) + (n3 << 8) + (n2 << 16) + (n << 24);
    }

    public static int readShort(RandomAccessFile randomAccessFile) throws EOFException, IOException {
        int n;
        int n2 = randomAccessFile.read();
        if ((n2 | (n = randomAccessFile.read())) < 0) {
            throw new EOFException();
        }
        return (n2 << 0) + (n << 8);
    }

    public static synchronized void put(IsoLot isoLot) {
        isoLot.info = null;
        isoLot.m_data.resetQuick();
        pool.release((Object)isoLot);
    }

    public static synchronized IsoLot get(Integer n, Integer n2, Integer n3, Integer n4, IsoChunk isoChunk) {
        IsoLot isoLot = (IsoLot)pool.alloc();
        isoLot.load((Integer)n, (Integer)n2, (Integer)n3, (Integer)n4, (IsoChunk)isoChunk);
        return isoLot;
    }

    public void load(Integer n, Integer n2, Integer n3, Integer n4, IsoChunk isoChunk) {
        String string = ChunkMapFilenames.instance.getHeader((int)n.intValue(), (int)n2.intValue());
        this.info = (LotHeader)InfoHeaders.get((Object)string);
        this.wx = n3.intValue();
        this.wy = n4.intValue();
        isoChunk.lotheader = this.info;
        try {
            string = "world_" + n + "_" + n2 + ".lotpack";
            File file = new File((String)((String)InfoFileNames.get((Object)string)));
            if (this.m_in == null || !this.m_lastUsedPath.equals((Object)file.getAbsolutePath())) {
                if (this.m_in != null) {
                    this.m_in.close();
                }
                this.m_in = new BufferedRandomAccessFile((String)file.getAbsolutePath(), (String)"r", (int)4096);
                this.m_lastUsedPath = file.getAbsolutePath();
            }
            int n5 = 0;
            int n6 = this.wx - n.intValue() * 30;
            int n7 = this.wy - n2.intValue() * 30;
            int n8 = n6 * 30 + n7;
            this.m_in.seek((long)((long)(4 + n8 * 8)));
            int n9 = IsoLot.readInt((RandomAccessFile)this.m_in);
            this.m_in.seek((long)((long)n9));
            this.m_data.resetQuick();
            int n10 = Math.min((int)this.info.levels, (int)8);
            for (int i = 0; i < n10; ++i) {
                for (int j = 0; j < 10; ++j) {
                    for (int k = 0; k < 10; ++k) {
                        int n11 = j + k * 10 + i * 100;
                        this.m_offsetInData[n11] = -1;
                        if (n5 > 0) {
                            --n5;
                            continue;
                        }
                        int n12 = IsoLot.readInt((RandomAccessFile)this.m_in);
                        if (n12 == -1 && (n5 = IsoLot.readInt((RandomAccessFile)this.m_in)) > 0) {
                            --n5;
                            continue;
                        }
                        if (n12 <= 1) continue;
                        this.m_offsetInData[n11] = this.m_data.size();
                        this.m_data.add((int)(n12 - 1));
                        int n13 = IsoLot.readInt((RandomAccessFile)this.m_in);
                        for (int i2 = 1; i2 < n12; ++i2) {
                            int n14 = IsoLot.readInt((RandomAccessFile)this.m_in);
                            this.m_data.add((int)n14);
                        }
                    }
                }
            }
        }
        catch (Exception exception) {
            Arrays.fill((int[])this.m_offsetInData, (int)-1);
            this.m_data.resetQuick();
            ExceptionLogger.logException((Throwable)exception);
        }
    }

    static {
        InfoHeaders = new HashMap();
        InfoHeaderNames = new ArrayList();
        InfoFileNames = new HashMap();
        pool = new ObjectPool(IsoLot::new);
    }
}
