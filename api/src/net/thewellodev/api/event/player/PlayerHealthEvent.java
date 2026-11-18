package net.thewellodev.api.event.player;

import net.thewellodev.api.entity.Player;

/**
 * Called when the health value sent by the client is compared with the server's record.
 * <ul>
 *   <li>{@link #isCheck()} = false: the client gained health ({@link #getReported()} is the new value,
 *       {@link #getAllowedHeal()} is the healing (food or potion) that permits the increase.</li>
 *   <li>{@link #isCheck()} = true: the client seems to ignore damage; {@link #getReported()} is the
 *       lowest health the server expects.</li>
 * </ul>
 * By default the server accepts the client's value. Calling {@link #setEnforced(boolean)} with true makes
 * the server keep its own value: the increase is rejected, or health is lowered to the expected minimum.
 * Anti-cheat plugins use this event; without such a plugin no check is made.
 */
public class PlayerHealthEvent extends PlayerEvent {

    private final int health;
    private final int reported;
    private final int allowedHeal;
    private final boolean check;
    private boolean enforced;

    public PlayerHealthEvent(Player player, int health, int reported, int allowedHeal, boolean check) {
        super(player);
        this.health = health;
        this.reported = reported;
        this.allowedHeal = allowedHeal;
        this.check = check;
    }

    /** The server's current health. */
    public int getHealth() {
        return health;
    }

    /** The health reported by the client (for a check: the lowest expected health). */
    public int getReported() {
        return reported;
    }

    /** Healing (food or potion) that permits this increase; 0 = none. Only meaningful for increases. */
    public int getAllowedHeal() {
        return allowedHeal;
    }

    /** True for a damage check, false for a health increase. */
    public boolean isCheck() {
        return check;
    }

    public boolean isEnforced() {
        return enforced;
    }

    public void setEnforced(boolean enforced) {
        this.enforced = enforced;
    }
}
