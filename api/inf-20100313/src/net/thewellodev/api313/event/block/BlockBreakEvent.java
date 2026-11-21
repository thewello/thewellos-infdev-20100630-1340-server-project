package net.thewellodev.api313.event.block;

import net.thewellodev.api.event.Cancellable;
import net.thewellodev.api313.Block313;
import net.thewellodev.api313.Player313;

/** Called when a player breaks a block. If cancelled, the block stays (the player sees it come back). */
public class BlockBreakEvent extends BlockEvent implements Cancellable {

    private final Player313 player;
    private boolean cancelled;

    public BlockBreakEvent(Block313 block, Player313 player) {
        super(block);
        this.player = player;
    }

    public Player313 getPlayer() {
        return player;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
