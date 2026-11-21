package net.thewellodev.api;

/** A position and look direction in the world. Y is the feet level. */
public class Location implements Cloneable {

    private World world;
    private double x, y, z;
    private float yaw, pitch;

    public Location(World world, double x, double y, double z) {
        this(world, x, y, z, 0F, 0F);
    }

    public Location(World world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public World getWorld() { return world; }

    public void setWorld(World w) { this.world = w; }

    public double getX() { return x; }

    public double getY() { return y; }

    public double getZ() { return z; }

    public void setX(double v) { x = v; }

    public void setY(double v) { y = v; }

    public void setZ(double v) { z = v; }

    public float getYaw() { return yaw; }

    public float getPitch() { return pitch; }

    public void setYaw(float v) { yaw = v; }

    public void setPitch(float v) { pitch = v; }

    public int getBlockX() { return floor(x); }

    public int getBlockY() { return floor(y); }

    public int getBlockZ() { return floor(z); }

    public Location add(double dx, double dy, double dz) {
        x += dx;
        y += dy;
        z += dz;
        return this;
    }

    public double distanceSquared(Location o) {
        double dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distance(Location o) {
        return Math.sqrt(distanceSquared(o));
    }

    public static int floor(double d) {
        int i = (int) d;
        return d < i ? i - 1 : i;
    }

    @Override
    public Location clone() {
        return new Location(world, x, y, z, yaw, pitch);
    }

    @Override
    public String toString() {
        return String.format("Location{x=%.2f,y=%.2f,z=%.2f,yaw=%.1f,pitch=%.1f}", x, y, z, yaw, pitch);
    }
}
