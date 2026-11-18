package net.thewellodev.api.event.player;

import net.thewellodev.api.entity.Player;
import net.thewellodev.api.event.Cancellable;

/**
 * Called when a player tries to hit another player or a mob (before damage is applied).
 * If cancelled, the hit is ignored. The distance is measured from the player's server-side position.
 * Arrow hits between players do not fire this event; use {@link PlayerDamageByPlayerEvent} for those.
 */
public class PlayerAttackEvent extends PlayerEvent implements Cancellable {

    private final Player target;
    private final boolean mob;
    private final double distanceSq;
    private final int damage;
    private final int heldItem;
    private boolean cancelled;

    public PlayerAttackEvent(Player attacker, Player target, boolean mob, double distanceSq, int damage, int heldItem) {
        super(attacker);
        this.target = target;
        this.mob = mob;
        this.distanceSq = distanceSq;
        this.damage = damage;
        this.heldItem = heldItem;
    }

    /** The attacked player, or null if the target is a mob. */
    public Player getTarget() {
        return target;
    }

    /** Whether the target is a mob. */
    public boolean isMob() {
        return mob;
    }

    /** Squared distance between attacker and target, in blocks. */
    public double getDistanceSq() {
        return distanceSq;
    }

    /** Damage reported by the client (the server clamps it to 1-10). */
    public int getDamage() {
        return damage;
    }

    /** Id of the held item, or -1 if empty. */
    public int getHeldItem() {
        return heldItem;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
