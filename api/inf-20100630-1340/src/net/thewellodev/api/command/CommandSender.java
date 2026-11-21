package net.thewellodev.api.command;

import net.thewellodev.api.Server;

/** Anything that can run a command (a player or the console). */
public interface CommandSender {

    /** Sends a message. '&amp;' colour codes are supported. */
    void sendMessage(String message);

    String getName();

    boolean isOp();

    void setOp(boolean op);

    /** Simple permission check: operators have everything; others only "thewello.default" nodes. */
    boolean hasPermission(String permission);

    Server getServer();
}
