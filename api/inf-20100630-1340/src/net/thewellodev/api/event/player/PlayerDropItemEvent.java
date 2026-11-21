package net.thewellodev.api.event.player;

import net.thewellodev.api.entity.Player;
import net.thewellodev.api.event.Cancellable;

/**
 * Called when an item is dropped by a player's client (throwing with Q, an item from a broken block,
 * or inventory scattered on death). If cancelled, the item is not shown to anyone and disappears.
 */
public class PlayerDropItemEvent extends PlayerEvent implements Cancellable {

    private final int typeId;
    private final int amount;
    private final int data;
    private boolean cancelled;

    public PlayerDropItemEvent(Player player, int typeId, int amount, int data) {
        super(player);
        this.typeId = typeId;
        this.amount = amount;
        this.data = data;
    }

    public int getTypeId() {
        return typeId;
    }

    public int getAmount() {
        return amount;
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
