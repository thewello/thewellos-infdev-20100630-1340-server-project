package net.thewellodev.server.api316;

import net.thewellodev.api.block.Block;
import net.thewellodev.api316.Block316;
import net.thewellodev.api316.Location316;
import net.thewellodev.api316.Material316;
import net.thewellodev.api316.World316;

/** A block as the inf-20100316 API shows it. */
final class Block316Impl implements Block316 {

    private final Api316 api;
    private final Block handle;

    Block316Impl(Api316 api, Block handle) {
        this.api = api;
        this.handle = handle;
    }

    public World316 getWorld() {
        return api.world(handle.getWorld());
    }

    public int getX() {
        return handle.getX();
    }

    public int getY() {
        return handle.getY();
    }

    public int getZ() {
        return handle.getZ();
    }

    public Location316 getLocation() {
        return api.location(handle.getLocation());
    }

    public int getTypeId() {
        return handle.getTypeId();
    }

    public Material316 getType() {
        return Material316.getMaterial(handle.getTypeId());
    }

    public boolean setTypeId(int id) {
        return handle.setTypeId(id);
    }

    public int getData() {
        return handle.getWorld().getBlockDataAt(getX(), getY(), getZ());
    }

    public boolean setTypeIdAndData(int id, int data) {
        return handle.getWorld().setBlockTypeIdAndData(getX(), getY(), getZ(), id, data);
    }

    public boolean setType(Material316 material) {
        return handle.setTypeId(material.getId());
    }

    public Block316 getRelative(int dx, int dy, int dz) {
        return api.block(handle.getRelative(dx, dy, dz));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Block316Impl && ((Block316Impl) o).getX() == getX() && ((Block316Impl) o).getY() == getY()
                && ((Block316Impl) o).getZ() == getZ();
    }

    @Override
    public int hashCode() {
        return (getX() * 31 + getY()) * 31 + getZ();
    }

    @Override
    public String toString() {
        return "Block316{" + getX() + "," + getY() + "," + getZ() + " id " + getTypeId() + "}";
    }
}
