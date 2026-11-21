package net.thewellodev.api316.event.block;

import net.thewellodev.api.event.Cancellable;
import net.thewellodev.api316.Block316;
import net.thewellodev.api316.Player316;

/**
 * Called when a player places a block. {@link #getBlock()} is the position (it still holds the old block);
 * {@link #getNewTypeId()} is the new block. If cancelled, the old block stays.
 */
public class BlockPlaceEvent extends BlockEvent implements Cancellable {

    private final Player316 player;
    private final int newTypeId;
    private boolean cancelled;

    public BlockPlaceEvent(Block316 block, Player316 player, int newTypeId) {
        super(block);
        this.player = player;
        this.newTypeId = newTypeId;
    }

    public Player316 getPlayer() {
        return player;
    }

    public int getNewTypeId() {
        return newTypeId;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
