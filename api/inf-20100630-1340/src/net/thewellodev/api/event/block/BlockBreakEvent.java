package net.thewellodev.api.event.block;

import net.thewellodev.api.block.Block;
import net.thewellodev.api.entity.Player;
import net.thewellodev.api.event.Cancellable;

/**
 * Called when a player breaks a block (the block is still in the world).
 * If cancelled, the block stays and returns on the player's screen.
 */
public class BlockBreakEvent extends BlockEvent implements Cancellable {

    private final Player player;
    private boolean cancelled;

    public BlockBreakEvent(Block block, Player player) {
        super(block);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
