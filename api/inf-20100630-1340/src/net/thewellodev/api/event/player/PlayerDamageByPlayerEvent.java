package net.thewellodev.api.event.player;

import net.thewellodev.api.entity.Player;
import net.thewellodev.api.event.Cancellable;

/**
 * Called when a player hits another player (PvP). {@link #getPlayer()} is the player who takes damage.
 * If cancelled, no damage is dealt. The damage can be changed (1 = half a heart).
 */
public class PlayerDamageByPlayerEvent extends PlayerEvent implements Cancellable {

    private final Player damager;
    private int damage;
    private boolean cancelled;

    public PlayerDamageByPlayerEvent(Player victim, Player damager, int damage) {
        super(victim);
        this.damager = damager;
        this.damage = damage;
    }

    public Player getDamager() {
        return damager;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = Math.max(0, damage);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
