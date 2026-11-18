package net.thewellodev.api.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Describes a command. */
public abstract class Command {

    private final String name;
    private String description = "";
    private String usage;
    private String permission;
    private List<String> aliases = new ArrayList<String>();

    protected Command(String name) {
        this.name = name.toLowerCase(java.util.Locale.ENGLISH);
        this.usage = "/" + this.name;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Command setDescription(String description) {
        this.description = description == null ? "" : description;
        return this;
    }

    /** Usage text; "&lt;command&gt;" is replaced with the command name. */
    public String getUsage() {
        return usage;
    }

    public Command setUsage(String usage) {
        this.usage = usage == null ? "/" + name : usage.replace("<command>", name);
        return this;
    }

    public String getPermission() {
        return permission;
    }

    public Command setPermission(String permission) {
        this.permission = permission;
        return this;
    }

    public List<String> getAliases() {
        return Collections.unmodifiableList(aliases);
    }

    public Command setAliases(List<String> aliases) {
        this.aliases = new ArrayList<String>();
        if (aliases != null) {
            for (String a : aliases) {
                this.aliases.add(a.toLowerCase(java.util.Locale.ENGLISH));
            }
        }
        return this;
    }

    /** Whether the sender may use this command. */
    public boolean testPermission(CommandSender sender) {
        if (permission == null || permission.length() == 0 || sender.hasPermission(permission)) {
            return true;
        }
        sender.sendMessage("&cYou do not have permission to use this command.");
        return false;
    }

    /** Runs the command. */
    public abstract boolean execute(CommandSender sender, String label, String[] args);
}
