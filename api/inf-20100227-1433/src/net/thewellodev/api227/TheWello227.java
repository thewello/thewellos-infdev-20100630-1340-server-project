package net.thewellodev.api227;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Static access point to the inf-20100227-1433 API (like the {@code Bukkit} class in Bukkit).
 *
 * <pre>
 * TheWello227.broadcastMessage("&amp;eHello everybody!");
 * Player227 p = TheWello227.getPlayer("Notch");
 * </pre>
 */
public final class TheWello227 {

    private static Server227 server;

    private TheWello227() {
    }

    public static Server227 getServer() {
        return server;
    }

    /** Called once by the server. */
    public static void setServer(Server227 s) {
        if (server != null) {
            throw new IllegalStateException("Server is already set");
        }
        server = s;
    }

    public static Collection<? extends Player227> getOnlinePlayers() {
        return server.getOnlinePlayers();
    }

    public static Player227 getPlayer(String name) {
        return server.getPlayer(name);
    }

    public static int broadcastMessage(String message) {
        return server.broadcastMessage(message);
    }

    public static World227 getWorld() {
        return server.getWorld();
    }

    public static Logger getLogger() {
        return server.getLogger();
    }

    public static String getVersion() {
        return server.getVersion();
    }
}
