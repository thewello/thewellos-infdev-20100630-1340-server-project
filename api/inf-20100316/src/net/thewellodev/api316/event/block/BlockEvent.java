package net.thewellodev.api316.event.block;

import net.thewellodev.api.event.Event;
import net.thewellodev.api316.Block316;

/** Base class of the events about one block. */
public abstract class BlockEvent extends Event {

    private final Block316 block;

    protected BlockEvent(Block316 block) {
        this.block = block;
    }

    public Block316 getBlock() {
        return block;
    }
}
