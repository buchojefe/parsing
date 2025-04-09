/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  gnu.trove.map.hash.TObjectIntHashMap
 *  zombie.iso.BuildingDef
 *  zombie.iso.BuildingID
 *  zombie.iso.IsoLot
 *  zombie.iso.LotHeader
 *  zombie.iso.MetaObject
 *  zombie.iso.RoomDef
 *  zombie.iso.RoomDef$RoomRect
 *  zombie.iso.RoomID
 *  zombie.iso.SliceY
 *  zombie.util.BufferedRandomAccessFile
 *  zombie.util.SharedStrings
 */
package zombie.pot;

import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import zombie.iso.BuildingDef;
import zombie.iso.BuildingID;
import zombie.iso.IsoLot;
import zombie.iso.LotHeader;
import zombie.iso.MetaObject;
import zombie.iso.RoomDef;
import zombie.iso.RoomID;
import zombie.iso.SliceY;
import zombie.util.BufferedRandomAccessFile;
import zombie.util.SharedStrings;

public final class POTLotHeader {
    private static final SharedStrings sharedStrings = new SharedStrings();
    private static final ArrayList<RoomDef> tempRooms = new ArrayList();
    public final boolean pot;
    public final int CHUNK_DIM;
    public final int CHUNKS_PER_CELL;
    public final int CELL_DIM;
    public final int x;
    public final int y;
    public int width = 0;
    public int height = 0;
    public int minLevel = -32;
    public int maxLevel = 31;
    public int minLevelNotEmpty = 1000;
    public int maxLevelNotEmpty = -1000;
    public int version = 0;
    public final HashMap<Long, RoomDef> Rooms = new HashMap();
    public final ArrayList<RoomDef> RoomList = new ArrayList();
    public final ArrayList<BuildingDef> Buildings = new ArrayList();
    public final ArrayList<String> tilesUsed = new ArrayList();
    public final TObjectIntHashMap<String> indexToTile = new TObjectIntHashMap(10, 0.5f, -1);
    public final byte[] zombieDensity;

    POTLotHeader(int n, int n2, boolean bl) {
        this.CHUNK_DIM = bl ? 8 : 10;
        this.CHUNKS_PER_CELL = bl ? 32 : 30;
        this.CELL_DIM = bl ? 256 : 300;
        this.pot = bl;
        this.x = n;
        this.y = n2;
        this.width = this.CHUNK_DIM;
        this.height = this.CHUNK_DIM;
        this.zombieDensity = new byte[this.CHUNKS_PER_CELL * this.CHUNKS_PER_CELL];
    }

    void clear() {
        for (BuildingDef buildingDef : this.Buildings) {
            buildingDef.Dispose();
        }
        this.Buildings.clear();
        this.Rooms.clear();
        this.RoomList.clear();
        this.tilesUsed.clear();
        this.indexToTile.clear();
    }

    void load(File file) {
        try (BufferedRandomAccessFile bufferedRandomAccessFile = new BufferedRandomAccessFile(file, "r", 4096);){
            int n;
            int n2;
            int n3;
            int n4;
            byte[] byArray = new byte[4];
            bufferedRandomAccessFile.read(byArray, 0, 4);
            boolean bl = Arrays.equals(byArray, LotHeader.LOTHEADER_MAGIC);
            if (!bl) {
                bufferedRandomAccessFile.seek(0L);
            }
            this.version = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
            if (this.version < 0 || this.version > 1) {
                throw new IOException("Unsupported version " + this.version);
            }
            int n5 = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
            for (n4 = 0; n4 < n5; ++n4) {
                String string = IsoLot.readString((BufferedRandomAccessFile)bufferedRandomAccessFile);
                string = sharedStrings.get(string.trim());
                this.tilesUsed.add(string);
                this.indexToTile.put((Object)string, n4);
            }
            n4 = this.version == 0 ? bufferedRandomAccessFile.read() : 0;
            this.width = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
            this.height = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
            if (this.version == 0) {
                this.minLevel = 0;
                this.maxLevel = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
            } else {
                this.minLevel = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
                this.maxLevel = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
            }
            this.minLevelNotEmpty = this.minLevel;
            this.maxLevelNotEmpty = this.maxLevel;
            int n6 = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
            for (n3 = 0; n3 < n6; ++n3) {
                int n7;
                int n8;
                int n9;
                int n10;
                int n11;
                String string = IsoLot.readString((BufferedRandomAccessFile)bufferedRandomAccessFile);
                long l = RoomID.makeID((int)this.x, (int)this.y, (int)n3);
                RoomDef roomDef = new RoomDef(l, sharedStrings.get(string));
                roomDef.level = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
                n2 = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
                for (n11 = 0; n11 < n2; ++n11) {
                    n10 = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
                    n9 = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
                    n8 = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
                    n7 = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
                    RoomDef.RoomRect roomRect = new RoomDef.RoomRect(n10 + this.x * this.CELL_DIM, n9 + this.y * this.CELL_DIM, n8, n7);
                    roomDef.rects.add(roomRect);
                }
                roomDef.CalculateBounds();
                this.Rooms.put(roomDef.ID, roomDef);
                this.RoomList.add(roomDef);
                n11 = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
                for (n10 = 0; n10 < n11; ++n10) {
                    n9 = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
                    n8 = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
                    n7 = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
                    roomDef.objects.add(new MetaObject(n9, n8 + this.x * this.CELL_DIM - roomDef.x, n7 + this.y * this.CELL_DIM - roomDef.y, roomDef));
                }
            }
            n3 = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
            for (n = 0; n < n3; ++n) {
                BuildingDef buildingDef = new BuildingDef();
                buildingDef.ID = BuildingID.makeID((int)this.x, (int)this.y, (int)n);
                int n12 = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
                for (int i = 0; i < n12; ++i) {
                    n2 = IsoLot.readInt((RandomAccessFile)bufferedRandomAccessFile);
                    long l = RoomID.makeID((int)this.x, (int)this.y, (int)n2);
                    RoomDef roomDef = this.Rooms.get(l);
                    roomDef.building = buildingDef;
                    buildingDef.rooms.add(roomDef);
                }
                buildingDef.CalculateBounds(tempRooms);
                this.Buildings.add(buildingDef);
            }
            for (n = 0; n < this.CHUNKS_PER_CELL; ++n) {
                for (int i = 0; i < this.CHUNKS_PER_CELL; ++i) {
                    this.zombieDensity[n + i * this.CHUNKS_PER_CELL] = (byte)bufferedRandomAccessFile.read();
                }
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /*
     * WARNING - void declaration
     */
    void save(String string) throws IOException {
        ByteBuffer byteBuffer = SliceY.SliceBuffer;
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.clear();
        byteBuffer.put(LotHeader.LOTHEADER_MAGIC);
        byteBuffer.putInt(1);
        byteBuffer.putInt(this.tilesUsed.size());
        for (int i = 0; i < this.tilesUsed.size(); ++i) {
            this.writeString(byteBuffer, this.tilesUsed.get(i));
        }
        byteBuffer.putInt(this.width);
        byteBuffer.putInt(this.height);
        byteBuffer.putInt(this.minLevelNotEmpty);
        byteBuffer.putInt(this.maxLevelNotEmpty);
        byteBuffer.putInt(this.RoomList.size());
        for (RoomDef roomDef : this.RoomList) {
            this.writeString(byteBuffer, roomDef.name);
            byteBuffer.putInt(roomDef.level);
            byteBuffer.putInt(roomDef.rects.size());
            for (RoomDef.RoomRect roomRect : roomDef.rects) {
                byteBuffer.putInt(roomRect.x - this.getMinSquareX());
                byteBuffer.putInt(roomRect.y - this.getMinSquareY());
                byteBuffer.putInt(roomRect.w);
                byteBuffer.putInt(roomRect.h);
            }
            byteBuffer.putInt(roomDef.objects.size());
            for (RoomDef.RoomRect roomRect : roomDef.objects) {
                byteBuffer.putInt(roomRect.getType());
                byteBuffer.putInt(roomRect.getX());
                byteBuffer.putInt(roomRect.getY());
            }
        }
        byteBuffer.putInt(this.Buildings.size());
        for (BuildingDef buildingDef : this.Buildings) {
            byteBuffer.putInt(buildingDef.rooms.size());
            for (RoomDef.RoomRect roomRect : buildingDef.rooms) {
                assert (roomRect.ID == (long)this.RoomList.indexOf(roomRect));
                byteBuffer.putInt(RoomID.getIndex((long)roomRect.ID));
            }
        }
        for (int i = 0; i < this.CHUNKS_PER_CELL; ++i) {
            void var4_11;
            boolean bl = false;
            while (var4_11 < this.CHUNKS_PER_CELL) {
                byteBuffer.put(this.zombieDensity[i + var4_11 * this.CHUNKS_PER_CELL]);
                ++var4_11;
            }
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(string);){
            fileOutputStream.write(byteBuffer.array(), 0, byteBuffer.position());
        }
    }

    void writeString(ByteBuffer byteBuffer, String string) {
        byte[] byArray = string.getBytes(StandardCharsets.UTF_8);
        byteBuffer.put(byArray);
        byteBuffer.put((byte)10);
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

    void addBuilding(BuildingDef buildingDef) {
        BuildingDef buildingDef2 = new BuildingDef();
        buildingDef2.ID = this.Buildings.size();
        for (RoomDef roomDef : buildingDef.rooms) {
            RoomDef roomDef2 = new RoomDef((long)this.RoomList.size(), roomDef.name);
            roomDef2.ID = this.RoomList.size();
            roomDef2.level = roomDef.level;
            roomDef2.building = buildingDef2;
            roomDef2.rects.addAll(roomDef.rects);
            roomDef2.objects.addAll(roomDef.objects);
            roomDef2.CalculateBounds();
            buildingDef2.rooms.add(roomDef2);
            this.Rooms.put(roomDef2.ID, roomDef2);
            this.RoomList.add(roomDef2);
        }
        buildingDef2.CalculateBounds(tempRooms);
        this.Buildings.add(buildingDef2);
    }

    byte getZombieDensityForSquare(int n, int n2) {
        if (!this.containsSquare(n, n2)) {
            return 0;
        }
        int n3 = n - this.getMinSquareX();
        int n4 = n2 - this.getMinSquareY();
        return this.zombieDensity[n3 / this.CHUNK_DIM + n4 / this.CHUNK_DIM * this.CHUNKS_PER_CELL];
    }

    void setZombieDensity(byte[] byArray) {
        for (int i = 0; i < this.CHUNKS_PER_CELL; ++i) {
            for (int j = 0; j < this.CHUNKS_PER_CELL; ++j) {
                int n = 0;
                for (int k = 0; k < this.CHUNK_DIM * this.CHUNK_DIM; ++k) {
                    n += byArray[j * this.CHUNK_DIM + i * this.CHUNK_DIM * this.CELL_DIM + k % this.CHUNK_DIM + k / this.CHUNK_DIM * this.CELL_DIM];
                }
                this.zombieDensity[j + i * this.CHUNKS_PER_CELL] = (byte)(n / (this.CHUNK_DIM * this.CHUNK_DIM));
            }
        }
    }

    int getTileIndex(String string) {
        int n = this.indexToTile.get((Object)(string = sharedStrings.get(string)));
        if (n == this.indexToTile.getNoEntryValue()) {
            n = this.tilesUsed.size();
            this.indexToTile.put((Object)string, this.tilesUsed.size());
            this.tilesUsed.add(string);
        }
        return n;
    }
}
