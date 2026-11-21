package net.thewellodev.api227.plugin;

import net.thewellodev.api.plugin.java.JavaPlugin;
import net.thewellodev.api227.Server227;
import net.thewellodev.api227.TheWello227;

/**
 * Base class of plugins for inf-20100227-1433. The plugin.yml must say
 * {@code api: inf-20100227-1433}; the server refuses it otherwise (and refuses plugins of other builds).
 *
 * <pre>
 * public class MyPlugin extends Plugin227 implements Listener {
 *     public void onEnable() {
 *         server().getPluginManager().registerEvents(this, this);
 *     }
 * }
 * </pre>
 *
 * Use the types of {@code net.thewellodev.api227} (Server227, Player227, World227, Block227, Location227,
 * Material227) and the events of {@code net.thewellodev.api227.event}. The classes of {@code net.thewellodev.api}
 * that are in the jar for the other build (Server, World, Player, Block, Material, ItemStack and their events) are
 * not for this build.
 */
public abstract class Plugin227 extends JavaPlugin {

    /** The server, as seen by this build's API. */
    public final Server227 server() {
        return TheWello227.getServer();
    }
}
