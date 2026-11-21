package net.thewellodev.api313;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Static access point to the inf-20100313 API (like the {@code Bukkit} class in Bukkit).
 *
 * <pre>
 * TheWello313.broadcastMessage("&amp;eHello everybody!");
 * Player313 p = TheWello313.getPlayer("Notch");
 * </pre>
 */
public final class TheWello313 {

    private static Server313 server;

    private TheWello313() {
    }

    public static Server313 getServer() {
        return server;
    }

    /** Called once by the server. */
    public static void setServer(Server313 s) {
        if (server != null) {
            throw new IllegalStateException("Server is already set");
        }
        server = s;
    }

    public static Collection<? extends Player313> getOnlinePlayers() {
        return server.getOnlinePlayers();
    }

    public static Player313 getPlayer(String name) {
        return server.getPlayer(name);
    }

    public static int broadcastMessage(String message) {
        return server.broadcastMessage(message);
    }

    public static World313 getWorld() {
        return server.getWorld();
    }

    public static Logger getLogger() {
        return server.getLogger();
    }

    public static String getVersion() {
        return server.getVersion();
    }
}
