package net.thewellodev.api.event.player;

import net.thewellodev.api.entity.Player;
import net.thewellodev.api.event.Cancellable;

/** Called when a player sends a line starting with "/", before the command runs. */
public class PlayerCommandPreprocessEvent extends PlayerEvent implements Cancellable {

    private boolean cancelled;
    private String message;

    public PlayerCommandPreprocessEvent(Player player, String message) {
        super(player);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
