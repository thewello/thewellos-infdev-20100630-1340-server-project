package net.thewellodev.api313.event.block;

import net.thewellodev.api.event.Event;
import net.thewellodev.api313.Block313;

/** Base class of the events about one block. */
public abstract class BlockEvent extends Event {

    private final Block313 block;

    protected BlockEvent(Block313 block) {
        this.block = block;
    }

    public Block313 getBlock() {
        return block;
    }
}
