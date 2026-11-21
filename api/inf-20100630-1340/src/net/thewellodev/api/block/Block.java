package net.thewellodev.api.block;

import net.thewellodev.api.Location;
import net.thewellodev.api.Material;
import net.thewellodev.api.World;

/** A block position in the world. Values are read live from the world on each call. */
public interface Block {

    World getWorld();

    int getX();

    int getY();

    int getZ();

    Location getLocation();

    int getTypeId();

    /** The material if it is known, otherwise null. */
    Material getType();

    /** Metadata (0-15). */
    int getData();

    boolean setTypeId(int id);

    boolean setType(Material material);

    boolean setTypeIdAndData(int id, int data);

    /** A neighbouring block relative to this one. */
    Block getRelative(int dx, int dy, int dz);
}
