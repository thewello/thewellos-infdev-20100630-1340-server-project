package net.thewellodev.api.event.block;

import net.thewellodev.api.block.Block;
import net.thewellodev.api.event.Event;

/** Events related to a block. */
public abstract class BlockEvent extends Event {

    protected final Block block;

    protected BlockEvent(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}
