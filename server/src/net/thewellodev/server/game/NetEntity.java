package net.thewellodev.server.game;

/** Current state of a synchronized entity (mob or arrow) on the server. The bridge updates it every tick. */
public final class NetEntity {
    public int id;
    /** Protocol.MOB_CLASSES indeksi */
    public int type;
    public double x, y, z;
    public float yaw, pitch;
    public int state;
    public int health;
    /** Dead (death animation) */
    public boolean dead;
    /** Took damage this tick */
    public boolean hurt;
    /** Left the world (died, despawned, chunk unloaded) */
    public boolean removed;
}
