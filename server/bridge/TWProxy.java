/**
 * An invisible entity that represents a player in the server world. Mobs target it, and arrows and
 * explosions hit it; the damage it takes is forwarded to the player's client. It does nothing by itself.
 */
public class TWProxy extends gi {

    public final String name;
    /** Alive and in survival mode: mobs can attack */
    public boolean targetable;
    private long lastHurt = -100;
    /** The player's movement input while in a minecart. */
    public float inM, inO;

    /** The entity being ridden: the cart reads this player's input (m, o) to speed up. */
    public void x() {
        super.x();
        m = inM;
        o = inO;
    }

    public TWProxy(fe w, String name) {
        super(w);
        this.name = name;
        this.ab = name;
    }

    /** No physics or AI: the position comes from the server. Only pushes nearby pushable entities (minecarts). */
    public void b_() {
        java.util.List near = f.b((eq) this, t.b(0.2D, 0.0D, 0.2D));
        if (near != null) {
            for (int i = 0; i < near.size(); i++) {
                eq other = (eq) near.get(i);
                if (other.s() && other != e) {
                    other.f((eq) this);
                }
            }
        }
    }

    public boolean a(eq attacker, int amount) {
        if (TWServerHook.entityListener == null || amount <= 0) {
            return false;
        }
        if (!targetable) {
            // Creative players take no damage, but arrows hit them and stop (they do not pass through or bounce back)
            return true;
        }
        long now = TWServerHook.ticks;
        if (now - lastHurt < 10) {
            return false; // 0.5 s damage protection, as in the game
        }
        lastHurt = now;
        if (attacker instanceof TWProxy) {
            TWServerHook.entityListener.onPlayerHitByPlayer(name, ((TWProxy) attacker).name, amount);
            return true;
        }
        TWServerHook.entityListener.onPlayerDamaged(name, TWServerHook.netIdOf(attacker), amount);
        return true;
    }

    /** Never saved to the world file. */
    public boolean c(ae tag) {
        return false;
    }
}
