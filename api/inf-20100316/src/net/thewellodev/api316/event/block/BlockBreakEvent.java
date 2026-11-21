package net.thewellodev.api316.event.block;

import net.thewellodev.api.event.Cancellable;
import net.thewellodev.api316.Block316;
import net.thewellodev.api316.Player316;

/** Called when a player breaks a block. If cancelled, the block stays (the player sees it come back). */
public class BlockBreakEvent extends BlockEvent implements Cancellable {

    private final Player316 player;
    private boolean cancelled;

    public BlockBreakEvent(Block316 block, Player316 player) {
        super(block);
        this.player = player;
    }

    public Player316 getPlayer() {
        return player;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
