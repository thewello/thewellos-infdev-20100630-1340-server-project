/**
 * The client-side view of another player on the server.
 * No physics or AI runs; the player moves smoothly (interpolated) toward the position sent by the server.
 */
public class TWRemotePlayer extends gi {

    private double tx, ty, tz;
    private float tyaw, tpitch;
    private int steps;
    public final String name;
    public int netId;

    public TWRemotePlayer(fe w, String name) {
        super(w);
        this.name = name;
        this.ab = name;
        // The name sent by the server is used for the skin URL only if it is a valid Minecraft name
        this.W = name != null && name.matches("[A-Za-z0-9_]{1,16}") ? "http://www.minecraft.net/skin/" + name + ".png" : null;
    }

    public void setTarget(double x, double y, double z, float yaw, float pitch) {
        tx = x;
        ty = y;
        tz = z;
        tyaw = yaw;
        tpitch = pitch;
        steps = 3;
    }

    /** onLivingUpdate: fizik yerine interpolasyon */
    public void e() {
        if (steps > 0) {
            double nx = j + (tx - j) / steps;
            double ny = k + (ty - k) / steps;
            double nz = l + (tz - l) / steps;
            float dyaw = tyaw - p;
            while (dyaw < -180F) {
                dyaw += 360F;
            }
            while (dyaw >= 180F) {
                dyaw -= 360F;
            }
            p += dyaw / steps;
            q += (tpitch - q) / steps;
            steps--;
            b(nx, ny, nz);
        }
        if (aw > 0) {
            aw--; // damage animation (red flash)
        }
        // Kol/bacak sallanma animasyonu
        aG = aH;
        double dx = j - g;
        double dz = l - i;
        float f = (float) Math.sqrt(dx * dx + dz * dz) * 4.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }
        aH += (f - aH) * 0.4F;
        aI += aH;
    }

    /**
     * If the local player hits, the damage is sent to the server (the server applies the PvP rules and
     * forwards it to the victim's client). Other damage sources (falling, drowning) are not handled here.
     */
    public boolean a(eq attacker, int amount) {
        if (attacker != null && attacker == TWClient.mc.g && amount > 0) {
            TWClient.sendAttack(this, amount);
        }
        return false;
    }

    /** Never saved to the world file. */
    public boolean c(ae tag) {
        return false;
    }
}
