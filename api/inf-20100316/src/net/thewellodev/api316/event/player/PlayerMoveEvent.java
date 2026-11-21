package net.thewellodev.api316.event.player;

import net.thewellodev.api316.Player316;
import net.thewellodev.api.event.Cancellable;
import net.thewellodev.api316.Location316;

/**
 * Called when a player moves (about 20 times a second while moving). If cancelled, the player is put back at
 * {@link #getFrom()}. {@link #setTo(Location316)} moves the player somewhere else.
 */
public class PlayerMoveEvent extends PlayerEvent implements Cancellable {

    private boolean cancelled;
    private final Location316 from;
    private Location316 to;

    public PlayerMoveEvent(Player316 player, Location316 from, Location316 to) {
        super(player);
        this.from = from;
        this.to = to;
    }

    public Location316 getFrom() {
        return from;
    }

    public Location316 getTo() {
        return to;
    }

    public void setTo(Location316 to) {
        this.to = to;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
