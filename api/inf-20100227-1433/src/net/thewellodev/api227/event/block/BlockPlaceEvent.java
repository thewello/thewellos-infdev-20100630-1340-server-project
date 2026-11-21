package net.thewellodev.api227.event.block;

import net.thewellodev.api.event.Cancellable;
import net.thewellodev.api227.Block227;
import net.thewellodev.api227.Player227;

/**
 * Called when a player places a block. {@link #getBlock()} is the position (it still holds the old block);
 * {@link #getNewTypeId()} is the new block. If cancelled, the old block stays.
 */
public class BlockPlaceEvent extends BlockEvent implements Cancellable {

    private final Player227 player;
    private final int newTypeId;
    private boolean cancelled;

    public BlockPlaceEvent(Block227 block, Player227 player, int newTypeId) {
        super(block);
        this.player = player;
        this.newTypeId = newTypeId;
    }

    public Player227 getPlayer() {
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
