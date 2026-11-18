/** World listener that forwards sound and particle events to players. */
public class TWListener implements bv {

    public void a(int x, int y, int z) {
    }

    public void b(int a, int b, int c, int d, int e, int f) {
    }

    public void a(String name, double x, double y, double z, float vol, float pitch) {
        if (TWServerHook.entityListener != null) {
            TWServerHook.entityListener.onSound(name, x, y, z, vol, pitch);
        }
    }

    public void a(String name, double x, double y, double z, double vx, double vy, double vz) {
        if (TWServerHook.entityListener != null) {
            TWServerHook.entityListener.onParticle(name, x, y, z, vx, vy, vz);
        }
    }

    public void a(eq e) {
    }

    public void b(eq e) {
    }

    public void e() {
    }
}
