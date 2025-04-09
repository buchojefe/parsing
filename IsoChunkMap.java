/*
 * Decompiled with CFR.
 */
package zombie.iso;

import java.io.IOException;
import java.lang.AssertionError;
import java.lang.Integer;
import java.lang.InterruptedException;
import java.lang.Math;
import java.lang.Object;
import java.lang.String;
import java.lang.System;
import java.lang.Thread;
import java.lang.Throwable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.physics.WorldSimulation;
import zombie.core.textures.ColorInfo;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.ChunkSaveWorker;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoFloorBloodSplat;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.LightingThread;
import zombie.iso.WorldReuserThread;
import zombie.iso.WorldStreamer;
import zombie.iso.areas.IsoRoom;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.MPStatistics;
import zombie.network.PacketTypes;
import zombie.ui.TextManager;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleCache;
import zombie.vehicles.VehicleManager;

public final class IsoChunkMap {
    public static final int LEVELS = 8;
    public static final int ChunksPerWidth = 10;
    public static final HashMap<Integer, IsoChunk> SharedChunks;
    public static int MPWorldXA;
    public static int MPWorldYA;
    public static int MPWorldZA;
    public static int WorldXA;
    public static int WorldYA;
    public static int WorldZA;
    public static final int[] SWorldX;
    public static final int[] SWorldY;
    public static final ConcurrentLinkedQueue<IsoChunk> chunkStore;
    public static final ReentrantLock bSettingChunk;
    private static int StartChunkGridWidth;
    public static int ChunkGridWidth;
    public static int ChunkWidthInTiles;
    private static final ColorInfo inf;
    private static final ArrayList<IsoChunk> saveList;
    private static final ArrayList<ArrayList<IsoFloorBloodSplat>> splatByType;
    public int PlayerID;
    public boolean ignore;
    public int WorldX;
    public int WorldY;
    public final ArrayList<String> filenameServerRequests;
    protected IsoChunk[] chunksSwapB;
    protected IsoChunk[] chunksSwapA;
    boolean bReadBufferA;
    int XMinTiles;
    int YMinTiles;
    int XMaxTiles;
    int YMaxTiles;
    private IsoCell cell;
    private final UpdateLimit checkVehiclesFrequency;
    static final /* synthetic */ boolean $assertionsDisabled;

    public IsoChunkMap(IsoCell isoCell) {
        super();
        this.PlayerID = 0;
        this.ignore = false;
        this.WorldX = IsoChunkMap.tileToChunk((int)WorldXA);
        this.WorldY = IsoChunkMap.tileToChunk((int)WorldYA);
        this.filenameServerRequests = new ArrayList();
        this.bReadBufferA = true;
        this.XMinTiles = -1;
        this.YMinTiles = -1;
        this.XMaxTiles = -1;
        this.YMaxTiles = -1;
        this.checkVehiclesFrequency = new UpdateLimit((long)3000L);
        this.cell = isoCell;
        WorldReuserThread.instance.finished = false;
        this.chunksSwapB = new IsoChunk[ChunkGridWidth * ChunkGridWidth];
        this.chunksSwapA = new IsoChunk[ChunkGridWidth * ChunkGridWidth];
    }

    public static void CalcChunkWidth() {
        if (DebugOptions.instance.WorldChunkMap5x5.getValue()) {
            ChunkGridWidth = 5;
            ChunkWidthInTiles = ChunkGridWidth * 10;
            return;
        }
        float f = (float)Core.getInstance().getScreenWidth();
        float f2 = f / 1920.0f;
        if (f2 > 1.0f) {
            f2 = 1.0f;
        }
        if ((ChunkGridWidth = (int)((double)((float)StartChunkGridWidth * f2) * 1.5)) / 2 * 2 == ChunkGridWidth) {
            ++ChunkGridWidth;
        }
        ChunkWidthInTiles = ChunkGridWidth * 10;
    }

    public static void setWorldStartPos(int n, int n2) {
        IsoChunkMap.SWorldX[IsoPlayer.getPlayerIndex()] = IsoChunkMap.tileToChunk((int)n);
        IsoChunkMap.SWorldY[IsoPlayer.getPlayerIndex()] = IsoChunkMap.tileToChunk((int)n2);
    }

    public void Dispose() {
        WorldReuserThread.instance.finished = true;
        IsoChunk.loadGridSquare.clear();
        this.chunksSwapA = null;
        this.chunksSwapB = null;
    }

    public void setInitialPos(int n, int n2) {
        this.WorldX = n;
        this.WorldY = n2;
        this.XMinTiles = -1;
        this.XMaxTiles = -1;
        this.YMinTiles = -1;
        this.YMaxTiles = -1;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     */
    public void processAllLoadGridSquare() {
        var1_1 = (IsoChunk)IsoChunk.loadGridSquare.poll();
        while (var1_1 != null) {
            IsoChunkMap.bSettingChunk.lock();
            try {
                var2_2 = false;
                for (var3_3 = 0; var3_3 < IsoPlayer.numPlayers; ++var3_3) {
                    var4_4 = IsoWorld.instance.CurrentCell.ChunkMap[var3_3];
                    if (var4_4.ignore || !var4_4.setChunkDirect((IsoChunk)var1_1, (boolean)false)) continue;
                    var2_2 = true;
                }
                if (var2_2) ** GOTO lbl19
                WorldReuserThread.instance.addReuseChunk((IsoChunk)var1_1);
            }
            catch (Throwable var5_5) {
                IsoChunkMap.bSettingChunk.unlock();
                throw var5_5;
            }
            IsoChunkMap.bSettingChunk.unlock();
            ** GOTO lbl22
lbl19:
            // 2 sources

            var1_1.doLoadGridsquare();
            IsoChunkMap.bSettingChunk.unlock();
lbl22:
            // 2 sources

            var1_1 = (IsoChunk)IsoChunk.loadGridSquare.poll();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void update() {
        int n;
        int n2;
        IsoChunk isoChunk;
        int n3 = IsoChunk.loadGridSquare.size();
        if (n3 != 0) {
            n3 = 1 + n3 * 3 / ChunkGridWidth;
        }
        while (n3 > 0) {
            isoChunk = (IsoChunk)IsoChunk.loadGridSquare.poll();
            if (isoChunk != null) {
                IsoChunkMap isoChunkMap;
                n2 = 0;
                for (n = 0; n < IsoPlayer.numPlayers; ++n) {
                    isoChunkMap = IsoWorld.instance.CurrentCell.ChunkMap[n];
                    if (isoChunkMap.ignore || !isoChunkMap.setChunkDirect((IsoChunk)isoChunk, (boolean)false)) continue;
                    n2 = 1;
                }
                if (n2 == 0) {
                    WorldReuserThread.instance.addReuseChunk((IsoChunk)isoChunk);
                    --n3;
                    continue;
                }
                isoChunk.bLoaded = true;
                bSettingChunk.lock();
                try {
                    isoChunk.doLoadGridsquare();
                    if (GameClient.bClient) {
                        List list = VehicleCache.vehicleGet((int)isoChunk.wx, (int)isoChunk.wy);
                        VehicleManager.instance.sendRequestGetFull((List)list);
                    }
                }
                catch (Throwable throwable) {
                    bSettingChunk.unlock();
                    throw throwable;
                }
                bSettingChunk.unlock();
                for (n = 0; n < IsoPlayer.numPlayers; ++n) {
                    isoChunkMap = IsoPlayer.players[n];
                    if (isoChunkMap == null) continue;
                    ((IsoPlayer)isoChunkMap).dirtyRecalcGridStackTime = 20.0f;
                }
            }
            --n3;
        }
        for (n2 = 0; n2 < ChunkGridWidth; ++n2) {
            for (n = 0; n < ChunkGridWidth; ++n) {
                isoChunk = this.getChunk((int)n, (int)n2);
                if (isoChunk == null) continue;
                isoChunk.update();
            }
        }
        if (this.checkVehiclesFrequency.Check() && GameClient.bClient) {
            this.checkVehicles();
        }
    }

    private void checkVehicles() {
        for (int i = 0; i < ChunkGridWidth; ++i) {
            for (int j = 0; j < ChunkGridWidth; ++j) {
                List list;
                IsoChunk isoChunk = this.getChunk((int)j, (int)i);
                if (isoChunk == null || !isoChunk.bLoaded || (list = VehicleCache.vehicleGet((int)isoChunk.wx, (int)isoChunk.wy)) == null || isoChunk.vehicles.size() == list.size()) continue;
                for (int k = 0; k < list.size(); ++k) {
                    short s = ((VehicleCache)list.get((int)k)).id;
                    boolean bl = false;
                    for (int i2 = 0; i2 < isoChunk.vehicles.size(); ++i2) {
                        if (((BaseVehicle)isoChunk.vehicles.get((int)i2)).getId() != s) continue;
                        bl = true;
                        break;
                    }
                    if (bl || VehicleManager.instance.getVehicleByID((short)s) != null) continue;
                    VehicleManager.instance.sendRequestGetFull((short)s, (PacketTypes.PacketType)PacketTypes.PacketType.Vehicles);
                }
            }
        }
    }

    public void checkIntegrity() {
        IsoWorld.instance.CurrentCell.ChunkMap[0].XMinTiles = -1;
        for (int i = IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldXMinTiles(); i < IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldXMaxTiles(); ++i) {
            for (int j = IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldYMinTiles(); j < IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldYMaxTiles(); ++j) {
                IsoGridSquare isoGridSquare = IsoWorld.instance.CurrentCell.getGridSquare((int)i, (int)j, (int)0);
                if (isoGridSquare == null || isoGridSquare.getX() == i && isoGridSquare.getY() == j) continue;
                int n = i / 10;
                int n2 = j / 10;
                n -= IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldXMin();
                n2 -= IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldYMin();
                IsoChunk isoChunk = null;
                isoChunk = new IsoChunk((IsoCell)IsoWorld.instance.CurrentCell);
                isoChunk.refs.add((Object)IsoWorld.instance.CurrentCell.ChunkMap[0]);
                WorldStreamer.instance.addJob((IsoChunk)isoChunk, (int)(i / 10), (int)(j / 10), (boolean)false);
                while (!isoChunk.bLoaded) {
                    try {
                        Thread.sleep((long)13L);
                    }
                    catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                }
            }
        }
    }

    public void checkIntegrityThread() {
        IsoWorld.instance.CurrentCell.ChunkMap[0].XMinTiles = -1;
        for (int i = IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldXMinTiles(); i < IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldXMaxTiles(); ++i) {
            for (int j = IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldYMinTiles(); j < IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldYMaxTiles(); ++j) {
                IsoGridSquare isoGridSquare = IsoWorld.instance.CurrentCell.getGridSquare((int)i, (int)j, (int)0);
                if (isoGridSquare != null && (isoGridSquare.getX() != i || isoGridSquare.getY() != j)) {
                    int n = i / 10;
                    int n2 = j / 10;
                    n -= IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldXMin();
                    n2 -= IsoWorld.instance.CurrentCell.ChunkMap[0].getWorldYMin();
                    IsoChunk isoChunk = new IsoChunk((IsoCell)IsoWorld.instance.CurrentCell);
                    isoChunk.refs.add((Object)IsoWorld.instance.CurrentCell.ChunkMap[0]);
                    WorldStreamer.instance.addJobInstant((IsoChunk)isoChunk, (int)i, (int)j, (int)(i / 10), (int)(j / 10));
                }
                if (isoGridSquare == null) continue;
            }
        }
    }

    public void LoadChunk(int n, int n2, int n3, int n4) {
        IsoChunk isoChunk = null;
        if (SharedChunks.containsKey((Object)Integer.valueOf((int)((n << 16) + n2)))) {
            isoChunk = (IsoChunk)SharedChunks.get((Object)Integer.valueOf((int)((n << 16) + n2)));
            isoChunk.setCache();
            this.setChunk((int)n3, (int)n4, (IsoChunk)isoChunk);
            isoChunk.refs.add((Object)this);
        } else {
            isoChunk = (IsoChunk)chunkStore.poll();
            if (isoChunk == null) {
                isoChunk = new IsoChunk((IsoCell)this.cell);
            } else {
                MPStatistics.decreaseStoredChunk();
            }
            SharedChunks.put((Object)Integer.valueOf((int)((n << 16) + n2)), (Object)isoChunk);
            isoChunk.refs.add((Object)this);
            WorldStreamer.instance.addJob((IsoChunk)isoChunk, (int)n, (int)n2, (boolean)false);
        }
    }

    public IsoChunk LoadChunkForLater(int n, int n2, int n3, int n4) {
        IsoChunk isoChunk;
        if (!IsoWorld.instance.getMetaGrid().isValidChunk((int)n, (int)n2)) {
            return null;
        }
        if (SharedChunks.containsKey((Object)Integer.valueOf((int)((n << 16) + n2)))) {
            isoChunk = (IsoChunk)SharedChunks.get((Object)Integer.valueOf((int)((n << 16) + n2)));
            if (!isoChunk.refs.contains((Object)this)) {
                isoChunk.refs.add((Object)this);
                isoChunk.lightCheck[this.PlayerID] = true;
            }
            if (!isoChunk.bLoaded) {
                return isoChunk;
            }
            this.setChunk((int)n3, (int)n4, (IsoChunk)isoChunk);
        } else {
            isoChunk = (IsoChunk)chunkStore.poll();
            if (isoChunk == null) {
                isoChunk = new IsoChunk((IsoCell)this.cell);
            } else {
                MPStatistics.decreaseStoredChunk();
            }
            SharedChunks.put((Object)Integer.valueOf((int)((n << 16) + n2)), (Object)isoChunk);
            isoChunk.refs.add((Object)this);
            WorldStreamer.instance.addJob((IsoChunk)isoChunk, (int)n, (int)n2, (boolean)true);
        }
        return isoChunk;
    }

    public IsoChunk getChunkForGridSquare(int n, int n2) {
        n = this.gridSquareToTileX((int)n);
        n2 = this.gridSquareToTileY((int)n2);
        if (this.isTileOutOfrange((int)n) || this.isTileOutOfrange((int)n2)) {
            return null;
        }
        return this.getChunk((int)IsoChunkMap.tileToChunk((int)n), (int)IsoChunkMap.tileToChunk((int)n2));
    }

    public IsoChunk getChunkCurrent(int n, int n2) {
        if (n < 0 || n >= ChunkGridWidth || n2 < 0 || n2 >= ChunkGridWidth) {
            return null;
        }
        if (!this.bReadBufferA) {
            return this.chunksSwapA[ChunkGridWidth * n2 + n];
        }
        return this.chunksSwapB[ChunkGridWidth * n2 + n];
    }

    public void setGridSquare(IsoGridSquare isoGridSquare, int n, int n2, int n3) {
        if (!($assertionsDisabled || isoGridSquare == null || isoGridSquare.x == n && isoGridSquare.y == n2 && isoGridSquare.z == n3)) {
            throw new AssertionError();
        }
        int n4 = this.gridSquareToTileX((int)n);
        int n5 = this.gridSquareToTileY((int)n2);
        if (this.isTileOutOfrange((int)n4) || this.isTileOutOfrange((int)n5) || this.isGridSquareOutOfRangeZ((int)n3)) {
            return;
        }
        IsoChunk isoChunk = this.getChunk((int)IsoChunkMap.tileToChunk((int)n4), (int)IsoChunkMap.tileToChunk((int)n5));
        if (isoChunk == null) {
            return;
        }
        if (n3 > isoChunk.maxLevel) {
            isoChunk.maxLevel = n3;
        }
        isoChunk.setSquare((int)this.tileToGridSquare((int)n4), (int)this.tileToGridSquare((int)n5), (int)n3, (IsoGridSquare)isoGridSquare);
    }

    public IsoGridSquare getGridSquare(int n, int n2, int n3) {
        n = this.gridSquareToTileX((int)n);
        n2 = this.gridSquareToTileY((int)n2);
        return this.getGridSquareDirect((int)n, (int)n2, (int)n3);
    }

    public IsoGridSquare getGridSquareDirect(int n, int n2, int n3) {
        if (this.isTileOutOfrange((int)n) || this.isTileOutOfrange((int)n2) || this.isGridSquareOutOfRangeZ((int)n3)) {
            return null;
        }
        IsoChunk isoChunk = this.getChunk((int)IsoChunkMap.tileToChunk((int)n), (int)IsoChunkMap.tileToChunk((int)n2));
        if (isoChunk == null) {
            return null;
        }
        return isoChunk.getGridSquare((int)this.tileToGridSquare((int)n), (int)this.tileToGridSquare((int)n2), (int)n3);
    }

    private int tileToGridSquare(int n) {
        return n % 10;
    }

    private static int tileToChunk(int n) {
        return n / 10;
    }

    private boolean isTileOutOfrange(int n) {
        return n < 0 || n >= this.getWidthInTiles();
    }

    private boolean isGridSquareOutOfRangeZ(int n) {
        return n < 0 || n >= 8;
    }

    private int gridSquareToTileX(int n) {
        int n2 = n - (this.WorldX - ChunkGridWidth / 2) * 10;
        return n2;
    }

    private int gridSquareToTileY(int n) {
        int n2 = n - (this.WorldY - ChunkGridWidth / 2) * 10;
        return n2;
    }

    public IsoChunk getChunk(int n, int n2) {
        if (n < 0 || n >= ChunkGridWidth || n2 < 0 || n2 >= ChunkGridWidth) {
            return null;
        }
        if (this.bReadBufferA) {
            return this.chunksSwapA[ChunkGridWidth * n2 + n];
        }
        return this.chunksSwapB[ChunkGridWidth * n2 + n];
    }

    private void setChunk(int n, int n2, IsoChunk isoChunk) {
        if (!this.bReadBufferA) {
            this.chunksSwapA[IsoChunkMap.ChunkGridWidth * n2 + n] = isoChunk;
        } else {
            this.chunksSwapB[IsoChunkMap.ChunkGridWidth * n2 + n] = isoChunk;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     */
    public boolean setChunkDirect(IsoChunk var1_1, boolean var2_2) {
        var3_3 = System.nanoTime();
        if (var2_2) {
            IsoChunkMap.bSettingChunk.lock();
        }
        var5_4 = System.nanoTime();
        var7_5 = var1_1.wx - this.WorldX;
        var8_6 = var1_1.wy - this.WorldY;
        var7_5 += IsoChunkMap.ChunkGridWidth / 2;
        var8_6 += IsoChunkMap.ChunkGridWidth / 2;
        if (var1_1.jobType == IsoChunk.JobType.Convert) {
            var7_5 = 0;
            var8_6 = 0;
        }
        if (var1_1.refs.isEmpty() || var7_5 < 0 || var8_6 < 0 || var7_5 >= IsoChunkMap.ChunkGridWidth || var8_6 >= IsoChunkMap.ChunkGridWidth) {
            if (var1_1.refs.contains((Object)this)) {
                var1_1.refs.remove((Object)this);
                if (var1_1.refs.isEmpty()) {
                    IsoChunkMap.SharedChunks.remove((Object)Integer.valueOf((int)((var1_1.wx << 16) + var1_1.wy)));
                }
            }
            if (var2_2) {
                IsoChunkMap.bSettingChunk.unlock();
            }
            return false;
        }
        try {
            if (this.bReadBufferA) {
                this.chunksSwapA[IsoChunkMap.ChunkGridWidth * var8_6 + var7_5] = var1_1;
            } else {
                this.chunksSwapB[IsoChunkMap.ChunkGridWidth * var8_6 + var7_5] = var1_1;
            }
            var1_1.bLoaded = true;
            if (var1_1.jobType == IsoChunk.JobType.None) {
                var1_1.setCache();
                var1_1.updateBuildings();
            }
            var9_7 = (double)(System.nanoTime() - var5_4) / 1000000.0;
            var11_8 = (double)(System.nanoTime() - var3_3) / 1000000.0;
            if (LightingThread.DebugLockTime && var11_8 > 10.0) {
                DebugLog.log((String)("setChunkDirect time " + var9_7 + "/" + var11_8 + " ms"));
            }
            ** if (!var2_2) goto lbl-1000
        }
        catch (Throwable var13_9) {
            if (var2_2) {
                IsoChunkMap.bSettingChunk.unlock();
            }
            throw var13_9;
        }
lbl-1000:
        // 1 sources

        {
            IsoChunkMap.bSettingChunk.unlock();
        }
lbl-1000:
        // 2 sources

        {
        }
        return true;
    }

    public void drawDebugChunkMap() {
        int n = 64;
        int n2 = 0;
        for (int i = 0; i < ChunkGridWidth; ++i) {
            n2 = 0;
            for (int j = 0; j < ChunkGridWidth; ++j) {
                IsoGridSquare isoGridSquare;
                n2 += 64;
                IsoChunk isoChunk = this.getChunk((int)i, (int)j);
                if (isoChunk == null || (isoGridSquare = isoChunk.getGridSquare((int)0, (int)0, (int)0)) != null) continue;
                TextManager.instance.DrawString((double)((double)n), (double)((double)n2), (String)("wx:" + isoChunk.wx + " wy:" + isoChunk.wy));
            }
            n += 128;
        }
    }

    private void LoadLeft() {
        this.XMinTiles = -1;
        this.YMinTiles = -1;
        this.XMaxTiles = -1;
        this.YMaxTiles = -1;
        this.Left();
        WorldSimulation.instance.scrollGroundLeft((int)this.PlayerID);
        this.XMinTiles = -1;
        this.YMinTiles = -1;
        this.XMaxTiles = -1;
        this.YMaxTiles = -1;
        for (int i = -(ChunkGridWidth / 2); i <= ChunkGridWidth / 2; ++i) {
            this.LoadChunkForLater((int)(this.WorldX - ChunkGridWidth / 2), (int)(this.WorldY + i), (int)0, (int)(i + ChunkGridWidth / 2));
        }
        this.SwapChunkBuffers();
        this.XMinTiles = -1;
        this.YMinTiles = -1;
        this.XMaxTiles = -1;
        this.YMaxTiles = -1;
        this.UpdateCellCache();
        LightingThread.instance.scrollLeft((int)this.PlayerID);
    }

    public void SwapChunkBuffers() {
        for (int i = 0; i < ChunkGridWidth * ChunkGridWidth; ++i) {
            if (this.bReadBufferA) {
                this.chunksSwapA[i] = null;
                continue;
            }
            this.chunksSwapB[i] = null;
        }
        this.XMaxTiles = -1;
        this.XMinTiles = -1;
        this.YMaxTiles = -1;
        this.YMinTiles = -1;
        this.bReadBufferA = !this.bReadBufferA;
    }

    private void setChunk(int n, IsoChunk isoChunk) {
        if (!this.bReadBufferA) {
            this.chunksSwapA[n] = isoChunk;
        } else {
            this.chunksSwapB[n] = isoChunk;
        }
    }

    private IsoChunk getChunk(int n) {
        if (this.bReadBufferA) {
            return this.chunksSwapA[n];
        }
        return this.chunksSwapB[n];
    }

    private void LoadRight() {
        this.XMinTiles = -1;
        this.YMinTiles = -1;
        this.XMaxTiles = -1;
        this.YMaxTiles = -1;
        this.Right();
        WorldSimulation.instance.scrollGroundRight((int)this.PlayerID);
        this.XMinTiles = -1;
        this.YMinTiles = -1;
        this.XMaxTiles = -1;
        this.YMaxTiles = -1;
        for (int i = -(ChunkGridWidth / 2); i <= ChunkGridWidth / 2; ++i) {
            this.LoadChunkForLater((int)(this.WorldX + ChunkGridWidth / 2), (int)(this.WorldY + i), (int)(ChunkGridWidth - 1), (int)(i + ChunkGridWidth / 2));
        }
        this.SwapChunkBuffers();
        this.XMinTiles = -1;
        this.YMinTiles = -1;
        this.XMaxTiles = -1;
        this.YMaxTiles = -1;
        this.UpdateCellCache();
        LightingThread.instance.scrollRight((int)this.PlayerID);
    }

    private void LoadUp() {
        this.XMinTiles = -1;
        this.YMinTiles = -1;
        this.XMaxTiles = -1;
        this.YMaxTiles = -1;
        this.Up();
        WorldSimulation.instance.scrollGroundUp((int)this.PlayerID);
        this.XMinTiles = -1;
        this.YMinTiles = -1;
        this.XMaxTiles = -1;
        this.YMaxTiles = -1;
        for (int i = -(ChunkGridWidth / 2); i <= ChunkGridWidth / 2; ++i) {
            this.LoadChunkForLater((int)(this.WorldX + i), (int)(this.WorldY - ChunkGridWidth / 2), (int)(i + ChunkGridWidth / 2), (int)0);
        }
        this.SwapChunkBuffers();
        this.XMinTiles = -1;
        this.YMinTiles = -1;
        this.XMaxTiles = -1;
        this.YMaxTiles = -1;
        this.UpdateCellCache();
        LightingThread.instance.scrollUp((int)this.PlayerID);
    }

    private void LoadDown() {
        this.XMinTiles = -1;
        this.YMinTiles = -1;
        this.XMaxTiles = -1;
        this.YMaxTiles = -1;
        this.Down();
        WorldSimulation.instance.scrollGroundDown((int)this.PlayerID);
        this.XMinTiles = -1;
        this.YMinTiles = -1;
        this.XMaxTiles = -1;
        this.YMaxTiles = -1;
        for (int i = -(ChunkGridWidth / 2); i <= ChunkGridWidth / 2; ++i) {
            this.LoadChunkForLater((int)(this.WorldX + i), (int)(this.WorldY + ChunkGridWidth / 2), (int)(i + ChunkGridWidth / 2), (int)(ChunkGridWidth - 1));
        }
        this.SwapChunkBuffers();
        this.XMinTiles = -1;
        this.YMinTiles = -1;
        this.XMaxTiles = -1;
        this.YMaxTiles = -1;
        this.UpdateCellCache();
        LightingThread.instance.scrollDown((int)this.PlayerID);
    }

    private void UpdateCellCache() {
        int n = this.getWidthInTiles();
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                for (int k = 0; k < 8; ++k) {
                    IsoGridSquare isoGridSquare = this.getGridSquare((int)(i + this.getWorldXMinTiles()), (int)(j + this.getWorldYMinTiles()), (int)k);
                    IsoWorld.instance.CurrentCell.setCacheGridSquareLocal((int)i, (int)j, (int)k, (IsoGridSquare)isoGridSquare, (int)this.PlayerID);
                }
            }
        }
    }

    private void Up() {
        for (int i = 0; i < ChunkGridWidth; ++i) {
            for (int j = ChunkGridWidth - 1; j > 0; --j) {
                int n;
                int n2;
                IsoChunk isoChunk = this.getChunk((int)i, (int)j);
                if (isoChunk == null && j == ChunkGridWidth - 1 && (isoChunk = (IsoChunk)SharedChunks.get((Object)Integer.valueOf((int)(((n2 = this.WorldX - ChunkGridWidth / 2 + i) << 16) + (n = this.WorldY - ChunkGridWidth / 2 + j))))) != null) {
                    if (isoChunk.refs.contains((Object)this)) {
                        isoChunk.refs.remove((Object)this);
                        if (isoChunk.refs.isEmpty()) {
                            SharedChunks.remove((Object)Integer.valueOf((int)((isoChunk.wx << 16) + isoChunk.wy)));
                        }
                    }
                    isoChunk = null;
                }
                if (isoChunk != null && j == ChunkGridWidth - 1) {
                    isoChunk.refs.remove((Object)this);
                    if (isoChunk.refs.isEmpty()) {
                        SharedChunks.remove((Object)Integer.valueOf((int)((isoChunk.wx << 16) + isoChunk.wy)));
                        isoChunk.removeFromWorld();
                        ChunkSaveWorker.instance.Add((IsoChunk)isoChunk);
                    }
                }
                this.setChunk((int)i, (int)j, (IsoChunk)this.getChunk((int)i, (int)(j - 1)));
            }
            this.setChunk((int)i, (int)0, null);
        }
        --this.WorldY;
    }

    private void Down() {
        for (int i = 0; i < ChunkGridWidth; ++i) {
            for (int j = 0; j < ChunkGridWidth - 1; ++j) {
                int n;
                int n2;
                IsoChunk isoChunk = this.getChunk((int)i, (int)j);
                if (isoChunk == null && j == 0 && (isoChunk = (IsoChunk)SharedChunks.get((Object)Integer.valueOf((int)(((n2 = this.WorldX - ChunkGridWidth / 2 + i) << 16) + (n = this.WorldY - ChunkGridWidth / 2 + j))))) != null) {
                    if (isoChunk.refs.contains((Object)this)) {
                        isoChunk.refs.remove((Object)this);
                        if (isoChunk.refs.isEmpty()) {
                            SharedChunks.remove((Object)Integer.valueOf((int)((isoChunk.wx << 16) + isoChunk.wy)));
                        }
                    }
                    isoChunk = null;
                }
                if (isoChunk != null && j == 0) {
                    isoChunk.refs.remove((Object)this);
                    if (isoChunk.refs.isEmpty()) {
                        SharedChunks.remove((Object)Integer.valueOf((int)((isoChunk.wx << 16) + isoChunk.wy)));
                        isoChunk.removeFromWorld();
                        ChunkSaveWorker.instance.Add((IsoChunk)isoChunk);
                    }
                }
                this.setChunk((int)i, (int)j, (IsoChunk)this.getChunk((int)i, (int)(j + 1)));
            }
            this.setChunk((int)i, (int)(ChunkGridWidth - 1), null);
        }
        ++this.WorldY;
    }

    private void Left() {
        for (int i = 0; i < ChunkGridWidth; ++i) {
            for (int j = ChunkGridWidth - 1; j > 0; --j) {
                int n;
                int n2;
                IsoChunk isoChunk = this.getChunk((int)j, (int)i);
                if (isoChunk == null && j == ChunkGridWidth - 1 && (isoChunk = (IsoChunk)SharedChunks.get((Object)Integer.valueOf((int)(((n2 = this.WorldX - ChunkGridWidth / 2 + j) << 16) + (n = this.WorldY - ChunkGridWidth / 2 + i))))) != null) {
                    if (isoChunk.refs.contains((Object)this)) {
                        isoChunk.refs.remove((Object)this);
                        if (isoChunk.refs.isEmpty()) {
                            SharedChunks.remove((Object)Integer.valueOf((int)((isoChunk.wx << 16) + isoChunk.wy)));
                        }
                    }
                    isoChunk = null;
                }
                if (isoChunk != null && j == ChunkGridWidth - 1) {
                    isoChunk.refs.remove((Object)this);
                    if (isoChunk.refs.isEmpty()) {
                        SharedChunks.remove((Object)Integer.valueOf((int)((isoChunk.wx << 16) + isoChunk.wy)));
                        isoChunk.removeFromWorld();
                        ChunkSaveWorker.instance.Add((IsoChunk)isoChunk);
                    }
                }
                this.setChunk((int)j, (int)i, (IsoChunk)this.getChunk((int)(j - 1), (int)i));
            }
            this.setChunk((int)0, (int)i, null);
        }
        --this.WorldX;
    }

    private void Right() {
        for (int i = 0; i < ChunkGridWidth; ++i) {
            for (int j = 0; j < ChunkGridWidth - 1; ++j) {
                int n;
                int n2;
                IsoChunk isoChunk = this.getChunk((int)j, (int)i);
                if (isoChunk == null && j == 0 && (isoChunk = (IsoChunk)SharedChunks.get((Object)Integer.valueOf((int)(((n2 = this.WorldX - ChunkGridWidth / 2 + j) << 16) + (n = this.WorldY - ChunkGridWidth / 2 + i))))) != null) {
                    if (isoChunk.refs.contains((Object)this)) {
                        isoChunk.refs.remove((Object)this);
                        if (isoChunk.refs.isEmpty()) {
                            SharedChunks.remove((Object)Integer.valueOf((int)((isoChunk.wx << 16) + isoChunk.wy)));
                        }
                    }
                    isoChunk = null;
                }
                if (isoChunk != null && j == 0) {
                    isoChunk.refs.remove((Object)this);
                    if (isoChunk.refs.isEmpty()) {
                        SharedChunks.remove((Object)Integer.valueOf((int)((isoChunk.wx << 16) + isoChunk.wy)));
                        isoChunk.removeFromWorld();
                        ChunkSaveWorker.instance.Add((IsoChunk)isoChunk);
                    }
                }
                this.setChunk((int)j, (int)i, (IsoChunk)this.getChunk((int)(j + 1), (int)i));
            }
            this.setChunk((int)(ChunkGridWidth - 1), (int)i, null);
        }
        ++this.WorldX;
    }

    public int getWorldXMin() {
        return this.WorldX - ChunkGridWidth / 2;
    }

    public int getWorldYMin() {
        return this.WorldY - ChunkGridWidth / 2;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     */
    public void ProcessChunkPos(IsoGameCharacter var1_1) {
        var2_2 = (int)var1_1.getX();
        var3_3 = (int)var1_1.getY();
        var4_4 = (int)var1_1.getZ();
        if (IsoPlayer.getInstance() != null && IsoPlayer.getInstance().getVehicle() != null) {
            var5_5 = IsoPlayer.getInstance();
            var6_7 = var5_5.getVehicle();
            var7_8 = var6_7.getCurrentSpeedKmHour() / 5.0f;
            if (!var5_5.isDriving()) {
                var7_8 = Math.min((float)(var7_8 * 2.0f), (float)20.0f);
            }
            var2_2 += Math.round((float)(var5_5.getForwardDirection().x * var7_8));
            var3_3 += Math.round((float)(var5_5.getForwardDirection().y * var7_8));
        }
        if ((var2_2 /= 10) == this.WorldX && (var3_3 /= 10) == this.WorldY) {
            return;
        }
        var5_6 = System.nanoTime();
        var7_9 = 0.0;
        IsoChunkMap.bSettingChunk.lock();
        var9_10 = System.nanoTime();
        try {
            if (Math.abs((int)(var2_2 - this.WorldX)) < IsoChunkMap.ChunkGridWidth && Math.abs((int)(var3_3 - this.WorldY)) < IsoChunkMap.ChunkGridWidth) ** GOTO lbl46
            if (LightingJNI.init) {
                LightingJNI.teleport((int)this.PlayerID, (int)(var2_2 - IsoChunkMap.ChunkGridWidth / 2), (int)(var3_3 - IsoChunkMap.ChunkGridWidth / 2));
            }
            this.Unload();
            var11_11 = IsoPlayer.players[this.PlayerID];
            var11_11.removeFromSquare();
            var11_11.square = null;
            this.WorldX = var2_2;
            this.WorldY = var3_3;
            if (!GameServer.bServer) {
                WorldSimulation.instance.activateChunkMap((int)this.PlayerID);
            }
            var12_13 = this.WorldX - IsoChunkMap.ChunkGridWidth / 2;
            var13_14 = this.WorldY - IsoChunkMap.ChunkGridWidth / 2;
            var14_15 = this.WorldX + IsoChunkMap.ChunkGridWidth / 2;
            var15_16 = this.WorldY + IsoChunkMap.ChunkGridWidth / 2;
            for (var16_17 = var12_13; var16_17 <= var14_15; ++var16_17) {
                for (var17_18 = var13_14; var17_18 <= var15_16; ++var17_18) {
                    this.LoadChunkForLater((int)var16_17, (int)var17_18, (int)(var16_17 - var12_13), (int)(var17_18 - var13_14));
                }
            }
            this.SwapChunkBuffers();
            this.UpdateCellCache();
            if (!IsoWorld.instance.getCell().getObjectList().contains((Object)var11_11)) {
                IsoWorld.instance.getCell().getAddList().add((Object)var11_11);
            }
            ** GOTO lbl61
lbl46:
            // 1 sources

            if (var2_2 == this.WorldX) ** GOTO lbl52
            if (var2_2 < this.WorldX) {
                this.LoadLeft();
            } else {
                this.LoadRight();
            }
            ** GOTO lbl61
lbl52:
            // 1 sources

            if (var3_3 == this.WorldY) ** GOTO lbl61
            if (var3_3 >= this.WorldY) ** GOTO lbl56
            this.LoadUp();
            ** GOTO lbl61
lbl56:
            // 1 sources

            this.LoadDown();
        }
        catch (Throwable var18_19) {
            IsoChunkMap.bSettingChunk.unlock();
            throw var18_19;
        }
lbl61:
        // 6 sources

        IsoChunkMap.bSettingChunk.unlock();
        var7_9 = (double)(System.nanoTime() - var9_10) / 1000000.0;
        var11_12 = (double)(System.nanoTime() - var5_6) / 1000000.0;
        if (LightingThread.DebugLockTime && var11_12 > 10.0) {
            DebugLog.log((String)("ProcessChunkPos time " + var7_9 + "/" + var11_12 + " ms"));
        }
    }

    public IsoRoom getRoom(int n) {
        return null;
    }

    public int getWidthInTiles() {
        return ChunkWidthInTiles;
    }

    public int getWorldXMinTiles() {
        if (this.XMinTiles != -1) {
            return this.XMinTiles;
        }
        this.XMinTiles = this.getWorldXMin() * 10;
        return this.XMinTiles;
    }

    public int getWorldYMinTiles() {
        if (this.YMinTiles != -1) {
            return this.YMinTiles;
        }
        this.YMinTiles = this.getWorldYMin() * 10;
        return this.YMinTiles;
    }

    public int getWorldXMaxTiles() {
        if (this.XMaxTiles != -1) {
            return this.XMaxTiles;
        }
        this.XMaxTiles = this.getWorldXMin() * 10 + this.getWidthInTiles();
        return this.XMaxTiles;
    }

    public int getWorldYMaxTiles() {
        if (this.YMaxTiles != -1) {
            return this.YMaxTiles;
        }
        this.YMaxTiles = this.getWorldYMin() * 10 + this.getWidthInTiles();
        return this.YMaxTiles;
    }

    public void Save() {
        if (GameServer.bServer) {
            return;
        }
        for (int i = 0; i < ChunkGridWidth; ++i) {
            for (int j = 0; j < ChunkGridWidth; ++j) {
                IsoChunk isoChunk = this.getChunk((int)i, (int)j);
                if (isoChunk == null || saveList.contains((Object)isoChunk)) continue;
                try {
                    isoChunk.Save((boolean)true);
                    continue;
                }
                catch (IOException iOException) {
                    iOException.printStackTrace();
                }
            }
        }
    }

    public void renderBloodForChunks(int n) {
        Object object;
        int n2;
        if (!DebugOptions.instance.Terrain.RenderTiles.BloodDecals.getValue()) {
            return;
        }
        if ((float)n > IsoCamera.CamCharacter.z) {
            return;
        }
        if (Core.OptionBloodDecals == 0) {
            return;
        }
        float f = (float)GameTime.getInstance().getWorldAgeHours();
        int n3 = IsoCamera.frameState.playerIndex;
        for (n2 = 0; n2 < IsoFloorBloodSplat.FloorBloodTypes.length; ++n2) {
            ((ArrayList)splatByType.get((int)n2)).clear();
        }
        for (n2 = 0; n2 < ChunkGridWidth; ++n2) {
            for (int i = 0; i < ChunkGridWidth; ++i) {
                IsoFloorBloodSplat isoFloorBloodSplat;
                int n4;
                object = this.getChunk((int)n2, (int)i);
                if (object == null) continue;
                for (n4 = 0; n4 < ((IsoChunk)object).FloorBloodSplatsFade.size(); ++n4) {
                    isoFloorBloodSplat = (IsoFloorBloodSplat)((IsoChunk)object).FloorBloodSplatsFade.get((int)n4);
                    if (isoFloorBloodSplat.index >= 1 && isoFloorBloodSplat.index <= 10 && IsoChunk.renderByIndex[Core.OptionBloodDecals - 1][isoFloorBloodSplat.index - 1] == 0 || (int)isoFloorBloodSplat.z != n || isoFloorBloodSplat.Type < 0 || isoFloorBloodSplat.Type >= IsoFloorBloodSplat.FloorBloodTypes.length) continue;
                    isoFloorBloodSplat.chunk = object;
                    ((ArrayList)splatByType.get((int)isoFloorBloodSplat.Type)).add((Object)isoFloorBloodSplat);
                }
                if (((IsoChunk)object).FloorBloodSplats.isEmpty()) continue;
                for (n4 = 0; n4 < ((IsoChunk)object).FloorBloodSplats.size(); ++n4) {
                    isoFloorBloodSplat = (IsoFloorBloodSplat)((IsoChunk)object).FloorBloodSplats.get((int)n4);
                    if (isoFloorBloodSplat.index >= 1 && isoFloorBloodSplat.index <= 10 && IsoChunk.renderByIndex[Core.OptionBloodDecals - 1][isoFloorBloodSplat.index - 1] == 0 || (int)isoFloorBloodSplat.z != n || isoFloorBloodSplat.Type < 0 || isoFloorBloodSplat.Type >= IsoFloorBloodSplat.FloorBloodTypes.length) continue;
                    isoFloorBloodSplat.chunk = object;
                    ((ArrayList)splatByType.get((int)isoFloorBloodSplat.Type)).add((Object)isoFloorBloodSplat);
                }
            }
        }
        for (n2 = 0; n2 < splatByType.size(); ++n2) {
            ArrayList arrayList = (ArrayList)splatByType.get((int)n2);
            if (arrayList.isEmpty()) continue;
            object = IsoFloorBloodSplat.FloorBloodTypes[n2];
            IsoSprite isoSprite = null;
            if (!IsoFloorBloodSplat.SpriteMap.containsKey((Object)object)) {
                IsoSprite isoSprite2 = IsoSprite.CreateSprite((IsoSpriteManager)IsoSpriteManager.instance);
                isoSprite2.LoadFramesPageSimple((String)object, (String)object, (String)object, (String)object);
                IsoFloorBloodSplat.SpriteMap.put((Object)object, (Object)isoSprite2);
                isoSprite = isoSprite2;
            } else {
                isoSprite = (IsoSprite)IsoFloorBloodSplat.SpriteMap.get((Object)object);
            }
            for (int i = 0; i < arrayList.size(); ++i) {
                IsoGridSquare isoGridSquare;
                IsoFloorBloodSplat isoFloorBloodSplat = (IsoFloorBloodSplat)arrayList.get((int)i);
                IsoChunkMap.inf.r = 1.0f;
                IsoChunkMap.inf.g = 1.0f;
                IsoChunkMap.inf.b = 1.0f;
                IsoChunkMap.inf.a = 0.27f;
                float f2 = (isoFloorBloodSplat.x + isoFloorBloodSplat.y / isoFloorBloodSplat.x) * (float)(isoFloorBloodSplat.Type + 1);
                float f3 = f2 * isoFloorBloodSplat.x / isoFloorBloodSplat.y * (float)(isoFloorBloodSplat.Type + 1) / (f2 + isoFloorBloodSplat.y);
                float f4 = f3 * f2 * f3 * isoFloorBloodSplat.x / (isoFloorBloodSplat.y + 2.0f);
                f2 *= 42367.543f;
                f3 *= 6367.123f;
                f4 *= 23367.133f;
                f2 %= 1000.0f;
                f3 %= 1000.0f;
                f4 %= 1000.0f;
                f2 /= 1000.0f;
                f3 /= 1000.0f;
                f4 /= 1000.0f;
                if (f2 > 0.25f) {
                    f2 = 0.25f;
                }
                IsoChunkMap.inf.r -= f2 * 2.0f;
                IsoChunkMap.inf.g -= f2 * 2.0f;
                IsoChunkMap.inf.b -= f2 * 2.0f;
                IsoChunkMap.inf.r += f3 / 3.0f;
                IsoChunkMap.inf.g -= f4 / 3.0f;
                IsoChunkMap.inf.b -= f4 / 3.0f;
                float f5 = f - isoFloorBloodSplat.worldAge;
                if (f5 >= 0.0f && f5 < 72.0f) {
                    float f6 = 1.0f - f5 / 72.0f;
                    IsoChunkMap.inf.r *= 0.2f + f6 * 0.8f;
                    IsoChunkMap.inf.g *= 0.2f + f6 * 0.8f;
                    IsoChunkMap.inf.b *= 0.2f + f6 * 0.8f;
                    IsoChunkMap.inf.a *= 0.25f + f6 * 0.75f;
                } else {
                    IsoChunkMap.inf.r *= 0.2f;
                    IsoChunkMap.inf.g *= 0.2f;
                    IsoChunkMap.inf.b *= 0.2f;
                    IsoChunkMap.inf.a *= 0.25f;
                }
                if (isoFloorBloodSplat.fade > 0) {
                    IsoChunkMap.inf.a *= (float)isoFloorBloodSplat.fade / ((float)PerformanceSettings.getLockFPS() * 5.0f);
                    if (--isoFloorBloodSplat.fade == 0) {
                        isoFloorBloodSplat.chunk.FloorBloodSplatsFade.remove((Object)isoFloorBloodSplat);
                    }
                }
                if ((isoGridSquare = isoFloorBloodSplat.chunk.getGridSquare((int)((int)isoFloorBloodSplat.x), (int)((int)isoFloorBloodSplat.y), (int)((int)isoFloorBloodSplat.z))) != null) {
                    int n5 = isoGridSquare.getVertLight((int)0, (int)n3);
                    int n6 = isoGridSquare.getVertLight((int)1, (int)n3);
                    int n7 = isoGridSquare.getVertLight((int)2, (int)n3);
                    int n8 = isoGridSquare.getVertLight((int)3, (int)n3);
                    float f7 = Color.getRedChannelFromABGR((int)n5);
                    float f8 = Color.getGreenChannelFromABGR((int)n5);
                    float f9 = Color.getBlueChannelFromABGR((int)n5);
                    float f10 = Color.getRedChannelFromABGR((int)n6);
                    float f11 = Color.getGreenChannelFromABGR((int)n6);
                    float f12 = Color.getBlueChannelFromABGR((int)n6);
                    float f13 = Color.getRedChannelFromABGR((int)n7);
                    float f14 = Color.getGreenChannelFromABGR((int)n7);
                    float f15 = Color.getBlueChannelFromABGR((int)n7);
                    float f16 = Color.getRedChannelFromABGR((int)n8);
                    float f17 = Color.getGreenChannelFromABGR((int)n8);
                    float f18 = Color.getBlueChannelFromABGR((int)n8);
                    IsoChunkMap.inf.r *= (f7 + f10 + f13 + f16) / 4.0f;
                    IsoChunkMap.inf.g *= (f8 + f11 + f14 + f17) / 4.0f;
                    IsoChunkMap.inf.b *= (f9 + f12 + f15 + f18) / 4.0f;
                }
                isoSprite.renderBloodSplat((float)((float)(isoFloorBloodSplat.chunk.wx * 10) + isoFloorBloodSplat.x), (float)((float)(isoFloorBloodSplat.chunk.wy * 10) + isoFloorBloodSplat.y), (float)isoFloorBloodSplat.z, (ColorInfo)inf);
            }
        }
    }

    public void copy(IsoChunkMap isoChunkMap) {
        IsoChunkMap isoChunkMap2 = this;
        isoChunkMap2.WorldX = isoChunkMap.WorldX;
        isoChunkMap2.WorldY = isoChunkMap.WorldY;
        isoChunkMap2.XMinTiles = -1;
        isoChunkMap2.YMinTiles = -1;
        isoChunkMap2.XMaxTiles = -1;
        isoChunkMap2.YMaxTiles = -1;
        for (int i = 0; i < ChunkGridWidth * ChunkGridWidth; ++i) {
            isoChunkMap2.bReadBufferA = isoChunkMap.bReadBufferA;
            if (isoChunkMap2.bReadBufferA) {
                if (isoChunkMap.chunksSwapA[i] == null) continue;
                isoChunkMap.chunksSwapA[i].refs.add((Object)isoChunkMap2);
                isoChunkMap2.chunksSwapA[i] = isoChunkMap.chunksSwapA[i];
                continue;
            }
            if (isoChunkMap.chunksSwapB[i] == null) continue;
            isoChunkMap.chunksSwapB[i].refs.add((Object)isoChunkMap2);
            isoChunkMap2.chunksSwapB[i] = isoChunkMap.chunksSwapB[i];
        }
    }

    public void Unload() {
        for (int i = 0; i < ChunkGridWidth; ++i) {
            for (int j = 0; j < ChunkGridWidth; ++j) {
                IsoChunk isoChunk = this.getChunk((int)j, (int)i);
                if (isoChunk == null) continue;
                if (isoChunk.refs.contains((Object)this)) {
                    isoChunk.refs.remove((Object)this);
                    if (isoChunk.refs.isEmpty()) {
                        SharedChunks.remove((Object)Integer.valueOf((int)((isoChunk.wx << 16) + isoChunk.wy)));
                        isoChunk.removeFromWorld();
                        ChunkSaveWorker.instance.Add((IsoChunk)isoChunk);
                    }
                }
                this.chunksSwapA[i * IsoChunkMap.ChunkGridWidth + j] = null;
                this.chunksSwapB[i * IsoChunkMap.ChunkGridWidth + j] = null;
            }
        }
        WorldSimulation.instance.deactivateChunkMap((int)this.PlayerID);
        this.XMinTiles = -1;
        this.XMaxTiles = -1;
        this.YMinTiles = -1;
        this.YMaxTiles = -1;
        if (IsoWorld.instance != null && IsoWorld.instance.CurrentCell != null) {
            IsoWorld.instance.CurrentCell.clearCacheGridSquare((int)this.PlayerID);
        }
    }

    static {
        $assertionsDisabled = !IsoChunkMap.class.desiredAssertionStatus();
        SharedChunks = new HashMap();
        MPWorldXA = 0;
        MPWorldYA = 0;
        MPWorldZA = 0;
        WorldXA = 11702;
        WorldYA = 6896;
        WorldZA = 0;
        SWorldX = new int[4];
        SWorldY = new int[4];
        chunkStore = new ConcurrentLinkedQueue();
        bSettingChunk = new ReentrantLock((boolean)true);
        ChunkGridWidth = StartChunkGridWidth = 13;
        ChunkWidthInTiles = 10 * ChunkGridWidth;
        inf = new ColorInfo();
        saveList = new ArrayList();
        splatByType = new ArrayList();
        for (int i = 0; i < IsoFloorBloodSplat.FloorBloodTypes.length; ++i) {
            splatByType.add((Object)new ArrayList());
        }
    }
}
