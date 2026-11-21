package net.thewellodev.api227.event.player;

import net.thewellodev.api.event.Event;
import net.thewellodev.api227.Player227;

/** Base class of the events about one player. */
public abstract class PlayerEvent extends Event {

    private final Player227 player;

    protected PlayerEvent(Player227 player) {
        this.player = player;
    }

    public Player227 getPlayer() {
        return player;
    }
}
