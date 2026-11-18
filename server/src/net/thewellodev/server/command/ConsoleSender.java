package net.thewellodev.server.command;

import net.thewellodev.api.Server;
import net.thewellodev.api.command.ConsoleCommandSender;
import net.thewellodev.api.util.ChatColor;
import net.thewellodev.server.TWServer;

/** The server console / panel. */
public class ConsoleSender implements ConsoleCommandSender {

    private final TWServer server;

    public ConsoleSender(TWServer server) {
        this.server = server;
    }

    public void sendMessage(String message) {
        if (message == null) {
            return;
        }
        for (String line : message.split("\n")) {
            server.getLogger().info(ChatColor.stripColor(line));
        }
    }

    public String getName() {
        return "CONSOLE";
    }

    public boolean isOp() {
        return true;
    }

    public void setOp(boolean op) {
        throw new UnsupportedOperationException("The console is always an operator");
    }

    public boolean hasPermission(String permission) {
        return true;
    }

    public Server getServer() {
        return server;
    }
}
