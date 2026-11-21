package net.thewellodev.api227.event.block;

import net.thewellodev.api.event.Cancellable;
import net.thewellodev.api227.Block227;
import net.thewellodev.api227.Player227;

/** Called when a player breaks a block. If cancelled, the block stays (the player sees it come back). */
public class BlockBreakEvent extends BlockEvent implements Cancellable {

    private final Player227 player;
    private boolean cancelled;

    public BlockBreakEvent(Block227 block, Player227 player) {
        super(block);
        this.player = player;
    }

    public Player227 getPlayer() {
        return player;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
