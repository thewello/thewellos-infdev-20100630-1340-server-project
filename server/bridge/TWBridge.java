import net.thewellodev.protocol.Protocol;
import net.thewellodev.server.game.GameBridge;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.thewellodev.server.game.NetEntity;

/**
 * The implementation of GameBridge on top of the obfuscated Infdev classes.
 *
 * fe = World, jw = Chunk, ch = IChunkProvider, gs = Block, ic = NibbleArray
 */
public class TWBridge implements GameBridge {

    private fe w;
    private final java.util.Random rand = new java.util.Random();
    private int lcg = new java.util.Random().nextInt();

    public void setBlockListener(BlockListener listener) {
        TWServerHook.listener = listener;
    }

    public void load(File container, String levelName, Long seed) {
        container.mkdirs();
        if (seed == null) {
            w = new fe(container, levelName);
        } else {
            w = new fe(container, levelName, seed.longValue());
        }
        TWServerHook.world = w;
        w.a((bv) new TWListener());
        TWServerHook.nobody = new TWProxy(w, "");
        TWServerHook.nobody.b(0.5D, -10000.0D, 0.5D);
        w.f = TWServerHook.nobody;
        w.g = difficulty;
    }

    public long getSeed() {
        return w.n;
    }

    public int[] getSpawn() {
        return new int[]{w.j, w.k, w.l};
    }

    public void setSpawn(int x, int y, int z) {
        w.j = x;
        w.k = y;
        w.l = z;
    }

    public long getTime() {
        return w.b;
    }

    public void setTime(long time) {
        w.b = time;
    }

    public void tick(int[] players) {
        // The game's temporary object pools (AABB 'en' and Vec3 'bo') are normally reset every tick by the client loop;
        // if not reset here, these pools grow without limit (hundreds of MB per minute)
        en.a();
        bo.a();
        // scheduled block updates (water, lava, sand...)
        try {
            w.a(false);
        } catch (RuntimeException e) {
            reportError("block updates", e);
        }
        // Remove items created on the server by block updates (e.g. a torch that breaks when its neighbour breaks):
        // the clients create and share these themselves, so they must not be duplicated.
        for (int i = 0; i < w.r.size(); i++) {
            Object e = w.r.get(i);
            if (e instanceof gx) {
                ((gx) e).l();
            }
        }
        tickCount++;
        TWServerHook.ticks = tickCount;
        if (tickCount % 40 == 0) {
            checkProxies();
        }
        // Natural mob spawning (every tick, as in single-player, taking turns between players)
        try {
            spawnMobs();
        } catch (RuntimeException e) {
            reportError("mob dogmasi", e);
        }
        // Entities and tile entities (mobs, arrows, falling sand/gravel...)
        try {
            w.d();
        } catch (RuntimeException e) {
            reportError("entity updates", e);
        }
        // Items created during entity updates (drops from dead mobs, explosions, falling sand)
        // Clients do not have these: convert them to shared items.
        shareNewItems(0);
        relayPickups();
        sweepBadEntities();
        // Time and sky light
        w.b++;
        int sky = w.a(1.0F);
        if (sky != w.c) {
            w.c = sky;
        }
        // random ticks around players (grass spread, sapling growth, fire...)
        for (int p = 0; p + 1 < players.length; p += 2) {
            int px = players[p];
            int pz = players[p + 1];
            for (int i = 0; i < 4000; i++) {
                lcg = lcg * 3 + 1013904223;
                int r = lcg >> 2;
                int x = (r & 127) - 64 + px;
                int z = (r >> 8 & 127) - 64 + pz;
                int y = r >> 16 & 127;
                if (!w.B.a(x >> 4, z >> 4)) {
                    continue;
                }
                int id = w.a(x, y, z);
                if (id > 0 && gs.o[id]) {
                    gs.n[id].a(w, x, y, z, rand);
                }
            }
        }
        // light updates
        int n = 0;
        while (n++ < 1000 && w.g()) {
        }
        en.a();
        bo.a();
    }

    private long lastErrorAt;

    private void reportError(String what, RuntimeException e) {
        long now = System.currentTimeMillis();
        if (now - lastErrorAt > 10000) {
            lastErrorAt = now;
            System.err.println("World tick error (" + what + "): " + e);
            e.printStackTrace();
        }
    }

    public int getBlockId(int x, int y, int z) {
        return w.a(x, y, z);
    }

    public int getBlockMeta(int x, int y, int z) {
        return w.e(x, y, z);
    }

    public boolean setBlock(int x, int y, int z, int id, int meta, boolean notify) {
        if (y < 0 || y >= 128) {
            return false;
        }
        // Same block id, only the metadata changes (opening or closing a door, sapling stage...): the game's
        // set-id-and-metadata methods do nothing when the id is the same, so the metadata needs its own method.
        if (w.a(x, y, z) == id && id != 0) {
            return w.c(x, y, z, meta); // the game itself only calls this for door-like metadata changes
        }
        if (notify) {
            return w.b(x, y, z, id, meta);
        }
        return w.a(x, y, z, id, meta);
    }

    public boolean isChunkLoaded(int cx, int cz) {
        return w.B.a(cx, cz);
    }

    public byte[] getChunkData(int cx, int cz) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                w.b(cx + dx, cz + dz);
            }
        }
        int n = 0;
        while (n++ < 100000 && w.g()) {
        }
        jw c = w.b(cx, cz);
        byte[] raw = new byte[Protocol.CHUNK_RAW_SIZE];
        int off = 0;
        System.arraycopy(c.b, 0, raw, off, Protocol.CHUNK_BLOCKS);
        off += Protocol.CHUNK_BLOCKS;
        System.arraycopy(c.e.a, 0, raw, off, Protocol.CHUNK_NIBBLES);
        off += Protocol.CHUNK_NIBBLES;
        System.arraycopy(c.f.a, 0, raw, off, Protocol.CHUNK_NIBBLES);
        off += Protocol.CHUNK_NIBBLES;
        System.arraycopy(c.g.a, 0, raw, off, Protocol.CHUNK_NIBBLES);
        off += Protocol.CHUNK_NIBBLES;
        System.arraycopy(c.h, 0, raw, off, 256);
        return raw;
    }

    public boolean isSolid(int x, int y, int z) {
        return y >= 0 && y < 128 && w.g(x, y, z);
    }

    public int safeY(int x, int z) {
        if (!w.B.a(x >> 4, z >> 4)) {
            return -1;
        }
        int tried = 0;
        for (int y = 127; y > 0 && tried < 24; y--) {
            int id = w.a(x, y, z);
            if (id == 0 || id == 18) {
                continue; // air and leaves: do not climb into trees, stop at the ground
            }
            if (id == 8 || id == 9 || id == 10 || id == 11 || id == 51) {
                return -1; // su, lav, ates
            }
            tried++;
            if (w.a(x, y + 1, z) == 0 && (y + 2 > 127 || w.a(x, y + 2, z) == 0)) {
                return y + 1; // solid block below, two empty blocks above
            }
        }
        return -1;
    }

    public int getHighestY(int x, int z) {
        for (int y = 127; y > 0; y--) {
            if (w.a(x, y, z) != 0) {
                return y + 1;
            }
        }
        return 1;
    }

    public void save(boolean full) {
        w.a(full, (jd) null);
    }

    public int getLoadedEntityCount() {
        return w.r.size();
    }

    public boolean isValidItem(int id) {
        if (id <= 0 || id >= ge.c.length) {
            return false;
        }
        return id < 256 ? gs.n[id] != null : ge.c[id] != null;
    }
    // ------------------------------------------------------------------ mob'lar

    private int tickCount;
    private int difficulty = 2;
    private boolean monsters = true, animals = true;
    /** Maximum monsters and animals per player (within 128 blocks). Defaults are the game's original values. */
    private int monsterCap = 100, animalCap = 20;
    private int spawnTurn;
    private final Map<String, TWProxy> proxyByName = new HashMap<String, TWProxy>();
    private final Map<Integer, eq> byNetId = new HashMap<Integer, eq>();
    private final Map<eq, NetEntity> infos = new IdentityHashMap<eq, NetEntity>();
    private final Map<eq, Integer> lastHurtTime = new IdentityHashMap<eq, Integer>();
    private final List<NetEntity> netList = new ArrayList<NetEntity>();
    private int nextNetId = Protocol.MOB_ID_BASE;
    private final Map<Class<?>, Integer> typeOf = new HashMap<Class<?>, Integer>();

    private static final Class<?>[] MONSTERS = {gw.class, fo.class, fy.class, ci.class};
    private static final Class<?>[] ANIMALS = {dn.class, ib.class};

    public TWBridge() {
        Class<?>[] cls = {gw.class, fo.class, fy.class, ci.class, dn.class, ib.class, eo.class, ac.class, db.class, ij.class, cy.class, jp.class};
        for (int i = 0; i < cls.length; i++) {
            if (!cls[i].getName().equals(Protocol.MOB_CLASSES[i])) {
                throw new IllegalStateException("Mob sinif tablosu uyusmuyor: " + Protocol.MOB_CLASSES[i]);
            }
            typeOf.put(cls[i], i);
        }
    }

    public void setEntityListener(EntityListener l) {
        TWServerHook.entityListener = l;
    }

    public void setMobCaps(int monsterCap, int animalCap) {
        this.monsterCap = Math.max(0, Math.min(500, monsterCap));
        this.animalCap = Math.max(0, Math.min(200, animalCap));
    }

    public void setMobSettings(int difficulty, boolean monsters, boolean animals) {
        this.difficulty = Math.max(0, Math.min(3, difficulty));
        this.monsters = monsters;
        this.animals = animals;
        if (w != null) {
            w.g = this.difficulty; // difficulty 0 (peaceful): monsters despawn on their own (gr.b_)
        }
    }

    public void updatePlayer(String name, double x, double y, double z, float yaw, float pitch, boolean targetable) {
        TWProxy p = proxyByName.get(name);
        if (p == null) {
            p = new TWProxy(w, name);
            proxyByName.put(name, p);
        }
        p.targetable = targetable;
        p.b(x, y + p.z, z); // k = goz yuksekligi; z = 1.62 (yOffset)
        p.p = yaw;
        p.q = pitch;
        if (!TWServerHook.proxies.contains(p) && w.B.a(hq.b(x) >> 4, hq.b(z) >> 4)) {
            p.y = false;
            w.a((eq) p);
            TWServerHook.proxies.add(p);
        }
    }

    public void removePlayer(String name) {
        TWProxy p = proxyByName.remove(name);
        if (p != null) {
            if (p.e != null) {
                p.h(p.e); // dismount: the minecart is left empty
            }
            TWServerHook.proxies.remove(p);
            if (w.r.contains(p)) {
                p.l();
            } else {
                p.y = true;
            }
        }
    }

    /** A proxy may have fallen out of the world when its chunk was unloaded: remove it from the list; updatePlayer adds it again. */
    private void checkProxies() {
        for (Iterator<TWProxy> it = TWServerHook.proxies.iterator(); it.hasNext(); ) {
            TWProxy p = it.next();
            if (p.y || !w.r.contains(p)) {
                it.remove();
            }
        }
    }

    public boolean attackMob(String attacker, int netId, int damage) {
        eq e = byNetId.get(netId);
        TWProxy p = proxyByName.get(attacker);
        if (e == null || e.y || p == null || (e instanceof ka && ((ka) e).au <= 0)) {
            return false;
        }
        if (!(e instanceof ka) && !(e instanceof cy) && !(e instanceof jp)) {
            return false;
        }
        int before = w.r.size();
        boolean hit = e.a((eq) p, damage);
        shareNewItems(before);
        return hit;
    }

    /** Converts the new items in w.r (after 'from') into shared items and removes the server copies. */
    private void shareNewItems(int from) {
        EntityListener l = TWServerHook.entityListener;
        for (int i = from; i < w.r.size(); i++) {
            Object o = w.r.get(i);
            if (!(o instanceof gx)) {
                continue;
            }
            gx it = (gx) o;
            if (it.y) {
                continue;
            }
            it.l();
            if (l != null && it.a != null && it.a.a > 0) {
                l.onItemSpawned(it.j, it.k, it.l, it.m, it.n, it.o, it.a.c, it.a.a, it.a.d);
            }
        }
    }

    private byte[] serTile(bk te) {
        try {
            ae tag = new ae();
            te.b(tag);
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            tag.a((java.io.DataOutput) new java.io.DataOutputStream(bo));
            return bo.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean tileBlock(int id, bk te) {
        return (te instanceof ii && id == 54) || (te instanceof el && (id == 61 || id == 62))
                || (te instanceof jn && (id == 63 || id == 68));
    }

    public List<net.thewellodev.server.game.TileData> getTiles(int cx, int cz) {
        List<net.thewellodev.server.game.TileData> out = new ArrayList<net.thewellodev.server.game.TileData>();
        if (!w.B.a(cx, cz)) {
            return out;
        }
        jw c = w.b(cx, cz);
        for (Object o : new ArrayList<Object>(c.l.values())) {
            bk te = (bk) o;
            if (tileBlock(w.a(te.b, te.c, te.d), te)) {
                byte[] d = serTile(te);
                if (d != null && d.length < 4000) {
                    out.add(new net.thewellodev.server.game.TileData(te.b, te.c, te.d, d));
                }
            }
        }
        return out;
    }

    public boolean isLockableBlock(int x, int y, int z) {
        if (y < 0 || y >= 128 || !w.B.a(x >> 4, z >> 4)) {
            return false;
        }
        int id = w.a(x, y, z);
        return id == 54 || id == 61 || id == 62;
    }

    public byte[] getTileNow(int x, int y, int z) {
        if (y < 0 || y >= 128 || !w.B.a(x >> 4, z >> 4)) {
            return new byte[]{0};
        }
        bk te = w.b(x, y, z);
        byte[] d = te == null || !tileBlock(w.a(x, y, z), te) ? null : serTile(te);
        return d == null ? new byte[]{0} : d;
    }

    public byte[] applyTile(int x, int y, int z, byte[] data) {
        if (y < 0 || y >= 128 || !w.B.a(x >> 4, z >> 4)) {
            return null;
        }
        try {
            ae tag = new ae();
            tag.a((java.io.DataInput) new java.io.DataInputStream(new java.io.ByteArrayInputStream(data)));
            tag.a("x", x);
            tag.a("y", y);
            tag.a("z", z);
            bk te = w.b(x, y, z);
            if (te == null) {
                // A chest, furnace or sign block exists but its tile entity does not exist yet: create it from the tag
                bk fresh = bk.c(tag);
                if (fresh == null || !tileBlock(w.a(x, y, z), fresh)) {
                    return null;
                }
                fresh.a = w;
                w.a(x, y, z, fresh);
                te = fresh;
            } else {
                if (!tileBlock(w.a(x, y, z), te)) {
                    return null;
                }
                te.a(tag);
            }
            if (te instanceof kd) {
                kd inv = (kd) te;
                for (int i = 0; i < inv.a(); i++) {
                    hw s = inv.a(i);
                    if (s != null && (s.a <= 0 || s.a > 64 || s.d < 0 || s.d > 1000 || !isValidItem(s.c))) {
                        inv.a(i, null);
                    }
                }
            } else if (te instanceof jn) {
                jn sign = (jn) te;
                for (int i = 0; i < sign.e.length; i++) {
                    String t = sign.e[i] == null ? "" : sign.e[i];
                    sign.e[i] = t.length() > 15 ? t.substring(0, 15) : t;
                }
            }
            return serTile(te);
        } catch (Throwable t) {
            return null;
        }
    }

    public boolean placeMinecart(int x, int y, int z) {
        if (y < 1 || y > 126 || !w.B.a(x >> 4, z >> 4) || w.a(x, y, z) != gs.aG.aJ || w.b(jp.class) >= 100) {
            return false;
        }
        jp cartE = new jp(w, x + 0.5F, y + 0.5F, z + 0.5F);
        w.a((eq) cartE);
        return true;
    }

    public boolean setRider(String player, int netId) {
        TWProxy p = proxyByName.get(player);
        if (p == null) {
            return false;
        }
        if (netId < 0) {
            if (p.e != null) {
                p.h(p.e);
            }
            p.inM = 0;
            p.inO = 0;
            return true;
        }
        eq v = byNetId.get(netId);
        if (!(v instanceof jp || (v instanceof ib && ((ib) v).a)) || v.y) {
            return false;
        }
        double dx = v.j - p.j, dy = v.k - p.k, dz = v.l - p.l;
        if (dx * dx + dy * dy + dz * dz > 8 * 8) {
            return false;
        }
        if (v.d != null && v.d != p && !v.d.y && TWServerHook.proxies.contains(v.d)) {
            return false; // baskasi binmis
        }
        if (p.e != v) {
            p.h(v);
        }
        return true;
    }

    public void setRiderInput(String player, float mx, float mz) {
        TWProxy p = proxyByName.get(player);
        if (p != null && p.e != null) {
            p.inM = Math.max(-0.4F, Math.min(0.4F, mx));
            p.inO = Math.max(-0.4F, Math.min(0.4F, mz));
        }
    }

    public boolean saddle(int netId) {
        eq e = byNetId.get(netId);
        if (e instanceof ib && !e.y) {
            ((ib) e).a = true;
            return true;
        }
        return false;
    }

    public int foodHeal(int itemId) {
        if (itemId <= 0 || itemId >= ge.c.length || ge.c[itemId] == null) {
            return 0;
        }
        return ge.c[itemId] instanceof ke ? ((ke) ge.c[itemId]).a : 0;
    }

    public String blockSoundName(int id, boolean breaking) {
        if (id <= 0 || id >= gs.n.length || gs.n[id] == null || gs.n[id].aS == null) {
            return null;
        }
        return breaking ? gs.n[id].aS.a() : gs.n[id].aS.d();
    }

    public float blockSoundVolume(int id) {
        return (gs.n[id].aS.b() + 1.0F) / 2.0F;
    }

    public float blockSoundPitch(int id) {
        return gs.n[id].aS.c() * 0.8F;
    }

    public void fireArrow(String player) {
        TWProxy p = proxyByName.get(player);
        if (p == null || p.y) {
            return;
        }
        w.a((eq) new eo(w, (ka) p));
    }

    /** Moves items collected by proxies (e.g. arrows on the ground) to the real players. */
    /** Removes entities whose position or speed is NaN or infinite (a division by zero result when the centre entity is hit in an explosion). */
    private void sweepBadEntities() {
        for (int i = 0; i < w.r.size(); i++) {
            Object o = w.r.get(i);
            if (!(o instanceof eq) || o instanceof TWProxy) {
                continue;
            }
            eq e = (eq) o;
            if (e.y) {
                continue;
            }
            if (badNum(e.j) || badNum(e.k) || badNum(e.l) || badNum(e.m) || badNum(e.n) || badNum(e.o)) {
                e.l();
            }
        }
    }

    private static boolean badNum(double v) {
        return Double.isNaN(v) || Double.isInfinite(v) || Math.abs(v) > 3.0E7;
    }

    private void relayPickups() {
        EntityListener l = TWServerHook.entityListener;
        for (TWProxy p : TWServerHook.proxies) {
            for (int i = 0; i < p.b.a.length; i++) {
                hw s = p.b.a[i];
                if (s != null) {
                    p.b.a[i] = null;
                    if (l != null && s.a > 0) {
                        l.onGiveItem(p.name, s.c, s.a, s.d);
                    }
                }
            }
        }
    }

    public boolean placePainting(int x, int y, int z, int dir, int motive) {
        hr[] all = hr.values();
        if (motive < 0 || motive >= all.length || dir < 0 || dir > 3 || y < 1 || y > 126
                || !w.B.a(x >> 4, z >> 4) || w.b(cy.class) >= 500) {
            return false;
        }
        cy pt = new cy(w, x, y, z, dir);
        pt.b = all[motive];
        pt.a(dir);
        if (!pt.b()) {
            return false;
        }
        w.a((eq) pt);
        return true;
    }

    public void igniteTnt(int x, int y, int z) {
        if (w.a(x, y, z) == 46 && gs.n[46] instanceof ah) {
            ((ah) gs.n[46]).a(w, x, y, z, w.e(x, y, z));
        }
    }

    public boolean spawnMob(int type, double x, double y, double z) {
        if (type < 0 || type == 6 || type >= 8 || !w.B.a(hq.b(x) >> 4, hq.b(z) >> 4)) {
            return false;
        }
        Class<?>[] cls = {gw.class, fo.class, fy.class, ci.class, dn.class, ib.class, null, ac.class};
        try {
            ka mob = (ka) cls[type].getConstructor(fe.class).newInstance(w);
            mob.b(x, y + mob.z, z, w.i.nextFloat() * 360.0F, 0.0F);
            w.a((eq) mob);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int killAllMobs() {
        int n = 0;
        for (int i = 0; i < w.r.size(); i++) {
            Object o = w.r.get(i);
            if (o instanceof ka && !(o instanceof gi) && !((eq) o).y) {
                ((eq) o).l();
                n++;
            }
        }
        return n;
    }

    public int getMaxStackSize(int id) {
        if (id < 256) {
            return 64; // blocks
        }
        if (id >= ge.c.length || ge.c[id] == null) {
            return 64;
        }
        return Math.max(1, ge.c[id].aA);
    }

    public int getMobCount() {
        int n = 0;
        for (int i = 0; i < w.r.size(); i++) {
            Object o = w.r.get(i);
            if (o instanceof ka && !(o instanceof gi) && !((eq) o).y) {
                n++;
            }
        }
        return n;
    }

    public int trimMobs(int max) {
        List<eq> mobs = new ArrayList<eq>();
        for (int i = 0; i < w.r.size(); i++) {
            Object o = w.r.get(i);
            if (o instanceof ka && !(o instanceof gi) && !((eq) o).y) {
                mobs.add((eq) o);
            }
        }
        int excess = mobs.size() - Math.max(0, max);
        if (excess <= 0) {
            return 0;
        }
        final java.util.IdentityHashMap<eq, Double> dist = new java.util.IdentityHashMap<eq, Double>();
        for (eq m : mobs) {
            double best = Double.MAX_VALUE;
            for (int i = 0; i < TWServerHook.proxies.size(); i++) {
                TWProxy p = TWServerHook.proxies.get(i);
                double dx = p.j - m.j, dy = p.k - m.k, dz = p.l - m.l;
                best = Math.min(best, dx * dx + dy * dy + dz * dz);
            }
            dist.put(m, best);
        }
        java.util.Collections.sort(mobs, new java.util.Comparator<eq>() {
            public int compare(eq a, eq b) {
                return Double.compare(dist.get(b), dist.get(a)); // farthest first
            }
        });
        for (int i = 0; i < excess; i++) {
            mobs.get(i).l();
        }
        return excess;
    }

    public int getTntCount() {
        int n = 0;
        for (int i = 0; i < w.r.size(); i++) {
            Object o = w.r.get(i);
            if (o instanceof db && !((eq) o).y) {
                n++;
            }
        }
        return n;
    }

    public int clearTnt() {
        int n = 0;
        for (int i = 0; i < w.r.size(); i++) {
            Object o = w.r.get(i);
            if (o instanceof db && !((eq) o).y) {
                ((eq) o).l();
                n++;
            }
        }
        return n;
    }

    public List<NetEntity> getNetEntities() {
        netList.clear();
        IdentityHashMap<eq, Boolean> seen = new IdentityHashMap<eq, Boolean>();
        for (int i = 0; i < w.r.size(); i++) {
            Object o = w.r.get(i);
            Integer type = typeOf.get(o.getClass());
            if (type == null) {
                continue;
            }
            eq e = (eq) o;
            NetEntity ne = infos.get(e);
            if (ne == null) {
                if (e.y) {
                    continue;
                }
                ne = new NetEntity();
                ne.id = nextNetId++;
                if (nextNetId < Protocol.MOB_ID_BASE) {
                    nextNetId = Protocol.MOB_ID_BASE;
                }
                ne.type = type;
                infos.put(e, ne);
                byNetId.put(ne.id, e);
                TWServerHook.netIds.put(e, ne.id);
            }
            seen.put(e, Boolean.TRUE);
            ne.x = e.j;
            ne.y = e.k - e.z; // feet height
            ne.z = e.l;
            if (e instanceof cy) {
                cy pt = (cy) e;
                ne.x = pt.Y;
                ne.y = pt.Z;
                ne.z = pt.aa;
            }
            ne.yaw = e.p;
            ne.pitch = e.q;
            int st = e.R > 0 ? Protocol.STATE_BURNING : 0;
            if (e instanceof dn && ((dn) e).a) {
                st |= Protocol.STATE_SHEARED;
            }
            if (e instanceof fy) {
                st |= Math.max(0, Math.min(63, ((fy) e).a)) << 2;
            }
            if (e instanceof ij) {
                st = ((ij) e).a & 255;
            }
            if (e instanceof ib && ((ib) e).a) {
                st |= Protocol.STATE_SHEARED; // domuzda: eyerli
            }
            if (e instanceof jp) {
                st = 0;
            }
            if (e instanceof cy) {
                cy pt = (cy) e;
                st = (pt.b.ordinal() & 31) | ((pt.a & 3) << 5);
            }
            ne.state = st;
            ne.hurt = false;
            if (e instanceof ka) {
                ka k = (ka) e;
                ne.health = k.au;
                Integer last = lastHurtTime.get(e);
                if (k.aw > (last == null ? 0 : last.intValue())) {
                    ne.hurt = true;
                }
                lastHurtTime.put(e, k.aw);
                ne.dead = k.au <= 0;
            } else {
                ne.dead = false;
            }
            ne.removed = e.y;
            netList.add(ne);
        }
        for (Iterator<Map.Entry<eq, NetEntity>> it = infos.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<eq, NetEntity> en = it.next();
            eq key = en.getKey();
            NetEntity ne = en.getValue();
            boolean gone = !seen.containsKey(key);
            if (gone || ne.removed) {
                if (gone) {
                    ne.removed = true;
                    netList.add(ne);
                }
                it.remove();
                byNetId.remove(ne.id);
                TWServerHook.netIds.remove(key);
                lastHurtTime.remove(key);
            }
        }
        return netList;
    }

    /**
     * The original natural mob spawn rule (ck.a), but only in loaded chunks and with a distance check to all players.
     * Each tick it is tried around one player in turn.
     */
    private void spawnMobs() {
        List<TWProxy> ps = new ArrayList<TWProxy>(TWServerHook.proxies);
        // As in single-player: tried around each player (depending on the mob count in their area)
        for (TWProxy center : ps) {
            if (center.y) {
                continue;
            }
            if (monsters && difficulty > 0) {
                spawnAround(center, gr.class, monsterCap, MONSTERS, false);
            }
            if (animals) {
                spawnAround(center, bj.class, animalCap, ANIMALS, true);
            }
        }
    }

    /** Number of mobs of the given type within 128 blocks (horizontally) of the centre: the equivalent of the single-player "mobs in the world" count. */
    private int countNear(TWProxy center, Class<?> base) {
        int n = 0;
        for (int i = 0; i < w.r.size(); i++) {
            Object o = w.r.get(i);
            if (base.isInstance(o)) {
                eq e = (eq) o;
                double dx = e.j - center.j, dz = e.l - center.l;
                if (dx * dx + dz * dz <= 128.0 * 128.0) {
                    n++;
                }
            }
        }
        return n;
    }

    private void spawnAround(TWProxy center, Class<?> base, int cap, Class<?>[] types, boolean animal) {
        if (countNear(center, base) >= cap) {
            return;
        }
        java.util.Random r = w.i;
        int cx = hq.b(center.j);
        int cz = hq.b(center.l);
        for (int attempt = 0; attempt < 10; attempt++) {
            Class<?> type = types[r.nextInt(types.length)];
            int x = cx + r.nextInt(256) - 128;
            int y = animal ? r.nextInt(r.nextInt(r.nextInt(112) + 8) + 8) : r.nextInt(128);
            int z = cz + r.nextInt(256) - 128;
            if (!loaded(x, z) || w.g(x, y, z) || w.f(x, y, z) != jy.a) {
                continue;
            }
            for (int group = 0; group < 3; group++) {
                int sx = x, sy = y, sz = z;
                for (int n = 0; n < 3; n++) {
                    sx += r.nextInt(6) - r.nextInt(6);
                    sz += r.nextInt(6) - r.nextInt(6);
                    if (sy < 1 || sy > 126 || !loaded(sx, sz)) {
                        continue;
                    }
                    if (!w.g(sx, sy - 1, sz) || w.g(sx, sy, sz) || w.f(sx, sy, sz).d() || w.g(sx, sy + 1, sz)) {
                        continue;
                    }
                    double fx = sx + 0.5, fy = sy + 1.0, fz = sz + 0.5;
                    if (playerWithin(fx, fy, fz, 24.0)) {
                        continue;
                    }
                    ka mob;
                    try {
                        mob = (ka) type.getConstructor(fe.class).newInstance(w);
                    } catch (Exception ex) {
                        return;
                    }
                    mob.b(fx, fy, fz, r.nextFloat() * 360.0F, 0.0F);
                    if (mob.a(fx, fy, fz)) {
                        w.a((eq) mob);
                    }
                }
            }
        }
    }

    /** Whether the position and its neighbouring chunks are loaded (so mob checks do not generate new chunks). */
    private boolean loaded(int x, int z) {
        int c1 = (x - 8) >> 4, c2 = (x + 8) >> 4, d1 = (z - 8) >> 4, d2 = (z + 8) >> 4;
        return w.B.a(c1, d1) && w.B.a(c1, d2) && w.B.a(c2, d1) && w.B.a(c2, d2);
    }

    private boolean playerWithin(double x, double y, double z, double dist) {
        for (TWProxy p : TWServerHook.proxies) {
            double dx = p.j - x, dy = p.k - y, dz = p.l - z;
            if (dx * dx + dy * dy + dz * dz < dist * dist) {
                return true;
            }
        }
        return false;
    }
}
