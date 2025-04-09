/*
 * Decompiled with CFR.
 */
package zombie;

import java.io.File;
import java.lang.Long;
import java.lang.Object;
import java.lang.String;
import java.util.concurrent.ConcurrentHashMap;
import zombie.ZomboidFileSystem;
import zombie.core.Core;

public final class ChunkMapFilenames {
    public static ChunkMapFilenames instance;
    public final ConcurrentHashMap<Long, Object> Map;
    public final ConcurrentHashMap<Long, Object> HeaderMap;
    String prefix;
    private File dirFile;
    private String cacheDir;

    public ChunkMapFilenames() {
        super();
        this.Map = new ConcurrentHashMap();
        this.HeaderMap = new ConcurrentHashMap();
        this.prefix = "map_";
    }

    public void clear() {
        this.dirFile = null;
        this.cacheDir = null;
        this.Map.clear();
        this.HeaderMap.clear();
    }

    public File getFilename(int n, int n2) {
        long l = (long)n << 32 | (long)n2;
        if (this.Map.containsKey((Object)Long.valueOf((long)l))) {
            return (File)this.Map.get((Object)Long.valueOf((long)l));
        }
        if (this.cacheDir == null) {
            this.cacheDir = ZomboidFileSystem.instance.getGameModeCacheDir();
        }
        String string = this.cacheDir + File.separator + Core.GameSaveWorld + File.separator + this.prefix + n + "_" + n2 + ".bin";
        File file = new File((String)string);
        this.Map.put((Object)Long.valueOf((long)l), (Object)file);
        return file;
    }

    public File getDir(String string) {
        if (this.cacheDir == null) {
            this.cacheDir = ZomboidFileSystem.instance.getGameModeCacheDir();
        }
        if (this.dirFile == null) {
            this.dirFile = new File((String)(this.cacheDir + File.separator + string));
        }
        return this.dirFile;
    }

    public String getHeader(int n, int n2) {
        long l = (long)n << 32 | (long)n2;
        if (this.HeaderMap.containsKey((Object)Long.valueOf((long)l))) {
            return this.HeaderMap.get((Object)Long.valueOf((long)l)).toString();
        }
        String string = n + "_" + n2 + ".lotheader";
        this.HeaderMap.put((Object)Long.valueOf((long)l), (Object)string);
        return string;
    }

    static {
        instance = new ChunkMapFilenames();
    }
}
