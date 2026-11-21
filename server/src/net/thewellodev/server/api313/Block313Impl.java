package net.thewellodev.server.api313;

import net.thewellodev.api.block.Block;
import net.thewellodev.api313.Block313;
import net.thewellodev.api313.Location313;
import net.thewellodev.api313.Material313;
import net.thewellodev.api313.World313;

/** A block as the inf-20100313 API shows it. */
final class Block313Impl implements Block313 {

    private final Api313 api;
    private final Block handle;

    Block313Impl(Api313 api, Block handle) {
        this.api = api;
        this.handle = handle;
    }

    public World313 getWorld() {
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

    public Location313 getLocation() {
        return api.location(handle.getLocation());
    }

    public int getTypeId() {
        return handle.getTypeId();
    }

    public Material313 getType() {
        return Material313.getMaterial(handle.getTypeId());
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

    public boolean setType(Material313 material) {
        return handle.setTypeId(material.getId());
    }

    public Block313 getRelative(int dx, int dy, int dz) {
        return api.block(handle.getRelative(dx, dy, dz));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Block313Impl && ((Block313Impl) o).getX() == getX() && ((Block313Impl) o).getY() == getY()
                && ((Block313Impl) o).getZ() == getZ();
    }

    @Override
    public int hashCode() {
        return (getX() * 31 + getY()) * 31 + getZ();
    }

    @Override
    public String toString() {
        return "Block313{" + getX() + "," + getY() + "," + getZ() + " id " + getTypeId() + "}";
    }
}
