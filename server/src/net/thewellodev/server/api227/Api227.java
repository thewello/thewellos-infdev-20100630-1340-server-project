package net.thewellodev.server.api227;

import net.thewellodev.api.Location;
import net.thewellodev.api.Server;
import net.thewellodev.api.World;
import net.thewellodev.api.block.Block;
import net.thewellodev.api.entity.Player;
import net.thewellodev.api227.Block227;
import net.thewellodev.api227.Location227;
import net.thewellodev.api227.Player227;
import net.thewellodev.api227.World227;

import java.util.HashMap;
import java.util.Map;

/**
 * The inf-20100227-1433 API is a view of the server's own objects: this class makes (and keeps) the wrappers
 * and converts locations. The server itself works with the types of net.thewellodev.api.
 */
public final class Api227 {

    private final Server server;
    private final Map<Player, Player227Impl> players = new HashMap<Player, Player227Impl>();
    private World227Impl world;

    public Api227(Server server) {
        this.server = server;
    }

    public synchronized Player227 player(Player p) {
        if (p == null) {
            return null;
        }
        Player227Impl w = players.get(p);
        if (w == null) {
            w = new Player227Impl(this, p);
            players.put(p, w);
        }
        return w;
    }

    /** Forgets the wrapper of a player who left. */
    public synchronized void forget(Player p) {
        players.remove(p);
    }

    public synchronized World227 world(World w) {
        if (w == null) {
            return null;
        }
        if (world == null || world.handle != w) {
            world = new World227Impl(this, w);
        }
        return world;
    }

    public Block227 block(Block b) {
        return b == null ? null : new Block227Impl(this, b);
    }

    public Location227 location(Location l) {
        return l == null ? null : new Location227(world(l.getWorld()), l.getX(), l.getY(), l.getZ(), l.getYaw(),
                l.getPitch());
    }

    public Location toServer(Location227 l) {
        if (l == null) {
            return null;
        }
        World w = l.getWorld() instanceof World227Impl ? ((World227Impl) l.getWorld()).handle : server.getWorld();
        return new Location(w, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    }

    /** The server's player behind a wrapper (or the object itself). */
    public static Player unwrap(Player227 p) {
        return p instanceof Player227Impl ? ((Player227Impl) p).handle : null;
    }
}
