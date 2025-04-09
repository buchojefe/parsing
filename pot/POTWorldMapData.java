/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  gnu.trove.map.hash.TObjectIntHashMap
 *  zombie.GameWindow
 *  zombie.core.math.PZMath
 *  zombie.worldMap.WorldMapCell
 *  zombie.worldMap.WorldMapFeature
 *  zombie.worldMap.WorldMapGeometry
 *  zombie.worldMap.WorldMapPoints
 *  zombie.worldMap.WorldMapProperties
 */
package zombie.pot;

import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import zombie.GameWindow;
import zombie.core.math.PZMath;
import zombie.worldMap.WorldMapCell;
import zombie.worldMap.WorldMapFeature;
import zombie.worldMap.WorldMapGeometry;
import zombie.worldMap.WorldMapPoints;
import zombie.worldMap.WorldMapProperties;

public final class POTWorldMapData {
    static final int VERSION1 = 1;
    static final int VERSION2 = 2;
    static final int VERSION_LATEST = 2;
    static final TObjectIntHashMap<String> m_stringTable = new TObjectIntHashMap();
    public final ArrayList<WorldMapCell> m_cells = new ArrayList();
    public final HashMap<Integer, WorldMapCell> m_cellLookup = new HashMap();
    public int m_minX;
    public int m_minY;
    public int m_maxX;
    public int m_maxY;

    public WorldMapCell getCell(int n, int n2) {
        Integer n3 = this.getCellKey(n, n2);
        return this.m_cellLookup.get(n3);
    }

    private Integer getCellKey(int n, int n2) {
        return n + n2 * 1000;
    }

    public void addFeature(WorldMapFeature worldMapFeature) {
        int n = this.getMinSquareX(worldMapFeature) / 256;
        int n2 = this.getMinSquareY(worldMapFeature) / 256;
        int n3 = this.getMaxSquareX(worldMapFeature) / 256;
        int n4 = this.getMaxSquareY(worldMapFeature) / 256;
        for (int i = n2; i <= n4; ++i) {
            for (int j = n; j <= n3; ++j) {
                WorldMapCell worldMapCell = this.getCell(j, i);
                if (worldMapCell == null) {
                    worldMapCell = new WorldMapCell();
                    worldMapCell.m_x = j;
                    worldMapCell.m_y = i;
                    this.m_cells.add(worldMapCell);
                    this.m_cellLookup.put(this.getCellKey(j, i), worldMapCell);
                }
                WorldMapFeature worldMapFeature2 = new WorldMapFeature(worldMapCell);
                this.convertFeature(worldMapFeature2, worldMapFeature);
                worldMapCell.m_features.add(worldMapFeature2);
            }
        }
    }

    public void saveBIN(String string, boolean bl) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(0x1E00000);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.clear();
        byteBuffer.put((byte)73);
        byteBuffer.put((byte)71);
        byteBuffer.put((byte)77);
        byteBuffer.put((byte)66);
        byteBuffer.putInt(2);
        byteBuffer.putInt(bl ? 256 : 300);
        byteBuffer.putInt(this.getWidthInCells());
        byteBuffer.putInt(this.getHeightInCells());
        this.writeStringTable(byteBuffer);
        for (int i = this.m_minY; i <= this.m_maxY; ++i) {
            for (int j = this.m_minX; j <= this.m_maxX; ++j) {
                WorldMapCell worldMapCell = this.getCell(j, i);
                if (worldMapCell == null) {
                    byteBuffer.putInt(-1);
                    continue;
                }
                this.writeCell(byteBuffer, worldMapCell);
            }
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(string);){
            fileOutputStream.write(byteBuffer.array(), 0, byteBuffer.position());
        }
    }

    void writeStringTable(ByteBuffer byteBuffer) {
        m_stringTable.clear();
        ArrayList<String> arrayList = new ArrayList<String>();
        for (int i = this.m_minY; i <= this.m_maxY; ++i) {
            for (int j = this.m_minX; j <= this.m_maxX; ++j) {
                WorldMapCell worldMapCell = this.getCell(j, i);
                if (worldMapCell == null) continue;
                for (WorldMapFeature worldMapFeature : worldMapCell.m_features) {
                    this.addString(arrayList, ((WorldMapGeometry)worldMapFeature.m_geometries.get((int)0)).m_type.name());
                    for (Map.Entry entry : worldMapFeature.m_properties.entrySet()) {
                        this.addString(arrayList, (String)entry.getKey());
                        this.addString(arrayList, (String)entry.getValue());
                    }
                }
            }
        }
        byteBuffer.putInt(arrayList.size());
        for (String string : arrayList) {
            this.SaveString(byteBuffer, string);
        }
    }

    void addString(ArrayList<String> arrayList, String string) {
        if (m_stringTable.containsKey((Object)string)) {
            return;
        }
        m_stringTable.put((Object)string, arrayList.size());
        arrayList.add(string);
    }

    void writeCell(ByteBuffer byteBuffer, WorldMapCell worldMapCell) {
        if (worldMapCell.m_features.isEmpty()) {
            byteBuffer.putInt(-1);
            return;
        }
        byteBuffer.putInt(worldMapCell.m_x);
        byteBuffer.putInt(worldMapCell.m_y);
        byteBuffer.putInt(worldMapCell.m_features.size());
        for (WorldMapFeature worldMapFeature : worldMapCell.m_features) {
            this.writeFeature(byteBuffer, worldMapFeature);
        }
    }

    void writeFeature(ByteBuffer byteBuffer, WorldMapFeature worldMapFeature) {
        WorldMapGeometry worldMapGeometry = (WorldMapGeometry)worldMapFeature.m_geometries.get(0);
        this.SaveStringIndex(byteBuffer, worldMapGeometry.m_type.name());
        byteBuffer.put((byte)worldMapGeometry.m_points.size());
        for (Object object : worldMapGeometry.m_points) {
            byteBuffer.putShort((short)object.numPoints());
            for (int i = 0; i < object.numPoints(); ++i) {
                byteBuffer.putShort((short)object.getX(i));
                byteBuffer.putShort((short)object.getY(i));
            }
        }
        byteBuffer.put((byte)worldMapFeature.m_properties.size());
        for (Object object : worldMapFeature.m_properties.entrySet()) {
            this.SaveStringIndex(byteBuffer, (String)object.getKey());
            this.SaveStringIndex(byteBuffer, (String)object.getValue());
        }
    }

    void SaveString(ByteBuffer byteBuffer, String string) {
        GameWindow.WriteStringUTF((ByteBuffer)byteBuffer, (String)string);
    }

    void SaveStringIndex(ByteBuffer byteBuffer, String string) {
        byteBuffer.putShort((short)m_stringTable.get((Object)string));
    }

    int getMinSquareX(WorldMapFeature worldMapFeature) {
        int n = Integer.MAX_VALUE;
        for (WorldMapGeometry worldMapGeometry : worldMapFeature.m_geometries) {
            n = PZMath.min((int)n, (int)worldMapGeometry.m_minX);
        }
        return worldMapFeature.m_cell.m_x * 300 + n;
    }

    int getMinSquareY(WorldMapFeature worldMapFeature) {
        int n = Integer.MAX_VALUE;
        for (WorldMapGeometry worldMapGeometry : worldMapFeature.m_geometries) {
            n = PZMath.min((int)n, (int)worldMapGeometry.m_minY);
        }
        return worldMapFeature.m_cell.m_y * 300 + n;
    }

    int getMaxSquareX(WorldMapFeature worldMapFeature) {
        int n = Integer.MAX_VALUE;
        for (WorldMapGeometry worldMapGeometry : worldMapFeature.m_geometries) {
            n = PZMath.min((int)n, (int)worldMapGeometry.m_maxX);
        }
        return worldMapFeature.m_cell.m_x * 300 + n;
    }

    int getMaxSquareY(WorldMapFeature worldMapFeature) {
        int n = Integer.MAX_VALUE;
        for (WorldMapGeometry worldMapGeometry : worldMapFeature.m_geometries) {
            n = PZMath.min((int)n, (int)worldMapGeometry.m_maxY);
        }
        return worldMapFeature.m_cell.m_y * 300 + n;
    }

    public int getWidthInCells() {
        return this.m_maxX - this.m_minX + 1;
    }

    public int getHeightInCells() {
        return this.m_maxY - this.m_minY + 1;
    }

    void convertFeature(WorldMapFeature worldMapFeature, WorldMapFeature worldMapFeature2) {
        for (WorldMapGeometry worldMapGeometry : worldMapFeature2.m_geometries) {
            WorldMapGeometry worldMapGeometry2 = new WorldMapGeometry();
            worldMapGeometry2.m_type = worldMapGeometry.m_type;
            for (WorldMapPoints worldMapPoints : worldMapGeometry.m_points) {
                WorldMapPoints worldMapPoints2 = new WorldMapPoints();
                for (int i = 0; i < worldMapPoints.numPoints(); ++i) {
                    int n = worldMapFeature2.m_cell.m_x * 300 + worldMapPoints.getX(i);
                    int n2 = worldMapFeature2.m_cell.m_y * 300 + worldMapPoints.getY(i);
                    int n3 = n - worldMapFeature.m_cell.m_x * 256;
                    int n4 = n2 - worldMapFeature.m_cell.m_y * 256;
                    worldMapPoints2.add(n3);
                    worldMapPoints2.add(n4);
                }
                worldMapGeometry2.m_points.add(worldMapPoints2);
            }
            worldMapGeometry2.calculateBounds();
            worldMapFeature.m_geometries.add(worldMapGeometry2);
        }
        worldMapFeature.m_properties = new WorldMapProperties();
        worldMapFeature.m_properties.putAll((Map)worldMapFeature2.m_properties);
    }
}
