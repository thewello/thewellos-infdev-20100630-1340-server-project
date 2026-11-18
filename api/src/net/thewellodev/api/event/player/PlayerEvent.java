package net.thewellodev.api.event.player;

import net.thewellodev.api.entity.Player;
import net.thewellodev.api.event.Event;

/** Events related to a player. */
public abstract class PlayerEvent extends Event {

    protected final Player player;

    protected PlayerEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
