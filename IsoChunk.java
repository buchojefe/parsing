/*
 * Decompiled with CFR.
 */
package zombie.iso;

import gnu.trove.list.array.TIntArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.AssertionError;
import java.lang.CharSequence;
import java.lang.Deprecated;
import java.lang.Exception;
import java.lang.Math;
import java.lang.Object;
import java.lang.RuntimeException;
import java.lang.String;
import java.lang.System;
import java.lang.Throwable;
import java.lang.invoke.StringConcatFactory;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.CRC32;
import org.joml.Quaternionf;
import zombie.ChunkMapFilenames;
import zombie.FliesSound;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.LoadGridsquarePerformanceWorkaround;
import zombie.LootRespawn;
import zombie.Lua.LuaEventManager;
import zombie.Lua.MapObjects;
import zombie.MapCollisionData;
import zombie.ReanimatedPlayers;
import zombie.SandboxOptions;
import zombie.SystemDisabler;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.ZombieSpawnRecorder;
import zombie.ZomboidFileSystem;
import zombie.audio.ObjectAmbientEmitters;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.Rand;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.physics.Bullet;
import zombie.core.physics.WorldSimulation;
import zombie.core.properties.PropertyContainer;
import zombie.core.raknet.UdpConnection;
import zombie.core.stash.StashSystem;
import zombie.core.utils.BoundedQueue;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.erosion.ErosionData;
import zombie.erosion.ErosionMain;
import zombie.globalObjects.SGlobalObjects;
import zombie.iso.BuildingDef;
import zombie.iso.CellLoader;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoFloorBloodSplat;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLot;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaChunk;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoRoomLight;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.LotHeader;
import zombie.iso.NearestWalls;
import zombie.iso.RoomDef;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.Vector2;
import zombie.iso.WorldReuserThread;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.RainManager;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.ChunkChecksum;
import zombie.network.ClientChunkRequest;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.MPStatistics;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.popman.ZombiePopulationManager;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedVehicleStory.VehicleStorySpawnData;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.VehicleScript;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.CollideWithObstaclesPoly;
import zombie.vehicles.PolygonalMap2;
import zombie.vehicles.VehicleType;
import zombie.vehicles.VehiclesDB2;

public final class IsoChunk {
    public static boolean bDoServerRequests;
    public int wx;
    public int wy;
    public final IsoGridSquare[][] squares;
    public FliesSound.ChunkData corpseData;
    public final NearestWalls.ChunkData nearestWalls;
    private ArrayList<IsoGameCharacter.Location> generatorsTouchingThisChunk;
    public int maxLevel;
    public final ArrayList<WorldSoundManager.WorldSound> SoundList;
    private int m_treeCount;
    private int m_numberOfWaterTiles;
    private IsoMetaGrid.Zone m_scavengeZone;
    private final TIntArrayList m_spawnedRooms;
    public IsoChunk next;
    public final CollideWithObstaclesPoly.ChunkData collision;
    public int m_adjacentChunkLoadedCounter;
    public VehicleStorySpawnData m_vehicleStorySpawnData;
    public Object m_loadVehiclesObject;
    public final ObjectAmbientEmitters.ChunkData m_objectEmitterData;
    public JobType jobType;
    public LotHeader lotheader;
    public final BoundedQueue<IsoFloorBloodSplat> FloorBloodSplats;
    public final ArrayList<IsoFloorBloodSplat> FloorBloodSplatsFade;
    private static final int MAX_BLOOD_SPLATS = 1000;
    private int nextSplatIndex;
    public static final byte[][] renderByIndex;
    public final ArrayList<IsoChunkMap> refs;
    public boolean bLoaded;
    private boolean blam;
    private boolean addZombies;
    private boolean bFixed2x;
    public final boolean[] lightCheck;
    public final boolean[] bLightingNeverDone;
    public final ArrayList<IsoRoomLight> roomLights;
    public final ArrayList<BaseVehicle> vehicles;
    public int lootRespawnHour;
    private long hashCodeObjects;
    public int ObjectsSyncCount;
    private static int AddVehicles_ForTest_vtype;
    private static int AddVehicles_ForTest_vskin;
    private static int AddVehicles_ForTest_vrot;
    private static final ArrayList<BaseVehicle> BaseVehicleCheckedVehicles;
    protected boolean physicsCheck;
    private static final int MAX_SHAPES = 4;
    private final PhysicsShapes[] shapes;
    private static final byte[] bshapes;
    private static final ChunkGetter chunkGetter;
    private boolean loadedPhysics;
    public final Object vehiclesForAddToWorldLock;
    public ArrayList<BaseVehicle> vehiclesForAddToWorld;
    public static final ConcurrentLinkedQueue<IsoChunk> loadGridSquare;
    private static final int BLOCK_SIZE = 65536;
    private static ByteBuffer SliceBuffer;
    private static ByteBuffer SliceBufferLoad;
    public static final Object WriteLock;
    private static final ArrayList<RoomDef> tempRoomDefs;
    private static final ArrayList<IsoBuilding> tempBuildings;
    private static final ArrayList<ChunkLock> Locks;
    private static final Stack<ChunkLock> FreeLocks;
    private static final SanityCheck sanityCheck;
    private static final CRC32 crcLoad;
    private static final CRC32 crcSave;
    private static String prefix;
    private ErosionData.Chunk erosion;
    private static final HashMap<String, String> Fix2xMap;
    public int randomID;
    public long revision;
    static final /* synthetic */ boolean $assertionsDisabled;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void updateSounds() {
        ArrayList arrayList = WorldSoundManager.instance.SoundList;
        synchronized (arrayList) {
            int n = this.SoundList.size();
            for (int i = 0; i < n; ++i) {
                WorldSoundManager.WorldSound worldSound = (WorldSoundManager.WorldSound)this.SoundList.get((int)i);
                if (worldSound != null && worldSound.life > 0) continue;
                this.SoundList.remove((int)i);
                --i;
                --n;
            }
        }
    }

    public IsoChunk(IsoCell isoCell) {
        super();
        this.wx = 0;
        this.wy = 0;
        this.nearestWalls = new NearestWalls.ChunkData();
        this.maxLevel = -1;
        this.SoundList = new ArrayList();
        this.m_treeCount = 0;
        this.m_numberOfWaterTiles = 0;
        this.m_scavengeZone = null;
        this.m_spawnedRooms = new TIntArrayList();
        this.collision = new CollideWithObstaclesPoly.ChunkData();
        this.m_adjacentChunkLoadedCounter = 0;
        this.m_loadVehiclesObject = null;
        this.m_objectEmitterData = new ObjectAmbientEmitters.ChunkData();
        this.jobType = JobType.None;
        this.FloorBloodSplats = new BoundedQueue((int)1000);
        this.FloorBloodSplatsFade = new ArrayList();
        this.refs = new ArrayList();
        this.lightCheck = new boolean[4];
        this.bLightingNeverDone = new boolean[4];
        this.roomLights = new ArrayList();
        this.vehicles = new ArrayList();
        this.lootRespawnHour = -1;
        this.ObjectsSyncCount = 0;
        this.physicsCheck = false;
        this.shapes = new PhysicsShapes[4];
        this.loadedPhysics = false;
        this.vehiclesForAddToWorldLock = new Object();
        this.vehiclesForAddToWorld = null;
        this.squares = new IsoGridSquare[8][100];
        for (int i = 0; i < 4; ++i) {
            this.lightCheck[i] = true;
            this.bLightingNeverDone[i] = true;
        }
        MPStatistics.increaseRelevantChunk();
    }

    @Deprecated
    public long getHashCodeObjects() {
        this.recalcHashCodeObjects();
        return this.hashCodeObjects;
    }

    @Deprecated
    public void recalcHashCodeObjects() {
        long l;
        this.hashCodeObjects = l = 0L;
    }

    @Deprecated
    public int hashCodeNoOverride() {
        return (int)this.hashCodeObjects;
    }

    public void addBloodSplat(float f, float f2, float f3, int n) {
        if (f < (float)(this.wx * 10) || f >= (float)((this.wx + 1) * 10)) {
            return;
        }
        if (f2 < (float)(this.wy * 10) || f2 >= (float)((this.wy + 1) * 10)) {
            return;
        }
        IsoGridSquare isoGridSquare = this.getGridSquare((int)((int)(f - (float)(this.wx * 10))), (int)((int)(f2 - (float)(this.wy * 10))), (int)((int)f3));
        if (isoGridSquare != null && isoGridSquare.isSolidFloor()) {
            IsoFloorBloodSplat isoFloorBloodSplat = new IsoFloorBloodSplat((float)(f - (float)(this.wx * 10)), (float)(f2 - (float)(this.wy * 10)), (float)f3, (int)n, (float)((float)GameTime.getInstance().getWorldAgeHours()));
            if (n < 8) {
                isoFloorBloodSplat.index = ++this.nextSplatIndex;
                if (this.nextSplatIndex >= 10) {
                    this.nextSplatIndex = 0;
                }
            }
            if (this.FloorBloodSplats.isFull()) {
                IsoFloorBloodSplat isoFloorBloodSplat2 = (IsoFloorBloodSplat)this.FloorBloodSplats.removeFirst();
                isoFloorBloodSplat2.fade = PerformanceSettings.getLockFPS() * 5;
                this.FloorBloodSplatsFade.add((Object)isoFloorBloodSplat2);
            }
            this.FloorBloodSplats.add((Object)isoFloorBloodSplat);
        }
    }

    public void AddCorpses(int n, int n2) {
        if (IsoWorld.getZombiesDisabled() || "Tutorial".equals((Object)Core.GameMode)) {
            return;
        }
        IsoMetaChunk isoMetaChunk = IsoWorld.instance.getMetaChunk((int)n, (int)n2);
        if (isoMetaChunk != null) {
            float f = isoMetaChunk.getZombieIntensity();
            f *= 0.1f;
            int n3 = 0;
            if (f < 1.0f) {
                if ((float)Rand.Next((int)100) < f * 100.0f) {
                    n3 = 1;
                }
            } else {
                n3 = Rand.Next((int)0, (int)((int)f));
            }
            if (n3 > 0) {
                int n4;
                int n5;
                IsoGridSquare isoGridSquare = null;
                int n6 = 0;
                do {
                    n5 = Rand.Next((int)10);
                    n4 = Rand.Next((int)10);
                    isoGridSquare = this.getGridSquare((int)n5, (int)n4, (int)0);
                } while (++n6 < 100 && (isoGridSquare == null || !RandomizedBuildingBase.is2x2AreaClear((IsoGridSquare)isoGridSquare)));
                if (n6 == 100) {
                    return;
                }
                if (isoGridSquare != null) {
                    n5 = 14;
                    if (Rand.Next((int)10) == 0) {
                        n5 = 50;
                    }
                    if (Rand.Next((int)40) == 0) {
                        n5 = 100;
                    }
                    for (n4 = 0; n4 < n5; n4 += 1) {
                        float f2 = (float)Rand.Next((int)3000) / 1000.0f;
                        float f3 = (float)Rand.Next((int)3000) / 1000.0f;
                        this.addBloodSplat((float)((float)isoGridSquare.getX() + (f2 -= 1.5f)), (float)((float)isoGridSquare.getY() + (f3 -= 1.5f)), (float)((float)isoGridSquare.getZ()), (int)Rand.Next((int)20));
                    }
                    n4 = Rand.Next((int)(15 - SandboxOptions.instance.TimeSinceApo.getValue())) == 0 ? 1 : 0;
                    VirtualZombieManager.instance.choices.clear();
                    VirtualZombieManager.instance.choices.add((Object)isoGridSquare);
                    IsoZombie isoZombie = VirtualZombieManager.instance.createRealZombieAlways((int)Rand.Next((int)8), (boolean)false);
                    isoZombie.setX((float)((float)isoGridSquare.x));
                    isoZombie.setY((float)((float)isoGridSquare.y));
                    isoZombie.setFakeDead((boolean)false);
                    isoZombie.setHealth((float)0.0f);
                    isoZombie.upKillCount = false;
                    if (!n4) {
                        isoZombie.dressInRandomOutfit();
                        for (int i = 0; i < 10; ++i) {
                            isoZombie.addHole(null);
                            isoZombie.addBlood(null, (boolean)false, (boolean)true, (boolean)false);
                            isoZombie.addDirt(null, null, (boolean)false);
                        }
                        isoZombie.DoCorpseInventory();
                    }
                    isoZombie.setSkeleton(n4 != 0);
                    if (n4) {
                        isoZombie.getHumanVisual().setSkinTextureIndex((int)2);
                    }
                    IsoDeadBody isoDeadBody = new IsoDeadBody((IsoGameCharacter)isoZombie, (boolean)true);
                    if (!n4 && Rand.Next((int)3) == 0) {
                        VirtualZombieManager.instance.createEatingZombies((IsoDeadBody)isoDeadBody, (int)Rand.Next((int)1, (int)4));
                    } else if (!n4 && Rand.Next((int)10) == 0) {
                        isoDeadBody.setFakeDead((boolean)true);
                        if (Rand.Next((int)5) == 0) {
                            isoDeadBody.setCrawling((boolean)true);
                        }
                    }
                }
            }
        }
    }

    public void AddBlood(int n, int n2) {
        IsoMetaChunk isoMetaChunk = IsoWorld.instance.getMetaChunk((int)n, (int)n2);
        if (isoMetaChunk != null) {
            float f = isoMetaChunk.getZombieIntensity();
            f *= 0.1f;
            if (Rand.Next((int)40) == 0) {
                f += 10.0f;
            }
            int n3 = 0;
            if (f < 1.0f) {
                if ((float)Rand.Next((int)100) < f * 100.0f) {
                    n3 = 1;
                }
            } else {
                n3 = Rand.Next((int)0, (int)((int)f));
            }
            if (n3 > 0) {
                VirtualZombieManager.instance.AddBloodToMap((int)n3, (IsoChunk)this);
            }
        }
    }

    private void checkVehiclePos(BaseVehicle baseVehicle, IsoChunk isoChunk) {
        this.fixVehiclePos((BaseVehicle)baseVehicle, (IsoChunk)isoChunk);
        IsoDirections isoDirections = baseVehicle.getDir();
        switch (isoDirections) {
            case E: 
            case W: {
                IsoGridSquare isoGridSquare;
                if (baseVehicle.x - (float)(isoChunk.wx * 10) < baseVehicle.getScript().getExtents().x) {
                    isoGridSquare = IsoWorld.instance.CurrentCell.getGridSquare((double)((double)(baseVehicle.x - baseVehicle.getScript().getExtents().x)), (double)((double)baseVehicle.y), (double)((double)baseVehicle.z));
                    if (isoGridSquare == null) {
                        return;
                    }
                    this.fixVehiclePos((BaseVehicle)baseVehicle, (IsoChunk)isoGridSquare.chunk);
                }
                if (!(baseVehicle.x - (float)(isoChunk.wx * 10) > 10.0f - baseVehicle.getScript().getExtents().x)) break;
                isoGridSquare = IsoWorld.instance.CurrentCell.getGridSquare((double)((double)(baseVehicle.x + baseVehicle.getScript().getExtents().x)), (double)((double)baseVehicle.y), (double)((double)baseVehicle.z));
                if (isoGridSquare == null) {
                    return;
                }
                this.fixVehiclePos((BaseVehicle)baseVehicle, (IsoChunk)isoGridSquare.chunk);
                break;
            }
            case N: 
            case S: {
                IsoGridSquare isoGridSquare;
                if (baseVehicle.y - (float)(isoChunk.wy * 10) < baseVehicle.getScript().getExtents().z) {
                    isoGridSquare = IsoWorld.instance.CurrentCell.getGridSquare((double)((double)baseVehicle.x), (double)((double)(baseVehicle.y - baseVehicle.getScript().getExtents().z)), (double)((double)baseVehicle.z));
                    if (isoGridSquare == null) {
                        return;
                    }
                    this.fixVehiclePos((BaseVehicle)baseVehicle, (IsoChunk)isoGridSquare.chunk);
                }
                if (!(baseVehicle.y - (float)(isoChunk.wy * 10) > 10.0f - baseVehicle.getScript().getExtents().z)) break;
                isoGridSquare = IsoWorld.instance.CurrentCell.getGridSquare((double)((double)baseVehicle.x), (double)((double)(baseVehicle.y + baseVehicle.getScript().getExtents().z)), (double)((double)baseVehicle.z));
                if (isoGridSquare == null) {
                    return;
                }
                this.fixVehiclePos((BaseVehicle)baseVehicle, (IsoChunk)isoGridSquare.chunk);
            }
        }
    }

    private boolean fixVehiclePos(BaseVehicle baseVehicle, IsoChunk isoChunk) {
        BaseVehicle.MinMaxPosition minMaxPosition = baseVehicle.getMinMaxPosition();
        boolean bl = false;
        IsoDirections isoDirections = baseVehicle.getDir();
        block4: for (int i = 0; i < isoChunk.vehicles.size(); ++i) {
            BaseVehicle.MinMaxPosition minMaxPosition2 = ((BaseVehicle)isoChunk.vehicles.get((int)i)).getMinMaxPosition();
            switch (isoDirections) {
                case E: 
                case W: {
                    float f = minMaxPosition2.minX - minMaxPosition.maxX;
                    if (f > 0.0f && minMaxPosition.minY < minMaxPosition2.maxY && minMaxPosition.maxY > minMaxPosition2.minY) {
                        baseVehicle.x -= f;
                        minMaxPosition.minX -= f;
                        minMaxPosition.maxX -= f;
                        bl = true;
                        continue block4;
                    }
                    f = minMaxPosition.minX - minMaxPosition2.maxX;
                    if (!(f > 0.0f) || !(minMaxPosition.minY < minMaxPosition2.maxY) || !(minMaxPosition.maxY > minMaxPosition2.minY)) continue block4;
                    baseVehicle.x += f;
                    minMaxPosition.minX += f;
                    minMaxPosition.maxX += f;
                    bl = true;
                    continue block4;
                }
                case N: 
                case S: {
                    float f = minMaxPosition2.minY - minMaxPosition.maxY;
                    if (f > 0.0f && minMaxPosition.minX < minMaxPosition2.maxX && minMaxPosition.maxX > minMaxPosition2.minX) {
                        baseVehicle.y -= f;
                        minMaxPosition.minY -= f;
                        minMaxPosition.maxY -= f;
                        bl = true;
                        continue block4;
                    }
                    f = minMaxPosition.minY - minMaxPosition2.maxY;
                    if (!(f > 0.0f) || !(minMaxPosition.minX < minMaxPosition2.maxX) || !(minMaxPosition.maxX > minMaxPosition2.minX)) continue block4;
                    baseVehicle.y += f;
                    minMaxPosition.minY += f;
                    minMaxPosition.maxY += f;
                    bl = true;
                }
            }
        }
        return bl;
    }

    private boolean isGoodVehiclePos(BaseVehicle baseVehicle, IsoChunk isoChunk) {
        int n = ((int)baseVehicle.x - 4) / 10 - 1;
        int n2 = ((int)baseVehicle.y - 4) / 10 - 1;
        int n3 = (int)Math.ceil((double)((double)((baseVehicle.x + 4.0f) / 10.0f))) + 1;
        int n4 = (int)Math.ceil((double)((double)((baseVehicle.y + 4.0f) / 10.0f))) + 1;
        for (int i = n2; i < n4; ++i) {
            for (int j = n; j < n3; ++j) {
                IsoChunk isoChunk2;
                IsoChunk isoChunk3 = isoChunk2 = GameServer.bServer ? ServerMap.instance.getChunk((int)j, (int)i) : IsoWorld.instance.CurrentCell.getChunkForGridSquare((int)(j * 10), (int)(i * 10), (int)0);
                if (isoChunk2 == null) continue;
                for (int k = 0; k < isoChunk2.vehicles.size(); ++k) {
                    BaseVehicle baseVehicle2 = (BaseVehicle)isoChunk2.vehicles.get((int)k);
                    if ((int)baseVehicle2.z != (int)baseVehicle.z || !baseVehicle.testCollisionWithVehicle((BaseVehicle)baseVehicle2)) continue;
                    return false;
                }
            }
        }
        return true;
    }

    private void AddVehicles_ForTest(IsoMetaGrid.Zone zone) {
        for (int i = zone.y - this.wy * 10 + 3; i < 0; i += 6) {
        }
        for (int i = zone.x - this.wx * 10 + 2; i < 0; i += 5) {
        }
        for (int i = i; i < 10 && this.wy * 10 + i < zone.y + zone.h; i += 6) {
            for (int j = i; j < 10 && this.wx * 10 + j < zone.x + zone.w; j += 5) {
                IsoGridSquare isoGridSquare = this.getGridSquare((int)j, (int)i, (int)0);
                if (isoGridSquare == null) continue;
                BaseVehicle baseVehicle = new BaseVehicle((IsoCell)IsoWorld.instance.CurrentCell);
                baseVehicle.setZone((String)"Test");
                switch (AddVehicles_ForTest_vtype) {
                    case 0: {
                        baseVehicle.setScriptName((String)"Base.CarNormal");
                        break;
                    }
                    case 1: {
                        baseVehicle.setScriptName((String)"Base.SmallCar");
                        break;
                    }
                    case 2: {
                        baseVehicle.setScriptName((String)"Base.SmallCar02");
                        break;
                    }
                    case 3: {
                        baseVehicle.setScriptName((String)"Base.CarTaxi");
                        break;
                    }
                    case 4: {
                        baseVehicle.setScriptName((String)"Base.CarTaxi2");
                        break;
                    }
                    case 5: {
                        baseVehicle.setScriptName((String)"Base.PickUpTruck");
                        break;
                    }
                    case 6: {
                        baseVehicle.setScriptName((String)"Base.PickUpVan");
                        break;
                    }
                    case 7: {
                        baseVehicle.setScriptName((String)"Base.CarStationWagon");
                        break;
                    }
                    case 8: {
                        baseVehicle.setScriptName((String)"Base.CarStationWagon2");
                        break;
                    }
                    case 9: {
                        baseVehicle.setScriptName((String)"Base.VanSeats");
                        break;
                    }
                    case 10: {
                        baseVehicle.setScriptName((String)"Base.Van");
                        break;
                    }
                    case 11: {
                        baseVehicle.setScriptName((String)"Base.StepVan");
                        break;
                    }
                    case 12: {
                        baseVehicle.setScriptName((String)"Base.PickUpTruck");
                        break;
                    }
                    case 13: {
                        baseVehicle.setScriptName((String)"Base.PickUpVan");
                        break;
                    }
                    case 14: {
                        baseVehicle.setScriptName((String)"Base.CarStationWagon");
                        break;
                    }
                    case 15: {
                        baseVehicle.setScriptName((String)"Base.CarStationWagon2");
                        break;
                    }
                    case 16: {
                        baseVehicle.setScriptName((String)"Base.VanSeats");
                        break;
                    }
                    case 17: {
                        baseVehicle.setScriptName((String)"Base.Van");
                        break;
                    }
                    case 18: {
                        baseVehicle.setScriptName((String)"Base.StepVan");
                        break;
                    }
                    case 19: {
                        baseVehicle.setScriptName((String)"Base.SUV");
                        break;
                    }
                    case 20: {
                        baseVehicle.setScriptName((String)"Base.OffRoad");
                        break;
                    }
                    case 21: {
                        baseVehicle.setScriptName((String)"Base.ModernCar");
                        break;
                    }
                    case 22: {
                        baseVehicle.setScriptName((String)"Base.ModernCar02");
                        break;
                    }
                    case 23: {
                        baseVehicle.setScriptName((String)"Base.CarLuxury");
                        break;
                    }
                    case 24: {
                        baseVehicle.setScriptName((String)"Base.SportsCar");
                        break;
                    }
                    case 25: {
                        baseVehicle.setScriptName((String)"Base.PickUpVanLightsPolice");
                        break;
                    }
                    case 26: {
                        baseVehicle.setScriptName((String)"Base.CarLightsPolice");
                        break;
                    }
                    case 27: {
                        baseVehicle.setScriptName((String)"Base.PickUpVanLightsFire");
                        break;
                    }
                    case 28: {
                        baseVehicle.setScriptName((String)"Base.PickUpTruckLightsFire");
                        break;
                    }
                    case 29: {
                        baseVehicle.setScriptName((String)"Base.PickUpVanLights");
                        break;
                    }
                    case 30: {
                        baseVehicle.setScriptName((String)"Base.PickUpTruckLights");
                        break;
                    }
                    case 31: {
                        baseVehicle.setScriptName((String)"Base.CarLights");
                        break;
                    }
                    case 32: {
                        baseVehicle.setScriptName((String)"Base.StepVanMail");
                        break;
                    }
                    case 33: {
                        baseVehicle.setScriptName((String)"Base.VanSpiffo");
                        break;
                    }
                    case 34: {
                        baseVehicle.setScriptName((String)"Base.VanAmbulance");
                        break;
                    }
                    case 35: {
                        baseVehicle.setScriptName((String)"Base.VanRadio");
                        break;
                    }
                    case 36: {
                        baseVehicle.setScriptName((String)"Base.PickupBurnt");
                        break;
                    }
                    case 37: {
                        baseVehicle.setScriptName((String)"Base.CarNormalBurnt");
                        break;
                    }
                    case 38: {
                        baseVehicle.setScriptName((String)"Base.TaxiBurnt");
                        break;
                    }
                    case 39: {
                        baseVehicle.setScriptName((String)"Base.ModernCarBurnt");
                        break;
                    }
                    case 40: {
                        baseVehicle.setScriptName((String)"Base.ModernCar02Burnt");
                        break;
                    }
                    case 41: {
                        baseVehicle.setScriptName((String)"Base.SportsCarBurnt");
                        break;
                    }
                    case 42: {
                        baseVehicle.setScriptName((String)"Base.SmallCarBurnt");
                        break;
                    }
                    case 43: {
                        baseVehicle.setScriptName((String)"Base.SmallCar02Burnt");
                        break;
                    }
                    case 44: {
                        baseVehicle.setScriptName((String)"Base.VanSeatsBurnt");
                        break;
                    }
                    case 45: {
                        baseVehicle.setScriptName((String)"Base.VanBurnt");
                        break;
                    }
                    case 46: {
                        baseVehicle.setScriptName((String)"Base.SUVBurnt");
                        break;
                    }
                    case 47: {
                        baseVehicle.setScriptName((String)"Base.OffRoadBurnt");
                        break;
                    }
                    case 48: {
                        baseVehicle.setScriptName((String)"Base.PickUpVanLightsBurnt");
                        break;
                    }
                    case 49: {
                        baseVehicle.setScriptName((String)"Base.AmbulanceBurnt");
                        break;
                    }
                    case 50: {
                        baseVehicle.setScriptName((String)"Base.VanRadioBurnt");
                        break;
                    }
                    case 51: {
                        baseVehicle.setScriptName((String)"Base.PickupSpecialBurnt");
                        break;
                    }
                    case 52: {
                        baseVehicle.setScriptName((String)"Base.NormalCarBurntPolice");
                        break;
                    }
                    case 53: {
                        baseVehicle.setScriptName((String)"Base.LuxuryCarBurnt");
                        break;
                    }
                    case 54: {
                        baseVehicle.setScriptName((String)"Base.PickUpVanBurnt");
                        break;
                    }
                    case 55: {
                        baseVehicle.setScriptName((String)"Base.PickUpTruckMccoy");
                    }
                }
                baseVehicle.setDir((IsoDirections)IsoDirections.W);
                double d = (double)(baseVehicle.getDir().toAngle() + (float)Math.PI) % (Math.PI * 2);
                baseVehicle.savedRot.setAngleAxis((double)d, (double)0.0, (double)1.0, (double)0.0);
                if (AddVehicles_ForTest_vrot == 1) {
                    baseVehicle.savedRot.setAngleAxis((double)1.5707963267948966, (double)0.0, (double)0.0, (double)1.0);
                }
                if (AddVehicles_ForTest_vrot == 2) {
                    baseVehicle.savedRot.setAngleAxis((double)Math.PI, (double)0.0, (double)0.0, (double)1.0);
                }
                baseVehicle.jniTransform.setRotation((Quaternionf)baseVehicle.savedRot);
                baseVehicle.setX((float)((float)isoGridSquare.x));
                baseVehicle.setY((float)((float)isoGridSquare.y + 3.0f - 3.0f));
                baseVehicle.setZ((float)((float)isoGridSquare.z));
                baseVehicle.jniTransform.origin.set((float)(baseVehicle.getX() - WorldSimulation.instance.offsetX), (float)baseVehicle.getZ(), (float)(baseVehicle.getY() - WorldSimulation.instance.offsetY));
                baseVehicle.setScript();
                this.checkVehiclePos((BaseVehicle)baseVehicle, (IsoChunk)this);
                this.vehicles.add((Object)baseVehicle);
                baseVehicle.setSkinIndex((int)AddVehicles_ForTest_vskin);
                if (++AddVehicles_ForTest_vrot < 2) continue;
                AddVehicles_ForTest_vrot = 0;
                if (++AddVehicles_ForTest_vskin < baseVehicle.getSkinCount()) continue;
                AddVehicles_ForTest_vtype = (AddVehicles_ForTest_vtype + 1) % 56;
                AddVehicles_ForTest_vskin = 0;
            }
        }
    }

    private void AddVehicles_OnZone(IsoMetaGrid.VehicleZone vehicleZone, String string) {
        IsoDirections isoDirections = IsoDirections.N;
        int n = 3;
        int n2 = 4;
        if (!(vehicleZone.w != n2 && vehicleZone.w != n2 + 1 && vehicleZone.w != n2 + 2 || vehicleZone.h > n && vehicleZone.h < n2 + 2)) {
            isoDirections = IsoDirections.W;
        }
        n2 = 5;
        if (vehicleZone.dir != IsoDirections.Max) {
            isoDirections = vehicleZone.dir;
        }
        if (isoDirections != IsoDirections.N && isoDirections != IsoDirections.S) {
            n2 = 3;
            n = 5;
        }
        int n3 = 10;
        for (float f = (float)(vehicleZone.y - this.wy * 10) + (float)n2 / 2.0f; f < 0.0f; f += (float)n2) {
        }
        for (float f = (float)(vehicleZone.x - this.wx * 10) + (float)n / 2.0f; f < 0.0f; f += (float)n) {
        }
        block16: for (float f = f; f < 10.0f && (float)(this.wy * 10) + f < (float)(vehicleZone.y + vehicleZone.h); f += (float)n2) {
            for (float f2 = f; f2 < 10.0f && (float)(this.wx * 10) + f2 < (float)(vehicleZone.x + vehicleZone.w); f2 += (float)n) {
                boolean bl;
                IsoGridSquare isoGridSquare = this.getGridSquare((int)((int)f2), (int)((int)f), (int)0);
                if (isoGridSquare == null) continue;
                VehicleType vehicleType = VehicleType.getRandomVehicleType((String)string);
                if (vehicleType == null) {
                    System.out.println((String)("Can't find car: " + string));
                    continue block16;
                }
                int n4 = vehicleType.spawnRate;
                switch (SandboxOptions.instance.CarSpawnRate.getValue()) {
                    case 1: {
                        break;
                    }
                    case 2: {
                        n4 = (int)Math.ceil((double)((double)((float)n4 / 10.0f)));
                        break;
                    }
                    case 3: {
                        n4 = (int)Math.ceil((double)((double)((float)n4 / 1.5f)));
                    }
                    case 4: {
                        break;
                    }
                    case 5: {
                        n4 *= 2;
                    }
                }
                if (SystemDisabler.doVehiclesEverywhere || DebugOptions.instance.VehicleSpawnEverywhere.getValue()) {
                    n4 = 100;
                }
                if (Rand.Next((int)100) > n4) continue;
                BaseVehicle baseVehicle = new BaseVehicle((IsoCell)IsoWorld.instance.CurrentCell);
                baseVehicle.setZone((String)string);
                baseVehicle.setVehicleType((String)vehicleType.name);
                if (vehicleType.isSpecialCar) {
                    baseVehicle.setDoColor((boolean)false);
                }
                if (!this.RandomizeModel((BaseVehicle)baseVehicle, (IsoMetaGrid.Zone)vehicleZone, (String)string, (VehicleType)vehicleType)) {
                    System.out.println((String)("Problem with Vehicle spawning: " + string + " " + vehicleType));
                    return;
                }
                int n5 = 15;
                switch (SandboxOptions.instance.CarAlarm.getValue()) {
                    case 1: {
                        n5 = -1;
                        break;
                    }
                    case 2: {
                        n5 = 3;
                        break;
                    }
                    case 3: {
                        n5 = 8;
                        break;
                    }
                    case 5: {
                        n5 = 25;
                        break;
                    }
                    case 6: {
                        n5 = 50;
                    }
                }
                boolean bl2 = bl = baseVehicle.getScriptName().toLowerCase().contains((CharSequence)"burnt") || baseVehicle.getScriptName().toLowerCase().contains((CharSequence)"smashed");
                if (Rand.Next((int)100) < n5 && !bl) {
                    baseVehicle.setAlarmed((boolean)true);
                }
                if (vehicleZone.isFaceDirection()) {
                    baseVehicle.setDir((IsoDirections)isoDirections);
                } else if (isoDirections == IsoDirections.N || isoDirections == IsoDirections.S) {
                    baseVehicle.setDir((IsoDirections)(Rand.Next((int)2) == 0 ? IsoDirections.N : IsoDirections.S));
                } else {
                    baseVehicle.setDir((IsoDirections)(Rand.Next((int)2) == 0 ? IsoDirections.W : IsoDirections.E));
                }
                float f3 = baseVehicle.getDir().toAngle() + (float)Math.PI;
                while ((double)f3 > Math.PI * 2) {
                    f3 = (float)((double)f3 - Math.PI * 2);
                }
                if (vehicleType.randomAngle) {
                    f3 = Rand.Next((float)0.0f, (float)((float)Math.PI * 2));
                }
                baseVehicle.savedRot.setAngleAxis((float)f3, (float)0.0f, (float)1.0f, (float)0.0f);
                baseVehicle.jniTransform.setRotation((Quaternionf)baseVehicle.savedRot);
                float f4 = baseVehicle.getScript().getExtents().z;
                float f5 = 0.5f;
                float f6 = (float)isoGridSquare.x + 0.5f;
                float f7 = (float)isoGridSquare.y + 0.5f;
                if (isoDirections == IsoDirections.N) {
                    f6 = (float)isoGridSquare.x + (float)n / 2.0f - (float)((int)((float)n / 2.0f));
                    f7 = (float)vehicleZone.y + f4 / 2.0f + f5;
                    if (f7 >= (float)(isoGridSquare.y + 1) && (int)f < n3 - 1 && this.getGridSquare((int)((int)f2), (int)((int)f + 1), (int)0) != null) {
                        isoGridSquare = this.getGridSquare((int)((int)f2), (int)((int)f + 1), (int)0);
                    }
                } else if (isoDirections == IsoDirections.S) {
                    f6 = (float)isoGridSquare.x + (float)n / 2.0f - (float)((int)((float)n / 2.0f));
                    f7 = (float)(vehicleZone.y + vehicleZone.h) - f4 / 2.0f - f5;
                    if (f7 < (float)isoGridSquare.y && (int)f > 0 && this.getGridSquare((int)((int)f2), (int)((int)f - 1), (int)0) != null) {
                        isoGridSquare = this.getGridSquare((int)((int)f2), (int)((int)f - 1), (int)0);
                    }
                } else if (isoDirections == IsoDirections.W) {
                    f6 = (float)vehicleZone.x + f4 / 2.0f + f5;
                    f7 = (float)isoGridSquare.y + (float)n2 / 2.0f - (float)((int)((float)n2 / 2.0f));
                    if (f6 >= (float)(isoGridSquare.x + 1) && (int)f2 < n3 - 1 && this.getGridSquare((int)((int)f2 + 1), (int)((int)f), (int)0) != null) {
                        isoGridSquare = this.getGridSquare((int)((int)f2 + 1), (int)((int)f), (int)0);
                    }
                } else if (isoDirections == IsoDirections.E) {
                    f6 = (float)(vehicleZone.x + vehicleZone.w) - f4 / 2.0f - f5;
                    f7 = (float)isoGridSquare.y + (float)n2 / 2.0f - (float)((int)((float)n2 / 2.0f));
                    if (f6 < (float)isoGridSquare.x && (int)f2 > 0 && this.getGridSquare((int)((int)f2 - 1), (int)((int)f), (int)0) != null) {
                        isoGridSquare = this.getGridSquare((int)((int)f2 - 1), (int)((int)f), (int)0);
                    }
                }
                if (f6 < (float)isoGridSquare.x + 0.005f) {
                    f6 = (float)isoGridSquare.x + 0.005f;
                }
                if (f6 > (float)(isoGridSquare.x + 1) - 0.005f) {
                    f6 = (float)(isoGridSquare.x + 1) - 0.005f;
                }
                if (f7 < (float)isoGridSquare.y + 0.005f) {
                    f7 = (float)isoGridSquare.y + 0.005f;
                }
                if (f7 > (float)(isoGridSquare.y + 1) - 0.005f) {
                    f7 = (float)(isoGridSquare.y + 1) - 0.005f;
                }
                baseVehicle.setX((float)f6);
                baseVehicle.setY((float)f7);
                baseVehicle.setZ((float)((float)isoGridSquare.z));
                baseVehicle.jniTransform.origin.set((float)(baseVehicle.getX() - WorldSimulation.instance.offsetX), (float)baseVehicle.getZ(), (float)(baseVehicle.getY() - WorldSimulation.instance.offsetY));
                float f8 = 100.0f - Math.min((float)(vehicleType.baseVehicleQuality * 120.0f), (float)100.0f);
                float f9 = baseVehicle.rust = (float)Rand.Next((int)100) < f8 ? 1.0f : 0.0f;
                if (IsoChunk.doSpawnedVehiclesInInvalidPosition((BaseVehicle)baseVehicle) || GameClient.bClient) {
                    this.vehicles.add((Object)baseVehicle);
                }
                if (vehicleType.chanceOfOverCar <= 0 || Rand.Next((int)100) > vehicleType.chanceOfOverCar) continue;
                this.spawnVehicleRandomAngle((IsoGridSquare)isoGridSquare, (IsoMetaGrid.Zone)vehicleZone, (String)string);
            }
        }
    }

    private void AddVehicles_OnZonePolyline(IsoMetaGrid.VehicleZone vehicleZone, String string) {
        int n = 5;
        Vector2 vector2 = new Vector2();
        for (int i = 0; i < vehicleZone.points.size() - 2; i += 2) {
            int n2 = vehicleZone.points.getQuick((int)i);
            int n3 = vehicleZone.points.getQuick((int)(i + 1));
            int n4 = vehicleZone.points.getQuick((int)((i + 2) % vehicleZone.points.size()));
            int n5 = vehicleZone.points.getQuick((int)((i + 3) % vehicleZone.points.size()));
            vector2.set((float)((float)(n4 - n2)), (float)((float)(n5 - n3)));
            for (float f = (float)n / 2.0f; f < vector2.getLength(); f += (float)n) {
                float f2 = (float)n2 + vector2.x / vector2.getLength() * f;
                float f3 = (float)n3 + vector2.y / vector2.getLength() * f;
                if (!(f2 >= (float)(this.wx * 10)) || !(f3 >= (float)(this.wy * 10)) || !(f2 < (float)((this.wx + 1) * 10)) || !(f3 < (float)((this.wy + 1) * 10))) continue;
                VehicleType vehicleType = VehicleType.getRandomVehicleType((String)string);
                if (vehicleType == null) {
                    System.out.println((String)("Can't find car: " + string));
                    return;
                }
                BaseVehicle baseVehicle = new BaseVehicle((IsoCell)IsoWorld.instance.CurrentCell);
                baseVehicle.setZone((String)string);
                baseVehicle.setVehicleType((String)vehicleType.name);
                if (vehicleType.isSpecialCar) {
                    baseVehicle.setDoColor((boolean)false);
                }
                if (!this.RandomizeModel((BaseVehicle)baseVehicle, (IsoMetaGrid.Zone)vehicleZone, (String)string, (VehicleType)vehicleType)) {
                    System.out.println((String)("Problem with Vehicle spawning: " + string + " " + vehicleType));
                    return;
                }
                int n6 = 15;
                switch (SandboxOptions.instance.CarAlarm.getValue()) {
                    case 1: {
                        n6 = -1;
                        break;
                    }
                    case 2: {
                        n6 = 3;
                        break;
                    }
                    case 3: {
                        n6 = 8;
                        break;
                    }
                    case 5: {
                        n6 = 25;
                        break;
                    }
                    case 6: {
                        n6 = 50;
                    }
                }
                if (Rand.Next((int)100) < n6) {
                    baseVehicle.setAlarmed((boolean)true);
                }
                float f4 = vector2.x;
                float f5 = vector2.y;
                vector2.normalize();
                baseVehicle.setDir((IsoDirections)IsoDirections.fromAngle((Vector2)vector2));
                float f6 = vector2.getDirectionNeg() + 0.0f;
                while ((double)f6 > Math.PI * 2) {
                    f6 = (float)((double)f6 - Math.PI * 2);
                }
                vector2.x = f4;
                vector2.y = f5;
                if (vehicleType.randomAngle) {
                    f6 = Rand.Next((float)0.0f, (float)((float)Math.PI * 2));
                }
                baseVehicle.savedRot.setAngleAxis((float)f6, (float)0.0f, (float)1.0f, (float)0.0f);
                baseVehicle.jniTransform.setRotation((Quaternionf)baseVehicle.savedRot);
                IsoGridSquare isoGridSquare = this.getGridSquare((int)((int)f2 - this.wx * 10), (int)((int)f3 - this.wy * 10), (int)0);
                if (f2 < (float)isoGridSquare.x + 0.005f) {
                    f2 = (float)isoGridSquare.x + 0.005f;
                }
                if (f2 > (float)(isoGridSquare.x + 1) - 0.005f) {
                    f2 = (float)(isoGridSquare.x + 1) - 0.005f;
                }
                if (f3 < (float)isoGridSquare.y + 0.005f) {
                    f3 = (float)isoGridSquare.y + 0.005f;
                }
                if (f3 > (float)(isoGridSquare.y + 1) - 0.005f) {
                    f3 = (float)(isoGridSquare.y + 1) - 0.005f;
                }
                baseVehicle.setX((float)f2);
                baseVehicle.setY((float)f3);
                baseVehicle.setZ((float)((float)isoGridSquare.z));
                baseVehicle.jniTransform.origin.set((float)(baseVehicle.getX() - WorldSimulation.instance.offsetX), (float)baseVehicle.getZ(), (float)(baseVehicle.getY() - WorldSimulation.instance.offsetY));
                float f7 = 100.0f - Math.min((float)(vehicleType.baseVehicleQuality * 120.0f), (float)100.0f);
                float f8 = baseVehicle.rust = (float)Rand.Next((int)100) < f7 ? 1.0f : 0.0f;
                if (!IsoChunk.doSpawnedVehiclesInInvalidPosition((BaseVehicle)baseVehicle) && !GameClient.bClient) continue;
                this.vehicles.add((Object)baseVehicle);
            }
        }
    }

    public static void removeFromCheckedVehicles(BaseVehicle baseVehicle) {
        BaseVehicleCheckedVehicles.remove((Object)baseVehicle);
    }

    public static void addFromCheckedVehicles(BaseVehicle baseVehicle) {
        if (!BaseVehicleCheckedVehicles.contains((Object)baseVehicle)) {
            BaseVehicleCheckedVehicles.add((Object)baseVehicle);
        }
    }

    public static void Reset() {
        BaseVehicleCheckedVehicles.clear();
    }

    public static boolean doSpawnedVehiclesInInvalidPosition(BaseVehicle baseVehicle) {
        IsoGridSquare isoGridSquare;
        if (GameServer.bServer ? (isoGridSquare = ServerMap.instance.getGridSquare((int)((int)baseVehicle.getX()), (int)((int)baseVehicle.getY()), (int)0)) != null && isoGridSquare.roomID != -1 : !GameClient.bClient && (isoGridSquare = IsoWorld.instance.CurrentCell.getGridSquare((int)((int)baseVehicle.getX()), (int)((int)baseVehicle.getY()), (int)0)) != null && isoGridSquare.roomID != -1) {
            return false;
        }
        boolean bl = true;
        for (int i = 0; i < BaseVehicleCheckedVehicles.size(); ++i) {
            if (!((BaseVehicle)BaseVehicleCheckedVehicles.get((int)i)).testCollisionWithVehicle((BaseVehicle)baseVehicle)) continue;
            bl = false;
        }
        if (bl) {
            IsoChunk.addFromCheckedVehicles((BaseVehicle)baseVehicle);
        }
        return bl;
    }

    private void spawnVehicleRandomAngle(IsoGridSquare isoGridSquare, IsoMetaGrid.Zone zone, String string) {
        VehicleType vehicleType;
        boolean bl = true;
        int n = 3;
        int n2 = 4;
        if (!(zone.w != n2 && zone.w != n2 + 1 && zone.w != n2 + 2 || zone.h > n && zone.h < n2 + 2)) {
            bl = false;
        }
        n2 = 5;
        if (!bl) {
            n2 = 3;
            n = 5;
        }
        if ((vehicleType = VehicleType.getRandomVehicleType((String)string)) == null) {
            System.out.println((String)("Can't find car: " + string));
            return;
        }
        BaseVehicle baseVehicle = new BaseVehicle((IsoCell)IsoWorld.instance.CurrentCell);
        baseVehicle.setZone((String)string);
        if (!this.RandomizeModel((BaseVehicle)baseVehicle, (IsoMetaGrid.Zone)zone, (String)string, (VehicleType)vehicleType)) {
            return;
        }
        if (bl) {
            baseVehicle.setDir((IsoDirections)(Rand.Next((int)2) == 0 ? IsoDirections.N : IsoDirections.S));
        } else {
            baseVehicle.setDir((IsoDirections)(Rand.Next((int)2) == 0 ? IsoDirections.W : IsoDirections.E));
        }
        float f = Rand.Next((float)0.0f, (float)((float)Math.PI * 2));
        baseVehicle.savedRot.setAngleAxis((float)f, (float)0.0f, (float)1.0f, (float)0.0f);
        baseVehicle.jniTransform.setRotation((Quaternionf)baseVehicle.savedRot);
        if (bl) {
            baseVehicle.setX((float)((float)isoGridSquare.x + (float)n / 2.0f - (float)((int)((float)n / 2.0f))));
            baseVehicle.setY((float)((float)isoGridSquare.y));
        } else {
            baseVehicle.setX((float)((float)isoGridSquare.x));
            baseVehicle.setY((float)((float)isoGridSquare.y + (float)n2 / 2.0f - (float)((int)((float)n2 / 2.0f))));
        }
        baseVehicle.setZ((float)((float)isoGridSquare.z));
        baseVehicle.jniTransform.origin.set((float)(baseVehicle.getX() - WorldSimulation.instance.offsetX), (float)baseVehicle.getZ(), (float)(baseVehicle.getY() - WorldSimulation.instance.offsetY));
        if (IsoChunk.doSpawnedVehiclesInInvalidPosition((BaseVehicle)baseVehicle) || GameClient.bClient) {
            this.vehicles.add((Object)baseVehicle);
        }
    }

    public boolean RandomizeModel(BaseVehicle baseVehicle, IsoMetaGrid.Zone zone, String string, VehicleType vehicleType) {
        String string2;
        VehicleScript vehicleScript;
        if (vehicleType.vehiclesDefinition.isEmpty()) {
            System.out.println((String)("no vehicle definition found for " + string));
            return false;
        }
        float f = Rand.Next((float)0.0f, (float)100.0f);
        float f2 = 0.0f;
        VehicleType.VehicleTypeDefinition vehicleTypeDefinition = null;
        for (int i = 0; i < vehicleType.vehiclesDefinition.size(); ++i) {
            vehicleTypeDefinition = (VehicleType.VehicleTypeDefinition)vehicleType.vehiclesDefinition.get((int)i);
            if (f < (f2 += vehicleTypeDefinition.spawnChance)) break;
        }
        if ((vehicleScript = ScriptManager.instance.getVehicle((String)(string2 = vehicleTypeDefinition.vehicleType))) == null) {
            DebugLog.log((String)("no such vehicle script \"" + string2 + "\" in IsoChunk.RandomizeModel"));
            return false;
        }
        int n = vehicleTypeDefinition.index;
        baseVehicle.setScriptName((String)string2);
        baseVehicle.setScript();
        try {
            if (n > -1) {
                baseVehicle.setSkinIndex((int)n);
            } else {
                baseVehicle.setSkinIndex((int)Rand.Next((int)baseVehicle.getSkinCount()));
            }
        }
        catch (Exception exception) {
            DebugLog.log((String)("problem with " + baseVehicle.getScriptName()));
            exception.printStackTrace();
            return false;
        }
        return true;
    }

    private void AddVehicles_TrafficJam_W(IsoMetaGrid.Zone zone, String string) {
        for (int i = zone.y - this.wy * 10 + 1; i < 0; i += 3) {
        }
        for (int i = zone.x - this.wx * 10 + 3; i < 0; i += 6) {
        }
        block2: for (int i = i; i < 10 && this.wy * 10 + i < zone.y + zone.h; i += 3 + Rand.Next((int)1)) {
            for (int j = i; j < 10 && this.wx * 10 + j < zone.x + zone.w; j += 6 + Rand.Next((int)1)) {
                IsoGridSquare isoGridSquare = this.getGridSquare((int)j, (int)i, (int)0);
                if (isoGridSquare == null) continue;
                VehicleType vehicleType = VehicleType.getRandomVehicleType((String)string);
                if (vehicleType == null) {
                    System.out.println((String)("Can't find car: " + string));
                    continue block2;
                }
                int n = 80;
                if (SystemDisabler.doVehiclesEverywhere || DebugOptions.instance.VehicleSpawnEverywhere.getValue()) {
                    n = 100;
                }
                if (Rand.Next((int)100) > n) continue;
                BaseVehicle baseVehicle = new BaseVehicle((IsoCell)IsoWorld.instance.CurrentCell);
                baseVehicle.setZone((String)"TrafficJam");
                baseVehicle.setVehicleType((String)vehicleType.name);
                if (!this.RandomizeModel((BaseVehicle)baseVehicle, (IsoMetaGrid.Zone)zone, (String)string, (VehicleType)vehicleType)) {
                    return;
                }
                baseVehicle.setScript();
                baseVehicle.setX((float)((float)isoGridSquare.x + Rand.Next((float)0.0f, (float)1.0f)));
                baseVehicle.setY((float)((float)isoGridSquare.y + Rand.Next((float)0.0f, (float)1.0f)));
                baseVehicle.setZ((float)((float)isoGridSquare.z));
                baseVehicle.jniTransform.origin.set((float)(baseVehicle.getX() - WorldSimulation.instance.offsetX), (float)baseVehicle.getZ(), (float)(baseVehicle.getY() - WorldSimulation.instance.offsetY));
                if (!this.isGoodVehiclePos((BaseVehicle)baseVehicle, (IsoChunk)this)) continue;
                baseVehicle.setSkinIndex((int)Rand.Next((int)(baseVehicle.getSkinCount() - 1)));
                baseVehicle.setDir((IsoDirections)IsoDirections.W);
                float f = (float)Math.abs((int)(zone.x + zone.w - isoGridSquare.x));
                f /= 20.0f;
                f = Math.min((float)2.0f, (float)f);
                float f2 = baseVehicle.getDir().toAngle() + (float)Math.PI - 0.25f + Rand.Next((float)0.0f, (float)f);
                while ((double)f2 > Math.PI * 2) {
                    f2 = (float)((double)f2 - Math.PI * 2);
                }
                baseVehicle.savedRot.setAngleAxis((float)f2, (float)0.0f, (float)1.0f, (float)0.0f);
                baseVehicle.jniTransform.setRotation((Quaternionf)baseVehicle.savedRot);
                if (!IsoChunk.doSpawnedVehiclesInInvalidPosition((BaseVehicle)baseVehicle) && !GameClient.bClient) continue;
                this.vehicles.add((Object)baseVehicle);
            }
        }
    }

    private void AddVehicles_TrafficJam_E(IsoMetaGrid.Zone zone, String string) {
        for (int i = zone.y - this.wy * 10 + 1; i < 0; i += 3) {
        }
        for (int i = zone.x - this.wx * 10 + 3; i < 0; i += 6) {
        }
        block2: for (int i = i; i < 10 && this.wy * 10 + i < zone.y + zone.h; i += 3 + Rand.Next((int)1)) {
            for (int j = i; j < 10 && this.wx * 10 + j < zone.x + zone.w; j += 6 + Rand.Next((int)1)) {
                IsoGridSquare isoGridSquare = this.getGridSquare((int)j, (int)i, (int)0);
                if (isoGridSquare == null) continue;
                VehicleType vehicleType = VehicleType.getRandomVehicleType((String)string);
                if (vehicleType == null) {
                    System.out.println((String)("Can't find car: " + string));
                    continue block2;
                }
                int n = 80;
                if (SystemDisabler.doVehiclesEverywhere || DebugOptions.instance.VehicleSpawnEverywhere.getValue()) {
                    n = 100;
                }
                if (Rand.Next((int)100) > n) continue;
                BaseVehicle baseVehicle = new BaseVehicle((IsoCell)IsoWorld.instance.CurrentCell);
                baseVehicle.setZone((String)"TrafficJam");
                baseVehicle.setVehicleType((String)vehicleType.name);
                if (!this.RandomizeModel((BaseVehicle)baseVehicle, (IsoMetaGrid.Zone)zone, (String)string, (VehicleType)vehicleType)) {
                    return;
                }
                baseVehicle.setScript();
                baseVehicle.setX((float)((float)isoGridSquare.x + Rand.Next((float)0.0f, (float)1.0f)));
                baseVehicle.setY((float)((float)isoGridSquare.y + Rand.Next((float)0.0f, (float)1.0f)));
                baseVehicle.setZ((float)((float)isoGridSquare.z));
                baseVehicle.jniTransform.origin.set((float)(baseVehicle.getX() - WorldSimulation.instance.offsetX), (float)baseVehicle.getZ(), (float)(baseVehicle.getY() - WorldSimulation.instance.offsetY));
                if (!this.isGoodVehiclePos((BaseVehicle)baseVehicle, (IsoChunk)this)) continue;
                baseVehicle.setSkinIndex((int)Rand.Next((int)(baseVehicle.getSkinCount() - 1)));
                baseVehicle.setDir((IsoDirections)IsoDirections.E);
                float f = (float)Math.abs((int)(zone.x + zone.w - isoGridSquare.x - zone.w));
                f /= 20.0f;
                f = Math.min((float)2.0f, (float)f);
                float f2 = baseVehicle.getDir().toAngle() + (float)Math.PI - 0.25f + Rand.Next((float)0.0f, (float)f);
                while ((double)f2 > Math.PI * 2) {
                    f2 = (float)((double)f2 - Math.PI * 2);
                }
                baseVehicle.savedRot.setAngleAxis((float)f2, (float)0.0f, (float)1.0f, (float)0.0f);
                baseVehicle.jniTransform.setRotation((Quaternionf)baseVehicle.savedRot);
                if (!IsoChunk.doSpawnedVehiclesInInvalidPosition((BaseVehicle)baseVehicle) && !GameClient.bClient) continue;
                this.vehicles.add((Object)baseVehicle);
            }
        }
    }

    private void AddVehicles_TrafficJam_S(IsoMetaGrid.Zone zone, String string) {
        for (int i = zone.y - this.wy * 10 + 3; i < 0; i += 6) {
        }
        for (int i = zone.x - this.wx * 10 + 1; i < 0; i += 3) {
        }
        block2: for (int i = i; i < 10 && this.wy * 10 + i < zone.y + zone.h; i += 6 + Rand.Next((int)-1, (int)1)) {
            for (int j = i; j < 10 && this.wx * 10 + j < zone.x + zone.w; j += 3 + Rand.Next((int)1)) {
                IsoGridSquare isoGridSquare = this.getGridSquare((int)j, (int)i, (int)0);
                if (isoGridSquare == null) continue;
                VehicleType vehicleType = VehicleType.getRandomVehicleType((String)string);
                if (vehicleType == null) {
                    System.out.println((String)("Can't find car: " + string));
                    continue block2;
                }
                int n = 80;
                if (SystemDisabler.doVehiclesEverywhere || DebugOptions.instance.VehicleSpawnEverywhere.getValue()) {
                    n = 100;
                }
                if (Rand.Next((int)100) > n) continue;
                BaseVehicle baseVehicle = new BaseVehicle((IsoCell)IsoWorld.instance.CurrentCell);
                baseVehicle.setZone((String)"TrafficJam");
                baseVehicle.setVehicleType((String)vehicleType.name);
                if (!this.RandomizeModel((BaseVehicle)baseVehicle, (IsoMetaGrid.Zone)zone, (String)string, (VehicleType)vehicleType)) {
                    return;
                }
                baseVehicle.setScript();
                baseVehicle.setX((float)((float)isoGridSquare.x + Rand.Next((float)0.0f, (float)1.0f)));
                baseVehicle.setY((float)((float)isoGridSquare.y + Rand.Next((float)0.0f, (float)1.0f)));
                baseVehicle.setZ((float)((float)isoGridSquare.z));
                baseVehicle.jniTransform.origin.set((float)(baseVehicle.getX() - WorldSimulation.instance.offsetX), (float)baseVehicle.getZ(), (float)(baseVehicle.getY() - WorldSimulation.instance.offsetY));
                if (!this.isGoodVehiclePos((BaseVehicle)baseVehicle, (IsoChunk)this)) continue;
                baseVehicle.setSkinIndex((int)Rand.Next((int)(baseVehicle.getSkinCount() - 1)));
                baseVehicle.setDir((IsoDirections)IsoDirections.S);
                float f = (float)Math.abs((int)(zone.y + zone.h - isoGridSquare.y - zone.h));
                f /= 20.0f;
                f = Math.min((float)2.0f, (float)f);
                float f2 = baseVehicle.getDir().toAngle() + (float)Math.PI - 0.25f + Rand.Next((float)0.0f, (float)f);
                while ((double)f2 > Math.PI * 2) {
                    f2 = (float)((double)f2 - Math.PI * 2);
                }
                baseVehicle.savedRot.setAngleAxis((float)f2, (float)0.0f, (float)1.0f, (float)0.0f);
                baseVehicle.jniTransform.setRotation((Quaternionf)baseVehicle.savedRot);
                if (!IsoChunk.doSpawnedVehiclesInInvalidPosition((BaseVehicle)baseVehicle) && !GameClient.bClient) continue;
                this.vehicles.add((Object)baseVehicle);
            }
        }
    }

    private void AddVehicles_TrafficJam_N(IsoMetaGrid.Zone zone, String string) {
        for (int i = zone.y - this.wy * 10 + 3; i < 0; i += 6) {
        }
        for (int i = zone.x - this.wx * 10 + 1; i < 0; i += 3) {
        }
        block2: for (int i = i; i < 10 && this.wy * 10 + i < zone.y + zone.h; i += 6 + Rand.Next((int)-1, (int)1)) {
            for (int j = i; j < 10 && this.wx * 10 + j < zone.x + zone.w; j += 3 + Rand.Next((int)1)) {
                IsoGridSquare isoGridSquare = this.getGridSquare((int)j, (int)i, (int)0);
                if (isoGridSquare == null) continue;
                VehicleType vehicleType = VehicleType.getRandomVehicleType((String)string);
                if (vehicleType == null) {
                    System.out.println((String)("Can't find car: " + string));
                    continue block2;
                }
                int n = 80;
                if (SystemDisabler.doVehiclesEverywhere || DebugOptions.instance.VehicleSpawnEverywhere.getValue()) {
                    n = 100;
                }
                if (Rand.Next((int)100) > n) continue;
                BaseVehicle baseVehicle = new BaseVehicle((IsoCell)IsoWorld.instance.CurrentCell);
                baseVehicle.setZone((String)"TrafficJam");
                baseVehicle.setVehicleType((String)vehicleType.name);
                if (!this.RandomizeModel((BaseVehicle)baseVehicle, (IsoMetaGrid.Zone)zone, (String)string, (VehicleType)vehicleType)) {
                    return;
                }
                baseVehicle.setScript();
                baseVehicle.setX((float)((float)isoGridSquare.x + Rand.Next((float)0.0f, (float)1.0f)));
                baseVehicle.setY((float)((float)isoGridSquare.y + Rand.Next((float)0.0f, (float)1.0f)));
                baseVehicle.setZ((float)((float)isoGridSquare.z));
                baseVehicle.jniTransform.origin.set((float)(baseVehicle.getX() - WorldSimulation.instance.offsetX), (float)baseVehicle.getZ(), (float)(baseVehicle.getY() - WorldSimulation.instance.offsetY));
                if (!this.isGoodVehiclePos((BaseVehicle)baseVehicle, (IsoChunk)this)) continue;
                baseVehicle.setSkinIndex((int)Rand.Next((int)(baseVehicle.getSkinCount() - 1)));
                baseVehicle.setDir((IsoDirections)IsoDirections.N);
                float f = (float)Math.abs((int)(zone.y + zone.h - isoGridSquare.y));
                f /= 20.0f;
                f = Math.min((float)2.0f, (float)f);
                float f2 = baseVehicle.getDir().toAngle() + (float)Math.PI - 0.25f + Rand.Next((float)0.0f, (float)f);
                while ((double)f2 > Math.PI * 2) {
                    f2 = (float)((double)f2 - Math.PI * 2);
                }
                baseVehicle.savedRot.setAngleAxis((float)f2, (float)0.0f, (float)1.0f, (float)0.0f);
                baseVehicle.jniTransform.setRotation((Quaternionf)baseVehicle.savedRot);
                if (!IsoChunk.doSpawnedVehiclesInInvalidPosition((BaseVehicle)baseVehicle) && !GameClient.bClient) continue;
                this.vehicles.add((Object)baseVehicle);
            }
        }
    }

    private void AddVehicles_TrafficJam_Polyline(IsoMetaGrid.Zone zone, String string) {
        Vector2 vector2 = new Vector2();
        Vector2 vector22 = new Vector2();
        float f = 0.0f;
        float f2 = zone.getPolylineLength();
        for (int i = 0; i < zone.points.size() - 2; i += 2) {
            int n = zone.points.getQuick((int)i);
            int n2 = zone.points.getQuick((int)(i + 1));
            int n3 = zone.points.getQuick((int)(i + 2));
            int n4 = zone.points.getQuick((int)(i + 3));
            vector2.set((float)((float)(n3 - n)), (float)((float)(n4 - n2)));
            float f3 = vector2.getLength();
            vector22.set((Vector2)vector2);
            vector22.tangent();
            vector22.normalize();
            float f4 = f;
            f += f3;
            for (float f5 = 3.0f; f5 <= f3 - 3.0f; f5 += (float)(6 + Rand.Next((int)-1, (int)1))) {
                float f6 = PZMath.clamp((float)(f5 + Rand.Next((float)-1.0f, (float)1.0f)), (float)3.0f, (float)(f3 - 3.0f));
                float f7 = Rand.Next((float)-1.0f, (float)1.0f);
                float f8 = (float)n + vector2.x / f3 * f6 + vector22.x * f7;
                float f9 = (float)n2 + vector2.y / f3 * f6 + vector22.y * f7;
                this.TryAddVehicle_TrafficJam((IsoMetaGrid.Zone)zone, (String)string, (float)f8, (float)f9, (Vector2)vector2, (float)(f4 + f6), (float)f2);
                float f10 = 2.0f;
                while (f10 + 1.5f <= (float)zone.polylineWidth / 2.0f) {
                    f7 = f10 + Rand.Next((float)-1.0f, (float)1.0f);
                    if (f7 + 1.5f <= (float)zone.polylineWidth / 2.0f) {
                        f6 = PZMath.clamp((float)(f5 + Rand.Next((float)-2.0f, (float)2.0f)), (float)3.0f, (float)(f3 - 3.0f));
                        f8 = (float)n + vector2.x / f3 * f6 + vector22.x * f7;
                        f9 = (float)n2 + vector2.y / f3 * f6 + vector22.y * f7;
                        this.TryAddVehicle_TrafficJam((IsoMetaGrid.Zone)zone, (String)string, (float)f8, (float)f9, (Vector2)vector2, (float)(f4 + f6), (float)f2);
                    }
                    if ((f7 = f10 + Rand.Next((float)-1.0f, (float)1.0f)) + 1.5f <= (float)zone.polylineWidth / 2.0f) {
                        f6 = PZMath.clamp((float)(f5 + Rand.Next((float)-2.0f, (float)2.0f)), (float)3.0f, (float)(f3 - 3.0f));
                        f8 = (float)n + vector2.x / f3 * f6 - vector22.x * f7;
                        f9 = (float)n2 + vector2.y / f3 * f6 - vector22.y * f7;
                        this.TryAddVehicle_TrafficJam((IsoMetaGrid.Zone)zone, (String)string, (float)f8, (float)f9, (Vector2)vector2, (float)(f4 + f6), (float)f2);
                    }
                    f10 += 2.0f;
                }
            }
        }
    }

    private void TryAddVehicle_TrafficJam(IsoMetaGrid.Zone zone, String string, float f, float f2, Vector2 vector2, float f3, float f4) {
        if (f < (float)(this.wx * 10) || f >= (float)((this.wx + 1) * 10) || f2 < (float)(this.wy * 10) || f2 >= (float)((this.wy + 1) * 10)) {
            return;
        }
        IsoGridSquare isoGridSquare = this.getGridSquare((int)((int)f - this.wx * 10), (int)((int)f2 - this.wy * 10), (int)0);
        if (isoGridSquare == null) {
            return;
        }
        VehicleType vehicleType = VehicleType.getRandomVehicleType((String)(string + "W"));
        if (vehicleType == null) {
            System.out.println((String)("Can't find car: " + string));
            return;
        }
        int n = 80;
        if (SystemDisabler.doVehiclesEverywhere || DebugOptions.instance.VehicleSpawnEverywhere.getValue()) {
            n = 100;
        }
        if (Rand.Next((int)100) > n) {
            return;
        }
        BaseVehicle baseVehicle = new BaseVehicle((IsoCell)IsoWorld.instance.CurrentCell);
        baseVehicle.setZone((String)"TrafficJam");
        baseVehicle.setVehicleType((String)vehicleType.name);
        if (!this.RandomizeModel((BaseVehicle)baseVehicle, (IsoMetaGrid.Zone)zone, (String)string, (VehicleType)vehicleType)) {
            return;
        }
        baseVehicle.setScript();
        baseVehicle.setX((float)f);
        baseVehicle.setY((float)f2);
        baseVehicle.setZ((float)((float)isoGridSquare.z));
        float f5 = vector2.x;
        float f6 = vector2.y;
        vector2.normalize();
        baseVehicle.setDir((IsoDirections)IsoDirections.fromAngle((Vector2)vector2));
        float f7 = vector2.getDirectionNeg();
        vector2.set((float)f5, (float)f6);
        float f8 = 90.0f * (f3 / f4);
        f7 += Rand.Next((float)(-f8), (float)f8) * ((float)Math.PI / 180);
        while ((double)f7 > Math.PI * 2) {
            f7 = (float)((double)f7 - Math.PI * 2);
        }
        baseVehicle.savedRot.setAngleAxis((float)f7, (float)0.0f, (float)1.0f, (float)0.0f);
        baseVehicle.jniTransform.setRotation((Quaternionf)baseVehicle.savedRot);
        baseVehicle.jniTransform.origin.set((float)(baseVehicle.getX() - WorldSimulation.instance.offsetX), (float)baseVehicle.getZ(), (float)(baseVehicle.getY() - WorldSimulation.instance.offsetY));
        if (this.isGoodVehiclePos((BaseVehicle)baseVehicle, (IsoChunk)this)) {
            baseVehicle.setSkinIndex((int)Rand.Next((int)(baseVehicle.getSkinCount() - 1)));
            if (IsoChunk.doSpawnedVehiclesInInvalidPosition((BaseVehicle)baseVehicle)) {
                this.vehicles.add((Object)baseVehicle);
            }
        }
    }

    public void AddVehicles() {
        Object object;
        IsoMetaCell isoMetaCell;
        if (SandboxOptions.instance.CarSpawnRate.getValue() == 1) {
            return;
        }
        if (VehicleType.vehicles.isEmpty()) {
            VehicleType.init();
        }
        if (GameClient.bClient) {
            return;
        }
        if (!SandboxOptions.instance.EnableVehicles.getValue()) {
            return;
        }
        if (!GameServer.bServer) {
            WorldSimulation.instance.create();
        }
        ArrayList<IsoMetaGrid.VehicleZone> arrayList = (isoMetaCell = IsoWorld.instance.getMetaGrid().getCellData((int)(this.wx / 30), (int)(this.wy / 30))) == null ? null : isoMetaCell.vehicleZones;
        for (int i = 0; arrayList != null && i < arrayList.size(); ++i) {
            IsoMetaGrid.VehicleZone vehicleZone = (IsoMetaGrid.VehicleZone)arrayList.get((int)i);
            if (vehicleZone.x + vehicleZone.w < this.wx * 10 || vehicleZone.y + vehicleZone.h < this.wy * 10 || vehicleZone.x >= (this.wx + 1) * 10 || vehicleZone.y >= (this.wy + 1) * 10) continue;
            object = vehicleZone.name;
            if (object.isEmpty()) {
                object = vehicleZone.type;
            }
            if (SandboxOptions.instance.TrafficJam.getValue()) {
                if (vehicleZone.isPolyline()) {
                    if ("TrafficJam".equalsIgnoreCase((String)object)) {
                        this.AddVehicles_TrafficJam_Polyline((IsoMetaGrid.Zone)vehicleZone, (String)object);
                        continue;
                    }
                    if ("RTrafficJam".equalsIgnoreCase((String)object) && Rand.Next((int)100) < 10) {
                        this.AddVehicles_TrafficJam_Polyline((IsoMetaGrid.Zone)vehicleZone, (String)object.replaceFirst((String)"rtraffic", (String)"traffic"));
                        continue;
                    }
                }
                if ("TrafficJamW".equalsIgnoreCase((String)object)) {
                    this.AddVehicles_TrafficJam_W((IsoMetaGrid.Zone)vehicleZone, (String)object);
                }
                if ("TrafficJamE".equalsIgnoreCase((String)object)) {
                    this.AddVehicles_TrafficJam_E((IsoMetaGrid.Zone)vehicleZone, (String)object);
                }
                if ("TrafficJamS".equalsIgnoreCase((String)object)) {
                    this.AddVehicles_TrafficJam_S((IsoMetaGrid.Zone)vehicleZone, (String)object);
                }
                if ("TrafficJamN".equalsIgnoreCase((String)object)) {
                    this.AddVehicles_TrafficJam_N((IsoMetaGrid.Zone)vehicleZone, (String)object);
                }
                if ("RTrafficJamW".equalsIgnoreCase((String)object) && Rand.Next((int)100) < 10) {
                    this.AddVehicles_TrafficJam_W((IsoMetaGrid.Zone)vehicleZone, (String)object.replaceFirst((String)"rtraffic", (String)"traffic"));
                }
                if ("RTrafficJamE".equalsIgnoreCase((String)object) && Rand.Next((int)100) < 10) {
                    this.AddVehicles_TrafficJam_E((IsoMetaGrid.Zone)vehicleZone, (String)object.replaceFirst((String)"rtraffic", (String)"traffic"));
                }
                if ("RTrafficJamS".equalsIgnoreCase((String)object) && Rand.Next((int)100) < 10) {
                    this.AddVehicles_TrafficJam_S((IsoMetaGrid.Zone)vehicleZone, (String)object.replaceFirst((String)"rtraffic", (String)"traffic"));
                }
                if ("RTrafficJamN".equalsIgnoreCase((String)object) && Rand.Next((int)100) < 10) {
                    this.AddVehicles_TrafficJam_N((IsoMetaGrid.Zone)vehicleZone, (String)object.replaceFirst((String)"rtraffic", (String)"traffic"));
                }
            }
            if (StringUtils.containsIgnoreCase((String)object, (String)"TrafficJam")) continue;
            if ("TestVehicles".equals((Object)object)) {
                this.AddVehicles_ForTest((IsoMetaGrid.Zone)vehicleZone);
                continue;
            }
            if (!VehicleType.hasTypeForZone((String)object)) continue;
            if (vehicleZone.isPolyline()) {
                this.AddVehicles_OnZonePolyline((IsoMetaGrid.VehicleZone)vehicleZone, (String)object);
                continue;
            }
            this.AddVehicles_OnZone((IsoMetaGrid.VehicleZone)vehicleZone, (String)object);
        }
        IsoMetaChunk isoMetaChunk = IsoWorld.instance.getMetaChunk((int)this.wx, (int)this.wy);
        if (isoMetaChunk == null) {
            return;
        }
        for (int i = 0; i < isoMetaChunk.numZones(); ++i) {
            object = isoMetaChunk.getZone((int)i);
            this.addRandomCarCrash((IsoMetaGrid.Zone)object, (boolean)false);
        }
    }

    public void addSurvivorInHorde(boolean bl) {
        if (!bl && IsoWorld.getZombiesDisabled()) {
            return;
        }
        IsoMetaChunk isoMetaChunk = IsoWorld.instance.getMetaChunk((int)this.wx, (int)this.wy);
        if (isoMetaChunk == null) {
            return;
        }
        for (int i = 0; i < isoMetaChunk.numZones(); ++i) {
            IsoMetaGrid.Zone zone = isoMetaChunk.getZone((int)i);
            if (!this.canAddSurvivorInHorde((IsoMetaGrid.Zone)zone, (boolean)bl)) continue;
            int n = 4;
            float f = (float)GameTime.getInstance().getWorldAgeHours() / 24.0f;
            n = (int)((float)n + (f += (float)((SandboxOptions.instance.TimeSinceApo.getValue() - 1) * 30)) * 0.03f);
            n = Math.max((int)15, (int)n);
            if (!bl && !(Rand.Next((float)0.0f, (float)500.0f) < 0.4f * (float)n)) continue;
            this.addSurvivorInHorde((IsoMetaGrid.Zone)zone);
            if (bl) break;
        }
    }

    private boolean canAddSurvivorInHorde(IsoMetaGrid.Zone zone, boolean bl) {
        if (!bl && IsoWorld.instance.getTimeSinceLastSurvivorInHorde() > 0) {
            return false;
        }
        if (!bl && IsoWorld.getZombiesDisabled()) {
            return false;
        }
        if (!bl && zone.hourLastSeen != 0) {
            return false;
        }
        if (!bl && zone.haveConstruction) {
            return false;
        }
        return "Nav".equals((Object)zone.getType());
    }

    private void addSurvivorInHorde(IsoMetaGrid.Zone zone) {
        ++zone.hourLastSeen;
        IsoWorld.instance.setTimeSinceLastSurvivorInHorde((int)5000);
        int n = Math.max((int)zone.x, (int)(this.wx * 10));
        int n2 = Math.max((int)zone.y, (int)(this.wy * 10));
        int n3 = Math.min((int)(zone.x + zone.w), (int)((this.wx + 1) * 10));
        int n4 = Math.min((int)(zone.y + zone.h), (int)((this.wy + 1) * 10));
        float f = (float)n + (float)(n3 - n) / 2.0f;
        float f2 = (float)n2 + (float)(n4 - n2) / 2.0f;
        VirtualZombieManager.instance.choices.clear();
        IsoGridSquare isoGridSquare = this.getGridSquare((int)((int)f - this.wx * 10), (int)((int)f2 - this.wy * 10), (int)0);
        if (isoGridSquare.getBuilding() != null) {
            return;
        }
        VirtualZombieManager.instance.choices.add((Object)isoGridSquare);
        int n5 = Rand.Next((int)15, (int)20);
        for (int i = 0; i < n5; ++i) {
            IsoZombie isoZombie = VirtualZombieManager.instance.createRealZombieAlways((int)Rand.Next((int)8), (boolean)false);
            if (isoZombie == null) continue;
            isoZombie.dressInRandomOutfit();
            ZombieSpawnRecorder.instance.record((IsoZombie)isoZombie, (String)"addSurvivorInHorde");
        }
        IsoZombie isoZombie = VirtualZombieManager.instance.createRealZombieAlways((int)Rand.Next((int)8), (boolean)false);
        if (isoZombie != null) {
            ZombieSpawnRecorder.instance.record((IsoZombie)isoZombie, (String)"addSurvivorInHorde");
            isoZombie.setAsSurvivor();
        }
    }

    public boolean canAddRandomCarCrash(IsoMetaGrid.Zone zone, boolean bl) {
        if (!bl && zone.hourLastSeen != 0) {
            return false;
        }
        if (!bl && zone.haveConstruction) {
            return false;
        }
        if (!"Nav".equals((Object)zone.getType())) {
            return false;
        }
        int n = Math.max((int)zone.x, (int)(this.wx * 10));
        int n2 = Math.max((int)zone.y, (int)(this.wy * 10));
        int n3 = Math.min((int)(zone.x + zone.w), (int)((this.wx + 1) * 10));
        int n4 = Math.min((int)(zone.y + zone.h), (int)((this.wy + 1) * 10));
        if (zone.w > 30 && zone.h < 13) {
            return n3 - n >= 10 && n4 - n2 >= 5;
        }
        if (zone.h > 30 && zone.w < 13) {
            return n3 - n >= 5 && n4 - n2 >= 10;
        }
        return false;
    }

    public void addRandomCarCrash(IsoMetaGrid.Zone zone, boolean bl) {
        if (!this.vehicles.isEmpty()) {
            return;
        }
        if (!"Nav".equals((Object)zone.getType())) {
            return;
        }
        RandomizedVehicleStoryBase.doRandomStory((IsoMetaGrid.Zone)zone, (IsoChunk)this, (boolean)false);
    }

    public static boolean FileExists(int n, int n2) {
        File file = ChunkMapFilenames.instance.getFilename((int)n, (int)n2);
        if (file == null) {
            file = ZomboidFileSystem.instance.getFileInCurrentSave((String)(prefix + n + "_" + n2 + ".bin"));
        }
        long l = 0L;
        return file.exists();
    }

    private void checkPhysics() {
        if (!this.physicsCheck) {
            return;
        }
        WorldSimulation.instance.create();
        Bullet.beginUpdateChunk((IsoChunk)this);
        int n = 0;
        if (n < 8) {
            for (int i = 0; i < 10; ++i) {
                for (int j = 0; j < 10; ++j) {
                    this.calcPhysics((int)j, (int)i, (int)n, (PhysicsShapes[])this.shapes);
                    int n2 = 0;
                    for (int k = 0; k < 4; ++k) {
                        if (this.shapes[k] == null) continue;
                        IsoChunk.bshapes[n2++] = (byte)(this.shapes[k].ordinal() + 1);
                    }
                    Bullet.updateChunk((int)j, (int)i, (int)n, (int)n2, (byte[])bshapes);
                }
            }
        }
        Bullet.endUpdateChunk();
        this.physicsCheck = false;
    }

    private void calcPhysics(int n, int n2, int n3, PhysicsShapes[] physicsShapesArray) {
        Object object;
        Object object2;
        int n4;
        boolean bl;
        for (int i = 0; i < 4; ++i) {
            physicsShapesArray[i] = null;
        }
        IsoGridSquare isoGridSquare = this.getGridSquare((int)n, (int)n2, (int)n3);
        if (isoGridSquare == null) {
            return;
        }
        int n5 = 0;
        if (n3 == 0) {
            bl = false;
            for (n4 = 0; n4 < isoGridSquare.getObjects().size(); ++n4) {
                object2 = (IsoObject)isoGridSquare.getObjects().get((int)n4);
                if (((IsoObject)object2).sprite == null || ((IsoObject)object2).sprite.name == null || !((IsoObject)object2).sprite.name.contains((CharSequence)"lighting_outdoor_") && !((IsoObject)object2).sprite.name.equals((Object)"recreational_sports_01_21") && !((IsoObject)object2).sprite.name.equals((Object)"recreational_sports_01_19") && !((IsoObject)object2).sprite.name.equals((Object)"recreational_sports_01_32") || ((IsoObject)object2).getProperties().Is((String)"MoveType") && "WallObject".equals((Object)((IsoObject)object2).getProperties().Val((String)"MoveType"))) continue;
                bl = true;
                break;
            }
            if (bl) {
                physicsShapesArray[n5++] = PhysicsShapes.Tree;
            }
        }
        bl = false;
        if (!isoGridSquare.getSpecialObjects().isEmpty()) {
            n4 = isoGridSquare.getSpecialObjects().size();
            for (int i = 0; i < n4; ++i) {
                object = (IsoObject)isoGridSquare.getSpecialObjects().get((int)i);
                if (!(object instanceof IsoThumpable) || !((IsoThumpable)object).isBlockAllTheSquare()) continue;
                bl = true;
                break;
            }
        }
        PropertyContainer propertyContainer = isoGridSquare.getProperties();
        if (isoGridSquare.hasTypes.isSet((IsoObjectType)IsoObjectType.isMoveAbleObject)) {
            physicsShapesArray[n5++] = PhysicsShapes.Tree;
        }
        if (isoGridSquare.hasTypes.isSet((IsoObjectType)IsoObjectType.tree)) {
            object2 = isoGridSquare.getProperties().Val((String)"tree");
            object = isoGridSquare.getProperties().Val((String)"WindType");
            if (object2 == null) {
                physicsShapesArray[n5++] = PhysicsShapes.Tree;
            }
            if (!(object2 == null || object2.equals((Object)"1") || object != null && object.equals((Object)"2") && (object2.equals((Object)"2") || object2.equals((Object)"1")))) {
                physicsShapesArray[n5++] = PhysicsShapes.Tree;
            }
        } else if (propertyContainer.Is((IsoFlagType)IsoFlagType.solid) || propertyContainer.Is((IsoFlagType)IsoFlagType.solidtrans) || propertyContainer.Is((IsoFlagType)IsoFlagType.blocksight) || isoGridSquare.HasStairs() || bl) {
            if (n5 == physicsShapesArray.length) {
                DebugLog.log((DebugType)DebugType.General, (String)("Error: Too many physics objects on gridsquare: " + isoGridSquare.x + ", " + isoGridSquare.y + ", " + isoGridSquare.z));
                return;
            }
            physicsShapesArray[n5++] = PhysicsShapes.Solid;
        } else if (n3 > 0 && (isoGridSquare.SolidFloorCached ? isoGridSquare.SolidFloor : isoGridSquare.TreatAsSolidFloor())) {
            if (n5 == physicsShapesArray.length) {
                DebugLog.log((DebugType)DebugType.General, (String)("Error: Too many physics objects on gridsquare: " + isoGridSquare.x + ", " + isoGridSquare.y + ", " + isoGridSquare.z));
                return;
            }
            physicsShapesArray[n5++] = PhysicsShapes.Floor;
        }
        if (isoGridSquare.getProperties().Is((String)"CarSlowFactor")) {
            return;
        }
        if (propertyContainer.Is((IsoFlagType)IsoFlagType.collideW) || propertyContainer.Is((IsoFlagType)IsoFlagType.windowW) || isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.DoorWallW) && !isoGridSquare.getProperties().Is((String)"GarageDoor")) {
            if (n5 == physicsShapesArray.length) {
                DebugLog.log((DebugType)DebugType.General, (String)("Error: Too many physics objects on gridsquare: " + isoGridSquare.x + ", " + isoGridSquare.y + ", " + isoGridSquare.z));
                return;
            }
            physicsShapesArray[n5++] = PhysicsShapes.WallW;
        }
        if (propertyContainer.Is((IsoFlagType)IsoFlagType.collideN) || propertyContainer.Is((IsoFlagType)IsoFlagType.windowN) || isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.DoorWallN) && !isoGridSquare.getProperties().Is((String)"GarageDoor")) {
            if (n5 == physicsShapesArray.length) {
                DebugLog.log((DebugType)DebugType.General, (String)("Error: Too many physics objects on gridsquare: " + isoGridSquare.x + ", " + isoGridSquare.y + ", " + isoGridSquare.z));
                return;
            }
            physicsShapesArray[n5++] = PhysicsShapes.WallN;
        }
        if (isoGridSquare.Is((String)"PhysicsShape")) {
            if (n5 == physicsShapesArray.length) {
                DebugLog.log((DebugType)DebugType.General, (String)("Error: Too many physics objects on gridsquare: " + isoGridSquare.x + ", " + isoGridSquare.y + ", " + isoGridSquare.z));
                return;
            }
            object2 = isoGridSquare.getProperties().Val((String)"PhysicsShape");
            if ("Solid".equals((Object)object2)) {
                physicsShapesArray[n5++] = PhysicsShapes.Solid;
            } else if ("WallN".equals((Object)object2)) {
                physicsShapesArray[n5++] = PhysicsShapes.WallN;
            } else if ("WallW".equals((Object)object2)) {
                physicsShapesArray[n5++] = PhysicsShapes.WallW;
            } else if ("WallS".equals((Object)object2)) {
                physicsShapesArray[n5++] = PhysicsShapes.WallS;
            } else if ("WallE".equals((Object)object2)) {
                physicsShapesArray[n5++] = PhysicsShapes.WallE;
            } else if ("Tree".equals((Object)object2)) {
                physicsShapesArray[n5++] = PhysicsShapes.Tree;
            } else if ("Floor".equals((Object)object2)) {
                physicsShapesArray[n5++] = PhysicsShapes.Floor;
            }
        }
    }

    public boolean LoadBrandNew(int n, int n2) {
        this.wx = n;
        this.wy = n2;
        if (!CellLoader.LoadCellBinaryChunk((IsoCell)IsoWorld.instance.CurrentCell, (int)n, (int)n2, (IsoChunk)this)) {
            return false;
        }
        if (!(Core.GameMode.equals((Object)"Tutorial") || Core.GameMode.equals((Object)"LastStand") || GameClient.bClient)) {
            this.addZombies = true;
        }
        return true;
    }

    public boolean LoadOrCreate(int n, int n2, ByteBuffer byteBuffer) {
        this.wx = n;
        this.wy = n2;
        if (byteBuffer != null && !this.blam) {
            return this.LoadFromBuffer((int)n, (int)n2, (ByteBuffer)byteBuffer);
        }
        File file = ChunkMapFilenames.instance.getFilename((int)n, (int)n2);
        if (file == null) {
            file = ZomboidFileSystem.instance.getFileInCurrentSave((String)(prefix + n + "_" + n2 + ".bin"));
        }
        if (file.exists() && !this.blam) {
            try {
                this.LoadFromDisk();
            }
            catch (Exception exception) {
                ExceptionLogger.logException((Throwable)exception, (String)("Error loading chunk " + n + "," + n2));
                if (GameServer.bServer) {
                    LoggerManager.getLogger((String)"map").write((String)("Error loading chunk " + n + "," + n2));
                    LoggerManager.getLogger((String)"map").write((Exception)exception);
                }
                this.BackupBlam((int)n, (int)n2, (Exception)exception);
                return false;
            }
        } else {
            return this.LoadBrandNew((int)n, (int)n2);
        }
        if (GameClient.bClient) {
            GameClient.instance.worldObjectsSyncReq.putRequestSyncIsoChunk((IsoChunk)this);
        }
        return true;
    }

    public boolean LoadFromBuffer(int n, int n2, ByteBuffer byteBuffer) {
        this.wx = n;
        this.wy = n2;
        if (!this.blam) {
            try {
                this.LoadFromDiskOrBuffer((ByteBuffer)byteBuffer);
                return true;
            }
            catch (Exception exception) {
                ExceptionLogger.logException((Throwable)exception);
                if (GameServer.bServer) {
                    LoggerManager.getLogger((String)"map").write((String)("Error loading chunk " + n + "," + n2));
                    LoggerManager.getLogger((String)"map").write((Exception)exception);
                }
                this.BackupBlam((int)n, (int)n2, (Exception)exception);
                return false;
            }
        }
        return this.LoadBrandNew((int)n, (int)n2);
    }

    private void ensureSurroundNotNull(int n, int n2, int n3) {
        IsoCell isoCell = IsoWorld.instance.CurrentCell;
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                IsoGridSquare isoGridSquare;
                if (i == 0 && j == 0 || n + i < 0 || n + i >= 10 || n2 + j < 0 || n2 + j >= 10 || (isoGridSquare = this.getGridSquare((int)(n + i), (int)(n2 + j), (int)n3)) != null) continue;
                isoGridSquare = IsoGridSquare.getNew((IsoCell)isoCell, null, (int)(this.wx * 10 + n + i), (int)(this.wy * 10 + n2 + j), (int)n3);
                this.setSquare((int)(n + i), (int)(n2 + j), (int)n3, (IsoGridSquare)isoGridSquare);
            }
        }
    }

    public void loadInWorldStreamerThread() {
        IsoGridSquare isoGridSquare;
        int n;
        int n2;
        int n3;
        IsoCell isoCell = IsoWorld.instance.CurrentCell;
        for (n3 = 0; n3 <= this.maxLevel; ++n3) {
            for (n2 = 0; n2 < 10; ++n2) {
                for (n = 0; n < 10; ++n) {
                    isoGridSquare = this.getGridSquare((int)n, (int)n2, (int)n3);
                    if (isoGridSquare == null && n3 == 0) {
                        isoGridSquare = IsoGridSquare.getNew((IsoCell)IsoWorld.instance.CurrentCell, null, (int)(this.wx * 10 + n), (int)(this.wy * 10 + n2), (int)n3);
                        this.setSquare((int)n, (int)n2, (int)n3, (IsoGridSquare)isoGridSquare);
                    }
                    if (n3 == 0 && isoGridSquare.getFloor() == null) {
                        DebugLog.log((String)("ERROR: added floor at " + isoGridSquare.x + "," + isoGridSquare.y + "," + isoGridSquare.z + " because there wasn't one"));
                        IsoObject isoObject = IsoObject.getNew();
                        isoObject.sprite = IsoSprite.getSprite((IsoSpriteManager)IsoSpriteManager.instance, (String)"carpentry_02_58", (int)0);
                        isoObject.square = isoGridSquare;
                        isoGridSquare.Objects.add((int)0, (Object)isoObject);
                    }
                    if (isoGridSquare == null) continue;
                    if (n3 > 0 && !isoGridSquare.getObjects().isEmpty()) {
                        this.ensureSurroundNotNull((int)n, (int)n2, (int)n3);
                        for (int i = n3 - 1; i > 0; --i) {
                            IsoGridSquare isoGridSquare2 = this.getGridSquare((int)n, (int)n2, (int)i);
                            if (isoGridSquare2 != null) continue;
                            isoGridSquare2 = IsoGridSquare.getNew((IsoCell)isoCell, null, (int)(this.wx * 10 + n), (int)(this.wy * 10 + n2), (int)i);
                            this.setSquare((int)n, (int)n2, (int)i, (IsoGridSquare)isoGridSquare2);
                            this.ensureSurroundNotNull((int)n, (int)n2, (int)i);
                        }
                    }
                    isoGridSquare.RecalcProperties();
                }
            }
        }
        if (!$assertionsDisabled && IsoChunk.chunkGetter.chunk != null) {
            throw new AssertionError();
        }
        IsoChunk.chunkGetter.chunk = this;
        for (n3 = 0; n3 < 10; ++n3) {
            block5: for (n2 = 0; n2 < 10; ++n2) {
                for (n = this.maxLevel; n > 0; --n) {
                    isoGridSquare = this.getGridSquare((int)n2, (int)n3, (int)n);
                    if (isoGridSquare == null || !isoGridSquare.Is((IsoFlagType)IsoFlagType.solidfloor)) continue;
                    --n;
                    while (n >= 0) {
                        isoGridSquare = this.getGridSquare((int)n2, (int)n3, (int)n);
                        if (isoGridSquare != null && !isoGridSquare.haveRoof) {
                            isoGridSquare.haveRoof = true;
                            isoGridSquare.getProperties().UnSet((IsoFlagType)IsoFlagType.exterior);
                        }
                        --n;
                    }
                    continue block5;
                }
            }
        }
        for (n3 = 0; n3 <= this.maxLevel; ++n3) {
            for (n2 = 0; n2 < 10; ++n2) {
                for (n = 0; n < 10; ++n) {
                    isoGridSquare = this.getGridSquare((int)n, (int)n2, (int)n3);
                    if (isoGridSquare == null) continue;
                    isoGridSquare.RecalcAllWithNeighbours((boolean)true, (IsoGridSquare.GetSquare)chunkGetter);
                }
            }
        }
        IsoChunk.chunkGetter.chunk = null;
        for (n3 = 0; n3 <= this.maxLevel; ++n3) {
            for (n2 = 0; n2 < 10; ++n2) {
                for (n = 0; n < 10; ++n) {
                    isoGridSquare = this.getGridSquare((int)n, (int)n2, (int)n3);
                    if (isoGridSquare == null) continue;
                    isoGridSquare.propertiesDirty = true;
                }
            }
        }
    }

    private void RecalcAllWithNeighbour(IsoGridSquare isoGridSquare, IsoDirections isoDirections, int n) {
        IsoGridSquare isoGridSquare2;
        int n2 = 0;
        int n3 = 0;
        if (isoDirections == IsoDirections.W || isoDirections == IsoDirections.NW || isoDirections == IsoDirections.SW) {
            n2 = -1;
        } else if (isoDirections == IsoDirections.E || isoDirections == IsoDirections.NE || isoDirections == IsoDirections.SE) {
            n2 = 1;
        }
        if (isoDirections == IsoDirections.N || isoDirections == IsoDirections.NW || isoDirections == IsoDirections.NE) {
            n3 = -1;
        } else if (isoDirections == IsoDirections.S || isoDirections == IsoDirections.SW || isoDirections == IsoDirections.SE) {
            n3 = 1;
        }
        int n4 = isoGridSquare.getX() + n2;
        int n5 = isoGridSquare.getY() + n3;
        int n6 = isoGridSquare.getZ() + n;
        IsoGridSquare isoGridSquare3 = isoGridSquare2 = n == 0 ? isoGridSquare.nav[isoDirections.index()] : IsoWorld.instance.CurrentCell.getGridSquare((int)n4, (int)n5, (int)n6);
        if (isoGridSquare2 != null) {
            isoGridSquare.ReCalculateCollide((IsoGridSquare)isoGridSquare2);
            isoGridSquare2.ReCalculateCollide((IsoGridSquare)isoGridSquare);
            isoGridSquare.ReCalculatePathFind((IsoGridSquare)isoGridSquare2);
            isoGridSquare2.ReCalculatePathFind((IsoGridSquare)isoGridSquare);
            isoGridSquare.ReCalculateVisionBlocked((IsoGridSquare)isoGridSquare2);
            isoGridSquare2.ReCalculateVisionBlocked((IsoGridSquare)isoGridSquare);
        }
        if (n == 0) {
            switch (isoDirections) {
                case W: {
                    if (isoGridSquare2 == null) {
                        isoGridSquare.w = null;
                        break;
                    }
                    isoGridSquare.w = isoGridSquare.testPathFindAdjacent(null, (int)-1, (int)0, (int)0) ? null : isoGridSquare2;
                    isoGridSquare2.e = isoGridSquare2.testPathFindAdjacent(null, (int)1, (int)0, (int)0) ? null : isoGridSquare;
                    break;
                }
                case N: {
                    if (isoGridSquare2 == null) {
                        isoGridSquare.n = null;
                        break;
                    }
                    isoGridSquare.n = isoGridSquare.testPathFindAdjacent(null, (int)0, (int)-1, (int)0) ? null : isoGridSquare2;
                    isoGridSquare2.s = isoGridSquare2.testPathFindAdjacent(null, (int)0, (int)1, (int)0) ? null : isoGridSquare;
                    break;
                }
                case E: {
                    if (isoGridSquare2 == null) {
                        isoGridSquare.e = null;
                        break;
                    }
                    isoGridSquare.e = isoGridSquare.testPathFindAdjacent(null, (int)1, (int)0, (int)0) ? null : isoGridSquare2;
                    isoGridSquare2.w = isoGridSquare2.testPathFindAdjacent(null, (int)-1, (int)0, (int)0) ? null : isoGridSquare;
                    break;
                }
                case S: {
                    if (isoGridSquare2 == null) {
                        isoGridSquare.s = null;
                        break;
                    }
                    isoGridSquare.s = isoGridSquare.testPathFindAdjacent(null, (int)0, (int)1, (int)0) ? null : isoGridSquare2;
                    isoGridSquare2.n = isoGridSquare2.testPathFindAdjacent(null, (int)0, (int)-1, (int)0) ? null : isoGridSquare;
                    break;
                }
                case NW: {
                    if (isoGridSquare2 == null) {
                        isoGridSquare.nw = null;
                        break;
                    }
                    isoGridSquare.nw = isoGridSquare.testPathFindAdjacent(null, (int)-1, (int)-1, (int)0) ? null : isoGridSquare2;
                    isoGridSquare2.se = isoGridSquare2.testPathFindAdjacent(null, (int)1, (int)1, (int)0) ? null : isoGridSquare;
                    break;
                }
                case NE: {
                    if (isoGridSquare2 == null) {
                        isoGridSquare.ne = null;
                        break;
                    }
                    isoGridSquare.ne = isoGridSquare.testPathFindAdjacent(null, (int)1, (int)-1, (int)0) ? null : isoGridSquare2;
                    isoGridSquare2.sw = isoGridSquare2.testPathFindAdjacent(null, (int)-1, (int)1, (int)0) ? null : isoGridSquare;
                    break;
                }
                case SE: {
                    if (isoGridSquare2 == null) {
                        isoGridSquare.se = null;
                        break;
                    }
                    isoGridSquare.se = isoGridSquare.testPathFindAdjacent(null, (int)1, (int)1, (int)0) ? null : isoGridSquare2;
                    isoGridSquare2.nw = isoGridSquare2.testPathFindAdjacent(null, (int)-1, (int)-1, (int)0) ? null : isoGridSquare;
                    break;
                }
                case SW: {
                    if (isoGridSquare2 == null) {
                        isoGridSquare.sw = null;
                        break;
                    }
                    isoGridSquare.sw = isoGridSquare.testPathFindAdjacent(null, (int)-1, (int)1, (int)0) ? null : isoGridSquare2;
                    isoGridSquare2.ne = isoGridSquare2.testPathFindAdjacent(null, (int)1, (int)-1, (int)0) ? null : isoGridSquare;
                }
            }
        }
    }

    private void EnsureSurroundNotNullX(int n, int n2, int n3) {
        IsoCell isoCell = IsoWorld.instance.CurrentCell;
        for (int i = n - 1; i <= n + 1; ++i) {
            IsoGridSquare isoGridSquare;
            if (i < 0 || i >= 10 || (isoGridSquare = this.getGridSquare((int)i, (int)n2, (int)n3)) != null) continue;
            isoGridSquare = IsoGridSquare.getNew((IsoCell)isoCell, null, (int)(this.wx * 10 + i), (int)(this.wy * 10 + n2), (int)n3);
            isoCell.ConnectNewSquare((IsoGridSquare)isoGridSquare, (boolean)false);
        }
    }

    private void EnsureSurroundNotNullY(int n, int n2, int n3) {
        IsoCell isoCell = IsoWorld.instance.CurrentCell;
        for (int i = n2 - 1; i <= n2 + 1; ++i) {
            IsoGridSquare isoGridSquare;
            if (i < 0 || i >= 10 || (isoGridSquare = this.getGridSquare((int)n, (int)i, (int)n3)) != null) continue;
            isoGridSquare = IsoGridSquare.getNew((IsoCell)isoCell, null, (int)(this.wx * 10 + n), (int)(this.wy * 10 + i), (int)n3);
            isoCell.ConnectNewSquare((IsoGridSquare)isoGridSquare, (boolean)false);
        }
    }

    private void EnsureSurroundNotNull(int n, int n2, int n3) {
        IsoCell isoCell = IsoWorld.instance.CurrentCell;
        IsoGridSquare isoGridSquare = this.getGridSquare((int)n, (int)n2, (int)n3);
        if (isoGridSquare != null) {
            return;
        }
        isoGridSquare = IsoGridSquare.getNew((IsoCell)isoCell, null, (int)(this.wx * 10 + n), (int)(this.wy * 10 + n2), (int)n3);
        isoCell.ConnectNewSquare((IsoGridSquare)isoGridSquare, (boolean)false);
    }

    public void loadInMainThread() {
        IsoGridSquare isoGridSquare;
        int n;
        int n2;
        IsoCell isoCell = IsoWorld.instance.CurrentCell;
        IsoChunk isoChunk = isoCell.getChunk((int)(this.wx - 1), (int)this.wy);
        IsoChunk isoChunk2 = isoCell.getChunk((int)this.wx, (int)(this.wy - 1));
        IsoChunk isoChunk3 = isoCell.getChunk((int)(this.wx + 1), (int)this.wy);
        IsoChunk isoChunk4 = isoCell.getChunk((int)this.wx, (int)(this.wy + 1));
        IsoChunk isoChunk5 = isoCell.getChunk((int)(this.wx - 1), (int)(this.wy - 1));
        IsoChunk isoChunk6 = isoCell.getChunk((int)(this.wx + 1), (int)(this.wy - 1));
        IsoChunk isoChunk7 = isoCell.getChunk((int)(this.wx + 1), (int)(this.wy + 1));
        IsoChunk isoChunk8 = isoCell.getChunk((int)(this.wx - 1), (int)(this.wy + 1));
        for (n2 = 1; n2 < 8; ++n2) {
            for (n = 0; n < 10; ++n) {
                if (isoChunk2 != null && (isoGridSquare = isoChunk2.getGridSquare((int)n, (int)9, (int)n2)) != null && !isoGridSquare.getObjects().isEmpty()) {
                    this.EnsureSurroundNotNullX((int)n, (int)0, (int)n2);
                }
                if (isoChunk4 == null || (isoGridSquare = isoChunk4.getGridSquare((int)n, (int)0, (int)n2)) == null || isoGridSquare.getObjects().isEmpty()) continue;
                this.EnsureSurroundNotNullX((int)n, (int)9, (int)n2);
            }
            for (n = 0; n < 10; ++n) {
                if (isoChunk != null && (isoGridSquare = isoChunk.getGridSquare((int)9, (int)n, (int)n2)) != null && !isoGridSquare.getObjects().isEmpty()) {
                    this.EnsureSurroundNotNullY((int)0, (int)n, (int)n2);
                }
                if (isoChunk3 == null || (isoGridSquare = isoChunk3.getGridSquare((int)0, (int)n, (int)n2)) == null || isoGridSquare.getObjects().isEmpty()) continue;
                this.EnsureSurroundNotNullY((int)9, (int)n, (int)n2);
            }
            if (isoChunk5 != null && (isoGridSquare = isoChunk5.getGridSquare((int)9, (int)9, (int)n2)) != null && !isoGridSquare.getObjects().isEmpty()) {
                this.EnsureSurroundNotNull((int)0, (int)0, (int)n2);
            }
            if (isoChunk6 != null && (isoGridSquare = isoChunk6.getGridSquare((int)0, (int)9, (int)n2)) != null && !isoGridSquare.getObjects().isEmpty()) {
                this.EnsureSurroundNotNull((int)9, (int)0, (int)n2);
            }
            if (isoChunk7 != null && (isoGridSquare = isoChunk7.getGridSquare((int)0, (int)0, (int)n2)) != null && !isoGridSquare.getObjects().isEmpty()) {
                this.EnsureSurroundNotNull((int)9, (int)9, (int)n2);
            }
            if (isoChunk8 == null || (isoGridSquare = isoChunk8.getGridSquare((int)9, (int)0, (int)n2)) == null || isoGridSquare.getObjects().isEmpty()) continue;
            this.EnsureSurroundNotNull((int)0, (int)9, (int)n2);
        }
        for (n2 = 1; n2 < 8; ++n2) {
            for (n = 0; n < 10; ++n) {
                if (isoChunk2 != null && (isoGridSquare = this.getGridSquare((int)n, (int)0, (int)n2)) != null && !isoGridSquare.getObjects().isEmpty()) {
                    isoChunk2.EnsureSurroundNotNullX((int)n, (int)9, (int)n2);
                }
                if (isoChunk4 == null || (isoGridSquare = this.getGridSquare((int)n, (int)9, (int)n2)) == null || isoGridSquare.getObjects().isEmpty()) continue;
                isoChunk4.EnsureSurroundNotNullX((int)n, (int)0, (int)n2);
            }
            for (n = 0; n < 10; ++n) {
                if (isoChunk != null && (isoGridSquare = this.getGridSquare((int)0, (int)n, (int)n2)) != null && !isoGridSquare.getObjects().isEmpty()) {
                    isoChunk.EnsureSurroundNotNullY((int)9, (int)n, (int)n2);
                }
                if (isoChunk3 == null || (isoGridSquare = this.getGridSquare((int)9, (int)n, (int)n2)) == null || isoGridSquare.getObjects().isEmpty()) continue;
                isoChunk3.EnsureSurroundNotNullY((int)0, (int)n, (int)n2);
            }
            if (isoChunk5 != null && (isoGridSquare = this.getGridSquare((int)0, (int)0, (int)n2)) != null && !isoGridSquare.getObjects().isEmpty()) {
                isoChunk5.EnsureSurroundNotNull((int)9, (int)9, (int)n2);
            }
            if (isoChunk6 != null && (isoGridSquare = this.getGridSquare((int)9, (int)0, (int)n2)) != null && !isoGridSquare.getObjects().isEmpty()) {
                isoChunk6.EnsureSurroundNotNull((int)0, (int)9, (int)n2);
            }
            if (isoChunk7 != null && (isoGridSquare = this.getGridSquare((int)9, (int)9, (int)n2)) != null && !isoGridSquare.getObjects().isEmpty()) {
                isoChunk7.EnsureSurroundNotNull((int)0, (int)0, (int)n2);
            }
            if (isoChunk8 == null || (isoGridSquare = this.getGridSquare((int)0, (int)9, (int)n2)) == null || isoGridSquare.getObjects().isEmpty()) continue;
            isoChunk8.EnsureSurroundNotNull((int)9, (int)0, (int)n2);
        }
        for (n2 = 0; n2 <= this.maxLevel; ++n2) {
            for (n = 0; n < 10; ++n) {
                for (int i = 0; i < 10; ++i) {
                    Object object;
                    isoGridSquare = this.getGridSquare((int)i, (int)n, (int)n2);
                    if (isoGridSquare == null) continue;
                    if (i == 0 || i == 9 || n == 0 || n == 9) {
                        IsoWorld.instance.CurrentCell.DoGridNav((IsoGridSquare)isoGridSquare, (IsoGridSquare.GetSquare)IsoGridSquare.cellGetSquare);
                        for (int j = -1; j <= 1; ++j) {
                            if (i == 0) {
                                this.RecalcAllWithNeighbour((IsoGridSquare)isoGridSquare, (IsoDirections)IsoDirections.W, (int)j);
                                this.RecalcAllWithNeighbour((IsoGridSquare)isoGridSquare, (IsoDirections)IsoDirections.NW, (int)j);
                                this.RecalcAllWithNeighbour((IsoGridSquare)isoGridSquare, (IsoDirections)IsoDirections.SW, (int)j);
                            } else if (i == 9) {
                                this.RecalcAllWithNeighbour((IsoGridSquare)isoGridSquare, (IsoDirections)IsoDirections.E, (int)j);
                                this.RecalcAllWithNeighbour((IsoGridSquare)isoGridSquare, (IsoDirections)IsoDirections.NE, (int)j);
                                this.RecalcAllWithNeighbour((IsoGridSquare)isoGridSquare, (IsoDirections)IsoDirections.SE, (int)j);
                            }
                            if (n == 0) {
                                this.RecalcAllWithNeighbour((IsoGridSquare)isoGridSquare, (IsoDirections)IsoDirections.N, (int)j);
                                if (i != 0) {
                                    this.RecalcAllWithNeighbour((IsoGridSquare)isoGridSquare, (IsoDirections)IsoDirections.NW, (int)j);
                                }
                                if (i == 9) continue;
                                this.RecalcAllWithNeighbour((IsoGridSquare)isoGridSquare, (IsoDirections)IsoDirections.NE, (int)j);
                                continue;
                            }
                            if (n != 9) continue;
                            this.RecalcAllWithNeighbour((IsoGridSquare)isoGridSquare, (IsoDirections)IsoDirections.S, (int)j);
                            if (i != 0) {
                                this.RecalcAllWithNeighbour((IsoGridSquare)isoGridSquare, (IsoDirections)IsoDirections.SW, (int)j);
                            }
                            if (i == 9) continue;
                            this.RecalcAllWithNeighbour((IsoGridSquare)isoGridSquare, (IsoDirections)IsoDirections.SE, (int)j);
                        }
                        object = isoGridSquare.nav[IsoDirections.N.index()];
                        IsoGridSquare isoGridSquare2 = isoGridSquare.nav[IsoDirections.S.index()];
                        IsoGridSquare isoGridSquare3 = isoGridSquare.nav[IsoDirections.W.index()];
                        IsoGridSquare isoGridSquare4 = isoGridSquare.nav[IsoDirections.E.index()];
                        if (object != null && isoGridSquare3 != null && (i == 0 || n == 0)) {
                            this.RecalcAllWithNeighbour((IsoGridSquare)object, (IsoDirections)IsoDirections.W, (int)0);
                        }
                        if (object != null && isoGridSquare4 != null && (i == 9 || n == 0)) {
                            this.RecalcAllWithNeighbour((IsoGridSquare)object, (IsoDirections)IsoDirections.E, (int)0);
                        }
                        if (isoGridSquare2 != null && isoGridSquare3 != null && (i == 0 || n == 9)) {
                            this.RecalcAllWithNeighbour((IsoGridSquare)isoGridSquare2, (IsoDirections)IsoDirections.W, (int)0);
                        }
                        if (isoGridSquare2 != null && isoGridSquare4 != null && (i == 9 || n == 9)) {
                            this.RecalcAllWithNeighbour((IsoGridSquare)isoGridSquare2, (IsoDirections)IsoDirections.E, (int)0);
                        }
                    }
                    if ((object = isoGridSquare.getRoom()) == null) continue;
                    object.addSquare((IsoGridSquare)isoGridSquare);
                }
            }
        }
        this.fixObjectAmbientEmittersOnAdjacentChunks((IsoChunk)isoChunk3, (IsoChunk)isoChunk4);
        for (n2 = 0; n2 < 4; ++n2) {
            if (isoChunk != null) {
                isoChunk.lightCheck[n2] = true;
            }
            if (isoChunk2 != null) {
                isoChunk2.lightCheck[n2] = true;
            }
            if (isoChunk3 != null) {
                isoChunk3.lightCheck[n2] = true;
            }
            if (isoChunk4 != null) {
                isoChunk4.lightCheck[n2] = true;
            }
            if (isoChunk5 != null) {
                isoChunk5.lightCheck[n2] = true;
            }
            if (isoChunk6 != null) {
                isoChunk6.lightCheck[n2] = true;
            }
            if (isoChunk7 != null) {
                isoChunk7.lightCheck[n2] = true;
            }
            if (isoChunk8 == null) continue;
            isoChunk8.lightCheck[n2] = true;
        }
        for (n2 = 0; n2 < IsoPlayer.numPlayers; ++n2) {
            LosUtil.cachecleared[n2] = true;
        }
        IsoLightSwitch.chunkLoaded((IsoChunk)this);
    }

    private void fixObjectAmbientEmittersOnAdjacentChunks(IsoChunk isoChunk, IsoChunk isoChunk2) {
        if (GameServer.bServer) {
            return;
        }
        if (isoChunk == null && isoChunk2 == null) {
            return;
        }
        for (int i = 0; i < 8; ++i) {
            IsoGridSquare isoGridSquare;
            int n;
            if (isoChunk != null) {
                for (n = 0; n < 10; ++n) {
                    isoGridSquare = isoChunk.getGridSquare((int)0, (int)n, (int)i);
                    this.fixObjectAmbientEmittersOnSquare((IsoGridSquare)isoGridSquare, (boolean)false);
                }
            }
            if (isoChunk2 == null) continue;
            for (n = 0; n < 10; ++n) {
                isoGridSquare = isoChunk2.getGridSquare((int)n, (int)0, (int)i);
                this.fixObjectAmbientEmittersOnSquare((IsoGridSquare)isoGridSquare, (boolean)true);
            }
        }
    }

    private void fixObjectAmbientEmittersOnSquare(IsoGridSquare isoGridSquare, boolean bl) {
        IsoWindow isoWindow;
        if (isoGridSquare == null || isoGridSquare.getSpecialObjects().isEmpty()) {
            return;
        }
        IsoObject isoObject = isoGridSquare.getDoor((boolean)bl);
        if (isoObject instanceof IsoDoor && ((IsoDoor)isoObject).isExterior() && !isoObject.hasObjectAmbientEmitter()) {
            isoObject.addObjectAmbientEmitter((ObjectAmbientEmitters.PerObjectLogic)new ObjectAmbientEmitters.DoorLogic().init((IsoObject)isoObject));
        }
        if ((isoWindow = isoGridSquare.getWindow((boolean)bl)) != null && isoWindow.isExterior() && !isoWindow.hasObjectAmbientEmitter()) {
            isoWindow.addObjectAmbientEmitter((ObjectAmbientEmitters.PerObjectLogic)new ObjectAmbientEmitters.WindowLogic().init((IsoObject)isoWindow));
        }
    }

    @Deprecated
    public void recalcNeighboursNow() {
        IsoGridSquare isoGridSquare;
        int n;
        int n2;
        IsoCell isoCell = IsoWorld.instance.CurrentCell;
        for (n2 = 0; n2 < 10; ++n2) {
            for (n = 0; n < 10; ++n) {
                for (int i = 0; i < 8; ++i) {
                    isoGridSquare = this.getGridSquare((int)n2, (int)n, (int)i);
                    if (isoGridSquare == null) continue;
                    if (i > 0 && !isoGridSquare.getObjects().isEmpty()) {
                        isoGridSquare.EnsureSurroundNotNull();
                        for (int j = i - 1; j > 0; --j) {
                            IsoGridSquare isoGridSquare2 = this.getGridSquare((int)n2, (int)n, (int)j);
                            if (isoGridSquare2 != null) continue;
                            isoGridSquare2 = IsoGridSquare.getNew((IsoCell)isoCell, null, (int)(this.wx * 10 + n2), (int)(this.wy * 10 + n), (int)j);
                            isoCell.ConnectNewSquare((IsoGridSquare)isoGridSquare2, (boolean)false);
                        }
                    }
                    isoGridSquare.RecalcProperties();
                }
            }
        }
        for (n2 = 1; n2 < 8; ++n2) {
            IsoGridSquare isoGridSquare3;
            for (n = -1; n < 11; ++n) {
                isoGridSquare3 = isoCell.getGridSquare((int)(this.wx * 10 + n), (int)(this.wy * 10 - 1), (int)n2);
                if (isoGridSquare3 != null && !isoGridSquare3.getObjects().isEmpty()) {
                    isoGridSquare3.EnsureSurroundNotNull();
                }
                if ((isoGridSquare3 = isoCell.getGridSquare((int)(this.wx * 10 + n), (int)(this.wy * 10 + 10), (int)n2)) == null || isoGridSquare3.getObjects().isEmpty()) continue;
                isoGridSquare3.EnsureSurroundNotNull();
            }
            for (n = 0; n < 10; ++n) {
                isoGridSquare3 = isoCell.getGridSquare((int)(this.wx * 10 - 1), (int)(this.wy * 10 + n), (int)n2);
                if (isoGridSquare3 != null && !isoGridSquare3.getObjects().isEmpty()) {
                    isoGridSquare3.EnsureSurroundNotNull();
                }
                if ((isoGridSquare3 = isoCell.getGridSquare((int)(this.wx * 10 + 10), (int)(this.wy * 10 + n), (int)n2)) == null || isoGridSquare3.getObjects().isEmpty()) continue;
                isoGridSquare3.EnsureSurroundNotNull();
            }
        }
        for (n2 = 0; n2 < 10; ++n2) {
            for (n = 0; n < 10; ++n) {
                for (int i = 0; i < 8; ++i) {
                    isoGridSquare = this.getGridSquare((int)n2, (int)n, (int)i);
                    if (isoGridSquare == null) continue;
                    isoGridSquare.RecalcAllWithNeighbours((boolean)true);
                    IsoRoom isoRoom = isoGridSquare.getRoom();
                    if (isoRoom == null) continue;
                    isoRoom.addSquare((IsoGridSquare)isoGridSquare);
                }
            }
        }
        for (n2 = 0; n2 < 10; ++n2) {
            for (n = 0; n < 10; ++n) {
                for (int i = 0; i < 8; ++i) {
                    isoGridSquare = this.getGridSquare((int)n2, (int)n, (int)i);
                    if (isoGridSquare == null) continue;
                    isoGridSquare.propertiesDirty = true;
                }
            }
        }
        IsoLightSwitch.chunkLoaded((IsoChunk)this);
    }

    public void updateBuildings() {
    }

    public static void updatePlayerInBullet() {
        ArrayList arrayList = GameServer.getPlayers();
        Bullet.updatePlayerList((ArrayList)arrayList);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void update() {
        if (!GameServer.bServer) {
            this.checkPhysics();
        }
        if (!this.loadedPhysics) {
            this.loadedPhysics = true;
            for (int i = 0; i < this.vehicles.size(); ++i) {
                ((BaseVehicle)this.vehicles.get((int)i)).chunk = this;
            }
        }
        if (this.vehiclesForAddToWorld != null) {
            Object object = this.vehiclesForAddToWorldLock;
            synchronized (object) {
                for (int i = 0; i < this.vehiclesForAddToWorld.size(); ++i) {
                    ((BaseVehicle)this.vehiclesForAddToWorld.get((int)i)).addToWorld();
                }
                this.vehiclesForAddToWorld.clear();
                this.vehiclesForAddToWorld = null;
            }
        }
        this.updateVehicleStory();
    }

    public void updateVehicleStory() {
        if (!this.bLoaded || this.m_vehicleStorySpawnData == null) {
            return;
        }
        IsoMetaChunk isoMetaChunk = IsoWorld.instance.getMetaChunk((int)this.wx, (int)this.wy);
        if (isoMetaChunk == null) {
            return;
        }
        VehicleStorySpawnData vehicleStorySpawnData = this.m_vehicleStorySpawnData;
        for (int i = 0; i < isoMetaChunk.numZones(); ++i) {
            IsoMetaGrid.Zone zone = isoMetaChunk.getZone((int)i);
            if (!vehicleStorySpawnData.isValid((IsoMetaGrid.Zone)zone, (IsoChunk)this)) continue;
            vehicleStorySpawnData.m_story.randomizeVehicleStory((IsoMetaGrid.Zone)zone, (IsoChunk)this);
            ++zone.hourLastSeen;
            break;
        }
    }

    public void setSquare(int n, int n2, int n3, IsoGridSquare isoGridSquare) {
        if (!($assertionsDisabled || isoGridSquare == null || isoGridSquare.x - this.wx * 10 == n && isoGridSquare.y - this.wy * 10 == n2 && isoGridSquare.z == n3)) {
            throw new AssertionError();
        }
        this.squares[n3][n2 * 10 + n] = isoGridSquare;
        if (isoGridSquare != null) {
            isoGridSquare.chunk = this;
            if (isoGridSquare.z > this.maxLevel) {
                this.maxLevel = isoGridSquare.z;
            }
        }
    }

    public IsoGridSquare getGridSquare(int n, int n2, int n3) {
        if (n < 0 || n >= 10 || n2 < 0 || n2 >= 10 || n3 >= 8 || n3 < 0) {
            return null;
        }
        return this.squares[n3][n2 * 10 + n];
    }

    public IsoRoom getRoom(int n) {
        return this.lotheader.getRoom((int)n);
    }

    public void removeFromWorld() {
        int n;
        loadGridSquare.remove((Object)this);
        if (GameClient.bClient && GameClient.instance.bConnected) {
            try {
                GameClient.instance.sendAddedRemovedItems((boolean)true);
            }
            catch (Exception exception) {
                ExceptionLogger.logException((Throwable)exception);
            }
        }
        try {
            MapCollisionData.instance.removeChunkFromWorld((IsoChunk)this);
            ZombiePopulationManager.instance.removeChunkFromWorld((IsoChunk)this);
            PolygonalMap2.instance.removeChunkFromWorld((IsoChunk)this);
            this.collision.clear();
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        int n2 = 100;
        for (n = 0; n < 8; ++n) {
            for (int i = 0; i < n2; ++i) {
                IsoObject isoObject;
                int n3;
                IsoGridSquare isoGridSquare = this.squares[n][i];
                if (isoGridSquare == null) continue;
                RainManager.RemoveAllOn((IsoGridSquare)isoGridSquare);
                isoGridSquare.clearWater();
                isoGridSquare.clearPuddles();
                if (isoGridSquare.getRoom() != null) {
                    isoGridSquare.getRoom().removeSquare((IsoGridSquare)isoGridSquare);
                }
                if (isoGridSquare.zone != null) {
                    isoGridSquare.zone.removeSquare((IsoGridSquare)isoGridSquare);
                }
                ArrayList arrayList = isoGridSquare.getMovingObjects();
                for (n3 = 0; n3 < arrayList.size(); ++n3) {
                    isoObject = (IsoMovingObject)arrayList.get((int)n3);
                    if (isoObject instanceof IsoSurvivor) {
                        IsoWorld.instance.CurrentCell.getSurvivorList().remove((Object)isoObject);
                        ((IsoMovingObject)isoObject).Despawn();
                    }
                    ((IsoMovingObject)isoObject).removeFromWorld();
                    ((IsoMovingObject)isoObject).last = null;
                    ((IsoMovingObject)isoObject).current = null;
                    if (arrayList.contains((Object)isoObject)) continue;
                    --n3;
                }
                arrayList.clear();
                for (n3 = 0; n3 < isoGridSquare.getObjects().size(); ++n3) {
                    isoObject = (IsoObject)isoGridSquare.getObjects().get((int)n3);
                    isoObject.removeFromWorld();
                }
                for (n3 = 0; n3 < isoGridSquare.getStaticMovingObjects().size(); ++n3) {
                    isoObject = (IsoMovingObject)isoGridSquare.getStaticMovingObjects().get((int)n3);
                    ((IsoMovingObject)isoObject).removeFromWorld();
                }
                this.disconnectFromAdjacentChunks((IsoGridSquare)isoGridSquare);
                isoGridSquare.softClear();
                isoGridSquare.chunk = null;
            }
        }
        for (n = 0; n < this.vehicles.size(); ++n) {
            BaseVehicle baseVehicle = (BaseVehicle)this.vehicles.get((int)n);
            if (!IsoWorld.instance.CurrentCell.getVehicles().contains((Object)baseVehicle) && !IsoWorld.instance.CurrentCell.addVehicles.contains((Object)baseVehicle)) continue;
            DebugLog.log((String)("IsoChunk.removeFromWorld: vehicle wasn't removed from world id=" + baseVehicle.VehicleID));
            baseVehicle.removeFromWorld();
        }
    }

    private void disconnectFromAdjacentChunks(IsoGridSquare isoGridSquare) {
        int n = isoGridSquare.x % 10;
        int n2 = isoGridSquare.y % 10;
        if (n != 0 && n != 9 && n2 != 0 && n2 != 9) {
            return;
        }
        int n3 = IsoDirections.N.index();
        int n4 = IsoDirections.S.index();
        if (isoGridSquare.nav[n3] != null && isoGridSquare.nav[n3].chunk != isoGridSquare.chunk) {
            isoGridSquare.nav[n3].nav[n4] = null;
            isoGridSquare.nav[n3].s = null;
        }
        n3 = IsoDirections.NW.index();
        n4 = IsoDirections.SE.index();
        if (isoGridSquare.nav[n3] != null && isoGridSquare.nav[n3].chunk != isoGridSquare.chunk) {
            isoGridSquare.nav[n3].nav[n4] = null;
            isoGridSquare.nav[n3].se = null;
        }
        n3 = IsoDirections.W.index();
        n4 = IsoDirections.E.index();
        if (isoGridSquare.nav[n3] != null && isoGridSquare.nav[n3].chunk != isoGridSquare.chunk) {
            isoGridSquare.nav[n3].nav[n4] = null;
            isoGridSquare.nav[n3].e = null;
        }
        n3 = IsoDirections.SW.index();
        n4 = IsoDirections.NE.index();
        if (isoGridSquare.nav[n3] != null && isoGridSquare.nav[n3].chunk != isoGridSquare.chunk) {
            isoGridSquare.nav[n3].nav[n4] = null;
            isoGridSquare.nav[n3].ne = null;
        }
        n3 = IsoDirections.S.index();
        n4 = IsoDirections.N.index();
        if (isoGridSquare.nav[n3] != null && isoGridSquare.nav[n3].chunk != isoGridSquare.chunk) {
            isoGridSquare.nav[n3].nav[n4] = null;
            isoGridSquare.nav[n3].n = null;
        }
        n3 = IsoDirections.SE.index();
        n4 = IsoDirections.NW.index();
        if (isoGridSquare.nav[n3] != null && isoGridSquare.nav[n3].chunk != isoGridSquare.chunk) {
            isoGridSquare.nav[n3].nav[n4] = null;
            isoGridSquare.nav[n3].nw = null;
        }
        n3 = IsoDirections.E.index();
        n4 = IsoDirections.W.index();
        if (isoGridSquare.nav[n3] != null && isoGridSquare.nav[n3].chunk != isoGridSquare.chunk) {
            isoGridSquare.nav[n3].nav[n4] = null;
            isoGridSquare.nav[n3].w = null;
        }
        n3 = IsoDirections.NE.index();
        n4 = IsoDirections.SW.index();
        if (isoGridSquare.nav[n3] != null && isoGridSquare.nav[n3].chunk != isoGridSquare.chunk) {
            isoGridSquare.nav[n3].nav[n4] = null;
            isoGridSquare.nav[n3].sw = null;
        }
    }

    public void doReuseGridsquares() {
        int n = 100;
        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < n; ++j) {
                IsoGridSquare isoGridSquare = this.squares[i][j];
                if (isoGridSquare == null) continue;
                LuaEventManager.triggerEvent((String)"ReuseGridsquare", (Object)isoGridSquare);
                for (int k = 0; k < isoGridSquare.getObjects().size(); ++k) {
                    IsoObject isoObject = (IsoObject)isoGridSquare.getObjects().get((int)k);
                    if (isoObject instanceof IsoTree) {
                        isoObject.reset();
                        CellLoader.isoTreeCache.add((Object)((IsoTree)isoObject));
                        continue;
                    }
                    if (isoObject instanceof IsoObject && isoObject.getObjectName().equals((Object)"IsoObject")) {
                        isoObject.reset();
                        CellLoader.isoObjectCache.add((Object)isoObject);
                        continue;
                    }
                    isoObject.reuseGridSquare();
                }
                isoGridSquare.discard();
                this.squares[i][j] = null;
            }
        }
        this.resetForStore();
        if (!$assertionsDisabled && IsoChunkMap.chunkStore.contains((Object)this)) {
            throw new AssertionError();
        }
        IsoChunkMap.chunkStore.add((Object)this);
    }

    private static int bufferSize(int n) {
        return (n + 65536 - 1) / 65536 * 65536;
    }

    private static ByteBuffer ensureCapacity(ByteBuffer byteBuffer, int n) {
        if (byteBuffer == null || byteBuffer.capacity() < n) {
            byteBuffer = ByteBuffer.allocate((int)IsoChunk.bufferSize((int)n));
        }
        return byteBuffer;
    }

    private static ByteBuffer ensureCapacity(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return ByteBuffer.allocate((int)65536);
        }
        if (byteBuffer.capacity() - byteBuffer.position() < 65536) {
            ByteBuffer byteBuffer2 = IsoChunk.ensureCapacity(null, (int)(byteBuffer.position() + 65536));
            return byteBuffer2.put((byte[])byteBuffer.array(), (int)0, (int)byteBuffer.position());
        }
        return byteBuffer;
    }

    public void LoadFromDisk() throws IOException {
        this.LoadFromDiskOrBuffer(null);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void LoadFromDiskOrBuffer(ByteBuffer byteBuffer) throws IOException {
        sanityCheck.beginLoad((IsoChunk)this);
        try {
            byte by;
            int n;
            int n2;
            int n3;
            int n4;
            ByteBuffer byteBuffer2 = byteBuffer == null ? (SliceBufferLoad = IsoChunk.SafeRead((String)prefix, (int)this.wx, (int)this.wy, (ByteBuffer)SliceBufferLoad)) : byteBuffer;
            int n5 = this.wx * 10;
            int n6 = this.wy * 10;
            String string = ChunkMapFilenames.instance.getHeader((int)(n5 /= 300), (int)(n6 /= 300));
            if (IsoLot.InfoHeaders.containsKey((Object)string)) {
                this.lotheader = (LotHeader)IsoLot.InfoHeaders.get((Object)string);
            }
            IsoCell.wx = this.wx;
            IsoCell.wy = this.wy;
            boolean bl = byteBuffer2.get() == 1;
            int n7 = byteBuffer2.getInt();
            if (bl) {
                DebugLog.log((String)("WorldVersion = " + n7 + ", debug = " + bl));
            }
            if (n7 > 195) {
                throw new RuntimeException((String)("unknown world version " + n7 + " while reading chunk " + this.wx + "," + this.wy));
            }
            boolean bl2 = this.bFixed2x = n7 >= 85;
            if (n7 >= 61) {
                n4 = byteBuffer2.getInt();
                sanityCheck.checkLength((long)((long)n4), (long)((long)byteBuffer2.limit()));
                long l = byteBuffer2.getLong();
                crcLoad.reset();
                crcLoad.update((byte[])byteBuffer2.array(), (int)17, (int)(byteBuffer2.limit() - 1 - 4 - 4 - 8));
                sanityCheck.checkCRC((long)l, (long)crcLoad.getValue());
            }
            n4 = 0;
            if (GameClient.bClient || GameServer.bServer) {
                n4 = ServerOptions.getInstance().BloodSplatLifespanDays.getValue();
            }
            float f = (float)GameTime.getInstance().getWorldAgeHours();
            int n8 = byteBuffer2.getInt();
            for (int i = 0; i < n8; ++i) {
                IsoFloorBloodSplat isoFloorBloodSplat = new IsoFloorBloodSplat();
                isoFloorBloodSplat.load((ByteBuffer)byteBuffer2, (int)n7);
                if (isoFloorBloodSplat.worldAge > f) {
                    isoFloorBloodSplat.worldAge = f;
                }
                if (n4 > 0 && f - isoFloorBloodSplat.worldAge >= (float)(n4 * 24)) continue;
                if (n7 < 73 && isoFloorBloodSplat.Type < 8) {
                    isoFloorBloodSplat.index = ++this.nextSplatIndex;
                }
                if (isoFloorBloodSplat.Type < 8) {
                    this.nextSplatIndex = isoFloorBloodSplat.index % 10;
                }
                this.FloorBloodSplats.add((Object)isoFloorBloodSplat);
            }
            IsoMetaGrid isoMetaGrid = IsoWorld.instance.getMetaGrid();
            byte by2 = 0;
            for (n3 = 0; n3 < 10; ++n3) {
                for (n2 = 0; n2 < 10; ++n2) {
                    by2 = byteBuffer2.get();
                    for (n = 0; n < 8; ++n) {
                        IsoGridSquare isoGridSquare = null;
                        by = 0;
                        if ((by2 & 1 << n) != 0) {
                            by = 1;
                        }
                        if (by == 1) {
                            if (isoGridSquare == null) {
                                isoGridSquare = IsoGridSquare.loadGridSquareCache != null ? IsoGridSquare.getNew(IsoGridSquare.loadGridSquareCache, (IsoCell)IsoWorld.instance.CurrentCell, null, (int)(n3 + this.wx * 10), (int)(n2 + this.wy * 10), (int)n) : IsoGridSquare.getNew((IsoCell)IsoWorld.instance.CurrentCell, null, (int)(n3 + this.wx * 10), (int)(n2 + this.wy * 10), (int)n);
                            }
                            isoGridSquare.chunk = this;
                            if (this.lotheader != null) {
                                RoomDef roomDef = isoMetaGrid.getRoomAt((int)isoGridSquare.x, (int)isoGridSquare.y, (int)isoGridSquare.z);
                                int n9 = roomDef != null ? roomDef.ID : -1;
                                isoGridSquare.setRoomID((int)n9);
                                roomDef = isoMetaGrid.getEmptyOutsideAt((int)isoGridSquare.x, (int)isoGridSquare.y, (int)isoGridSquare.z);
                                if (roomDef != null) {
                                    IsoRoom isoRoom = this.getRoom((int)roomDef.ID);
                                    isoGridSquare.roofHideBuilding = isoRoom == null ? null : isoRoom.building;
                                }
                            }
                            isoGridSquare.ResetIsoWorldRegion();
                            this.setSquare((int)n3, (int)n2, (int)n, (IsoGridSquare)isoGridSquare);
                        }
                        if (by != 1 || isoGridSquare == null) continue;
                        isoGridSquare.load((ByteBuffer)byteBuffer2, (int)n7, (boolean)bl);
                        isoGridSquare.FixStackableObjects();
                        if (this.jobType != JobType.SoftReset) continue;
                        if (!isoGridSquare.getStaticMovingObjects().isEmpty()) {
                            isoGridSquare.getStaticMovingObjects().clear();
                        }
                        for (int i = 0; i < isoGridSquare.getObjects().size(); ++i) {
                            IsoObject isoObject = (IsoObject)isoGridSquare.getObjects().get((int)i);
                            isoObject.softReset();
                            if (isoObject.getObjectIndex() != -1) continue;
                            --i;
                        }
                        isoGridSquare.setOverlayDone((boolean)false);
                    }
                }
            }
            if (n7 >= 45) {
                this.getErosionData().load((ByteBuffer)byteBuffer2, (int)n7);
                this.getErosionData().set((IsoChunk)this);
            }
            if (n7 >= 127) {
                n3 = (int)byteBuffer2.getShort();
                if (n3 > 0 && this.generatorsTouchingThisChunk == null) {
                    this.generatorsTouchingThisChunk = new ArrayList();
                }
                if (this.generatorsTouchingThisChunk != null) {
                    this.generatorsTouchingThisChunk.clear();
                }
                for (n2 = 0; n2 < n3; ++n2) {
                    n = byteBuffer2.getInt();
                    int n10 = byteBuffer2.getInt();
                    by = byteBuffer2.get();
                    IsoGameCharacter.Location location = new IsoGameCharacter.Location((int)n, (int)n10, (int)by);
                    this.generatorsTouchingThisChunk.add((Object)location);
                }
            }
            this.vehicles.clear();
            if (!GameClient.bClient) {
                if (n7 >= 91) {
                    n3 = (int)byteBuffer2.getShort();
                    for (n2 = 0; n2 < n3; ++n2) {
                        IsoGridSquare isoGridSquare;
                        n = (int)byteBuffer2.get();
                        byte by3 = byteBuffer2.get();
                        by = byteBuffer2.get();
                        IsoObject isoObject = IsoObject.factoryFromFileInput((IsoCell)IsoWorld.instance.CurrentCell, (ByteBuffer)byteBuffer2);
                        if (isoObject == null || !(isoObject instanceof BaseVehicle)) continue;
                        isoObject.square = isoGridSquare = this.getGridSquare((int)n, (int)by3, (int)by);
                        ((IsoMovingObject)isoObject).current = isoGridSquare;
                        try {
                            isoObject.load((ByteBuffer)byteBuffer2, (int)n7, (boolean)bl);
                            this.vehicles.add((Object)((BaseVehicle)isoObject));
                            IsoChunk.addFromCheckedVehicles((BaseVehicle)((BaseVehicle)isoObject));
                            if (this.jobType != JobType.SoftReset) continue;
                            isoObject.softReset();
                            continue;
                        }
                        catch (Exception exception) {
                            throw new RuntimeException((Throwable)exception);
                        }
                    }
                }
                if (n7 >= 125) {
                    this.lootRespawnHour = byteBuffer2.getInt();
                }
                if (n7 >= 160) {
                    n3 = (int)byteBuffer2.get();
                    for (n2 = 0; n2 < n3; ++n2) {
                        n = byteBuffer2.getInt();
                        this.addSpawnedRoom((int)n);
                    }
                }
            }
        }
        catch (Throwable throwable) {
            sanityCheck.endLoad((IsoChunk)this);
            this.bFixed2x = true;
            throw throwable;
        }
        sanityCheck.endLoad((IsoChunk)this);
        this.bFixed2x = true;
        if (this.getGridSquare((int)0, (int)0, (int)0) == null && this.getGridSquare((int)9, (int)9, (int)0) == null) {
            throw new RuntimeException((String)("black chunk " + this.wx + "," + this.wy));
        }
    }

    public void doLoadGridsquare() {
        int n;
        if (this.jobType == JobType.SoftReset) {
            this.m_spawnedRooms.clear();
        }
        if (!GameServer.bServer) {
            this.loadInMainThread();
        }
        if (this.addZombies && !VehiclesDB2.instance.isChunkSeen((int)this.wx, (int)this.wy)) {
            try {
                this.AddVehicles();
            }
            catch (Throwable throwable) {
                ExceptionLogger.logException((Throwable)throwable);
            }
        }
        this.AddZombieZoneStory();
        VehiclesDB2.instance.setChunkSeen((int)this.wx, (int)this.wy);
        if (this.addZombies) {
            if (IsoWorld.instance.getTimeSinceLastSurvivorInHorde() > 0) {
                IsoWorld.instance.setTimeSinceLastSurvivorInHorde((int)(IsoWorld.instance.getTimeSinceLastSurvivorInHorde() - 1));
            }
            this.addSurvivorInHorde((boolean)false);
        }
        this.update();
        if (!GameServer.bServer) {
            FliesSound.instance.chunkLoaded((IsoChunk)this);
            NearestWalls.chunkLoaded((IsoChunk)this);
        }
        if (this.addZombies) {
            int n2 = 5 + SandboxOptions.instance.TimeSinceApo.getValue();
            if (Rand.Next((int)(n2 = Math.min((int)20, (int)n2))) == 0) {
                this.AddCorpses((int)this.wx, (int)this.wy);
            }
            if (Rand.Next((int)(n2 * 2)) == 0) {
                this.AddBlood((int)this.wx, (int)this.wy);
            }
        }
        LoadGridsquarePerformanceWorkaround.init((int)this.wx, (int)this.wy);
        tempBuildings.clear();
        if (!GameClient.bClient) {
            for (int i = 0; i < this.vehicles.size(); ++i) {
                BaseVehicle baseVehicle = (BaseVehicle)this.vehicles.get((int)i);
                if (!baseVehicle.addedToWorld && VehiclesDB2.instance.isVehicleLoaded((BaseVehicle)baseVehicle)) {
                    baseVehicle.removeFromSquare();
                    this.vehicles.remove((int)i);
                    --i;
                    continue;
                }
                if (!baseVehicle.addedToWorld) {
                    baseVehicle.addToWorld();
                }
                if (baseVehicle.sqlID != -1) continue;
                if (!$assertionsDisabled) {
                    throw new AssertionError();
                }
                if (baseVehicle.square == null) {
                    float f = 5.0E-4f;
                    int n3 = this.wx * 10;
                    n = this.wy * 10;
                    int n4 = n3 + 10;
                    int n5 = n + 10;
                    float f2 = PZMath.clamp((float)baseVehicle.x, (float)((float)n3 + f), (float)((float)n4 - f));
                    float f3 = PZMath.clamp((float)baseVehicle.y, (float)((float)n + f), (float)((float)n5 - f));
                    baseVehicle.square = this.getGridSquare((int)((int)f2 - this.wx * 10), (int)((int)f3 - this.wy * 10), (int)0);
                }
                VehiclesDB2.instance.addVehicle((BaseVehicle)baseVehicle);
            }
        }
        this.m_treeCount = 0;
        this.m_scavengeZone = null;
        this.m_numberOfWaterTiles = 0;
        for (int i = 0; i <= this.maxLevel; ++i) {
            for (int j = 0; j < 10; ++j) {
                for (int k = 0; k < 10; ++k) {
                    IsoObject isoObject;
                    IsoGridSquare isoGridSquare = this.getGridSquare((int)j, (int)k, (int)i);
                    if (isoGridSquare != null && !isoGridSquare.getObjects().isEmpty()) {
                        for (n = 0; n < isoGridSquare.getObjects().size(); ++n) {
                            isoObject = (IsoObject)isoGridSquare.getObjects().get((int)n);
                            isoObject.addToWorld();
                            if (i != 0 || isoObject.getSprite() == null || !isoObject.getSprite().getProperties().Is((IsoFlagType)IsoFlagType.water)) continue;
                            ++this.m_numberOfWaterTiles;
                        }
                        if (isoGridSquare.HasTree()) {
                            ++this.m_treeCount;
                        }
                        if (this.jobType != JobType.SoftReset) {
                            ErosionMain.LoadGridsquare((IsoGridSquare)isoGridSquare);
                        }
                        if (this.addZombies) {
                            MapObjects.newGridSquare((IsoGridSquare)isoGridSquare);
                        }
                        MapObjects.loadGridSquare((IsoGridSquare)isoGridSquare);
                        LuaEventManager.triggerEvent((String)"LoadGridsquare", (Object)isoGridSquare);
                        LoadGridsquarePerformanceWorkaround.LoadGridsquare((IsoGridSquare)isoGridSquare);
                    }
                    if (isoGridSquare != null && !isoGridSquare.getStaticMovingObjects().isEmpty()) {
                        for (n = 0; n < isoGridSquare.getStaticMovingObjects().size(); ++n) {
                            isoObject = (IsoMovingObject)isoGridSquare.getStaticMovingObjects().get((int)n);
                            isoObject.addToWorld();
                        }
                    }
                    if (isoGridSquare == null || isoGridSquare.getBuilding() == null || tempBuildings.contains((Object)isoGridSquare.getBuilding())) continue;
                    tempBuildings.add((Object)isoGridSquare.getBuilding());
                }
            }
        }
        if (this.jobType != JobType.SoftReset) {
            ErosionMain.ChunkLoaded((IsoChunk)this);
        }
        if (this.jobType != JobType.SoftReset) {
            SGlobalObjects.chunkLoaded((int)this.wx, (int)this.wy);
        }
        ReanimatedPlayers.instance.addReanimatedPlayersToChunk((IsoChunk)this);
        if (this.jobType != JobType.SoftReset) {
            MapCollisionData.instance.addChunkToWorld((IsoChunk)this);
            ZombiePopulationManager.instance.addChunkToWorld((IsoChunk)this);
            PolygonalMap2.instance.addChunkToWorld((IsoChunk)this);
            IsoGenerator.chunkLoaded((IsoChunk)this);
            LootRespawn.chunkLoaded((IsoChunk)this);
        }
        if (!GameServer.bServer) {
            ArrayList<IsoRoomLight> arrayList = IsoWorld.instance.CurrentCell.roomLights;
            for (int i = 0; i < this.roomLights.size(); ++i) {
                IsoRoomLight isoRoomLight = (IsoRoomLight)this.roomLights.get((int)i);
                if (arrayList.contains((Object)isoRoomLight)) continue;
                arrayList.add((Object)isoRoomLight);
            }
        }
        this.roomLights.clear();
        if (this.jobType != JobType.SoftReset) {
            this.randomizeBuildingsEtc();
        }
        this.checkAdjacentChunks();
        try {
            if (GameServer.bServer && this.jobType != JobType.SoftReset) {
                for (int i = 0; i < GameServer.udpEngine.connections.size(); ++i) {
                    UdpConnection udpConnection = (UdpConnection)GameServer.udpEngine.connections.get((int)i);
                    if (udpConnection.chunkObjectState.isEmpty()) continue;
                    for (int j = 0; j < udpConnection.chunkObjectState.size(); j += 2) {
                        short s = udpConnection.chunkObjectState.get((int)j);
                        n = (int)udpConnection.chunkObjectState.get((int)(j + 1));
                        if (s != this.wx || n != this.wy) continue;
                        udpConnection.chunkObjectState.remove((int)j, (int)2);
                        j -= 2;
                        ByteBufferWriter byteBufferWriter = udpConnection.startPacket();
                        PacketTypes.PacketType.ChunkObjectState.doPacket((ByteBufferWriter)byteBufferWriter);
                        byteBufferWriter.putShort((short)((short)this.wx));
                        byteBufferWriter.putShort((short)((short)this.wy));
                        try {
                            if (this.saveObjectState((ByteBuffer)byteBufferWriter.bb)) {
                                PacketTypes.PacketType.ChunkObjectState.send((UdpConnection)udpConnection);
                                continue;
                            }
                            udpConnection.cancelPacket();
                            continue;
                        }
                        catch (Throwable throwable) {
                            throwable.printStackTrace();
                            udpConnection.cancelPacket();
                        }
                    }
                }
            }
            if (GameClient.bClient) {
                ByteBufferWriter byteBufferWriter = GameClient.connection.startPacket();
                PacketTypes.PacketType.ChunkObjectState.doPacket((ByteBufferWriter)byteBufferWriter);
                byteBufferWriter.putShort((short)((short)this.wx));
                byteBufferWriter.putShort((short)((short)this.wy));
                PacketTypes.PacketType.ChunkObjectState.send((UdpConnection)GameClient.connection);
            }
        }
        catch (Throwable throwable) {
            ExceptionLogger.logException((Throwable)throwable);
        }
    }

    private void randomizeBuildingsEtc() {
        IsoRoom isoRoom;
        int n;
        tempRoomDefs.clear();
        IsoWorld.instance.MetaGrid.getRoomsIntersecting((int)(this.wx * 10 - 1), (int)(this.wy * 10 - 1), (int)11, (int)11, tempRoomDefs);
        for (n = 0; n < tempRoomDefs.size(); ++n) {
            IsoBuilding isoBuilding;
            isoRoom = ((RoomDef)tempRoomDefs.get((int)n)).getIsoRoom();
            if (isoRoom == null || tempBuildings.contains((Object)(isoBuilding = isoRoom.getBuilding()))) continue;
            tempBuildings.add((Object)isoBuilding);
        }
        for (n = 0; n < tempBuildings.size(); ++n) {
            isoRoom = (IsoBuilding)tempBuildings.get((int)n);
            if (!GameClient.bClient && isoRoom.def != null && isoRoom.def.isFullyStreamedIn()) {
                StashSystem.doBuildingStash((BuildingDef)isoRoom.def);
            }
            RandomizedBuildingBase.ChunkLoaded((IsoBuilding)isoRoom);
        }
        if (!GameClient.bClient && !tempBuildings.isEmpty()) {
            for (n = 0; n < tempBuildings.size(); ++n) {
                isoRoom = (IsoBuilding)tempBuildings.get((int)n);
                for (int i = 0; i < isoRoom.Rooms.size(); ++i) {
                    IsoRoom isoRoom2 = (IsoRoom)isoRoom.Rooms.get((int)i);
                    if (!isoRoom2.def.bDoneSpawn || this.isSpawnedRoom((int)isoRoom2.def.ID) || !isoRoom2.def.intersects((int)(this.wx * 10), (int)(this.wy * 10), (int)10, (int)10)) continue;
                    this.addSpawnedRoom((int)isoRoom2.def.ID);
                    VirtualZombieManager.instance.addIndoorZombiesToChunk((IsoChunk)this, (IsoRoom)isoRoom2);
                }
            }
        }
    }

    private void checkAdjacentChunks() {
        IsoCell isoCell = IsoWorld.instance.CurrentCell;
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                IsoChunk isoChunk;
                if (j == 0 && i == 0 || (isoChunk = isoCell.getChunk((int)(this.wx + j), (int)(this.wy + i))) == null) continue;
                ++isoChunk.m_adjacentChunkLoadedCounter;
            }
        }
    }

    private void AddZombieZoneStory() {
        IsoMetaChunk isoMetaChunk = IsoWorld.instance.getMetaChunk((int)this.wx, (int)this.wy);
        if (isoMetaChunk == null) {
            return;
        }
        for (int i = 0; i < isoMetaChunk.numZones(); ++i) {
            IsoMetaGrid.Zone zone = isoMetaChunk.getZone((int)i);
            RandomizedZoneStoryBase.isValidForStory((IsoMetaGrid.Zone)zone, (boolean)false);
        }
    }

    public void setCache() {
        IsoWorld.instance.CurrentCell.setCacheChunk((IsoChunk)this);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static ChunkLock acquireLock(int n, int n2) {
        ArrayList<ChunkLock> arrayList = Locks;
        synchronized (arrayList) {
            for (int i = 0; i < Locks.size(); ++i) {
                if (((ChunkLock)IsoChunk.Locks.get((int)i)).wx != n || ((ChunkLock)IsoChunk.Locks.get((int)i)).wy != n2) continue;
                return ((ChunkLock)Locks.get((int)i)).ref();
            }
            ChunkLock chunkLock = FreeLocks.isEmpty() ? new ChunkLock((int)n, (int)n2) : ((ChunkLock)FreeLocks.pop()).set((int)n, (int)n2);
            Locks.add((Object)chunkLock);
            return chunkLock.ref();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void releaseLock(ChunkLock chunkLock) {
        ArrayList<ChunkLock> arrayList = Locks;
        synchronized (arrayList) {
            if (chunkLock.deref() == 0) {
                Locks.remove((Object)chunkLock);
                FreeLocks.push((Object)chunkLock);
            }
        }
    }

    public void setCacheIncludingNull() {
        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 10; ++j) {
                for (int k = 0; k < 10; ++k) {
                    IsoGridSquare isoGridSquare = this.getGridSquare((int)j, (int)k, (int)i);
                    IsoWorld.instance.CurrentCell.setCacheGridSquare((int)(this.wx * 10 + j), (int)(this.wy * 10 + k), (int)i, (IsoGridSquare)isoGridSquare);
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     */
    public void Save(boolean var1_1) throws IOException {
        if (Core.getInstance().isNoSave() || GameClient.bClient) {
            if (!var1_1 && !GameServer.bServer && this.jobType != JobType.Convert) {
                WorldReuserThread.instance.addReuseChunk((IsoChunk)this);
            }
            return;
        }
        var2_2 = IsoChunk.WriteLock;
        synchronized (var2_2) {
            IsoChunk.sanityCheck.beginSave((IsoChunk)this);
            try {
                var3_3 = ChunkMapFilenames.instance.getDir((String)Core.GameSaveWorld);
                if (!var3_3.exists()) {
                    var3_3.mkdir();
                }
                IsoChunk.SliceBuffer = this.Save((ByteBuffer)IsoChunk.SliceBuffer, (CRC32)IsoChunk.crcSave);
                if (GameClient.bClient || GameServer.bServer) {
                    var4_4 = ChunkChecksum.getChecksumIfExists((int)this.wx, (int)this.wy);
                    IsoChunk.crcSave.reset();
                    IsoChunk.crcSave.update((byte[])IsoChunk.SliceBuffer.array(), (int)0, (int)IsoChunk.SliceBuffer.position());
                    if (var4_4 != IsoChunk.crcSave.getValue()) {
                        ChunkChecksum.setChecksum((int)this.wx, (int)this.wy, (long)IsoChunk.crcSave.getValue());
                        IsoChunk.SafeWrite((String)IsoChunk.prefix, (int)this.wx, (int)this.wy, (ByteBuffer)IsoChunk.SliceBuffer);
                    }
                } else {
                    IsoChunk.SafeWrite((String)IsoChunk.prefix, (int)this.wx, (int)this.wy, (ByteBuffer)IsoChunk.SliceBuffer);
                }
                if (var1_1 || GameServer.bServer) ** GOTO lbl33
                if (this.jobType == JobType.Convert) ** GOTO lbl28
                WorldReuserThread.instance.addReuseChunk((IsoChunk)this);
                ** GOTO lbl33
lbl28:
                // 1 sources

                this.doReuseGridsquares();
            }
            catch (Throwable var6_5) {
                IsoChunk.sanityCheck.endSave((IsoChunk)this);
                throw var6_5;
            }
lbl33:
            // 3 sources

            IsoChunk.sanityCheck.endSave((IsoChunk)this);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void SafeWrite(String string, int n, int n2, ByteBuffer byteBuffer) throws IOException {
        ChunkLock chunkLock = IsoChunk.acquireLock((int)n, (int)n2);
        chunkLock.lockForWriting();
        try {
            File file = ChunkMapFilenames.instance.getFilename((int)n, (int)n2);
            sanityCheck.beginSaveFile((String)file.getAbsolutePath());
            try {
                FileOutputStream fileOutputStream = new FileOutputStream((File)file);
                try {
                    fileOutputStream.getChannel().truncate((long)0L);
                    fileOutputStream.write((byte[])byteBuffer.array(), (int)0, (int)byteBuffer.position());
                }
                catch (Throwable throwable) {
                    try {
                        fileOutputStream.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed((Throwable)throwable2);
                    }
                    throw throwable;
                }
                fileOutputStream.close();
            }
            catch (Throwable throwable) {
                sanityCheck.endSaveFile();
                throw throwable;
            }
            sanityCheck.endSaveFile();
        }
        catch (Throwable throwable) {
            chunkLock.unlockForWriting();
            IsoChunk.releaseLock((ChunkLock)chunkLock);
            throw throwable;
        }
        chunkLock.unlockForWriting();
        IsoChunk.releaseLock((ChunkLock)chunkLock);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static ByteBuffer SafeRead(String string, int n, int n2, ByteBuffer byteBuffer) throws IOException {
        ChunkLock chunkLock = IsoChunk.acquireLock((int)n, (int)n2);
        chunkLock.lockForReading();
        try {
            File file = ChunkMapFilenames.instance.getFilename((int)n, (int)n2);
            if (file == null) {
                file = ZomboidFileSystem.instance.getFileInCurrentSave((String)(string + n + "_" + n2 + ".bin"));
            }
            sanityCheck.beginLoadFile((String)file.getAbsolutePath());
            try {
                FileInputStream fileInputStream = new FileInputStream((File)file);
                try {
                    byteBuffer = IsoChunk.ensureCapacity((ByteBuffer)byteBuffer, (int)((int)file.length()));
                    byteBuffer.clear();
                    int n3 = fileInputStream.read((byte[])byteBuffer.array());
                    byteBuffer.limit((int)PZMath.max((int)n3, (int)0));
                }
                catch (Throwable throwable) {
                    try {
                        fileInputStream.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed((Throwable)throwable2);
                    }
                    throw throwable;
                }
                fileInputStream.close();
            }
            catch (Throwable throwable) {
                sanityCheck.endLoadFile((String)file.getAbsolutePath());
                throw throwable;
            }
            sanityCheck.endLoadFile((String)file.getAbsolutePath());
        }
        catch (Throwable throwable) {
            chunkLock.unlockForReading();
            IsoChunk.releaseLock((ChunkLock)chunkLock);
            throw throwable;
        }
        chunkLock.unlockForReading();
        IsoChunk.releaseLock((ChunkLock)chunkLock);
        return byteBuffer;
    }

    public void SaveLoadedChunk(ClientChunkRequest.Chunk chunk, CRC32 cRC32) throws IOException {
        chunk.bb = this.Save((ByteBuffer)chunk.bb, (CRC32)cRC32);
    }

    public static boolean IsDebugSave() {
        if (!Core.bDebug) {
            return false;
        }
        return false;
    }

    public ByteBuffer Save(ByteBuffer byteBuffer, CRC32 cRC32) throws IOException {
        int n;
        int n2;
        byteBuffer.rewind();
        byteBuffer = IsoChunk.ensureCapacity((ByteBuffer)byteBuffer);
        byteBuffer.clear();
        byteBuffer.put((byte)(IsoChunk.IsDebugSave() ? (byte)1 : 0));
        byteBuffer.putInt((int)195);
        byteBuffer.putInt((int)0);
        byteBuffer.putLong((long)0L);
        int n3 = Math.min((int)1000, (int)this.FloorBloodSplats.size());
        int n4 = this.FloorBloodSplats.size() - n3;
        byteBuffer.putInt((int)n3);
        for (n2 = n4; n2 < this.FloorBloodSplats.size(); ++n2) {
            IsoFloorBloodSplat isoFloorBloodSplat = (IsoFloorBloodSplat)this.FloorBloodSplats.get((int)n2);
            isoFloorBloodSplat.save((ByteBuffer)byteBuffer);
        }
        n2 = byteBuffer.position();
        byte by = 0;
        int n5 = 0;
        int n6 = 0;
        for (n = 0; n < 10; ++n) {
            for (int i = 0; i < 10; ++i) {
                by = 0;
                n5 = byteBuffer.position();
                byteBuffer.put((byte)by);
                block5: for (int j = 0; j < 8; ++j) {
                    IsoGridSquare isoGridSquare = this.getGridSquare((int)n, (int)i, (int)j);
                    byteBuffer = IsoChunk.ensureCapacity((ByteBuffer)byteBuffer);
                    if (isoGridSquare == null || !isoGridSquare.shouldSave()) continue;
                    by = (byte)(by | 1 << j);
                    int n7 = byteBuffer.position();
                    while (true) {
                        try {
                            isoGridSquare.save((ByteBuffer)byteBuffer, null, (boolean)IsoChunk.IsDebugSave());
                            continue block5;
                        }
                        catch (BufferOverflowException bufferOverflowException) {
                            DebugLog.log((String)"IsoChunk.Save: BufferOverflowException, growing ByteBuffer");
                            byteBuffer = IsoChunk.ensureCapacity((ByteBuffer)byteBuffer);
                            byteBuffer.position((int)n7);
                            continue;
                        }
                        break;
                    }
                }
                n6 = byteBuffer.position();
                byteBuffer.position((int)n5);
                byteBuffer.put((byte)by);
                byteBuffer.position((int)n6);
            }
        }
        byteBuffer = IsoChunk.ensureCapacity((ByteBuffer)byteBuffer);
        this.getErosionData().save((ByteBuffer)byteBuffer);
        if (this.generatorsTouchingThisChunk == null) {
            byteBuffer.putShort((short)0);
        } else {
            byteBuffer.putShort((short)((short)this.generatorsTouchingThisChunk.size()));
            for (n = 0; n < this.generatorsTouchingThisChunk.size(); ++n) {
                IsoGameCharacter.Location location = (IsoGameCharacter.Location)this.generatorsTouchingThisChunk.get((int)n);
                byteBuffer.putInt((int)location.x);
                byteBuffer.putInt((int)location.y);
                byteBuffer.put((byte)((byte)location.z));
            }
        }
        byteBuffer.putShort((short)0);
        if (!(GameServer.bServer && !GameServer.bSoftReset || GameClient.bClient || GameWindow.bLoadedAsClient)) {
            VehiclesDB2.instance.unloadChunk((IsoChunk)this);
        }
        if (GameClient.bClient) {
            n = ServerOptions.instance.HoursForLootRespawn.getValue();
            this.lootRespawnHour = n <= 0 || GameTime.getInstance().getWorldAgeHours() < (double)n ? -1 : 7 + (int)(GameTime.getInstance().getWorldAgeHours() / (double)n) * n;
        }
        byteBuffer.putInt((int)this.lootRespawnHour);
        if (!$assertionsDisabled && this.m_spawnedRooms.size() > 127) {
            throw new AssertionError();
        }
        byteBuffer.put((byte)((byte)this.m_spawnedRooms.size()));
        for (n = 0; n < this.m_spawnedRooms.size(); ++n) {
            byteBuffer.putInt((int)this.m_spawnedRooms.get((int)n));
        }
        n = byteBuffer.position();
        cRC32.reset();
        cRC32.update((byte[])byteBuffer.array(), (int)17, (int)(n - 1 - 4 - 4 - 8));
        byteBuffer.position((int)5);
        byteBuffer.putInt((int)n);
        byteBuffer.putLong((long)cRC32.getValue());
        byteBuffer.position((int)n);
        return byteBuffer;
    }

    public boolean saveObjectState(ByteBuffer byteBuffer) throws IOException {
        boolean bl = true;
        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 10; ++j) {
                for (int k = 0; k < 10; ++k) {
                    IsoGridSquare isoGridSquare = this.getGridSquare((int)k, (int)j, (int)i);
                    if (isoGridSquare == null) continue;
                    int n = isoGridSquare.getObjects().size();
                    IsoObject[] isoObjectArray = (IsoObject[])isoGridSquare.getObjects().getElements();
                    for (int i2 = 0; i2 < n; ++i2) {
                        IsoObject isoObject = isoObjectArray[i2];
                        int n2 = byteBuffer.position();
                        byteBuffer.position((int)(n2 + 2 + 2 + 4 + 2));
                        int n3 = byteBuffer.position();
                        isoObject.saveState((ByteBuffer)byteBuffer);
                        int n4 = byteBuffer.position();
                        if (n4 > n3) {
                            byteBuffer.position((int)n2);
                            byteBuffer.putShort((short)((short)(k + j * 10 + i * 10 * 10)));
                            byteBuffer.putShort((short)((short)i2));
                            byteBuffer.putInt((int)isoObject.getObjectName().hashCode());
                            byteBuffer.putShort((short)((short)(n4 - n3)));
                            byteBuffer.position((int)n4);
                            bl = false;
                            continue;
                        }
                        byteBuffer.position((int)n2);
                    }
                }
            }
        }
        if (bl) {
            return false;
        }
        byteBuffer.putShort((short)-1);
        return true;
    }

    public void loadObjectState(ByteBuffer byteBuffer) throws IOException {
        short s = byteBuffer.getShort();
        while (s != -1) {
            int n = s % 10;
            int n2 = s / 100;
            int n3 = (s - n2 * 10 * 10) / 10;
            short s2 = byteBuffer.getShort();
            int n4 = byteBuffer.getInt();
            short s3 = byteBuffer.getShort();
            int n5 = byteBuffer.position();
            IsoGridSquare isoGridSquare = this.getGridSquare((int)n, (int)n3, (int)n2);
            if (isoGridSquare != null && s2 >= 0 && s2 < isoGridSquare.getObjects().size()) {
                IsoObject isoObject = (IsoObject)isoGridSquare.getObjects().get((int)s2);
                if (n4 == isoObject.getObjectName().hashCode()) {
                    isoObject.loadState((ByteBuffer)byteBuffer);
                    if (!$assertionsDisabled && byteBuffer.position() != n5 + s3) {
                        throw new AssertionError();
                    }
                } else {
                    byteBuffer.position((int)(n5 + s3));
                }
            } else {
                byteBuffer.position((int)(n5 + s3));
            }
            s = byteBuffer.getShort();
        }
    }

    public void Blam(int n, int n2) {
        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 10; ++j) {
                for (int k = 0; k < 10; ++k) {
                    this.setSquare((int)j, (int)k, (int)i, null);
                }
            }
        }
        this.blam = true;
    }

    private void BackupBlam(int n, int n2, Exception exception) {
        FileOutputStream fileOutputStream;
        File file;
        File file2 = ZomboidFileSystem.instance.getFileInCurrentSave((String)"blam");
        file2.mkdirs();
        try {
            file = new File((String)(file2 + File.separator + "map_" + n + "_" + n2 + "_error.txt"));
            fileOutputStream = new FileOutputStream((File)file);
            PrintStream printStream = new PrintStream((OutputStream)fileOutputStream);
            exception.printStackTrace((PrintStream)printStream);
            printStream.close();
        }
        catch (Exception exception2) {
            exception2.printStackTrace();
        }
        file = ZomboidFileSystem.instance.getFileInCurrentSave((String)("map_" + n + "_" + n2 + ".bin"));
        if (!file.exists()) {
            return;
        }
        fileOutputStream = new File((String)(file2.getPath() + File.separator + "map_" + n + "_" + n2 + ".bin"));
        try {
            IsoChunk.copyFile((File)file, (File)fileOutputStream);
        }
        catch (Exception exception3) {
            exception3.printStackTrace();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     */
    private static void copyFile(File var0, File var1_1) throws IOException {
        if (!var1_1.exists()) {
            var1_1.createNewFile();
        }
        var2_2 = null;
        var3_3 = null;
        try {
            var2_2 = new FileInputStream((File)var0).getChannel();
            var3_3 = new FileOutputStream((File)var1_1).getChannel();
            var3_3.transferFrom((ReadableByteChannel)var2_2, (long)0L, (long)var2_2.size());
            if (var2_2 == null) ** GOTO lbl19
        }
        catch (Throwable var4_4) {
            if (var2_2 != null) {
                var2_2.close();
            }
            if (var3_3 != null) {
                var3_3.close();
            }
            throw var4_4;
        }
        var2_2.close();
lbl19:
        // 2 sources

        if (var3_3 != null) {
            var3_3.close();
        }
    }

    public ErosionData.Chunk getErosionData() {
        if (this.erosion == null) {
            this.erosion = new ErosionData.Chunk();
        }
        return this.erosion;
    }

    private static int newtiledefinitions(int n, int n2) {
        int n3 = 1;
        return n3 * 100 * 1000 + 10000 + n * 1000 + n2;
    }

    public static int Fix2x(IsoGridSquare isoGridSquare, int n) {
        if (isoGridSquare == null || isoGridSquare.chunk == null) {
            return n;
        }
        if (isoGridSquare.chunk.bFixed2x) {
            return n;
        }
        HashMap hashMap = IsoSpriteManager.instance.NamedMap;
        if (n >= IsoChunk.newtiledefinitions((int)140, (int)48) && n <= IsoChunk.newtiledefinitions((int)140, (int)51)) {
            return -1;
        }
        if (n >= IsoChunk.newtiledefinitions((int)8, (int)14) && n <= IsoChunk.newtiledefinitions((int)8, (int)71) && n % 8 >= 6) {
            return -1;
        }
        if (n == IsoChunk.newtiledefinitions((int)92, (int)2)) {
            return n + 20;
        }
        if (n == IsoChunk.newtiledefinitions((int)92, (int)20)) {
            return n + 1;
        }
        if (n == IsoChunk.newtiledefinitions((int)92, (int)21)) {
            return n - 1;
        }
        if (n >= IsoChunk.newtiledefinitions((int)92, (int)26) && n <= IsoChunk.newtiledefinitions((int)92, (int)29)) {
            return n + 6;
        }
        if (n == IsoChunk.newtiledefinitions((int)11, (int)16)) {
            return IsoChunk.newtiledefinitions((int)11, (int)45);
        }
        if (n == IsoChunk.newtiledefinitions((int)11, (int)17)) {
            return IsoChunk.newtiledefinitions((int)11, (int)43);
        }
        if (n == IsoChunk.newtiledefinitions((int)11, (int)18)) {
            return IsoChunk.newtiledefinitions((int)11, (int)41);
        }
        if (n == IsoChunk.newtiledefinitions((int)11, (int)19)) {
            return IsoChunk.newtiledefinitions((int)11, (int)47);
        }
        if (n == IsoChunk.newtiledefinitions((int)11, (int)24)) {
            return IsoChunk.newtiledefinitions((int)11, (int)26);
        }
        if (n == IsoChunk.newtiledefinitions((int)11, (int)25)) {
            return IsoChunk.newtiledefinitions((int)11, (int)27);
        }
        if (n == IsoChunk.newtiledefinitions((int)27, (int)42)) {
            return n + 1;
        }
        if (n == IsoChunk.newtiledefinitions((int)27, (int)43)) {
            return n - 1;
        }
        if (n == IsoChunk.newtiledefinitions((int)27, (int)44)) {
            return n + 3;
        }
        if (n == IsoChunk.newtiledefinitions((int)27, (int)47)) {
            return n - 2;
        }
        if (n == IsoChunk.newtiledefinitions((int)27, (int)45)) {
            return n + 1;
        }
        if (n == IsoChunk.newtiledefinitions((int)27, (int)46)) {
            return n - 2;
        }
        if (n == IsoChunk.newtiledefinitions((int)34, (int)4)) {
            return n + 1;
        }
        if (n == IsoChunk.newtiledefinitions((int)34, (int)5)) {
            return n - 1;
        }
        if (n >= IsoChunk.newtiledefinitions((int)14, (int)0) && n <= IsoChunk.newtiledefinitions((int)14, (int)7)) {
            return -1;
        }
        if (n >= IsoChunk.newtiledefinitions((int)14, (int)8) && n <= IsoChunk.newtiledefinitions((int)14, (int)12)) {
            return n + 72;
        }
        if (n == IsoChunk.newtiledefinitions((int)14, (int)13)) {
            return n + 71;
        }
        if (n >= IsoChunk.newtiledefinitions((int)14, (int)16) && n <= IsoChunk.newtiledefinitions((int)14, (int)17)) {
            return n + 72;
        }
        if (n == IsoChunk.newtiledefinitions((int)14, (int)18)) {
            return n + 73;
        }
        if (n == IsoChunk.newtiledefinitions((int)14, (int)19)) {
            return n + 66;
        }
        if (n == IsoChunk.newtiledefinitions((int)14, (int)20)) {
            return -1;
        }
        if (n == IsoChunk.newtiledefinitions((int)14, (int)21)) {
            return IsoChunk.newtiledefinitions((int)14, (int)89);
        }
        if (n == IsoChunk.newtiledefinitions((int)21, (int)0)) {
            return IsoChunk.newtiledefinitions((int)125, (int)16);
        }
        if (n == IsoChunk.newtiledefinitions((int)21, (int)1)) {
            return IsoChunk.newtiledefinitions((int)125, (int)32);
        }
        if (n == IsoChunk.newtiledefinitions((int)21, (int)2)) {
            return IsoChunk.newtiledefinitions((int)125, (int)48);
        }
        if (n == IsoChunk.newtiledefinitions((int)26, (int)0)) {
            return IsoChunk.newtiledefinitions((int)26, (int)6);
        }
        if (n == IsoChunk.newtiledefinitions((int)26, (int)6)) {
            return IsoChunk.newtiledefinitions((int)26, (int)0);
        }
        if (n == IsoChunk.newtiledefinitions((int)26, (int)1)) {
            return IsoChunk.newtiledefinitions((int)26, (int)7);
        }
        if (n == IsoChunk.newtiledefinitions((int)26, (int)7)) {
            return IsoChunk.newtiledefinitions((int)26, (int)1);
        }
        if (n == IsoChunk.newtiledefinitions((int)26, (int)8)) {
            return IsoChunk.newtiledefinitions((int)26, (int)14);
        }
        if (n == IsoChunk.newtiledefinitions((int)26, (int)14)) {
            return IsoChunk.newtiledefinitions((int)26, (int)8);
        }
        if (n == IsoChunk.newtiledefinitions((int)26, (int)9)) {
            return IsoChunk.newtiledefinitions((int)26, (int)15);
        }
        if (n == IsoChunk.newtiledefinitions((int)26, (int)15)) {
            return IsoChunk.newtiledefinitions((int)26, (int)9);
        }
        if (n == IsoChunk.newtiledefinitions((int)26, (int)16)) {
            return IsoChunk.newtiledefinitions((int)26, (int)22);
        }
        if (n == IsoChunk.newtiledefinitions((int)26, (int)22)) {
            return IsoChunk.newtiledefinitions((int)26, (int)16);
        }
        if (n == IsoChunk.newtiledefinitions((int)26, (int)17)) {
            return IsoChunk.newtiledefinitions((int)26, (int)23);
        }
        if (n == IsoChunk.newtiledefinitions((int)26, (int)23)) {
            return IsoChunk.newtiledefinitions((int)26, (int)17);
        }
        if (n >= IsoChunk.newtiledefinitions((int)148, (int)0) && n <= IsoChunk.newtiledefinitions((int)148, (int)16)) {
            int n2 = n - IsoChunk.newtiledefinitions((int)148, (int)0);
            return IsoChunk.newtiledefinitions((int)160, (int)n2);
        }
        if (n >= IsoChunk.newtiledefinitions((int)42, (int)44) && n <= IsoChunk.newtiledefinitions((int)42, (int)47) || n >= IsoChunk.newtiledefinitions((int)42, (int)52) && n <= IsoChunk.newtiledefinitions((int)42, (int)55)) {
            return -1;
        }
        if (n == IsoChunk.newtiledefinitions((int)43, (int)24)) {
            return n + 4;
        }
        if (n == IsoChunk.newtiledefinitions((int)43, (int)26)) {
            return n + 2;
        }
        if (n == IsoChunk.newtiledefinitions((int)43, (int)33)) {
            return n - 4;
        }
        if (n == IsoChunk.newtiledefinitions((int)44, (int)0)) {
            return IsoChunk.newtiledefinitions((int)44, (int)1);
        }
        if (n == IsoChunk.newtiledefinitions((int)44, (int)1)) {
            return IsoChunk.newtiledefinitions((int)44, (int)0);
        }
        if (n == IsoChunk.newtiledefinitions((int)44, (int)2)) {
            return IsoChunk.newtiledefinitions((int)44, (int)7);
        }
        if (n == IsoChunk.newtiledefinitions((int)44, (int)3)) {
            return IsoChunk.newtiledefinitions((int)44, (int)6);
        }
        if (n == IsoChunk.newtiledefinitions((int)44, (int)4)) {
            return IsoChunk.newtiledefinitions((int)44, (int)5);
        }
        if (n == IsoChunk.newtiledefinitions((int)44, (int)5)) {
            return IsoChunk.newtiledefinitions((int)44, (int)4);
        }
        if (n == IsoChunk.newtiledefinitions((int)44, (int)6)) {
            return IsoChunk.newtiledefinitions((int)44, (int)3);
        }
        if (n == IsoChunk.newtiledefinitions((int)44, (int)7)) {
            return IsoChunk.newtiledefinitions((int)44, (int)2);
        }
        if (n == IsoChunk.newtiledefinitions((int)44, (int)16)) {
            return IsoChunk.newtiledefinitions((int)44, (int)45);
        }
        if (n == IsoChunk.newtiledefinitions((int)44, (int)17)) {
            return IsoChunk.newtiledefinitions((int)44, (int)44);
        }
        if (n == IsoChunk.newtiledefinitions((int)44, (int)18)) {
            return IsoChunk.newtiledefinitions((int)44, (int)46);
        }
        if (n >= IsoChunk.newtiledefinitions((int)44, (int)19) && n <= IsoChunk.newtiledefinitions((int)44, (int)22)) {
            return n + 33;
        }
        if (n == IsoChunk.newtiledefinitions((int)44, (int)23)) {
            return IsoChunk.newtiledefinitions((int)44, (int)47);
        }
        if (n == IsoChunk.newtiledefinitions((int)46, (int)8)) {
            return IsoChunk.newtiledefinitions((int)46, (int)5);
        }
        if (n == IsoChunk.newtiledefinitions((int)46, (int)14)) {
            return IsoChunk.newtiledefinitions((int)46, (int)10);
        }
        if (n == IsoChunk.newtiledefinitions((int)46, (int)15)) {
            return IsoChunk.newtiledefinitions((int)46, (int)11);
        }
        if (n == IsoChunk.newtiledefinitions((int)46, (int)22)) {
            return IsoChunk.newtiledefinitions((int)46, (int)14);
        }
        if (n == IsoChunk.newtiledefinitions((int)46, (int)23)) {
            return IsoChunk.newtiledefinitions((int)46, (int)15);
        }
        if (n == IsoChunk.newtiledefinitions((int)46, (int)54)) {
            return IsoChunk.newtiledefinitions((int)46, (int)55);
        }
        if (n == IsoChunk.newtiledefinitions((int)46, (int)55)) {
            return IsoChunk.newtiledefinitions((int)46, (int)54);
        }
        if (n == IsoChunk.newtiledefinitions((int)106, (int)32)) {
            return IsoChunk.newtiledefinitions((int)106, (int)34);
        }
        if (n == IsoChunk.newtiledefinitions((int)106, (int)34)) {
            return IsoChunk.newtiledefinitions((int)106, (int)32);
        }
        if (n == IsoChunk.newtiledefinitions((int)47, (int)0) || n == IsoChunk.newtiledefinitions((int)47, (int)4)) {
            return n + 1;
        }
        if (n == IsoChunk.newtiledefinitions((int)47, (int)1) || n == IsoChunk.newtiledefinitions((int)47, (int)5)) {
            return n - 1;
        }
        if (n >= IsoChunk.newtiledefinitions((int)47, (int)8) && n <= IsoChunk.newtiledefinitions((int)47, (int)13)) {
            return n + 8;
        }
        if (n >= IsoChunk.newtiledefinitions((int)47, (int)22) && n <= IsoChunk.newtiledefinitions((int)47, (int)23)) {
            return n - 12;
        }
        if (n >= IsoChunk.newtiledefinitions((int)47, (int)44) && n <= IsoChunk.newtiledefinitions((int)47, (int)47)) {
            return n + 4;
        }
        if (n >= IsoChunk.newtiledefinitions((int)47, (int)48) && n <= IsoChunk.newtiledefinitions((int)47, (int)51)) {
            return n - 4;
        }
        if (n == IsoChunk.newtiledefinitions((int)48, (int)56)) {
            return IsoChunk.newtiledefinitions((int)48, (int)58);
        }
        if (n == IsoChunk.newtiledefinitions((int)48, (int)58)) {
            return IsoChunk.newtiledefinitions((int)48, (int)56);
        }
        if (n == IsoChunk.newtiledefinitions((int)52, (int)57)) {
            return IsoChunk.newtiledefinitions((int)52, (int)58);
        }
        if (n == IsoChunk.newtiledefinitions((int)52, (int)58)) {
            return IsoChunk.newtiledefinitions((int)52, (int)59);
        }
        if (n == IsoChunk.newtiledefinitions((int)52, (int)45)) {
            return IsoChunk.newtiledefinitions((int)52, (int)44);
        }
        if (n == IsoChunk.newtiledefinitions((int)52, (int)46)) {
            return IsoChunk.newtiledefinitions((int)52, (int)45);
        }
        if (n == IsoChunk.newtiledefinitions((int)54, (int)13)) {
            return IsoChunk.newtiledefinitions((int)54, (int)18);
        }
        if (n == IsoChunk.newtiledefinitions((int)54, (int)15)) {
            return IsoChunk.newtiledefinitions((int)54, (int)19);
        }
        if (n == IsoChunk.newtiledefinitions((int)54, (int)21)) {
            return IsoChunk.newtiledefinitions((int)54, (int)16);
        }
        if (n == IsoChunk.newtiledefinitions((int)54, (int)22)) {
            return IsoChunk.newtiledefinitions((int)54, (int)13);
        }
        if (n == IsoChunk.newtiledefinitions((int)54, (int)23)) {
            return IsoChunk.newtiledefinitions((int)54, (int)17);
        }
        if (n >= IsoChunk.newtiledefinitions((int)67, (int)0) && n <= IsoChunk.newtiledefinitions((int)67, (int)16)) {
            int n3 = 64 + Rand.Next((int)16);
            return ((IsoSprite)hashMap.get((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"f_bushes_1_"}, (int)n3))).ID;
        }
        if (n == IsoChunk.newtiledefinitions((int)68, (int)6)) {
            return -1;
        }
        if (n >= IsoChunk.newtiledefinitions((int)68, (int)16) && n <= IsoChunk.newtiledefinitions((int)68, (int)17)) {
            return ((IsoSprite)hashMap.get((Object)"d_plants_1_53")).ID;
        }
        if (n >= IsoChunk.newtiledefinitions((int)68, (int)18) && n <= IsoChunk.newtiledefinitions((int)68, (int)23)) {
            int n4 = Rand.Next((int)4) * 16 + Rand.Next((int)8);
            return ((IsoSprite)hashMap.get((Object)StringConcatFactory.makeConcatWithConstants("makeConcatWithConstants", new Object[]{"d_plants_1_"}, (int)n4))).ID;
        }
        if (n >= IsoChunk.newtiledefinitions((int)79, (int)24) && n <= IsoChunk.newtiledefinitions((int)79, (int)41)) {
            return IsoChunk.newtiledefinitions((int)81, (int)(n - IsoChunk.newtiledefinitions((int)79, (int)24)));
        }
        return n;
    }

    public static String Fix2x(String string) {
        int n;
        Object object;
        if (Fix2xMap.isEmpty()) {
            object = Fix2xMap;
            for (n = 48; n <= 51; ++n) {
                object.put((Object)("blends_streetoverlays_01_" + n), (Object)"");
            }
            object.put((Object)"fencing_01_14", (Object)"");
            object.put((Object)"fencing_01_15", (Object)"");
            object.put((Object)"fencing_01_22", (Object)"");
            object.put((Object)"fencing_01_23", (Object)"");
            object.put((Object)"fencing_01_30", (Object)"");
            object.put((Object)"fencing_01_31", (Object)"");
            object.put((Object)"fencing_01_38", (Object)"");
            object.put((Object)"fencing_01_39", (Object)"");
            object.put((Object)"fencing_01_46", (Object)"");
            object.put((Object)"fencing_01_47", (Object)"");
            object.put((Object)"fencing_01_62", (Object)"");
            object.put((Object)"fencing_01_63", (Object)"");
            object.put((Object)"fencing_01_70", (Object)"");
            object.put((Object)"fencing_01_71", (Object)"");
            object.put((Object)"fixtures_bathroom_02_2", (Object)"fixtures_bathroom_02_22");
            object.put((Object)"fixtures_bathroom_02_20", (Object)"fixtures_bathroom_02_21");
            object.put((Object)"fixtures_bathroom_02_21", (Object)"fixtures_bathroom_02_20");
            for (n = 26; n <= 29; ++n) {
                object.put((Object)("fixtures_bathroom_02_" + n), (Object)("fixtures_bathroom_02_" + (n + 6)));
            }
            object.put((Object)"fixtures_counters_01_16", (Object)"fixtures_counters_01_45");
            object.put((Object)"fixtures_counters_01_17", (Object)"fixtures_counters_01_43");
            object.put((Object)"fixtures_counters_01_18", (Object)"fixtures_counters_01_41");
            object.put((Object)"fixtures_counters_01_19", (Object)"fixtures_counters_01_47");
            object.put((Object)"fixtures_counters_01_24", (Object)"fixtures_counters_01_26");
            object.put((Object)"fixtures_counters_01_25", (Object)"fixtures_counters_01_27");
            for (n = 0; n <= 7; ++n) {
                object.put((Object)("fixtures_railings_01_" + n), (Object)"");
            }
            for (n = 8; n <= 12; ++n) {
                object.put((Object)("fixtures_railings_01_" + n), (Object)("fixtures_railings_01_" + (n + 72)));
            }
            object.put((Object)"fixtures_railings_01_13", (Object)"fixtures_railings_01_84");
            for (n = 16; n <= 17; ++n) {
                object.put((Object)("fixtures_railings_01_" + n), (Object)("fixtures_railings_01_" + (n + 72)));
            }
            object.put((Object)"fixtures_railings_01_18", (Object)"fixtures_railings_01_91");
            object.put((Object)"fixtures_railings_01_19", (Object)"fixtures_railings_01_85");
            object.put((Object)"fixtures_railings_01_20", (Object)"");
            object.put((Object)"fixtures_railings_01_21", (Object)"fixtures_railings_01_89");
            object.put((Object)"floors_exterior_natural_01_0", (Object)"blends_natural_01_16");
            object.put((Object)"floors_exterior_natural_01_1", (Object)"blends_natural_01_32");
            object.put((Object)"floors_exterior_natural_01_2", (Object)"blends_natural_01_48");
            object.put((Object)"floors_rugs_01_0", (Object)"floors_rugs_01_6");
            object.put((Object)"floors_rugs_01_6", (Object)"floors_rugs_01_0");
            object.put((Object)"floors_rugs_01_1", (Object)"floors_rugs_01_7");
            object.put((Object)"floors_rugs_01_7", (Object)"floors_rugs_01_1");
            object.put((Object)"floors_rugs_01_8", (Object)"floors_rugs_01_14");
            object.put((Object)"floors_rugs_01_14", (Object)"floors_rugs_01_8");
            object.put((Object)"floors_rugs_01_9", (Object)"floors_rugs_01_15");
            object.put((Object)"floors_rugs_01_15", (Object)"floors_rugs_01_9");
            object.put((Object)"floors_rugs_01_16", (Object)"floors_rugs_01_22");
            object.put((Object)"floors_rugs_01_22", (Object)"floors_rugs_01_16");
            object.put((Object)"floors_rugs_01_17", (Object)"floors_rugs_01_23");
            object.put((Object)"floors_rugs_01_23", (Object)"floors_rugs_01_17");
            object.put((Object)"furniture_bedding_01_42", (Object)"furniture_bedding_01_43");
            object.put((Object)"furniture_bedding_01_43", (Object)"furniture_bedding_01_42");
            object.put((Object)"furniture_bedding_01_44", (Object)"furniture_bedding_01_47");
            object.put((Object)"furniture_bedding_01_47", (Object)"furniture_bedding_01_45");
            object.put((Object)"furniture_bedding_01_45", (Object)"furniture_bedding_01_46");
            object.put((Object)"furniture_bedding_01_46", (Object)"furniture_bedding_01_44");
            object.put((Object)"furniture_tables_low_01_4", (Object)"furniture_tables_low_01_5");
            object.put((Object)"furniture_tables_low_01_5", (Object)"furniture_tables_low_01_4");
            for (n = 0; n <= 5; ++n) {
                object.put((Object)("location_business_machinery_" + n), (Object)("location_business_machinery_01_" + n));
                object.put((Object)("location_business_machinery_" + (n + 8)), (Object)("location_business_machinery_01_" + (n + 8)));
                object.put((Object)("location_ business_machinery_" + n), (Object)("location_business_machinery_01_" + n));
                object.put((Object)("location_ business_machinery_" + (n + 8)), (Object)("location_business_machinery_01_" + (n + 8)));
            }
            for (n = 44; n <= 47; ++n) {
                object.put((Object)("location_hospitality_sunstarmotel_01_" + n), (Object)"");
            }
            for (n = 52; n <= 55; ++n) {
                object.put((Object)("location_hospitality_sunstarmotel_01_" + n), (Object)"");
            }
            object.put((Object)"location_hospitality_sunstarmotel_02_24", (Object)"location_hospitality_sunstarmotel_02_28");
            object.put((Object)"location_hospitality_sunstarmotel_02_26", (Object)"location_hospitality_sunstarmotel_02_28");
            object.put((Object)"location_hospitality_sunstarmotel_02_33", (Object)"location_hospitality_sunstarmotel_02_29");
            object.put((Object)"location_restaurant_bar_01_0", (Object)"location_restaurant_bar_01_1");
            object.put((Object)"location_restaurant_bar_01_1", (Object)"location_restaurant_bar_01_0");
            object.put((Object)"location_restaurant_bar_01_2", (Object)"location_restaurant_bar_01_7");
            object.put((Object)"location_restaurant_bar_01_3", (Object)"location_restaurant_bar_01_6");
            object.put((Object)"location_restaurant_bar_01_4", (Object)"location_restaurant_bar_01_5");
            object.put((Object)"location_restaurant_bar_01_5", (Object)"location_restaurant_bar_01_4");
            object.put((Object)"location_restaurant_bar_01_6", (Object)"location_restaurant_bar_01_3");
            object.put((Object)"location_restaurant_bar_01_7", (Object)"location_restaurant_bar_01_2");
            object.put((Object)"location_restaurant_bar_01_16", (Object)"location_restaurant_bar_01_45");
            object.put((Object)"location_restaurant_bar_01_17", (Object)"location_restaurant_bar_01_44");
            object.put((Object)"location_restaurant_bar_01_18", (Object)"location_restaurant_bar_01_46");
            for (n = 19; n <= 22; ++n) {
                object.put((Object)("location_restaurant_bar_01_" + n), (Object)("location_restaurant_bar_01_" + (n + 33)));
            }
            object.put((Object)"location_restaurant_bar_01_23", (Object)"location_restaurant_bar_01_47");
            object.put((Object)"location_restaurant_pie_01_8", (Object)"location_restaurant_pie_01_5");
            object.put((Object)"location_restaurant_pie_01_14", (Object)"location_restaurant_pie_01_10");
            object.put((Object)"location_restaurant_pie_01_15", (Object)"location_restaurant_pie_01_11");
            object.put((Object)"location_restaurant_pie_01_22", (Object)"location_restaurant_pie_01_14");
            object.put((Object)"location_restaurant_pie_01_23", (Object)"location_restaurant_pie_01_15");
            object.put((Object)"location_restaurant_pie_01_54", (Object)"location_restaurant_pie_01_55");
            object.put((Object)"location_restaurant_pie_01_55", (Object)"location_restaurant_pie_01_54");
            object.put((Object)"location_pizzawhirled_01_32", (Object)"location_pizzawhirled_01_34");
            object.put((Object)"location_pizzawhirled_01_34", (Object)"location_pizzawhirled_01_32");
            object.put((Object)"location_restaurant_seahorse_01_0", (Object)"location_restaurant_seahorse_01_1");
            object.put((Object)"location_restaurant_seahorse_01_1", (Object)"location_restaurant_seahorse_01_0");
            object.put((Object)"location_restaurant_seahorse_01_4", (Object)"location_restaurant_seahorse_01_5");
            object.put((Object)"location_restaurant_seahorse_01_5", (Object)"location_restaurant_seahorse_01_4");
            for (n = 8; n <= 13; ++n) {
                object.put((Object)("location_restaurant_seahorse_01_" + n), (Object)("location_restaurant_seahorse_01_" + (n + 8)));
            }
            for (n = 22; n <= 23; ++n) {
                object.put((Object)("location_restaurant_seahorse_01_" + n), (Object)("location_restaurant_seahorse_01_" + (n - 12)));
            }
            for (n = 44; n <= 47; ++n) {
                object.put((Object)("location_restaurant_seahorse_01_" + n), (Object)("location_restaurant_seahorse_01_" + (n + 4)));
            }
            for (n = 48; n <= 51; ++n) {
                object.put((Object)("location_restaurant_seahorse_01_" + n), (Object)("location_restaurant_seahorse_01_" + (n - 4)));
            }
            object.put((Object)"location_restaurant_spiffos_01_56", (Object)"location_restaurant_spiffos_01_58");
            object.put((Object)"location_restaurant_spiffos_01_58", (Object)"location_restaurant_spiffos_01_56");
            object.put((Object)"location_shop_fossoil_01_45", (Object)"location_shop_fossoil_01_44");
            object.put((Object)"location_shop_fossoil_01_46", (Object)"location_shop_fossoil_01_45");
            object.put((Object)"location_shop_fossoil_01_57", (Object)"location_shop_fossoil_01_58");
            object.put((Object)"location_shop_fossoil_01_58", (Object)"location_shop_fossoil_01_59");
            object.put((Object)"location_shop_greenes_01_13", (Object)"location_shop_greenes_01_18");
            object.put((Object)"location_shop_greenes_01_15", (Object)"location_shop_greenes_01_19");
            object.put((Object)"location_shop_greenes_01_21", (Object)"location_shop_greenes_01_16");
            object.put((Object)"location_shop_greenes_01_22", (Object)"location_shop_greenes_01_13");
            object.put((Object)"location_shop_greenes_01_23", (Object)"location_shop_greenes_01_17");
            object.put((Object)"location_shop_greenes_01_67", (Object)"location_shop_greenes_01_70");
            object.put((Object)"location_shop_greenes_01_68", (Object)"location_shop_greenes_01_67");
            object.put((Object)"location_shop_greenes_01_70", (Object)"location_shop_greenes_01_71");
            object.put((Object)"location_shop_greenes_01_75", (Object)"location_shop_greenes_01_78");
            object.put((Object)"location_shop_greenes_01_76", (Object)"location_shop_greenes_01_75");
            object.put((Object)"location_shop_greenes_01_78", (Object)"location_shop_greenes_01_79");
            for (n = 0; n <= 16; ++n) {
                object.put((Object)("vegetation_foliage_01_" + n), (Object)"randBush");
            }
            object.put((Object)"vegetation_groundcover_01_0", (Object)"blends_grassoverlays_01_16");
            object.put((Object)"vegetation_groundcover_01_1", (Object)"blends_grassoverlays_01_8");
            object.put((Object)"vegetation_groundcover_01_2", (Object)"blends_grassoverlays_01_0");
            object.put((Object)"vegetation_groundcover_01_3", (Object)"blends_grassoverlays_01_64");
            object.put((Object)"vegetation_groundcover_01_4", (Object)"blends_grassoverlays_01_56");
            object.put((Object)"vegetation_groundcover_01_5", (Object)"blends_grassoverlays_01_48");
            object.put((Object)"vegetation_groundcover_01_6", (Object)"");
            object.put((Object)"vegetation_groundcover_01_44", (Object)"blends_grassoverlays_01_40");
            object.put((Object)"vegetation_groundcover_01_45", (Object)"blends_grassoverlays_01_32");
            object.put((Object)"vegetation_groundcover_01_46", (Object)"blends_grassoverlays_01_24");
            object.put((Object)"vegetation_groundcover_01_16", (Object)"d_plants_1_53");
            object.put((Object)"vegetation_groundcover_01_17", (Object)"d_plants_1_53");
            for (n = 18; n <= 23; ++n) {
                object.put((Object)("vegetation_groundcover_01_" + n), (Object)"randPlant");
            }
            for (n = 20; n <= 23; ++n) {
                object.put((Object)("walls_exterior_house_01_" + n), (Object)("walls_exterior_house_01_" + (n + 12)));
                object.put((Object)("walls_exterior_house_01_" + (n + 8)), (Object)("walls_exterior_house_01_" + (n + 8 + 12)));
            }
            for (n = 24; n <= 41; ++n) {
                object.put((Object)("walls_exterior_roofs_01_" + n), (Object)("walls_exterior_roofs_03_" + n));
            }
        }
        if ((object = (String)Fix2xMap.get((Object)string)) == null) {
            return string;
        }
        if ("randBush".equals(object)) {
            n = 64 + Rand.Next((int)16);
            return "f_bushes_1_" + n;
        }
        if ("randPlant".equals(object)) {
            n = Rand.Next((int)4) * 16 + Rand.Next((int)8);
            return "d_plants_1_" + n;
        }
        return object;
    }

    public void addGeneratorPos(int n, int n2, int n3) {
        if (this.generatorsTouchingThisChunk == null) {
            this.generatorsTouchingThisChunk = new ArrayList();
        }
        for (int i = 0; i < this.generatorsTouchingThisChunk.size(); ++i) {
            IsoGameCharacter.Location location = (IsoGameCharacter.Location)this.generatorsTouchingThisChunk.get((int)i);
            if (location.x != n || location.y != n2 || location.z != n3) continue;
            return;
        }
        IsoGameCharacter.Location location = new IsoGameCharacter.Location((int)n, (int)n2, (int)n3);
        this.generatorsTouchingThisChunk.add((Object)location);
    }

    public void removeGeneratorPos(int n, int n2, int n3) {
        if (this.generatorsTouchingThisChunk == null) {
            return;
        }
        for (int i = 0; i < this.generatorsTouchingThisChunk.size(); ++i) {
            IsoGameCharacter.Location location = (IsoGameCharacter.Location)this.generatorsTouchingThisChunk.get((int)i);
            if (location.x != n || location.y != n2 || location.z != n3) continue;
            this.generatorsTouchingThisChunk.remove((int)i);
            --i;
        }
    }

    public boolean isGeneratorPoweringSquare(int n, int n2, int n3) {
        if (this.generatorsTouchingThisChunk == null) {
            return false;
        }
        for (int i = 0; i < this.generatorsTouchingThisChunk.size(); ++i) {
            IsoGameCharacter.Location location = (IsoGameCharacter.Location)this.generatorsTouchingThisChunk.get((int)i);
            if (!IsoGenerator.isPoweringSquare((int)location.x, (int)location.y, (int)location.z, (int)n, (int)n2, (int)n3)) continue;
            return true;
        }
        return false;
    }

    public void checkForMissingGenerators() {
        if (this.generatorsTouchingThisChunk == null) {
            return;
        }
        for (int i = 0; i < this.generatorsTouchingThisChunk.size(); ++i) {
            IsoGenerator isoGenerator;
            IsoGameCharacter.Location location = (IsoGameCharacter.Location)this.generatorsTouchingThisChunk.get((int)i);
            IsoGridSquare isoGridSquare = IsoWorld.instance.CurrentCell.getGridSquare((int)location.x, (int)location.y, (int)location.z);
            if (isoGridSquare == null || (isoGenerator = isoGridSquare.getGenerator()) != null && isoGenerator.isActivated()) continue;
            this.generatorsTouchingThisChunk.remove((int)i);
            --i;
        }
    }

    public boolean isNewChunk() {
        return this.addZombies;
    }

    public void addSpawnedRoom(int n) {
        if (!this.m_spawnedRooms.contains((int)n)) {
            this.m_spawnedRooms.add((int)n);
        }
    }

    public boolean isSpawnedRoom(int n) {
        return this.m_spawnedRooms.contains((int)n);
    }

    public IsoMetaGrid.Zone getScavengeZone() {
        int n;
        if (this.m_scavengeZone != null) {
            return this.m_scavengeZone;
        }
        IsoMetaChunk isoMetaChunk = IsoWorld.instance.getMetaGrid().getChunkData((int)this.wx, (int)this.wy);
        if (isoMetaChunk != null && isoMetaChunk.numZones() > 0) {
            for (n = 0; n < isoMetaChunk.numZones(); ++n) {
                IsoMetaGrid.Zone zone = isoMetaChunk.getZone((int)n);
                if ("DeepForest".equals((Object)zone.type) || "Forest".equals((Object)zone.type)) {
                    this.m_scavengeZone = zone;
                    return zone;
                }
                if (!"Nav".equals((Object)zone.type) && !"Town".equals((Object)zone.type)) continue;
                return null;
            }
        }
        if (this.m_treeCount < (n = 5)) {
            return null;
        }
        int n2 = 0;
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                IsoChunk isoChunk;
                if (j == 0 && i == 0) continue;
                IsoChunk isoChunk2 = isoChunk = GameServer.bServer ? ServerMap.instance.getChunk((int)(this.wx + j), (int)(this.wy + i)) : IsoWorld.instance.CurrentCell.getChunk((int)(this.wx + j), (int)(this.wy + i));
                if (isoChunk == null || isoChunk.m_treeCount < n || ++n2 != 8) continue;
                int n3 = 10;
                this.m_scavengeZone = new IsoMetaGrid.Zone((String)"", (String)"Forest", (int)(this.wx * n3), (int)(this.wy * n3), (int)0, (int)n3, (int)n3);
                return this.m_scavengeZone;
            }
        }
        return null;
    }

    public void resetForStore() {
        int n;
        this.randomID = 0;
        this.revision = 0L;
        this.nextSplatIndex = 0;
        this.FloorBloodSplats.clear();
        this.FloorBloodSplatsFade.clear();
        this.jobType = JobType.None;
        this.maxLevel = -1;
        this.bFixed2x = false;
        this.vehicles.clear();
        this.roomLights.clear();
        this.blam = false;
        this.lotheader = null;
        this.bLoaded = false;
        this.addZombies = false;
        this.physicsCheck = false;
        this.loadedPhysics = false;
        this.wx = 0;
        this.wy = 0;
        this.erosion = null;
        this.lootRespawnHour = -1;
        if (this.generatorsTouchingThisChunk != null) {
            this.generatorsTouchingThisChunk.clear();
        }
        this.m_treeCount = 0;
        this.m_scavengeZone = null;
        this.m_numberOfWaterTiles = 0;
        this.m_spawnedRooms.resetQuick();
        this.m_adjacentChunkLoadedCounter = 0;
        for (n = 0; n < this.squares.length; ++n) {
            for (int i = 0; i < this.squares[0].length; ++i) {
                this.squares[n][i] = null;
            }
        }
        for (n = 0; n < 4; ++n) {
            this.lightCheck[n] = true;
            this.bLightingNeverDone[n] = true;
        }
        this.refs.clear();
        this.m_vehicleStorySpawnData = null;
        this.m_loadVehiclesObject = null;
        this.m_objectEmitterData.reset();
        MPStatistics.increaseStoredChunk();
    }

    public int getNumberOfWaterTiles() {
        return this.m_numberOfWaterTiles;
    }

    public void setRandomVehicleStoryToSpawnLater(VehicleStorySpawnData vehicleStorySpawnData) {
        this.m_vehicleStorySpawnData = vehicleStorySpawnData;
    }

    public boolean hasObjectAmbientEmitter(IsoObject isoObject) {
        return this.m_objectEmitterData.hasObject((IsoObject)isoObject);
    }

    public void addObjectAmbientEmitter(IsoObject isoObject, ObjectAmbientEmitters.PerObjectLogic perObjectLogic) {
        this.m_objectEmitterData.addObject((IsoObject)isoObject, (ObjectAmbientEmitters.PerObjectLogic)perObjectLogic);
    }

    public void removeObjectAmbientEmitter(IsoObject isoObject) {
        this.m_objectEmitterData.removeObject((IsoObject)isoObject);
    }

    static {
        $assertionsDisabled = !IsoChunk.class.desiredAssertionStatus();
        bDoServerRequests = true;
        renderByIndex = (byte[][])new byte[][]{{1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {1, 0, 0, 0, 0, 1, 0, 0, 0, 0}, {1, 0, 0, 1, 0, 0, 1, 0, 0, 0}, {1, 0, 0, 1, 0, 1, 0, 0, 1, 0}, {1, 0, 1, 0, 1, 0, 1, 0, 1, 0}, {1, 1, 0, 1, 1, 0, 1, 1, 0, 0}, {1, 1, 0, 1, 1, 0, 1, 1, 0, 1}, {1, 1, 1, 1, 0, 1, 1, 1, 1, 0}, {1, 1, 1, 1, 1, 1, 1, 1, 1, 0}, {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}};
        AddVehicles_ForTest_vtype = 0;
        AddVehicles_ForTest_vskin = 0;
        AddVehicles_ForTest_vrot = 0;
        BaseVehicleCheckedVehicles = new ArrayList();
        bshapes = new byte[4];
        chunkGetter = new ChunkGetter();
        loadGridSquare = new ConcurrentLinkedQueue();
        SliceBuffer = ByteBuffer.allocate((int)65536);
        SliceBufferLoad = ByteBuffer.allocate((int)65536);
        WriteLock = new Object();
        tempRoomDefs = new ArrayList();
        tempBuildings = new ArrayList();
        Locks = new ArrayList();
        FreeLocks = new Stack();
        sanityCheck = new SanityCheck();
        crcLoad = new CRC32();
        crcSave = new CRC32();
        prefix = "map_";
        Fix2xMap = new HashMap();
    }
}
