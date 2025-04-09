/*
 * Decompiled with CFR.
 */
package zombie.iso;

import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.AssertionError;
import java.lang.Boolean;
import java.lang.CharSequence;
import java.lang.Deprecated;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.InterruptedException;
import java.lang.Math;
import java.lang.Object;
import java.lang.String;
import java.lang.System;
import java.lang.Thread;
import java.lang.Throwable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joml.Vector2i;
import org.joml.Vector3f;
import se.krka.kahlua.integration.annotations.LuaMethod;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaThread;
import se.krka.kahlua.vm.LuaClosure;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaHookManager;
import zombie.Lua.LuaManager;
import zombie.MovingObjectUpdateScheduler;
import zombie.ReanimatedPlayers;
import zombie.SandboxOptions;
import zombie.VirtualZombieManager;
import zombie.ZomboidFileSystem;
import zombie.ai.astar.Mover;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.Rand;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.physics.WorldSimulation;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.utils.IntGrid;
import zombie.core.utils.OnceEvery;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.erosion.utils.Noise2D;
import zombie.gameStates.GameLoadingState;
import zombie.input.JoypadManager;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.BuildingDef;
import zombie.iso.CellLoader;
import zombie.iso.ChunkSaveWorker;
import zombie.iso.DiamondMatrixIterator;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridOcclusionData;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoGridStack;
import zombie.iso.IsoHeatSource;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoLot;
import zombie.iso.IsoMarkers;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoPushableObject;
import zombie.iso.IsoRoomLight;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWater;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.LotHeader;
import zombie.iso.RoomDef;
import zombie.iso.SliceY;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.Vector2;
import zombie.iso.WorldMarkers;
import zombie.iso.WorldReuserThread;
import zombie.iso.areas.BuildingScore;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.IsoRoomExit;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.sprite.CorpseFlies;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.sprite.shapers.FloorShaperAttachedSprites;
import zombie.iso.sprite.shapers.FloorShaperDiamond;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.fog.ImprovedFog;
import zombie.iso.weather.fx.IsoWeatherFX;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.popman.NetworkZombieSimulator;
import zombie.savefile.ClientPlayerDB;
import zombie.savefile.PlayerDB;
import zombie.scripting.objects.VehicleScript;
import zombie.ui.UIManager;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class IsoCell {
    public static int MaxHeight;
    private static Shader m_floorRenderShader;
    private static Shader m_wallRenderShader;
    public ArrayList<IsoGridSquare> Trees;
    static final ArrayList<IsoGridSquare> stchoices;
    public final IsoChunkMap[] ChunkMap;
    public final ArrayList<IsoBuilding> BuildingList;
    private final ArrayList<IsoWindow> WindowList;
    private final ArrayList<IsoMovingObject> ObjectList;
    private final ArrayList<IsoPushableObject> PushableObjectList;
    private final HashMap<Integer, BuildingScore> BuildingScores;
    private final ArrayList<IsoRoom> RoomList;
    private final ArrayList<IsoObject> StaticUpdaterObjectList;
    private final ArrayList<IsoZombie> ZombieList;
    private final ArrayList<IsoGameCharacter> RemoteSurvivorList;
    private final ArrayList<IsoMovingObject> removeList;
    private final ArrayList<IsoMovingObject> addList;
    private final ArrayList<IsoObject> ProcessIsoObject;
    private final ArrayList<IsoObject> ProcessIsoObjectRemove;
    private final ArrayList<InventoryItem> ProcessItems;
    private final ArrayList<InventoryItem> ProcessItemsRemove;
    private final ArrayList<IsoWorldInventoryObject> ProcessWorldItems;
    public final ArrayList<IsoWorldInventoryObject> ProcessWorldItemsRemove;
    private final IsoGridSquare[][] gridSquares;
    public static final boolean ENABLE_SQUARE_CACHE = true;
    private int height;
    private int width;
    private int worldX;
    private int worldY;
    public IntGrid DangerScore;
    private boolean safeToAdd;
    private final Stack<IsoLightSource> LamppostPositions;
    public final ArrayList<IsoRoomLight> roomLights;
    private final ArrayList<IsoHeatSource> heatSources;
    public final ArrayList<BaseVehicle> addVehicles;
    public final ArrayList<BaseVehicle> vehicles;
    public static final int ISOANGLEFACTOR = 3;
    private static final int ZOMBIESCANBUDGET = 10;
    private static final float NEARESTZOMBIEDISTSQRMAX = 150.0f;
    private int zombieScanCursor;
    private final IsoZombie[] nearestVisibleZombie;
    private final float[] nearestVisibleZombieDistSqr;
    private static Stack<BuildingScore> buildingscores;
    static ArrayList<IsoGridSquare> GridStack;
    public static final int RTF_SolidFloor = 1;
    public static final int RTF_VegetationCorpses = 2;
    public static final int RTF_MinusFloorCharacters = 4;
    public static final int RTF_ShadedFloor = 8;
    public static final int RTF_Shadows = 16;
    private static final ArrayList<IsoGridSquare> ShadowSquares;
    private static final ArrayList<IsoGridSquare> MinusFloorCharacters;
    private static final ArrayList<IsoGridSquare> SolidFloor;
    private static final ArrayList<IsoGridSquare> ShadedFloor;
    private static final ArrayList<IsoGridSquare> VegetationCorpses;
    public static final PerPlayerRender[] perPlayerRender;
    private final int[] StencilXY;
    private final int[] StencilXY2z;
    public int StencilX1;
    public int StencilY1;
    public int StencilX2;
    public int StencilY2;
    private Texture m_stencilTexture;
    private final DiamondMatrixIterator diamondMatrixIterator;
    private final Vector2i diamondMatrixPos;
    public int DeferredCharacterTick;
    private boolean hasSetupSnowGrid;
    private SnowGridTiles snowGridTiles_Square;
    private SnowGridTiles[] snowGridTiles_Strip;
    private SnowGridTiles[] snowGridTiles_Edge;
    private SnowGridTiles[] snowGridTiles_Cove;
    private SnowGridTiles snowGridTiles_Enclosed;
    private int m_snowFirstNonSquare;
    private Noise2D snowNoise2D;
    private SnowGrid snowGridCur;
    private SnowGrid snowGridPrev;
    private int snowFracTarget;
    private long snowFadeTime;
    private float snowTransitionTime;
    private int raport;
    private static final int SNOWSHORE_NONE = 0;
    private static final int SNOWSHORE_N = 1;
    private static final int SNOWSHORE_E = 2;
    private static final int SNOWSHORE_S = 4;
    private static final int SNOWSHORE_W = 8;
    public boolean recalcFloors;
    static int wx;
    static int wy;
    final KahluaTable[] drag;
    final ArrayList<IsoSurvivor> SurvivorList;
    private static Texture texWhite;
    private static IsoCell instance;
    private int currentLX;
    private int currentLY;
    private int currentLZ;
    int recalcShading;
    int lastMinX;
    int lastMinY;
    private float rainScroll;
    private int[] rainX;
    private int[] rainY;
    private Texture[] rainTextures;
    private long[] rainFileTime;
    private float rainAlphaMax;
    private float[] rainAlpha;
    protected int rainIntensity;
    protected int rainSpeed;
    int lightUpdateCount;
    public boolean bRendering;
    final boolean[] bHideFloors;
    final int[] unhideFloorsCounter;
    boolean bOccludedByOrphanStructureFlag;
    int playerPeekedRoomId;
    final ArrayList<ArrayList<IsoBuilding>> playerOccluderBuildings;
    final IsoBuilding[][] playerOccluderBuildingsArr;
    final int[] playerWindowPeekingRoomId;
    final boolean[] playerHidesOrphanStructures;
    final boolean[] playerCutawaysDirty;
    final Vector2 tempCutawaySqrVector;
    ArrayList<Integer> tempPrevPlayerCutawayRoomIDs;
    ArrayList<Integer> tempPlayerCutawayRoomIDs;
    final IsoGridSquare[] lastPlayerSquare;
    final boolean[] lastPlayerSquareHalf;
    final IsoDirections[] lastPlayerDir;
    final Vector2[] lastPlayerAngle;
    int hidesOrphanStructuresAbove;
    final Rectangle buildingRectTemp;
    final ArrayList<ArrayList<IsoBuilding>> zombieOccluderBuildings;
    final IsoBuilding[][] zombieOccluderBuildingsArr;
    final IsoGridSquare[] lastZombieSquare;
    final boolean[] lastZombieSquareHalf;
    final ArrayList<ArrayList<IsoBuilding>> otherOccluderBuildings;
    final IsoBuilding[][] otherOccluderBuildingsArr;
    final int mustSeeSquaresRadius = 4;
    final int mustSeeSquaresGridSize = 10;
    final ArrayList<IsoGridSquare> gridSquaresTempLeft;
    final ArrayList<IsoGridSquare> gridSquaresTempRight;
    private IsoWeatherFX weatherFX;
    private int minX;
    private int maxX;
    private int minY;
    private int maxY;
    private int minZ;
    private int maxZ;
    private OnceEvery dangerUpdate;
    private Thread LightInfoUpdate;
    private final Stack<IsoRoom> SpottedRooms;
    private IsoZombie fakeZombieForHit;
    static final /* synthetic */ boolean $assertionsDisabled;

    public static int getMaxHeight() {
        return MaxHeight;
    }

    public LotHeader getCurrentLotHeader() {
        IsoChunk isoChunk = this.getChunkForGridSquare((int)((int)IsoCamera.CamCharacter.x), (int)((int)IsoCamera.CamCharacter.y), (int)((int)IsoCamera.CamCharacter.z));
        return isoChunk.lotheader;
    }

    public IsoChunkMap getChunkMap(int n) {
        return this.ChunkMap[n];
    }

    public IsoGridSquare getFreeTile(RoomDef roomDef) {
        stchoices.clear();
        for (int i = 0; i < roomDef.rects.size(); ++i) {
            RoomDef.RoomRect roomRect = (RoomDef.RoomRect)roomDef.rects.get((int)i);
            for (int j = roomRect.x; j < roomRect.x + roomRect.w; ++j) {
                for (int k = roomRect.y; k < roomRect.y + roomRect.h; ++k) {
                    IsoGridSquare isoGridSquare = this.getGridSquare((int)j, (int)k, (int)roomDef.level);
                    if (isoGridSquare == null) continue;
                    isoGridSquare.setCachedIsFree((boolean)false);
                    isoGridSquare.setCacheIsFree((boolean)false);
                    if (!isoGridSquare.isFree((boolean)false)) continue;
                    stchoices.add((Object)isoGridSquare);
                }
            }
        }
        if (stchoices.isEmpty()) {
            return null;
        }
        IsoGridSquare isoGridSquare = (IsoGridSquare)stchoices.get((int)Rand.Next((int)stchoices.size()));
        stchoices.clear();
        return isoGridSquare;
    }

    public static Stack getBuildings() {
        return buildingscores;
    }

    public static void setBuildings(Stack stack) {
        buildingscores = stack;
    }

    public IsoZombie getNearestVisibleZombie(int n) {
        return this.nearestVisibleZombie[n];
    }

    public IsoChunk getChunkForGridSquare(int n, int n2, int n3) {
        int n4 = n;
        int n5 = n2;
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            if (this.ChunkMap[i].ignore) continue;
            n = n4;
            n2 = n5;
            if ((n -= this.ChunkMap[i].getWorldXMinTiles()) < 0 || (n2 -= this.ChunkMap[i].getWorldYMinTiles()) < 0) continue;
            IsoChunkMap cfr_ignored_0 = this.ChunkMap[i];
            IsoChunkMap cfr_ignored_1 = this.ChunkMap[i];
            IsoChunk isoChunk = null;
            isoChunk = this.ChunkMap[i].getChunk((int)(n /= 10), (int)(n2 /= 10));
            if (isoChunk == null) continue;
            return isoChunk;
        }
        return null;
    }

    public IsoChunk getChunk(int n, int n2) {
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoChunk isoChunk;
            IsoChunkMap isoChunkMap = this.ChunkMap[i];
            if (isoChunkMap.ignore || (isoChunk = isoChunkMap.getChunk((int)(n - isoChunkMap.getWorldXMin()), (int)(n2 - isoChunkMap.getWorldYMin()))) == null) continue;
            return isoChunk;
        }
        return null;
    }

    public IsoCell(int n, int n2) {
        super();
        this.Trees = new ArrayList();
        this.ChunkMap = new IsoChunkMap[4];
        this.BuildingList = new ArrayList();
        this.WindowList = new ArrayList();
        this.ObjectList = new ArrayList();
        this.PushableObjectList = new ArrayList();
        this.BuildingScores = new HashMap();
        this.RoomList = new ArrayList();
        this.StaticUpdaterObjectList = new ArrayList();
        this.ZombieList = new ArrayList();
        this.RemoteSurvivorList = new ArrayList();
        this.removeList = new ArrayList();
        this.addList = new ArrayList();
        this.ProcessIsoObject = new ArrayList();
        this.ProcessIsoObjectRemove = new ArrayList();
        this.ProcessItems = new ArrayList();
        this.ProcessItemsRemove = new ArrayList();
        this.ProcessWorldItems = new ArrayList();
        this.ProcessWorldItemsRemove = new ArrayList();
        this.gridSquares = new IsoGridSquare[4][IsoChunkMap.ChunkWidthInTiles * IsoChunkMap.ChunkWidthInTiles * 8];
        this.safeToAdd = true;
        this.LamppostPositions = new Stack();
        this.roomLights = new ArrayList();
        this.heatSources = new ArrayList();
        this.addVehicles = new ArrayList();
        this.vehicles = new ArrayList();
        this.zombieScanCursor = 0;
        this.nearestVisibleZombie = new IsoZombie[4];
        this.nearestVisibleZombieDistSqr = new float[4];
        this.StencilXY = new int[]{0, 0, -1, 0, 0, -1, -1, -1, -2, -1, -1, -2, -2, -2, -3, -2, -2, -3, -3, -3};
        this.StencilXY2z = new int[]{0, 0, -1, 0, 0, -1, -1, -1, -2, -1, -1, -2, -2, -2, -3, -2, -2, -3, -3, -3, -4, -3, -3, -4, -4, -4, -5, -4, -4, -5, -5, -5, -6, -5, -5, -6, -6, -6};
        this.m_stencilTexture = null;
        this.diamondMatrixIterator = new DiamondMatrixIterator((int)123);
        this.diamondMatrixPos = new Vector2i();
        this.DeferredCharacterTick = 0;
        this.hasSetupSnowGrid = false;
        this.m_snowFirstNonSquare = -1;
        this.snowNoise2D = new Noise2D();
        this.snowFracTarget = 0;
        this.snowFadeTime = 0L;
        this.snowTransitionTime = 5000.0f;
        this.raport = 0;
        this.recalcFloors = false;
        this.drag = new KahluaTable[4];
        this.SurvivorList = new ArrayList();
        this.currentLX = 0;
        this.currentLY = 0;
        this.currentLZ = 0;
        this.recalcShading = 30;
        this.lastMinX = -1234567;
        this.lastMinY = -1234567;
        this.rainX = new int[4];
        this.rainY = new int[4];
        this.rainTextures = new Texture[5];
        this.rainFileTime = new long[5];
        this.rainAlphaMax = 0.6f;
        this.rainAlpha = new float[4];
        this.rainIntensity = 0;
        this.rainSpeed = 6;
        this.lightUpdateCount = 11;
        this.bRendering = false;
        this.bHideFloors = new boolean[4];
        this.unhideFloorsCounter = new int[4];
        this.bOccludedByOrphanStructureFlag = false;
        this.playerPeekedRoomId = -1;
        this.playerOccluderBuildings = new ArrayList((int)4);
        this.playerOccluderBuildingsArr = new IsoBuilding[4][];
        this.playerWindowPeekingRoomId = new int[4];
        this.playerHidesOrphanStructures = new boolean[4];
        this.playerCutawaysDirty = new boolean[4];
        this.tempCutawaySqrVector = new Vector2();
        this.tempPrevPlayerCutawayRoomIDs = new ArrayList();
        this.tempPlayerCutawayRoomIDs = new ArrayList();
        this.lastPlayerSquare = new IsoGridSquare[4];
        this.lastPlayerSquareHalf = new boolean[4];
        this.lastPlayerDir = new IsoDirections[4];
        this.lastPlayerAngle = new Vector2[4];
        this.hidesOrphanStructuresAbove = MaxHeight;
        this.buildingRectTemp = new Rectangle();
        this.zombieOccluderBuildings = new ArrayList((int)4);
        this.zombieOccluderBuildingsArr = new IsoBuilding[4][];
        this.lastZombieSquare = new IsoGridSquare[4];
        this.lastZombieSquareHalf = new boolean[4];
        this.otherOccluderBuildings = new ArrayList((int)4);
        this.otherOccluderBuildingsArr = new IsoBuilding[4][];
        this.mustSeeSquaresRadius = 4;
        this.mustSeeSquaresGridSize = 10;
        this.gridSquaresTempLeft = new ArrayList((int)100);
        this.gridSquaresTempRight = new ArrayList((int)100);
        this.dangerUpdate = new OnceEvery((float)0.4f, (boolean)false);
        this.LightInfoUpdate = null;
        this.SpottedRooms = new Stack();
        IsoWorld.instance.CurrentCell = this;
        instance = this;
        this.width = n;
        this.height = n2;
        for (int i = 0; i < 4; ++i) {
            this.ChunkMap[i] = new IsoChunkMap((IsoCell)this);
            this.ChunkMap[i].PlayerID = i;
            this.ChunkMap[i].ignore = i > 0;
            this.playerOccluderBuildings.add((Object)new ArrayList((int)5));
            this.zombieOccluderBuildings.add((Object)new ArrayList((int)5));
            this.otherOccluderBuildings.add((Object)new ArrayList((int)5));
        }
        WorldReuserThread.instance.run();
    }

    public short getStencilValue(int n, int n2, int n3) {
        short[][][] sArray = IsoCell.perPlayerRender[IsoCamera.frameState.playerIndex].StencilValues;
        int n4 = 0;
        int n5 = 0;
        for (int i = 0; i < this.StencilXY.length; i += 2) {
            short[] sArray2;
            int n6 = -n3 * 3;
            int n7 = n + n6 + this.StencilXY[i];
            int n8 = n2 + n6 + this.StencilXY[i + 1];
            if (n7 < this.minX || n7 >= this.maxX || n8 < this.minY || n8 >= this.maxY || (sArray2 = sArray[n7 - this.minX][n8 - this.minY])[0] == 0) continue;
            if (n4 == 0) {
                n4 = sArray2[0];
                n5 = sArray2[1];
                continue;
            }
            n4 = Math.min((int)sArray2[0], (int)n4);
            n5 = Math.max((int)sArray2[1], (int)n5);
        }
        if (n4 == 0) {
            return 1;
        }
        if (n4 > 10) {
            return (short)(n4 - 10);
        }
        return (short)(n5 + 1);
    }

    public void setStencilValue(int n, int n2, int n3, int n4) {
        short[][][] sArray = IsoCell.perPlayerRender[IsoCamera.frameState.playerIndex].StencilValues;
        for (int i = 0; i < this.StencilXY.length; i += 2) {
            int n5 = -n3 * 3;
            int n6 = n + n5 + this.StencilXY[i];
            int n7 = n2 + n5 + this.StencilXY[i + 1];
            if (n6 < this.minX || n6 >= this.maxX || n7 < this.minY || n7 >= this.maxY) continue;
            short[] sArray2 = sArray[n6 - this.minX][n7 - this.minY];
            if (sArray2[0] == 0) {
                sArray2[0] = (short)n4;
                sArray2[1] = (short)n4;
                continue;
            }
            sArray2[0] = (short)Math.min((int)sArray2[0], (int)n4);
            sArray2[1] = (short)Math.max((int)sArray2[1], (int)n4);
        }
    }

    public short getStencilValue2z(int n, int n2, int n3) {
        short[][][] sArray = IsoCell.perPlayerRender[IsoCamera.frameState.playerIndex].StencilValues;
        int n4 = 0;
        int n5 = 0;
        int n6 = -n3 * 3;
        for (int i = 0; i < this.StencilXY2z.length; i += 2) {
            short[] sArray2;
            int n7 = n + n6 + this.StencilXY2z[i];
            int n8 = n2 + n6 + this.StencilXY2z[i + 1];
            if (n7 < this.minX || n7 >= this.maxX || n8 < this.minY || n8 >= this.maxY || (sArray2 = sArray[n7 - this.minX][n8 - this.minY])[0] == 0) continue;
            if (n4 == 0) {
                n4 = sArray2[0];
                n5 = sArray2[1];
                continue;
            }
            n4 = Math.min((int)sArray2[0], (int)n4);
            n5 = Math.max((int)sArray2[1], (int)n5);
        }
        if (n4 == 0) {
            return 1;
        }
        if (n4 > 10) {
            return (short)(n4 - 10);
        }
        return (short)(n5 + 1);
    }

    public void setStencilValue2z(int n, int n2, int n3, int n4) {
        short[][][] sArray = IsoCell.perPlayerRender[IsoCamera.frameState.playerIndex].StencilValues;
        int n5 = -n3 * 3;
        for (int i = 0; i < this.StencilXY2z.length; i += 2) {
            int n6 = n + n5 + this.StencilXY2z[i];
            int n7 = n2 + n5 + this.StencilXY2z[i + 1];
            if (n6 < this.minX || n6 >= this.maxX || n7 < this.minY || n7 >= this.maxY) continue;
            short[] sArray2 = sArray[n6 - this.minX][n7 - this.minY];
            if (sArray2[0] == 0) {
                sArray2[0] = (short)n4;
                sArray2[1] = (short)n4;
                continue;
            }
            sArray2[0] = (short)Math.min((int)sArray2[0], (int)n4);
            sArray2[1] = (short)Math.max((int)sArray2[1], (int)n4);
        }
    }

    public void CalculateVertColoursForTile(IsoGridSquare isoGridSquare, int n, int n2, int n3, int n4) {
        IsoGridSquare isoGridSquare2 = !IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)0, (int)0, (int)1) ? isoGridSquare.nav[IsoDirections.NW.index()] : null;
        IsoGridSquare isoGridSquare3 = !IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)1, (int)0, (int)1) ? isoGridSquare.nav[IsoDirections.N.index()] : null;
        IsoGridSquare isoGridSquare4 = !IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)2, (int)0, (int)1) ? isoGridSquare.nav[IsoDirections.NE.index()] : null;
        IsoGridSquare isoGridSquare5 = !IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)2, (int)1, (int)1) ? isoGridSquare.nav[IsoDirections.E.index()] : null;
        IsoGridSquare isoGridSquare6 = !IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)2, (int)2, (int)1) ? isoGridSquare.nav[IsoDirections.SE.index()] : null;
        IsoGridSquare isoGridSquare7 = !IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)1, (int)2, (int)1) ? isoGridSquare.nav[IsoDirections.S.index()] : null;
        IsoGridSquare isoGridSquare8 = !IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)0, (int)2, (int)1) ? isoGridSquare.nav[IsoDirections.SW.index()] : null;
        IsoGridSquare isoGridSquare9 = !IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)0, (int)1, (int)1) ? isoGridSquare.nav[IsoDirections.W.index()] : null;
        this.CalculateColor((IsoGridSquare)isoGridSquare2, (IsoGridSquare)isoGridSquare3, (IsoGridSquare)isoGridSquare9, (IsoGridSquare)isoGridSquare, (int)0, (int)n4);
        this.CalculateColor((IsoGridSquare)isoGridSquare3, (IsoGridSquare)isoGridSquare4, (IsoGridSquare)isoGridSquare5, (IsoGridSquare)isoGridSquare, (int)1, (int)n4);
        this.CalculateColor((IsoGridSquare)isoGridSquare6, (IsoGridSquare)isoGridSquare7, (IsoGridSquare)isoGridSquare5, (IsoGridSquare)isoGridSquare, (int)2, (int)n4);
        this.CalculateColor((IsoGridSquare)isoGridSquare8, (IsoGridSquare)isoGridSquare7, (IsoGridSquare)isoGridSquare9, (IsoGridSquare)isoGridSquare, (int)3, (int)n4);
    }

    private Texture getStencilTexture() {
        if (this.m_stencilTexture == null) {
            this.m_stencilTexture = Texture.getSharedTexture((String)"media/mask_circledithernew.png");
        }
        return this.m_stencilTexture;
    }

    public void DrawStencilMask() {
        Texture texture = this.getStencilTexture();
        if (texture == null) {
            return;
        }
        IndieGL.glStencilMask((int)255);
        IndieGL.glClear((int)1280);
        int n = IsoCamera.getOffscreenWidth((int)IsoPlayer.getPlayerIndex()) / 2;
        int n2 = IsoCamera.getOffscreenHeight((int)IsoPlayer.getPlayerIndex()) / 2;
        n -= texture.getWidth() / (2 / Core.TileScale);
        n2 -= texture.getHeight() / (2 / Core.TileScale);
        IndieGL.enableStencilTest();
        IndieGL.enableAlphaTest();
        IndieGL.glAlphaFunc((int)516, (float)0.1f);
        IndieGL.glStencilFunc((int)519, (int)128, (int)255);
        IndieGL.glStencilOp((int)7680, (int)7680, (int)7681);
        IndieGL.glColorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
        texture.renderstrip((int)(n - (int)IsoCamera.getRightClickOffX()), (int)(n2 - (int)IsoCamera.getRightClickOffY()), (int)(texture.getWidth() * Core.TileScale), (int)(texture.getHeight() * Core.TileScale), (float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f, null);
        IndieGL.glColorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        IndieGL.glStencilFunc((int)519, (int)0, (int)255);
        IndieGL.glStencilOp((int)7680, (int)7680, (int)7680);
        IndieGL.glStencilMask((int)127);
        IndieGL.glAlphaFunc((int)519, (float)0.0f);
        this.StencilX1 = n - (int)IsoCamera.getRightClickOffX();
        this.StencilY1 = n2 - (int)IsoCamera.getRightClickOffY();
        this.StencilX2 = this.StencilX1 + texture.getWidth() * Core.TileScale;
        this.StencilY2 = this.StencilY1 + texture.getHeight() * Core.TileScale;
    }

    public void RenderTiles(int n) {
        s_performance.isoCellRenderTiles.invokeAndMeasure((Object)this, (Object)Integer.valueOf((int)n), IsoCell::renderTilesInternal);
    }

    private void renderTilesInternal(int n) {
        if (!DebugOptions.instance.Terrain.RenderTiles.Enable.getValue()) {
            return;
        }
        if (m_floorRenderShader == null) {
            RenderThread.invokeOnRenderContext(this::initTileShaders);
        }
        int n2 = IsoCamera.frameState.playerIndex;
        IsoPlayer isoPlayer = IsoPlayer.players[n2];
        isoPlayer.dirtyRecalcGridStackTime -= GameTime.getInstance().getMultiplier() / 4.0f;
        PerPlayerRender perPlayerRender = this.getPerPlayerRenderAt((int)n2);
        perPlayerRender.setSize((int)(this.maxX - this.minX + 1), (int)(this.maxY - this.minY + 1));
        long l = System.currentTimeMillis();
        if (this.minX != perPlayerRender.minX || this.minY != perPlayerRender.minY || this.maxX != perPlayerRender.maxX || this.maxY != perPlayerRender.maxY) {
            perPlayerRender.minX = this.minX;
            perPlayerRender.minY = this.minY;
            perPlayerRender.maxX = this.maxX;
            perPlayerRender.maxY = this.maxY;
            isoPlayer.dirtyRecalcGridStack = true;
            WeatherFxMask.forceMaskUpdate((int)n2);
        }
        s_performance.renderTiles.recalculateAnyGridStacks.start();
        boolean bl = isoPlayer.dirtyRecalcGridStack;
        this.recalculateAnyGridStacks((PerPlayerRender)perPlayerRender, (int)n, (int)n2, (long)l);
        s_performance.renderTiles.recalculateAnyGridStacks.end();
        ++this.DeferredCharacterTick;
        s_performance.renderTiles.flattenAnyFoliage.start();
        this.flattenAnyFoliage((PerPlayerRender)perPlayerRender, (int)n2);
        s_performance.renderTiles.flattenAnyFoliage.end();
        if (this.SetCutawayRoomsForPlayer() || bl) {
            IsoGridStack isoGridStack = perPlayerRender.GridStacks;
            for (int i = 0; i < n + 1; ++i) {
                GridStack = (ArrayList)isoGridStack.Squares.get((int)i);
                for (int j = 0; j < GridStack.size(); ++j) {
                    IsoGridSquare isoGridSquare = (IsoGridSquare)GridStack.get((int)j);
                    isoGridSquare.setPlayerCutawayFlag((int)n2, (boolean)this.IsCutawaySquare((IsoGridSquare)isoGridSquare, (long)l), (long)l);
                }
            }
        }
        s_performance.renderTiles.performRenderTiles.start();
        this.performRenderTiles((PerPlayerRender)perPlayerRender, (int)n, (int)n2, (long)l);
        s_performance.renderTiles.performRenderTiles.end();
        this.playerCutawaysDirty[n2] = false;
        ShadowSquares.clear();
        MinusFloorCharacters.clear();
        ShadedFloor.clear();
        SolidFloor.clear();
        VegetationCorpses.clear();
        s_performance.renderTiles.renderDebugPhysics.start();
        this.renderDebugPhysics((int)n2);
        s_performance.renderTiles.renderDebugPhysics.end();
        s_performance.renderTiles.renderDebugLighting.start();
        this.renderDebugLighting((PerPlayerRender)perPlayerRender, (int)n);
        s_performance.renderTiles.renderDebugLighting.end();
    }

    private void initTileShaders() {
        if (DebugLog.isEnabled((DebugType)DebugType.Shader)) {
            DebugLog.Shader.debugln((String)"Loading shader: \"floorTile\"");
        }
        m_floorRenderShader = new Shader((String)"floorTile");
        if (DebugLog.isEnabled((DebugType)DebugType.Shader)) {
            DebugLog.Shader.debugln((String)"Loading shader: \"wallTile\"");
        }
        m_wallRenderShader = new Shader((String)"wallTile");
    }

    private PerPlayerRender getPerPlayerRenderAt(int n) {
        if (perPlayerRender[n] == null) {
            IsoCell.perPlayerRender[n] = new PerPlayerRender();
        }
        return perPlayerRender[n];
    }

    private void recalculateAnyGridStacks(PerPlayerRender perPlayerRender, int n, int n2, long l) {
        IsoPlayer isoPlayer = IsoPlayer.players[n2];
        if (!isoPlayer.dirtyRecalcGridStack) {
            return;
        }
        isoPlayer.dirtyRecalcGridStack = false;
        IsoGridStack isoGridStack = perPlayerRender.GridStacks;
        boolean[][][] blArray = perPlayerRender.VisiOccludedFlags;
        boolean[][] blArray2 = perPlayerRender.VisiCulledFlags;
        int n3 = -1;
        int n4 = -1;
        int n5 = -1;
        WeatherFxMask.setDiamondIterDone((int)n2);
        for (int i = n; i >= 0; --i) {
            int n6;
            Object object;
            GridStack = (ArrayList)isoGridStack.Squares.get((int)i);
            GridStack.clear();
            if (i >= this.maxZ) continue;
            if (DebugOptions.instance.Terrain.RenderTiles.NewRender.getValue()) {
                DiamondMatrixIterator diamondMatrixIterator = this.diamondMatrixIterator.reset((int)(this.maxX - this.minX));
                IsoGridSquare isoGridSquare = null;
                object = this.diamondMatrixPos;
                while (diamondMatrixIterator.next((Vector2i)object)) {
                    if (((Vector2i)object).y >= this.maxY - this.minY + 1) continue;
                    isoGridSquare = this.ChunkMap[n2].getGridSquare((int)(((Vector2i)object).x + this.minX), (int)(((Vector2i)object).y + this.minY), (int)i);
                    if (i == 0) {
                        blArray[((Vector2i)object).x][((Vector2i)object).y][0] = false;
                        blArray[((Vector2i)object).x][((Vector2i)object).y][1] = false;
                        blArray2[((Vector2i)object).x][((Vector2i)object).y] = false;
                    }
                    if (isoGridSquare == null) {
                        WeatherFxMask.addMaskLocation(null, (int)(((Vector2i)object).x + this.minX), (int)(((Vector2i)object).y + this.minY), (int)i);
                        continue;
                    }
                    IsoChunk isoChunk = isoGridSquare.getChunk();
                    if (isoChunk == null || !isoGridSquare.IsOnScreen((boolean)true)) continue;
                    WeatherFxMask.addMaskLocation((IsoGridSquare)isoGridSquare, (int)(((Vector2i)object).x + this.minX), (int)(((Vector2i)object).y + this.minY), (int)i);
                    n6 = (int)(this.IsDissolvedSquare((IsoGridSquare)isoGridSquare, (int)n2) ? 1 : 0);
                    isoGridSquare.setIsDissolved((int)n2, n6 != 0, (long)l);
                    if (isoGridSquare.getIsDissolved((int)n2, (long)l)) continue;
                    isoGridSquare.cacheLightInfo();
                    GridStack.add((Object)isoGridSquare);
                }
                continue;
            }
            for (int j = this.minY; j < this.maxY; ++j) {
                int n7 = this.minX;
                object = this.ChunkMap[n2].getGridSquare((int)n7, (int)j, (int)i);
                int n8 = IsoDirections.E.index();
                while (n7 < this.maxX) {
                    IsoChunk isoChunk;
                    if (i == 0) {
                        blArray[n7 - this.minX][j - this.minY][0] = false;
                        blArray[n7 - this.minX][j - this.minY][1] = false;
                        blArray2[n7 - this.minX][j - this.minY] = false;
                    }
                    if (object != null && ((IsoGridSquare)object).getY() != j) {
                        object = null;
                    }
                    n6 = -1;
                    int n9 = -1;
                    int n10 = n7;
                    int n11 = j;
                    IsoChunkMap cfr_ignored_0 = this.ChunkMap[n2];
                    IsoChunkMap cfr_ignored_1 = this.ChunkMap[n2];
                    n10 -= (this.ChunkMap[n2].WorldX - IsoChunkMap.ChunkGridWidth / 2) * 10;
                    IsoChunkMap cfr_ignored_2 = this.ChunkMap[n2];
                    IsoChunkMap cfr_ignored_3 = this.ChunkMap[n2];
                    n11 -= (this.ChunkMap[n2].WorldY - IsoChunkMap.ChunkGridWidth / 2) * 10;
                    IsoChunkMap cfr_ignored_4 = this.ChunkMap[n2];
                    IsoChunkMap cfr_ignored_5 = this.ChunkMap[n2];
                    n6 = n10 /= 10;
                    n9 = n11 /= 10;
                    if ((n6 != n3 || n9 != n4) && (isoChunk = this.ChunkMap[n2].getChunkForGridSquare((int)n7, (int)j)) != null) {
                        n5 = isoChunk.maxLevel;
                    }
                    n3 = n6;
                    n4 = n9;
                    if (n5 < i) {
                        ++n7;
                        continue;
                    }
                    if (object == null && (object = this.getGridSquare((int)n7, (int)j, (int)i)) == null && (object = this.ChunkMap[n2].getGridSquare((int)n7, (int)j, (int)i)) == null) {
                        ++n7;
                        continue;
                    }
                    IsoChunk isoChunk2 = ((IsoGridSquare)object).getChunk();
                    if (isoChunk2 != null && ((IsoGridSquare)object).IsOnScreen((boolean)true)) {
                        WeatherFxMask.addMaskLocation((IsoGridSquare)object, (int)((IsoGridSquare)object).x, (int)((IsoGridSquare)object).y, (int)i);
                        n11 = (int)(this.IsDissolvedSquare((IsoGridSquare)object, (int)n2) ? 1 : 0);
                        ((IsoGridSquare)object).setIsDissolved((int)n2, n11 != 0, (long)l);
                        if (!((IsoGridSquare)object).getIsDissolved((int)n2, (long)l)) {
                            ((IsoGridSquare)object).cacheLightInfo();
                            GridStack.add((Object)object);
                        }
                    }
                    object = ((IsoGridSquare)object).nav[n8];
                    ++n7;
                }
            }
        }
        this.CullFullyOccludedSquares((IsoGridStack)isoGridStack, (boolean[][][])blArray, (boolean[][])blArray2);
    }

    private void flattenAnyFoliage(PerPlayerRender perPlayerRender, int n) {
        int n2;
        short[][][] sArray = perPlayerRender.StencilValues;
        boolean[][] blArray = perPlayerRender.FlattenGrassEtc;
        for (n2 = this.minY; n2 <= this.maxY; ++n2) {
            for (int i = this.minX; i <= this.maxX; ++i) {
                sArray[i - this.minX][n2 - this.minY][0] = 0;
                sArray[i - this.minX][n2 - this.minY][1] = 0;
                blArray[i - this.minX][n2 - this.minY] = false;
            }
        }
        for (n2 = 0; n2 < this.vehicles.size(); ++n2) {
            BaseVehicle baseVehicle = (BaseVehicle)this.vehicles.get((int)n2);
            if (baseVehicle.getAlpha((int)n) <= 0.0f) continue;
            for (int i = -2; i < 5; ++i) {
                for (int j = -2; j < 5; ++j) {
                    int n3 = (int)baseVehicle.x + j;
                    int n4 = (int)baseVehicle.y + i;
                    if (n3 < this.minX || n3 > this.maxX || n4 < this.minY || n4 > this.maxY) continue;
                    blArray[n3 - this.minX][n4 - this.minY] = true;
                }
            }
        }
    }

    private void performRenderTiles(PerPlayerRender perPlayerRender, int n, int n2, long l) {
        Shader shader;
        Shader shader2;
        IsoGridStack isoGridStack = perPlayerRender.GridStacks;
        boolean[][] blArray = perPlayerRender.FlattenGrassEtc;
        if (!Core.bDebug || DebugOptions.instance.Terrain.RenderTiles.UseShaders.getValue()) {
            shader2 = m_floorRenderShader;
            shader = m_wallRenderShader;
        } else {
            shader2 = null;
            shader = null;
        }
        for (int i = 0; i < n + 1; ++i) {
            boolean bl;
            IsoGridSquare isoGridSquare;
            int n3;
            s_performance.renderTiles.PperformRenderTilesLayer pperformRenderTilesLayer = (s_performance.renderTiles.PperformRenderTilesLayer)s_performance.renderTiles.performRenderTilesLayers.start((int)i);
            GridStack = (ArrayList)isoGridStack.Squares.get((int)i);
            ShadowSquares.clear();
            SolidFloor.clear();
            ShadedFloor.clear();
            VegetationCorpses.clear();
            MinusFloorCharacters.clear();
            IndieGL.glClear((int)256);
            if (i == 0 && DebugOptions.instance.Terrain.RenderTiles.Water.getValue() && DebugOptions.instance.Terrain.RenderTiles.WaterBody.getValue()) {
                pperformRenderTilesLayer.renderIsoWater.start();
                IsoWater.getInstance().render(GridStack, (boolean)false);
                pperformRenderTilesLayer.renderIsoWater.end();
            }
            pperformRenderTilesLayer.renderFloor.start();
            for (n3 = 0; n3 < GridStack.size(); ++n3) {
                isoGridSquare = (IsoGridSquare)GridStack.get((int)n3);
                if (isoGridSquare.chunk != null && isoGridSquare.chunk.bLightingNeverDone[n2]) continue;
                isoGridSquare.bFlattenGrassEtc = i == 0 && blArray[isoGridSquare.x - this.minX][isoGridSquare.y - this.minY];
                int n4 = isoGridSquare.renderFloor((Shader)shader2);
                if (!isoGridSquare.getStaticMovingObjects().isEmpty()) {
                    n4 |= 2;
                    n4 |= 0x10;
                    if (isoGridSquare.HasStairs()) {
                        n4 |= 4;
                    }
                }
                if (!isoGridSquare.getWorldObjects().isEmpty()) {
                    n4 |= 2;
                }
                if (!isoGridSquare.getLocalTemporaryObjects().isEmpty()) {
                    n4 |= 4;
                }
                for (int j = 0; j < isoGridSquare.getMovingObjects().size(); ++j) {
                    IsoMovingObject isoMovingObject = (IsoMovingObject)isoGridSquare.getMovingObjects().get((int)j);
                    bl = isoMovingObject.bOnFloor;
                    if (bl && isoMovingObject instanceof IsoZombie) {
                        IsoZombie isoZombie = (IsoZombie)isoMovingObject;
                        bl = isoZombie.isProne();
                        if (!BaseVehicle.RENDER_TO_TEXTURE) {
                            bl = false;
                        }
                    }
                    n4 = bl ? (n4 |= 2) : (n4 |= 4);
                    n4 |= 0x10;
                }
                if (!isoGridSquare.getDeferedCharacters().isEmpty()) {
                    n4 |= 4;
                }
                if (isoGridSquare.hasFlies()) {
                    n4 |= 4;
                }
                if ((n4 & 1) != 0) {
                    SolidFloor.add((Object)isoGridSquare);
                }
                if ((n4 & 8) != 0) {
                    ShadedFloor.add((Object)isoGridSquare);
                }
                if ((n4 & 2) != 0) {
                    VegetationCorpses.add((Object)isoGridSquare);
                }
                if ((n4 & 4) != 0) {
                    MinusFloorCharacters.add((Object)isoGridSquare);
                }
                if ((n4 & 0x10) == 0) continue;
                ShadowSquares.add((Object)isoGridSquare);
            }
            pperformRenderTilesLayer.renderFloor.end();
            pperformRenderTilesLayer.renderPuddles.start();
            IsoPuddles.getInstance().render(SolidFloor, (int)i);
            pperformRenderTilesLayer.renderPuddles.end();
            if (i == 0 && DebugOptions.instance.Terrain.RenderTiles.Water.getValue() && DebugOptions.instance.Terrain.RenderTiles.WaterShore.getValue()) {
                pperformRenderTilesLayer.renderShore.start();
                IsoWater.getInstance().render(null, (boolean)true);
                pperformRenderTilesLayer.renderShore.end();
            }
            if (!SolidFloor.isEmpty()) {
                pperformRenderTilesLayer.renderSnow.start();
                this.RenderSnow((int)i);
                pperformRenderTilesLayer.renderSnow.end();
            }
            if (!GridStack.isEmpty()) {
                pperformRenderTilesLayer.renderBlood.start();
                this.ChunkMap[n2].renderBloodForChunks((int)i);
                pperformRenderTilesLayer.renderBlood.end();
            }
            if (!ShadedFloor.isEmpty()) {
                pperformRenderTilesLayer.renderFloorShading.start();
                this.RenderFloorShading((int)i);
                pperformRenderTilesLayer.renderFloorShading.end();
            }
            WorldMarkers.instance.renderGridSquareMarkers((PerPlayerRender)perPlayerRender, (int)i, (int)n2);
            if (DebugOptions.instance.Terrain.RenderTiles.Shadows.getValue()) {
                pperformRenderTilesLayer.renderShadows.start();
                this.renderShadows();
                pperformRenderTilesLayer.renderShadows.end();
            }
            if (DebugOptions.instance.Terrain.RenderTiles.Lua.getValue()) {
                pperformRenderTilesLayer.luaOnPostFloorLayerDraw.start();
                LuaEventManager.triggerEvent((String)"OnPostFloorLayerDraw", (Object)Integer.valueOf((int)i));
                pperformRenderTilesLayer.luaOnPostFloorLayerDraw.end();
            }
            IsoMarkers.instance.renderIsoMarkers((PerPlayerRender)perPlayerRender, (int)i, (int)n2);
            IsoMarkers.instance.renderCircleIsoMarkers((PerPlayerRender)perPlayerRender, (int)i, (int)n2);
            if (DebugOptions.instance.Terrain.RenderTiles.VegetationCorpses.getValue()) {
                pperformRenderTilesLayer.vegetationCorpses.start();
                for (n3 = 0; n3 < VegetationCorpses.size(); ++n3) {
                    isoGridSquare = (IsoGridSquare)VegetationCorpses.get((int)n3);
                    isoGridSquare.renderMinusFloor((int)this.maxZ, (boolean)false, (boolean)true, (boolean)false, (boolean)false, (boolean)false, (Shader)shader);
                    isoGridSquare.renderCharacters((int)this.maxZ, (boolean)true, (boolean)true);
                }
                pperformRenderTilesLayer.vegetationCorpses.end();
            }
            ImprovedFog.startRender((int)n2, (int)i);
            if (DebugOptions.instance.Terrain.RenderTiles.MinusFloorCharacters.getValue()) {
                pperformRenderTilesLayer.minusFloorCharacters.start();
                for (n3 = 0; n3 < MinusFloorCharacters.size(); ++n3) {
                    isoGridSquare = (IsoGridSquare)MinusFloorCharacters.get((int)n3);
                    IsoGridSquare isoGridSquare2 = isoGridSquare.nav[IsoDirections.S.index()];
                    IsoGridSquare isoGridSquare3 = isoGridSquare.nav[IsoDirections.E.index()];
                    boolean bl2 = isoGridSquare2 != null && isoGridSquare2.getPlayerCutawayFlag((int)n2, (long)l);
                    bl = isoGridSquare.getPlayerCutawayFlag((int)n2, (long)l);
                    boolean bl3 = isoGridSquare3 != null && isoGridSquare3.getPlayerCutawayFlag((int)n2, (long)l);
                    this.currentLY = isoGridSquare.getY() - this.minY;
                    this.currentLZ = i;
                    ImprovedFog.renderRowsBehind((IsoGridSquare)isoGridSquare);
                    boolean bl4 = isoGridSquare.renderMinusFloor((int)this.maxZ, (boolean)false, (boolean)false, (boolean)bl2, (boolean)bl, (boolean)bl3, (Shader)shader);
                    isoGridSquare.renderDeferredCharacters((int)this.maxZ);
                    isoGridSquare.renderCharacters((int)this.maxZ, (boolean)false, (boolean)true);
                    if (isoGridSquare.hasFlies()) {
                        CorpseFlies.render((int)isoGridSquare.x, (int)isoGridSquare.y, (int)isoGridSquare.z);
                    }
                    if (!bl4) continue;
                    isoGridSquare.renderMinusFloor((int)this.maxZ, (boolean)true, (boolean)false, (boolean)bl2, (boolean)bl, (boolean)bl3, (Shader)shader);
                }
                pperformRenderTilesLayer.minusFloorCharacters.end();
            }
            IsoMarkers.instance.renderIsoMarkersDeferred((PerPlayerRender)perPlayerRender, (int)i, (int)n2);
            ImprovedFog.endRender();
            pperformRenderTilesLayer.end();
        }
    }

    private void renderShadows() {
        boolean bl = Core.getInstance().getOptionCorpseShadows();
        for (int i = 0; i < ShadowSquares.size(); ++i) {
            IsoGameCharacter isoGameCharacter;
            IsoMovingObject isoMovingObject;
            int n;
            IsoGridSquare isoGridSquare = (IsoGridSquare)ShadowSquares.get((int)i);
            for (n = 0; n < isoGridSquare.getMovingObjects().size(); ++n) {
                isoMovingObject = (IsoMovingObject)isoGridSquare.getMovingObjects().get((int)n);
                isoGameCharacter = (IsoGameCharacter)Type.tryCastTo((Object)isoMovingObject, IsoGameCharacter.class);
                if (isoGameCharacter != null) {
                    isoGameCharacter.renderShadow((float)isoGameCharacter.getX(), (float)isoGameCharacter.getY(), (float)isoGameCharacter.getZ());
                    continue;
                }
                BaseVehicle baseVehicle = (BaseVehicle)Type.tryCastTo((Object)isoMovingObject, BaseVehicle.class);
                if (baseVehicle == null) continue;
                baseVehicle.renderShadow();
            }
            if (!bl) continue;
            for (n = 0; n < isoGridSquare.getStaticMovingObjects().size(); ++n) {
                isoMovingObject = (IsoMovingObject)isoGridSquare.getStaticMovingObjects().get((int)n);
                isoGameCharacter = (IsoDeadBody)Type.tryCastTo((Object)isoMovingObject, IsoDeadBody.class);
                if (isoGameCharacter == null) continue;
                isoGameCharacter.renderShadow();
            }
        }
    }

    private void renderDebugPhysics(int n) {
        if (Core.bDebug && DebugOptions.instance.PhysicsRender.getValue()) {
            TextureDraw.GenericDrawer genericDrawer = WorldSimulation.getDrawer((int)n);
            SpriteRenderer.instance.drawGeneric((TextureDraw.GenericDrawer)genericDrawer);
        }
    }

    private void renderDebugLighting(PerPlayerRender perPlayerRender, int n) {
        if (Core.bDebug && DebugOptions.instance.LightingRender.getValue()) {
            IsoGridStack isoGridStack = perPlayerRender.GridStacks;
            int n2 = 1;
            for (int i = 0; i < n + 1; ++i) {
                GridStack = (ArrayList)isoGridStack.Squares.get((int)i);
                for (int j = 0; j < GridStack.size(); ++j) {
                    IsoGridSquare isoGridSquare = (IsoGridSquare)GridStack.get((int)j);
                    float f = IsoUtils.XToScreenExact((float)((float)isoGridSquare.x + 0.3f), (float)((float)isoGridSquare.y), (float)0.0f, (int)0);
                    float f2 = IsoUtils.YToScreenExact((float)((float)isoGridSquare.x + 0.3f), (float)((float)isoGridSquare.y), (float)0.0f, (int)0);
                    float f3 = IsoUtils.XToScreenExact((float)((float)isoGridSquare.x + 0.6f), (float)((float)isoGridSquare.y), (float)0.0f, (int)0);
                    float f4 = IsoUtils.YToScreenExact((float)((float)isoGridSquare.x + 0.6f), (float)((float)isoGridSquare.y), (float)0.0f, (int)0);
                    float f5 = IsoUtils.XToScreenExact((float)((float)(isoGridSquare.x + 1)), (float)((float)isoGridSquare.y + 0.3f), (float)0.0f, (int)0);
                    float f6 = IsoUtils.YToScreenExact((float)((float)(isoGridSquare.x + 1)), (float)((float)isoGridSquare.y + 0.3f), (float)0.0f, (int)0);
                    float f7 = IsoUtils.XToScreenExact((float)((float)(isoGridSquare.x + 1)), (float)((float)isoGridSquare.y + 0.6f), (float)0.0f, (int)0);
                    float f8 = IsoUtils.YToScreenExact((float)((float)(isoGridSquare.x + 1)), (float)((float)isoGridSquare.y + 0.6f), (float)0.0f, (int)0);
                    float f9 = IsoUtils.XToScreenExact((float)((float)isoGridSquare.x + 0.6f), (float)((float)(isoGridSquare.y + 1)), (float)0.0f, (int)0);
                    float f10 = IsoUtils.YToScreenExact((float)((float)isoGridSquare.x + 0.6f), (float)((float)(isoGridSquare.y + 1)), (float)0.0f, (int)0);
                    float f11 = IsoUtils.XToScreenExact((float)((float)isoGridSquare.x + 0.3f), (float)((float)(isoGridSquare.y + 1)), (float)0.0f, (int)0);
                    float f12 = IsoUtils.YToScreenExact((float)((float)isoGridSquare.x + 0.3f), (float)((float)(isoGridSquare.y + 1)), (float)0.0f, (int)0);
                    float f13 = IsoUtils.XToScreenExact((float)((float)isoGridSquare.x), (float)((float)isoGridSquare.y + 0.6f), (float)0.0f, (int)0);
                    float f14 = IsoUtils.YToScreenExact((float)((float)isoGridSquare.x), (float)((float)isoGridSquare.y + 0.6f), (float)0.0f, (int)0);
                    float f15 = IsoUtils.XToScreenExact((float)((float)isoGridSquare.x), (float)((float)isoGridSquare.y + 0.3f), (float)0.0f, (int)0);
                    float f16 = IsoUtils.YToScreenExact((float)((float)isoGridSquare.x), (float)((float)isoGridSquare.y + 0.3f), (float)0.0f, (int)0);
                    if (IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)0, (int)0, (int)n2)) {
                        LineDrawer.drawLine((float)f, (float)f2, (float)f3, (float)f4, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f, (int)0);
                    }
                    if (IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)0, (int)1, (int)n2)) {
                        LineDrawer.drawLine((float)f3, (float)f4, (float)f5, (float)f6, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f, (int)0);
                    }
                    if (IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)0, (int)2, (int)n2)) {
                        LineDrawer.drawLine((float)f5, (float)f6, (float)f7, (float)f8, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f, (int)0);
                    }
                    if (IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)1, (int)2, (int)n2)) {
                        LineDrawer.drawLine((float)f7, (float)f8, (float)f9, (float)f10, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f, (int)0);
                    }
                    if (IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)2, (int)2, (int)n2)) {
                        LineDrawer.drawLine((float)f9, (float)f10, (float)f11, (float)f12, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f, (int)0);
                    }
                    if (IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)2, (int)1, (int)n2)) {
                        LineDrawer.drawLine((float)f11, (float)f12, (float)f13, (float)f14, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f, (int)0);
                    }
                    if (IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)2, (int)0, (int)n2)) {
                        LineDrawer.drawLine((float)f13, (float)f14, (float)f15, (float)f16, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f, (int)0);
                    }
                    if (!IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)1, (int)0, (int)n2)) continue;
                    LineDrawer.drawLine((float)f15, (float)f16, (float)f, (float)f2, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f, (int)0);
                }
            }
        }
    }

    private void CullFullyOccludedSquares(IsoGridStack isoGridStack, boolean[][][] blArray, boolean[][] blArray2) {
        int n;
        int n2 = 0;
        for (n = 1; n < MaxHeight + 1; ++n) {
            n2 += ((ArrayList)isoGridStack.Squares.get((int)n)).size();
        }
        if (n2 < 500) {
            return;
        }
        n = 0;
        for (int i = MaxHeight; i >= 0; --i) {
            GridStack = (ArrayList)isoGridStack.Squares.get((int)i);
            for (int j = GridStack.size() - 1; j >= 0; --j) {
                int n3;
                boolean bl;
                IsoGridSquare isoGridSquare = (IsoGridSquare)GridStack.get((int)j);
                int n4 = isoGridSquare.getX() - i * 3 - this.minX;
                int n5 = isoGridSquare.getY() - i * 3 - this.minY;
                if (n4 < 0 || n4 >= blArray2.length) {
                    GridStack.remove((int)j);
                    continue;
                }
                if (n5 < 0 || n5 >= blArray2[0].length) {
                    GridStack.remove((int)j);
                    continue;
                }
                if (i < MaxHeight) {
                    boolean bl2 = bl = !blArray2[n4][n5];
                    if (bl) {
                        bl = false;
                        if (n4 > 2) {
                            bl = n5 > 2 ? !blArray[n4 - 3][n5 - 3][0] || !blArray[n4 - 3][n5 - 3][1] || !blArray[n4 - 3][n5 - 2][0] || !blArray[n4 - 2][n5 - 3][1] || !blArray[n4 - 2][n5 - 2][0] || !blArray[n4 - 2][n5 - 2][1] || !blArray[n4 - 2][n5 - 1][0] || !blArray[n4 - 1][n5 - 2][0] || !blArray[n4 - 1][n5 - 1][1] || !blArray[n4 - 1][n5 - 1][0] || !blArray[n4 - 1][n5][0] || !blArray[n4][n5 - 1][1] || !blArray[n4][n5][0] || !blArray[n4][n5][1] : (n5 > 1 ? !blArray[n4 - 3][n5 - 2][0] || !blArray[n4 - 2][n5 - 2][0] || !blArray[n4 - 2][n5 - 2][1] || !blArray[n4 - 2][n5 - 1][0] || !blArray[n4 - 1][n5 - 2][0] || !blArray[n4 - 1][n5 - 1][1] || !blArray[n4 - 1][n5 - 1][0] || !blArray[n4 - 1][n5][0] || !blArray[n4][n5 - 1][1] || !blArray[n4][n5][0] || !blArray[n4][n5][1] : (n5 > 0 ? !blArray[n4 - 2][n5 - 1][0] || !blArray[n4 - 1][n5 - 1][1] || !blArray[n4 - 1][n5 - 1][0] || !blArray[n4 - 1][n5][0] || !blArray[n4][n5 - 1][1] || !blArray[n4][n5][0] || !blArray[n4][n5][1] : !blArray[n4 - 1][n5][0] || !blArray[n4][n5][0] || !blArray[n4][n5][1]));
                        } else if (n4 > 1) {
                            bl = n5 > 2 ? !blArray[n4 - 2][n5 - 3][1] || !blArray[n4 - 2][n5 - 2][0] || !blArray[n4 - 2][n5 - 2][1] || !blArray[n4 - 2][n5 - 1][0] || !blArray[n4 - 1][n5 - 2][0] || !blArray[n4 - 1][n5 - 1][1] || !blArray[n4 - 1][n5 - 1][0] || !blArray[n4 - 1][n5][0] || !blArray[n4][n5 - 1][1] || !blArray[n4][n5][0] || !blArray[n4][n5][1] : (n5 > 1 ? !blArray[n4 - 2][n5 - 2][0] || !blArray[n4 - 2][n5 - 2][1] || !blArray[n4 - 2][n5 - 1][0] || !blArray[n4 - 1][n5 - 2][0] || !blArray[n4 - 1][n5 - 1][1] || !blArray[n4 - 1][n5 - 1][0] || !blArray[n4 - 1][n5][0] || !blArray[n4][n5 - 1][1] || !blArray[n4][n5][0] || !blArray[n4][n5][1] : (n5 > 0 ? !blArray[n4 - 2][n5 - 1][0] || !blArray[n4 - 1][n5 - 1][1] || !blArray[n4 - 1][n5 - 1][0] || !blArray[n4 - 1][n5][0] || !blArray[n4][n5 - 1][1] || !blArray[n4][n5][0] || !blArray[n4][n5][1] : !blArray[n4 - 1][n5][0] || !blArray[n4][n5][0] || !blArray[n4][n5][1]));
                        } else if (n4 > 0) {
                            bl = n5 > 2 ? !blArray[n4 - 1][n5 - 2][0] || !blArray[n4 - 1][n5 - 1][1] || !blArray[n4 - 1][n5 - 1][0] || !blArray[n4 - 1][n5][0] || !blArray[n4][n5 - 1][1] || !blArray[n4][n5][0] || !blArray[n4][n5][1] : (n5 > 1 ? !blArray[n4 - 1][n5 - 2][0] || !blArray[n4 - 1][n5 - 1][1] || !blArray[n4 - 1][n5 - 1][0] || !blArray[n4 - 1][n5][0] || !blArray[n4][n5 - 1][1] || !blArray[n4][n5][0] || !blArray[n4][n5][1] : (n5 > 0 ? !blArray[n4 - 1][n5 - 1][1] || !blArray[n4 - 1][n5 - 1][0] || !blArray[n4 - 1][n5][0] || !blArray[n4][n5 - 1][1] || !blArray[n4][n5][0] || !blArray[n4][n5][1] : !blArray[n4 - 1][n5][0] || !blArray[n4][n5][0] || !blArray[n4][n5][1]));
                        } else if (n5 > 2) {
                            bl = !blArray[n4][n5 - 1][1] || !blArray[n4][n5][0] || !blArray[n4][n5][1];
                        } else if (n5 > 1) {
                            bl = !blArray[n4][n5 - 1][1] || !blArray[n4][n5][0] || !blArray[n4][n5][1];
                        } else if (n5 > 0) {
                            bl = !blArray[n4][n5 - 1][1] || !blArray[n4][n5][0] || !blArray[n4][n5][1];
                        } else {
                            boolean bl3 = bl = !blArray[n4][n5][0] || !blArray[n4][n5][1];
                        }
                    }
                    if (!bl) {
                        GridStack.remove((int)j);
                        blArray2[n4][n5] = true;
                        continue;
                    }
                }
                ++n;
                bl = IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)0, (int)1, (int)1) && isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.cutW);
                boolean bl4 = IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)1, (int)0, (int)1) && isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.cutN);
                boolean bl5 = false;
                if (bl || bl4) {
                    boolean bl6 = bl5 = ((float)isoGridSquare.x > IsoCamera.frameState.CamCharacterX || (float)isoGridSquare.y > IsoCamera.frameState.CamCharacterY) && isoGridSquare.z >= (int)IsoCamera.frameState.CamCharacterZ;
                    if (bl5) {
                        n3 = (int)(isoGridSquare.CachedScreenX - IsoCamera.frameState.OffX);
                        int n6 = (int)(isoGridSquare.CachedScreenY - IsoCamera.frameState.OffY);
                        if (n3 + 32 * Core.TileScale <= this.StencilX1 || n3 - 32 * Core.TileScale >= this.StencilX2 || n6 + 32 * Core.TileScale <= this.StencilY1 || n6 - 96 * Core.TileScale >= this.StencilY2) {
                            bl5 = false;
                        }
                    }
                }
                n3 = 0;
                if (bl && !bl5) {
                    ++n3;
                    if (n4 > 0) {
                        blArray[n4 - 1][n5][0] = true;
                        if (n5 > 0) {
                            blArray[n4 - 1][n5 - 1][1] = true;
                        }
                    }
                    if (n4 > 1 && n5 > 0) {
                        blArray[n4 - 2][n5 - 1][0] = true;
                        if (n5 > 1) {
                            blArray[n4 - 2][n5 - 2][1] = true;
                        }
                    }
                    if (n4 > 2 && n5 > 1) {
                        blArray[n4 - 3][n5 - 2][0] = true;
                        if (n5 > 2) {
                            blArray[n4 - 3][n5 - 3][1] = true;
                        }
                    }
                }
                if (bl4 && !bl5) {
                    ++n3;
                    if (n5 > 0) {
                        blArray[n4][n5 - 1][1] = true;
                        if (n4 > 0) {
                            blArray[n4 - 1][n5 - 1][0] = true;
                        }
                    }
                    if (n5 > 1 && n4 > 0) {
                        blArray[n4 - 1][n5 - 2][1] = true;
                        if (n4 > 1) {
                            blArray[n4 - 2][n5 - 2][0] = true;
                        }
                    }
                    if (n5 > 2 && n4 > 1) {
                        blArray[n4 - 2][n5 - 3][1] = true;
                        if (n4 > 2) {
                            blArray[n4 - 3][n5 - 3][0] = true;
                        }
                    }
                }
                if (IsoGridSquare.getMatrixBit((int)isoGridSquare.visionMatrix, (int)1, (int)1, (int)0)) {
                    ++n3;
                    blArray[n4][n5][0] = true;
                    blArray[n4][n5][1] = true;
                }
                if (n3 != 3) continue;
                blArray2[n4][n5] = true;
            }
        }
    }

    public void RenderFloorShading(int n) {
        Texture texture;
        if (!DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Floor.LightingOld.getValue() || DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Floor.Lighting.getValue()) {
            return;
        }
        if (n >= this.maxZ || PerformanceSettings.LightingFrameSkip >= 3) {
            return;
        }
        if (Core.bDebug && DebugOptions.instance.DebugDraw_SkipWorldShading.getValue()) {
            return;
        }
        if (texWhite == null) {
            texWhite = Texture.getWhite();
        }
        if ((texture = texWhite) == null) {
            return;
        }
        int n2 = IsoCamera.frameState.playerIndex;
        int n3 = (int)IsoCamera.frameState.OffX;
        int n4 = (int)IsoCamera.frameState.OffY;
        for (int i = 0; i < ShadedFloor.size(); ++i) {
            IsoGridSquare isoGridSquare = (IsoGridSquare)ShadedFloor.get((int)i);
            if (!isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.solidfloor)) continue;
            float f = 0.0f;
            float f2 = 0.0f;
            float f3 = 0.0f;
            if (isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.FloorHeightOneThird)) {
                f2 = -1.0f;
                f = -1.0f;
            } else if (isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.FloorHeightTwoThirds)) {
                f2 = -2.0f;
                f = -2.0f;
            }
            float f4 = IsoUtils.XToScreen((float)((float)isoGridSquare.getX() + f), (float)((float)isoGridSquare.getY() + f2), (float)((float)n + f3), (int)0);
            float f5 = IsoUtils.YToScreen((float)((float)isoGridSquare.getX() + f), (float)((float)isoGridSquare.getY() + f2), (float)((float)n + f3), (int)0);
            f4 -= (float)n3;
            f5 -= (float)n4;
            int n5 = isoGridSquare.getVertLight((int)0, (int)n2);
            int n6 = isoGridSquare.getVertLight((int)1, (int)n2);
            int n7 = isoGridSquare.getVertLight((int)2, (int)n2);
            int n8 = isoGridSquare.getVertLight((int)3, (int)n2);
            if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Floor.LightingDebug.getValue()) {
                n5 = -65536;
                n6 = -65536;
                n7 = -16776961;
                n8 = -16776961;
            }
            texture.renderdiamond((float)(f4 - (float)(32 * Core.TileScale)), (float)(f5 + (float)(16 * Core.TileScale)), (float)((float)(64 * Core.TileScale)), (float)((float)(32 * Core.TileScale)), (int)n8, (int)n5, (int)n6, (int)n7);
        }
    }

    public boolean IsPlayerWindowPeeking(int n) {
        return this.playerWindowPeekingRoomId[n] != -1;
    }

    public boolean CanBuildingSquareOccludePlayer(IsoGridSquare isoGridSquare, int n) {
        ArrayList arrayList = (ArrayList)this.playerOccluderBuildings.get((int)n);
        for (int i = 0; i < arrayList.size(); ++i) {
            IsoBuilding isoBuilding = (IsoBuilding)arrayList.get((int)i);
            int n2 = isoBuilding.getDef().getX();
            int n3 = isoBuilding.getDef().getY();
            int n4 = isoBuilding.getDef().getX2() - n2;
            int n5 = isoBuilding.getDef().getY2() - n3;
            this.buildingRectTemp.setBounds((int)(n2 - 1), (int)(n3 - 1), (int)(n4 + 2), (int)(n5 + 2));
            if (!this.buildingRectTemp.contains((int)isoGridSquare.getX(), (int)isoGridSquare.getY())) continue;
            return true;
        }
        return false;
    }

    public int GetEffectivePlayerRoomId() {
        int n = IsoCamera.frameState.playerIndex;
        int n2 = this.playerWindowPeekingRoomId[n];
        if (IsoPlayer.players[n] != null && IsoPlayer.players[n].isClimbing()) {
            n2 = -1;
        }
        if (n2 != -1) {
            return n2;
        }
        IsoGridSquare isoGridSquare = IsoPlayer.players[n].current;
        if (isoGridSquare != null) {
            return isoGridSquare.getRoomID();
        }
        return -1;
    }

    private boolean SetCutawayRoomsForPlayer() {
        int n = IsoCamera.frameState.playerIndex;
        IsoPlayer isoPlayer = IsoPlayer.players[n];
        ArrayList<Integer> arrayList = this.tempPrevPlayerCutawayRoomIDs;
        this.tempPrevPlayerCutawayRoomIDs = this.tempPlayerCutawayRoomIDs;
        this.tempPlayerCutawayRoomIDs = arrayList;
        this.tempPlayerCutawayRoomIDs.clear();
        IsoGridSquare isoGridSquare = isoPlayer.getSquare();
        if (isoGridSquare == null) {
            return false;
        }
        IsoBuilding isoBuilding = isoGridSquare.getBuilding();
        int n2 = isoGridSquare.getRoomID();
        boolean bl = false;
        if (n2 == -1) {
            if (this.playerWindowPeekingRoomId[n] != -1) {
                this.tempPlayerCutawayRoomIDs.add((Object)Integer.valueOf((int)this.playerWindowPeekingRoomId[n]));
            } else {
                bl = this.playerCutawaysDirty[n];
            }
        } else {
            int n3 = (int)(isoPlayer.getX() - 1.5f);
            int n4 = (int)(isoPlayer.getY() - 1.5f);
            int n5 = (int)(isoPlayer.getX() + 1.5f);
            int n6 = (int)(isoPlayer.getY() + 1.5f);
            for (int i = n3; i <= n5; ++i) {
                for (int j = n4; j <= n6; ++j) {
                    IsoGridSquare isoGridSquare2 = this.getGridSquare((int)i, (int)j, (int)isoGridSquare.getZ());
                    if (isoGridSquare2 == null) continue;
                    int n7 = isoGridSquare2.getRoomID();
                    if (!isoGridSquare2.getCanSee((int)n) || n7 == -1 || this.tempPlayerCutawayRoomIDs.contains((Object)Integer.valueOf((int)n7))) continue;
                    this.tempCutawaySqrVector.set((float)((float)isoGridSquare2.getX() + 0.5f - isoPlayer.getX()), (float)((float)isoGridSquare2.getY() + 0.5f - isoPlayer.getY()));
                    if (isoGridSquare != isoGridSquare2 && !(isoPlayer.getForwardDirection().dot((Vector2)this.tempCutawaySqrVector) > 0.0f)) continue;
                    this.tempPlayerCutawayRoomIDs.add((Object)Integer.valueOf((int)n7));
                }
            }
            Collections.sort(this.tempPlayerCutawayRoomIDs);
        }
        return bl || !this.tempPlayerCutawayRoomIDs.equals(this.tempPrevPlayerCutawayRoomIDs);
    }

    private boolean IsCutawaySquare(IsoGridSquare isoGridSquare, long l) {
        int n = IsoCamera.frameState.playerIndex;
        IsoPlayer isoPlayer = IsoPlayer.players[n];
        if (isoPlayer.current == null) {
            return false;
        }
        if (isoGridSquare == null) {
            return false;
        }
        IsoGridSquare isoGridSquare2 = isoPlayer.current;
        if (isoGridSquare2.getZ() != isoGridSquare.getZ()) {
            return false;
        }
        if (this.tempPlayerCutawayRoomIDs.isEmpty()) {
            IsoGridSquare isoGridSquare3 = isoGridSquare.nav[IsoDirections.N.index()];
            IsoGridSquare isoGridSquare4 = isoGridSquare.nav[IsoDirections.W.index()];
            if (this.IsCollapsibleBuildingSquare((IsoGridSquare)isoGridSquare)) {
                if (isoPlayer.getZ() == 0.0f) {
                    return true;
                }
                if (isoGridSquare.getBuilding() != null && (isoGridSquare2.getX() < isoGridSquare.getBuilding().def.x || isoGridSquare2.getY() < isoGridSquare.getBuilding().def.y)) {
                    return true;
                }
                IsoGridSquare isoGridSquare5 = isoGridSquare;
                for (int i = 0; i < 3 && (isoGridSquare5 = isoGridSquare5.nav[IsoDirections.NW.index()]) != null; ++i) {
                    if (!isoGridSquare5.isCanSee((int)n)) continue;
                    return true;
                }
            }
            if (isoGridSquare3 != null && isoGridSquare3.getRoomID() == -1 && isoGridSquare4 != null && isoGridSquare4.getRoomID() == -1) {
                return this.DoesSquareHaveValidCutaways((IsoGridSquare)isoGridSquare2, (IsoGridSquare)isoGridSquare, (int)n, (long)l);
            }
        } else {
            int n2;
            int n3;
            IsoGridSquare isoGridSquare6 = isoGridSquare.nav[IsoDirections.N.index()];
            IsoGridSquare isoGridSquare7 = isoGridSquare.nav[IsoDirections.E.index()];
            IsoGridSquare isoGridSquare8 = isoGridSquare.nav[IsoDirections.S.index()];
            IsoGridSquare isoGridSquare9 = isoGridSquare.nav[IsoDirections.W.index()];
            IsoGridSquare isoGridSquare10 = isoGridSquare2.nav[IsoDirections.N.index()];
            IsoGridSquare isoGridSquare11 = isoGridSquare2.nav[IsoDirections.E.index()];
            IsoGridSquare isoGridSquare12 = isoGridSquare2.nav[IsoDirections.S.index()];
            IsoGridSquare isoGridSquare13 = isoGridSquare2.nav[IsoDirections.W.index()];
            boolean bl = false;
            boolean bl2 = false;
            for (n3 = 0; n3 < 8; ++n3) {
                if (isoGridSquare.nav[n3] == null || isoGridSquare.nav[n3].getRoomID() == isoGridSquare.getRoomID()) continue;
                bl = true;
                break;
            }
            if (!this.tempPlayerCutawayRoomIDs.contains((Object)Integer.valueOf((int)isoGridSquare.getRoomID()))) {
                bl2 = true;
            }
            if (bl || bl2 || isoGridSquare.getWall() != null) {
                IsoGridSquare isoGridSquare14 = isoGridSquare;
                for (n2 = 0; n2 < 3 && (isoGridSquare14 = isoGridSquare14.nav[IsoDirections.NW.index()]) != null; ++n2) {
                    if (isoGridSquare14.getRoomID() == -1 || !this.tempPlayerCutawayRoomIDs.contains((Object)Integer.valueOf((int)isoGridSquare14.getRoomID()))) continue;
                    if ((bl || bl2) && isoGridSquare14.getCanSee((int)n)) {
                        return true;
                    }
                    if (isoGridSquare.getWall() == null || !isoGridSquare14.isCouldSee((int)n)) continue;
                    return true;
                }
            }
            if (isoGridSquare6 != null && isoGridSquare9 != null && (isoGridSquare6.getThumpableWallOrHoppable((boolean)false) != null || isoGridSquare9.getThumpableWallOrHoppable((boolean)true) != null || isoGridSquare.getThumpableWallOrHoppable((boolean)true) != null || isoGridSquare.getThumpableWallOrHoppable((boolean)false) != null)) {
                return this.DoesSquareHaveValidCutaways((IsoGridSquare)isoGridSquare2, (IsoGridSquare)isoGridSquare, (int)n, (long)l);
            }
            if (isoGridSquare2.getRoomID() == -1 && (isoGridSquare10 != null && isoGridSquare10.getRoomID() != -1 || isoGridSquare11 != null && isoGridSquare11.getRoomID() != -1 || isoGridSquare12 != null && isoGridSquare12.getRoomID() != -1 || isoGridSquare13 != null && isoGridSquare13.getRoomID() != -1)) {
                n3 = isoGridSquare2.x - isoGridSquare.x;
                n2 = isoGridSquare2.y - isoGridSquare.y;
                if (n3 < 0 && n2 < 0) {
                    if (n3 >= -3) {
                        if (n2 >= -3) {
                            return true;
                        }
                        if (isoGridSquare6 != null && isoGridSquare8 != null && isoGridSquare.getWall((boolean)false) != null && isoGridSquare6.getWall((boolean)false) != null && isoGridSquare8.getWall((boolean)false) != null && isoGridSquare8.getPlayerCutawayFlag((int)n, (long)l)) {
                            return true;
                        }
                    } else if (isoGridSquare7 != null && isoGridSquare9 != null) {
                        if (isoGridSquare.getWall((boolean)true) != null && isoGridSquare9.getWall((boolean)true) != null && isoGridSquare7.getWall((boolean)true) != null && isoGridSquare7.getPlayerCutawayFlag((int)n, (long)l)) {
                            return true;
                        }
                        if (isoGridSquare.getWall((boolean)true) != null && isoGridSquare9.getWall((boolean)true) != null && isoGridSquare7.getWall((boolean)true) != null && isoGridSquare7.getPlayerCutawayFlag((int)n, (long)l)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean DoesSquareHaveValidCutaways(IsoGridSquare isoGridSquare, IsoGridSquare isoGridSquare2, int n, long l) {
        int n2;
        IsoGridSquare isoGridSquare3 = isoGridSquare2.nav[IsoDirections.N.index()];
        IsoGridSquare isoGridSquare4 = isoGridSquare2.nav[IsoDirections.E.index()];
        IsoGridSquare isoGridSquare5 = isoGridSquare2.nav[IsoDirections.S.index()];
        IsoGridSquare isoGridSquare6 = isoGridSquare2.nav[IsoDirections.W.index()];
        IsoObject isoObject = isoGridSquare2.getWall((boolean)true);
        IsoObject isoObject2 = isoGridSquare2.getWall((boolean)false);
        IsoObject isoObject3 = null;
        IsoObject isoObject4 = null;
        if (isoGridSquare3 != null && isoGridSquare3.nav[IsoDirections.W.index()] != null && isoGridSquare3.nav[IsoDirections.W.index()].getRoomID() == isoGridSquare3.getRoomID()) {
            isoObject4 = isoGridSquare3.getWall((boolean)false);
        }
        if (isoGridSquare6 != null && isoGridSquare6.nav[IsoDirections.N.index()] != null && isoGridSquare6.nav[IsoDirections.N.index()].getRoomID() == isoGridSquare6.getRoomID()) {
            isoObject3 = isoGridSquare6.getWall((boolean)true);
        }
        if (isoObject2 != null || isoObject != null || isoObject4 != null || isoObject3 != null) {
            IsoGridSquare isoGridSquare7 = isoGridSquare2.nav[IsoDirections.NW.index()];
            for (n2 = 0; n2 < 2 && isoGridSquare7 != null && isoGridSquare7.getRoomID() == isoGridSquare.getRoomID(); ++n2) {
                IsoGridSquare isoGridSquare8 = isoGridSquare7.nav[IsoDirections.S.index()];
                IsoGridSquare isoGridSquare9 = isoGridSquare7.nav[IsoDirections.E.index()];
                if (isoGridSquare8 != null && isoGridSquare8.getBuilding() != null || isoGridSquare9 != null && isoGridSquare9.getBuilding() != null) break;
                if (isoGridSquare7.isCanSee((int)n) && isoGridSquare7.isCouldSee((int)n) && isoGridSquare7.DistTo((IsoGridSquare)isoGridSquare) <= (float)(6 - (n2 + 1))) {
                    return true;
                }
                if (isoGridSquare7.getBuilding() != null) continue;
                isoGridSquare7 = isoGridSquare7.nav[IsoDirections.NW.index()];
            }
        }
        int n3 = isoGridSquare.x - isoGridSquare2.x;
        n2 = isoGridSquare.y - isoGridSquare2.y;
        if (isoObject != null && isoObject.sprite.name.contains((CharSequence)"fencing") || isoObject2 != null && isoObject2.sprite.name.contains((CharSequence)"fencing")) {
            if (isoObject != null && isoObject3 != null && n2 >= -6 && n2 < 0) {
                return true;
            }
            if (isoObject2 != null && isoObject4 != null && n3 >= -6 && n3 < 0) {
                return true;
            }
        } else if (!(!(isoGridSquare2.DistTo((IsoGridSquare)isoGridSquare) <= 6.0f) || isoGridSquare2.nav[IsoDirections.NW.index()] == null || isoGridSquare2.nav[IsoDirections.NW.index()].getRoomID() != isoGridSquare2.getRoomID() || isoGridSquare2.getWall((boolean)true) != null && isoGridSquare2.getWall((boolean)true) != isoObject || isoGridSquare2.getWall((boolean)false) != null && isoGridSquare2.getWall((boolean)false) != isoObject2)) {
            if (isoGridSquare5 != null && isoGridSquare3 != null && n2 != 0) {
                if (n2 > 0 && isoObject2 != null && isoGridSquare5.getWall((boolean)false) != null && isoGridSquare3.getWall((boolean)false) != null && isoGridSquare5.getPlayerCutawayFlag((int)n, (long)l)) {
                    return true;
                }
                if (n2 < 0 && isoObject2 != null && isoGridSquare3.getWall((boolean)false) != null && isoGridSquare3.getPlayerCutawayFlag((int)n, (long)l)) {
                    return true;
                }
            }
            if (isoGridSquare4 != null && isoGridSquare6 != null && n3 != 0) {
                if (n3 > 0 && isoObject != null && isoGridSquare4.getWall((boolean)true) != null && isoGridSquare6.getWall((boolean)true) != null && isoGridSquare4.getPlayerCutawayFlag((int)n, (long)l)) {
                    return true;
                }
                if (n3 < 0 && isoObject != null && isoGridSquare6.getWall((boolean)true) != null && isoGridSquare6.getPlayerCutawayFlag((int)n, (long)l)) {
                    return true;
                }
            }
        }
        if (isoGridSquare2 == isoGridSquare && isoGridSquare2.nav[IsoDirections.NW.index()] != null && isoGridSquare2.nav[IsoDirections.NW.index()].getRoomID() == isoGridSquare2.getRoomID()) {
            if (isoObject != null && isoGridSquare3 != null && isoGridSquare3.getWall((boolean)false) == null && isoGridSquare3.isCanSee((int)n) && isoGridSquare3.isCouldSee((int)n)) {
                return true;
            }
            if (isoObject2 != null && isoGridSquare6 != null && isoGridSquare6.getWall((boolean)true) != null && isoGridSquare6.isCanSee((int)n) && isoGridSquare6.isCouldSee((int)n)) {
                return true;
            }
        }
        if (isoGridSquare3 != null && isoGridSquare6 != null && n3 != 0 && n2 != 0 && isoObject4 != null && isoObject3 != null && isoGridSquare3.getPlayerCutawayFlag((int)n, (long)l) && isoGridSquare6.getPlayerCutawayFlag((int)n, (long)l)) {
            return true;
        }
        return n3 < 0 && n3 >= -6 && n2 < 0 && n2 >= -6 && (isoObject2 != null && isoGridSquare2.getWall((boolean)true) == null || isoObject != null && isoGridSquare2.getWall((boolean)false) == null);
    }

    private boolean IsCollapsibleBuildingSquare(IsoGridSquare isoGridSquare) {
        BuildingDef buildingDef;
        IsoBuilding isoBuilding;
        int n;
        int n2;
        if (isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.forceRender)) {
            return false;
        }
        for (n2 = 0; n2 < 4; ++n2) {
            int n3 = 500;
            for (n = 0; n < n3 && this.playerOccluderBuildingsArr[n2] != null && (isoBuilding = this.playerOccluderBuildingsArr[n2][n]) != null; ++n) {
                buildingDef = isoBuilding.getDef();
                if (this.collapsibleBuildingSquareAlgorithm((BuildingDef)buildingDef, (IsoGridSquare)isoGridSquare, (IsoGridSquare)IsoPlayer.players[n2].getSquare())) {
                    return true;
                }
                if (isoGridSquare.getY() - buildingDef.getY2() == 1 && isoGridSquare.getWall((boolean)true) != null) {
                    return true;
                }
                if (isoGridSquare.getX() - buildingDef.getX2() != 1 || isoGridSquare.getWall((boolean)false) == null) continue;
                return true;
            }
        }
        n2 = IsoCamera.frameState.playerIndex;
        IsoPlayer isoPlayer = IsoPlayer.players[n2];
        if (isoPlayer.getVehicle() != null) {
            return false;
        }
        for (n = 0; n < 500 && this.zombieOccluderBuildingsArr[n2] != null && (isoBuilding = this.zombieOccluderBuildingsArr[n2][n]) != null; ++n) {
            buildingDef = isoBuilding.getDef();
            if (!this.collapsibleBuildingSquareAlgorithm((BuildingDef)buildingDef, (IsoGridSquare)isoGridSquare, (IsoGridSquare)isoPlayer.getSquare())) continue;
            return true;
        }
        for (n = 0; n < 500 && this.otherOccluderBuildingsArr[n2] != null && (isoBuilding = this.otherOccluderBuildingsArr[n2][n]) != null; ++n) {
            buildingDef = isoBuilding.getDef();
            if (!this.collapsibleBuildingSquareAlgorithm((BuildingDef)buildingDef, (IsoGridSquare)isoGridSquare, (IsoGridSquare)isoPlayer.getSquare())) continue;
            return true;
        }
        return false;
    }

    private boolean collapsibleBuildingSquareAlgorithm(BuildingDef buildingDef, IsoGridSquare isoGridSquare, IsoGridSquare isoGridSquare2) {
        int n = buildingDef.getX();
        int n2 = buildingDef.getY();
        int n3 = buildingDef.getX2() - n;
        int n4 = buildingDef.getY2() - n2;
        this.buildingRectTemp.setBounds((int)n, (int)n2, (int)n3, (int)n4);
        if (isoGridSquare2.getRoomID() == -1 && this.buildingRectTemp.contains((int)isoGridSquare2.getX(), (int)isoGridSquare2.getY())) {
            this.buildingRectTemp.setBounds((int)(n - 1), (int)(n2 - 1), (int)(n3 + 2), (int)(n4 + 2));
            IsoGridSquare isoGridSquare3 = isoGridSquare.nav[IsoDirections.N.index()];
            IsoGridSquare isoGridSquare4 = isoGridSquare.nav[IsoDirections.W.index()];
            IsoGridSquare isoGridSquare5 = isoGridSquare.nav[IsoDirections.NW.index()];
            if (isoGridSquare5 == null || isoGridSquare3 == null || isoGridSquare4 == null) {
                return false;
            }
            boolean bl = isoGridSquare.getRoomID() == -1;
            boolean bl2 = isoGridSquare3.getRoomID() == -1;
            boolean bl3 = isoGridSquare4.getRoomID() == -1;
            boolean bl4 = isoGridSquare5.getRoomID() == -1;
            boolean bl5 = isoGridSquare2.getY() < isoGridSquare.getY();
            boolean bl6 = isoGridSquare2.getX() < isoGridSquare.getX();
            return this.buildingRectTemp.contains((int)isoGridSquare.getX(), (int)isoGridSquare.getY()) && (isoGridSquare2.getZ() < isoGridSquare.getZ() || bl && (!bl2 && bl5 || !bl3 && bl6) || bl && bl2 && bl3 && !bl4 || !bl && (bl4 || bl2 == bl3 || bl2 && bl6 || bl3 && bl5));
        }
        this.buildingRectTemp.setBounds((int)(n - 1), (int)(n2 - 1), (int)(n3 + 2), (int)(n4 + 2));
        return this.buildingRectTemp.contains((int)isoGridSquare.getX(), (int)isoGridSquare.getY());
    }

    private boolean IsDissolvedSquare(IsoGridSquare isoGridSquare, int n) {
        IsoPlayer isoPlayer = IsoPlayer.players[n];
        if (isoPlayer.current == null) {
            return false;
        }
        IsoGridSquare isoGridSquare2 = isoPlayer.current;
        if (isoGridSquare2.getZ() >= isoGridSquare.getZ()) {
            return false;
        }
        if (!PerformanceSettings.NewRoofHiding) {
            return this.bHideFloors[n] && isoGridSquare.getZ() >= this.maxZ;
        }
        if (isoGridSquare.getZ() > this.hidesOrphanStructuresAbove) {
            IsoGridSquare isoGridSquare3;
            IsoBuilding isoBuilding = isoGridSquare.getBuilding();
            if (isoBuilding == null) {
                isoBuilding = isoGridSquare.roofHideBuilding;
            }
            for (int i = isoGridSquare.getZ() - 1; i >= 0 && isoBuilding == null; --i) {
                isoGridSquare3 = this.getGridSquare((int)isoGridSquare.x, (int)isoGridSquare.y, (int)i);
                if (isoGridSquare3 == null || (isoBuilding = isoGridSquare3.getBuilding()) != null) continue;
                isoBuilding = isoGridSquare3.roofHideBuilding;
            }
            if (isoBuilding == null) {
                IsoGridSquare isoGridSquare4;
                if (isoGridSquare.isSolidFloor()) {
                    return true;
                }
                IsoGridSquare isoGridSquare5 = isoGridSquare.nav[IsoDirections.N.index()];
                if (isoGridSquare5 != null && isoGridSquare5.getBuilding() == null) {
                    if (isoGridSquare5.getPlayerBuiltFloor() != null) {
                        return true;
                    }
                    if (isoGridSquare5.HasStairsBelow()) {
                        return true;
                    }
                }
                if ((isoGridSquare3 = isoGridSquare.nav[IsoDirections.W.index()]) != null && isoGridSquare3.getBuilding() == null) {
                    if (isoGridSquare3.getPlayerBuiltFloor() != null) {
                        return true;
                    }
                    if (isoGridSquare3.HasStairsBelow()) {
                        return true;
                    }
                }
                if (isoGridSquare.Is((IsoFlagType)IsoFlagType.WallSE) && (isoGridSquare4 = isoGridSquare.nav[IsoDirections.NW.index()]) != null && isoGridSquare4.getBuilding() == null) {
                    if (isoGridSquare4.getPlayerBuiltFloor() != null) {
                        return true;
                    }
                    if (isoGridSquare4.HasStairsBelow()) {
                        return true;
                    }
                }
            }
        }
        return this.IsCollapsibleBuildingSquare((IsoGridSquare)isoGridSquare);
    }

    private int GetBuildingHeightAt(IsoBuilding isoBuilding, int n, int n2, int n3) {
        for (int i = MaxHeight; i > n3; --i) {
            IsoGridSquare isoGridSquare = this.getGridSquare((int)n, (int)n2, (int)i);
            if (isoGridSquare == null || isoGridSquare.getBuilding() != isoBuilding) continue;
            return i;
        }
        return n3;
    }

    private void updateSnow(int n) {
        if (this.snowGridCur == null) {
            this.snowGridCur = new SnowGrid((IsoCell)this, (int)n);
            this.snowGridPrev = new SnowGrid((IsoCell)this, (int)0);
            return;
        }
        if (n != this.snowGridCur.frac) {
            this.snowGridPrev.init((int)this.snowGridCur.frac);
            this.snowGridCur.init((int)n);
            this.snowFadeTime = System.currentTimeMillis();
            DebugLog.log((String)("snow from " + this.snowGridPrev.frac + " to " + this.snowGridCur.frac));
        }
    }

    public void setSnowTarget(int n) {
        if (!SandboxOptions.instance.EnableSnowOnGround.getValue()) {
            n = 0;
        }
        this.snowFracTarget = n;
    }

    public boolean gridSquareIsSnow(int n, int n2, int n3) {
        IsoGridSquare isoGridSquare = this.getGridSquare((int)n, (int)n2, (int)n3);
        if (isoGridSquare != null) {
            if (!isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.solidfloor)) {
                return false;
            }
            if (isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.water)) {
                return false;
            }
            if (!isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.exterior) || isoGridSquare.room != null || isoGridSquare.isInARoom()) {
                return false;
            }
            int n4 = isoGridSquare.getX() % this.snowGridCur.w;
            int n5 = isoGridSquare.getY() % this.snowGridCur.h;
            return this.snowGridCur.check((int)n4, (int)n5);
        }
        return false;
    }

    private void RenderSnow(int n) {
        if (!DebugOptions.instance.Weather.Snow.getValue()) {
            return;
        }
        this.updateSnow((int)this.snowFracTarget);
        SnowGrid snowGrid = this.snowGridCur;
        if (snowGrid == null) {
            return;
        }
        SnowGrid snowGrid2 = this.snowGridPrev;
        if (snowGrid.frac <= 0 && snowGrid2.frac <= 0) {
            return;
        }
        float f = 1.0f;
        float f2 = 0.0f;
        long l = System.currentTimeMillis();
        long l2 = l - this.snowFadeTime;
        if ((float)l2 < this.snowTransitionTime) {
            float f3;
            f = f3 = (float)l2 / this.snowTransitionTime;
            f2 = 1.0f - f;
        }
        Shader shader = null;
        if (DebugOptions.instance.Terrain.RenderTiles.UseShaders.getValue()) {
            shader = m_floorRenderShader;
        }
        FloorShaperAttachedSprites.instance.setShore((boolean)false);
        FloorShaperDiamond.instance.setShore((boolean)false);
        IndieGL.StartShader((Shader)shader, (int)IsoCamera.frameState.playerIndex);
        int n2 = (int)IsoCamera.frameState.OffX;
        int n3 = (int)IsoCamera.frameState.OffY;
        for (int i = 0; i < SolidFloor.size(); ++i) {
            int n4;
            IsoGridSquare isoGridSquare = (IsoGridSquare)SolidFloor.get((int)i);
            if (isoGridSquare.room != null || !isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.exterior) || !isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.solidfloor)) continue;
            if (isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.water)) {
                n4 = IsoCell.getShoreInt((IsoGridSquare)isoGridSquare);
                if (n4 == 0) {
                    continue;
                }
            } else {
                n4 = 0;
            }
            int n5 = isoGridSquare.getX() % snowGrid.w;
            int n6 = isoGridSquare.getY() % snowGrid.h;
            float f4 = IsoUtils.XToScreen((float)((float)isoGridSquare.getX()), (float)((float)isoGridSquare.getY()), (float)((float)n), (int)0);
            float f5 = IsoUtils.YToScreen((float)((float)isoGridSquare.getX()), (float)((float)isoGridSquare.getY()), (float)((float)n), (int)0);
            f4 -= (float)n2;
            f5 -= (float)n3;
            float f6 = (float)(32 * Core.TileScale);
            float f7 = (float)(96 * Core.TileScale);
            f4 -= f6;
            f5 -= f7;
            int n7 = IsoCamera.frameState.playerIndex;
            int n8 = isoGridSquare.getVertLight((int)0, (int)n7);
            int n9 = isoGridSquare.getVertLight((int)1, (int)n7);
            int n10 = isoGridSquare.getVertLight((int)2, (int)n7);
            int n11 = isoGridSquare.getVertLight((int)3, (int)n7);
            if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Floor.LightingDebug.getValue()) {
                n8 = -65536;
                n9 = -65536;
                n10 = -16776961;
                n11 = -16776961;
            }
            FloorShaperAttachedSprites.instance.setVertColors((int)n8, (int)n9, (int)n10, (int)n11);
            FloorShaperDiamond.instance.setVertColors((int)n8, (int)n9, (int)n10, (int)n11);
            for (n7 = 0; n7 < 2; ++n7) {
                if (f2 > f) {
                    this.renderSnowTileGeneral((SnowGrid)snowGrid, (float)f, (IsoGridSquare)isoGridSquare, (int)n4, (int)n5, (int)n6, (int)((int)f4), (int)((int)f5), (int)n7);
                    this.renderSnowTileGeneral((SnowGrid)snowGrid2, (float)f2, (IsoGridSquare)isoGridSquare, (int)n4, (int)n5, (int)n6, (int)((int)f4), (int)((int)f5), (int)n7);
                    continue;
                }
                this.renderSnowTileGeneral((SnowGrid)snowGrid2, (float)f2, (IsoGridSquare)isoGridSquare, (int)n4, (int)n5, (int)n6, (int)((int)f4), (int)((int)f5), (int)n7);
                this.renderSnowTileGeneral((SnowGrid)snowGrid, (float)f, (IsoGridSquare)isoGridSquare, (int)n4, (int)n5, (int)n6, (int)((int)f4), (int)((int)f5), (int)n7);
            }
        }
        IndieGL.StartShader(null);
    }

    private void renderSnowTileGeneral(SnowGrid snowGrid, float f, IsoGridSquare isoGridSquare, int n, int n2, int n3, int n4, int n5, int n6) {
        if (f <= 0.0f) {
            return;
        }
        Texture texture = snowGrid.grid[n2][n3][n6];
        if (texture == null) {
            return;
        }
        if (n6 == 0) {
            this.renderSnowTile((SnowGrid)snowGrid, (int)n2, (int)n3, (int)n6, (IsoGridSquare)isoGridSquare, (int)n, (Texture)texture, (int)n4, (int)n5, (float)f);
        } else if (n == 0) {
            byte by = snowGrid.gridType[n2][n3][n6];
            this.renderSnowTileBase((Texture)texture, (int)n4, (int)n5, (float)f, (by < this.m_snowFirstNonSquare ? 1 : 0) != 0);
        }
    }

    private void renderSnowTileBase(Texture texture, int n, int n2, float f, boolean bl) {
        FloorShaperDiamond floorShaperDiamond = bl ? FloorShaperDiamond.instance : FloorShaperAttachedSprites.instance;
        floorShaperDiamond.setAlpha4((float)f);
        texture.render((float)((float)n), (float)((float)n2), (float)((float)texture.getWidth()), (float)((float)texture.getHeight()), (float)1.0f, (float)1.0f, (float)1.0f, (float)f, (Consumer)floorShaperDiamond);
    }

    private void renderSnowTile(SnowGrid snowGrid, int n, int n2, int n3, IsoGridSquare isoGridSquare, int n4, Texture texture, int n5, int n6, float f) {
        boolean bl;
        if (n4 == 0) {
            byte by = snowGrid.gridType[n][n2][n3];
            this.renderSnowTileBase((Texture)texture, (int)n5, (int)n6, (float)f, (by < this.m_snowFirstNonSquare ? 1 : 0) != 0);
            return;
        }
        int n7 = 0;
        boolean bl2 = snowGrid.check((int)n, (int)n2);
        boolean bl3 = (n4 & 1) == 1 && (bl2 || snowGrid.check((int)n, (int)(n2 - 1)));
        boolean bl4 = (n4 & 2) == 2 && (bl2 || snowGrid.check((int)(n + 1), (int)n2));
        boolean bl5 = (n4 & 4) == 4 && (bl2 || snowGrid.check((int)n, (int)(n2 + 1)));
        boolean bl6 = bl = (n4 & 8) == 8 && (bl2 || snowGrid.check((int)(n - 1), (int)n2));
        if (bl3) {
            ++n7;
        }
        if (bl5) {
            ++n7;
        }
        if (bl4) {
            ++n7;
        }
        if (bl) {
            ++n7;
        }
        SnowGridTiles snowGridTiles = null;
        SnowGridTiles snowGridTiles2 = null;
        boolean bl7 = false;
        if (n7 == 0) {
            return;
        }
        if (n7 == 1) {
            if (bl3) {
                snowGridTiles = this.snowGridTiles_Strip[0];
            } else if (bl5) {
                snowGridTiles = this.snowGridTiles_Strip[1];
            } else if (bl4) {
                snowGridTiles = this.snowGridTiles_Strip[3];
            } else if (bl) {
                snowGridTiles = this.snowGridTiles_Strip[2];
            }
        } else if (n7 == 2) {
            if (bl3 && bl5) {
                snowGridTiles = this.snowGridTiles_Strip[0];
                snowGridTiles2 = this.snowGridTiles_Strip[1];
            } else if (bl4 && bl) {
                snowGridTiles = this.snowGridTiles_Strip[2];
                snowGridTiles2 = this.snowGridTiles_Strip[3];
            } else if (bl3) {
                snowGridTiles = this.snowGridTiles_Edge[bl ? 0 : 3];
            } else if (bl5) {
                snowGridTiles = this.snowGridTiles_Edge[bl ? 2 : 1];
            } else if (bl) {
                snowGridTiles = this.snowGridTiles_Edge[bl3 ? 0 : 2];
            } else if (bl4) {
                snowGridTiles = this.snowGridTiles_Edge[bl3 ? 3 : 1];
            }
        } else if (n7 == 3) {
            if (!bl3) {
                snowGridTiles = this.snowGridTiles_Cove[1];
            } else if (!bl5) {
                snowGridTiles = this.snowGridTiles_Cove[0];
            } else if (!bl4) {
                snowGridTiles = this.snowGridTiles_Cove[2];
            } else if (!bl) {
                snowGridTiles = this.snowGridTiles_Cove[3];
            }
            bl7 = true;
        } else if (n7 == 4) {
            snowGridTiles = this.snowGridTiles_Enclosed;
            bl7 = true;
        }
        if (snowGridTiles != null) {
            int n8 = (isoGridSquare.getX() + isoGridSquare.getY()) % snowGridTiles.size();
            texture = snowGridTiles.get((int)n8);
            if (texture != null) {
                this.renderSnowTileBase((Texture)texture, (int)n5, (int)n6, (float)f, (boolean)bl7);
            }
            if (snowGridTiles2 != null && (texture = snowGridTiles2.get((int)n8)) != null) {
                this.renderSnowTileBase((Texture)texture, (int)n5, (int)n6, (float)f, (boolean)false);
            }
        }
    }

    private static int getShoreInt(IsoGridSquare isoGridSquare) {
        int n = 0;
        if (IsoCell.isSnowShore((IsoGridSquare)isoGridSquare, (int)0, (int)-1)) {
            n |= 1;
        }
        if (IsoCell.isSnowShore((IsoGridSquare)isoGridSquare, (int)1, (int)0)) {
            n |= 2;
        }
        if (IsoCell.isSnowShore((IsoGridSquare)isoGridSquare, (int)0, (int)1)) {
            n |= 4;
        }
        if (IsoCell.isSnowShore((IsoGridSquare)isoGridSquare, (int)-1, (int)0)) {
            n |= 8;
        }
        return n;
    }

    private static boolean isSnowShore(IsoGridSquare isoGridSquare, int n, int n2) {
        IsoGridSquare isoGridSquare2 = IsoWorld.instance.getCell().getGridSquare((int)(isoGridSquare.getX() + n), (int)(isoGridSquare.getY() + n2), (int)0);
        return isoGridSquare2 != null && !isoGridSquare2.getProperties().Is((IsoFlagType)IsoFlagType.water);
    }

    public IsoBuilding getClosestBuildingExcept(IsoGameCharacter isoGameCharacter, IsoRoom isoRoom) {
        IsoBuilding isoBuilding = null;
        float f = 1000000.0f;
        for (int i = 0; i < this.BuildingList.size(); ++i) {
            IsoBuilding isoBuilding2 = (IsoBuilding)this.BuildingList.get((int)i);
            for (int j = 0; j < isoBuilding2.Exits.size(); ++j) {
                float f2 = isoGameCharacter.DistTo((int)((IsoRoomExit)isoBuilding2.Exits.get((int)j)).x, (int)((IsoRoomExit)isoBuilding2.Exits.get((int)j)).y);
                if (!(f2 < f) || isoRoom != null && isoRoom.building == isoBuilding2) continue;
                isoBuilding = isoBuilding2;
                f = f2;
            }
        }
        return isoBuilding;
    }

    public int getDangerScore(int n, int n2) {
        if (n < 0 || n2 < 0 || n >= this.width || n2 >= this.height) {
            return 1000000;
        }
        return this.DangerScore.getValue((int)n, (int)n2);
    }

    private void ObjectDeletionAddition() {
        IsoMovingObject isoMovingObject;
        int n;
        for (n = 0; n < this.removeList.size(); ++n) {
            isoMovingObject = (IsoMovingObject)this.removeList.get((int)n);
            if (isoMovingObject instanceof IsoZombie) {
                VirtualZombieManager.instance.RemoveZombie((IsoZombie)((IsoZombie)isoMovingObject));
            }
            if (isoMovingObject instanceof IsoPlayer && !((IsoPlayer)isoMovingObject).isDead()) continue;
            MovingObjectUpdateScheduler.instance.removeObject((IsoMovingObject)isoMovingObject);
            this.ObjectList.remove((Object)isoMovingObject);
            if (isoMovingObject.getCurrentSquare() != null) {
                isoMovingObject.getCurrentSquare().getMovingObjects().remove((Object)isoMovingObject);
            }
            if (isoMovingObject.getLastSquare() == null) continue;
            isoMovingObject.getLastSquare().getMovingObjects().remove((Object)isoMovingObject);
        }
        this.removeList.clear();
        for (n = 0; n < this.addList.size(); ++n) {
            isoMovingObject = (IsoMovingObject)this.addList.get((int)n);
            this.ObjectList.add((Object)isoMovingObject);
        }
        this.addList.clear();
        for (n = 0; n < this.addVehicles.size(); ++n) {
            isoMovingObject = (BaseVehicle)this.addVehicles.get((int)n);
            if (!this.ObjectList.contains((Object)isoMovingObject)) {
                this.ObjectList.add((Object)isoMovingObject);
            }
            if (this.vehicles.contains((Object)isoMovingObject)) continue;
            this.vehicles.add((Object)isoMovingObject);
        }
        this.addVehicles.clear();
    }

    private void ProcessItems(Iterator iterator) {
        InventoryItem inventoryItem;
        int n;
        int n2 = this.ProcessItems.size();
        for (n = 0; n < n2; ++n) {
            inventoryItem = (InventoryItem)this.ProcessItems.get((int)n);
            inventoryItem.update();
            if (!inventoryItem.finishupdate()) continue;
            this.ProcessItemsRemove.add((Object)inventoryItem);
        }
        n2 = this.ProcessWorldItems.size();
        for (n = 0; n < n2; ++n) {
            inventoryItem = (IsoWorldInventoryObject)this.ProcessWorldItems.get((int)n);
            inventoryItem.update();
            if (!inventoryItem.finishupdate()) continue;
            this.ProcessWorldItemsRemove.add((Object)inventoryItem);
        }
    }

    private void ProcessIsoObject() {
        this.ProcessIsoObject.removeAll(this.ProcessIsoObjectRemove);
        this.ProcessIsoObjectRemove.clear();
        int n = this.ProcessIsoObject.size();
        for (int i = 0; i < n; ++i) {
            IsoObject isoObject = (IsoObject)this.ProcessIsoObject.get((int)i);
            if (isoObject == null) continue;
            isoObject.update();
            if (n <= this.ProcessIsoObject.size()) continue;
            --i;
            --n;
        }
    }

    private void ProcessObjects(Iterator iterator) {
        MovingObjectUpdateScheduler.instance.update();
        for (int i = 0; i < this.ZombieList.size(); ++i) {
            IsoZombie isoZombie = (IsoZombie)this.ZombieList.get((int)i);
            isoZombie.updateVocalProperties();
        }
    }

    private void ProcessRemoveItems(Iterator iterator) {
        this.ProcessItems.removeAll(this.ProcessItemsRemove);
        this.ProcessWorldItems.removeAll(this.ProcessWorldItemsRemove);
        this.ProcessItemsRemove.clear();
        this.ProcessWorldItemsRemove.clear();
    }

    private void ProcessStaticUpdaters() {
        int n = this.StaticUpdaterObjectList.size();
        for (int i = 0; i < n; ++i) {
            try {
                ((IsoObject)this.StaticUpdaterObjectList.get((int)i)).update();
            }
            catch (Exception exception) {
                Logger.getLogger((String)GameWindow.class.getName()).log((Level)Level.SEVERE, null, (Throwable)exception);
            }
            if (n <= this.StaticUpdaterObjectList.size()) continue;
            --i;
            --n;
        }
    }

    public void addToProcessIsoObject(IsoObject isoObject) {
        if (isoObject == null) {
            return;
        }
        this.ProcessIsoObjectRemove.remove((Object)isoObject);
        if (!this.ProcessIsoObject.contains((Object)isoObject)) {
            this.ProcessIsoObject.add((Object)isoObject);
        }
    }

    public void addToProcessIsoObjectRemove(IsoObject isoObject) {
        if (isoObject == null) {
            return;
        }
        if (!this.ProcessIsoObject.contains((Object)isoObject)) {
            return;
        }
        if (!this.ProcessIsoObjectRemove.contains((Object)isoObject)) {
            this.ProcessIsoObjectRemove.add((Object)isoObject);
        }
    }

    public void addToStaticUpdaterObjectList(IsoObject isoObject) {
        if (isoObject == null) {
            return;
        }
        if (!this.StaticUpdaterObjectList.contains((Object)isoObject)) {
            this.StaticUpdaterObjectList.add((Object)isoObject);
        }
    }

    public void addToProcessItems(InventoryItem inventoryItem) {
        if (inventoryItem == null) {
            return;
        }
        this.ProcessItemsRemove.remove((Object)inventoryItem);
        if (!this.ProcessItems.contains((Object)inventoryItem)) {
            this.ProcessItems.add((Object)inventoryItem);
        }
    }

    public void addToProcessItems(ArrayList arrayList) {
        if (arrayList == null) {
            return;
        }
        for (int i = 0; i < arrayList.size(); ++i) {
            InventoryItem inventoryItem = (InventoryItem)arrayList.get((int)i);
            if (inventoryItem == null) continue;
            this.ProcessItemsRemove.remove((Object)inventoryItem);
            if (this.ProcessItems.contains((Object)inventoryItem)) continue;
            this.ProcessItems.add((Object)inventoryItem);
        }
    }

    public void addToProcessItemsRemove(InventoryItem inventoryItem) {
        if (inventoryItem == null) {
            return;
        }
        if (!this.ProcessItemsRemove.contains((Object)inventoryItem)) {
            this.ProcessItemsRemove.add((Object)inventoryItem);
        }
    }

    public void addToProcessItemsRemove(ArrayList arrayList) {
        if (arrayList == null) {
            return;
        }
        for (int i = 0; i < arrayList.size(); ++i) {
            InventoryItem inventoryItem = (InventoryItem)arrayList.get((int)i);
            if (inventoryItem == null || this.ProcessItemsRemove.contains((Object)inventoryItem)) continue;
            this.ProcessItemsRemove.add((Object)inventoryItem);
        }
    }

    public void addToProcessWorldItems(IsoWorldInventoryObject isoWorldInventoryObject) {
        if (isoWorldInventoryObject == null) {
            return;
        }
        this.ProcessWorldItemsRemove.remove((Object)isoWorldInventoryObject);
        if (!this.ProcessWorldItems.contains((Object)isoWorldInventoryObject)) {
            this.ProcessWorldItems.add((Object)isoWorldInventoryObject);
        }
    }

    public void addToProcessWorldItemsRemove(IsoWorldInventoryObject isoWorldInventoryObject) {
        if (isoWorldInventoryObject == null) {
            return;
        }
        if (!this.ProcessWorldItemsRemove.contains((Object)isoWorldInventoryObject)) {
            this.ProcessWorldItemsRemove.add((Object)isoWorldInventoryObject);
        }
    }

    public IsoSurvivor getNetworkPlayer(int n) {
        int n2 = this.RemoteSurvivorList.size();
        for (int i = 0; i < n2; ++i) {
            if (((IsoGameCharacter)this.RemoteSurvivorList.get((int)i)).getRemoteID() != n) continue;
            return (IsoSurvivor)this.RemoteSurvivorList.get((int)i);
        }
        return null;
    }

    IsoGridSquare ConnectNewSquare(IsoGridSquare isoGridSquare, boolean bl, boolean bl2) {
        int n = isoGridSquare.getX();
        int n2 = isoGridSquare.getY();
        int n3 = isoGridSquare.getZ();
        this.setCacheGridSquare((int)n, (int)n2, (int)n3, (IsoGridSquare)isoGridSquare);
        this.DoGridNav((IsoGridSquare)isoGridSquare, (IsoGridSquare.GetSquare)IsoGridSquare.cellGetSquare);
        return isoGridSquare;
    }

    public void DoGridNav(IsoGridSquare isoGridSquare, IsoGridSquare.GetSquare getSquare) {
        int n = isoGridSquare.getX();
        int n2 = isoGridSquare.getY();
        int n3 = isoGridSquare.getZ();
        isoGridSquare.nav[IsoDirections.N.index()] = getSquare.getGridSquare((int)n, (int)(n2 - 1), (int)n3);
        isoGridSquare.nav[IsoDirections.NW.index()] = getSquare.getGridSquare((int)(n - 1), (int)(n2 - 1), (int)n3);
        isoGridSquare.nav[IsoDirections.W.index()] = getSquare.getGridSquare((int)(n - 1), (int)n2, (int)n3);
        isoGridSquare.nav[IsoDirections.SW.index()] = getSquare.getGridSquare((int)(n - 1), (int)(n2 + 1), (int)n3);
        isoGridSquare.nav[IsoDirections.S.index()] = getSquare.getGridSquare((int)n, (int)(n2 + 1), (int)n3);
        isoGridSquare.nav[IsoDirections.SE.index()] = getSquare.getGridSquare((int)(n + 1), (int)(n2 + 1), (int)n3);
        isoGridSquare.nav[IsoDirections.E.index()] = getSquare.getGridSquare((int)(n + 1), (int)n2, (int)n3);
        isoGridSquare.nav[IsoDirections.NE.index()] = getSquare.getGridSquare((int)(n + 1), (int)(n2 - 1), (int)n3);
        if (isoGridSquare.nav[IsoDirections.N.index()] != null) {
            isoGridSquare.nav[IsoDirections.N.index()].nav[IsoDirections.S.index()] = isoGridSquare;
        }
        if (isoGridSquare.nav[IsoDirections.NW.index()] != null) {
            isoGridSquare.nav[IsoDirections.NW.index()].nav[IsoDirections.SE.index()] = isoGridSquare;
        }
        if (isoGridSquare.nav[IsoDirections.W.index()] != null) {
            isoGridSquare.nav[IsoDirections.W.index()].nav[IsoDirections.E.index()] = isoGridSquare;
        }
        if (isoGridSquare.nav[IsoDirections.SW.index()] != null) {
            isoGridSquare.nav[IsoDirections.SW.index()].nav[IsoDirections.NE.index()] = isoGridSquare;
        }
        if (isoGridSquare.nav[IsoDirections.S.index()] != null) {
            isoGridSquare.nav[IsoDirections.S.index()].nav[IsoDirections.N.index()] = isoGridSquare;
        }
        if (isoGridSquare.nav[IsoDirections.SE.index()] != null) {
            isoGridSquare.nav[IsoDirections.SE.index()].nav[IsoDirections.NW.index()] = isoGridSquare;
        }
        if (isoGridSquare.nav[IsoDirections.E.index()] != null) {
            isoGridSquare.nav[IsoDirections.E.index()].nav[IsoDirections.W.index()] = isoGridSquare;
        }
        if (isoGridSquare.nav[IsoDirections.NE.index()] != null) {
            isoGridSquare.nav[IsoDirections.NE.index()].nav[IsoDirections.SW.index()] = isoGridSquare;
        }
    }

    public IsoGridSquare ConnectNewSquare(IsoGridSquare isoGridSquare, boolean bl) {
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            if (this.ChunkMap[i].ignore) continue;
            this.ChunkMap[i].setGridSquare((IsoGridSquare)isoGridSquare, (int)isoGridSquare.getX(), (int)isoGridSquare.getY(), (int)isoGridSquare.getZ());
        }
        IsoGridSquare isoGridSquare2 = this.ConnectNewSquare((IsoGridSquare)isoGridSquare, (boolean)bl, (boolean)false);
        return isoGridSquare2;
    }

    public void PlaceLot(String string, int n, int n2, int n3, boolean bl) {
    }

    public void PlaceLot(IsoLot isoLot, int n, int n2, int n3, boolean bl) {
        int n4 = Math.min((int)(n3 + isoLot.info.levels), (int)(n3 + 8));
        for (int i = n; i < n + isoLot.info.width; ++i) {
            for (int j = n2; j < n2 + isoLot.info.height; ++j) {
                for (int k = n3; k < n4; ++k) {
                    int n5;
                    int n6;
                    int n7;
                    int n8 = i - n;
                    int n9 = j - n2;
                    int n10 = k - n3;
                    if (i >= this.width || j >= this.height || i < 0 || j < 0 || k < 0 || (n7 = isoLot.m_offsetInData[n6 = n8 + n9 * 10 + n10 * 100]) == -1 || (n5 = isoLot.m_data.getQuick((int)n7)) <= 0) continue;
                    boolean bl2 = false;
                    for (int i2 = 0; i2 < n5; ++i2) {
                        String string = (String)isoLot.info.tilesUsed.get((int)isoLot.m_data.getQuick((int)(n7 + 1 + i2)));
                        IsoSprite isoSprite = (IsoSprite)IsoSpriteManager.instance.NamedMap.get((Object)string);
                        if (isoSprite == null) {
                            Logger.getLogger((String)GameWindow.class.getName()).log((Level)Level.SEVERE, (String)("Missing tile definition: " + string));
                            continue;
                        }
                        IsoGridSquare isoGridSquare = this.getGridSquare((int)i, (int)j, (int)k);
                        if (isoGridSquare == null) {
                            isoGridSquare = IsoGridSquare.loadGridSquareCache != null ? IsoGridSquare.getNew(IsoGridSquare.loadGridSquareCache, (IsoCell)this, null, (int)i, (int)j, (int)k) : IsoGridSquare.getNew((IsoCell)this, null, (int)i, (int)j, (int)k);
                            this.ChunkMap[IsoPlayer.getPlayerIndex()].setGridSquare((IsoGridSquare)isoGridSquare, (int)i, (int)j, (int)k);
                        } else {
                            if (bl && i2 == 0 && isoSprite.getProperties().Is((IsoFlagType)IsoFlagType.solidfloor) && (!isoSprite.Properties.Is((IsoFlagType)IsoFlagType.hidewalls) || n5 > 1)) {
                                bl2 = true;
                            }
                            if (bl2 && i2 == 0) {
                                isoGridSquare.getObjects().clear();
                            }
                        }
                        CellLoader.DoTileObjectCreation((IsoSprite)isoSprite, (IsoObjectType)isoSprite.getType(), (IsoGridSquare)isoGridSquare, (IsoCell)this, (int)i, (int)j, (int)k, (String)string);
                    }
                }
            }
        }
    }

    public void PlaceLot(IsoLot isoLot, int n, int n2, int n3, IsoChunk isoChunk, int n4, int n5) {
        n4 *= 10;
        n5 *= 10;
        IsoMetaGrid isoMetaGrid = IsoWorld.instance.getMetaGrid();
        int n6 = Math.min((int)(n3 + isoLot.info.levels), (int)(n3 + 8));
        try {
            for (int i = n4 + n; i < n4 + n + 10; ++i) {
                for (int j = n5 + n2; j < n5 + n2 + 10; ++j) {
                    for (int k = n3; k < n6; ++k) {
                        Object object;
                        int n7;
                        int n8;
                        int n9;
                        int n10;
                        int n11 = i - n4 - n;
                        int n12 = j - n5 - n2;
                        int n13 = k - n3;
                        if (i >= n4 + 10 || j >= n5 + 10 || i < n4 || j < n5 || k < 0 || (n10 = isoLot.m_offsetInData[n9 = n11 + n12 * 10 + n13 * 100]) == -1 || (n8 = isoLot.m_data.getQuick((int)n10)) <= 0) continue;
                        IsoGridSquare isoGridSquare = isoChunk.getGridSquare((int)(i - n4), (int)(j - n5), (int)k);
                        if (isoGridSquare == null) {
                            isoGridSquare = IsoGridSquare.loadGridSquareCache != null ? IsoGridSquare.getNew(IsoGridSquare.loadGridSquareCache, (IsoCell)this, null, (int)i, (int)j, (int)k) : IsoGridSquare.getNew((IsoCell)this, null, (int)i, (int)j, (int)k);
                            isoGridSquare.setX((int)i);
                            isoGridSquare.setY((int)j);
                            isoGridSquare.setZ((int)k);
                            isoChunk.setSquare((int)(i - n4), (int)(j - n5), (int)k, (IsoGridSquare)isoGridSquare);
                        }
                        for (int i2 = -1; i2 <= 1; ++i2) {
                            for (n7 = -1; n7 <= 1; ++n7) {
                                if (i2 == 0 && n7 == 0 || i2 + i - n4 < 0 || i2 + i - n4 >= 10 || n7 + j - n5 < 0 || n7 + j - n5 >= 10 || (object = isoChunk.getGridSquare((int)(i + i2 - n4), (int)(j + n7 - n5), (int)k)) != null) continue;
                                object = IsoGridSquare.getNew((IsoCell)this, null, (int)(i + i2), (int)(j + n7), (int)k);
                                isoChunk.setSquare((int)(i + i2 - n4), (int)(j + n7 - n5), (int)k, (IsoGridSquare)object);
                            }
                        }
                        RoomDef roomDef = isoMetaGrid.getRoomAt((int)i, (int)j, (int)k);
                        n7 = roomDef != null ? roomDef.ID : -1;
                        isoGridSquare.setRoomID((int)n7);
                        isoGridSquare.ResetIsoWorldRegion();
                        roomDef = isoMetaGrid.getEmptyOutsideAt((int)i, (int)j, (int)k);
                        if (roomDef != null) {
                            object = isoChunk.getRoom((int)roomDef.ID);
                            isoGridSquare.roofHideBuilding = object == null ? null : object.building;
                        }
                        boolean bl = true;
                        for (int i3 = 0; i3 < n8; ++i3) {
                            IsoSprite isoSprite;
                            String string = (String)isoLot.info.tilesUsed.get((int)isoLot.m_data.get((int)(n10 + 1 + i3)));
                            if (!isoLot.info.bFixed2x) {
                                string = IsoChunk.Fix2x((String)string);
                            }
                            if ((isoSprite = (IsoSprite)IsoSpriteManager.instance.NamedMap.get((Object)string)) == null) {
                                Logger.getLogger((String)GameWindow.class.getName()).log((Level)Level.SEVERE, (String)("Missing tile definition: " + string));
                                continue;
                            }
                            if (i3 == 0 && isoSprite.getProperties().Is((IsoFlagType)IsoFlagType.solidfloor) && (!isoSprite.Properties.Is((IsoFlagType)IsoFlagType.hidewalls) || n8 > 1)) {
                                bl = true;
                            }
                            if (bl && i3 == 0) {
                                isoGridSquare.getObjects().clear();
                            }
                            CellLoader.DoTileObjectCreation((IsoSprite)isoSprite, (IsoObjectType)isoSprite.getType(), (IsoGridSquare)isoGridSquare, (IsoCell)this, (int)i, (int)j, (int)k, (String)string);
                        }
                        isoGridSquare.FixStackableObjects();
                    }
                }
            }
        }
        catch (Exception exception) {
            DebugLog.log((String)"Failed to load chunk, blocking out area");
            ExceptionLogger.logException((Throwable)exception);
            for (int i = n4 + n; i < n4 + n + 10; ++i) {
                for (int j = n5 + n2; j < n5 + n2 + 10; ++j) {
                    for (int k = n3; k < n6; ++k) {
                        isoChunk.setSquare((int)(i - n4 - n), (int)(j - n5 - n2), (int)(k - n3), null);
                        this.setCacheGridSquare((int)i, (int)j, (int)k, null);
                    }
                }
            }
        }
    }

    public void setDrag(KahluaTable kahluaTable, int n) {
        Object object;
        if (n < 0 || n >= 4) {
            return;
        }
        if (this.drag[n] != null && this.drag[n] != kahluaTable && ((object = this.drag[n].rawget((Object)"deactivate")) instanceof JavaFunction || object instanceof LuaClosure)) {
            LuaManager.caller.pcallvoid((KahluaThread)LuaManager.thread, (Object)object, (Object)this.drag[n]);
        }
        this.drag[n] = kahluaTable;
    }

    public KahluaTable getDrag(int n) {
        if (n < 0 || n >= 4) {
            return null;
        }
        return this.drag[n];
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean DoBuilding(int n, boolean bl) {
        boolean bl2;
        try {
            s_performance.isoCellDoBuilding.start();
            bl2 = this.doBuildingInternal((int)n, (boolean)bl);
        }
        catch (Throwable throwable) {
            s_performance.isoCellDoBuilding.end();
            throw throwable;
        }
        s_performance.isoCellDoBuilding.end();
        return bl2;
    }

    private boolean doBuildingInternal(int n, boolean bl) {
        if (UIManager.getPickedTile() != null && this.drag[n] != null && JoypadManager.instance.getFromPlayer((int)n) == null) {
            if (!IsoWorld.instance.isValidSquare((int)((int)UIManager.getPickedTile().x), (int)((int)UIManager.getPickedTile().y), (int)((int)IsoCamera.CamCharacter.getZ()))) {
                return false;
            }
            IsoGridSquare isoGridSquare = this.getGridSquare((int)((int)UIManager.getPickedTile().x), (int)((int)UIManager.getPickedTile().y), (int)((int)IsoCamera.CamCharacter.getZ()));
            if (!bl) {
                if (isoGridSquare == null && (isoGridSquare = this.createNewGridSquare((int)((int)UIManager.getPickedTile().x), (int)((int)UIManager.getPickedTile().y), (int)((int)IsoCamera.CamCharacter.getZ()), (boolean)true)) == null) {
                    return false;
                }
                isoGridSquare.EnsureSurroundNotNull();
            }
            LuaEventManager.triggerEvent((String)"OnDoTileBuilding2", (Object)this.drag[n], (Object)Boolean.valueOf((boolean)bl), (Object)Integer.valueOf((int)((int)UIManager.getPickedTile().x)), (Object)Integer.valueOf((int)((int)UIManager.getPickedTile().y)), (Object)Integer.valueOf((int)((int)IsoCamera.CamCharacter.getZ())), (Object)isoGridSquare);
        }
        if (this.drag[n] != null && JoypadManager.instance.getFromPlayer((int)n) != null) {
            LuaEventManager.triggerEvent((String)"OnDoTileBuilding3", (Object)this.drag[n], (Object)Boolean.valueOf((boolean)bl), (Object)Integer.valueOf((int)((int)IsoPlayer.players[n].getX())), (Object)Integer.valueOf((int)((int)IsoPlayer.players[n].getY())), (Object)Integer.valueOf((int)((int)IsoCamera.CamCharacter.getZ())));
        }
        if (bl) {
            IndieGL.glBlendFunc((int)770, (int)771);
        }
        return false;
    }

    public float DistanceFromSupport(int n, int n2, int n3) {
        return 0.0f;
    }

    public ArrayList getBuildingList() {
        return this.BuildingList;
    }

    public ArrayList getWindowList() {
        return this.WindowList;
    }

    public void addToWindowList(IsoWindow isoWindow) {
        if (GameServer.bServer) {
            return;
        }
        if (isoWindow == null) {
            return;
        }
        if (!this.WindowList.contains((Object)isoWindow)) {
            this.WindowList.add((Object)isoWindow);
        }
    }

    public void removeFromWindowList(IsoWindow isoWindow) {
        this.WindowList.remove((Object)isoWindow);
    }

    public ArrayList getObjectList() {
        return this.ObjectList;
    }

    public IsoRoom getRoom(int n) {
        IsoRoom isoRoom = this.ChunkMap[IsoPlayer.getPlayerIndex()].getRoom((int)n);
        return isoRoom;
    }

    public ArrayList getPushableObjectList() {
        return this.PushableObjectList;
    }

    public HashMap getBuildingScores() {
        return this.BuildingScores;
    }

    public ArrayList getRoomList() {
        return this.RoomList;
    }

    public ArrayList getStaticUpdaterObjectList() {
        return this.StaticUpdaterObjectList;
    }

    public ArrayList getZombieList() {
        return this.ZombieList;
    }

    public ArrayList getRemoteSurvivorList() {
        return this.RemoteSurvivorList;
    }

    public ArrayList getRemoveList() {
        return this.removeList;
    }

    public ArrayList getAddList() {
        return this.addList;
    }

    public void addMovingObject(IsoMovingObject isoMovingObject) {
        this.addList.add((Object)isoMovingObject);
    }

    public ArrayList getProcessItems() {
        return this.ProcessItems;
    }

    public ArrayList getProcessWorldItems() {
        return this.ProcessWorldItems;
    }

    public ArrayList getProcessIsoObjects() {
        return this.ProcessIsoObject;
    }

    public ArrayList getProcessItemsRemove() {
        return this.ProcessItemsRemove;
    }

    public ArrayList getVehicles() {
        return this.vehicles;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int n) {
        this.height = n;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int n) {
        this.width = n;
    }

    public int getWorldX() {
        return this.worldX;
    }

    public void setWorldX(int n) {
        this.worldX = n;
    }

    public int getWorldY() {
        return this.worldY;
    }

    public void setWorldY(int n) {
        this.worldY = n;
    }

    public boolean isSafeToAdd() {
        return this.safeToAdd;
    }

    public void setSafeToAdd(boolean bl) {
        this.safeToAdd = bl;
    }

    public Stack getLamppostPositions() {
        return this.LamppostPositions;
    }

    public IsoLightSource getLightSourceAt(int n, int n2, int n3) {
        for (int i = 0; i < this.LamppostPositions.size(); ++i) {
            IsoLightSource isoLightSource = (IsoLightSource)this.LamppostPositions.get((int)i);
            if (isoLightSource.getX() != n || isoLightSource.getY() != n2 || isoLightSource.getZ() != n3) continue;
            return isoLightSource;
        }
        return null;
    }

    public void addLamppost(IsoLightSource isoLightSource) {
        if (isoLightSource == null || this.LamppostPositions.contains((Object)isoLightSource)) {
            return;
        }
        this.LamppostPositions.add((Object)isoLightSource);
        IsoGridSquare.RecalcLightTime = -1;
        GameTime.instance.lightSourceUpdate = 100.0f;
    }

    public IsoLightSource addLamppost(int n, int n2, int n3, float f, float f2, float f3, int n4) {
        IsoLightSource isoLightSource = new IsoLightSource((int)n, (int)n2, (int)n3, (float)f, (float)f2, (float)f3, (int)n4);
        this.LamppostPositions.add((Object)isoLightSource);
        IsoGridSquare.RecalcLightTime = -1;
        GameTime.instance.lightSourceUpdate = 100.0f;
        return isoLightSource;
    }

    public void removeLamppost(int n, int n2, int n3) {
        for (int i = 0; i < this.LamppostPositions.size(); ++i) {
            IsoLightSource isoLightSource = (IsoLightSource)this.LamppostPositions.get((int)i);
            if (isoLightSource.getX() != n || isoLightSource.getY() != n2 || isoLightSource.getZ() != n3) continue;
            isoLightSource.clearInfluence();
            this.LamppostPositions.remove((Object)isoLightSource);
            IsoGridSquare.RecalcLightTime = -1;
            GameTime.instance.lightSourceUpdate = 100.0f;
            return;
        }
    }

    public void removeLamppost(IsoLightSource isoLightSource) {
        isoLightSource.life = 0;
        IsoGridSquare.RecalcLightTime = -1;
        GameTime.instance.lightSourceUpdate = 100.0f;
    }

    public int getCurrentLightX() {
        return this.currentLX;
    }

    public void setCurrentLightX(int n) {
        this.currentLX = n;
    }

    public int getCurrentLightY() {
        return this.currentLY;
    }

    public void setCurrentLightY(int n) {
        this.currentLY = n;
    }

    public int getCurrentLightZ() {
        return this.currentLZ;
    }

    public void setCurrentLightZ(int n) {
        this.currentLZ = n;
    }

    public int getMinX() {
        return this.minX;
    }

    public void setMinX(int n) {
        this.minX = n;
    }

    public int getMaxX() {
        return this.maxX;
    }

    public void setMaxX(int n) {
        this.maxX = n;
    }

    public int getMinY() {
        return this.minY;
    }

    public void setMinY(int n) {
        this.minY = n;
    }

    public int getMaxY() {
        return this.maxY;
    }

    public void setMaxY(int n) {
        this.maxY = n;
    }

    public int getMinZ() {
        return this.minZ;
    }

    public void setMinZ(int n) {
        this.minZ = n;
    }

    public int getMaxZ() {
        return this.maxZ;
    }

    public void setMaxZ(int n) {
        this.maxZ = n;
    }

    public OnceEvery getDangerUpdate() {
        return this.dangerUpdate;
    }

    public void setDangerUpdate(OnceEvery onceEvery) {
        this.dangerUpdate = onceEvery;
    }

    public Thread getLightInfoUpdate() {
        return this.LightInfoUpdate;
    }

    public void setLightInfoUpdate(Thread thread) {
        this.LightInfoUpdate = thread;
    }

    public ArrayList getSurvivorList() {
        return this.SurvivorList;
    }

    public static int getRComponent(int n) {
        return n & 0xFF;
    }

    public static int getGComponent(int n) {
        return (n & 0xFF00) >> 8;
    }

    public static int getBComponent(int n) {
        return (n & 0xFF0000) >> 16;
    }

    public static int toIntColor(float f, float f2, float f3, float f4) {
        return (int)(f * 255.0f) << 0 | (int)(f2 * 255.0f) << 8 | (int)(f3 * 255.0f) << 16 | (int)(f4 * 255.0f) << 24;
    }

    public IsoGridSquare getRandomOutdoorTile() {
        IsoGridSquare isoGridSquare = null;
        do {
            if ((isoGridSquare = this.getGridSquare((int)(this.ChunkMap[IsoPlayer.getPlayerIndex()].getWorldXMin() * 10 + Rand.Next((int)this.width)), (int)(this.ChunkMap[IsoPlayer.getPlayerIndex()].getWorldYMin() * 10 + Rand.Next((int)this.height)), (int)0)) == null) continue;
            isoGridSquare.setCachedIsFree((boolean)false);
        } while (isoGridSquare == null || !isoGridSquare.isFree((boolean)false) || isoGridSquare.getRoom() != null);
        return isoGridSquare;
    }

    private static void InsertAt(int n, BuildingScore buildingScore, BuildingScore[] buildingScoreArray) {
        for (int i = buildingScoreArray.length - 1; i > n; --i) {
            buildingScoreArray[i] = buildingScoreArray[i - 1];
        }
        buildingScoreArray[n] = buildingScore;
    }

    static void Place(BuildingScore buildingScore, BuildingScore[] buildingScoreArray, BuildingSearchCriteria buildingSearchCriteria) {
        for (int i = 0; i < buildingScoreArray.length; ++i) {
            if (buildingScoreArray[i] == null) continue;
            boolean bl = false;
            if (buildingScoreArray[i] == null) {
                bl = true;
            } else {
                switch (buildingSearchCriteria) {
                    case General: {
                        if (!(buildingScoreArray[i].food + buildingScoreArray[i].defense + (float)buildingScoreArray[i].size + buildingScoreArray[i].weapons < buildingScore.food + buildingScore.defense + (float)buildingScore.size + buildingScore.weapons)) break;
                        bl = true;
                        break;
                    }
                    case Food: {
                        if (!(buildingScoreArray[i].food < buildingScore.food)) break;
                        bl = true;
                        break;
                    }
                    case Wood: {
                        if (!(buildingScoreArray[i].wood < buildingScore.wood)) break;
                        bl = true;
                        break;
                    }
                    case Weapons: {
                        if (!(buildingScoreArray[i].weapons < buildingScore.weapons)) break;
                        bl = true;
                        break;
                    }
                    case Defense: {
                        if (!(buildingScoreArray[i].defense < buildingScore.defense)) break;
                        bl = true;
                    }
                }
            }
            if (!bl) continue;
            IsoCell.InsertAt((int)i, (BuildingScore)buildingScore, (BuildingScore[])buildingScoreArray);
            return;
        }
    }

    public Stack getBestBuildings(BuildingSearchCriteria buildingSearchCriteria, int n) {
        int n2;
        int n3;
        Object[] objectArray = new BuildingScore[n];
        if (this.BuildingScores.isEmpty()) {
            n3 = this.BuildingList.size();
            for (n2 = 0; n2 < n3; ++n2) {
                ((IsoBuilding)this.BuildingList.get((int)n2)).update();
            }
        }
        n3 = this.BuildingScores.size();
        for (n2 = 0; n2 < n3; ++n2) {
            BuildingScore buildingScore = (BuildingScore)this.BuildingScores.get((Object)Integer.valueOf((int)n2));
            IsoCell.Place((BuildingScore)buildingScore, (BuildingScore[])objectArray, (BuildingSearchCriteria)buildingSearchCriteria);
        }
        buildingscores.clear();
        buildingscores.addAll((Collection)Arrays.asList((Object[])objectArray));
        return buildingscores;
    }

    public boolean blocked(Mover mover, int n, int n2, int n3, int n4, int n5, int n6) {
        IsoGridSquare isoGridSquare = this.getGridSquare((int)n4, (int)n5, (int)n6);
        if (isoGridSquare == null) {
            return true;
        }
        return mover instanceof IsoMovingObject ? isoGridSquare.testPathFindAdjacent((IsoMovingObject)((IsoMovingObject)mover), (int)(n - n4), (int)(n2 - n5), (int)(n3 - n6)) : isoGridSquare.testPathFindAdjacent(null, (int)(n - n4), (int)(n2 - n5), (int)(n3 - n6));
    }

    public void Dispose() {
        int n;
        for (n = 0; n < this.ObjectList.size(); ++n) {
            IsoMovingObject isoMovingObject = (IsoMovingObject)this.ObjectList.get((int)n);
            if (!(isoMovingObject instanceof IsoZombie)) continue;
            isoMovingObject.setCurrent(null);
            isoMovingObject.setLast(null);
            VirtualZombieManager.instance.addToReusable((IsoZombie)((IsoZombie)isoMovingObject));
        }
        for (n = 0; n < this.RoomList.size(); ++n) {
            ((IsoRoom)this.RoomList.get((int)n)).TileList.clear();
            ((IsoRoom)this.RoomList.get((int)n)).Exits.clear();
            ((IsoRoom)this.RoomList.get((int)n)).WaterSources.clear();
            ((IsoRoom)this.RoomList.get((int)n)).lightSwitches.clear();
            ((IsoRoom)this.RoomList.get((int)n)).Beds.clear();
        }
        for (n = 0; n < this.BuildingList.size(); ++n) {
            ((IsoBuilding)this.BuildingList.get((int)n)).Exits.clear();
            ((IsoBuilding)this.BuildingList.get((int)n)).Rooms.clear();
            ((IsoBuilding)this.BuildingList.get((int)n)).container.clear();
            ((IsoBuilding)this.BuildingList.get((int)n)).Windows.clear();
        }
        LuaEventManager.clear();
        LuaHookManager.clear();
        this.LamppostPositions.clear();
        this.ProcessItems.clear();
        this.ProcessItemsRemove.clear();
        this.ProcessWorldItems.clear();
        this.ProcessWorldItemsRemove.clear();
        this.BuildingScores.clear();
        this.BuildingList.clear();
        this.WindowList.clear();
        this.PushableObjectList.clear();
        this.RoomList.clear();
        this.SurvivorList.clear();
        this.ObjectList.clear();
        this.ZombieList.clear();
        for (n = 0; n < this.ChunkMap.length; ++n) {
            this.ChunkMap[n].Dispose();
            this.ChunkMap[n] = null;
        }
        for (n = 0; n < this.gridSquares.length; ++n) {
            if (this.gridSquares[n] == null) continue;
            Arrays.fill((Object[])this.gridSquares[n], null);
            this.gridSquares[n] = null;
        }
    }

    @LuaMethod(name="getGridSquare")
    public IsoGridSquare getGridSquare(double d, double d2, double d3) {
        if (GameServer.bServer) {
            return ServerMap.instance.getGridSquare((int)((int)d), (int)((int)d2), (int)((int)d3));
        }
        return this.getGridSquare((int)((int)d), (int)((int)d2), (int)((int)d3));
    }

    @LuaMethod(name="getOrCreateGridSquare")
    public IsoGridSquare getOrCreateGridSquare(double d, double d2, double d3) {
        if (GameServer.bServer) {
            IsoGridSquare isoGridSquare = ServerMap.instance.getGridSquare((int)((int)d), (int)((int)d2), (int)((int)d3));
            if (isoGridSquare == null) {
                isoGridSquare = IsoGridSquare.getNew((IsoCell)this, null, (int)((int)d), (int)((int)d2), (int)((int)d3));
                ServerMap.instance.setGridSquare((int)((int)d), (int)((int)d2), (int)((int)d3), (IsoGridSquare)isoGridSquare);
                this.ConnectNewSquare((IsoGridSquare)isoGridSquare, (boolean)true);
            }
            return isoGridSquare;
        }
        IsoGridSquare isoGridSquare = this.getGridSquare((int)((int)d), (int)((int)d2), (int)((int)d3));
        if (isoGridSquare == null) {
            isoGridSquare = IsoGridSquare.getNew((IsoCell)this, null, (int)((int)d), (int)((int)d2), (int)((int)d3));
            this.ConnectNewSquare((IsoGridSquare)isoGridSquare, (boolean)true);
        }
        return isoGridSquare;
    }

    public void setCacheGridSquare(int n, int n2, int n3, IsoGridSquare isoGridSquare) {
        if (!($assertionsDisabled || isoGridSquare == null || n == isoGridSquare.getX() && n2 == isoGridSquare.getY() && n3 == isoGridSquare.getZ())) {
            throw new AssertionError();
        }
        if (GameServer.bServer) {
            return;
        }
        if (!$assertionsDisabled && this.getChunkForGridSquare((int)n, (int)n2, (int)n3) == null) {
            throw new AssertionError();
        }
        int n4 = IsoChunkMap.ChunkWidthInTiles;
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            if (this.ChunkMap[i].ignore) continue;
            this.ChunkMap[i].YMinTiles = -1;
            this.ChunkMap[i].XMinTiles = -1;
            this.ChunkMap[i].YMaxTiles = -1;
            this.ChunkMap[i].XMaxTiles = -1;
            int n5 = n - this.ChunkMap[i].getWorldXMinTiles();
            int n6 = n2 - this.ChunkMap[i].getWorldYMinTiles();
            if (n3 >= 8 || n3 < 0 || n5 < 0 || n5 >= n4 || n6 < 0 || n6 >= n4) continue;
            this.gridSquares[i][n5 + n6 * n4 + n3 * n4 * n4] = isoGridSquare;
        }
    }

    public void setCacheChunk(IsoChunk isoChunk) {
        if (GameServer.bServer) {
            return;
        }
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            this.setCacheChunk((IsoChunk)isoChunk, (int)i);
        }
    }

    public void setCacheChunk(IsoChunk isoChunk, int n) {
        if (GameServer.bServer) {
            return;
        }
        int n2 = IsoChunkMap.ChunkWidthInTiles;
        IsoChunkMap isoChunkMap = this.ChunkMap[n];
        if (isoChunkMap.ignore) {
            return;
        }
        int n3 = isoChunk.wx - isoChunkMap.getWorldXMin();
        int n4 = isoChunk.wy - isoChunkMap.getWorldYMin();
        if (n3 < 0 || n3 >= IsoChunkMap.ChunkGridWidth || n4 < 0 || n4 >= IsoChunkMap.ChunkGridWidth) {
            return;
        }
        IsoGridSquare[] isoGridSquareArray = this.gridSquares[n];
        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 10; ++j) {
                for (int k = 0; k < 10; ++k) {
                    IsoGridSquare isoGridSquare = isoChunk.squares[i][k + j * 10];
                    int n5 = n3 * 10 + k;
                    int n6 = n4 * 10 + j;
                    isoGridSquareArray[n5 + n6 * n2 + i * n2 * n2] = isoGridSquare;
                }
            }
        }
    }

    public void clearCacheGridSquare(int n) {
        if (GameServer.bServer) {
            return;
        }
        int n2 = IsoChunkMap.ChunkWidthInTiles;
        this.gridSquares[n] = new IsoGridSquare[n2 * n2 * 8];
    }

    public void setCacheGridSquareLocal(int n, int n2, int n3, IsoGridSquare isoGridSquare, int n4) {
        if (GameServer.bServer) {
            return;
        }
        int n5 = IsoChunkMap.ChunkWidthInTiles;
        if (n3 >= 8 || n3 < 0 || n < 0 || n >= n5 || n2 < 0 || n2 >= n5) {
            return;
        }
        this.gridSquares[n4][n + n2 * n5 + n3 * n5 * n5] = isoGridSquare;
    }

    public IsoGridSquare getGridSquare(Double d, Double d2, Double d3) {
        return this.getGridSquare((int)d.intValue(), (int)d2.intValue(), (int)d3.intValue());
    }

    public IsoGridSquare getGridSquare(int n, int n2, int n3) {
        if (GameServer.bServer) {
            return ServerMap.instance.getGridSquare((int)n, (int)n2, (int)n3);
        }
        int n4 = IsoChunkMap.ChunkWidthInTiles;
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoGridSquare isoGridSquare;
            int n5;
            if (this.ChunkMap[i].ignore) continue;
            if (n3 == 0) {
                n5 = 0;
            }
            n5 = n - this.ChunkMap[i].getWorldXMinTiles();
            int n6 = n2 - this.ChunkMap[i].getWorldYMinTiles();
            if (n3 >= 8 || n3 < 0 || n5 < 0 || n5 >= n4 || n6 < 0 || n6 >= n4 || (isoGridSquare = this.gridSquares[i][n5 + n6 * n4 + n3 * n4 * n4]) == null) continue;
            return isoGridSquare;
        }
        return null;
    }

    public void EnsureSurroundNotNull(int n, int n2, int n3) {
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                this.createNewGridSquare((int)(n + i), (int)(n2 + j), (int)n3, (boolean)false);
            }
        }
    }

    public void DeleteAllMovingObjects() {
        this.ObjectList.clear();
    }

    @LuaMethod(name="getMaxFloors")
    public int getMaxFloors() {
        return 8;
    }

    public KahluaTable getLuaObjectList() {
        KahluaTable kahluaTable = LuaManager.platform.newTable();
        LuaManager.env.rawset((Object)"Objects", (Object)kahluaTable);
        for (int i = 0; i < this.ObjectList.size(); ++i) {
            kahluaTable.rawset((int)(i + 1), (Object)this.ObjectList.get((int)i));
        }
        return kahluaTable;
    }

    public int getHeightInTiles() {
        return this.ChunkMap[IsoPlayer.getPlayerIndex()].getWidthInTiles();
    }

    public int getWidthInTiles() {
        return this.ChunkMap[IsoPlayer.getPlayerIndex()].getWidthInTiles();
    }

    public boolean isNull(int n, int n2, int n3) {
        IsoGridSquare isoGridSquare = this.getGridSquare((int)n, (int)n2, (int)n3);
        return isoGridSquare == null || !isoGridSquare.isFree((boolean)false);
    }

    public void Remove(IsoMovingObject isoMovingObject) {
        if (isoMovingObject instanceof IsoPlayer && !((IsoPlayer)isoMovingObject).isDead()) {
            return;
        }
        this.removeList.add((Object)isoMovingObject);
    }

    boolean isBlocked(IsoGridSquare isoGridSquare, IsoGridSquare isoGridSquare2) {
        return isoGridSquare.room != isoGridSquare2.room;
    }

    private int CalculateColor(IsoGridSquare isoGridSquare, IsoGridSquare isoGridSquare2, IsoGridSquare isoGridSquare3, IsoGridSquare isoGridSquare4, int n, int n2) {
        ColorInfo colorInfo;
        float f = 0.0f;
        float f2 = 0.0f;
        float f3 = 0.0f;
        float f4 = 1.0f;
        if (isoGridSquare4 == null) {
            return 0;
        }
        float f5 = 0.0f;
        boolean bl = true;
        if (isoGridSquare != null && isoGridSquare4.room == isoGridSquare.room && isoGridSquare.getChunk() != null) {
            f5 += 1.0f;
            colorInfo = isoGridSquare.lighting[n2].lightInfo();
            f += colorInfo.r;
            f2 += colorInfo.g;
            f3 += colorInfo.b;
        }
        if (isoGridSquare2 != null && isoGridSquare4.room == isoGridSquare2.room && isoGridSquare2.getChunk() != null) {
            f5 += 1.0f;
            colorInfo = isoGridSquare2.lighting[n2].lightInfo();
            f += colorInfo.r;
            f2 += colorInfo.g;
            f3 += colorInfo.b;
        }
        if (isoGridSquare3 != null && isoGridSquare4.room == isoGridSquare3.room && isoGridSquare3.getChunk() != null) {
            f5 += 1.0f;
            colorInfo = isoGridSquare3.lighting[n2].lightInfo();
            f += colorInfo.r;
            f2 += colorInfo.g;
            f3 += colorInfo.b;
        }
        if (isoGridSquare4 != null) {
            f5 += 1.0f;
            colorInfo = isoGridSquare4.lighting[n2].lightInfo();
            f += colorInfo.r;
            f2 += colorInfo.g;
            f3 += colorInfo.b;
        }
        if (f5 != 0.0f) {
            f /= f5;
            f2 /= f5;
            f3 /= f5;
        }
        if (f > 1.0f) {
            f = 1.0f;
        }
        if (f2 > 1.0f) {
            f2 = 1.0f;
        }
        if (f3 > 1.0f) {
            f3 = 1.0f;
        }
        if (f < 0.0f) {
            f = 0.0f;
        }
        if (f2 < 0.0f) {
            f2 = 0.0f;
        }
        if (f3 < 0.0f) {
            f3 = 0.0f;
        }
        if (isoGridSquare4 != null) {
            isoGridSquare4.setVertLight((int)n, (int)((int)(f * 255.0f) << 0 | (int)(f2 * 255.0f) << 8 | (int)(f3 * 255.0f) << 16 | 0xFF000000), (int)n2);
            isoGridSquare4.setVertLight((int)(n + 4), (int)((int)(f * 255.0f) << 0 | (int)(f2 * 255.0f) << 8 | (int)(f3 * 255.0f) << 16 | 0xFF000000), (int)n2);
        }
        return n;
    }

    public static IsoCell getInstance() {
        return instance;
    }

    public void render() {
        s_performance.isoCellRender.invokeAndMeasure((Object)this, IsoCell::renderInternal);
    }

    private void renderInternal() {
        int n;
        int n2;
        IsoGridSquare isoGridSquare;
        IsoGridSquare isoGridSquare2;
        int n3;
        int n4 = IsoCamera.frameState.playerIndex;
        IsoPlayer isoPlayer = IsoPlayer.players[n4];
        isoPlayer.dirtyRecalcGridStack = isoPlayer.dirtyRecalcGridStackTime > 0.0f;
        if (!PerformanceSettings.NewRoofHiding) {
            if (this.bHideFloors[n4] && this.unhideFloorsCounter[n4] > 0) {
                int n5 = n4;
                this.unhideFloorsCounter[n5] = this.unhideFloorsCounter[n5] - 1;
            }
            if (this.unhideFloorsCounter[n4] <= 0) {
                this.bHideFloors[n4] = false;
                this.unhideFloorsCounter[n4] = 60;
            }
        }
        if ((n3 = 8) < 8) {
            ++n3;
        }
        --this.recalcShading;
        int n6 = 0;
        int n7 = 0;
        int n8 = n6 + IsoCamera.getOffscreenWidth((int)n4);
        int n9 = n7 + IsoCamera.getOffscreenHeight((int)n4);
        float f = IsoUtils.XToIso((float)((float)n6), (float)((float)n7), (float)0.0f);
        float f2 = IsoUtils.YToIso((float)((float)n8), (float)((float)n7), (float)0.0f);
        float f3 = IsoUtils.XToIso((float)((float)n8), (float)((float)n9), (float)6.0f);
        float f4 = IsoUtils.YToIso((float)((float)n6), (float)((float)n9), (float)6.0f);
        this.minY = (int)f2;
        this.maxY = (int)f4;
        this.minX = (int)f;
        this.maxX = (int)f3;
        this.minX -= 2;
        this.minY -= 2;
        this.maxZ = MaxHeight;
        if (IsoCamera.CamCharacter == null) {
            this.maxZ = 1;
        }
        int n10 = 0;
        n10 = 4;
        if (GameTime.instance.FPSMultiplier > 1.5f) {
            n10 = 6;
        }
        if (this.minX != this.lastMinX || this.minY != this.lastMinY) {
            this.lightUpdateCount = 10;
        }
        if (!PerformanceSettings.NewRoofHiding) {
            IsoGridSquare isoGridSquare3 = isoGridSquare2 = IsoCamera.CamCharacter == null ? null : IsoCamera.CamCharacter.getCurrentSquare();
            if (isoGridSquare2 != null) {
                isoGridSquare = this.getGridSquare((double)((double)Math.round((float)IsoCamera.CamCharacter.getX())), (double)((double)Math.round((float)IsoCamera.CamCharacter.getY())), (double)((double)IsoCamera.CamCharacter.getZ()));
                if (isoGridSquare != null && this.IsBehindStuff((IsoGridSquare)isoGridSquare)) {
                    this.bHideFloors[n4] = true;
                }
                if (!this.bHideFloors[n4] && isoGridSquare2.getProperties().Is((IsoFlagType)IsoFlagType.hidewalls) || !isoGridSquare2.getProperties().Is((IsoFlagType)IsoFlagType.exterior)) {
                    this.bHideFloors[n4] = true;
                }
            }
            if (this.bHideFloors[n4]) {
                this.maxZ = (int)IsoCamera.CamCharacter.getZ() + 1;
            }
        }
        if (PerformanceSettings.LightingFrameSkip < 3) {
            this.DrawStencilMask();
        }
        if (PerformanceSettings.LightingFrameSkip == 3) {
            int n11 = IsoCamera.getOffscreenWidth((int)n4) / 2;
            int n12 = IsoCamera.getOffscreenHeight((int)n4) / 2;
            n2 = 409;
            this.StencilX1 = (n11 -= n2 / (2 / Core.TileScale)) - (int)IsoCamera.cameras[n4].RightClickX;
            this.StencilY1 = (n12 -= n2 / (2 / Core.TileScale)) - (int)IsoCamera.cameras[n4].RightClickY;
            this.StencilX2 = this.StencilX1 + n2 * Core.TileScale;
            this.StencilY2 = this.StencilY1 + n2 * Core.TileScale;
        }
        if (PerformanceSettings.NewRoofHiding && isoPlayer.dirtyRecalcGridStack) {
            this.hidesOrphanStructuresAbove = n3;
            isoGridSquare2 = null;
            ((ArrayList)this.otherOccluderBuildings.get((int)n4)).clear();
            if (this.otherOccluderBuildingsArr[n4] != null) {
                this.otherOccluderBuildingsArr[n4][0] = null;
            } else {
                this.otherOccluderBuildingsArr[n4] = new IsoBuilding[500];
            }
            if (IsoCamera.CamCharacter != null && IsoCamera.CamCharacter.getCurrentSquare() != null) {
                IsoGridSquare isoGridSquare4;
                IsoBuilding isoBuilding;
                IsoGridSquare isoGridSquare5;
                isoGridSquare = IsoCamera.CamCharacter.getCurrentSquare();
                n2 = 10;
                if (this.ZombieList.size() < 10) {
                    n2 = this.ZombieList.size();
                }
                if (this.nearestVisibleZombie[n4] != null) {
                    if (this.nearestVisibleZombie[n4].isDead()) {
                        this.nearestVisibleZombie[n4] = null;
                    } else {
                        float f5 = this.nearestVisibleZombie[n4].x - IsoCamera.CamCharacter.x;
                        float f6 = this.nearestVisibleZombie[n4].y - IsoCamera.CamCharacter.y;
                        this.nearestVisibleZombieDistSqr[n4] = f5 * f5 + f6 * f6;
                    }
                }
                int n13 = 0;
                while (n13 < n2) {
                    IsoZombie isoZombie;
                    if (this.zombieScanCursor >= this.ZombieList.size()) {
                        this.zombieScanCursor = 0;
                    }
                    if ((isoZombie = (IsoZombie)this.ZombieList.get((int)this.zombieScanCursor)) != null && (isoGridSquare5 = isoZombie.getCurrentSquare()) != null && isoGridSquare.z == isoGridSquare5.z && isoGridSquare5.getCanSee((int)n4)) {
                        if (this.nearestVisibleZombie[n4] == null) {
                            this.nearestVisibleZombie[n4] = isoZombie;
                            var19_35 = this.nearestVisibleZombie[n4].x - IsoCamera.CamCharacter.x;
                            var20_38 = this.nearestVisibleZombie[n4].y - IsoCamera.CamCharacter.y;
                            this.nearestVisibleZombieDistSqr[n4] = var19_35 * var19_35 + var20_38 * var20_38;
                        } else {
                            var19_35 = isoZombie.x - IsoCamera.CamCharacter.x;
                            var20_38 = isoZombie.y - IsoCamera.CamCharacter.y;
                            float f7 = var19_35 * var19_35 + var20_38 * var20_38;
                            if (f7 < this.nearestVisibleZombieDistSqr[n4]) {
                                this.nearestVisibleZombie[n4] = isoZombie;
                                this.nearestVisibleZombieDistSqr[n4] = f7;
                            }
                        }
                    }
                    ++n13;
                    ++this.zombieScanCursor;
                }
                for (n13 = 0; n13 < 4; ++n13) {
                    IsoGridSquare isoGridSquare6;
                    double d;
                    double d2;
                    boolean bl;
                    IsoPlayer isoPlayer2 = IsoPlayer.players[n13];
                    if (isoPlayer2 == null || isoPlayer2.getCurrentSquare() == null) continue;
                    isoGridSquare5 = isoPlayer2.getCurrentSquare();
                    if (n13 == n4) {
                        isoGridSquare2 = isoGridSquare5;
                    }
                    boolean bl2 = bl = (d2 = (double)isoPlayer2.x - Math.floor((double)((double)isoPlayer2.x))) > (d = (double)isoPlayer2.y - Math.floor((double)((double)isoPlayer2.y)));
                    if (this.lastPlayerAngle[n13] == null) {
                        this.lastPlayerAngle[n13] = new Vector2((Vector2)isoPlayer2.getForwardDirection());
                        this.playerCutawaysDirty[n13] = true;
                    } else if (isoPlayer2.getForwardDirection().dot((Vector2)this.lastPlayerAngle[n13]) < 0.98f) {
                        this.lastPlayerAngle[n13].set((Vector2)isoPlayer2.getForwardDirection());
                        this.playerCutawaysDirty[n13] = true;
                    }
                    IsoDirections isoDirections = IsoDirections.fromAngle((Vector2)isoPlayer2.getForwardDirection());
                    if (this.lastPlayerSquare[n13] != isoGridSquare5 || this.lastPlayerSquareHalf[n13] != bl || this.lastPlayerDir[n13] != isoDirections) {
                        this.playerCutawaysDirty[n13] = true;
                        this.lastPlayerSquare[n13] = isoGridSquare5;
                        this.lastPlayerSquareHalf[n13] = bl;
                        this.lastPlayerDir[n13] = isoDirections;
                        isoBuilding = isoGridSquare5.getBuilding();
                        this.playerWindowPeekingRoomId[n13] = -1;
                        this.GetBuildingsInFrontOfCharacter((ArrayList)((ArrayList)this.playerOccluderBuildings.get((int)n13)), (IsoGridSquare)isoGridSquare5, (boolean)bl);
                        if (this.playerOccluderBuildingsArr[n4] == null) {
                            this.playerOccluderBuildingsArr[n4] = new IsoBuilding[500];
                        }
                        this.playerHidesOrphanStructures[n13] = this.bOccludedByOrphanStructureFlag;
                        if (isoBuilding == null && !isoPlayer2.bRemote && (isoBuilding = this.GetPeekedInBuilding((IsoGridSquare)isoGridSquare5, (IsoDirections)isoDirections)) != null) {
                            this.playerWindowPeekingRoomId[n13] = this.playerPeekedRoomId;
                        }
                        if (isoBuilding != null) {
                            this.AddUniqueToBuildingList((ArrayList)((ArrayList)this.playerOccluderBuildings.get((int)n13)), (IsoBuilding)isoBuilding);
                        }
                        isoGridSquare6 = (ArrayList)this.playerOccluderBuildings.get((int)n13);
                        for (int i = 0; i < isoGridSquare6.size(); ++i) {
                            IsoBuilding isoBuilding2;
                            this.playerOccluderBuildingsArr[n4][i] = isoBuilding2 = (IsoBuilding)isoGridSquare6.get((int)i);
                        }
                        this.playerOccluderBuildingsArr[n4][isoGridSquare6.size()] = null;
                    }
                    if (n13 == n4 && isoGridSquare2 != null) {
                        int n14;
                        this.gridSquaresTempLeft.clear();
                        this.gridSquaresTempRight.clear();
                        this.GetSquaresAroundPlayerSquare((IsoPlayer)isoPlayer2, (IsoGridSquare)isoGridSquare2, this.gridSquaresTempLeft, this.gridSquaresTempRight);
                        for (n14 = 0; n14 < this.gridSquaresTempLeft.size(); ++n14) {
                            isoGridSquare6 = (IsoGridSquare)this.gridSquaresTempLeft.get((int)n14);
                            if (!isoGridSquare6.getCanSee((int)n4) || isoGridSquare6.getBuilding() != null && isoGridSquare6.getBuilding() != isoGridSquare2.getBuilding()) continue;
                            ArrayList arrayList = this.GetBuildingsInFrontOfMustSeeSquare((IsoGridSquare)isoGridSquare6, (IsoGridOcclusionData.OcclusionFilter)IsoGridOcclusionData.OcclusionFilter.Right);
                            for (int i = 0; i < arrayList.size(); ++i) {
                                this.AddUniqueToBuildingList((ArrayList)((ArrayList)this.otherOccluderBuildings.get((int)n4)), (IsoBuilding)((IsoBuilding)arrayList.get((int)i)));
                            }
                            int n15 = n4;
                            this.playerHidesOrphanStructures[n15] = this.playerHidesOrphanStructures[n15] | this.bOccludedByOrphanStructureFlag;
                        }
                        for (n14 = 0; n14 < this.gridSquaresTempRight.size(); ++n14) {
                            isoGridSquare6 = (IsoGridSquare)this.gridSquaresTempRight.get((int)n14);
                            if (!isoGridSquare6.getCanSee((int)n4) || isoGridSquare6.getBuilding() != null && isoGridSquare6.getBuilding() != isoGridSquare2.getBuilding()) continue;
                            ArrayList arrayList = this.GetBuildingsInFrontOfMustSeeSquare((IsoGridSquare)isoGridSquare6, (IsoGridOcclusionData.OcclusionFilter)IsoGridOcclusionData.OcclusionFilter.Left);
                            for (int i = 0; i < arrayList.size(); ++i) {
                                this.AddUniqueToBuildingList((ArrayList)((ArrayList)this.otherOccluderBuildings.get((int)n4)), (IsoBuilding)((IsoBuilding)arrayList.get((int)i)));
                            }
                            int n16 = n4;
                            this.playerHidesOrphanStructures[n16] = this.playerHidesOrphanStructures[n16] | this.bOccludedByOrphanStructureFlag;
                        }
                        isoBuilding = (ArrayList)this.otherOccluderBuildings.get((int)n4);
                        if (this.otherOccluderBuildingsArr[n4] == null) {
                            this.otherOccluderBuildingsArr[n4] = new IsoBuilding[500];
                        }
                        for (int i = 0; i < isoBuilding.size(); ++i) {
                            IsoBuilding isoBuilding3;
                            this.otherOccluderBuildingsArr[n4][i] = isoBuilding3 = (IsoBuilding)isoBuilding.get((int)i);
                        }
                        this.otherOccluderBuildingsArr[n4][isoBuilding.size()] = null;
                    }
                    if (!this.playerHidesOrphanStructures[n13] || this.hidesOrphanStructuresAbove <= isoGridSquare5.getZ()) continue;
                    this.hidesOrphanStructuresAbove = isoGridSquare5.getZ();
                }
                if (isoGridSquare2 != null && this.hidesOrphanStructuresAbove < isoGridSquare2.getZ()) {
                    this.hidesOrphanStructuresAbove = isoGridSquare2.getZ();
                }
                n13 = 0;
                if (this.nearestVisibleZombie[n4] != null && this.nearestVisibleZombieDistSqr[n4] < 150.0f && (isoGridSquare4 = this.nearestVisibleZombie[n4].getCurrentSquare()) != null && isoGridSquare4.getCanSee((int)n4)) {
                    double d;
                    double d3 = (double)this.nearestVisibleZombie[n4].x - Math.floor((double)((double)this.nearestVisibleZombie[n4].x));
                    boolean bl = d3 > (d = (double)this.nearestVisibleZombie[n4].y - Math.floor((double)((double)this.nearestVisibleZombie[n4].y)));
                    n13 = 1;
                    if (this.lastZombieSquare[n4] != isoGridSquare4 || this.lastZombieSquareHalf[n4] != bl) {
                        this.lastZombieSquare[n4] = isoGridSquare4;
                        this.lastZombieSquareHalf[n4] = bl;
                        this.GetBuildingsInFrontOfCharacter((ArrayList)((ArrayList)this.zombieOccluderBuildings.get((int)n4)), (IsoGridSquare)isoGridSquare4, (boolean)bl);
                        ArrayList arrayList = (ArrayList)this.zombieOccluderBuildings.get((int)n4);
                        if (this.zombieOccluderBuildingsArr[n4] == null) {
                            this.zombieOccluderBuildingsArr[n4] = new IsoBuilding[500];
                        }
                        for (int i = 0; i < arrayList.size(); ++i) {
                            this.zombieOccluderBuildingsArr[n4][i] = isoBuilding = (IsoBuilding)arrayList.get((int)i);
                        }
                        this.zombieOccluderBuildingsArr[n4][arrayList.size()] = null;
                    }
                }
                if (n13 == 0) {
                    ((ArrayList)this.zombieOccluderBuildings.get((int)n4)).clear();
                    if (this.zombieOccluderBuildingsArr[n4] != null) {
                        this.zombieOccluderBuildingsArr[n4][0] = null;
                    } else {
                        this.zombieOccluderBuildingsArr[n4] = new IsoBuilding[500];
                    }
                }
            } else {
                for (int i = 0; i < 4; ++i) {
                    ((ArrayList)this.playerOccluderBuildings.get((int)i)).clear();
                    if (this.playerOccluderBuildingsArr[i] != null) {
                        this.playerOccluderBuildingsArr[i][0] = null;
                    } else {
                        this.playerOccluderBuildingsArr[i] = new IsoBuilding[500];
                    }
                    this.lastPlayerSquare[i] = null;
                    this.playerCutawaysDirty[i] = true;
                }
                this.playerWindowPeekingRoomId[n4] = -1;
                ((ArrayList)this.zombieOccluderBuildings.get((int)n4)).clear();
                if (this.zombieOccluderBuildingsArr[n4] != null) {
                    this.zombieOccluderBuildingsArr[n4][0] = null;
                } else {
                    this.zombieOccluderBuildingsArr[n4] = new IsoBuilding[500];
                }
                this.lastZombieSquare[n4] = null;
            }
        }
        if (!PerformanceSettings.NewRoofHiding) {
            for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                IsoBuilding isoBuilding;
                this.playerWindowPeekingRoomId[i] = -1;
                IsoPlayer isoPlayer3 = IsoPlayer.players[i];
                if (isoPlayer3 == null || (isoBuilding = isoPlayer3.getCurrentBuilding()) != null) continue;
                IsoDirections isoDirections = IsoDirections.fromAngle((Vector2)isoPlayer3.getForwardDirection());
                isoBuilding = this.GetPeekedInBuilding((IsoGridSquare)isoPlayer3.getCurrentSquare(), (IsoDirections)isoDirections);
                if (isoBuilding == null) continue;
                this.playerWindowPeekingRoomId[i] = this.playerPeekedRoomId;
            }
        }
        if (IsoCamera.CamCharacter != null && IsoCamera.CamCharacter.getCurrentSquare() != null && IsoCamera.CamCharacter.getCurrentSquare().getProperties().Is((IsoFlagType)IsoFlagType.hidewalls)) {
            this.maxZ = (int)IsoCamera.CamCharacter.getZ() + 1;
        }
        this.bRendering = true;
        try {
            this.RenderTiles((int)n3);
        }
        catch (Exception exception) {
            this.bRendering = false;
            Logger.getLogger((String)GameWindow.class.getName()).log((Level)Level.SEVERE, null, (Throwable)exception);
        }
        this.bRendering = false;
        if (IsoGridSquare.getRecalcLightTime() < 0) {
            IsoGridSquare.setRecalcLightTime((int)60);
        }
        if (IsoGridSquare.getLightcache() <= 0) {
            IsoGridSquare.setLightcache((int)90);
        }
        for (n = 0; n < this.ObjectList.size(); ++n) {
            IsoMovingObject isoMovingObject = (IsoMovingObject)this.ObjectList.get((int)n);
            isoMovingObject.renderlast();
        }
        for (n = 0; n < this.StaticUpdaterObjectList.size(); ++n) {
            IsoObject isoObject = (IsoObject)this.StaticUpdaterObjectList.get((int)n);
            isoObject.renderlast();
        }
        IsoTree.renderChopTreeIndicators();
        if (Core.bDebug) {
            // empty if block
        }
        this.lastMinX = this.minX;
        this.lastMinY = this.minY;
        this.DoBuilding((int)IsoPlayer.getPlayerIndex(), (boolean)true);
        this.renderRain();
    }

    public void invalidatePeekedRoom(int n) {
        this.lastPlayerDir[n] = IsoDirections.Max;
    }

    private boolean initWeatherFx() {
        if (GameServer.bServer) {
            return false;
        }
        if (this.weatherFX == null) {
            this.weatherFX = new IsoWeatherFX();
            this.weatherFX.init();
        }
        return true;
    }

    private void updateWeatherFx() {
        if (this.initWeatherFx()) {
            this.weatherFX.update();
        }
    }

    private void renderWeatherFx() {
        if (this.initWeatherFx()) {
            this.weatherFX.render();
        }
    }

    public IsoWeatherFX getWeatherFX() {
        return this.weatherFX;
    }

    private void renderRain() {
    }

    public void setRainAlpha(int n) {
        this.rainAlphaMax = (float)n / 100.0f;
    }

    public void setRainIntensity(int n) {
        this.rainIntensity = n;
    }

    public void setRainSpeed(int n) {
        this.rainSpeed = n;
    }

    public void reloadRainTextures() {
    }

    private void GetBuildingsInFrontOfCharacter(ArrayList arrayList, IsoGridSquare isoGridSquare, boolean bl) {
        arrayList.clear();
        this.bOccludedByOrphanStructureFlag = false;
        if (isoGridSquare == null) {
            return;
        }
        int n = isoGridSquare.getX();
        int n2 = isoGridSquare.getY();
        int n3 = isoGridSquare.getZ();
        this.GetBuildingsInFrontOfCharacterSquare((int)n, (int)n2, (int)n3, (boolean)bl, (ArrayList)arrayList);
        if (n3 < MaxHeight) {
            this.GetBuildingsInFrontOfCharacterSquare((int)(n - 1 + 3), (int)(n2 - 1 + 3), (int)(n3 + 1), (boolean)bl, (ArrayList)arrayList);
            this.GetBuildingsInFrontOfCharacterSquare((int)(n - 2 + 3), (int)(n2 - 2 + 3), (int)(n3 + 1), (boolean)bl, (ArrayList)arrayList);
            if (bl) {
                this.GetBuildingsInFrontOfCharacterSquare((int)(n + 3), (int)(n2 - 1 + 3), (int)(n3 + 1), (!bl ? 1 : 0) != 0, (ArrayList)arrayList);
                this.GetBuildingsInFrontOfCharacterSquare((int)(n - 1 + 3), (int)(n2 - 2 + 3), (int)(n3 + 1), (!bl ? 1 : 0) != 0, (ArrayList)arrayList);
            } else {
                this.GetBuildingsInFrontOfCharacterSquare((int)(n - 1 + 3), (int)(n2 + 3), (int)(n3 + 1), (!bl ? 1 : 0) != 0, (ArrayList)arrayList);
                this.GetBuildingsInFrontOfCharacterSquare((int)(n - 2 + 3), (int)(n2 - 1 + 3), (int)(n3 + 1), (!bl ? 1 : 0) != 0, (ArrayList)arrayList);
            }
        }
    }

    private void GetBuildingsInFrontOfCharacterSquare(int n, int n2, int n3, boolean bl, ArrayList arrayList) {
        IsoGridSquare isoGridSquare = this.getGridSquare((int)n, (int)n2, (int)n3);
        if (isoGridSquare == null) {
            if (n3 < MaxHeight) {
                this.GetBuildingsInFrontOfCharacterSquare((int)(n + 3), (int)(n2 + 3), (int)(n3 + 1), (boolean)bl, (ArrayList)arrayList);
            }
            return;
        }
        IsoGridOcclusionData isoGridOcclusionData = isoGridSquare.getOrCreateOcclusionData();
        IsoGridOcclusionData.OcclusionFilter occlusionFilter = bl ? IsoGridOcclusionData.OcclusionFilter.Right : IsoGridOcclusionData.OcclusionFilter.Left;
        this.bOccludedByOrphanStructureFlag |= isoGridOcclusionData.getCouldBeOccludedByOrphanStructures((IsoGridOcclusionData.OcclusionFilter)occlusionFilter);
        ArrayList arrayList2 = isoGridOcclusionData.getBuildingsCouldBeOccluders((IsoGridOcclusionData.OcclusionFilter)occlusionFilter);
        for (int i = 0; i < arrayList2.size(); ++i) {
            this.AddUniqueToBuildingList((ArrayList)arrayList, (IsoBuilding)((IsoBuilding)arrayList2.get((int)i)));
        }
    }

    private ArrayList GetBuildingsInFrontOfMustSeeSquare(IsoGridSquare isoGridSquare, IsoGridOcclusionData.OcclusionFilter occlusionFilter) {
        IsoGridOcclusionData isoGridOcclusionData = isoGridSquare.getOrCreateOcclusionData();
        this.bOccludedByOrphanStructureFlag = isoGridOcclusionData.getCouldBeOccludedByOrphanStructures((IsoGridOcclusionData.OcclusionFilter)IsoGridOcclusionData.OcclusionFilter.All);
        return isoGridOcclusionData.getBuildingsCouldBeOccluders((IsoGridOcclusionData.OcclusionFilter)occlusionFilter);
    }

    private IsoBuilding GetPeekedInBuilding(IsoGridSquare isoGridSquare, IsoDirections isoDirections) {
        IsoBuilding isoBuilding;
        IsoGridSquare isoGridSquare2;
        this.playerPeekedRoomId = -1;
        if (isoGridSquare == null) {
            return null;
        }
        if ((isoDirections == IsoDirections.NW || isoDirections == IsoDirections.N || isoDirections == IsoDirections.NE) && LosUtil.lineClear((IsoCell)this, (int)isoGridSquare.x, (int)isoGridSquare.y, (int)isoGridSquare.z, (int)isoGridSquare.x, (int)(isoGridSquare.y - 1), (int)isoGridSquare.z, (boolean)false) != LosUtil.TestResults.Blocked && (isoGridSquare2 = isoGridSquare.nav[IsoDirections.N.index()]) != null && (isoBuilding = isoGridSquare2.getBuilding()) != null) {
            this.playerPeekedRoomId = isoGridSquare2.getRoomID();
            return isoBuilding;
        }
        if ((isoDirections == IsoDirections.SW || isoDirections == IsoDirections.W || isoDirections == IsoDirections.NW) && LosUtil.lineClear((IsoCell)this, (int)isoGridSquare.x, (int)isoGridSquare.y, (int)isoGridSquare.z, (int)(isoGridSquare.x - 1), (int)isoGridSquare.y, (int)isoGridSquare.z, (boolean)false) != LosUtil.TestResults.Blocked && (isoGridSquare2 = isoGridSquare.nav[IsoDirections.W.index()]) != null && (isoBuilding = isoGridSquare2.getBuilding()) != null) {
            this.playerPeekedRoomId = isoGridSquare2.getRoomID();
            return isoBuilding;
        }
        if ((isoDirections == IsoDirections.SE || isoDirections == IsoDirections.S || isoDirections == IsoDirections.SW) && LosUtil.lineClear((IsoCell)this, (int)isoGridSquare.x, (int)isoGridSquare.y, (int)isoGridSquare.z, (int)isoGridSquare.x, (int)(isoGridSquare.y + 1), (int)isoGridSquare.z, (boolean)false) != LosUtil.TestResults.Blocked && (isoGridSquare2 = isoGridSquare.nav[IsoDirections.S.index()]) != null && (isoBuilding = isoGridSquare2.getBuilding()) != null) {
            this.playerPeekedRoomId = isoGridSquare2.getRoomID();
            return isoBuilding;
        }
        if ((isoDirections == IsoDirections.NE || isoDirections == IsoDirections.E || isoDirections == IsoDirections.SE) && LosUtil.lineClear((IsoCell)this, (int)isoGridSquare.x, (int)isoGridSquare.y, (int)isoGridSquare.z, (int)(isoGridSquare.x + 1), (int)isoGridSquare.y, (int)isoGridSquare.z, (boolean)false) != LosUtil.TestResults.Blocked && (isoGridSquare2 = isoGridSquare.nav[IsoDirections.E.index()]) != null && (isoBuilding = isoGridSquare2.getBuilding()) != null) {
            this.playerPeekedRoomId = isoGridSquare2.getRoomID();
            return isoBuilding;
        }
        return null;
    }

    void GetSquaresAroundPlayerSquare(IsoPlayer isoPlayer, IsoGridSquare isoGridSquare, ArrayList arrayList, ArrayList arrayList2) {
        float f = isoPlayer.x - 4.0f;
        float f2 = isoPlayer.y - 4.0f;
        int n = (int)f;
        int n2 = (int)f2;
        int n3 = isoGridSquare.getZ();
        for (int i = n2; i < n2 + 10; ++i) {
            for (int j = n; j < n + 10; ++j) {
                IsoGridSquare isoGridSquare2;
                float f3;
                float f4;
                if (j < (int)isoPlayer.x && i < (int)isoPlayer.y || j == (int)isoPlayer.x && i == (int)isoPlayer.y || !((double)(f4 = (float)i - isoPlayer.y) < (double)(f3 = (float)j - isoPlayer.x) + 4.5) || !((double)f4 > (double)f3 - 4.5) || (isoGridSquare2 = this.getGridSquare((int)j, (int)i, (int)n3)) == null) continue;
                if (f4 >= f3) {
                    arrayList.add((Object)isoGridSquare2);
                }
                if (!(f4 <= f3)) continue;
                arrayList2.add((Object)isoGridSquare2);
            }
        }
    }

    private boolean IsBehindStuff(IsoGridSquare isoGridSquare) {
        if (!isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.exterior)) {
            return true;
        }
        for (int i = 1; i < 8 && isoGridSquare.getZ() + i < MaxHeight; ++i) {
            for (int j = -5; j <= 6; ++j) {
                for (int k = -5; k <= 6; ++k) {
                    IsoGridSquare isoGridSquare2;
                    int n = j;
                    if (k < n - 5 || k > n + 5 || (isoGridSquare2 = this.getGridSquare((int)(isoGridSquare.getX() + k + i * 3), (int)(isoGridSquare.getY() + j + i * 3), (int)(isoGridSquare.getZ() + i))) == null || isoGridSquare2.getObjects().isEmpty()) continue;
                    if (i == 1 && isoGridSquare2.getObjects().size() == 1) {
                        IsoObject isoObject = (IsoObject)isoGridSquare2.getObjects().get((int)0);
                        if (isoObject.sprite != null && isoObject.sprite.name != null && isoObject.sprite.name.startsWith((String)"lighting_outdoor")) continue;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static IsoDirections FromMouseTile() {
        IsoDirections isoDirections = IsoDirections.N;
        float f = UIManager.getPickedTileLocal().x;
        float f2 = UIManager.getPickedTileLocal().y;
        float f3 = 0.5f - Math.abs((float)(0.5f - f2));
        float f4 = 0.5f - Math.abs((float)(0.5f - f));
        if (f > 0.5f && f4 < f3) {
            isoDirections = IsoDirections.E;
        } else if (f2 > 0.5f && f4 > f3) {
            isoDirections = IsoDirections.S;
        } else if (f < 0.5f && f4 < f3) {
            isoDirections = IsoDirections.W;
        } else if (f2 < 0.5f && f4 > f3) {
            isoDirections = IsoDirections.N;
        }
        return isoDirections;
    }

    public void update() {
        s_performance.isoCellUpdate.invokeAndMeasure((Object)this, IsoCell::updateInternal);
    }

    private void updateInternal() {
        MovingObjectUpdateScheduler.instance.startFrame();
        IsoSprite.alphaStep = 0.075f * (GameTime.getInstance().getMultiplier() / 1.6f);
        ++IsoGridSquare.gridSquareCacheEmptyTimer;
        this.ProcessSpottedRooms();
        if (!GameServer.bServer) {
            for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                if (IsoPlayer.players[i] == null || IsoPlayer.players[i].isDead() && IsoPlayer.players[i].ReanimatedCorpse == null) continue;
                IsoPlayer.setInstance((IsoPlayer)IsoPlayer.players[i]);
                IsoCamera.CamCharacter = IsoPlayer.players[i];
                this.ChunkMap[i].update();
            }
        }
        this.ProcessRemoveItems(null);
        this.ProcessItems(null);
        this.ProcessRemoveItems(null);
        this.ProcessIsoObject();
        this.safeToAdd = false;
        this.ProcessObjects(null);
        if (GameClient.bClient && (NetworkZombieSimulator.getInstance().anyUnknownZombies() && GameClient.instance.sendZombieRequestsTimer.Check() || GameClient.instance.sendZombieTimer.Check())) {
            NetworkZombieSimulator.getInstance().send();
            GameClient.instance.sendZombieTimer.Reset();
            GameClient.instance.sendZombieRequestsTimer.Reset();
        }
        this.safeToAdd = true;
        this.ProcessStaticUpdaters();
        this.ObjectDeletionAddition();
        IsoDeadBody.updateBodies();
        IsoGridSquare.setLightcache((int)(IsoGridSquare.getLightcache() - 1));
        IsoGridSquare.setRecalcLightTime((int)(IsoGridSquare.getRecalcLightTime() - 1));
        if (GameServer.bServer) {
            this.LamppostPositions.clear();
            this.roomLights.clear();
        }
        if (!GameTime.isGamePaused()) {
            this.rainScroll += (float)this.rainSpeed / 10.0f * 0.075f * (30.0f / (float)PerformanceSettings.getLockFPS());
            if (this.rainScroll > 1.0f) {
                this.rainScroll = 0.0f;
            }
        }
        if (!GameServer.bServer) {
            this.updateWeatherFx();
        }
    }

    IsoGridSquare getRandomFreeTile() {
        IsoGridSquare isoGridSquare = null;
        boolean bl = true;
        do {
            bl = true;
            isoGridSquare = this.getGridSquare((int)Rand.Next((int)this.width), (int)Rand.Next((int)this.height), (int)0);
            if (isoGridSquare == null) {
                bl = false;
                continue;
            }
            if (!isoGridSquare.isFree((boolean)false)) {
                bl = false;
                continue;
            }
            if (isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.solid) || isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.solidtrans)) {
                bl = false;
                continue;
            }
            if (isoGridSquare.getMovingObjects().size() > 0) {
                bl = false;
                continue;
            }
            if (isoGridSquare.Has((IsoObjectType)IsoObjectType.stairsBN) || isoGridSquare.Has((IsoObjectType)IsoObjectType.stairsMN) || isoGridSquare.Has((IsoObjectType)IsoObjectType.stairsTN)) {
                bl = false;
                continue;
            }
            if (!isoGridSquare.Has((IsoObjectType)IsoObjectType.stairsBW) && !isoGridSquare.Has((IsoObjectType)IsoObjectType.stairsMW) && !isoGridSquare.Has((IsoObjectType)IsoObjectType.stairsTW)) continue;
            bl = false;
        } while (!bl);
        return isoGridSquare;
    }

    IsoGridSquare getRandomOutdoorFreeTile() {
        IsoGridSquare isoGridSquare = null;
        boolean bl = true;
        do {
            bl = true;
            isoGridSquare = this.getGridSquare((int)Rand.Next((int)this.width), (int)Rand.Next((int)this.height), (int)0);
            if (isoGridSquare == null) {
                bl = false;
                continue;
            }
            if (!isoGridSquare.isFree((boolean)false)) {
                bl = false;
                continue;
            }
            if (isoGridSquare.getRoom() != null) {
                bl = false;
                continue;
            }
            if (isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.solid) || isoGridSquare.getProperties().Is((IsoFlagType)IsoFlagType.solidtrans)) {
                bl = false;
                continue;
            }
            if (isoGridSquare.getMovingObjects().size() > 0) {
                bl = false;
                continue;
            }
            if (isoGridSquare.Has((IsoObjectType)IsoObjectType.stairsBN) || isoGridSquare.Has((IsoObjectType)IsoObjectType.stairsMN) || isoGridSquare.Has((IsoObjectType)IsoObjectType.stairsTN)) {
                bl = false;
                continue;
            }
            if (!isoGridSquare.Has((IsoObjectType)IsoObjectType.stairsBW) && !isoGridSquare.Has((IsoObjectType)IsoObjectType.stairsMW) && !isoGridSquare.Has((IsoObjectType)IsoObjectType.stairsTW)) continue;
            bl = false;
        } while (!bl);
        return isoGridSquare;
    }

    public IsoGridSquare getRandomFreeTileInRoom() {
        Stack stack = new Stack();
        for (int i = 0; i < this.RoomList.size(); ++i) {
            if (((IsoRoom)this.RoomList.get((int)i)).TileList.size() <= 9 || ((IsoRoom)this.RoomList.get((int)i)).Exits.isEmpty() || !((IsoGridSquare)((IsoRoom)this.RoomList.get((int)i)).TileList.get((int)0)).getProperties().Is((IsoFlagType)IsoFlagType.solidfloor)) continue;
            stack.add((Object)((IsoRoom)this.RoomList.get((int)i)));
        }
        if (stack.isEmpty()) {
            return null;
        }
        IsoRoom isoRoom = (IsoRoom)stack.get((int)Rand.Next((int)stack.size()));
        return isoRoom.getFreeTile();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void roomSpotted(IsoRoom isoRoom) {
        Stack<IsoRoom> stack = this.SpottedRooms;
        synchronized (stack) {
            if (!this.SpottedRooms.contains((Object)isoRoom)) {
                this.SpottedRooms.push((Object)isoRoom);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void ProcessSpottedRooms() {
        Stack<IsoRoom> stack = this.SpottedRooms;
        synchronized (stack) {
            for (int i = 0; i < this.SpottedRooms.size(); ++i) {
                IsoGridSquare isoGridSquare;
                IsoRoom isoRoom = (IsoRoom)this.SpottedRooms.get((int)i);
                if (isoRoom.def.bDoneSpawn) continue;
                isoRoom.def.bDoneSpawn = true;
                LuaEventManager.triggerEvent((String)"OnSeeNewRoom", (Object)isoRoom);
                VirtualZombieManager.instance.roomSpotted((IsoRoom)isoRoom);
                if (GameClient.bClient || Core.bLastStand || !"shed".equals((Object)isoRoom.def.name) && !"garagestorage".equals((Object)isoRoom.def.name) && !"storageunit".equals((Object)isoRoom.def.name)) continue;
                int n = 7;
                if ("shed".equals((Object)isoRoom.def.name) || "garagestorage".equals((Object)isoRoom.def.name)) {
                    n = 4;
                }
                switch (SandboxOptions.instance.GeneratorSpawning.getValue()) {
                    case 1: {
                        n += 3;
                        break;
                    }
                    case 2: {
                        n += 2;
                        break;
                    }
                    case 4: {
                        n -= 2;
                        break;
                    }
                    case 5: {
                        n -= 3;
                    }
                }
                if (Rand.Next((int)n) != 0 || (isoGridSquare = isoRoom.getRandomFreeSquare()) == null) continue;
                IsoGenerator isoGenerator = new IsoGenerator((InventoryItem)InventoryItemFactory.CreateItem((String)"Base.Generator"), (IsoCell)this, (IsoGridSquare)isoGridSquare);
                if (!GameServer.bServer) continue;
                isoGenerator.transmitCompleteItemToClients();
            }
            this.SpottedRooms.clear();
        }
    }

    public void savePlayer() throws IOException {
        if (IsoPlayer.players[0] != null && !IsoPlayer.players[0].isDead()) {
            IsoPlayer.players[0].save();
        }
        GameClient.instance.sendPlayerSave((IsoPlayer)IsoPlayer.players[0]);
    }

    public void save(DataOutputStream dataOutputStream, boolean bl) throws IOException {
        while (ChunkSaveWorker.instance.bSaving) {
            try {
                Thread.sleep((long)30L);
            }
            catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            this.ChunkMap[i].Save();
        }
        dataOutputStream.writeInt((int)this.width);
        dataOutputStream.writeInt((int)this.height);
        dataOutputStream.writeInt((int)MaxHeight);
        File file = ZomboidFileSystem.instance.getFileInCurrentSave((String)"map_t.bin");
        FileOutputStream fileOutputStream = new FileOutputStream((File)file);
        dataOutputStream = new DataOutputStream((OutputStream)new BufferedOutputStream((OutputStream)fileOutputStream));
        GameTime.instance.save((DataOutputStream)dataOutputStream);
        dataOutputStream.flush();
        dataOutputStream.close();
        IsoWorld.instance.MetaGrid.save();
        if (PlayerDB.isAllow()) {
            PlayerDB.getInstance().savePlayers();
        }
        ReanimatedPlayers.instance.saveReanimatedPlayers();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean LoadPlayer(int n) throws FileNotFoundException, IOException {
        if (GameClient.bClient) {
            return ClientPlayerDB.getInstance().loadNetworkPlayer();
        }
        File file = ZomboidFileSystem.instance.getFileInCurrentSave((String)"map_p.bin");
        if (!file.exists()) {
            PlayerDB.getInstance().importPlayersFromVehiclesDB();
            return PlayerDB.getInstance().loadLocalPlayer((int)1);
        }
        FileInputStream fileInputStream = new FileInputStream((File)file);
        BufferedInputStream bufferedInputStream = new BufferedInputStream((InputStream)fileInputStream);
        Object object = SliceY.SliceBufferLock;
        synchronized (object) {
            SliceY.SliceBuffer.clear();
            int n2 = bufferedInputStream.read((byte[])SliceY.SliceBuffer.array());
            SliceY.SliceBuffer.limit((int)n2);
            byte by = SliceY.SliceBuffer.get();
            byte by2 = SliceY.SliceBuffer.get();
            byte by3 = SliceY.SliceBuffer.get();
            byte by4 = SliceY.SliceBuffer.get();
            if (by == 80 && by2 == 76 && by3 == 89 && by4 == 82) {
                n = SliceY.SliceBuffer.getInt();
            } else {
                SliceY.SliceBuffer.rewind();
            }
            if (n >= 69) {
                String string = GameWindow.ReadString((ByteBuffer)SliceY.SliceBuffer);
                if (GameClient.bClient && n < 71) {
                    string = ServerOptions.instance.ServerPlayerID.getValue();
                }
                if (GameClient.bClient && !IsoPlayer.isServerPlayerIDValid((String)string)) {
                    GameLoadingState.GameLoadingString = Translator.getText((String)"IGUI_MP_ServerPlayerIDMismatch");
                    GameLoadingState.playerWrongIP = true;
                    return false;
                }
            }
            IsoCell.instance.ChunkMap[IsoPlayer.getPlayerIndex()].WorldX = SliceY.SliceBuffer.getInt() + IsoWorld.saveoffsetx * 30;
            IsoCell.instance.ChunkMap[IsoPlayer.getPlayerIndex()].WorldY = SliceY.SliceBuffer.getInt() + IsoWorld.saveoffsety * 30;
            SliceY.SliceBuffer.getInt();
            SliceY.SliceBuffer.getInt();
            SliceY.SliceBuffer.getInt();
            if (IsoPlayer.getInstance() == null) {
                IsoPlayer.setInstance((IsoPlayer)new IsoPlayer((IsoCell)instance));
                IsoPlayer.players[0] = IsoPlayer.getInstance();
            }
            IsoPlayer.getInstance().load((ByteBuffer)SliceY.SliceBuffer, (int)n);
            fileInputStream.close();
        }
        PlayerDB.getInstance().saveLocalPlayersForce();
        file.delete();
        PlayerDB.getInstance().uploadLocalPlayers2DB();
        return true;
    }

    public IsoGridSquare getRelativeGridSquare(int n, int n2, int n3) {
        IsoChunkMap cfr_ignored_0 = this.ChunkMap[0];
        int n4 = this.ChunkMap[0].getWorldXMin() * 10;
        IsoChunkMap cfr_ignored_1 = this.ChunkMap[0];
        int n5 = this.ChunkMap[0].getWorldYMin() * 10;
        return this.getGridSquare((int)(n += n4), (int)(n2 += n5), (int)n3);
    }

    public IsoGridSquare createNewGridSquare(int n, int n2, int n3, boolean bl) {
        if (!IsoWorld.instance.isValidSquare((int)n, (int)n2, (int)n3)) {
            return null;
        }
        IsoGridSquare isoGridSquare = this.getGridSquare((int)n, (int)n2, (int)n3);
        if (isoGridSquare != null) {
            return isoGridSquare;
        }
        if (GameServer.bServer) {
            int n4 = n / 10;
            int n5 = n2 / 10;
            if (ServerMap.instance.getChunk((int)n4, (int)n5) != null) {
                isoGridSquare = IsoGridSquare.getNew((IsoCell)this, null, (int)n, (int)n2, (int)n3);
                ServerMap.instance.setGridSquare((int)n, (int)n2, (int)n3, (IsoGridSquare)isoGridSquare);
            }
        } else if (this.getChunkForGridSquare((int)n, (int)n2, (int)n3) != null) {
            isoGridSquare = IsoGridSquare.getNew((IsoCell)this, null, (int)n, (int)n2, (int)n3);
            this.ConnectNewSquare((IsoGridSquare)isoGridSquare, (boolean)true);
        }
        if (isoGridSquare != null && bl) {
            isoGridSquare.RecalcAllWithNeighbours((boolean)true);
        }
        return isoGridSquare;
    }

    public IsoGridSquare getGridSquareDirect(int n, int n2, int n3, int n4) {
        int n5 = IsoChunkMap.ChunkWidthInTiles;
        return this.gridSquares[n4][n + n2 * n5 + n3 * n5 * n5];
    }

    public boolean isInChunkMap(int n, int n2) {
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            int n3 = this.ChunkMap[i].getWorldXMinTiles();
            int n4 = this.ChunkMap[i].getWorldXMaxTiles();
            int n5 = this.ChunkMap[i].getWorldYMinTiles();
            int n6 = this.ChunkMap[i].getWorldYMaxTiles();
            if (n < n3 || n >= n4 || n2 < n5 || n2 >= n6) continue;
            return true;
        }
        return false;
    }

    public ArrayList getProcessIsoObjectRemove() {
        return this.ProcessIsoObjectRemove;
    }

    public void checkHaveRoof(int n, int n2) {
        boolean bl = false;
        for (int i = 8; i >= 0; --i) {
            IsoGridSquare isoGridSquare = this.getGridSquare((int)n, (int)n2, (int)i);
            if (isoGridSquare == null) continue;
            if (bl != isoGridSquare.haveRoof) {
                isoGridSquare.haveRoof = bl;
                isoGridSquare.RecalcAllWithNeighbours((boolean)true);
            }
            if (!isoGridSquare.Is((IsoFlagType)IsoFlagType.solidfloor)) continue;
            bl = true;
        }
    }

    public IsoZombie getFakeZombieForHit() {
        if (this.fakeZombieForHit == null) {
            this.fakeZombieForHit = new IsoZombie((IsoCell)this);
        }
        return this.fakeZombieForHit;
    }

    public void addHeatSource(IsoHeatSource isoHeatSource) {
        if (GameServer.bServer) {
            return;
        }
        if (this.heatSources.contains((Object)isoHeatSource)) {
            DebugLog.log((String)"ERROR addHeatSource called again with the same HeatSource");
            return;
        }
        this.heatSources.add((Object)isoHeatSource);
    }

    public void removeHeatSource(IsoHeatSource isoHeatSource) {
        if (GameServer.bServer) {
            return;
        }
        this.heatSources.remove((Object)isoHeatSource);
    }

    public void updateHeatSources() {
        if (GameServer.bServer) {
            return;
        }
        for (int i = this.heatSources.size() - 1; i >= 0; --i) {
            IsoHeatSource isoHeatSource = (IsoHeatSource)this.heatSources.get((int)i);
            if (isoHeatSource.isInBounds()) continue;
            this.heatSources.remove((int)i);
        }
    }

    public int getHeatSourceTemperature(int n, int n2, int n3) {
        int n4 = 0;
        for (int i = 0; i < this.heatSources.size(); ++i) {
            LosUtil.TestResults testResults;
            float f;
            IsoHeatSource isoHeatSource = (IsoHeatSource)this.heatSources.get((int)i);
            if (isoHeatSource.getZ() != n3 || !((f = IsoUtils.DistanceToSquared((float)((float)n), (float)((float)n2), (float)((float)isoHeatSource.getX()), (float)((float)isoHeatSource.getY()))) < (float)(isoHeatSource.getRadius() * isoHeatSource.getRadius())) || (testResults = LosUtil.lineClear((IsoCell)this, (int)isoHeatSource.getX(), (int)isoHeatSource.getY(), (int)isoHeatSource.getZ(), (int)n, (int)n2, (int)n3, (boolean)false)) != LosUtil.TestResults.Clear && testResults != LosUtil.TestResults.ClearThroughOpenDoor) continue;
            n4 = (int)((double)n4 + (double)isoHeatSource.getTemperature() * (1.0 - Math.sqrt((double)((double)f)) / (double)isoHeatSource.getRadius()));
        }
        return n4;
    }

    public float getHeatSourceHighestTemperature(float f, int n, int n2, int n3) {
        float f2;
        float f3 = f2 = f;
        float f4 = 0.0f;
        IsoGridSquare isoGridSquare = null;
        float f5 = 0.0f;
        for (int i = 0; i < this.heatSources.size(); ++i) {
            LosUtil.TestResults testResults;
            IsoHeatSource isoHeatSource = (IsoHeatSource)this.heatSources.get((int)i);
            if (isoHeatSource.getZ() != n3) continue;
            float f6 = IsoUtils.DistanceToSquared((float)((float)n), (float)((float)n2), (float)((float)isoHeatSource.getX()), (float)((float)isoHeatSource.getY()));
            isoGridSquare = this.getGridSquare((int)isoHeatSource.getX(), (int)isoHeatSource.getY(), (int)isoHeatSource.getZ());
            f5 = 0.0f;
            if (isoGridSquare != null) {
                if (!isoGridSquare.isInARoom()) {
                    f5 = f2 - 30.0f;
                    if (f5 < -15.0f) {
                        f5 = -15.0f;
                    } else if (f5 > 5.0f) {
                        f5 = 5.0f;
                    }
                } else {
                    f5 = f2 - 30.0f;
                    if (f5 < -7.0f) {
                        f5 = -7.0f;
                    } else if (f5 > 7.0f) {
                        f5 = 7.0f;
                    }
                }
            }
            if ((f4 = ClimateManager.lerp((float)((float)(1.0 - Math.sqrt((double)((double)f6)) / (double)isoHeatSource.getRadius())), (float)f2, (float)((float)isoHeatSource.getTemperature() + f5))) <= f3 || !(f6 < (float)(isoHeatSource.getRadius() * isoHeatSource.getRadius())) || (testResults = LosUtil.lineClear((IsoCell)this, (int)isoHeatSource.getX(), (int)isoHeatSource.getY(), (int)isoHeatSource.getZ(), (int)n, (int)n2, (int)n3, (boolean)false)) != LosUtil.TestResults.Clear && testResults != LosUtil.TestResults.ClearThroughOpenDoor) continue;
            f3 = f4;
        }
        return f3;
    }

    public void putInVehicle(IsoGameCharacter isoGameCharacter) {
        if (isoGameCharacter == null || isoGameCharacter.savedVehicleSeat == -1) {
            return;
        }
        int n = ((int)isoGameCharacter.getX() - 4) / 10;
        int n2 = ((int)isoGameCharacter.getY() - 4) / 10;
        int n3 = ((int)isoGameCharacter.getX() + 4) / 10;
        int n4 = ((int)isoGameCharacter.getY() + 4) / 10;
        for (int i = n2; i <= n4; ++i) {
            for (int j = n; j <= n3; ++j) {
                IsoChunk isoChunk = this.getChunkForGridSquare((int)(j * 10), (int)(i * 10), (int)((int)isoGameCharacter.getZ()));
                if (isoChunk == null) continue;
                for (int k = 0; k < isoChunk.vehicles.size(); ++k) {
                    BaseVehicle baseVehicle = (BaseVehicle)isoChunk.vehicles.get((int)k);
                    if ((int)baseVehicle.getZ() != (int)isoGameCharacter.getZ() || !(IsoUtils.DistanceToSquared((float)baseVehicle.getX(), (float)baseVehicle.getY(), (float)isoGameCharacter.savedVehicleX, (float)isoGameCharacter.savedVehicleY) < 0.010000001f)) continue;
                    if (baseVehicle.VehicleID == -1) {
                        return;
                    }
                    VehicleScript.Position position = baseVehicle.getPassengerPosition((int)isoGameCharacter.savedVehicleSeat, (String)"inside");
                    if (position != null && !baseVehicle.isSeatOccupied((int)isoGameCharacter.savedVehicleSeat)) {
                        baseVehicle.enter((int)isoGameCharacter.savedVehicleSeat, (IsoGameCharacter)isoGameCharacter, (Vector3f)position.offset);
                        LuaEventManager.triggerEvent((String)"OnEnterVehicle", (Object)isoGameCharacter);
                        if (baseVehicle.getCharacter((int)isoGameCharacter.savedVehicleSeat) == isoGameCharacter && isoGameCharacter.savedVehicleRunning) {
                            baseVehicle.resumeRunningAfterLoad();
                        }
                    }
                    return;
                }
            }
        }
    }

    @Deprecated
    public void resumeVehicleSounds(IsoGameCharacter isoGameCharacter) {
        if (isoGameCharacter == null || isoGameCharacter.savedVehicleSeat == -1) {
            return;
        }
        int n = ((int)isoGameCharacter.getX() - 4) / 10;
        int n2 = ((int)isoGameCharacter.getY() - 4) / 10;
        int n3 = ((int)isoGameCharacter.getX() + 4) / 10;
        int n4 = ((int)isoGameCharacter.getY() + 4) / 10;
        for (int i = n2; i <= n4; ++i) {
            for (int j = n; j <= n3; ++j) {
                IsoChunk isoChunk = this.getChunkForGridSquare((int)(j * 10), (int)(i * 10), (int)((int)isoGameCharacter.getZ()));
                if (isoChunk == null) continue;
                for (int k = 0; k < isoChunk.vehicles.size(); ++k) {
                    BaseVehicle baseVehicle = (BaseVehicle)isoChunk.vehicles.get((int)k);
                    if (!baseVehicle.lightbarSirenMode.isEnable()) continue;
                    baseVehicle.setLightbarSirenMode((int)baseVehicle.lightbarSirenMode.get());
                }
            }
        }
    }

    private void AddUniqueToBuildingList(ArrayList arrayList, IsoBuilding isoBuilding) {
        for (int i = 0; i < arrayList.size(); ++i) {
            if (arrayList.get((int)i) != isoBuilding) continue;
            return;
        }
        arrayList.add((Object)isoBuilding);
    }

    public IsoSpriteManager getSpriteManager() {
        return IsoSpriteManager.instance;
    }

    static {
        $assertionsDisabled = !IsoCell.class.desiredAssertionStatus();
        MaxHeight = 8;
        stchoices = new ArrayList();
        buildingscores = new Stack();
        GridStack = null;
        ShadowSquares = new ArrayList((int)1000);
        MinusFloorCharacters = new ArrayList((int)1000);
        SolidFloor = new ArrayList((int)5000);
        ShadedFloor = new ArrayList((int)5000);
        VegetationCorpses = new ArrayList((int)5000);
        perPlayerRender = new PerPlayerRender[4];
    }
}
