/*
 * Decompiled with CFR.
 */
package zombie;

import java.io.File;
import java.lang.Exception;
import java.lang.InterruptedException;
import java.lang.Object;
import java.lang.String;
import java.lang.System;
import java.lang.Thread;
import java.lang.Throwable;
import java.nio.ByteBuffer;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.MapCollisionData;
import zombie.SandboxOptions;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.gameStates.IngameState;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLot;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaChunk;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.popman.ZombiePopulationManager;

public final class MapCollisionData {
    public static final MapCollisionData instance;
    public static final byte BIT_SOLID = 1;
    public static final byte BIT_WALLN = 2;
    public static final byte BIT_WALLW = 4;
    public static final byte BIT_WATER = 8;
    public static final byte BIT_ROOM = 16;
    private static final int SQUARES_PER_CHUNK = 10;
    private static final int CHUNKS_PER_CELL = 30;
    private static final int SQUARES_PER_CELL = 300;
    private static int[] curXY;
    public final Object renderLock;
    private final Stack<PathTask> freePathTasks;
    private final ConcurrentLinkedQueue<PathTask> pathTaskQueue;
    private final ConcurrentLinkedQueue<PathTask> pathResultQueue;
    private final Sync sync;
    private final byte[] squares;
    private final int SQUARE_UPDATE_SIZE = 9;
    private final ByteBuffer squareUpdateBuffer;
    private boolean bClient;
    private boolean bPaused;
    private boolean bNoSave;
    private MCDThread thread;
    private long lastUpdate;

    public MapCollisionData() {
        super();
        this.renderLock = new Object();
        this.freePathTasks = new Stack();
        this.pathTaskQueue = new ConcurrentLinkedQueue();
        this.pathResultQueue = new ConcurrentLinkedQueue();
        this.sync = new Sync();
        this.squares = new byte[100];
        this.SQUARE_UPDATE_SIZE = 9;
        this.squareUpdateBuffer = ByteBuffer.allocateDirect((int)1024);
    }

    private static native void n_init(int var0, int var1, int var2, int var3);

    private static native void n_chunkUpdateTask(int var0, int var1, byte[] var2);

    private static native void n_squareUpdateTask(int var0, ByteBuffer var1);

    private static native int n_pathTask(int var0, int var1, int var2, int var3, int[] var4);

    private static native boolean n_hasDataForThread();

    private static native boolean n_shouldWait();

    private static native void n_update();

    private static native void n_save();

    private static native void n_stop();

    private static native void n_setGameState(String var0, boolean var1);

    private static native void n_setGameState(String var0, double var1);

    private static native void n_setGameState(String var0, float var1);

    private static native void n_setGameState(String var0, int var1);

    private static native void n_setGameState(String var0, String var1);

    private static native void n_initMetaGrid(int var0, int var1, int var2, int var3);

    private static native void n_initMetaCell(int var0, int var1, String var2);

    private static native void n_initMetaChunk(int var0, int var1, int var2, int var3, int var4);

    private static void writeToStdErr(String string) {
        System.err.println((String)string);
    }

    public void init(IsoMetaGrid isoMetaGrid) {
        this.bClient = GameClient.bClient;
        if (this.bClient) {
            return;
        }
        int n = isoMetaGrid.getMinX();
        int n2 = isoMetaGrid.getMinY();
        int n3 = isoMetaGrid.getWidth();
        int n4 = isoMetaGrid.getHeight();
        MapCollisionData.n_setGameState((String)"Core.GameMode", (String)Core.getInstance().getGameMode());
        MapCollisionData.n_setGameState((String)"Core.GameSaveWorld", (String)Core.GameSaveWorld);
        MapCollisionData.n_setGameState((String)"Core.bLastStand", (boolean)Core.bLastStand);
        this.bNoSave = Core.getInstance().isNoSave();
        MapCollisionData.n_setGameState((String)"Core.noSave", (boolean)this.bNoSave);
        MapCollisionData.n_setGameState((String)"GameWindow.CacheDir", (String)ZomboidFileSystem.instance.getCacheDir());
        MapCollisionData.n_setGameState((String)"GameWindow.GameModeCacheDir", (String)(ZomboidFileSystem.instance.getGameModeCacheDir() + File.separator));
        MapCollisionData.n_setGameState((String)"GameWindow.SaveDir", (String)ZomboidFileSystem.instance.getSaveDir());
        MapCollisionData.n_setGameState((String)"SandboxOptions.Distribution", (int)SandboxOptions.instance.Distribution.getValue());
        MapCollisionData.n_setGameState((String)"SandboxOptions.Zombies", (int)SandboxOptions.instance.Zombies.getValue());
        MapCollisionData.n_setGameState((String)"World.ZombiesDisabled", (boolean)IsoWorld.getZombiesDisabled());
        this.bPaused = true;
        MapCollisionData.n_setGameState((String)"PAUSED", (boolean)true);
        MapCollisionData.n_initMetaGrid((int)n, (int)n2, (int)n3, (int)n4);
        for (int i = n2; i < n2 + n4; ++i) {
            for (int j = n; j < n + n3; ++j) {
                IsoMetaCell isoMetaCell = isoMetaGrid.getCellData((int)j, (int)i);
                MapCollisionData.n_initMetaCell((int)j, (int)i, (String)((String)IsoLot.InfoFileNames.get((Object)("chunkdata_" + j + "_" + i + ".bin"))));
                if (isoMetaCell == null) continue;
                for (int k = 0; k < 30; ++k) {
                    for (int i2 = 0; i2 < 30; ++i2) {
                        IsoMetaChunk isoMetaChunk = isoMetaCell.getChunk((int)i2, (int)k);
                        if (isoMetaChunk == null) continue;
                        MapCollisionData.n_initMetaChunk((int)j, (int)i, (int)i2, (int)k, (int)isoMetaChunk.getUnadjustedZombieIntensity());
                    }
                }
            }
        }
        MapCollisionData.n_init((int)n, (int)n2, (int)n3, (int)n4);
    }

    public void start() {
        if (this.bClient) {
            return;
        }
        if (this.thread != null) {
            return;
        }
        this.thread = new MCDThread((MapCollisionData)this);
        this.thread.setDaemon((boolean)true);
        this.thread.setName((String)"MapCollisionDataJNI");
        if (GameServer.bServer) {
            this.thread.start();
        }
    }

    public void startGame() {
        if (GameClient.bClient) {
            return;
        }
        this.updateMain();
        ZombiePopulationManager.instance.updateMain();
        MapCollisionData.n_update();
        ZombiePopulationManager.instance.updateThread();
        this.updateMain();
        ZombiePopulationManager.instance.updateMain();
        this.thread.start();
    }

    public void updateMain() {
        if (this.bClient) {
            return;
        }
        PathTask pathTask = (PathTask)this.pathResultQueue.poll();
        while (pathTask != null) {
            pathTask.result.finished((int)pathTask.status, (int)pathTask.curX, (int)pathTask.curY);
            pathTask.release();
            pathTask = (PathTask)this.pathResultQueue.poll();
        }
        long l = System.currentTimeMillis();
        if (l - this.lastUpdate > 10000L) {
            this.lastUpdate = l;
            this.notifyThread();
        }
    }

    public boolean hasDataForThread() {
        if (this.squareUpdateBuffer.position() > 0) {
            try {
                MapCollisionData.n_squareUpdateTask((int)(this.squareUpdateBuffer.position() / 9), (ByteBuffer)this.squareUpdateBuffer);
            }
            catch (Throwable throwable) {
                this.squareUpdateBuffer.clear();
                throw throwable;
            }
            this.squareUpdateBuffer.clear();
        }
        return MapCollisionData.n_hasDataForThread();
    }

    public void updateGameState() {
        boolean bl = Core.getInstance().isNoSave();
        if (this.bNoSave != bl) {
            this.bNoSave = bl;
            MapCollisionData.n_setGameState((String)"Core.noSave", (boolean)this.bNoSave);
        }
        boolean bl2 = GameTime.isGamePaused();
        if (GameWindow.states.current != IngameState.instance) {
            bl2 = true;
        }
        if (GameServer.bServer) {
            bl2 = IngameState.instance.Paused;
        }
        if (bl2 != this.bPaused) {
            this.bPaused = bl2;
            MapCollisionData.n_setGameState((String)"PAUSED", (boolean)this.bPaused);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void notifyThread() {
        Object object = this.thread.notifier;
        synchronized (object) {
            this.thread.notifier.notify();
        }
    }

    public void addChunkToWorld(IsoChunk isoChunk) {
        if (this.bClient) {
            return;
        }
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                IsoGridSquare isoGridSquare = isoChunk.getGridSquare((int)j, (int)i, (int)0);
                if (isoGridSquare == null) {
                    this.squares[j + i * 10] = 1;
                    continue;
                }
                byte by = 0;
                if (this.isSolid((IsoGridSquare)isoGridSquare)) {
                    by = (byte)(by | 1);
                }
                if (this.isBlockedN((IsoGridSquare)isoGridSquare)) {
                    by = (byte)(by | 2);
                }
                if (this.isBlockedW((IsoGridSquare)isoGridSquare)) {
                    by = (byte)(by | 4);
                }
                if (this.isWater((IsoGridSquare)isoGridSquare)) {
                    by = (byte)(by | 8);
                }
                if (this.isRoom((IsoGridSquare)isoGridSquare)) {
                    by = (byte)(by | 0x10);
                }
                this.squares[j + i * 10] = by;
            }
        }
        MapCollisionData.n_chunkUpdateTask((int)isoChunk.wx, (int)isoChunk.wy, (byte[])this.squares);
    }

    public void removeChunkFromWorld(IsoChunk isoChunk) {
        if (this.bClient) {
            return;
        }
    }

    public void squareChanged(IsoGridSquare isoGridSquare) {
        if (this.bClient) {
            return;
        }
        try {
            byte by = 0;
            if (this.isSolid((IsoGridSquare)isoGridSquare)) {
                by = (byte)(by | 1);
            }
            if (this.isBlockedN((IsoGridSquare)isoGridSquare)) {
                by = (byte)(by | 2);
            }
            if (this.isBlockedW((IsoGridSquare)isoGridSquare)) {
                by = (byte)(by | 4);
            }
            if (this.isWater((IsoGridSquare)isoGridSquare)) {
                by = (byte)(by | 8);
            }
            if (this.isRoom((IsoGridSquare)isoGridSquare)) {
                by = (byte)(by | 0x10);
            }
            this.squareUpdateBuffer.putInt((int)isoGridSquare.x);
            this.squareUpdateBuffer.putInt((int)isoGridSquare.y);
            this.squareUpdateBuffer.put((byte)by);
            if (this.squareUpdateBuffer.remaining() < 9) {
                MapCollisionData.n_squareUpdateTask((int)(this.squareUpdateBuffer.position() / 9), (ByteBuffer)this.squareUpdateBuffer);
                this.squareUpdateBuffer.clear();
            }
        }
        catch (Exception exception) {
            ExceptionLogger.logException((Throwable)exception);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void save() {
        if (this.bClient) {
            return;
        }
        ZombiePopulationManager.instance.beginSaveRealZombies();
        if (!this.thread.isAlive()) {
            MapCollisionData.n_save();
            ZombiePopulationManager.instance.save();
            return;
        }
        this.thread.bSave = true;
        Object object = this.thread.notifier;
        synchronized (object) {
            this.thread.notifier.notify();
        }
        while (this.thread.bSave) {
            try {
                Thread.sleep((long)5L);
            }
            catch (InterruptedException interruptedException) {}
        }
        ZombiePopulationManager.instance.endSaveRealZombies();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void stop() {
        if (this.bClient) {
            return;
        }
        this.thread.bStop = true;
        Object object = this.thread.notifier;
        synchronized (object) {
            this.thread.notifier.notify();
        }
        while (this.thread.isAlive()) {
            try {
                Thread.sleep((long)5L);
            }
            catch (InterruptedException interruptedException) {}
        }
        MapCollisionData.n_stop();
        this.thread = null;
        this.pathTaskQueue.clear();
        this.pathResultQueue.clear();
        this.squareUpdateBuffer.clear();
    }

    private boolean isSolid(IsoGridSquare isoGridSquare) {
        boolean bl;
        boolean bl2 = bl = isoGridSquare.isSolid() || isoGridSquare.isSolidTrans();
        if (isoGridSquare.HasStairs()) {
            bl = true;
        }
        if (isoGridSquare.Is((IsoFlagType)IsoFlagType.water)) {
            bl = false;
        }
        if (isoGridSquare.Has((IsoObjectType)IsoObjectType.tree)) {
            bl = false;
        }
        return bl;
    }

    private boolean isBlockedN(IsoGridSquare isoGridSquare) {
        if (isoGridSquare.Is((IsoFlagType)IsoFlagType.HoppableN)) {
            return false;
        }
        boolean bl = isoGridSquare.Is((IsoFlagType)IsoFlagType.collideN);
        if (isoGridSquare.Has((IsoObjectType)IsoObjectType.doorFrN)) {
            bl = true;
        }
        if (isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.DoorWallN)) {
            bl = true;
        }
        if (isoGridSquare.Has((IsoObjectType)IsoObjectType.windowFN)) {
            bl = true;
        }
        if (isoGridSquare.Is((IsoFlagType)IsoFlagType.windowN)) {
            bl = true;
        }
        if (isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.WindowN)) {
            bl = true;
        }
        return bl;
    }

    private boolean isBlockedW(IsoGridSquare isoGridSquare) {
        if (isoGridSquare.Is((IsoFlagType)IsoFlagType.HoppableW)) {
            return false;
        }
        boolean bl = isoGridSquare.Is((IsoFlagType)IsoFlagType.collideW);
        if (isoGridSquare.Has((IsoObjectType)IsoObjectType.doorFrW)) {
            bl = true;
        }
        if (isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.DoorWallW)) {
            bl = true;
        }
        if (isoGridSquare.Has((IsoObjectType)IsoObjectType.windowFW)) {
            bl = true;
        }
        if (isoGridSquare.Is((IsoFlagType)IsoFlagType.windowW)) {
            bl = true;
        }
        if (isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.WindowW)) {
            bl = true;
        }
        return bl;
    }

    private boolean isWater(IsoGridSquare isoGridSquare) {
        boolean bl = isoGridSquare.Is((IsoFlagType)IsoFlagType.water);
        return bl;
    }

    private boolean isRoom(IsoGridSquare isoGridSquare) {
        return isoGridSquare.getRoom() != null;
    }

    static {
        instance = new MapCollisionData();
        curXY = new int[2];
    }
}
