package net.thewellodev.api.event.player;

import net.thewellodev.api.ItemStack;
import net.thewellodev.api.entity.Player;

/**
 * Called when the client sends an inventory summary (about every 5 ticks, when it changed).
 * The server keeps the summary for {@link net.thewellodev.api.entity.Player#getInventoryContents()}.
 */
public class PlayerInventoryEvent extends PlayerEvent {

    private final ItemStack[] contents;
    private final int totalItems;

    public PlayerInventoryEvent(Player player, ItemStack[] contents, int totalItems) {
        super(player);
        this.contents = contents;
        this.totalItems = totalItems;
    }

    /** Items per slot in order; empty slots are null. */
    public ItemStack[] getContents() {
        return contents.clone();
    }

    /** Total number of items in the inventory. */
    public int getTotalItems() {
        return totalItems;
    }
}
