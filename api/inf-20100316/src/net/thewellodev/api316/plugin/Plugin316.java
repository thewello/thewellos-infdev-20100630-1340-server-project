package net.thewellodev.api316.plugin;

import net.thewellodev.api.plugin.java.JavaPlugin;
import net.thewellodev.api316.Server316;
import net.thewellodev.api316.TheWello316;

/**
 * Base class of plugins for inf-20100316. The plugin.yml must say
 * {@code api: inf-20100316}; the server refuses it otherwise (and refuses plugins of other builds).
 *
 * <pre>
 * public class MyPlugin extends Plugin316 implements Listener {
 *     public void onEnable() {
 *         server().getPluginManager().registerEvents(this, this);
 *     }
 * }
 * </pre>
 *
 * Use the types of {@code net.thewellodev.api316} (Server316, Player316, World316, Block316, Location316,
 * Material316) and the events of {@code net.thewellodev.api316.event}. The classes of {@code net.thewellodev.api}
 * that are in the jar for the other build (Server, World, Player, Block, Material, ItemStack and their events) are
 * not for this build.
 */
public abstract class Plugin316 extends JavaPlugin {

    /** The server, as seen by this build's API. */
    public final Server316 server() {
        return TheWello316.getServer();
    }
}
