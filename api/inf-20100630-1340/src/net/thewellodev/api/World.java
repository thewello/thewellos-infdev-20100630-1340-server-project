package net.thewellodev.api;

import net.thewellodev.api.block.Block;
import net.thewellodev.api.entity.Player;

import java.util.List;

/**
 * The Infdev world on the server. The Y axis runs from 0 to 127.
 * All methods must be called from the main server thread (events, commands, scheduler tasks).
 */
public interface World {

    String getName();

    long getSeed();

    Block getBlockAt(int x, int y, int z);

    Block getBlockAt(Location loc);

    int getBlockTypeIdAt(int x, int y, int z);

    int getBlockDataAt(int x, int y, int z);

    /** Changes a block, updates its neighbours and sends the change to players. */
    boolean setBlockTypeId(int x, int y, int z, int typeId);

    boolean setBlockTypeIdAndData(int x, int y, int z, int typeId, int data);

    /** One block above the highest solid block. */
    int getHighestBlockYAt(int x, int z);

    Location getSpawnLocation();

    void setSpawnLocation(int x, int y, int z);

    /** World time in ticks. 24000 ticks = 1 day. */
    long getTime();

    void setTime(long time);

    List<? extends Player> getPlayers();

    boolean isChunkLoaded(int chunkX, int chunkZ);

    /** Drops an item that all players can see. @return the network id of the item, or -1 if invalid */
    int dropItem(Location location, int typeId, int amount, int data);

    /** Number of shared items on the ground (whole world). */
    int getItemCount();

    /** Number of ground items in a chunk. */
    int getItemCount(int chunkX, int chunkZ);

    /** Removes all ground items. @return number removed */
    int clearItems();

    /**
     * Merges nearby ground items of the same type into stacks up to the stack limit, within {@code radius} blocks.
     * @return number of items that were removed by merging
     */
    int mergeItems(double radius);

    /** Number of living mobs (excluding players, arrows, TNT, paintings and so on). */
    int getMobCount();

    /** Removes all mobs. @return number removed */
    int clearMobs();

    /** Reduces the mob count to {@code max}, removing the ones farthest from players first. @return number removed */
    int trimMobs(int max);

    /** Number of burning TNT entities. */
    int getTntCount();

    /** Removes all burning TNT. @return number removed */
    int clearTnt();

    /** Maximum stack size of an item (1 for tools, weapons and armour; usually 64). */
    int getMaxStackSize(int typeId);

    /** Whether this id is a valid block or item in the game. */
    boolean isValidItem(int typeId);

    void save();
}
