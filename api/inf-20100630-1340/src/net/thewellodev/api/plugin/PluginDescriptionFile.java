package net.thewellodev.api.plugin;

import net.thewellodev.api.config.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * plugin.yml contents. Example:
 * <pre>
 * name: HelloPlugin
 * version: 1.0
 * main: com.example.hello.HelloPlugin
 * author: Your name
 * description: Example plugin
 * depend: [OtherPlugin]
 * commands:
 *   hello:
 *     description: Says hello
 *     usage: /&lt;command&gt; [name]
 *     aliases: [hello, hi]
 *     permission: hello.use
 * </pre>
 */
public class PluginDescriptionFile {

    /** plugin.yml definition of one command. */
    public static class CommandInfo {
        public final String name;
        public final String description;
        public final String usage;
        public final String permission;
        public final List<String> aliases;

        public CommandInfo(String name, String description, String usage, String permission, List<String> aliases) {
            this.name = name;
            this.description = description;
            this.usage = usage;
            this.permission = permission;
            this.aliases = aliases;
        }
    }

    /** The build a plugin without an "api" line is made for. */
    public static final String DEFAULT_API = "inf-20100630-1340";

    private final String name;
    private final String api;
    private final String version;
    private final String main;
    private final String description;
    private final List<String> authors;
    private final String website;
    private final List<String> depend;
    private final List<String> softDepend;
    private final Map<String, CommandInfo> commands = new LinkedHashMap<String, CommandInfo>();

    public PluginDescriptionFile(InputStream in) throws InvalidPluginException {
        Configuration c = new Configuration();
        try {
            c.load(in);
        } catch (IOException e) {
            throw new InvalidPluginException("could not read plugin.yml", e);
        }
        name = c.getString("name");
        main = c.getString("main");
        if (name == null || name.trim().length() == 0) {
            throw new InvalidPluginException("plugin.yml has no 'name'");
        }
        if (!name.matches("[A-Za-z0-9 _.-]+")) {
            throw new InvalidPluginException("Invalid plugin name: " + name);
        }
        if (main == null || main.trim().length() == 0) {
            throw new InvalidPluginException("plugin.yml has no 'main'");
        }
        api = c.getString("api", DEFAULT_API).trim();
        version = c.getString("version", "1.0");
        description = c.getString("description", "");
        website = c.getString("website", "");
        List<String> a = new ArrayList<String>();
        if (c.contains("author")) {
            a.add(c.getString("author"));
        }
        a.addAll(c.getStringList("authors"));
        authors = Collections.unmodifiableList(a);
        depend = Collections.unmodifiableList(c.getStringList("depend"));
        softDepend = Collections.unmodifiableList(c.getStringList("softdepend"));
        for (String cmd : c.getKeys("commands")) {
            String p = "commands." + cmd;
            commands.put(cmd.toLowerCase(java.util.Locale.ENGLISH), new CommandInfo(cmd.toLowerCase(java.util.Locale.ENGLISH),
                    c.getString(p + ".description", ""),
                    c.getString(p + ".usage", "/" + cmd.toLowerCase(java.util.Locale.ENGLISH)),
                    c.getString(p + ".permission", null),
                    c.getStringList(p + ".aliases")));
        }
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    /**
     * The game build whose API the plugin uses (plugin.yml: {@code api: inf-20100227-1433}); inf-20100630-1340 when
     * the line is missing. The server only loads plugins of the build it runs.
     */
    public String getApi() {
        return api;
    }

    public String getMain() {
        return main;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public String getWebsite() {
        return website;
    }

    public List<String> getDepend() {
        return depend;
    }

    public List<String> getSoftDepend() {
        return softDepend;
    }

    public Map<String, CommandInfo> getCommands() {
        return Collections.unmodifiableMap(commands);
    }

    public String getFullName() {
        return name + " v" + version;
    }
}
