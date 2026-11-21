package net.thewellodev.server.api313;

import net.thewellodev.api.Location;
import net.thewellodev.api.Server;
import net.thewellodev.api.World;
import net.thewellodev.api.block.Block;
import net.thewellodev.api.entity.Player;
import net.thewellodev.api313.Block313;
import net.thewellodev.api313.Location313;
import net.thewellodev.api313.Player313;
import net.thewellodev.api313.World313;

import java.util.HashMap;
import java.util.Map;

/**
 * The inf-20100313 API is a view of the server's own objects: this class makes (and keeps) the wrappers
 * and converts locations. The server itself works with the types of net.thewellodev.api.
 */
public final class Api313 {

    private final Server server;
    private final Map<Player, Player313Impl> players = new HashMap<Player, Player313Impl>();
    private World313Impl world;

    public Api313(Server server) {
        this.server = server;
    }

    public synchronized Player313 player(Player p) {
        if (p == null) {
            return null;
        }
        Player313Impl w = players.get(p);
        if (w == null) {
            w = new Player313Impl(this, p);
            players.put(p, w);
        }
        return w;
    }

    /** Forgets the wrapper of a player who left. */
    public synchronized void forget(Player p) {
        players.remove(p);
    }

    public synchronized World313 world(World w) {
        if (w == null) {
            return null;
        }
        if (world == null || world.handle != w) {
            world = new World313Impl(this, w);
        }
        return world;
    }

    public Block313 block(Block b) {
        return b == null ? null : new Block313Impl(this, b);
    }

    public Location313 location(Location l) {
        return l == null ? null : new Location313(world(l.getWorld()), l.getX(), l.getY(), l.getZ(), l.getYaw(),
                l.getPitch());
    }

    public Location toServer(Location313 l) {
        if (l == null) {
            return null;
        }
        World w = l.getWorld() instanceof World313Impl ? ((World313Impl) l.getWorld()).handle : server.getWorld();
        return new Location(w, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    }

    /** The server's player behind a wrapper (or the object itself). */
    public static Player unwrap(Player313 p) {
        return p instanceof Player313Impl ? ((Player313Impl) p).handle : null;
    }
}
