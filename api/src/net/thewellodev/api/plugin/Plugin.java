package net.thewellodev.api.plugin;

import net.thewellodev.api.Server;
import net.thewellodev.api.command.CommandExecutor;
import net.thewellodev.api.config.Configuration;

import java.io.File;
import java.util.logging.Logger;

/** A TheWelloDev plugin. Usually extends {@link net.thewellodev.api.plugin.java.JavaPlugin}. */
public interface Plugin extends CommandExecutor {

    String getName();

    PluginDescriptionFile getDescription();

    Server getServer();

    /** plugins/&lt;PluginAdi&gt;/ klasoru. */
    File getDataFolder();

    /** plugins/&lt;PluginAdi&gt;/config.yml */
    Configuration getConfig();

    void saveConfig();

    void reloadConfig();

    Logger getLogger();

    boolean isEnabled();

    void onLoad();

    void onEnable();

    void onDisable();
}
