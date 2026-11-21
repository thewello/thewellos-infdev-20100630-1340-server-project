import net.thewellodev.protocol.Protocol;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Synchronizes chests, furnaces and sign contents. Data from the server is applied to the local tile entity;
 * when the contents of a tile entity near the player (8 blocks) change, they are sent to the server.
 * The server validates them and sends them on to other players.
 */
public class TWTiles {

    private static final Map<Long, String> known = new HashMap<Long, String>();
    private static final List<TWConnection.Pkt> pending = new ArrayList<TWConnection.Pkt>();
    private static int ticks;

    static void clear() {
        known.clear();
        pending.clear();
        pendingOpen = null;
        held = -1;
        heldTe = null;
        heldScreen = null;
        bypass = false;
    }

    // ---- chest/furnace lock (only one player at a time)
    private static long pendingKey = -1;
    private static bk pendingOpen;
    private static int pendingKind;
    private static long pendingAt;
    private static long held = -1;
    private static bk heldTe;
    private static dc heldScreen;
    private static boolean bypass;

    static boolean isHeld(bk te) {
        return held >= 0 && key(te.b, te.c, te.d) == held;
    }

    /** @return true = cancel the game's own open action (waiting for the lock reply) */
    static boolean requestOpen(dd p, bk te, int kind) {
        if (bypass || TWClient.conn == null || TWClient.mpWorld == null || te == null || te.a != TWClient.mpWorld) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (pendingOpen != null && now - pendingAt < 2000) {
            return true; // a previous request is still pending
        }
        pendingOpen = te;
        pendingKind = kind;
        pendingKey = key(te.b, te.c, te.d);
        pendingAt = now;
        TWClient.conn.sendXYZ(Protocol.LOCK_REQUEST, te.b, te.c, te.d);
        return true;
    }

    static void onLockResult(int x, int y, int z, boolean granted) {
        if (pendingOpen == null || pendingKey != key(x, y, z)) {
            return;
        }
        bk te = pendingOpen;
        pendingOpen = null;
        aw m = TWClient.mc;
        if (!granted || m == null || m.g == null) {
            if (!granted) {
                TWClient.addChat("&cThis chest/furnace is in use by another player.");
            }
            return;
        }
        // The current contents were already applied (TILE_DATA arrives before the lock reply); take the local tile entity
        bk cur = TWClient.mpWorld == null ? null : TWClient.mpWorld.b(x, y, z);
        if (cur != null) {
            te = cur;
        }
        bypass = true;
        try {
            if (pendingKind == 1 && te instanceof el) {
                m.g.a((el) te);
            } else if (te instanceof kd) {
                m.g.a((kd) te);
            }
        } finally {
            bypass = false;
        }
        held = key(x, y, z);
        heldTe = te;
        heldScreen = m.p;
        if (heldScreen == null) {
            // if the GUI did not open, release the lock at once
            TWClient.conn.sendXYZ(Protocol.LOCK_RELEASE, x, y, z);
            held = -1;
            heldTe = null;
        }
    }

    /** When the GUI closes, send the last contents and release the lock (called every tick). */
    static void lockTick() {
        aw m = TWClient.mc;
        TWConnection c = TWClient.conn;
        if (pendingOpen != null && System.currentTimeMillis() - pendingAt > 3000) {
            pendingOpen = null; // no answer came
        }
        if (held < 0 || m == null || c == null) {
            return;
        }
        if (m.p != heldScreen || heldTe == null) {
            sendTile(heldTe, c);
            c.sendXYZ(Protocol.LOCK_RELEASE, heldTe == null ? 0 : heldTe.b, heldTe == null ? 0 : heldTe.c, heldTe == null ? 0 : heldTe.d);
            held = -1;
            heldTe = null;
            heldScreen = null;
        }
    }

    private static void sendTile(bk te, TWConnection c) {
        if (te == null) {
            return;
        }
        try {
            ae tag = new ae();
            te.b(tag);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            tag.a((DataOutput) new DataOutputStream(bo));
            if (bo.size() < 4000) {
                c.sendTile(te.b, te.c, te.d, bo.toByteArray());
                known.put(key(te.b, te.c, te.d), signature(te));
            }
        } catch (Throwable t) {
            // could not send
        }
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    /** Data from the server: kept until the chunk arrives. */
    static void onData(TWConnection.Pkt p) {
        if (!apply(p)) {
            if (pending.size() < 500) {
                pending.add(p);
            }
        }
    }

    private static boolean apply(TWConnection.Pkt p) {
        fe w = TWClient.mpWorld;
        if (w == null || TWClient.provider == null) {
            return false;
        }
        int x = p.i1, y = p.i2, z = p.i3;
        if (y < 0 || y >= 128 || !TWClient.provider.a(x >> 4, z >> 4)) {
            return false;
        }
        try {
            ae tag = new ae();
            tag.a((DataInput) new DataInputStream(new ByteArrayInputStream(p.data)));
            tag.a("x", x);
            tag.a("y", y);
            tag.a("z", z);
            bk te = w.b(x, y, z);
            String want = tag.g("id");
            if (te == null || !sameType(te, want)) {
                bk fresh = bk.c(tag);
                if (fresh == null) {
                    return true;
                }
                fresh.a = w;
                w.a(x, y, z, fresh);
                te = fresh;
            } else {
                te.a(tag);
            }
            known.put(key(x, y, z), signature(te));
        } catch (Throwable t) {
            // corrupt data: ignore
        }
        return true;
    }

    private static boolean sameType(bk te, String id) {
        ae t = new ae();
        te.b(t);
        return id != null && id.equals(t.g("id"));
    }

    /** Only the fields a player can change: items and sign lines. */
    private static String signature(bk te) {
        StringBuilder sb = new StringBuilder();
        if (te instanceof kd) {
            kd inv = (kd) te;
            for (int i = 0; i < inv.a(); i++) {
                hw s = inv.a(i);
                sb.append(s == null ? "-" : s.c + ":" + s.a + ":" + s.d).append(',');
            }
        } else if (te instanceof jn) {
            for (String l : ((jn) te).e) {
                sb.append(l).append('|');
            }
        }
        return sb.toString();
    }

    private static boolean syncable(bk te) {
        return te instanceof el || te instanceof ii || te instanceof jn;
    }

    static void tick() {
        ticks++;
        fe w = TWClient.mpWorld;
        TWConnection c = TWClient.conn;
        if (w == null || c == null || TWClient.provider == null) {
            return;
        }
        if (!pending.isEmpty() && ticks % 5 == 0) {
            Iterator<TWConnection.Pkt> it = pending.iterator();
            while (it.hasNext()) {
                if (apply(it.next())) {
                    it.remove();
                }
            }
        }
        if (ticks % 8 != 0 || TWClient.mc == null || TWClient.mc.g == null) {
            return;
        }
        dd me = TWClient.mc.g;
        int px = (int) Math.floor(me.j), pz = (int) Math.floor(me.l);
        for (int cx = (px >> 4) - 1; cx <= (px >> 4) + 1; cx++) {
            for (int cz = (pz >> 4) - 1; cz <= (pz >> 4) + 1; cz++) {
                if (!TWClient.provider.a(cx, cz)) {
                    continue;
                }
                jw ch = w.b(cx, cz);
                for (Object o : new ArrayList<Object>(ch.l.values())) {
                    bk te = (bk) o;
                    if (!syncable(te)) {
                        continue;
                    }
                    if ((te instanceof ii || te instanceof el) && !isHeld(te)) {
                        continue; // chests and furnaces are only sent by the player holding the lock
                    }
                    double dx = te.b + 0.5 - me.j, dy = te.c + 0.5 - me.k, dz = te.d + 0.5 - me.l;
                    if (dx * dx + dy * dy + dz * dz > 8 * 8) {
                        continue;
                    }
                    long k = key(te.b, te.c, te.d);
                    String sig = signature(te);
                    String old = known.get(k);
                    if (old == null) {
                        known.put(k, sig);
                    } else if (!old.equals(sig)) {
                        known.put(k, sig);
                        try {
                            ae tag = new ae();
                            te.b(tag);
                            ByteArrayOutputStream bo = new ByteArrayOutputStream();
                            tag.a((DataOutput) new DataOutputStream(bo));
                            if (bo.size() < 4000) {
                                c.sendTile(te.b, te.c, te.d, bo.toByteArray());
                            }
                        } catch (Throwable t) {
                            // could not send
                        }
                    }
                }
            }
        }
    }
}
