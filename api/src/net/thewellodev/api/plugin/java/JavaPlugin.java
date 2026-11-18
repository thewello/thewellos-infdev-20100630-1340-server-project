package net.thewellodev.api.plugin.java;

import net.thewellodev.api.Server;
import net.thewellodev.api.command.Command;
import net.thewellodev.api.command.CommandSender;
import net.thewellodev.api.command.PluginCommand;
import net.thewellodev.api.config.Configuration;
import net.thewellodev.api.plugin.Plugin;
import net.thewellodev.api.plugin.PluginDescriptionFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plugin'lerin genisletecegi temel sinif.
 *
 * <pre>
 * public class HelloPlugin extends JavaPlugin {
 *     public void onEnable() {
 *         getLogger().info("Acildi!");
 *     }
 * }
 * </pre>
 */
public abstract class JavaPlugin implements Plugin {

    private Server server;
    private PluginDescriptionFile description;
    private File dataFolder;
    private File file;
    private ClassLoader classLoader;
    private Logger logger;
    private Configuration config;
    private boolean enabled;
    private boolean initialized;

    /**
     * Called by the server when the plugin is loaded. Plugin code should not call it.
     */
    public final void initialize(Server server, PluginDescriptionFile description, File dataFolder, File file,
                                 ClassLoader classLoader) {
        if (initialized) {
            throw new IllegalStateException("Plugin is already started");
        }
        initialized = true;
        this.server = server;
        this.description = description;
        this.dataFolder = dataFolder;
        this.file = file;
        this.classLoader = classLoader;
        this.logger = new PluginLogger(this, server.getLogger());
    }

    /** Called by the server to enable or disable the plugin. */
    public final void setEnabled(boolean enable) {
        if (enabled == enable) {
            return;
        }
        enabled = enable;
        if (enable) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final String getName() {
        return description.getName();
    }

    public final PluginDescriptionFile getDescription() {
        return description;
    }

    public final Server getServer() {
        return server;
    }

    public final File getDataFolder() {
        return dataFolder;
    }

    /** The plugin's jar file. */
    protected final File getFile() {
        return file;
    }

    protected final ClassLoader getClassLoader() {
        return classLoader;
    }

    public final Logger getLogger() {
        return logger;
    }

    /** Returns the command defined in plugin.yml (to set an executor). */
    public PluginCommand getCommand(String name) {
        PluginCommand c = server.getPluginCommand(name);
        if (c != null && c.getPlugin() == this) {
            return c;
        }
        return null;
    }

    public Configuration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    public void reloadConfig() {
        try {
            config = Configuration.load(new File(dataFolder, "config.yml"));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "could not read config.yml", e);
            config = new Configuration(new File(dataFolder, "config.yml"));
        }
    }

    public void saveConfig() {
        try {
            getConfig().save();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "could not save config.yml", e);
        }
    }

    /** Copies config.yml from the jar to the data folder, if the file does not exist yet. */
    public void saveDefaultConfig() {
        File f = new File(dataFolder, "config.yml");
        if (!f.exists()) {
            saveResource("config.yml", false);
            config = null;
        }
    }

    /** Copies a file from the jar to the data folder. */
    public void saveResource(String path, boolean replace) {
        File out = new File(dataFolder, path);
        if (out.exists() && !replace) {
            return;
        }
        InputStream in = classLoader.getResourceAsStream(path);
        if (in == null) {
            logger.warning("File '" + path + "' not found in the jar");
            return;
        }
        try {
            out.getParentFile().mkdirs();
            OutputStream o = new FileOutputStream(out);
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                o.write(buf, 0, n);
            }
            o.close();
            in.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, path + " could not be saved", e);
        }
    }

    public void onLoad() {
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    /** Default command handler; override it for the commands listed in plugin.yml. */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    @Override
    public String toString() {
        return description == null ? getClass().getName() : description.getFullName();
    }

    /** Logger that prefixes messages with [PluginName]. */
    private static final class PluginLogger extends Logger {
        private final String prefix;

        PluginLogger(Plugin plugin, Logger parent) {
            super(parent.getName() + "." + plugin.getName(), null);
            this.prefix = "[" + plugin.getName() + "] ";
            setParent(parent);
            setLevel(Level.ALL);
        }

        @Override
        public void log(java.util.logging.LogRecord record) {
            record.setMessage(prefix + record.getMessage());
            super.log(record);
        }
    }
}
