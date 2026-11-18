package net.thewellodev.api;

import net.thewellodev.api.entity.Player;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Static access point to the TheWelloDev API (like the {@code Bukkit} class in Bukkit).
 *
 * <pre>
 * TheWello.broadcastMessage("&amp;eMerhaba herkese!");
 * Player p = TheWello.getPlayer("Notch");
 * </pre>
 */
public final class TheWello {

    private static Server server;

    private TheWello() {
    }

    public static Server getServer() {
        return server;
    }

    /** Called once by the server. */
    public static void setServer(Server s) {
        if (server != null) {
            throw new IllegalStateException("Server is already set");
        }
        server = s;
    }

    public static Collection<? extends Player> getOnlinePlayers() {
        return server.getOnlinePlayers();
    }

    public static Player getPlayer(String name) {
        return server.getPlayer(name);
    }

    public static int broadcastMessage(String message) {
        return server.broadcastMessage(message);
    }

    public static World getWorld() {
        return server.getWorld();
    }

    public static Logger getLogger() {
        return server.getLogger();
    }

    public static String getVersion() {
        return server.getVersion();
    }
}
