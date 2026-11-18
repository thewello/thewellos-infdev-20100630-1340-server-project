import net.thewellodev.protocol.Protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Client side of the server's mobs (and skeleton arrows). Mobs are created here as real game objects
 * (model, texture, sound and death animation come from the game itself), but AI and physics do not run:
 * the position comes from the server, and hits are sent to the server.
 */
public class TWMobs {

    static final class NetMob {
        int id, type, state;
        eq e;
        double tx, ty, tz;
        float tyaw, tpitch;
        int steps;
        boolean dying;
    }

    private static final Map<Integer, NetMob> byId = new HashMap<Integer, NetMob>();
    private static final Map<eq, NetMob> byEntity = new IdentityHashMap<eq, NetMob>();
    private static final List<NetMob> pending = new ArrayList<NetMob>();
    /** Server difficulty (0 peaceful .. 3 hard) */
    public static int difficulty = 2;
    private static int ticks;
    static boolean creating;

    private static int savedDifficulty = -1;

    /**
     * In multiplayer the difficulty belongs to the server: any value the player sets in the options menu is pulled back
     * to the server's difficulty every tick (the menu also shows the server's value). It is restored when leaving.
     */
    static void lockOptions(aw m) {
        if (m.y == null || TWClient.mpWorld == null) {
            return;
        }
        if (savedDifficulty < 0) {
            savedDifficulty = m.y.x;
        }
        if (m.y.x != difficulty) {
            m.y.x = difficulty;
        }
    }

    /** The local player respawned: riding state and pending mount states are reset. */
    static void onLocalRespawn() {
        lastRide = -1;
    }

    static void clear() {
        if (savedDifficulty >= 0 && TWClient.mc != null && TWClient.mc.y != null) {
            TWClient.mc.y.x = savedDifficulty;
        }
        savedDifficulty = -1;
        lastRide = -1;
        byId.clear();
        byEntity.clear();
        pending.clear();
    }

    static eq entity(int id) {
        NetMob nm = byId.get(id);
        return nm == null ? null : nm.e;
    }

    // ------------------------------------------------------------------ paketler

    static void onSpawn(TWConnection.Pkt p) {
        if (p.i2 < 0 || p.i2 >= Protocol.MOB_CLASSES.length) {
            return;
        }
        onRemove(p.i1, false);
        NetMob nm = new NetMob();
        nm.id = p.i1;
        nm.type = p.i2;
        nm.state = p.i3;
        nm.tx = p.x;
        nm.ty = p.y;
        nm.tz = p.z;
        nm.tyaw = p.yaw;
        nm.tpitch = p.pitch;
        byId.put(nm.id, nm);
        if (!create(nm)) {
            pending.add(nm);
        }
    }

    static void onMove(TWConnection.Pkt p) {
        NetMob nm = byId.get(p.i1);
        if (nm == null || nm.dying) {
            return;
        }
        nm.tx = p.x;
        nm.ty = p.y;
        nm.tz = p.z;
        nm.tyaw = p.yaw;
        nm.tpitch = p.pitch;
        nm.state = p.i3;
        // Arrows are very fast: move straight there; mobs use a smooth transition over 3 ticks
        nm.steps = nm.type == 6 || nm.type >= 8 ? 1 : 4;
        if (nm.type == 11) {
            nm.steps = 2;
        }
        if (nm.e != null && nm.e.y) {
            // Removed from the local world when its chunk was unloaded: recreate it
            byEntity.remove(nm.e);
            nm.e = null;
            pending.add(nm);
        }
    }

    static void onRemove(int id, boolean died) {
        NetMob nm = byId.remove(id);
        if (nm == null) {
            return;
        }
        pending.remove(nm);
        if (nm.e == null) {
            return;
        }
        if (died && nm.e instanceof ka && !nm.e.y) {
            // The game's own death animation (fall over + smoke); the entity removes itself when it ends
            ka k = (ka) nm.e;
            nm.dying = true;
            k.au = 0;
            k.aw = k.ax = 10;
        } else {
            byEntity.remove(nm.e);
            nm.e.l();
        }
    }

    static void onHurt(int id) {
        NetMob nm = byId.get(id);
        if (nm == null || !(nm.e instanceof ka)) {
            return;
        }
        ka k = (ka) nm.e;
        k.aw = k.ax = 10;
    }

    private static void playSound(ka k, String sound) {
        if (sound != null && k.f != null) {
            k.f.a(k, sound, 1.0F, (k.O.nextFloat() - k.O.nextFloat()) * 0.2F + 1.0F);
        }
    }

    /** Every game tick (after the packets). */
    static void tick() {
        ticks++;
        if (!pendingMounts.isEmpty() && ticks % 5 == 0) {
            applyMounts();
        }
        if (!pending.isEmpty() && ticks % 5 == 0) {
            Iterator<NetMob> it = pending.iterator();
            while (it.hasNext()) {
                NetMob nm = it.next();
                if (byId.get(nm.id) != nm || create(nm)) {
                    it.remove();
                }
            }
        }
        if (ticks % 20 == 0) {
            Iterator<Map.Entry<eq, NetMob>> it = byEntity.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<eq, NetMob> en = it.next();
                if (en.getKey().y && en.getValue().dying) {
                    it.remove();
                }
            }
        }
    }

    private static int lastRide = -1;

    /** If the minecart or pig being ridden changes, tell the server; while riding, send the movement input. */
    static void rideTick(aw m, TWConnection c) {
        if (m.g == null) {
            return;
        }
        int now = -1;
        eq v = m.g.e;
        if (v != null && !byEntity.isEmpty()) {
            NetMob nm = byEntity.get(v);
            if (nm != null) {
                now = nm.id;
            }
        }
        if (now != lastRide) {
            lastRide = now;
            c.sendInt(Protocol.RIDE, now);
        }
        if (now >= 0) {
            c.sendRideInput((float) m.g.m, (float) m.g.o);
        }
    }

    private static final Map<Integer, Integer> pendingMounts = new HashMap<Integer, Integer>();

    /** Another player mounted or dismounted a vehicle/pig (the local ghost player mounts the same way). */
    static void onMount(int playerId, int vehicleId) {
        pendingMounts.put(playerId, vehicleId);
        applyMounts();
    }

    private static void applyMounts() {
        Iterator<Map.Entry<Integer, Integer>> it = pendingMounts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> en = it.next();
            TWRemotePlayer rp = TWClient.players.get(en.getKey());
            if (rp == null) {
                it.remove();
                continue;
            }
            if (en.getValue() < 0) {
                if (rp.e != null) {
                    rp.h(rp.e);
                }
                it.remove();
                continue;
            }
            NetMob nm = byId.get(en.getValue());
            if (nm != null && nm.e != null && !nm.e.y) {
                if (rp.e != nm.e) {
                    rp.h(nm.e);
                }
                it.remove();
            }
        }
    }

    /** Start of ea.b(hw,ka) (a saddle used on a mob): tell the server. */
    public static void onSaddle(ea item, hw stack, ka target) {
        NetMob nm = byEntity.isEmpty() ? null : byEntity.get(target);
        if (nm != null && nm.type == 5 && TWClient.conn != null) {
            TWClient.conn.sendInt(Protocol.SADDLE, nm.id);
        }
    }

    public static boolean netDamage(jp self, eq attacker, int amount) {
        return damage(self, attacker, amount);
    }

    private static boolean create(NetMob nm) {
        fe w = TWClient.mpWorld;
        if (w == null || TWClient.provider == null) {
            return false;
        }
        if (!TWClient.provider.a(floor(nm.tx) >> 4, floor(nm.tz) >> 4)) {
            return false;
        }
        eq e;
        try {
            Class<?> c = Class.forName(Protocol.MOB_CLASSES[nm.type], true, TWMobs.class.getClassLoader());
            if (nm.type == 8) {
                e = (eq) c.getConstructor(fe.class, float.class, float.class, float.class)
                        .newInstance(w, (float) nm.tx, (float) nm.ty, (float) nm.tz);
            } else if (nm.type == 10) {
                cy pt = new cy(w, (int) Math.floor(nm.tx + 0.5), (int) Math.floor(nm.ty + 0.5),
                        (int) Math.floor(nm.tz + 0.5), (nm.state >> 5) & 3);
                hr[] all = hr.values();
                pt.b = all[(nm.state & 31) % all.length];
                pt.a((nm.state >> 5) & 3);
                e = pt;
            } else if (nm.type == 9) {
                e = (eq) c.getConstructor(fe.class, float.class, float.class, float.class, int.class)
                        .newInstance(w, (float) nm.tx, (float) nm.ty, (float) nm.tz, nm.state);
            } else {
                e = (eq) c.getConstructor(fe.class).newInstance(w);
            }
        } catch (Throwable t) {
            return true; // unknown type: give up
        }
        if (nm.type != 10) {
            // The picture's position and rotation are computed by cy.a(dir) itself (it sticks to the wall); do not overwrite them
            e.b(nm.tx, nm.ty + e.z, nm.tz, nm.tyaw, nm.tpitch);
        }
        e.g = e.G = e.j;
        e.h = e.H = e.k;
        e.i = e.I = e.l;
        nm.e = e;
        nm.steps = 0;
        byEntity.put(e, nm);
        applyState(nm);
        creating = true;
        try {
            w.a(e);
        } finally {
            creating = false;
        }
        return true;
    }

    private static void applyState(NetMob nm) {
        eq e = nm.e;
        if (nm.type == 11) {
            return;
        }
        if (nm.type >= 8) {
            return;
        }
        boolean burning = (nm.state & Protocol.STATE_BURNING) != 0;
        e.R = burning ? 20 : 0;
        if (e instanceof ib) {
            ((ib) e).a = (nm.state & Protocol.STATE_SHEARED) != 0;
        } else if (e instanceof dn) {
            ((dn) e).a = (nm.state & Protocol.STATE_SHEARED) != 0;
        } else if (e instanceof fy) {
            fy c = (fy) e;
            c.b = c.a;
            c.a = nm.state >> 2;
        }
    }

    private static void interpolate(NetMob nm) {
        eq e = nm.e;
        if (nm.type == 10) {
            return; // resimler sabit
        }
        if (nm.steps <= 0) {
            return;
        }
        double ty = nm.ty + e.z;
        double nx = e.j + (nm.tx - e.j) / nm.steps;
        double ny = e.k + (ty - e.k) / nm.steps;
        double nz = e.l + (nm.tz - e.l) / nm.steps;
        float dyaw = nm.tyaw - e.p;
        while (dyaw < -180F) {
            dyaw += 360F;
        }
        while (dyaw >= 180F) {
            dyaw -= 360F;
        }
        e.p += dyaw / nm.steps;
        e.q += (nm.tpitch - e.q) / nm.steps;
        nm.steps--;
        e.b(nx, ny, nz);
        // Whether the feet are on the ground (approximate; used for the walking animation and network logic)
        e.u = Math.abs(ny - e.h) < 0.01D;
    }

    // ------------------------------------------------------------------ hook'lar

    /** Replaces e.b_() inside fe.c(eq): synchronized arrows do not run their own physics. */
    public static void tickEntity(eq e) {
        NetMob nm = byEntity.isEmpty() ? null : byEntity.get(e);
        if (nm != null && !(e instanceof ka)) {
            try {
                e.g = e.j;
                e.h = e.k;
                e.i = e.l;
                e.r = e.p;
                e.s = e.q;
                interpolate(nm);
            } catch (Throwable t) {
                TWClient.fault("tickEntity", t);
            }
            return;
        }
        e.b_();
    }

    /** Instead of ka.b_() / e() (AI and movement): interpolation for synchronized mobs. */
    public static void livingUpdate(ka e) {
        NetMob nm = byEntity.isEmpty() ? null : byEntity.get(e);
        if (nm == null) {
            e.e();
            return;
        }
        try {
            if (!nm.dying) {
                interpolate(nm);
            }
            applyState(nm);
            // kol/bacak sallanma animasyonu
            e.aG = e.aH;
            double dx = e.j - e.g;
            double dz = e.l - e.i;
            float f = (float) Math.sqrt(dx * dx + dz * dz) * 4.0F;
            if (f > 1.0F) {
                f = 1.0F;
            }
            e.aH += (f - e.aH) * 0.4F;
            e.aI += e.aH;
        } catch (Throwable t) {
            TWClient.fault("livingUpdate", t);
        }
    }

    /** Start of fe.a(eq,String,F,F): local sounds of synchronized mobs are not played (they come from the server). */
    public static boolean skipSound(fe w, eq e, String s, float a, float b) {
        return w == TWClient.mpWorld && !byEntity.isEmpty() && byEntity.containsKey(e);
    }

    /** Start of cy.a(eq,int) (hitting a painting): the hit is sent to the server. */
    public static boolean netDamage(cy self, eq attacker, int amount) {
        return damage(self, attacker, amount);
    }

    /** Start of ka/gr/dn.a(eq,int): no local damage to synchronized mobs; the player's hit is sent to the server. */
    public static boolean netDamage(ka self, eq attacker, int amount) {
        return damage(self, attacker, amount);
    }

    public static boolean netDamage(gr self, eq attacker, int amount) {
        return damage(self, attacker, amount);
    }

    public static boolean netDamage(dn self, eq attacker, int amount) {
        return damage(self, attacker, amount);
    }

    private static boolean damage(eq self, eq attacker, int amount) {
        NetMob nm = byEntity.isEmpty() ? null : byEntity.get(self);
        if (nm == null) {
            return false;
        }
        if (!nm.dying && amount > 0 && attacker != null && TWClient.mc != null && attacker == TWClient.mc.g
                && TWClient.conn != null) {
            TWClient.conn.sendAttack(nm.id, amount);
        }
        return true;
    }

    /** Inside aw.i(), when the world difficulty is set: in multiplayer the server's difficulty applies. */
    public static int filterDifficulty(int optionValue) {
        if (TWClient.mpWorld != null && TWClient.mc != null && TWClient.mc.e == TWClient.mpWorld) {
            return difficulty;
        }
        return optionValue;
    }

    private static int floor(double d) {
        int i = (int) d;
        return d < i ? i - 1 : i;
    }
}
