package net.thewellodev.api227;

/** A block position in the world. Values are read live from the world on each call. */
public interface Block227 {

    World227 getWorld();

    int getX();

    int getY();

    int getZ();

    Location227 getLocation();

    /** The block id (0 = air). */
    int getTypeId();

    /** The material if it is known, otherwise null. */
    Material227 getType();

    boolean setTypeId(int id);

    boolean setType(Material227 material);

    /** A neighbouring block relative to this one. */
    Block227 getRelative(int dx, int dy, int dz);
}
