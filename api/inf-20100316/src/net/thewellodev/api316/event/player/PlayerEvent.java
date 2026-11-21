package net.thewellodev.api316.event.player;

import net.thewellodev.api.event.Event;
import net.thewellodev.api316.Player316;

/** Base class of the events about one player. */
public abstract class PlayerEvent extends Event {

    private final Player316 player;

    protected PlayerEvent(Player316 player) {
        this.player = player;
    }

    public Player316 getPlayer() {
        return player;
    }
}
