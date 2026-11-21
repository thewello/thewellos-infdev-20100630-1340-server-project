package net.thewellodev.api.event.server;

import net.thewellodev.api.event.Event;
import net.thewellodev.api.plugin.Plugin;

/** Called after a plugin has been disabled. */
public class PluginDisableEvent extends Event {

    private final Plugin plugin;

    public PluginDisableEvent(Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
