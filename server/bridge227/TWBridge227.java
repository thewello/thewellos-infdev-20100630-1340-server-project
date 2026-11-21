import net.thewellodev.protocol.Protocol;
import net.thewellodev.server.game.GameBridge;
import net.thewellodev.server.game.NetEntity;
import net.thewellodev.server.game.TileData;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bridge to the original Infdev inf-20100227-1433 world code (see docs/versions/inf-20100227-1433.md).
 *
 * What this build has: terrain generated per chunk, blocks (ids only, no metadata, no light arrays), no mobs,
 * no minecarts, no signs, no tile entities, no world seed. The terrain generator is not seeded from level-seed, and
 * ungenerated terrain differs between runs (the cause is not traced yet). Saved chunks are kept as they are saved.
 * Everything the build does not have is a no-op here, so the server runs the same way it does for 20100630.
 *
 * Every game class is reached by reflection: the 20100227 jar has a class net/minecraft/a/a.class, so the
 * compiler cannot name net.minecraft.a.a.e or net.minecraft.a.a.f in source. Used: the world (e: block get and
 * set), its chunk store field (e.k), the store's chunk map (f.h), the store's private chunk getter (f.a(int,int)),
 * the chunk's block array (c.a) and the map key class (g).
 *
 * Saves: one GZIP NBT file per chunk (c.X.Z.dat) and level.dat, in worlds/inf-20100227-1433/&lt;level-name&gt;/.
 * Chunk files hold the same tags as 20100630 (Level compound: xPos, zPos, LastUpdate, Blocks, Data, SkyLight,
 * BlockLight, HeightMap, TerrainPopulated). Blocks use the same index (x &lt;&lt; 11 | z &lt;&lt; 7 | y).
 */
public class TWBridge227 implements GameBridge {

    private static final int BLOCKS = Protocol.CHUNK_BLOCKS;
    private static final int NIBBLES = Protocol.CHUNK_NIBBLES;
    private static final Pattern CHUNK_FILE = Pattern.compile("c\\.(-?\\d+)\\.(-?\\d+)\\.dat");

    private BlockListener blockListener;
    private EntityListener entityListener;

    /** The world (net.minecraft.a.a.e). */
    private Object world;
    /** Its chunk store (net.minecraft.a.a.f). */
    private Object store;
    private Class<?> storeClass;
    /** World.a(int,int,int): block id. */
    private Method getBlockMethod;
    /** World.a(int,int,int,int): sets a block id, returns whether it changed. */
    private Method setBlockMethod;
    /** The chunk store's map from chunk key to chunk (private field f.h). */
    private Map<Object, Object> chunks;
    /** The chunk store's private getter that loads or generates a chunk (f.a(int,int)). */
    private Method chunkAt;
    /** Constructor of the chunk map key (g(f,int,int)). */
    private Constructor<?> keyCtor;
    private Field keyX;
    private Field keyZ;
    /** Constructor of a chunk (c(f,byte[])). */
    private Constructor<?> chunkCtor;
    /** The block array of a chunk (c.a). */
    private Field chunkBlocks;

    private File levelDir;
    private long seed;
    private long time;
    private int[] spawn = {0, 64, 0};

    // ----- loading and saving -----------------------------------------------------------------------------

    public void setBlockListener(BlockListener listener) {
        this.blockListener = listener;
    }

    public void load(File container, String levelName, Long seedArg) {
        levelDir = new File(container, levelName);
        if (!levelDir.isDirectory() && !levelDir.mkdirs()) {
            throw new IllegalStateException("Could not create " + levelDir);
        }
        boolean existing = new File(levelDir, "level.dat").isFile();
        try {
            loadLevelFile();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read the world in " + levelDir + ": " + ex, ex);
        }
        if (!existing) {
            // a new world: the seed is the level-seed, or a random one; it is saved with the world
            seed = seedArg != null ? seedArg : new java.util.Random().nextLong();
        }
        TWSeed.reset(seed); // the terrain generators take their random numbers from it (see TWSeed)
        try {
            ClassLoader cl = getClass().getClassLoader();
            Class<?> worldClass = Class.forName("net.minecraft.a.a.e", true, cl);
            storeClass = Class.forName("net.minecraft.a.a.f", true, cl);
            world = worldClass.getConstructor().newInstance();
            getBlockMethod = worldClass.getMethod("a", int.class, int.class, int.class);
            setBlockMethod = worldClass.getMethod("a", int.class, int.class, int.class, int.class);
            store = field(worldClass, "k").get(world);
            chunks = castMap(field(storeClass, "h").get(store));
            chunkAt = storeClass.getDeclaredMethod("a", int.class, int.class);
            chunkAt.setAccessible(true);
            Class<?> keyClass = Class.forName("net.minecraft.a.a.g", true, cl);
            keyCtor = keyClass.getDeclaredConstructor(storeClass, int.class, int.class);
            keyCtor.setAccessible(true);
            keyX = field(keyClass, "a");
            keyZ = field(keyClass, "b");
            Class<?> chunkClass = Class.forName("net.minecraft.a.a.c", true, cl);
            chunkCtor = chunkClass.getDeclaredConstructor(storeClass, byte[].class);
            chunkCtor.setAccessible(true);
            chunkBlocks = field(chunkClass, "a");
        } catch (Exception ex) {
            throw new IllegalStateException("Could not start the inf-20100227-1433 world code: " + ex, ex);
        }
        try {
            loadChunkFiles();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read the world in " + levelDir + ": " + ex, ex);
        }
        if (!existing) {
            spawn = new int[]{0, getHighestY(0, 0), 0};
            try {
                writeLevelFile(); // keeps the seed even if the server never saves
            } catch (java.io.IOException ex) {
                throw new IllegalStateException("Could not save the world in " + levelDir + ": " + ex, ex);
            }
        }
    }

    public long getSeed() {
        return seed;
    }

    public int[] getSpawn() {
        return spawn.clone();
    }

    public void setSpawn(int x, int y, int z) {
        spawn = new int[]{x, y, z};
    }

    public long getTime() {
        return time;
    }

    public void setTime(long t) {
        time = t;
    }

    /** This build has no scheduled updates, entities or random ticks: nothing to do. */
    public void tick(int[] playerBlockPositions) {
    }

    public void save(boolean full) {
        if (world == null) {
            return;
        }
        try {
            if (!levelDir.isDirectory() && !levelDir.mkdirs()) {
                throw new java.io.IOException("Could not create " + levelDir);
            }
            // Every loaded chunk is written; the build keeps no dirty flags.
            for (Map.Entry<Object, Object> en : chunks.entrySet()) {
                Object key = en.getKey();
                int cx = signedCoord(keyX.getInt(key));
                int cz = signedCoord(keyZ.getInt(key));
                writeChunk(cx, cz, (byte[]) chunkBlocks.get(en.getValue()));
            }
            writeLevelFile();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not save the world in " + levelDir + ": " + ex, ex);
        }
    }

    private void loadLevelFile() throws java.io.IOException {
        File file = new File(levelDir, "level.dat");
        if (!file.isFile()) {
            return;
        }
        Map<String, Object> data = castStringMap(TW227Nbt.readFile(file).get("Data"));
        spawn = new int[]{number(data, "SpawnX"), number(data, "SpawnY"), number(data, "SpawnZ")};
        time = number(data, "Time");
        seed = ((Number) data.get("RandomSeed")).longValue();
    }

    private void writeLevelFile() throws java.io.IOException {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("RandomSeed", seed);
        data.put("SpawnX", spawn[0]);
        data.put("SpawnY", spawn[1]);
        data.put("SpawnZ", spawn[2]);
        data.put("Time", time);
        data.put("LastPlayed", System.currentTimeMillis());
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("Data", data);
        TW227Nbt.writeFile(new File(levelDir, "level.dat"), root);
    }

    private void loadChunkFiles() throws Exception {
        File[] files = levelDir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            Matcher m = CHUNK_FILE.matcher(file.getName());
            if (!m.matches()) {
                continue;
            }
            int cx = Integer.parseInt(m.group(1));
            int cz = Integer.parseInt(m.group(2));
            Map<String, Object> level = castStringMap(TW227Nbt.readFile(file).get("Level"));
            byte[] blocks = (byte[]) level.get("Blocks");
            if (blocks == null || blocks.length != BLOCKS) {
                throw new java.io.IOException("Bad chunk " + file.getName());
            }
            putChunk(cx, cz, blocks);
        }
    }

    private void writeChunk(int cx, int cz, byte[] blocks) throws Exception {
        Map<String, Object> level = new LinkedHashMap<String, Object>();
        level.put("xPos", cx);
        level.put("zPos", cz);
        level.put("LastUpdate", 0L);
        level.put("Blocks", blocks);
        level.put("Data", new byte[NIBBLES]);
        byte[] sky = new byte[NIBBLES];
        Arrays.fill(sky, (byte) 0xFF);
        level.put("SkyLight", sky);
        level.put("BlockLight", new byte[NIBBLES]);
        level.put("HeightMap", heightMap(blocks));
        level.put("TerrainPopulated", (byte) 1);
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("Level", level);
        TW227Nbt.writeFile(new File(levelDir, "c." + cx + "." + cz + ".dat"), root);
    }

    // ----- blocks and chunks ------------------------------------------------------------------------------

    public int getBlockId(int x, int y, int z) {
        try {
            return (Integer) getBlockMethod.invoke(world, x, y, z);
        } catch (Exception ex) {
            Throwable cause = ex instanceof java.lang.reflect.InvocationTargetException && ex.getCause() != null
                    ? ex.getCause() : ex;
            throw new IllegalStateException("Block read failed at " + x + "," + y + "," + z + ": " + cause, cause);
        }
    }

    /** This build has no block metadata. */
    public int getBlockMeta(int x, int y, int z) {
        return 0;
    }

    public boolean setBlock(int x, int y, int z, int id, int meta, boolean notify) {
        boolean changed;
        try {
            changed = (Boolean) setBlockMethod.invoke(world, x, y, z, id);
        } catch (Exception ex) {
            throw new IllegalStateException("Block write failed at " + x + "," + y + "," + z + ": " + ex, ex);
        }
        if (changed && notify && blockListener != null) {
            blockListener.onBlockChanged(x, y, z);
        }
        return changed;
    }

    public boolean isChunkLoaded(int cx, int cz) {
        try {
            return chunks.containsKey(key(cx, cz));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** Loads or generates the chunk and returns its raw data (see Protocol.CHUNK_RAW_SIZE). */
    public byte[] getChunkData(int cx, int cz) {
        try {
            Object chunk = chunkAt.invoke(store, gameCoord(cx), gameCoord(cz));
            byte[] blocks = (byte[]) chunkBlocks.get(chunk);
            byte[] raw = new byte[Protocol.CHUNK_RAW_SIZE];
            System.arraycopy(blocks, 0, raw, 0, BLOCKS);
            int off = BLOCKS + NIBBLES; // metadata: zeros (this build has none)
            Arrays.fill(raw, off, off + NIBBLES, (byte) 0xFF); // sky light: full (no light data in this build)
            off += NIBBLES + NIBBLES;   // block light: zeros
            System.arraycopy(heightMap(blocks), 0, raw, off, 256);
            return raw;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read chunk " + cx + "," + cz + ": " + ex, ex);
        }
    }

    private void putChunk(int cx, int cz, byte[] blocks) throws Exception {
        chunks.put(key(cx, cz), chunkCtor.newInstance(store, blocks));
    }

    private Object key(int cx, int cz) throws Exception {
        return keyCtor.newInstance(store, gameCoord(cx), gameCoord(cz));
    }

    /**
     * The game finds the chunk of a block with an unsigned shift (x >>> 4), so the chunks at negative coordinates
     * are kept under huge positive numbers (-1 is 0x0FFFFFFF). Everything that touches the game's chunk map has to
     * use the same numbers, or the block reads and writes of the game and the chunk data would be different chunks.
     */
    private static int gameCoord(int chunkCoord) {
        return chunkCoord & 0x0FFFFFFF;
    }

    /** The signed chunk coordinate of one of the game's chunk numbers. */
    private static int signedCoord(int gameCoord) {
        return (gameCoord << 4) >> 4;
    }

    /** Height map of a chunk: index z * 16 + x, the value is the highest solid y plus one. */
    private static byte[] heightMap(byte[] blocks) {
        byte[] map = new byte[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int h = 0;
                for (int y = 127; y >= 0; y--) {
                    if (isSolidId(blocks[(x << 11) | (z << 7) | y] & 0xFF)) {
                        h = y + 1;
                        break;
                    }
                }
                map[z * 16 + x] = (byte) h;
            }
        }
        return map;
    }

    // ----- block rules ------------------------------------------------------------------------------------

    /**
     * Solid block (the player cannot walk through it). Ids from the 20100227 block registry (net/minecraft/a/a/b/x):
     * not solid are air (0), sapling (6), water and lava (8-11), flowers and mushrooms (37-40), torch (50), fire (51).
     */
    private static boolean isSolidId(int id) {
        if (id == 0 || id == 6 || (id >= 8 && id <= 11) || (id >= 37 && id <= 40) || id == 50 || id == 51) {
            return false;
        }
        return true;
    }

    public boolean isSolid(int x, int y, int z) {
        return y >= 0 && y < 128 && isSolidId(getBlockId(x, y, z));
    }

    /** Standing height in the column: the air above the highest block, or -1 if that block is water, lava or fire. */
    public int safeY(int x, int z) {
        for (int y = 127; y > 0; y--) {
            int id = getBlockId(x, y, z);
            if (id == 0 || id == 18) {
                continue; // air and leaves: keep looking for the ground
            }
            if (id == 8 || id == 9 || id == 10 || id == 11 || id == 51) {
                return -1;
            }
            return y + 1;
        }
        return -1;
    }

    /**
     * The single player of this build is created at (0, 64, 0), moved up one block at a time while it is inside a
     * block, and then falls to the ground. The same is worked out here, so a multiplayer player starts where a
     * single player does.
     */
    public double[] exactSpawn() {
        int feet = 64;
        while (feet < 126 && (columnsSolid(feet) || columnsSolid(feet + 1))) {
            feet++;
        }
        while (feet > 1 && !columnsSolid(feet - 1)) {
            feet--;
        }
        return new double[]{0.0, feet + 0.05, 0.0};
    }

    private boolean columnsSolid(int y) {
        return isSolid(0, y, 0) || isSolid(-1, y, 0) || isSolid(0, y, -1) || isSolid(-1, y, -1);
    }

    public int getHighestY(int x, int z) {
        for (int y = 127; y >= 0; y--) {
            if (isSolid(x, y, z)) {
                return y + 1;
            }
        }
        return 0;
    }

    // ----- what this build does not have ------------------------------------------------------------------

    public int getLoadedEntityCount() {
        return 0;
    }

    public int getLoadedChunkCount() {
        return chunks.size();
    }

    /** Block ids 1..255 exist in this build (the item list is the block list). */
    public boolean isValidItem(int id) {
        return id > 0 && id < 256;
    }

    public void setEntityListener(EntityListener listener) {
        this.entityListener = listener;
    }

    public void setMobSettings(int difficulty, boolean monsters, boolean animals) {
    }

    public void setMobCaps(int monsterCap, int animalCap) {
    }

    public void updatePlayer(String name, double x, double y, double z, float yaw, float pitch, boolean targetable) {
    }

    public boolean placeMinecart(int x, int y, int z) {
        return false;
    }

    public boolean setRider(String player, int netId) {
        return false;
    }

    public void setRiderInput(String player, float mx, float mz) {
    }

    public boolean saddle(int netId) {
        return false;
    }

    public int foodHeal(int itemId) {
        return 0;
    }

    public String blockSoundName(int blockId, boolean breaking) {
        return null;
    }

    public float blockSoundVolume(int blockId) {
        return 1f;
    }

    public float blockSoundPitch(int blockId) {
        return 1f;
    }

    public void fireArrow(String player) {
    }

    public void removePlayer(String name) {
    }

    public boolean attackMob(String attacker, int netId, int damage) {
        return false;
    }

    public List<NetEntity> getNetEntities() {
        return new ArrayList<NetEntity>();
    }

    public List<TileData> getTiles(int cx, int cz) {
        return new ArrayList<TileData>();
    }

    public boolean isLockableBlock(int x, int y, int z) {
        return false;
    }

    /** A 1-byte empty tag: there are no tile entities in this build. */
    public byte[] getTileNow(int x, int y, int z) {
        return new byte[]{0};
    }

    public byte[] applyTile(int x, int y, int z, byte[] data) {
        return null;
    }

    public boolean placePainting(int x, int y, int z, int dir, int motive) {
        return false;
    }

    public void igniteTnt(int x, int y, int z) {
    }

    public boolean spawnMob(int type, double x, double y, double z) {
        return false;
    }

    public int killAllMobs() {
        return 0;
    }

    /** The game's stack sizes are not extracted for this build yet: 64 for everything. */
    public int getMaxStackSize(int id) {
        return 64;
    }

    public int getMobCount() {
        return 0;
    }

    public int trimMobs(int max) {
        return 0;
    }

    public int getTntCount() {
        return 0;
    }

    public int clearTnt() {
        return 0;
    }

    // ----- helpers ----------------------------------------------------------------------------------------

    private static Field field(Class<?> c, String name) throws NoSuchFieldException {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private static int number(Map<String, Object> m, String key) {
        return ((Number) m.get(key)).intValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> castMap(Object o) {
        return (Map<Object, Object>) o;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castStringMap(Object o) {
        return (Map<String, Object>) o;
    }
}
