package net.thewellodev.api.event.server;

import net.thewellodev.api.command.CommandSender;
import net.thewellodev.api.event.Cancellable;
import net.thewellodev.api.event.Event;

/** Called when a command is entered in the console or the server panel. The command has no leading "/". */
public class ServerCommandEvent extends Event implements Cancellable {

    private final CommandSender sender;
    private String command;
    private boolean cancelled;

    public ServerCommandEvent(CommandSender sender, String command) {
        this.sender = sender;
        this.command = command;
    }

    public CommandSender getSender() {
        return sender;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
