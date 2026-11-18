package net.thewellodev.api.event.server;

import net.thewellodev.api.event.Event;
import net.thewellodev.api.plugin.Plugin;

/** Called after a plugin has been enabled. */
public class PluginEnableEvent extends Event {

    private final Plugin plugin;

    public PluginEnableEvent(Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
