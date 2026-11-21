package net.thewellodev.api.event.player;

import net.thewellodev.api.entity.Player;

/** Called when a player leaves (or is kicked). The message can be set to null to hide it. */
public class PlayerQuitEvent extends PlayerEvent {

    private String quitMessage;

    public PlayerQuitEvent(Player player, String quitMessage) {
        super(player);
        this.quitMessage = quitMessage;
    }

    public String getQuitMessage() {
        return quitMessage;
    }

    public void setQuitMessage(String quitMessage) {
        this.quitMessage = quitMessage;
    }
}
