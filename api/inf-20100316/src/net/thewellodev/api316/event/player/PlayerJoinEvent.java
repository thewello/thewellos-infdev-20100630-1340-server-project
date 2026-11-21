package net.thewellodev.api316.event.player;

import net.thewellodev.api316.Player316;

/** Called when a player has joined the world. The join message is shown to everybody (null: no message). */
public class PlayerJoinEvent extends PlayerEvent {

    private String joinMessage;

    public PlayerJoinEvent(Player316 player, String joinMessage) {
        super(player);
        this.joinMessage = joinMessage;
    }

    public String getJoinMessage() {
        return joinMessage;
    }

    public void setJoinMessage(String joinMessage) {
        this.joinMessage = joinMessage;
    }
}
