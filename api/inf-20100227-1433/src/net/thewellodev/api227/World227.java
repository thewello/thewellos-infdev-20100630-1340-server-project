package net.thewellodev.api227;

import java.util.List;

/**
 * The inf-20100227-1433 world on the server. The Y axis runs from 0 to 127.
 * All methods must be called from the main server thread (events, commands, scheduler tasks).
 *
 * This build has no block metadata, no items on the ground, no mobs and no day and night: this API has none of them.
 */
public interface World227 {

    String getName();

    /**
     * The seed of the world. The terrain is made from it (the same seed gives the same terrain); see the server's
     * level-seed setting.
     */
    long getSeed();

    Block227 getBlockAt(int x, int y, int z);

    Block227 getBlockAt(Location227 loc);

    /** The block id at a position (0 = air). Chunks are loaded or generated when needed. */
    int getBlockTypeIdAt(int x, int y, int z);

    /** Changes a block and sends the change to the players. @return true if the block changed */
    boolean setBlockTypeId(int x, int y, int z, int typeId);

    /** One block above the highest solid block. */
    int getHighestBlockYAt(int x, int z);

    Location227 getSpawnLocation();

    void setSpawnLocation(int x, int y, int z);

    List<? extends Player227> getPlayers();

    boolean isChunkLoaded(int chunkX, int chunkZ);

    /** Whether this id is a block of this build (1 to 255 exist in the item list; blocks are 1 to 51). */
    boolean isValidBlock(int typeId);

    void save();
}
