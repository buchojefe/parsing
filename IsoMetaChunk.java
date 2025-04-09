/*
 * Decompiled with CFR.
 */
package zombie.iso;

import java.lang.Object;
import java.lang.System;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import zombie.SandboxOptions;
import zombie.core.Rand;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;

public final class IsoMetaChunk {
    public static final float zombiesMinPerChunk = 0.06f;
    public static final float zombiesFullPerChunk = 12.0f;
    private int ZombieIntensity;
    private IsoMetaGrid.Zone[] zones;
    private int zonesSize;
    private RoomDef[] rooms;
    private int roomsSize;

    public IsoMetaChunk() {
        super();
        this.ZombieIntensity = 0;
    }

    public float getZombieIntensity(boolean bl) {
        float f = (float)this.ZombieIntensity;
        float f2 = f / 255.0f;
        if (SandboxOptions.instance.Distribution.getValue() == 2) {
            f = 128.0f;
            f2 = 0.5f;
        }
        f *= 0.5f;
        if (SandboxOptions.instance.Zombies.getValue() == 1) {
            f *= 4.0f;
        } else if (SandboxOptions.instance.Zombies.getValue() == 2) {
            f *= 3.0f;
        } else if (SandboxOptions.instance.Zombies.getValue() == 3) {
            f *= 2.0f;
        } else if (SandboxOptions.instance.Zombies.getValue() == 5) {
            f *= 0.35f;
        } else if (SandboxOptions.instance.Zombies.getValue() == 6) {
            f = 0.0f;
        }
        f2 = f / 255.0f;
        float f3 = 11.94f;
        f = 0.06f + (f3 *= f2);
        if (!bl) {
            return f;
        }
        float f4 = f2 * 10.0f;
        if (Rand.Next((int)3) == 0) {
            return 0.0f;
        }
        f4 *= 0.5f;
        int n = 1000;
        if (SandboxOptions.instance.Zombies.getValue() == 1) {
            n = (int)((float)n / 2.0f);
        } else if (SandboxOptions.instance.Zombies.getValue() == 2) {
            n = (int)((float)n / 1.7f);
        } else if (SandboxOptions.instance.Zombies.getValue() == 3) {
            n = (int)((float)n / 1.5f);
        } else if (SandboxOptions.instance.Zombies.getValue() == 5) {
            n = (int)((float)n * 1.5f);
        }
        if ((float)Rand.Next((int)n) < f4 && IsoWorld.getZombiesEnabled() && (f = 120.0f) > 12.0f) {
            f = 12.0f;
        }
        return f;
    }

    public float getZombieIntensity() {
        return this.getZombieIntensity((boolean)true);
    }

    public void setZombieIntensity(int n) {
        if (n >= 0) {
            this.ZombieIntensity = n;
        }
    }

    public float getLootZombieIntensity() {
        float f = (float)this.ZombieIntensity;
        float f2 = f / 255.0f;
        f2 = f / 255.0f;
        float f3 = 11.94f;
        f = 0.06f + (f3 *= f2);
        float f4 = f2 * 10.0f;
        f2 = f2 * f2 * f2;
        if ((float)Rand.Next((int)300) <= f4) {
            f = 120.0f;
        }
        if (IsoWorld.getZombiesDisabled()) {
            return 400.0f;
        }
        return f;
    }

    public int getUnadjustedZombieIntensity() {
        return this.ZombieIntensity;
    }

    public void addZone(IsoMetaGrid.Zone zone) {
        if (this.zones == null) {
            this.zones = new IsoMetaGrid.Zone[8];
        }
        if (this.zonesSize == this.zones.length) {
            IsoMetaGrid.Zone[] zoneArray = new IsoMetaGrid.Zone[this.zones.length + 8];
            System.arraycopy((Object)this.zones, (int)0, (Object)zoneArray, (int)0, (int)this.zonesSize);
            this.zones = zoneArray;
        }
        this.zones[this.zonesSize++] = zone;
    }

    public void removeZone(IsoMetaGrid.Zone zone) {
        if (this.zones == null) {
            return;
        }
        for (int i = 0; i < this.zonesSize; ++i) {
            if (this.zones[i] != zone) continue;
            while (i < this.zonesSize - 1) {
                this.zones[i] = this.zones[i + 1];
                ++i;
            }
            this.zones[this.zonesSize - 1] = null;
            --this.zonesSize;
            break;
        }
    }

    public IsoMetaGrid.Zone getZone(int n) {
        if (n < 0 || n >= this.zonesSize) {
            return null;
        }
        return this.zones[n];
    }

    public IsoMetaGrid.Zone getZoneAt(int n, int n2, int n3) {
        if (this.zones == null || this.zonesSize <= 0) {
            return null;
        }
        IsoMetaGrid.Zone zone = null;
        for (int i = this.zonesSize - 1; i >= 0; --i) {
            IsoMetaGrid.Zone zone2 = this.zones[i];
            if (!zone2.contains((int)n, (int)n2, (int)n3)) continue;
            if (zone2.isPreferredZoneForSquare) {
                return zone2;
            }
            if (zone != null) continue;
            zone = zone2;
        }
        return zone;
    }

    public ArrayList getZonesAt(int n, int n2, int n3, ArrayList arrayList) {
        for (int i = 0; i < this.zonesSize; ++i) {
            IsoMetaGrid.Zone zone = this.zones[i];
            if (!zone.contains((int)n, (int)n2, (int)n3)) continue;
            arrayList.add((Object)zone);
        }
        return arrayList;
    }

    public void getZonesUnique(Set set) {
        for (int i = 0; i < this.zonesSize; ++i) {
            IsoMetaGrid.Zone zone = this.zones[i];
            set.add((Object)zone);
        }
    }

    public void getZonesIntersecting(int n, int n2, int n3, int n4, int n5, ArrayList arrayList) {
        for (int i = 0; i < this.zonesSize; ++i) {
            IsoMetaGrid.Zone zone = this.zones[i];
            if (arrayList.contains((Object)zone) || !zone.intersects((int)n, (int)n2, (int)n3, (int)n4, (int)n5)) continue;
            arrayList.add((Object)zone);
        }
    }

    public void clearZones() {
        if (this.zones != null) {
            for (int i = 0; i < this.zones.length; ++i) {
                this.zones[i] = null;
            }
        }
        this.zones = null;
        this.zonesSize = 0;
    }

    public void clearRooms() {
        if (this.rooms != null) {
            for (int i = 0; i < this.rooms.length; ++i) {
                this.rooms[i] = null;
            }
        }
        this.rooms = null;
        this.roomsSize = 0;
    }

    public int numZones() {
        return this.zonesSize;
    }

    public void addRoom(RoomDef roomDef) {
        if (this.rooms == null) {
            this.rooms = new RoomDef[8];
        }
        if (this.roomsSize == this.rooms.length) {
            RoomDef[] roomDefArray = new RoomDef[this.rooms.length + 8];
            System.arraycopy((Object)this.rooms, (int)0, (Object)roomDefArray, (int)0, (int)this.roomsSize);
            this.rooms = roomDefArray;
        }
        this.rooms[this.roomsSize++] = roomDef;
    }

    public RoomDef getRoomAt(int n, int n2, int n3) {
        for (int i = 0; i < this.roomsSize; ++i) {
            RoomDef roomDef = this.rooms[i];
            if (roomDef.isEmptyOutside() || roomDef.level != n3) continue;
            for (int j = 0; j < roomDef.rects.size(); ++j) {
                RoomDef.RoomRect roomRect = (RoomDef.RoomRect)roomDef.rects.get((int)j);
                if (roomRect.x > n || roomRect.y > n2 || n >= roomRect.getX2() || n2 >= roomRect.getY2()) continue;
                return roomDef;
            }
        }
        return null;
    }

    public RoomDef getEmptyOutsideAt(int n, int n2, int n3) {
        for (int i = 0; i < this.roomsSize; ++i) {
            RoomDef roomDef = this.rooms[i];
            if (!roomDef.isEmptyOutside() || roomDef.level != n3) continue;
            for (int j = 0; j < roomDef.rects.size(); ++j) {
                RoomDef.RoomRect roomRect = (RoomDef.RoomRect)roomDef.rects.get((int)j);
                if (roomRect.x > n || roomRect.y > n2 || n >= roomRect.getX2() || n2 >= roomRect.getY2()) continue;
                return roomDef;
            }
        }
        return null;
    }

    public int getNumRooms() {
        return this.roomsSize;
    }

    public void getRoomsIntersecting(int n, int n2, int n3, int n4, ArrayList arrayList) {
        for (int i = 0; i < this.roomsSize; ++i) {
            RoomDef roomDef = this.rooms[i];
            if (roomDef.isEmptyOutside() || arrayList.contains((Object)roomDef) || !roomDef.intersects((int)n, (int)n2, (int)n3, (int)n4)) continue;
            arrayList.add((Object)roomDef);
        }
    }

    public void Dispose() {
        if (this.rooms != null) {
            Arrays.fill((Object[])this.rooms, null);
        }
        if (this.zones != null) {
            Arrays.fill((Object[])this.zones, null);
        }
    }
}
