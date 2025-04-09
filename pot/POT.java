/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  gnu.trove.list.array.TIntArrayList
 *  gnu.trove.map.hash.TIntObjectHashMap
 *  zombie.asset.AssetManager
 *  zombie.asset.AssetPath
 *  zombie.core.math.PZMath
 *  zombie.core.random.RandLua
 *  zombie.core.random.RandStandard
 *  zombie.iso.BuildingDef
 *  zombie.iso.NewMapBinaryFile
 *  zombie.iso.NewMapBinaryFile$ChunkData
 *  zombie.iso.NewMapBinaryFile$Header
 *  zombie.iso.SliceY
 *  zombie.worldMap.WorldMapBinary
 *  zombie.worldMap.WorldMapCell
 *  zombie.worldMap.WorldMapData
 *  zombie.worldMap.WorldMapDataAssetManager
 *  zombie.worldMap.WorldMapFeature
 */
package zombie.pot;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.core.math.PZMath;
import zombie.core.random.RandLua;
import zombie.core.random.RandStandard;
import zombie.iso.BuildingDef;
import zombie.iso.NewMapBinaryFile;
import zombie.iso.SliceY;
import zombie.pot.POTChunkData;
import zombie.pot.POTLotHeader;
import zombie.pot.POTLotPack;
import zombie.pot.POTWorldMapData;
import zombie.worldMap.WorldMapBinary;
import zombie.worldMap.WorldMapCell;
import zombie.worldMap.WorldMapData;
import zombie.worldMap.WorldMapDataAssetManager;
import zombie.worldMap.WorldMapFeature;

public final class POT {
    String m_mapDirectoryIn;
    String m_mapDirectoryOut;
    int m_minX;
    int m_minY;
    int m_maxX;
    int m_maxY;
    final TIntObjectHashMap<File> m_lotHeaderFiles = new TIntObjectHashMap();
    final TIntObjectHashMap<File> m_lotPackFiles = new TIntObjectHashMap();
    final TIntObjectHashMap<File> m_chunkDataFiles = new TIntObjectHashMap();
    final byte[] zombieDensityPerSquare = new byte[65536];
    final TIntObjectHashMap<POTLotHeader> m_newLotHeader = new TIntObjectHashMap();
    final TIntObjectHashMap<POTLotHeader> m_oldLotHeader = new TIntObjectHashMap();
    final TIntObjectHashMap<POTLotPack> m_oldLotPack = new TIntObjectHashMap();
    final TIntObjectHashMap<POTChunkData> m_oldChunkData = new TIntObjectHashMap();
    final TIntArrayList m_onlyTheseCells = new TIntArrayList();
    public static final int CHUNK_DIM_OLD = 10;
    public static final int CHUNK_PER_CELL_OLD = 30;
    public static final int CELL_DIM_OLD = 300;
    public static final int CHUNK_DIM_NEW = 8;
    public static final int CHUNK_PER_CELL_NEW = 32;
    public static final int CELL_DIM_NEW = 256;
    static final int LEVELS = 64;

    public void convertMapDirectory(String string, String string2) throws Exception {
        Files.createDirectories(Paths.get(string2, new String[0]), new FileAttribute[0]);
        this.m_mapDirectoryIn = string;
        this.m_mapDirectoryOut = string2;
        this.readFileNames();
        this.convertLotHeaders();
        this.convertLotPack();
        this.convertChunkData();
        if (!this.m_onlyTheseCells.isEmpty()) {
            return;
        }
        this.convertObjectsLua();
        this.convertSpawnPointsLua();
        this.convertWorldMapBIN("worldmap.xml.bin");
        this.convertWorldMapBIN("worldmap-forest.xml.bin");
        this.convertWorldMapXML();
    }

    boolean shouldIgnoreCell(int n, int n2) {
        if (this.m_onlyTheseCells.isEmpty()) {
            return false;
        }
        for (int i = 0; i < this.m_onlyTheseCells.size(); i += 2) {
            int n3 = this.m_onlyTheseCells.get(i);
            int n4 = this.m_onlyTheseCells.get(i + 1);
            if (n < n3 - 1 || n > n3 + 1 || n2 < n4 - 1 || n2 > n4 + 1) continue;
            return false;
        }
        return true;
    }

    boolean shouldConvertNewCell(int n, int n2) {
        if (this.m_onlyTheseCells.isEmpty()) {
            return true;
        }
        int n3 = n * 256 / 300;
        int n4 = n2 * 256 / 300;
        int n5 = ((n + 1) * 256 - 1) / 300;
        int n6 = ((n2 + 1) * 256 - 1) / 300;
        for (int i = n4; i <= n6; ++i) {
            for (int j = n3; j <= n5; ++j) {
                for (int k = 0; k < this.m_onlyTheseCells.size(); k += 2) {
                    int n7 = this.m_onlyTheseCells.get(k);
                    int n8 = this.m_onlyTheseCells.get(k + 1);
                    if (j != n7 || i != n8) continue;
                    return true;
                }
            }
        }
        return false;
    }

    void readFileNames() {
        this.m_minX = Integer.MAX_VALUE;
        this.m_minY = Integer.MAX_VALUE;
        this.m_maxX = Integer.MIN_VALUE;
        this.m_maxY = Integer.MIN_VALUE;
        File file = new File(this.m_mapDirectoryIn);
        File[] fileArray = file.listFiles();
        for (int i = 0; i < fileArray.length; ++i) {
            int n;
            int n2;
            int n3;
            String[] stringArray;
            String string = fileArray[i].getName();
            String string2 = string.substring(string.lastIndexOf(46));
            string = string.substring(0, string.lastIndexOf(46));
            if (".lotheader".equals(string2)) {
                stringArray = string.split("_");
                n3 = Integer.parseInt(stringArray[0]);
                if (this.shouldIgnoreCell(n3, n2 = Integer.parseInt(stringArray[1]))) continue;
                this.m_minX = PZMath.min((int)this.m_minX, (int)n3);
                this.m_minY = PZMath.min((int)this.m_minY, (int)n2);
                this.m_maxX = PZMath.max((int)this.m_maxX, (int)n3);
                this.m_maxY = PZMath.max((int)this.m_maxY, (int)n2);
                n = n3 + n2 * 1000;
                this.m_lotHeaderFiles.put(n, (Object)fileArray[i]);
                continue;
            }
            if (".lotpack".equals(string2)) {
                stringArray = string.replace("world_", "").split("_");
                n3 = Integer.parseInt(stringArray[0]);
                if (this.shouldIgnoreCell(n3, n2 = Integer.parseInt(stringArray[1]))) continue;
                n = n3 + n2 * 1000;
                this.m_lotPackFiles.put(n, (Object)fileArray[i]);
                continue;
            }
            if (!string.startsWith("chunkdata_") || this.shouldIgnoreCell(n3 = Integer.parseInt((stringArray = string.replace("chunkdata_", "").split("_"))[0]), n2 = Integer.parseInt(stringArray[1]))) continue;
            n = n3 + n2 * 1000;
            this.m_chunkDataFiles.put(n, (Object)fileArray[i]);
        }
    }

    void convertLotHeaders() {
        for (int i = this.m_minY * 300; i < (this.m_maxY + 1) * 300; i += 256) {
            for (int j = this.m_minX * 300; j <= (this.m_maxX + 1) * 300; j += 256) {
                int n = j / 256;
                int n2 = i / 256;
                if (!this.shouldConvertNewCell(n, n2)) continue;
                this.convertLotHeader(n, n2);
            }
        }
    }

    void convertLotHeader(int n, int n2) {
        int n3;
        POTLotHeader pOTLotHeader = new POTLotHeader(n, n2, true);
        int n4 = n * 256 / 300;
        int n5 = n2 * 256 / 300;
        int n6 = ((n + 1) * 256 - 1) / 300;
        int n7 = ((n2 + 1) * 256 - 1) / 300;
        Arrays.fill(this.zombieDensityPerSquare, (byte)0);
        for (n3 = n5; n3 <= n7; ++n3) {
            for (int i = n4; i <= n6; ++i) {
                POTLotHeader pOTLotHeader2 = this.getOldLotHeader(i, n3);
                if (pOTLotHeader2 == null) continue;
                for (BuildingDef buildingDef : pOTLotHeader2.Buildings) {
                    if (!pOTLotHeader.containsSquare(buildingDef.x, buildingDef.y)) continue;
                    pOTLotHeader.addBuilding(buildingDef);
                }
                for (int j = 0; j < 256; ++j) {
                    for (int k = 0; k < 256; ++k) {
                        this.zombieDensityPerSquare[k + j * 256] = pOTLotHeader2.getZombieDensityForSquare(i * 300 + k, n3 * 300 + j);
                    }
                }
            }
        }
        pOTLotHeader.setZombieDensity(this.zombieDensityPerSquare);
        n3 = n + n2 * 1000;
        this.m_newLotHeader.put(n3, (Object)pOTLotHeader);
    }

    POTLotHeader getNewLotHeader(int n, int n2) {
        int n3 = n + n2 * 1000;
        POTLotHeader pOTLotHeader = (POTLotHeader)this.m_newLotHeader.get(n3);
        if (pOTLotHeader == null) {
            pOTLotHeader = new POTLotHeader(n, n2, true);
            this.m_newLotHeader.put(n3, (Object)pOTLotHeader);
        }
        return pOTLotHeader;
    }

    POTLotHeader getOldLotHeader(int n, int n2) {
        int n3 = n + n2 * 1000;
        File file = (File)this.m_lotHeaderFiles.get(n3);
        if (file == null) {
            return null;
        }
        POTLotHeader pOTLotHeader = (POTLotHeader)this.m_oldLotHeader.get(n3);
        if (pOTLotHeader == null) {
            pOTLotHeader = new POTLotHeader(n, n2, false);
            pOTLotHeader.load(file);
            this.m_oldLotHeader.put(n3, (Object)pOTLotHeader);
        }
        return pOTLotHeader;
    }

    POTLotPack getOldLotPack(POTLotHeader pOTLotHeader) throws IOException {
        int n = pOTLotHeader.x + pOTLotHeader.y * 1000;
        POTLotPack pOTLotPack = (POTLotPack)this.m_oldLotPack.get(n);
        if (pOTLotPack == null) {
            pOTLotPack = new POTLotPack(pOTLotHeader);
            File file = (File)this.m_lotPackFiles.get(n);
            pOTLotPack.load(file);
            this.m_oldLotPack.put(n, (Object)pOTLotPack);
        }
        return pOTLotPack;
    }

    void convertLotPack() throws IOException {
        for (int i = this.m_minY * 300; i < (this.m_maxY + 1) * 300; i += 256) {
            for (int j = this.m_minX * 300; j < (this.m_maxX + 1) * 300; j += 256) {
                int n;
                int n2 = j / 256;
                int n3 = i / 256;
                if (!this.shouldConvertNewCell(n2, n3)) continue;
                if (n3 == 30) {
                    n = 1;
                }
                this.convertLotPack(n2, n3);
                n = j / 300 - 1;
                int n4 = i / 300;
                for (int k = this.m_minY; k <= this.m_maxY; ++k) {
                    for (int i2 = this.m_minX; i2 <= this.m_maxX && (i2 != n || k != n4); ++i2) {
                        POTLotHeader pOTLotHeader;
                        POTLotPack pOTLotPack = (POTLotPack)this.m_oldLotPack.remove(i2 + k * 1000);
                        if (pOTLotPack != null) {
                            pOTLotPack.clear();
                        }
                        if ((pOTLotHeader = (POTLotHeader)this.m_oldLotHeader.remove(i2 + k * 1000)) == null) continue;
                        pOTLotHeader.clear();
                    }
                }
            }
        }
    }

    void convertLotPack(int n, int n2) throws IOException {
        POTLotHeader pOTLotHeader = this.getNewLotHeader(n, n2);
        if (pOTLotHeader == null) {
            return;
        }
        pOTLotHeader.minLevelNotEmpty = 1000;
        pOTLotHeader.maxLevelNotEmpty = -1000;
        POTLotPack pOTLotPack = new POTLotPack(pOTLotHeader);
        for (int i = -32; i <= 31; ++i) {
            int n3 = pOTLotHeader.getMaxSquareY();
            for (int j = pOTLotHeader.getMinSquareY(); j <= n3; ++j) {
                int n4 = pOTLotHeader.getMaxSquareX();
                for (int k = pOTLotHeader.getMinSquareX(); k <= n4; ++k) {
                    pOTLotPack.setSquareData(k, j, i, this.getOldLotPackSquareData(k, j, i));
                }
            }
        }
        pOTLotHeader.save(String.format("%s%s%d_%d.lotheader", this.m_mapDirectoryOut, File.separator, pOTLotHeader.x, pOTLotHeader.y));
        pOTLotPack.save(String.format("%s%sworld_%d_%d.lotpack", this.m_mapDirectoryOut, File.separator, pOTLotPack.x, pOTLotPack.y));
        this.m_newLotHeader.remove(n + n2 * 1000);
        pOTLotHeader.clear();
        pOTLotPack.clear();
    }

    String[] getOldLotPackSquareData(int n, int n2, int n3) throws IOException {
        POTLotHeader pOTLotHeader = this.getOldLotHeader(n / 300, n2 / 300);
        if (pOTLotHeader == null) {
            return null;
        }
        if (!pOTLotHeader.containsSquare(n, n2)) {
            return null;
        }
        if (n3 < pOTLotHeader.minLevel || n3 > pOTLotHeader.maxLevel) {
            return null;
        }
        POTLotPack pOTLotPack = this.getOldLotPack(pOTLotHeader);
        return pOTLotPack.getSquareData(n, n2, n3);
    }

    void convertChunkData() throws IOException {
        for (int i = this.m_minY * 300; i < (this.m_maxY + 1) * 300; i += 256) {
            for (int j = this.m_minX * 300; j < (this.m_maxX + 1) * 300; j += 256) {
                int n = j / 256;
                int n2 = i / 256;
                if (!this.shouldConvertNewCell(n, n2)) continue;
                this.convertChunkData(n, n2);
            }
        }
    }

    void convertChunkData(int n, int n2) throws IOException {
        POTChunkData pOTChunkData = new POTChunkData(n, n2, true);
        int n3 = pOTChunkData.getMaxSquareY();
        for (int i = pOTChunkData.getMinSquareY(); i <= n3; ++i) {
            int n4 = pOTChunkData.getMaxSquareX();
            for (int j = pOTChunkData.getMinSquareX(); j <= n4; ++j) {
                pOTChunkData.setSquareBits(j, i, this.getOldChunkDataBits(j, i));
            }
        }
        pOTChunkData.save(String.format("%s%schunkdata_%d_%d.bin", this.m_mapDirectoryOut, File.separator, pOTChunkData.x, pOTChunkData.y));
    }

    POTChunkData getOldChunkData(int n, int n2) throws IOException {
        int n3 = n + n2 * 1000;
        File file = (File)this.m_chunkDataFiles.get(n3);
        if (file == null) {
            return null;
        }
        POTChunkData pOTChunkData = (POTChunkData)this.m_oldChunkData.get(n3);
        if (pOTChunkData == null) {
            pOTChunkData = new POTChunkData(n, n2, false);
            pOTChunkData.load(file);
            this.m_oldChunkData.put(n3, (Object)pOTChunkData);
        }
        return pOTChunkData;
    }

    byte getOldChunkDataBits(int n, int n2) throws IOException {
        POTChunkData pOTChunkData = this.getOldChunkData(n / 300, n2 / 300);
        if (pOTChunkData == null) {
            return 0;
        }
        if (!pOTChunkData.containsSquare(n, n2)) {
            return 0;
        }
        return pOTChunkData.getSquareBits(n, n2);
    }

    void convertObjectsLua() {
    }

    void convertSpawnPointsLua() {
    }

    void convertWorldMapBIN(String string) throws Exception {
        String string2 = this.m_mapDirectoryIn + File.separator + string;
        File file = new File(string2);
        if (!file.exists()) {
            return;
        }
        WorldMapBinary worldMapBinary = new WorldMapBinary();
        WorldMapData worldMapData = new WorldMapData(new AssetPath(string2), (AssetManager)WorldMapDataAssetManager.instance);
        worldMapBinary.read(this.m_mapDirectoryIn + File.separator + string, worldMapData);
        worldMapData.onLoaded();
        POTWorldMapData pOTWorldMapData = new POTWorldMapData();
        for (int i = worldMapData.m_minY; i <= worldMapData.m_maxY; ++i) {
            for (int j = worldMapData.m_minX; j <= worldMapData.m_maxX; ++j) {
                WorldMapCell worldMapCell = worldMapData.getCell(j, i);
                if (worldMapCell == null) continue;
                for (WorldMapFeature worldMapFeature : worldMapCell.m_features) {
                    pOTWorldMapData.addFeature(worldMapFeature);
                }
            }
        }
        pOTWorldMapData.m_minX = Integer.MAX_VALUE;
        pOTWorldMapData.m_minY = Integer.MAX_VALUE;
        pOTWorldMapData.m_maxX = Integer.MIN_VALUE;
        pOTWorldMapData.m_maxY = Integer.MIN_VALUE;
        for (WorldMapCell worldMapCell : pOTWorldMapData.m_cells) {
            pOTWorldMapData.m_minX = Math.min(pOTWorldMapData.m_minX, worldMapCell.m_x);
            pOTWorldMapData.m_minY = Math.min(pOTWorldMapData.m_minY, worldMapCell.m_y);
            pOTWorldMapData.m_maxX = Math.max(pOTWorldMapData.m_maxX, worldMapCell.m_x);
            pOTWorldMapData.m_maxY = Math.max(pOTWorldMapData.m_maxY, worldMapCell.m_y);
        }
        pOTWorldMapData.saveBIN(this.m_mapDirectoryOut + File.separator + string, true);
    }

    void convertWorldMapXML() {
    }

    void convertNewMapBinaryDirectory(String string, String string2) throws IOException {
        File file = new File(string);
        File[] fileArray = file.listFiles();
        for (int i = 0; i < fileArray.length; ++i) {
            String string3 = fileArray[i].getName();
            String string4 = string3.substring(string3.lastIndexOf(46));
            if (!".pzby".equalsIgnoreCase(string4)) continue;
            this.convertNewMapBinaryFile(fileArray[i], new File(string2, fileArray[i].getName()));
        }
    }

    void convertNewMapBinaryFile(File file, File file2) throws IOException {
        NewMapBinaryFile newMapBinaryFile = new NewMapBinaryFile(false);
        NewMapBinaryFile.Header header = newMapBinaryFile.loadHeader(file.getAbsolutePath());
        for (int i = 0; i < header.m_height; ++i) {
            for (int j = 0; j < header.m_width; ++j) {
                NewMapBinaryFile.ChunkData chunkData = newMapBinaryFile.loadChunk(header, j, i);
            }
        }
    }

    public static void runOnStart() {
        POT pOT = new POT();
        try {
            RandStandard.INSTANCE.init();
            RandLua.INSTANCE.init();
        }
        catch (Exception exception) {
            exception.printStackTrace();
            SliceY.SliceBuffer.order(ByteOrder.BIG_ENDIAN);
        }
        System.exit(0);
    }
}
