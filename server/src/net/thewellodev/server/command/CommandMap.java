package net.thewellodev.server.command;

import net.thewellodev.api.command.Command;
import net.thewellodev.api.command.PluginCommand;
import net.thewellodev.api.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Maps command names and aliases to commands. Plugin commands do not replace built-in commands with the same name; they are reachable as "plugin:command". */
public class CommandMap {

    private final Map<String, Command> known = new TreeMap<String, Command>();
    private final Map<Command, Plugin> owners = new java.util.HashMap<Command, Plugin>();

    private final java.util.Set<String> disabled = new java.util.HashSet<String>();

    /** Built-in commands that are not registered (they do not exist in the running game build). Call before registering. */
    public void disableBuiltins(Collection<String> names) {
        disabled.addAll(names);
    }

    public void registerBuiltin(Command cmd) {
        if (disabled.contains(cmd.getName())) {
            return;
        }
        known.put(cmd.getName(), cmd);
        for (String a : cmd.getAliases()) {
            if (!known.containsKey(a)) {
                known.put(a, cmd);
            }
        }
    }

    public void register(PluginCommand cmd, Plugin plugin) {
        owners.put(cmd, plugin);
        String prefix = plugin.getName().toLowerCase(java.util.Locale.ENGLISH).replace(' ', '_') + ":";
        known.put(prefix + cmd.getName(), cmd);
        if (!known.containsKey(cmd.getName())) {
            known.put(cmd.getName(), cmd);
        }
        for (String a : cmd.getAliases()) {
            known.put(prefix + a, cmd);
            if (!known.containsKey(a)) {
                known.put(a, cmd);
            }
        }
    }

    /** The plugin that registered the command; null for built-in commands. */
    public Plugin getOwner(Command cmd) {
        return owners.get(cmd);
    }

    public void unregisterAll(Plugin plugin) {
        Iterator<Map.Entry<String, Command>> it = known.entrySet().iterator();
        while (it.hasNext()) {
            Command c = it.next().getValue();
            if (owners.get(c) == plugin) {
                it.remove();
            }
        }
        Iterator<Map.Entry<Command, Plugin>> it2 = owners.entrySet().iterator();
        while (it2.hasNext()) {
            if (it2.next().getValue() == plugin) {
                it2.remove();
            }
        }
    }

    public Command get(String label) {
        return known.get(label.toLowerCase(java.util.Locale.ENGLISH));
    }

    /** Unique commands, sorted by name. */
    public List<Command> getCommands() {
        Collection<Command> v = new LinkedHashSet<Command>(known.values());
        List<Command> l = new ArrayList<Command>(v);
        java.util.Collections.sort(l, new java.util.Comparator<Command>() {
            public int compare(Command a, Command b) {
                return a.getName().compareTo(b.getName());
            }
        });
        return l;
    }

    public List<String> getLabels() {
        return new ArrayList<String>(known.keySet());
    }
}
