package net.thewellodev.server.world;

import net.thewellodev.api.Location;
import net.thewellodev.api.Material;
import net.thewellodev.api.World;
import net.thewellodev.api.block.Block;

/** API Block implementation; values are read from the world on each call. */
public class TWBlock implements Block {

    private final TWWorld world;
    private final int x, y, z;

    public TWBlock(TWWorld world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public World getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Location getLocation() {
        return new Location(world, x, y, z);
    }

    public int getTypeId() {
        return world.getBlockTypeIdAt(x, y, z);
    }

    public Material getType() {
        return Material.getMaterial(getTypeId());
    }

    public int getData() {
        return world.getBlockDataAt(x, y, z);
    }

    public boolean setTypeId(int id) {
        return world.setBlockTypeId(x, y, z, id);
    }

    public boolean setType(Material material) {
        return setTypeId(material.getId());
    }

    public boolean setTypeIdAndData(int id, int data) {
        return world.setBlockTypeIdAndData(x, y, z, id, data);
    }

    public Block getRelative(int dx, int dy, int dz) {
        return new TWBlock(world, x + dx, y + dy, z + dz);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TWBlock)) {
            return false;
        }
        TWBlock b = (TWBlock) o;
        return b.x == x && b.y == y && b.z == z && b.world == world;
    }

    @Override
    public int hashCode() {
        return (x * 31 + y) * 31 + z;
    }

    @Override
    public String toString() {
        return "Block{" + x + "," + y + "," + z + " id=" + getTypeId() + ":" + getData() + "}";
    }
}
