/*
 * Decompiled with CFR.
 */
package zombie.iso;

import gnu.trove.TIntCollection;
import gnu.trove.list.array.TIntArrayList;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Boolean;
import java.lang.CharSequence;
import java.lang.Deprecated;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Float;
import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.InterruptedException;
import java.lang.Math;
import java.lang.Object;
import java.lang.RuntimeException;
import java.lang.Short;
import java.lang.String;
import java.lang.System;
import java.lang.Thread;
import java.lang.ThreadLocal;
import java.lang.Throwable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.MapGroups;
import zombie.SandboxOptions;
import zombie.ZomboidFileSystem;
import zombie.characters.Faction;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.Rand;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.stash.StashSystem;
import zombie.debug.DebugLog;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.BuildingDef;
import zombie.iso.IsoLot;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaChunk;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LotHeader;
import zombie.iso.RoomDef;
import zombie.iso.SliceY;
import zombie.iso.Vector2;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.iso.objects.IsoMannequin;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedBuilding.RBBasic;
import zombie.util.SharedStrings;
import zombie.util.StringUtils;
import zombie.vehicles.Clipper;
import zombie.vehicles.ClipperOffset;

public final class IsoMetaGrid {
    private static final int NUM_LOADER_THREADS = 8;
    private static ArrayList<String> s_PreferredZoneTypes;
    private static Clipper s_clipper;
    private static ClipperOffset s_clipperOffset;
    private static ByteBuffer s_clipperBuffer;
    private static final ThreadLocal<IsoGameCharacter.Location> TL_Location;
    private static final ThreadLocal<ArrayList<Zone>> TL_ZoneList;
    static Rectangle a;
    static Rectangle b;
    static ArrayList<RoomDef> roomChoices;
    private final ArrayList<RoomDef> tempRooms;
    private final ArrayList<Zone> tempZones1;
    private final ArrayList<Zone> tempZones2;
    private final MetaGridLoaderThread[] threads;
    public int minX;
    public int minY;
    public int maxX;
    public int maxY;
    public final ArrayList<Zone> Zones;
    public final ArrayList<BuildingDef> Buildings;
    public final ArrayList<VehicleZone> VehiclesZones;
    public IsoMetaCell[][] Grid;
    public final ArrayList<IsoGameCharacter> MetaCharacters;
    final ArrayList<Vector2> HighZombieList;
    private int width;
    private int height;
    private final SharedStrings sharedStrings;
    private long createStartTime;

    public IsoMetaGrid() {
        super();
        this.tempRooms = new ArrayList();
        this.tempZones1 = new ArrayList();
        this.tempZones2 = new ArrayList();
        this.threads = new MetaGridLoaderThread[8];
        this.minX = 10000000;
        this.minY = 10000000;
        this.maxX = -10000000;
        this.maxY = -10000000;
        this.Zones = new ArrayList();
        this.Buildings = new ArrayList();
        this.VehiclesZones = new ArrayList();
        this.MetaCharacters = new ArrayList();
        this.HighZombieList = new ArrayList();
        this.sharedStrings = new SharedStrings();
    }

    public void AddToMeta(IsoGameCharacter isoGameCharacter) {
        IsoWorld.instance.CurrentCell.Remove((IsoMovingObject)isoGameCharacter);
        if (!this.MetaCharacters.contains((Object)isoGameCharacter)) {
            this.MetaCharacters.add((Object)isoGameCharacter);
        }
    }

    public void RemoveFromMeta(IsoPlayer isoPlayer) {
        this.MetaCharacters.remove((Object)isoPlayer);
        if (!IsoWorld.instance.CurrentCell.getObjectList().contains((Object)isoPlayer)) {
            IsoWorld.instance.CurrentCell.getObjectList().add((Object)isoPlayer);
        }
    }

    public int getMinX() {
        return this.minX;
    }

    public int getMinY() {
        return this.minY;
    }

    public int getMaxX() {
        return this.maxX;
    }

    public int getMaxY() {
        return this.maxY;
    }

    public Zone getZoneAt(int n, int n2, int n3) {
        IsoMetaChunk isoMetaChunk = this.getChunkDataFromTile((int)n, (int)n2);
        if (isoMetaChunk != null) {
            return isoMetaChunk.getZoneAt((int)n, (int)n2, (int)n3);
        }
        return null;
    }

    public ArrayList getZonesAt(int n, int n2, int n3) {
        return this.getZonesAt((int)n, (int)n2, (int)n3, (ArrayList)new ArrayList());
    }

    public ArrayList getZonesAt(int n, int n2, int n3, ArrayList arrayList) {
        IsoMetaChunk isoMetaChunk = this.getChunkDataFromTile((int)n, (int)n2);
        if (isoMetaChunk != null) {
            return isoMetaChunk.getZonesAt((int)n, (int)n2, (int)n3, (ArrayList)arrayList);
        }
        return arrayList;
    }

    public ArrayList getZonesIntersecting(int n, int n2, int n3, int n4, int n5) {
        ArrayList arrayList = new ArrayList();
        return this.getZonesIntersecting((int)n, (int)n2, (int)n3, (int)n4, (int)n5, (ArrayList)arrayList);
    }

    public ArrayList getZonesIntersecting(int n, int n2, int n3, int n4, int n5, ArrayList arrayList) {
        for (int i = n2 / 300; i <= (n2 + n5) / 300; ++i) {
            for (int j = n / 300; j <= (n + n4) / 300; ++j) {
                if (j < this.minX || j > this.maxX || i < this.minY || i > this.maxY || this.Grid[j - this.minX][i - this.minY] == null) continue;
                this.Grid[j - this.minX][i - this.minY].getZonesIntersecting((int)n, (int)n2, (int)n3, (int)n4, (int)n5, (ArrayList)arrayList);
            }
        }
        return arrayList;
    }

    public Zone getZoneWithBoundsAndType(int n, int n2, int n3, int n4, int n5, String string) {
        ArrayList arrayList = (ArrayList)TL_ZoneList.get();
        arrayList.clear();
        this.getZonesIntersecting((int)n, (int)n2, (int)n3, (int)n4, (int)n5, (ArrayList)arrayList);
        for (int i = 0; i < arrayList.size(); ++i) {
            Zone zone = (Zone)arrayList.get((int)i);
            if (zone.x != n || zone.y != n2 || zone.z != n3 || zone.w != n4 || zone.h != n5 || !StringUtils.equalsIgnoreCase((String)zone.type, (String)string)) continue;
            return zone;
        }
        return null;
    }

    public VehicleZone getVehicleZoneAt(int n, int n2, int n3) {
        IsoMetaCell isoMetaCell = this.getMetaGridFromTile((int)n, (int)n2);
        if (isoMetaCell == null || isoMetaCell.vehicleZones.isEmpty()) {
            return null;
        }
        for (int i = 0; i < isoMetaCell.vehicleZones.size(); ++i) {
            VehicleZone vehicleZone = (VehicleZone)isoMetaCell.vehicleZones.get((int)i);
            if (!vehicleZone.contains((int)n, (int)n2, (int)n3)) continue;
            return vehicleZone;
        }
        return null;
    }

    public BuildingDef getBuildingAt(int n, int n2) {
        for (int i = 0; i < this.Buildings.size(); ++i) {
            BuildingDef buildingDef = (BuildingDef)this.Buildings.get((int)i);
            if (buildingDef.x > n || buildingDef.y > n2 || buildingDef.getW() <= n - buildingDef.x || buildingDef.getH() <= n2 - buildingDef.y) continue;
            return buildingDef;
        }
        return null;
    }

    public BuildingDef getBuildingAtRelax(int n, int n2) {
        for (int i = 0; i < this.Buildings.size(); ++i) {
            BuildingDef buildingDef = (BuildingDef)this.Buildings.get((int)i);
            if (buildingDef.x > n + 1 || buildingDef.y > n2 + 1 || buildingDef.getW() <= n - buildingDef.x - 1 || buildingDef.getH() <= n2 - buildingDef.y - 1) continue;
            return buildingDef;
        }
        return null;
    }

    public RoomDef getRoomAt(int n, int n2, int n3) {
        IsoMetaChunk isoMetaChunk = this.getChunkDataFromTile((int)n, (int)n2);
        if (isoMetaChunk != null) {
            return isoMetaChunk.getRoomAt((int)n, (int)n2, (int)n3);
        }
        return null;
    }

    public RoomDef getEmptyOutsideAt(int n, int n2, int n3) {
        IsoMetaChunk isoMetaChunk = this.getChunkDataFromTile((int)n, (int)n2);
        if (isoMetaChunk != null) {
            return isoMetaChunk.getEmptyOutsideAt((int)n, (int)n2, (int)n3);
        }
        return null;
    }

    public void getRoomsIntersecting(int n, int n2, int n3, int n4, ArrayList arrayList) {
        for (int i = n2 / 300; i <= (n2 + this.height) / 300; ++i) {
            for (int j = n / 300; j <= (n + this.width) / 300; ++j) {
                IsoMetaCell isoMetaCell;
                if (j < this.minX || j > this.maxX || i < this.minY || i > this.maxY || (isoMetaCell = this.Grid[j - this.minX][i - this.minY]) == null) continue;
                isoMetaCell.getRoomsIntersecting((int)n, (int)n2, (int)n3, (int)n4, (ArrayList)arrayList);
            }
        }
    }

    public int countRoomsIntersecting(int n, int n2, int n3, int n4) {
        this.tempRooms.clear();
        for (int i = n2 / 300; i <= (n2 + this.height) / 300; ++i) {
            for (int j = n / 300; j <= (n + this.width) / 300; ++j) {
                IsoMetaCell isoMetaCell;
                if (j < this.minX || j > this.maxX || i < this.minY || i > this.maxY || (isoMetaCell = this.Grid[j - this.minX][i - this.minY]) == null) continue;
                isoMetaCell.getRoomsIntersecting((int)n, (int)n2, (int)n3, (int)n4, this.tempRooms);
            }
        }
        return this.tempRooms.size();
    }

    public int countNearbyBuildingsRooms(IsoPlayer isoPlayer) {
        int n = (int)isoPlayer.getX() - 20;
        int n2 = (int)isoPlayer.getY() - 20;
        int n3 = 40;
        int n4 = 40;
        int n5 = this.countRoomsIntersecting((int)n, (int)n2, (int)n3, (int)n4);
        return n5;
    }

    private boolean isInside(Zone zone, BuildingDef buildingDef) {
        IsoMetaGrid.a.x = zone.x;
        IsoMetaGrid.a.y = zone.y;
        IsoMetaGrid.a.width = zone.w;
        IsoMetaGrid.a.height = zone.h;
        IsoMetaGrid.b.x = buildingDef.x;
        IsoMetaGrid.b.y = buildingDef.y;
        IsoMetaGrid.b.width = buildingDef.getW();
        IsoMetaGrid.b.height = buildingDef.getH();
        return a.contains((Rectangle)b);
    }

    private boolean isAdjacent(Zone zone, Zone zone2) {
        if (zone == zone2) {
            return false;
        }
        IsoMetaGrid.a.x = zone.x;
        IsoMetaGrid.a.y = zone.y;
        IsoMetaGrid.a.width = zone.w;
        IsoMetaGrid.a.height = zone.h;
        IsoMetaGrid.b.x = zone2.x;
        IsoMetaGrid.b.y = zone2.y;
        IsoMetaGrid.b.width = zone2.w;
        IsoMetaGrid.b.height = zone2.h;
        --IsoMetaGrid.a.x;
        --IsoMetaGrid.a.y;
        IsoMetaGrid.a.width += 2;
        IsoMetaGrid.a.height += 2;
        --IsoMetaGrid.b.x;
        --IsoMetaGrid.b.y;
        IsoMetaGrid.b.width += 2;
        IsoMetaGrid.b.height += 2;
        return a.intersects((Rectangle)b);
    }

    public Zone registerZone(String string, String string2, int n, int n2, int n3, int n4, int n5) {
        return this.registerZone((String)string, (String)string2, (int)n, (int)n2, (int)n3, (int)n4, (int)n5, (ZoneGeometryType)ZoneGeometryType.INVALID, null, (int)0);
    }

    public Zone registerZone(String string, String string2, int n, int n2, int n3, int n4, int n5, ZoneGeometryType zoneGeometryType, TIntArrayList tIntArrayList, int n6) {
        string = this.sharedStrings.get((String)string);
        string2 = this.sharedStrings.get((String)string2);
        Zone zone = new Zone((String)string, (String)string2, (int)n, (int)n2, (int)n3, (int)n4, (int)n5);
        zone.geometryType = zoneGeometryType;
        if (tIntArrayList != null) {
            zone.points.addAll((TIntCollection)tIntArrayList);
            zone.polylineWidth = n6;
        }
        zone.isPreferredZoneForSquare = IsoMetaGrid.isPreferredZoneForSquare((String)string2);
        if (n < this.minX * 300 - 100 || n2 < this.minY * 300 - 100 || n + n4 > (this.maxX + 1) * 300 + 100 || n2 + n5 > (this.maxY + 1) * 300 + 100 || n3 < 0 || n3 >= 8 || n4 > 600 || n5 > 600) {
            return zone;
        }
        this.addZone((Zone)zone);
        return zone;
    }

    /*
     * Exception decompiling
     */
    public Zone registerGeometryZone(String var1_1, String var2_2, int var3_3, String var4_4, KahluaTable var5_5, KahluaTable var6_6) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SwitchStringRewriter$TooOptimisticMatchException
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SwitchStringRewriter.getString(SwitchStringRewriter.java:404)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SwitchStringRewriter.access$600(SwitchStringRewriter.java:53)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SwitchStringRewriter$SwitchStringMatchResultCollector.collectMatches(SwitchStringRewriter.java:368)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.ResetAfterTest.match(ResetAfterTest.java:24)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.KleeneN.match(KleeneN.java:24)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchSequence.match(MatchSequence.java:26)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.ResetAfterTest.match(ResetAfterTest.java:23)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SwitchStringRewriter.rewriteComplex(SwitchStringRewriter.java:201)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SwitchStringRewriter.rewrite(SwitchStringRewriter.java:73)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:881)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at com.heliosdecompiler.transformerapi.decompilers.cfr.CFRDecompiler.decompile(CFRDecompiler.java:43)
         *     at com.heliosdecompiler.transformerapi.StandardTransformers$Decompilers.decompile(StandardTransformers.java:74)
         *     at com.heliosdecompiler.transformerapi.StandardTransformers.decompile(StandardTransformers.java:46)
         *     at org.jd.gui.view.component.ClassFilePage.save(ClassFilePage.java:161)
         *     at org.jd.gui.view.component.DynamicPage.save(DynamicPage.java:85)
         *     at org.jd.gui.controller.MainController.save(MainController.java:306)
         *     at org.jd.gui.controller.MainController.onSaveSource(MainController.java:298)
         *     at org.jd.gui.controller.MainController.lambda$new$3(MainController.java:148)
         *     at org.jd.gui.util.swing.SwingUtil$1.actionPerformed(SwingUtil.java:42)
         *     at java.desktop/javax.swing.AbstractButton.fireActionPerformed(Unknown Source)
         *     at java.desktop/javax.swing.AbstractButton$Handler.actionPerformed(Unknown Source)
         *     at java.desktop/javax.swing.DefaultButtonModel.fireActionPerformed(Unknown Source)
         *     at java.desktop/javax.swing.DefaultButtonModel.setPressed(Unknown Source)
         *     at java.desktop/javax.swing.AbstractButton.doClick(Unknown Source)
         *     at java.desktop/javax.swing.plaf.basic.BasicMenuItemUI.doClick(Unknown Source)
         *     at java.desktop/javax.swing.plaf.basic.BasicMenuItemUI$Handler.mouseReleased(Unknown Source)
         *     at java.desktop/java.awt.Component.processMouseEvent(Unknown Source)
         *     at java.desktop/javax.swing.JComponent.processMouseEvent(Unknown Source)
         *     at java.desktop/java.awt.Component.processEvent(Unknown Source)
         *     at java.desktop/java.awt.Container.processEvent(Unknown Source)
         *     at java.desktop/java.awt.Component.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.Container.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.Component.dispatchEvent(Unknown Source)
         *     at java.desktop/java.awt.LightweightDispatcher.retargetMouseEvent(Unknown Source)
         *     at java.desktop/java.awt.LightweightDispatcher.processMouseEvent(Unknown Source)
         *     at java.desktop/java.awt.LightweightDispatcher.dispatchEvent(Unknown Source)
         *     at java.desktop/java.awt.Container.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.Window.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.Component.dispatchEvent(Unknown Source)
         *     at java.desktop/java.awt.EventQueue.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.EventQueue$4.run(Unknown Source)
         *     at java.desktop/java.awt.EventQueue$4.run(Unknown Source)
         *     at java.base/java.security.AccessController.doPrivileged(Unknown Source)
         *     at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(Unknown Source)
         *     at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(Unknown Source)
         *     at java.desktop/java.awt.EventQueue$5.run(Unknown Source)
         *     at java.desktop/java.awt.EventQueue$5.run(Unknown Source)
         *     at java.base/java.security.AccessController.doPrivileged(Unknown Source)
         *     at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(Unknown Source)
         *     at java.desktop/java.awt.EventQueue.dispatchEvent(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpOneEventForFilters(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpEventsForFilter(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpEventsForHierarchy(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpEvents(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpEvents(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.run(Unknown Source)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private void calculatePolylineOutlineBounds(TIntArrayList tIntArrayList, int n, int[] nArray) {
        int n2;
        int n3;
        if (s_clipperOffset == null) {
            s_clipperOffset = new ClipperOffset();
            s_clipperBuffer = ByteBuffer.allocateDirect((int)3072);
        }
        s_clipperOffset.clear();
        s_clipperBuffer.clear();
        float f = n % 2 == 0 ? 0.0f : 0.5f;
        for (n3 = 0; n3 < tIntArrayList.size(); n3 += 2) {
            n2 = tIntArrayList.get((int)n3);
            int n4 = tIntArrayList.get((int)(n3 + 1));
            s_clipperBuffer.putFloat((float)((float)n2 + f));
            s_clipperBuffer.putFloat((float)((float)n4 + f));
        }
        s_clipperBuffer.flip();
        s_clipperOffset.addPath((int)(tIntArrayList.size() / 2), (ByteBuffer)s_clipperBuffer, (int)ClipperOffset.JoinType.jtMiter.ordinal(), (int)ClipperOffset.EndType.etOpenButt.ordinal());
        s_clipperOffset.execute((double)((double)((float)n / 2.0f)));
        n3 = s_clipperOffset.getPolygonCount();
        if (n3 < 1) {
            DebugLog.General.warn((Object)"Failed to generate polyline outline");
            return;
        }
        s_clipperBuffer.clear();
        s_clipperOffset.getPolygon((int)0, (ByteBuffer)s_clipperBuffer);
        n2 = (int)s_clipperBuffer.getShort();
        float f2 = Float.MAX_VALUE;
        float f3 = Float.MAX_VALUE;
        float f4 = -3.4028235E38f;
        float f5 = -3.4028235E38f;
        for (int i = 0; i < n2; ++i) {
            float f6 = s_clipperBuffer.getFloat();
            float f7 = s_clipperBuffer.getFloat();
            f2 = PZMath.min((float)f2, (float)f6);
            f3 = PZMath.min((float)f3, (float)f7);
            f4 = PZMath.max((float)f4, (float)f6);
            f5 = PZMath.max((float)f5, (float)f7);
        }
        nArray[0] = (int)PZMath.floor((float)f2);
        nArray[1] = (int)PZMath.floor((float)f3);
        nArray[2] = (int)PZMath.ceil((float)f4);
        nArray[3] = (int)PZMath.ceil((float)f5);
    }

    @Deprecated
    public Zone registerZoneNoOverlap(String string, String string2, int n, int n2, int n3, int n4, int n5) {
        if (n < this.minX * 300 - 100 || n2 < this.minY * 300 - 100 || n + n4 > (this.maxX + 1) * 300 + 100 || n2 + n5 > (this.maxY + 1) * 300 + 100 || n3 < 0 || n3 >= 8 || n4 > 600 || n5 > 600) {
            return null;
        }
        return this.registerZone((String)string, (String)string2, (int)n, (int)n2, (int)n3, (int)n4, (int)n5);
    }

    private void addZone(Zone zone) {
        this.Zones.add((Object)zone);
        for (int i = zone.y / 300; i <= (zone.y + zone.h) / 300; ++i) {
            for (int j = zone.x / 300; j <= (zone.x + zone.w) / 300; ++j) {
                if (j < this.minX || j > this.maxX || i < this.minY || i > this.maxY || this.Grid[j - this.minX][i - this.minY] == null) continue;
                this.Grid[j - this.minX][i - this.minY].addZone((Zone)zone, (int)(j * 300), (int)(i * 300));
            }
        }
    }

    public void removeZone(Zone zone) {
        this.Zones.remove((Object)zone);
        for (int i = zone.y / 300; i <= (zone.y + zone.h) / 300; ++i) {
            for (int j = zone.x / 300; j <= (zone.x + zone.w) / 300; ++j) {
                if (j < this.minX || j > this.maxX || i < this.minY || i > this.maxY || this.Grid[j - this.minX][i - this.minY] == null) continue;
                this.Grid[j - this.minX][i - this.minY].removeZone((Zone)zone);
            }
        }
    }

    public void removeZonesForCell(int n, int n2) {
        int n3;
        IsoMetaCell isoMetaCell = this.getCellData((int)n, (int)n2);
        if (isoMetaCell == null) {
            return;
        }
        ArrayList<Zone> arrayList = this.tempZones1;
        arrayList.clear();
        for (n3 = 0; n3 < 900; ++n3) {
            isoMetaCell.ChunkMap[n3].getZonesIntersecting((int)(n * 300), (int)(n2 * 300), (int)0, (int)300, (int)300, arrayList);
        }
        for (n3 = 0; n3 < arrayList.size(); ++n3) {
            ArrayList<Zone> arrayList2;
            Zone zone = (Zone)arrayList.get((int)n3);
            if (!zone.difference((int)(n * 300), (int)(n2 * 300), (int)0, (int)300, (int)300, arrayList2 = this.tempZones2)) continue;
            this.removeZone((Zone)zone);
            for (int i = 0; i < arrayList2.size(); ++i) {
                this.addZone((Zone)((Zone)arrayList2.get((int)i)));
            }
        }
        if (!isoMetaCell.vehicleZones.isEmpty()) {
            isoMetaCell.vehicleZones.clear();
        }
        if (!isoMetaCell.mannequinZones.isEmpty()) {
            isoMetaCell.mannequinZones.clear();
        }
    }

    public void removeZonesForLotDirectory(String string) {
        if (this.Zones.isEmpty()) {
            return;
        }
        File file = new File((String)ZomboidFileSystem.instance.getString((String)("media/maps/" + string + "/")));
        if (!file.isDirectory()) {
            return;
        }
        ChooseGameInfo.Map map = ChooseGameInfo.getMapDetails((String)string);
        if (map == null) {
            return;
        }
        String[] stringArray = file.list();
        if (stringArray == null) {
            return;
        }
        for (int i = 0; i < stringArray.length; ++i) {
            String string2 = stringArray[i];
            if (!string2.endsWith((String)".lotheader")) continue;
            String[] stringArray2 = string2.split((String)"_");
            stringArray2[1] = stringArray2[1].replace((CharSequence)".lotheader", (CharSequence)"");
            int n = Integer.parseInt((String)stringArray2[0].trim());
            int n2 = Integer.parseInt((String)stringArray2[1].trim());
            this.removeZonesForCell((int)n, (int)n2);
        }
    }

    public void processZones() {
        int n = 0;
        for (int i = this.minX; i <= this.maxX; ++i) {
            for (int j = this.minY; j <= this.maxY; ++j) {
                if (this.Grid[i - this.minX][j - this.minY] == null) continue;
                for (int k = 0; k < 30; ++k) {
                    for (int i2 = 0; i2 < 30; ++i2) {
                        n = Math.max((int)n, (int)this.Grid[i - this.minX][j - this.minY].getChunk((int)i2, (int)k).numZones());
                    }
                }
            }
        }
        DebugLog.log((String)("Max #ZONES on one chunk is " + n));
    }

    public Zone registerVehiclesZone(String string, String string2, int n, int n2, int n3, int n4, int n5, KahluaTable kahluaTable) {
        if (string2.equals((Object)"Vehicle") || string2.equals((Object)"ParkingStall")) {
            string = this.sharedStrings.get((String)string);
            string2 = this.sharedStrings.get((String)string2);
            VehicleZone vehicleZone = new VehicleZone((String)string, (String)string2, (int)n, (int)n2, (int)n3, (int)n4, (int)n5, (KahluaTable)kahluaTable);
            this.VehiclesZones.add((Object)vehicleZone);
            int n6 = (int)Math.ceil((double)((double)((float)(vehicleZone.x + vehicleZone.w) / 300.0f)));
            int n7 = (int)Math.ceil((double)((double)((float)(vehicleZone.y + vehicleZone.h) / 300.0f)));
            for (int i = vehicleZone.y / 300; i < n7; ++i) {
                for (int j = vehicleZone.x / 300; j < n6; ++j) {
                    if (j < this.minX || j > this.maxX || i < this.minY || i > this.maxY || this.Grid[j - this.minX][i - this.minY] == null) continue;
                    this.Grid[j - this.minX][i - this.minY].vehicleZones.add((Object)vehicleZone);
                }
            }
            return vehicleZone;
        }
        return null;
    }

    public void checkVehiclesZones() {
        int n = 0;
        while (n < this.VehiclesZones.size()) {
            boolean bl = true;
            for (int i = 0; i < n; ++i) {
                Zone zone = (Zone)this.VehiclesZones.get((int)n);
                Zone zone2 = (Zone)this.VehiclesZones.get((int)i);
                if (zone.getX() != zone2.getX() || zone.getY() != zone2.getY() || zone.h != zone2.h || zone.w != zone2.w) continue;
                bl = false;
                DebugLog.log((String)("checkVehiclesZones: ERROR! Zone '" + zone.name + "':'" + zone.type + "' (" + zone.x + ", " + zone.y + ") duplicate with Zone '" + zone2.name + "':'" + zone2.type + "' (" + zone2.x + ", " + zone2.y + ")"));
                break;
            }
            if (bl) {
                ++n;
                continue;
            }
            this.VehiclesZones.remove((int)n);
        }
    }

    public Zone registerMannequinZone(String string, String string2, int n, int n2, int n3, int n4, int n5, KahluaTable kahluaTable) {
        if ("Mannequin".equals((Object)string2)) {
            string = this.sharedStrings.get((String)string);
            string2 = this.sharedStrings.get((String)string2);
            IsoMannequin.MannequinZone mannequinZone = new IsoMannequin.MannequinZone((String)string, (String)string2, (int)n, (int)n2, (int)n3, (int)n4, (int)n5, (KahluaTable)kahluaTable);
            int n6 = (int)Math.ceil((double)((double)((float)(mannequinZone.x + mannequinZone.w) / 300.0f)));
            int n7 = (int)Math.ceil((double)((double)((float)(mannequinZone.y + mannequinZone.h) / 300.0f)));
            for (int i = mannequinZone.y / 300; i < n7; ++i) {
                for (int j = mannequinZone.x / 300; j < n6; ++j) {
                    if (j < this.minX || j > this.maxX || i < this.minY || i > this.maxY || this.Grid[j - this.minX][i - this.minY] == null) continue;
                    this.Grid[j - this.minX][i - this.minY].mannequinZones.add((Object)mannequinZone);
                }
            }
            return mannequinZone;
        }
        return null;
    }

    public void registerRoomTone(String string, String string2, int n, int n2, int n3, int n4, int n5, KahluaTable kahluaTable) {
        if (!"RoomTone".equals((Object)string2)) {
            return;
        }
        IsoMetaCell isoMetaCell = this.getCellData((int)(n / 300), (int)(n2 / 300));
        if (isoMetaCell == null) {
            return;
        }
        RoomTone roomTone = new RoomTone();
        roomTone.x = n;
        roomTone.y = n2;
        roomTone.z = n3;
        roomTone.enumValue = kahluaTable.getString((String)"RoomTone");
        roomTone.entireBuilding = Boolean.TRUE.equals((Object)kahluaTable.rawget((Object)"EntireBuilding"));
        isoMetaCell.roomTones.add((Object)roomTone);
    }

    public boolean isZoneAbove(Zone zone, Zone zone2, int n, int n2, int n3) {
        if (zone == null || zone == zone2) {
            return false;
        }
        ArrayList arrayList = (ArrayList)TL_ZoneList.get();
        arrayList.clear();
        this.getZonesAt((int)n, (int)n2, (int)n3, (ArrayList)arrayList);
        return arrayList.indexOf((Object)zone) > arrayList.indexOf((Object)zone2);
    }

    public void save(ByteBuffer byteBuffer) {
        this.savePart((ByteBuffer)byteBuffer, (int)0, (boolean)false);
        this.savePart((ByteBuffer)byteBuffer, (int)1, (boolean)false);
    }

    /*
     * Exception decompiling
     */
    public void savePart(ByteBuffer var1_1, int var2_2, boolean var3_3) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * java.lang.NullPointerException: Cannot invoke "org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer.getBoundSuperForBase(org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance)" because "bindingSuperContainer" is null
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LoopLivenessClash.getIterableIterType(LoopLivenessClash.java:35)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LoopLivenessClash.detect(LoopLivenessClash.java:66)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LoopLivenessClash.detect(LoopLivenessClash.java:25)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:827)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at com.heliosdecompiler.transformerapi.decompilers.cfr.CFRDecompiler.decompile(CFRDecompiler.java:43)
         *     at com.heliosdecompiler.transformerapi.StandardTransformers$Decompilers.decompile(StandardTransformers.java:74)
         *     at com.heliosdecompiler.transformerapi.StandardTransformers.decompile(StandardTransformers.java:46)
         *     at org.jd.gui.view.component.ClassFilePage.save(ClassFilePage.java:161)
         *     at org.jd.gui.view.component.DynamicPage.save(DynamicPage.java:85)
         *     at org.jd.gui.controller.MainController.save(MainController.java:306)
         *     at org.jd.gui.controller.MainController.onSaveSource(MainController.java:298)
         *     at org.jd.gui.controller.MainController.lambda$new$3(MainController.java:148)
         *     at org.jd.gui.util.swing.SwingUtil$1.actionPerformed(SwingUtil.java:42)
         *     at java.desktop/javax.swing.AbstractButton.fireActionPerformed(Unknown Source)
         *     at java.desktop/javax.swing.AbstractButton$Handler.actionPerformed(Unknown Source)
         *     at java.desktop/javax.swing.DefaultButtonModel.fireActionPerformed(Unknown Source)
         *     at java.desktop/javax.swing.DefaultButtonModel.setPressed(Unknown Source)
         *     at java.desktop/javax.swing.AbstractButton.doClick(Unknown Source)
         *     at java.desktop/javax.swing.plaf.basic.BasicMenuItemUI.doClick(Unknown Source)
         *     at java.desktop/javax.swing.plaf.basic.BasicMenuItemUI$Handler.mouseReleased(Unknown Source)
         *     at java.desktop/java.awt.Component.processMouseEvent(Unknown Source)
         *     at java.desktop/javax.swing.JComponent.processMouseEvent(Unknown Source)
         *     at java.desktop/java.awt.Component.processEvent(Unknown Source)
         *     at java.desktop/java.awt.Container.processEvent(Unknown Source)
         *     at java.desktop/java.awt.Component.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.Container.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.Component.dispatchEvent(Unknown Source)
         *     at java.desktop/java.awt.LightweightDispatcher.retargetMouseEvent(Unknown Source)
         *     at java.desktop/java.awt.LightweightDispatcher.processMouseEvent(Unknown Source)
         *     at java.desktop/java.awt.LightweightDispatcher.dispatchEvent(Unknown Source)
         *     at java.desktop/java.awt.Container.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.Window.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.Component.dispatchEvent(Unknown Source)
         *     at java.desktop/java.awt.EventQueue.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.EventQueue$4.run(Unknown Source)
         *     at java.desktop/java.awt.EventQueue$4.run(Unknown Source)
         *     at java.base/java.security.AccessController.doPrivileged(Unknown Source)
         *     at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(Unknown Source)
         *     at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(Unknown Source)
         *     at java.desktop/java.awt.EventQueue$5.run(Unknown Source)
         *     at java.desktop/java.awt.EventQueue$5.run(Unknown Source)
         *     at java.base/java.security.AccessController.doPrivileged(Unknown Source)
         *     at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(Unknown Source)
         *     at java.desktop/java.awt.EventQueue.dispatchEvent(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpOneEventForFilters(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpEventsForFilter(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpEventsForHierarchy(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpEvents(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpEvents(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.run(Unknown Source)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void load() {
        File file = ZomboidFileSystem.instance.getFileInCurrentSave((String)"map_meta.bin");
        try {
            FileInputStream fileInputStream = new FileInputStream((File)file);
            try {
                BufferedInputStream bufferedInputStream = new BufferedInputStream((InputStream)fileInputStream);
                try {
                    Object object = SliceY.SliceBufferLock;
                    synchronized (object) {
                        SliceY.SliceBuffer.clear();
                        int n = bufferedInputStream.read((byte[])SliceY.SliceBuffer.array());
                        SliceY.SliceBuffer.limit((int)n);
                        this.load((ByteBuffer)SliceY.SliceBuffer);
                    }
                }
                catch (Throwable throwable) {
                    try {
                        bufferedInputStream.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed((Throwable)throwable2);
                    }
                    throw throwable;
                }
                bufferedInputStream.close();
            }
            catch (Throwable throwable) {
                try {
                    fileInputStream.close();
                }
                catch (Throwable throwable3) {
                    throwable.addSuppressed((Throwable)throwable3);
                }
                throw throwable;
            }
            fileInputStream.close();
        }
        catch (FileNotFoundException fileNotFoundException) {
        }
        catch (Exception exception) {
            ExceptionLogger.logException((Throwable)exception);
        }
    }

    public void load(ByteBuffer byteBuffer) {
        int n;
        int n2;
        int n3;
        int n4;
        int n5;
        IsoMetaCell isoMetaCell;
        int n6;
        int n7;
        int n8;
        int n9;
        int n10;
        byteBuffer.mark();
        byte by = byteBuffer.get();
        byte by2 = byteBuffer.get();
        byte by3 = byteBuffer.get();
        byte by4 = byteBuffer.get();
        if (by == 77 && by2 == 69 && by3 == 84 && by4 == 65) {
            n10 = byteBuffer.getInt();
        } else {
            n10 = 33;
            byteBuffer.reset();
        }
        int n11 = this.minX;
        int n12 = this.minY;
        int n13 = this.maxX;
        int n14 = this.maxY;
        if (n10 >= 194) {
            n11 = byteBuffer.getInt();
            n12 = byteBuffer.getInt();
            n13 = byteBuffer.getInt();
            n14 = byteBuffer.getInt();
            n9 = n13 - n11 + 1;
            n8 = n14 - n12 + 1;
        } else {
            n9 = byteBuffer.getInt();
            n8 = byteBuffer.getInt();
            if (n9 == 40 && n8 == 42 && this.width == 66 && this.height == 53 && this.getLotDirectories().contains((Object)"Muldraugh, KY")) {
                n11 = 10;
                n12 = 3;
            }
            n13 = n11 + n9 - 1;
            n14 = n12 + n8 - 1;
        }
        if (n9 != this.Grid.length || n8 != this.Grid[0].length) {
            DebugLog.log((String)("map_meta.bin world size (" + n9 + "x" + n8 + ") does not match the current map size (" + this.Grid.length + "x" + this.Grid[0].length + ")"));
        }
        int n15 = 0;
        int n16 = 0;
        for (n7 = n11; n7 <= n13; ++n7) {
            for (n6 = n12; n6 <= n14; ++n6) {
                int n17;
                boolean bl;
                boolean bl2;
                int n18;
                boolean bl3;
                long l;
                isoMetaCell = this.getCellData((int)n7, (int)n6);
                n5 = byteBuffer.getInt();
                for (n4 = 0; n4 < n5; ++n4) {
                    RoomDef roomDef;
                    n3 = n10 < 194 ? byteBuffer.getInt() : 0;
                    l = n10 >= 194 ? byteBuffer.getLong() : 0L;
                    bl3 = false;
                    n18 = 0;
                    bl2 = false;
                    bl = false;
                    if (n10 >= 160) {
                        n17 = byteBuffer.getShort();
                        bl3 = (n17 & 1) != 0;
                        n18 = (n17 & 2) != 0 ? 1 : 0;
                        bl2 = (n17 & 4) != 0;
                        bl = (n17 & 8) != 0;
                    } else {
                        boolean bl4 = bl3 = byteBuffer.get() == 1;
                        if (n10 >= 34) {
                            n18 = byteBuffer.get() == 1 ? 1 : 0;
                        } else {
                            int n19 = n18 = Rand.Next((int)2) == 0 ? 1 : 0;
                        }
                    }
                    if (isoMetaCell == null || isoMetaCell.info == null) continue;
                    RoomDef roomDef2 = roomDef = n10 < 194 ? (RoomDef)isoMetaCell.info.Rooms.get((Object)Integer.valueOf((int)n3)) : (RoomDef)isoMetaCell.info.RoomByMetaID.get((long)l);
                    if (roomDef != null) {
                        roomDef.setExplored((boolean)bl3);
                        roomDef.bLightsActive = n18;
                        roomDef.bDoneSpawn = bl2;
                        roomDef.setRoofFixed((boolean)bl);
                        continue;
                    }
                    if (n10 < 194) {
                        DebugLog.General.error((Object)("invalid room ID #" + n3 + " in cell " + n7 + "," + n6 + " while reading map_meta.bin"));
                        continue;
                    }
                    DebugLog.General.error((Object)("invalid room metaID #" + l + " in cell " + n7 + "," + n6 + " while reading map_meta.bin"));
                }
                n4 = byteBuffer.getInt();
                n15 += n4;
                for (n3 = 0; n3 < n4; ++n3) {
                    int n20;
                    l = n10 >= 194 ? byteBuffer.getLong() : 0L;
                    bl3 = byteBuffer.get() == 1;
                    int n21 = n18 = n10 >= 57 ? byteBuffer.getInt() : -1;
                    boolean bl5 = n10 >= 74 ? byteBuffer.get() == 1 : (bl2 = false);
                    bl = n10 >= 107 ? byteBuffer.get() == 1 : false;
                    n17 = n10 >= 111 && n10 < 121 ? byteBuffer.getInt() : 0;
                    int n22 = n20 = n10 >= 125 ? byteBuffer.getInt() : 0;
                    if (isoMetaCell == null || isoMetaCell.info == null) continue;
                    BuildingDef buildingDef = null;
                    if (n10 >= 194) {
                        buildingDef = (BuildingDef)isoMetaCell.info.BuildingByMetaID.get((long)l);
                    } else if (n3 < isoMetaCell.info.Buildings.size()) {
                        buildingDef = (BuildingDef)isoMetaCell.info.Buildings.get((int)n3);
                    }
                    if (buildingDef != null) {
                        if (bl3) {
                            ++n16;
                        }
                        buildingDef.bAlarmed = bl3;
                        buildingDef.setKeyId((int)n18);
                        if (n10 >= 74) {
                            buildingDef.seen = bl2;
                        }
                        buildingDef.hasBeenVisited = bl;
                        buildingDef.lootRespawnHour = n20;
                        continue;
                    }
                    if (n10 < 194) continue;
                    DebugLog.General.error((Object)("invalid building metaID #" + l + " in cell " + n7 + "," + n6 + " while reading map_meta.bin"));
                }
            }
        }
        if (n10 <= 112) {
            this.Zones.clear();
            for (n7 = 0; n7 < this.height; ++n7) {
                for (n6 = 0; n6 < this.width; ++n6) {
                    isoMetaCell = this.Grid[n6][n7];
                    if (isoMetaCell == null) continue;
                    for (n5 = 0; n5 < 30; ++n5) {
                        for (n4 = 0; n4 < 30; ++n4) {
                            isoMetaCell.ChunkMap[n4 + n5 * 30].clearZones();
                        }
                    }
                }
            }
            this.loadZone((ByteBuffer)byteBuffer, (int)n10);
        }
        SafeHouse.clearSafehouseList();
        n7 = byteBuffer.getInt();
        for (n6 = 0; n6 < n7; ++n6) {
            SafeHouse.load((ByteBuffer)byteBuffer, (int)n10);
        }
        NonPvpZone.nonPvpZoneList.clear();
        n6 = byteBuffer.getInt();
        for (n2 = 0; n2 < n6; ++n2) {
            NonPvpZone nonPvpZone = new NonPvpZone();
            nonPvpZone.load((ByteBuffer)byteBuffer, (int)n10);
            NonPvpZone.getAllZones().add((Object)nonPvpZone);
        }
        Faction.factions = new ArrayList();
        n2 = byteBuffer.getInt();
        for (n = 0; n < n2; ++n) {
            Faction faction = new Faction();
            faction.load((ByteBuffer)byteBuffer, (int)n10);
            Faction.getFactions().add((Object)faction);
        }
        if (GameServer.bServer) {
            n = byteBuffer.getInt();
            StashSystem.load((ByteBuffer)byteBuffer, (int)n10);
        } else if (GameClient.bClient) {
            n = byteBuffer.getInt();
            byteBuffer.position((int)n);
        } else {
            StashSystem.load((ByteBuffer)byteBuffer, (int)n10);
        }
        ArrayList arrayList = RBBasic.getUniqueRDSSpawned();
        arrayList.clear();
        int n23 = byteBuffer.getInt();
        for (n3 = 0; n3 < n23; ++n3) {
            arrayList.add((Object)GameWindow.ReadString((ByteBuffer)byteBuffer));
        }
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public IsoMetaCell getCellData(int n, int n2) {
        if (n - this.minX < 0 || n2 - this.minY < 0 || n - this.minX >= this.width || n2 - this.minY >= this.height) {
            return null;
        }
        return this.Grid[n - this.minX][n2 - this.minY];
    }

    public IsoMetaCell getCellDataAbs(int n, int n2) {
        return this.Grid[n][n2];
    }

    public IsoMetaCell getCurrentCellData() {
        int n = IsoWorld.instance.CurrentCell.ChunkMap[IsoPlayer.getPlayerIndex()].WorldX;
        int n2 = IsoWorld.instance.CurrentCell.ChunkMap[IsoPlayer.getPlayerIndex()].WorldY;
        float f = (float)n;
        float f2 = (float)n2;
        f /= 30.0f;
        f2 /= 30.0f;
        if (f < 0.0f) {
            f = (float)((int)f - 1);
        }
        if (f2 < 0.0f) {
            f2 = (float)((int)f2 - 1);
        }
        n = (int)f;
        n2 = (int)f2;
        return this.getCellData((int)n, (int)n2);
    }

    public IsoMetaCell getMetaGridFromTile(int n, int n2) {
        int n3 = n / 300;
        int n4 = n2 / 300;
        return this.getCellData((int)n3, (int)n4);
    }

    public IsoMetaChunk getCurrentChunkData() {
        int n = IsoWorld.instance.CurrentCell.ChunkMap[IsoPlayer.getPlayerIndex()].WorldX;
        int n2 = IsoWorld.instance.CurrentCell.ChunkMap[IsoPlayer.getPlayerIndex()].WorldY;
        float f = (float)n;
        float f2 = (float)n2;
        f /= 30.0f;
        f2 /= 30.0f;
        if (f < 0.0f) {
            f = (float)((int)f - 1);
        }
        if (f2 < 0.0f) {
            f2 = (float)((int)f2 - 1);
        }
        n = (int)f;
        n2 = (int)f2;
        return this.getCellData((int)n, (int)n2).getChunk((int)(IsoWorld.instance.CurrentCell.ChunkMap[IsoPlayer.getPlayerIndex()].WorldX - n * 30), (int)(IsoWorld.instance.CurrentCell.ChunkMap[IsoPlayer.getPlayerIndex()].WorldY - n2 * 30));
    }

    public IsoMetaChunk getChunkData(int n, int n2) {
        IsoMetaCell isoMetaCell;
        int n3 = n;
        int n4 = n2;
        float f = (float)n3;
        float f2 = (float)n4;
        f /= 30.0f;
        f2 /= 30.0f;
        if (f < 0.0f) {
            f = (float)((int)f - 1);
        }
        if (f2 < 0.0f) {
            f2 = (float)((int)f2 - 1);
        }
        if ((isoMetaCell = this.getCellData((int)(n3 = (int)f), (int)(n4 = (int)f2))) == null) {
            return null;
        }
        return isoMetaCell.getChunk((int)(n - n3 * 30), (int)(n2 - n4 * 30));
    }

    public IsoMetaChunk getChunkDataFromTile(int n, int n2) {
        int n3 = n / 10;
        int n4 = n2 / 10;
        int n5 = (n3 -= this.minX * 30) / 30;
        int n6 = (n4 -= this.minY * 30) / 30;
        n3 += this.minX * 30;
        n4 += this.minY * 30;
        IsoMetaCell isoMetaCell = this.getCellData((int)(n5 += this.minX), (int)(n6 += this.minY));
        if (isoMetaCell == null) {
            return null;
        }
        return isoMetaCell.getChunk((int)(n3 - n5 * 30), (int)(n4 - n6 * 30));
    }

    public boolean isValidSquare(int n, int n2) {
        if (n < this.minX * 300) {
            return false;
        }
        if (n >= (this.maxX + 1) * 300) {
            return false;
        }
        if (n2 < this.minY * 300) {
            return false;
        }
        return n2 < (this.maxY + 1) * 300;
    }

    public boolean isValidChunk(int n, int n2) {
        n2 *= 10;
        if ((n *= 10) < this.minX * 300) {
            return false;
        }
        if (n >= (this.maxX + 1) * 300) {
            return false;
        }
        if (n2 < this.minY * 300) {
            return false;
        }
        if (n2 >= (this.maxY + 1) * 300) {
            return false;
        }
        return this.Grid[n / 300 - this.minX][n2 / 300 - this.minY].info != null;
    }

    public void Create() {
        this.CreateStep1();
        this.CreateStep2();
    }

    public void CreateStep1() {
        Object object;
        this.minX = 10000000;
        this.minY = 10000000;
        this.maxX = -10000000;
        this.maxY = -10000000;
        IsoLot.InfoHeaders.clear();
        IsoLot.InfoHeaderNames.clear();
        IsoLot.InfoFileNames.clear();
        long l = System.currentTimeMillis();
        DebugLog.log((String)"IsoMetaGrid.Create: begin scanning directories");
        ArrayList arrayList = this.getLotDirectories();
        DebugLog.log((String)"Looking in these map folders:");
        for (String string : arrayList) {
            string = ZomboidFileSystem.instance.getString((String)("media/maps/" + string + "/"));
            DebugLog.log((String)("    " + new File((String)string).getAbsolutePath()));
        }
        DebugLog.log((String)"<End of map-folders list>");
        for (String string : arrayList) {
            File file = new File((String)ZomboidFileSystem.instance.getString((String)("media/maps/" + string + "/")));
            if (!file.isDirectory()) continue;
            ChooseGameInfo.Map map = ChooseGameInfo.getMapDetails((String)string);
            object = file.list();
            for (int i = 0; i < ((String[])object).length; ++i) {
                if (IsoLot.InfoFileNames.containsKey((Object)object[i])) continue;
                if (object[i].endsWith((String)".lotheader")) {
                    String[] stringArray = object[i].split((String)"_");
                    stringArray[1] = stringArray[1].replace((CharSequence)".lotheader", (CharSequence)"");
                    int n = Integer.parseInt((String)stringArray[0].trim());
                    int n2 = Integer.parseInt((String)stringArray[1].trim());
                    if (n < this.minX) {
                        this.minX = n;
                    }
                    if (n2 < this.minY) {
                        this.minY = n2;
                    }
                    if (n > this.maxX) {
                        this.maxX = n;
                    }
                    if (n2 > this.maxY) {
                        this.maxY = n2;
                    }
                    IsoLot.InfoFileNames.put((Object)object[i], (Object)(file.getAbsolutePath() + File.separator + object[i]));
                    LotHeader lotHeader = new LotHeader();
                    lotHeader.cellX = n;
                    lotHeader.cellY = n2;
                    lotHeader.bFixed2x = map.isFixed2x();
                    IsoLot.InfoHeaders.put((Object)object[i], (Object)lotHeader);
                    IsoLot.InfoHeaderNames.add((Object)object[i]);
                    continue;
                }
                if (object[i].endsWith((String)".lotpack")) {
                    IsoLot.InfoFileNames.put((Object)object[i], (Object)(file.getAbsolutePath() + File.separator + object[i]));
                    continue;
                }
                if (!object[i].startsWith((String)"chunkdata_")) continue;
                IsoLot.InfoFileNames.put((Object)object[i], (Object)(file.getAbsolutePath() + File.separator + object[i]));
            }
        }
        if (this.maxX < this.minX || this.maxY < this.minY) {
            throw new IllegalStateException((String)"Failed to find any .lotheader files");
        }
        this.Grid = new IsoMetaCell[this.maxX - this.minX + 1][this.maxY - this.minY + 1];
        this.width = this.maxX - this.minX + 1;
        this.height = this.maxY - this.minY + 1;
        long l2 = System.currentTimeMillis() - l;
        DebugLog.log((String)("IsoMetaGrid.Create: finished scanning directories in " + (float)l2 / 1000.0f + " seconds"));
        DebugLog.log((String)"IsoMetaGrid.Create: begin loading");
        this.createStartTime = System.currentTimeMillis();
        for (int i = 0; i < 8; ++i) {
            object = new MetaGridLoaderThread((IsoMetaGrid)this, (int)(this.minY + i));
            object.setDaemon((boolean)true);
            object.setName((String)("MetaGridLoaderThread" + i));
            object.start();
            this.threads[i] = object;
        }
    }

    public void CreateStep2() {
        int n;
        boolean bl = true;
        block2: while (bl) {
            bl = false;
            for (n = 0; n < 8; ++n) {
                if (!this.threads[n].isAlive()) continue;
                bl = true;
                try {
                    Thread.sleep((long)100L);
                }
                catch (InterruptedException interruptedException) {}
                continue block2;
            }
        }
        for (n = 0; n < 8; ++n) {
            this.threads[n].postLoad();
            this.threads[n] = null;
        }
        for (n = 0; n < this.Buildings.size(); ++n) {
            BuildingDef buildingDef = (BuildingDef)this.Buildings.get((int)n);
            if (Core.GameMode.equals((Object)"LastStand") || buildingDef.rooms.size() <= 2) continue;
            int n2 = 11;
            if (SandboxOptions.instance.getElecShutModifier() > -1 && GameTime.instance.NightsSurvived < SandboxOptions.instance.getElecShutModifier()) {
                n2 = 9;
            }
            if (SandboxOptions.instance.Alarm.getValue() == 1) {
                n2 = -1;
            } else if (SandboxOptions.instance.Alarm.getValue() == 2) {
                n2 += 5;
            } else if (SandboxOptions.instance.Alarm.getValue() == 3) {
                n2 += 3;
            } else if (SandboxOptions.instance.Alarm.getValue() == 5) {
                n2 -= 3;
            } else if (SandboxOptions.instance.Alarm.getValue() == 6) {
                n2 -= 5;
            }
            if (n2 <= -1) continue;
            buildingDef.bAlarmed = Rand.Next((int)n2) == 0;
        }
        long l = System.currentTimeMillis() - this.createStartTime;
        DebugLog.log((String)("IsoMetaGrid.Create: finished loading in " + (float)l / 1000.0f + " seconds"));
    }

    /*
     * Exception decompiling
     */
    public void Dispose() {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * java.lang.NullPointerException: Cannot invoke "org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer.getBoundSuperForBase(org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance)" because "bindingSuperContainer" is null
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LoopLivenessClash.getIterableIterType(LoopLivenessClash.java:35)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LoopLivenessClash.detect(LoopLivenessClash.java:66)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LoopLivenessClash.detect(LoopLivenessClash.java:25)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:827)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at com.heliosdecompiler.transformerapi.decompilers.cfr.CFRDecompiler.decompile(CFRDecompiler.java:43)
         *     at com.heliosdecompiler.transformerapi.StandardTransformers$Decompilers.decompile(StandardTransformers.java:74)
         *     at com.heliosdecompiler.transformerapi.StandardTransformers.decompile(StandardTransformers.java:46)
         *     at org.jd.gui.view.component.ClassFilePage.save(ClassFilePage.java:161)
         *     at org.jd.gui.view.component.DynamicPage.save(DynamicPage.java:85)
         *     at org.jd.gui.controller.MainController.save(MainController.java:306)
         *     at org.jd.gui.controller.MainController.onSaveSource(MainController.java:298)
         *     at org.jd.gui.controller.MainController.lambda$new$3(MainController.java:148)
         *     at org.jd.gui.util.swing.SwingUtil$1.actionPerformed(SwingUtil.java:42)
         *     at java.desktop/javax.swing.AbstractButton.fireActionPerformed(Unknown Source)
         *     at java.desktop/javax.swing.AbstractButton$Handler.actionPerformed(Unknown Source)
         *     at java.desktop/javax.swing.DefaultButtonModel.fireActionPerformed(Unknown Source)
         *     at java.desktop/javax.swing.DefaultButtonModel.setPressed(Unknown Source)
         *     at java.desktop/javax.swing.AbstractButton.doClick(Unknown Source)
         *     at java.desktop/javax.swing.plaf.basic.BasicMenuItemUI.doClick(Unknown Source)
         *     at java.desktop/javax.swing.plaf.basic.BasicMenuItemUI$Handler.mouseReleased(Unknown Source)
         *     at java.desktop/java.awt.Component.processMouseEvent(Unknown Source)
         *     at java.desktop/javax.swing.JComponent.processMouseEvent(Unknown Source)
         *     at java.desktop/java.awt.Component.processEvent(Unknown Source)
         *     at java.desktop/java.awt.Container.processEvent(Unknown Source)
         *     at java.desktop/java.awt.Component.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.Container.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.Component.dispatchEvent(Unknown Source)
         *     at java.desktop/java.awt.LightweightDispatcher.retargetMouseEvent(Unknown Source)
         *     at java.desktop/java.awt.LightweightDispatcher.processMouseEvent(Unknown Source)
         *     at java.desktop/java.awt.LightweightDispatcher.dispatchEvent(Unknown Source)
         *     at java.desktop/java.awt.Container.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.Window.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.Component.dispatchEvent(Unknown Source)
         *     at java.desktop/java.awt.EventQueue.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.EventQueue$4.run(Unknown Source)
         *     at java.desktop/java.awt.EventQueue$4.run(Unknown Source)
         *     at java.base/java.security.AccessController.doPrivileged(Unknown Source)
         *     at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(Unknown Source)
         *     at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(Unknown Source)
         *     at java.desktop/java.awt.EventQueue$5.run(Unknown Source)
         *     at java.desktop/java.awt.EventQueue$5.run(Unknown Source)
         *     at java.base/java.security.AccessController.doPrivileged(Unknown Source)
         *     at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(Unknown Source)
         *     at java.desktop/java.awt.EventQueue.dispatchEvent(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpOneEventForFilters(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpEventsForFilter(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpEventsForHierarchy(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpEvents(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpEvents(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.run(Unknown Source)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    public Vector2 getRandomIndoorCoord() {
        return null;
    }

    public RoomDef getRandomRoomBetweenRange(float f, float f2, float f3, float f4) {
        RoomDef roomDef = null;
        float f5 = 0.0f;
        roomChoices.clear();
        LotHeader lotHeader = null;
        for (int i = 0; i < IsoLot.InfoHeaderNames.size(); ++i) {
            lotHeader = (LotHeader)IsoLot.InfoHeaders.get((Object)IsoLot.InfoHeaderNames.get((int)i));
            if (lotHeader.RoomList.isEmpty()) continue;
            for (int j = 0; j < lotHeader.RoomList.size(); ++j) {
                roomDef = (RoomDef)lotHeader.RoomList.get((int)j);
                f5 = IsoUtils.DistanceManhatten((float)f, (float)f2, (float)((float)roomDef.x), (float)((float)roomDef.y));
                if (!(f5 > f3) || !(f5 < f4)) continue;
                roomChoices.add((Object)roomDef);
            }
        }
        if (!roomChoices.isEmpty()) {
            return (RoomDef)roomChoices.get((int)Rand.Next((int)roomChoices.size()));
        }
        return null;
    }

    public RoomDef getRandomRoomNotInRange(float f, float f2, int n) {
        LotHeader lotHeader;
        RoomDef roomDef = null;
        do {
            lotHeader = null;
            do {
                lotHeader = (LotHeader)IsoLot.InfoHeaders.get((Object)IsoLot.InfoHeaderNames.get((int)Rand.Next((int)IsoLot.InfoHeaderNames.size())));
            } while (lotHeader.RoomList.isEmpty());
        } while ((roomDef = (RoomDef)lotHeader.RoomList.get((int)Rand.Next((int)lotHeader.RoomList.size()))) == null || IsoUtils.DistanceManhatten((float)f, (float)f2, (float)((float)roomDef.x), (float)((float)roomDef.y)) < (float)n);
        return roomDef;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void save() {
        try {
            Object object;
            BufferedOutputStream bufferedOutputStream;
            File file = ZomboidFileSystem.instance.getFileInCurrentSave((String)"map_meta.bin");
            FileOutputStream fileOutputStream = new FileOutputStream((File)file);
            try {
                bufferedOutputStream = new BufferedOutputStream((OutputStream)fileOutputStream);
                try {
                    object = SliceY.SliceBufferLock;
                    synchronized (object) {
                        SliceY.SliceBuffer.clear();
                        this.save((ByteBuffer)SliceY.SliceBuffer);
                        bufferedOutputStream.write((byte[])SliceY.SliceBuffer.array(), (int)0, (int)SliceY.SliceBuffer.position());
                    }
                }
                catch (Throwable throwable) {
                    try {
                        bufferedOutputStream.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed((Throwable)throwable2);
                    }
                    throw throwable;
                }
                bufferedOutputStream.close();
            }
            catch (Throwable throwable) {
                try {
                    fileOutputStream.close();
                }
                catch (Throwable throwable3) {
                    throwable.addSuppressed((Throwable)throwable3);
                }
                throw throwable;
            }
            fileOutputStream.close();
            fileOutputStream = ZomboidFileSystem.instance.getFileInCurrentSave((String)"map_zone.bin");
            bufferedOutputStream = new FileOutputStream((File)fileOutputStream);
            try {
                object = new BufferedOutputStream((OutputStream)bufferedOutputStream);
                try {
                    Object object2 = SliceY.SliceBufferLock;
                    synchronized (object2) {
                        SliceY.SliceBuffer.clear();
                        this.saveZone((ByteBuffer)SliceY.SliceBuffer);
                        object.write((byte[])SliceY.SliceBuffer.array(), (int)0, (int)SliceY.SliceBuffer.position());
                    }
                }
                catch (Throwable throwable) {
                    try {
                        object.close();
                    }
                    catch (Throwable throwable4) {
                        throwable.addSuppressed((Throwable)throwable4);
                    }
                    throw throwable;
                }
                object.close();
            }
            catch (Throwable throwable) {
                try {
                    bufferedOutputStream.close();
                }
                catch (Throwable throwable5) {
                    throwable.addSuppressed((Throwable)throwable5);
                }
                throw throwable;
            }
            bufferedOutputStream.close();
        }
        catch (Exception exception) {
            ExceptionLogger.logException((Throwable)exception);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void loadZones() {
        File file = ZomboidFileSystem.instance.getFileInCurrentSave((String)"map_zone.bin");
        try {
            FileInputStream fileInputStream = new FileInputStream((File)file);
            try {
                BufferedInputStream bufferedInputStream = new BufferedInputStream((InputStream)fileInputStream);
                try {
                    Object object = SliceY.SliceBufferLock;
                    synchronized (object) {
                        SliceY.SliceBuffer.clear();
                        int n = bufferedInputStream.read((byte[])SliceY.SliceBuffer.array());
                        SliceY.SliceBuffer.limit((int)n);
                        this.loadZone((ByteBuffer)SliceY.SliceBuffer, (int)-1);
                    }
                }
                catch (Throwable throwable) {
                    try {
                        bufferedInputStream.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed((Throwable)throwable2);
                    }
                    throw throwable;
                }
                bufferedInputStream.close();
            }
            catch (Throwable throwable) {
                try {
                    fileInputStream.close();
                }
                catch (Throwable throwable3) {
                    throwable.addSuppressed((Throwable)throwable3);
                }
                throw throwable;
            }
            fileInputStream.close();
        }
        catch (FileNotFoundException fileNotFoundException) {
        }
        catch (Exception exception) {
            ExceptionLogger.logException((Throwable)exception);
        }
    }

    /*
     * Exception decompiling
     */
    public void loadZone(ByteBuffer var1_1, int var2_2) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * java.lang.NullPointerException: Cannot invoke "org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer.getBoundSuperForBase(org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance)" because "bindingSuperContainer" is null
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LoopLivenessClash.getIterableIterType(LoopLivenessClash.java:35)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LoopLivenessClash.detect(LoopLivenessClash.java:66)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LoopLivenessClash.detect(LoopLivenessClash.java:25)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:827)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at com.heliosdecompiler.transformerapi.decompilers.cfr.CFRDecompiler.decompile(CFRDecompiler.java:43)
         *     at com.heliosdecompiler.transformerapi.StandardTransformers$Decompilers.decompile(StandardTransformers.java:74)
         *     at com.heliosdecompiler.transformerapi.StandardTransformers.decompile(StandardTransformers.java:46)
         *     at org.jd.gui.view.component.ClassFilePage.save(ClassFilePage.java:161)
         *     at org.jd.gui.view.component.DynamicPage.save(DynamicPage.java:85)
         *     at org.jd.gui.controller.MainController.save(MainController.java:306)
         *     at org.jd.gui.controller.MainController.onSaveSource(MainController.java:298)
         *     at org.jd.gui.controller.MainController.lambda$new$3(MainController.java:148)
         *     at org.jd.gui.util.swing.SwingUtil$1.actionPerformed(SwingUtil.java:42)
         *     at java.desktop/javax.swing.AbstractButton.fireActionPerformed(Unknown Source)
         *     at java.desktop/javax.swing.AbstractButton$Handler.actionPerformed(Unknown Source)
         *     at java.desktop/javax.swing.DefaultButtonModel.fireActionPerformed(Unknown Source)
         *     at java.desktop/javax.swing.DefaultButtonModel.setPressed(Unknown Source)
         *     at java.desktop/javax.swing.AbstractButton.doClick(Unknown Source)
         *     at java.desktop/javax.swing.plaf.basic.BasicMenuItemUI.doClick(Unknown Source)
         *     at java.desktop/javax.swing.plaf.basic.BasicMenuItemUI$Handler.mouseReleased(Unknown Source)
         *     at java.desktop/java.awt.Component.processMouseEvent(Unknown Source)
         *     at java.desktop/javax.swing.JComponent.processMouseEvent(Unknown Source)
         *     at java.desktop/java.awt.Component.processEvent(Unknown Source)
         *     at java.desktop/java.awt.Container.processEvent(Unknown Source)
         *     at java.desktop/java.awt.Component.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.Container.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.Component.dispatchEvent(Unknown Source)
         *     at java.desktop/java.awt.LightweightDispatcher.retargetMouseEvent(Unknown Source)
         *     at java.desktop/java.awt.LightweightDispatcher.processMouseEvent(Unknown Source)
         *     at java.desktop/java.awt.LightweightDispatcher.dispatchEvent(Unknown Source)
         *     at java.desktop/java.awt.Container.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.Window.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.Component.dispatchEvent(Unknown Source)
         *     at java.desktop/java.awt.EventQueue.dispatchEventImpl(Unknown Source)
         *     at java.desktop/java.awt.EventQueue$4.run(Unknown Source)
         *     at java.desktop/java.awt.EventQueue$4.run(Unknown Source)
         *     at java.base/java.security.AccessController.doPrivileged(Unknown Source)
         *     at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(Unknown Source)
         *     at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(Unknown Source)
         *     at java.desktop/java.awt.EventQueue$5.run(Unknown Source)
         *     at java.desktop/java.awt.EventQueue$5.run(Unknown Source)
         *     at java.base/java.security.AccessController.doPrivileged(Unknown Source)
         *     at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(Unknown Source)
         *     at java.desktop/java.awt.EventQueue.dispatchEvent(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpOneEventForFilters(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpEventsForFilter(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpEventsForHierarchy(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpEvents(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.pumpEvents(Unknown Source)
         *     at java.desktop/java.awt.EventDispatchThread.run(Unknown Source)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    public void saveZone(ByteBuffer byteBuffer) {
        int n;
        Zone zone;
        byteBuffer.put((byte)90);
        byteBuffer.put((byte)79);
        byteBuffer.put((byte)78);
        byteBuffer.put((byte)69);
        byteBuffer.putInt((int)195);
        HashSet hashSet = new HashSet();
        for (int i = 0; i < this.Zones.size(); ++i) {
            zone = (Zone)this.Zones.get((int)i);
            hashSet.add((Object)zone.getName());
            hashSet.add((Object)zone.getOriginalName());
            hashSet.add((Object)zone.getType());
        }
        ArrayList arrayList = new ArrayList((Collection)hashSet);
        zone = new HashMap();
        for (n = 0; n < arrayList.size(); ++n) {
            zone.put((Object)((String)arrayList.get((int)n)), (Object)Integer.valueOf((int)n));
        }
        if (arrayList.size() > Short.MAX_VALUE) {
            throw new IllegalStateException((String)"IsoMetaGrid.saveZone() string table is too large");
        }
        byteBuffer.putInt((int)arrayList.size());
        for (n = 0; n < arrayList.size(); ++n) {
            GameWindow.WriteString((ByteBuffer)byteBuffer, (String)((String)arrayList.get((int)n)));
        }
        byteBuffer.putInt((int)this.Zones.size());
        for (n = 0; n < this.Zones.size(); ++n) {
            Object object = (Zone)this.Zones.get((int)n);
            byteBuffer.putShort((short)((Integer)zone.get((Object)((Zone)object).getName())).shortValue());
            byteBuffer.putShort((short)((Integer)zone.get((Object)((Zone)object).getType())).shortValue());
            byteBuffer.putInt((int)((Zone)object).x);
            byteBuffer.putInt((int)((Zone)object).y);
            byteBuffer.put((byte)((byte)((Zone)object).z));
            byteBuffer.putInt((int)((Zone)object).w);
            byteBuffer.putInt((int)((Zone)object).h);
            byteBuffer.put((byte)((byte)((Zone)object).geometryType.ordinal()));
            if (!((Zone)object).isRectangle()) {
                if (((Zone)object).isPolyline()) {
                    byteBuffer.put((byte)((byte)((Zone)object).polylineWidth));
                }
                byteBuffer.putShort((short)((short)((Zone)object).points.size()));
                for (int i = 0; i < ((Zone)object).points.size(); ++i) {
                    byteBuffer.putInt((int)((Zone)object).points.get((int)i));
                }
            }
            byteBuffer.putInt((int)((Zone)object).hourLastSeen);
            byteBuffer.put((byte)(((Zone)object).haveConstruction ? (byte)1 : 0));
            byteBuffer.putInt((int)((Zone)object).lastActionTimestamp);
            byteBuffer.putShort((short)((Integer)zone.get((Object)((Zone)object).getOriginalName())).shortValue());
            byteBuffer.putDouble((double)((Zone)object).id.doubleValue());
        }
        hashSet.clear();
        arrayList.clear();
        zone.clear();
        byteBuffer.putInt((int)IsoWorld.instance.getSpawnedZombieZone().size());
        for (Object object : IsoWorld.instance.getSpawnedZombieZone().keySet()) {
            ArrayList arrayList2 = (ArrayList)IsoWorld.instance.getSpawnedZombieZone().get((Object)object);
            GameWindow.WriteString((ByteBuffer)byteBuffer, (String)object);
            byteBuffer.putInt((int)arrayList2.size());
            for (int i = 0; i < arrayList2.size(); ++i) {
                byteBuffer.putDouble((double)((Double)arrayList2.get((int)i)).doubleValue());
            }
        }
    }

    private void getLotDirectories(String string, ArrayList arrayList) {
        if (arrayList.contains((Object)string)) {
            return;
        }
        ChooseGameInfo.Map map = ChooseGameInfo.getMapDetails((String)string);
        if (map == null) {
            return;
        }
        arrayList.add((Object)string);
        for (String string2 : map.getLotDirectories()) {
            this.getLotDirectories((String)string2, (ArrayList)arrayList);
        }
    }

    public ArrayList getLotDirectories() {
        MapGroups mapGroups;
        if (GameClient.bClient) {
            Core.GameMap = GameClient.GameMap;
        }
        if (GameServer.bServer) {
            Core.GameMap = GameServer.GameMap;
        }
        if (Core.GameMap.equals((Object)"DEFAULT")) {
            mapGroups = new MapGroups();
            mapGroups.createGroups();
            if (mapGroups.getNumberOfGroups() != 1) {
                throw new RuntimeException((String)"GameMap is DEFAULT but there are multiple worlds to choose from");
            }
            mapGroups.setWorld((int)0);
        }
        mapGroups = new ArrayList();
        if (Core.GameMap.contains((CharSequence)";")) {
            String[] stringArray = Core.GameMap.split((String)";");
            for (int i = 0; i < stringArray.length; ++i) {
                String string = stringArray[i].trim();
                if (string.isEmpty() || mapGroups.contains((Object)string)) continue;
                mapGroups.add((Object)string);
            }
        } else {
            this.getLotDirectories((String)Core.GameMap, (ArrayList)mapGroups);
        }
        return mapGroups;
    }

    public static boolean isPreferredZoneForSquare(String string) {
        return s_PreferredZoneTypes.contains((Object)string);
    }

    static {
        s_PreferredZoneTypes = new ArrayList();
        s_clipper = null;
        s_clipperOffset = null;
        s_clipperBuffer = null;
        TL_Location = ThreadLocal.withInitial(IsoGameCharacter.Location::new);
        TL_ZoneList = ThreadLocal.withInitial(ArrayList::new);
        a = new Rectangle();
        b = new Rectangle();
        roomChoices = new ArrayList((int)50);
        s_PreferredZoneTypes.add((Object)"DeepForest");
        s_PreferredZoneTypes.add((Object)"Farm");
        s_PreferredZoneTypes.add((Object)"FarmLand");
        s_PreferredZoneTypes.add((Object)"Forest");
        s_PreferredZoneTypes.add((Object)"Vegitation");
        s_PreferredZoneTypes.add((Object)"Nav");
        s_PreferredZoneTypes.add((Object)"TownZone");
        s_PreferredZoneTypes.add((Object)"TrailerPark");
    }
}
