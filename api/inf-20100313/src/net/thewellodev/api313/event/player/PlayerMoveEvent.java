package net.thewellodev.api313.event.player;

import net.thewellodev.api313.Player313;
import net.thewellodev.api.event.Cancellable;
import net.thewellodev.api313.Location313;

/**
 * Called when a player moves (about 20 times a second while moving). If cancelled, the player is put back at
 * {@link #getFrom()}. {@link #setTo(Location313)} moves the player somewhere else.
 */
public class PlayerMoveEvent extends PlayerEvent implements Cancellable {

    private boolean cancelled;
    private final Location313 from;
    private Location313 to;

    public PlayerMoveEvent(Player313 player, Location313 from, Location313 to) {
        super(player);
        this.from = from;
        this.to = to;
    }

    public Location313 getFrom() {
        return from;
    }

    public Location313 getTo() {
        return to;
    }

    public void setTo(Location313 to) {
        this.to = to;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
