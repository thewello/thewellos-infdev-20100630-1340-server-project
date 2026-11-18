package net.thewellodev.api.event.block;

import net.thewellodev.api.block.Block;
import net.thewellodev.api.entity.Player;
import net.thewellodev.api.event.Cancellable;

/**
 * Called when a player places a block or changes one (for example opening a door).
 * {@link #getBlock()} is still the old block; the new one is {@link #getNewTypeId()} / {@link #getNewData()}.
 * If cancelled, the change is reverted.
 */
public class BlockPlaceEvent extends BlockEvent implements Cancellable {

    private final Player player;
    private final int newTypeId;
    private final int newData;
    private boolean cancelled;

    public BlockPlaceEvent(Block block, Player player, int newTypeId, int newData) {
        super(block);
        this.player = player;
        this.newTypeId = newTypeId;
        this.newData = newData;
    }

    public Player getPlayer() {
        return player;
    }

    public int getNewTypeId() {
        return newTypeId;
    }

    public int getNewData() {
        return newData;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
