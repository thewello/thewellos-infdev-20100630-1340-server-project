package net.thewellodev.server.api316;

import net.thewellodev.api.Location;
import net.thewellodev.api.Server;
import net.thewellodev.api.World;
import net.thewellodev.api.block.Block;
import net.thewellodev.api.entity.Player;
import net.thewellodev.api316.Block316;
import net.thewellodev.api316.Location316;
import net.thewellodev.api316.Player316;
import net.thewellodev.api316.World316;

import java.util.HashMap;
import java.util.Map;

/**
 * The inf-20100316 API is a view of the server's own objects: this class makes (and keeps) the wrappers
 * and converts locations. The server itself works with the types of net.thewellodev.api.
 */
public final class Api316 {

    private final Server server;
    private final Map<Player, Player316Impl> players = new HashMap<Player, Player316Impl>();
    private World316Impl world;

    public Api316(Server server) {
        this.server = server;
    }

    public synchronized Player316 player(Player p) {
        if (p == null) {
            return null;
        }
        Player316Impl w = players.get(p);
        if (w == null) {
            w = new Player316Impl(this, p);
            players.put(p, w);
        }
        return w;
    }

    /** Forgets the wrapper of a player who left. */
    public synchronized void forget(Player p) {
        players.remove(p);
    }

    public synchronized World316 world(World w) {
        if (w == null) {
            return null;
        }
        if (world == null || world.handle != w) {
            world = new World316Impl(this, w);
        }
        return world;
    }

    public Block316 block(Block b) {
        return b == null ? null : new Block316Impl(this, b);
    }

    public Location316 location(Location l) {
        return l == null ? null : new Location316(world(l.getWorld()), l.getX(), l.getY(), l.getZ(), l.getYaw(),
                l.getPitch());
    }

    public Location toServer(Location316 l) {
        if (l == null) {
            return null;
        }
        World w = l.getWorld() instanceof World316Impl ? ((World316Impl) l.getWorld()).handle : server.getWorld();
        return new Location(w, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    }

    /** The server's player behind a wrapper (or the object itself). */
    public static Player unwrap(Player316 p) {
        return p instanceof Player316Impl ? ((Player316Impl) p).handle : null;
    }
}
