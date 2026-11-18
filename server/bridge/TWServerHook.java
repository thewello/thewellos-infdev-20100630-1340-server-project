import net.thewellodev.server.game.GameBridge;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Server-side hooks. */
public class TWServerHook {

    public static volatile GameBridge.BlockListener listener;
    public static volatile GameBridge.EntityListener entityListener;
    public static fe world;
    /** Server tick counter (kept separate because world time can be changed with /time) */
    public static int ticks;
    /** Player proxies in the world */
    public static final List<TWProxy> proxies = new ArrayList<TWProxy>();
    /** A distant dummy target used when no player is online (mobs despawn) */
    public static TWProxy nobody;
    /** Entity -> network id */
    public static final Map<eq, Integer> netIds = new IdentityHashMap<eq, Integer>();

    public static void onBlockSet(boolean ok, fe w, int x, int y, int z, int a, int b) {
        if (ok && w == world && listener != null) {
            listener.onBlockChanged(x, y, z);
        }
    }

    public static void onBlockSet(boolean ok, fe w, int x, int y, int z, int a) {
        if (ok && w == world && listener != null) {
            listener.onBlockChanged(x, y, z);
        }
    }

    /** ka.a(eq,int,dx,dz) (knockback): avoid a NaN position from division by zero (mob or player at the same point). */
    public static boolean skipZeroKnockback(ka self, eq attacker, int dmg, double dx, double dz) {
        return dx * dx + dz * dz < 1.0E-8 || Double.isNaN(dx) || Double.isNaN(dz);
    }

    public static int netIdOf(eq e) {
        if (e == null) {
            return -1;
        }
        Integer id = netIds.get(e);
        return id == null ? -1 : id.intValue();
    }

    /**
     * Replaces e.b_() inside fe.c(eq): the game is single-player, so mobs look at the "player in the world"
     * field (fe.f). Before each entity update, that field is set to the nearest player.
     */
    public static void tickEntity(eq e) {
        if (e.f == world && world != null && !(e instanceof TWProxy)) {
            world.f = nearest(e.j, e.k, e.l, e instanceof gr);
        }
        e.b_();
    }

    /** @param forMonster monsters prefer players who can be attacked (survival, alive) */
    public static TWProxy nearest(double x, double y, double z, boolean forMonster) {
        TWProxy best = null;
        double bd = Double.MAX_VALUE;
        for (int pass = forMonster ? 0 : 1; pass < 2 && best == null; pass++) {
            for (int i = 0; i < proxies.size(); i++) {
                TWProxy p = proxies.get(i);
                if (pass == 0 && !p.targetable) {
                    continue;
                }
                double dx = p.j - x, dy = p.k - y, dz = p.l - z;
                double d = dx * dx + dy * dy + dz * dz;
                if (d < bd) {
                    bd = d;
                    best = p;
                }
            }
        }
        return best != null ? best : nobody;
    }

    /**
     * cs.a() (mob spawner block: is a player within 16 blocks?). Put the nearest player in fe.f
     * so the original check runs; cancel (false) if there are no players.
     */
    public static boolean noPlayerNearby(cs spawner) {
        if (world == null || proxies.isEmpty()) {
            return true;
        }
        world.f = nearest(spawner.b + 0.5, spawner.c + 0.5, spawner.d + 0.5, true);
        return false;
    }
}
