package net.thewellodev.api.event.player;

import net.thewellodev.api.entity.Player;

/** Called when a player dies. The death message can be changed, or set to null to hide it. */
public class PlayerDeathEvent extends PlayerEvent {

    private final Player killer;
    private String deathMessage;

    public PlayerDeathEvent(Player player, Player killer, String deathMessage) {
        super(player);
        this.killer = killer;
        this.deathMessage = deathMessage;
    }

    /** The player who hit this player in the last 10 seconds, or null. */
    public Player getKiller() {
        return killer;
    }

    public String getDeathMessage() {
        return deathMessage;
    }

    public void setDeathMessage(String deathMessage) {
        this.deathMessage = deathMessage;
    }
}
