package net.thewellodev.api.event.world;

import net.thewellodev.api.Location;
import net.thewellodev.api.event.Cancellable;
import net.thewellodev.api.event.Event;

/**
 * Called when a shared item is about to appear on the ground (dropped by a player, from a broken block,
 * or dropped by a mob). If cancelled, the item is never created.
 */
public class ItemSpawnEvent extends Event implements Cancellable {

    private final Location location;
    private final int typeId;
    private int amount;
    private final int data;
    private boolean cancelled;

    public ItemSpawnEvent(Location location, int typeId, int amount, int data) {
        this.location = location;
        this.typeId = typeId;
        this.amount = amount;
        this.data = data;
    }

    public Location getLocation() {
        return location;
    }

    public int getTypeId() {
        return typeId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getData() {
        return data;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
