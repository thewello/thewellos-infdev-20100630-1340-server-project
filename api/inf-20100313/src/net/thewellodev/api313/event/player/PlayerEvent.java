package net.thewellodev.api313.event.player;

import net.thewellodev.api.event.Event;
import net.thewellodev.api313.Player313;

/** Base class of the events about one player. */
public abstract class PlayerEvent extends Event {

    private final Player313 player;

    protected PlayerEvent(Player313 player) {
        this.player = player;
    }

    public Player313 getPlayer() {
        return player;
    }
}
