/*
 * Decompiled with CFR.
 */
package zombie.iso;

import java.io.File;
import java.io.IOException;
import java.lang.CharSequence;
import java.lang.Float;
import java.lang.Integer;
import java.lang.NumberFormatException;
import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.Rand;
import zombie.core.properties.PropertyContainer;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoLot;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoPushableObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.WorldStreamer;
import zombie.iso.areas.IsoRoom;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoCombinationWasherDryer;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoJukebox;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoTelevision;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWheelieBin;
import zombie.iso.objects.IsoWindow;
import zombie.iso.sprite.IsoDirectionFrame;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class CellLoader {
    public static final ArrayDeque<IsoObject> isoObjectCache;
    public static final ArrayDeque<IsoTree> isoTreeCache;
    static int wanderX;
    static int wanderY;
    static IsoRoom wanderRoom;
    static final HashSet<String> missingTiles;
    public static final HashMap<IsoSprite, IsoSprite> smashedWindowSpriteMap;

    public CellLoader() {
        super();
    }

    public static void DoTileObjectCreation(IsoSprite isoSprite, IsoObjectType isoObjectType, IsoGridSquare isoGridSquare, IsoCell isoCell, int n, int n2, int n3, String string) throws NumberFormatException {
        Object object;
        IsoObject isoObject = null;
        if (isoGridSquare == null) {
            return;
        }
        boolean bl = false;
        if (smashedWindowSpriteMap.containsKey((Object)isoSprite)) {
            isoSprite = (IsoSprite)smashedWindowSpriteMap.get((Object)isoSprite);
            isoObjectType = isoSprite.getType();
            bl = true;
        }
        PropertyContainer propertyContainer = isoSprite.getProperties();
        if (isoSprite.solidfloor && propertyContainer.Is((IsoFlagType)IsoFlagType.diamondFloor) && !propertyContainer.Is((IsoFlagType)IsoFlagType.transparentFloor) && (object = isoGridSquare.getFloor()) != null && ((IsoObject)object).getProperties().Is((IsoFlagType)IsoFlagType.diamondFloor)) {
            ((IsoObject)object).clearAttachedAnimSprite();
            ((IsoObject)object).setSprite((IsoSprite)isoSprite);
            return;
        }
        if (isoObjectType == IsoObjectType.doorW || isoObjectType == IsoObjectType.doorN) {
            object = new IsoDoor((IsoCell)isoCell, (IsoGridSquare)isoGridSquare, (IsoSprite)isoSprite, (isoObjectType == IsoObjectType.doorN ? 1 : 0) != 0);
            isoObject = object;
            CellLoader.AddSpecialObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
            if (isoSprite.getProperties().Is((String)"DoubleDoor")) {
                ((IsoDoor)object).Locked = false;
                ((IsoDoor)object).lockedByKey = false;
            }
            if (isoSprite.getProperties().Is((String)"GarageDoor")) {
                ((IsoDoor)object).Locked = !object.IsOpen();
                ((IsoDoor)object).lockedByKey = false;
            }
            GameClient.instance.objectSyncReq.putRequest((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
        } else if (isoObjectType == IsoObjectType.lightswitch) {
            isoObject = new IsoLightSwitch((IsoCell)isoCell, (IsoGridSquare)isoGridSquare, (IsoSprite)isoSprite, (int)isoGridSquare.getRoomID());
            CellLoader.AddObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
            GameClient.instance.objectSyncReq.putRequest((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
            if (isoObject.sprite.getProperties().Is((String)"lightR")) {
                float f = Float.parseFloat((String)isoObject.sprite.getProperties().Val((String)"lightR")) / 255.0f;
                float f2 = Float.parseFloat((String)isoObject.sprite.getProperties().Val((String)"lightG")) / 255.0f;
                float f3 = Float.parseFloat((String)isoObject.sprite.getProperties().Val((String)"lightB")) / 255.0f;
                int n4 = 10;
                if (isoObject.sprite.getProperties().Is((String)"LightRadius") && Integer.parseInt((String)isoObject.sprite.getProperties().Val((String)"LightRadius")) > 0) {
                    n4 = Integer.parseInt((String)isoObject.sprite.getProperties().Val((String)"LightRadius"));
                }
                IsoLightSource isoLightSource = new IsoLightSource((int)isoObject.square.getX(), (int)isoObject.square.getY(), (int)isoObject.square.getZ(), (float)f, (float)f2, (float)f3, (int)n4);
                isoLightSource.bActive = true;
                isoLightSource.bHydroPowered = true;
                isoLightSource.switches.add((Object)((IsoLightSwitch)isoObject));
                ((IsoLightSwitch)isoObject).lights.add((Object)isoLightSource);
            } else {
                ((IsoLightSwitch)isoObject).lightRoom = true;
            }
        } else if (isoObjectType == IsoObjectType.curtainN || isoObjectType == IsoObjectType.curtainS || isoObjectType == IsoObjectType.curtainE || isoObjectType == IsoObjectType.curtainW) {
            boolean bl2 = Integer.parseInt((String)string.substring((int)(string.lastIndexOf((String)"_") + 1))) % 8 <= 3;
            isoObject = new IsoCurtain((IsoCell)isoCell, (IsoGridSquare)isoGridSquare, (IsoSprite)isoSprite, (isoObjectType == IsoObjectType.curtainN || isoObjectType == IsoObjectType.curtainS ? 1 : 0) != 0, (boolean)bl2);
            CellLoader.AddSpecialObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
            GameClient.instance.objectSyncReq.putRequest((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
        } else if (isoSprite.getProperties().Is((IsoFlagType)IsoFlagType.windowW) || isoSprite.getProperties().Is((IsoFlagType)IsoFlagType.windowN)) {
            isoObject = new IsoWindow((IsoCell)isoCell, (IsoGridSquare)isoGridSquare, (IsoSprite)isoSprite, (boolean)isoSprite.getProperties().Is((IsoFlagType)IsoFlagType.windowN));
            if (bl) {
                ((IsoWindow)isoObject).setSmashed((boolean)true);
            }
            CellLoader.AddSpecialObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
            GameClient.instance.objectSyncReq.putRequest((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
        } else if (isoSprite.getProperties().Is((IsoFlagType)IsoFlagType.container) && isoSprite.getProperties().Val((String)"container").equals((Object)"barbecue")) {
            isoObject = new IsoBarbecue((IsoCell)isoCell, (IsoGridSquare)isoGridSquare, (IsoSprite)isoSprite);
            CellLoader.AddObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
        } else if (isoSprite.getProperties().Is((IsoFlagType)IsoFlagType.container) && isoSprite.getProperties().Val((String)"container").equals((Object)"fireplace")) {
            isoObject = new IsoFireplace((IsoCell)isoCell, (IsoGridSquare)isoGridSquare, (IsoSprite)isoSprite);
            CellLoader.AddObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
        } else if ("IsoCombinationWasherDryer".equals((Object)isoSprite.getProperties().Val((String)"IsoType"))) {
            isoObject = new IsoCombinationWasherDryer((IsoCell)isoCell, (IsoGridSquare)isoGridSquare, (IsoSprite)isoSprite);
            CellLoader.AddObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
        } else if (isoSprite.getProperties().Is((IsoFlagType)IsoFlagType.container) && isoSprite.getProperties().Val((String)"container").equals((Object)"clothingdryer")) {
            isoObject = new IsoClothingDryer((IsoCell)isoCell, (IsoGridSquare)isoGridSquare, (IsoSprite)isoSprite);
            CellLoader.AddObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
        } else if (isoSprite.getProperties().Is((IsoFlagType)IsoFlagType.container) && isoSprite.getProperties().Val((String)"container").equals((Object)"clothingwasher")) {
            isoObject = new IsoClothingWasher((IsoCell)isoCell, (IsoGridSquare)isoGridSquare, (IsoSprite)isoSprite);
            CellLoader.AddObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
        } else if (isoSprite.getProperties().Is((IsoFlagType)IsoFlagType.container) && isoSprite.getProperties().Val((String)"container").equals((Object)"woodstove")) {
            isoObject = new IsoFireplace((IsoCell)isoCell, (IsoGridSquare)isoGridSquare, (IsoSprite)isoSprite);
            CellLoader.AddObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
        } else if (isoSprite.getProperties().Is((IsoFlagType)IsoFlagType.container) && (isoSprite.getProperties().Val((String)"container").equals((Object)"stove") || isoSprite.getProperties().Val((String)"container").equals((Object)"microwave"))) {
            isoObject = new IsoStove((IsoCell)isoCell, (IsoGridSquare)isoGridSquare, (IsoSprite)isoSprite);
            CellLoader.AddObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
            GameClient.instance.objectSyncReq.putRequest((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
        } else if (isoObjectType == IsoObjectType.jukebox) {
            isoObject = new IsoJukebox((IsoCell)isoCell, (IsoGridSquare)isoGridSquare, (IsoSprite)isoSprite);
            isoObject.OutlineOnMouseover = true;
            CellLoader.AddObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
        } else if (isoObjectType == IsoObjectType.radio) {
            isoObject = new IsoRadio((IsoCell)isoCell, (IsoGridSquare)isoGridSquare, (IsoSprite)isoSprite);
            CellLoader.AddObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
        } else if (isoSprite.getProperties().Is((String)"signal")) {
            object = isoSprite.getProperties().Val((String)"signal");
            if ("radio".equals((Object)object)) {
                isoObject = new IsoRadio((IsoCell)isoCell, (IsoGridSquare)isoGridSquare, (IsoSprite)isoSprite);
            } else if ("tv".equals((Object)object)) {
                isoObject = new IsoTelevision((IsoCell)isoCell, (IsoGridSquare)isoGridSquare, (IsoSprite)isoSprite);
            }
            CellLoader.AddObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
        } else {
            if (isoSprite.getProperties().Is((IsoFlagType)IsoFlagType.WallOverlay)) {
                object = null;
                if (isoSprite.getProperties().Is((IsoFlagType)IsoFlagType.attachedSE)) {
                    object = isoGridSquare.getWallSE();
                } else if (isoSprite.getProperties().Is((IsoFlagType)IsoFlagType.attachedW)) {
                    object = isoGridSquare.getWall((boolean)false);
                } else if (isoSprite.getProperties().Is((IsoFlagType)IsoFlagType.attachedN)) {
                    object = isoGridSquare.getWall((boolean)true);
                } else {
                    for (int i = isoGridSquare.getObjects().size() - 1; i >= 0; --i) {
                        IsoObject isoObject2 = (IsoObject)isoGridSquare.getObjects().get((int)i);
                        if (!isoObject2.sprite.getProperties().Is((IsoFlagType)IsoFlagType.cutW) && !isoObject2.sprite.getProperties().Is((IsoFlagType)IsoFlagType.cutN)) continue;
                        object = isoObject2;
                        break;
                    }
                }
                if (object != null) {
                    if (((IsoObject)object).AttachedAnimSprite == null) {
                        ((IsoObject)object).AttachedAnimSprite = new ArrayList((int)4);
                    }
                    ((IsoObject)object).AttachedAnimSprite.add((Object)IsoSpriteInstance.get((IsoSprite)isoSprite));
                } else {
                    isoObject = IsoObject.getNew();
                    isoObject.sx = 0.0f;
                    isoObject.sprite = isoSprite;
                    isoObject.square = isoGridSquare;
                    CellLoader.AddObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
                }
                return;
            }
            if (isoSprite.getProperties().Is((IsoFlagType)IsoFlagType.FloorOverlay)) {
                object = isoGridSquare.getFloor();
                if (object != null) {
                    if (((IsoObject)object).AttachedAnimSprite == null) {
                        ((IsoObject)object).AttachedAnimSprite = new ArrayList((int)4);
                    }
                    ((IsoObject)object).AttachedAnimSprite.add((Object)IsoSpriteInstance.get((IsoSprite)isoSprite));
                }
            } else if (IsoMannequin.isMannequinSprite((IsoSprite)isoSprite)) {
                isoObject = new IsoMannequin((IsoCell)isoCell, (IsoGridSquare)isoGridSquare, (IsoSprite)isoSprite);
                CellLoader.AddObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
            } else if (isoObjectType == IsoObjectType.tree) {
                if (isoSprite.getName() != null && isoSprite.getName().startsWith((String)"vegetation_trees") && ((object = isoGridSquare.getFloor()) == null || ((IsoObject)object).getSprite() == null || ((IsoObject)object).getSprite().getName() == null || !((IsoObject)object).getSprite().getName().startsWith((String)"blends_natural"))) {
                    DebugLog.log((String)("ERROR: removed tree at " + isoGridSquare.x + "," + isoGridSquare.y + "," + isoGridSquare.z + " because floor is not blends_natural"));
                    return;
                }
                isoObject = IsoTree.getNew();
                isoObject.sprite = isoSprite;
                isoObject.square = isoGridSquare;
                isoObject.sx = 0.0f;
                ((IsoTree)isoObject).initTree();
                for (int i = 0; i < isoGridSquare.getObjects().size(); ++i) {
                    IsoObject isoObject3 = (IsoObject)isoGridSquare.getObjects().get((int)i);
                    if (!(isoObject3 instanceof IsoTree)) continue;
                    isoGridSquare.getObjects().remove((int)i);
                    isoObject3.reset();
                    isoTreeCache.push((Object)((IsoTree)isoObject3));
                    break;
                }
                CellLoader.AddObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
            } else {
                if ((isoSprite.CurrentAnim.Frames.isEmpty() || ((IsoDirectionFrame)isoSprite.CurrentAnim.Frames.get((int)0)).getTexture((IsoDirections)IsoDirections.N) == null) && !GameServer.bServer) {
                    if (!missingTiles.contains((Object)string)) {
                        if (Core.bDebug) {
                            DebugLog.General.error((Object)("CellLoader> missing tile " + string));
                        }
                        missingTiles.add((Object)string);
                    }
                    isoSprite.LoadFramesNoDirPageSimple((String)(Core.bDebug ? "media/ui/missing-tile-debug.png" : "media/ui/missing-tile.png"));
                    if (isoSprite.CurrentAnim.Frames.isEmpty() || ((IsoDirectionFrame)isoSprite.CurrentAnim.Frames.get((int)0)).getTexture((IsoDirections)IsoDirections.N) == null) {
                        return;
                    }
                }
                object = GameServer.bServer ? null : ((IsoDirectionFrame)isoSprite.CurrentAnim.Frames.get((int)0)).getTexture((IsoDirections)IsoDirections.N).getName();
                boolean bl3 = true;
                if (!GameServer.bServer && object.contains((CharSequence)"TileObjectsExt") && (object.contains((CharSequence)"_5") || object.contains((CharSequence)"_6") || object.contains((CharSequence)"_7") || object.contains((CharSequence)"_8"))) {
                    isoObject = new IsoWheelieBin((IsoCell)isoCell, (int)n, (int)n2, (int)n3);
                    if (object.contains((CharSequence)"_5")) {
                        isoObject.dir = IsoDirections.S;
                    }
                    if (object.contains((CharSequence)"_6")) {
                        isoObject.dir = IsoDirections.W;
                    }
                    if (object.contains((CharSequence)"_7")) {
                        isoObject.dir = IsoDirections.N;
                    }
                    if (object.contains((CharSequence)"_8")) {
                        isoObject.dir = IsoDirections.E;
                    }
                    bl3 = false;
                }
                if (bl3) {
                    isoObject = IsoObject.getNew();
                    isoObject.sx = 0.0f;
                    isoObject.sprite = isoSprite;
                    isoObject.square = isoGridSquare;
                    CellLoader.AddObject((IsoGridSquare)isoGridSquare, (IsoObject)isoObject);
                    if (isoObject.sprite.getProperties().Is((String)"lightR")) {
                        float f = Float.parseFloat((String)isoObject.sprite.getProperties().Val((String)"lightR"));
                        float f4 = Float.parseFloat((String)isoObject.sprite.getProperties().Val((String)"lightG"));
                        float f5 = Float.parseFloat((String)isoObject.sprite.getProperties().Val((String)"lightB"));
                        isoCell.getLamppostPositions().add((Object)new IsoLightSource((int)isoObject.square.getX(), (int)isoObject.square.getY(), (int)isoObject.square.getZ(), (float)f, (float)f4, (float)f5, (int)8));
                    }
                }
            }
        }
        if (isoObject != null) {
            isoObject.tile = string;
            isoObject.createContainersFromSpriteProperties();
            if (isoObject.sprite.getProperties().Is((IsoFlagType)IsoFlagType.vegitation)) {
                isoObject.tintr = 0.7f + (float)Rand.Next((int)30) / 100.0f;
                isoObject.tintg = 0.7f + (float)Rand.Next((int)30) / 100.0f;
                isoObject.tintb = 0.7f + (float)Rand.Next((int)30) / 100.0f;
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     */
    public static boolean LoadCellBinaryChunk(IsoCell var0, int var1_1, int var2_2, IsoChunk var3_3) {
        var4_4 = var1_1;
        var5_5 = var2_2;
        var6_6 = "world_" + var4_4 / 30 + "_" + var5_5 / 30 + ".lotpack";
        if (!IsoLot.InfoFileNames.containsKey((Object)var6_6)) {
            DebugLog.log((String)("LoadCellBinaryChunk: NO SUCH LOT " + var6_6));
            return false;
        }
        var7_7 = new File((String)((String)IsoLot.InfoFileNames.get((Object)var6_6)));
        if (var7_7.exists()) {
            var8_8 = null;
            try {
                var8_8 = IsoLot.get((Integer)Integer.valueOf((int)(var4_4 / 30)), (Integer)Integer.valueOf((int)(var5_5 / 30)), (Integer)Integer.valueOf((int)var1_1), (Integer)Integer.valueOf((int)var2_2), (IsoChunk)var3_3);
                var0.PlaceLot((IsoLot)var8_8, (int)0, (int)0, (int)0, (IsoChunk)var3_3, (int)var1_1, (int)var2_2);
                ** if (var8_8 == null) goto lbl-1000
            }
            catch (Throwable var9_9) {
                if (var8_8 != null) {
                    IsoLot.put(var8_8);
                }
                throw var9_9;
            }
lbl-1000:
            // 1 sources

            {
                IsoLot.put((IsoLot)var8_8);
            }
lbl-1000:
            // 2 sources

            {
            }
            return true;
        }
        DebugLog.log((String)("LoadCellBinaryChunk: NO SUCH LOT " + var6_6));
        return false;
    }

    public static IsoCell LoadCellBinaryChunk(IsoSpriteManager isoSpriteManager, int n, int n2) throws IOException {
        wanderX = 0;
        wanderY = 0;
        wanderRoom = null;
        wanderX = 0;
        wanderY = 0;
        IsoCell isoCell = new IsoCell((int)300, (int)300);
        int n3 = IsoPlayer.numPlayers;
        n3 = 1;
        if (!GameServer.bServer) {
            if (GameClient.bClient) {
                WorldStreamer.instance.requestLargeAreaZip((int)n, (int)n2, (int)(IsoChunkMap.ChunkGridWidth / 2 + 2));
                IsoChunk.bDoServerRequests = false;
            }
            for (int i = 0; i < n3; ++i) {
                isoCell.ChunkMap[i].setInitialPos((int)n, (int)n2);
                IsoPlayer.assumedPlayer = i;
                IsoChunkMap cfr_ignored_0 = isoCell.ChunkMap[i];
                int n4 = n - IsoChunkMap.ChunkGridWidth / 2;
                IsoChunkMap cfr_ignored_1 = isoCell.ChunkMap[i];
                int n5 = n2 - IsoChunkMap.ChunkGridWidth / 2;
                IsoChunkMap cfr_ignored_2 = isoCell.ChunkMap[i];
                int n6 = n + IsoChunkMap.ChunkGridWidth / 2 + 1;
                IsoChunkMap cfr_ignored_3 = isoCell.ChunkMap[i];
                int n7 = n2 + IsoChunkMap.ChunkGridWidth / 2 + 1;
                for (int j = n4; j < n6; ++j) {
                    for (int k = n5; k < n7; ++k) {
                        if (!IsoWorld.instance.getMetaGrid().isValidChunk((int)j, (int)k)) continue;
                        isoCell.ChunkMap[i].LoadChunk((int)j, (int)k, (int)(j - n4), (int)(k - n5));
                    }
                }
            }
        }
        IsoPlayer.assumedPlayer = 0;
        LuaEventManager.triggerEvent((String)"OnPostMapLoad", (Object)isoCell, (Object)Integer.valueOf((int)n), (Object)Integer.valueOf((int)n2));
        CellLoader.ConnectMultitileObjects((IsoCell)isoCell);
        return isoCell;
    }

    private static void RecurseMultitileObjects(IsoCell isoCell, IsoGridSquare isoGridSquare, IsoGridSquare isoGridSquare2, ArrayList arrayList) {
        Object object;
        Iterator iterator = isoGridSquare2.getMovingObjects().iterator();
        Object object2 = null;
        boolean bl = false;
        while (iterator != null && iterator.hasNext()) {
            int n;
            IsoMovingObject isoMovingObject = (IsoMovingObject)iterator.next();
            if (!(isoMovingObject instanceof IsoPushableObject)) continue;
            object = (IsoPushableObject)isoMovingObject;
            int n2 = isoGridSquare.getX() - isoGridSquare2.getX();
            int n3 = isoGridSquare.getY() - isoGridSquare2.getY();
            if (n3 != 0 && isoMovingObject.sprite.getProperties().Is((String)"connectY") && (n = Integer.parseInt((String)isoMovingObject.sprite.getProperties().Val((String)"connectY"))) == n3) {
                ((IsoPushableObject)object).connectList = arrayList;
                arrayList.add((Object)object);
                object2 = object;
                bl = false;
                break;
            }
            if (n2 == 0 || !isoMovingObject.sprite.getProperties().Is((String)"connectX") || (n = Integer.parseInt((String)isoMovingObject.sprite.getProperties().Val((String)"connectX"))) != n2) continue;
            ((IsoPushableObject)object).connectList = arrayList;
            arrayList.add((Object)object);
            object2 = object;
            bl = true;
            break;
        }
        if (object2 != null) {
            if (((IsoPushableObject)object2).sprite.getProperties().Is((String)"connectY") && bl) {
                int n = Integer.parseInt((String)((IsoPushableObject)object2).sprite.getProperties().Val((String)"connectY"));
                object = isoCell.getGridSquare((int)((IsoMovingObject)object2).getCurrentSquare().getX(), (int)(((IsoMovingObject)object2).getCurrentSquare().getY() + n), (int)((IsoMovingObject)object2).getCurrentSquare().getZ());
                CellLoader.RecurseMultitileObjects((IsoCell)isoCell, (IsoGridSquare)((IsoMovingObject)object2).getCurrentSquare(), (IsoGridSquare)object, ((IsoPushableObject)object2).connectList);
            }
            if (((IsoPushableObject)object2).sprite.getProperties().Is((String)"connectX") && !bl) {
                int n = Integer.parseInt((String)((IsoPushableObject)object2).sprite.getProperties().Val((String)"connectX"));
                object = isoCell.getGridSquare((int)(((IsoMovingObject)object2).getCurrentSquare().getX() + n), (int)((IsoMovingObject)object2).getCurrentSquare().getY(), (int)((IsoMovingObject)object2).getCurrentSquare().getZ());
                CellLoader.RecurseMultitileObjects((IsoCell)isoCell, (IsoGridSquare)((IsoMovingObject)object2).getCurrentSquare(), (IsoGridSquare)object, ((IsoPushableObject)object2).connectList);
            }
        }
    }

    private static void ConnectMultitileObjects(IsoCell isoCell) {
        Iterator iterator = isoCell.getObjectList().iterator();
        while (iterator != null && iterator.hasNext()) {
            IsoGridSquare isoGridSquare;
            int n;
            IsoMovingObject isoMovingObject = (IsoMovingObject)iterator.next();
            if (!(isoMovingObject instanceof IsoPushableObject)) continue;
            IsoPushableObject isoPushableObject = (IsoPushableObject)isoMovingObject;
            if (!isoMovingObject.sprite.getProperties().Is((String)"connectY") && !isoMovingObject.sprite.getProperties().Is((String)"connectX") || isoPushableObject.connectList != null) continue;
            isoPushableObject.connectList = new ArrayList();
            isoPushableObject.connectList.add((Object)isoPushableObject);
            if (isoMovingObject.sprite.getProperties().Is((String)"connectY")) {
                n = Integer.parseInt((String)isoMovingObject.sprite.getProperties().Val((String)"connectY"));
                isoGridSquare = isoCell.getGridSquare((int)isoMovingObject.getCurrentSquare().getX(), (int)(isoMovingObject.getCurrentSquare().getY() + n), (int)isoMovingObject.getCurrentSquare().getZ());
                if (isoGridSquare == null) {
                    boolean bl = false;
                }
                CellLoader.RecurseMultitileObjects((IsoCell)isoCell, (IsoGridSquare)isoPushableObject.getCurrentSquare(), (IsoGridSquare)isoGridSquare, isoPushableObject.connectList);
            }
            if (!isoMovingObject.sprite.getProperties().Is((String)"connectX")) continue;
            n = Integer.parseInt((String)isoMovingObject.sprite.getProperties().Val((String)"connectX"));
            isoGridSquare = isoCell.getGridSquare((int)(isoMovingObject.getCurrentSquare().getX() + n), (int)isoMovingObject.getCurrentSquare().getY(), (int)isoMovingObject.getCurrentSquare().getZ());
            CellLoader.RecurseMultitileObjects((IsoCell)isoCell, (IsoGridSquare)isoPushableObject.getCurrentSquare(), (IsoGridSquare)isoGridSquare, isoPushableObject.connectList);
        }
    }

    private static void AddObject(IsoGridSquare isoGridSquare, IsoObject isoObject) {
        int n = isoGridSquare.placeWallAndDoorCheck((IsoObject)isoObject, (int)isoGridSquare.getObjects().size());
        if (n != isoGridSquare.getObjects().size() && n >= 0 && n <= isoGridSquare.getObjects().size()) {
            isoGridSquare.getObjects().add((int)n, (Object)isoObject);
        } else {
            isoGridSquare.getObjects().add((Object)isoObject);
        }
    }

    private static void AddSpecialObject(IsoGridSquare isoGridSquare, IsoObject isoObject) {
        int n = isoGridSquare.placeWallAndDoorCheck((IsoObject)isoObject, (int)isoGridSquare.getObjects().size());
        if (n != isoGridSquare.getObjects().size() && n >= 0 && n <= isoGridSquare.getObjects().size()) {
            isoGridSquare.getObjects().add((int)n, (Object)isoObject);
        } else {
            isoGridSquare.getObjects().add((Object)isoObject);
            isoGridSquare.getSpecialObjects().add((Object)isoObject);
        }
    }

    static {
        isoObjectCache = new ArrayDeque();
        isoTreeCache = new ArrayDeque();
        wanderX = 0;
        wanderY = 0;
        wanderRoom = null;
        missingTiles = new HashSet();
        smashedWindowSpriteMap = new HashMap();
    }
}
