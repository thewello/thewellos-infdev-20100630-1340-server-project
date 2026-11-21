package net.thewellodev.api227.event.block;

import net.thewellodev.api.event.Event;
import net.thewellodev.api227.Block227;

/** Base class of the events about one block. */
public abstract class BlockEvent extends Event {

    private final Block227 block;

    protected BlockEvent(Block227 block) {
        this.block = block;
    }

    public Block227 getBlock() {
        return block;
    }
}
