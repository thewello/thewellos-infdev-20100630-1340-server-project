package net.thewellodev.api313.plugin;

import net.thewellodev.api.plugin.java.JavaPlugin;
import net.thewellodev.api313.Server313;
import net.thewellodev.api313.TheWello313;

/**
 * Base class of plugins for inf-20100313. The plugin.yml must say
 * {@code api: inf-20100313}; the server refuses it otherwise (and refuses plugins of other builds).
 *
 * <pre>
 * public class MyPlugin extends Plugin313 implements Listener {
 *     public void onEnable() {
 *         server().getPluginManager().registerEvents(this, this);
 *     }
 * }
 * </pre>
 *
 * Use the types of {@code net.thewellodev.api313} (Server313, Player313, World313, Block313, Location313,
 * Material313) and the events of {@code net.thewellodev.api313.event}. The classes of {@code net.thewellodev.api}
 * that are in the jar for the other build (Server, World, Player, Block, Material, ItemStack and their events) are
 * not for this build.
 */
public abstract class Plugin313 extends JavaPlugin {

    /** The server, as seen by this build's API. */
    public final Server313 server() {
        return TheWello313.getServer();
    }
}
