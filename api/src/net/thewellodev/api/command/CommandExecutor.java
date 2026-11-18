package net.thewellodev.api.command;

/** Command handler. JavaPlugin implements it; it can also be set with getCommand("x").setExecutor(...). */
public interface CommandExecutor {

    /**
     * @param sender  the sender of the command
     * @param command the command defined in plugin.yml
     * @param label   the name or alias that was used
     * @param args    argumanlar
     * @return false shows the usage message to the sender
     */
    boolean onCommand(CommandSender sender, Command command, String label, String[] args);
}
