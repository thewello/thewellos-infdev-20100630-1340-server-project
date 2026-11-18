package net.thewellodev.api.command;

import net.thewellodev.api.plugin.Plugin;

/** A command defined in plugin.yml and owned by a plugin. */
public class PluginCommand extends Command {

    private final Plugin owner;
    private CommandExecutor executor;

    public PluginCommand(String name, Plugin owner) {
        super(name);
        this.owner = owner;
        this.executor = owner;
    }

    public Plugin getPlugin() {
        return owner;
    }

    public CommandExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(CommandExecutor executor) {
        this.executor = executor == null ? owner : executor;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!owner.isEnabled()) {
            sender.sendMessage("&cThis command's plugin (" + owner.getName() + ") is disabled.");
            return true;
        }
        if (!testPermission(sender)) {
            return true;
        }
        boolean ok;
        try {
            ok = executor.onCommand(sender, this, label, args);
        } catch (Throwable t) {
            owner.getLogger().log(java.util.logging.Level.SEVERE,
                    "Error while running '/" + label + "' (" + owner.getName() + ")", t);
            sender.sendMessage("&cAn error occurred while running the command.");
            return true;
        }
        if (!ok) {
            sender.sendMessage("&eUsage: " + getUsage());
        }
        return true;
    }
}
