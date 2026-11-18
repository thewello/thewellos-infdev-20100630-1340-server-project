package net.thewellodev.api.event.player;

import net.thewellodev.api.Location;
import net.thewellodev.api.entity.Player;
import net.thewellodev.api.event.Cancellable;

/**
 * Called when a player moves or turns (position or look direction changed).
 * If cancelled, the player is teleported back to the "from" location.
 */
public class PlayerMoveEvent extends PlayerEvent implements Cancellable {

    private boolean cancelled;
    private final Location from;
    private Location to;

    public PlayerMoveEvent(Player player, Location from, Location to) {
        super(player);
        this.from = from;
        this.to = to;
    }

    public Location getFrom() {
        return from.clone();
    }

    public Location getTo() {
        return to.clone();
    }

    /** Redirects the player to another location (a teleport is performed). */
    public void setTo(Location to) {
        this.to = to.clone();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
