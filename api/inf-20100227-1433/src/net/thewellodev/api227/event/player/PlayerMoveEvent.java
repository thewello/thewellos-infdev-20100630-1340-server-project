package net.thewellodev.api227.event.player;

import net.thewellodev.api227.Player227;
import net.thewellodev.api.event.Cancellable;
import net.thewellodev.api227.Location227;

/**
 * Called when a player moves (about 20 times a second while moving). If cancelled, the player is put back at
 * {@link #getFrom()}. {@link #setTo(Location227)} moves the player somewhere else.
 */
public class PlayerMoveEvent extends PlayerEvent implements Cancellable {

    private boolean cancelled;
    private final Location227 from;
    private Location227 to;

    public PlayerMoveEvent(Player227 player, Location227 from, Location227 to) {
        super(player);
        this.from = from;
        this.to = to;
    }

    public Location227 getFrom() {
        return from;
    }

    public Location227 getTo() {
        return to;
    }

    public void setTo(Location227 to) {
        this.to = to;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
