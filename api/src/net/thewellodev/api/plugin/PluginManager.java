package net.thewellodev.api.plugin;

import net.thewellodev.api.event.Event;
import net.thewellodev.api.event.Listener;

import java.util.List;

/** Plugin'leri ve event'leri yonetir. */
public interface PluginManager {

    /** Registers all @EventHandler methods of a listener. */
    void registerEvents(Listener listener, Plugin plugin);

    /** Removes all listeners of a plugin. */
    void unregisterEvents(Plugin plugin);

    /** Sends an event to all matching handlers (call it from the main server thread). */
    <T extends Event> T callEvent(T event);

    Plugin getPlugin(String name);

    List<Plugin> getPlugins();

    boolean isPluginEnabled(String name);

    void enablePlugin(Plugin plugin);

    void disablePlugin(Plugin plugin);
}
