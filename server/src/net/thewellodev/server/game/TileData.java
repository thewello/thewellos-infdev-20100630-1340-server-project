package net.thewellodev.server.game;

/** Serialized form of a tile entity (chest, furnace, sign). */
public final class TileData {
    public final int x, y, z;
    public final byte[] data;

    public TileData(int x, int y, int z, byte[] data) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.data = data;
    }
}
