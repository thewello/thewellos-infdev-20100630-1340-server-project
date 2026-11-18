package net.thewellodev.api.event.player;

import net.thewellodev.api.entity.Player;
import net.thewellodev.api.event.Cancellable;

/** Called when a player is about to pick up an item from the ground. If cancelled, the item stays. */
public class PlayerPickupItemEvent extends PlayerEvent implements Cancellable {

    private final int itemNetId;
    private final int typeId;
    private final int amount;
    private final int data;
    private boolean cancelled;

    public PlayerPickupItemEvent(Player player, int itemNetId, int typeId, int amount, int data) {
        super(player);
        this.itemNetId = itemNetId;
        this.typeId = typeId;
        this.amount = amount;
        this.data = data;
    }

    /** Network id of the ground item. */
    public int getItemNetId() {
        return itemNetId;
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
