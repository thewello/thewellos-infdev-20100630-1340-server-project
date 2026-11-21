package net.thewellodev.api316;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Static access point to the inf-20100316 API (like the {@code Bukkit} class in Bukkit).
 *
 * <pre>
 * TheWello316.broadcastMessage("&amp;eHello everybody!");
 * Player316 p = TheWello316.getPlayer("Notch");
 * </pre>
 */
public final class TheWello316 {

    private static Server316 server;

    private TheWello316() {
    }

    public static Server316 getServer() {
        return server;
    }

    /** Called once by the server. */
    public static void setServer(Server316 s) {
        if (server != null) {
            throw new IllegalStateException("Server is already set");
        }
        server = s;
    }

    public static Collection<? extends Player316> getOnlinePlayers() {
        return server.getOnlinePlayers();
    }

    public static Player316 getPlayer(String name) {
        return server.getPlayer(name);
    }

    public static int broadcastMessage(String message) {
        return server.broadcastMessage(message);
    }

    public static World316 getWorld() {
        return server.getWorld();
    }

    public static Logger getLogger() {
        return server.getLogger();
    }

    public static String getVersion() {
        return server.getVersion();
    }
}
