package net.thewellodev.server.api227;

import net.thewellodev.api.block.Block;
import net.thewellodev.api227.Block227;
import net.thewellodev.api227.Location227;
import net.thewellodev.api227.Material227;
import net.thewellodev.api227.World227;

/** A block as the inf-20100227-1433 API shows it. */
final class Block227Impl implements Block227 {

    private final Api227 api;
    private final Block handle;

    Block227Impl(Api227 api, Block handle) {
        this.api = api;
        this.handle = handle;
    }

    public World227 getWorld() {
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

    public Location227 getLocation() {
        return api.location(handle.getLocation());
    }

    public int getTypeId() {
        return handle.getTypeId();
    }

    public Material227 getType() {
        return Material227.getMaterial(handle.getTypeId());
    }

    public boolean setTypeId(int id) {
        return handle.setTypeId(id);
    }

    public boolean setType(Material227 material) {
        return handle.setTypeId(material.getId());
    }

    public Block227 getRelative(int dx, int dy, int dz) {
        return api.block(handle.getRelative(dx, dy, dz));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Block227Impl && ((Block227Impl) o).getX() == getX() && ((Block227Impl) o).getY() == getY()
                && ((Block227Impl) o).getZ() == getZ();
    }

    @Override
    public int hashCode() {
        return (getX() * 31 + getY()) * 31 + getZ();
    }

    @Override
    public String toString() {
        return "Block227{" + getX() + "," + getY() + "," + getZ() + " id " + getTypeId() + "}";
    }
}
