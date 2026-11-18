package net.thewellodev.server.world;

import net.thewellodev.api.Location;
import net.thewellodev.api.World;
import net.thewellodev.api.block.Block;
import net.thewellodev.api.entity.Player;
import net.thewellodev.server.TWServer;
import net.thewellodev.server.game.GameBridge;

import java.util.ArrayList;
import java.util.List;

/** API World implementation (the real Infdev world, through GameBridge). */
public class TWWorld implements World {

    private final TWServer server;
    private final GameBridge bridge;
    private final String name;

    public TWWorld(TWServer server, GameBridge bridge, String name) {
        this.server = server;
        this.bridge = bridge;
        this.name = name;
    }

    public GameBridge getBridge() {
        return bridge;
    }

    private void checkThread() {
        if (!server.isPrimaryThread()) {
            throw new IllegalStateException("The world can only be accessed from the main server thread (use the scheduler)");
        }
    }

    public String getName() {
        return name;
    }

    public long getSeed() {
        return bridge.getSeed();
    }

    public Block getBlockAt(int x, int y, int z) {
        return new TWBlock(this, x, y, z);
    }

    public Block getBlockAt(Location loc) {
        return getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public int getBlockTypeIdAt(int x, int y, int z) {
        checkThread();
        if (y < 0 || y >= 128) {
            return 0;
        }
        return bridge.getBlockId(x, y, z);
    }

    public int getBlockDataAt(int x, int y, int z) {
        checkThread();
        if (y < 0 || y >= 128) {
            return 0;
        }
        return bridge.getBlockMeta(x, y, z);
    }

    public boolean setBlockTypeId(int x, int y, int z, int typeId) {
        return setBlockTypeIdAndData(x, y, z, typeId, 0);
    }

    public boolean setBlockTypeIdAndData(int x, int y, int z, int typeId, int data) {
        checkThread();
        if (y < 0 || y >= 128 || typeId < 0 || typeId > 255) {
            return false;
        }
        return bridge.setBlock(x, y, z, typeId, data & 15, true);
    }

    public int getHighestBlockYAt(int x, int z) {
        checkThread();
        return bridge.getHighestY(x, z);
    }

    public Location getSpawnLocation() {
        int[] s = bridge.getSpawn();
        return new Location(this, s[0] + 0.5, s[1], s[2] + 0.5);
    }

    public void setSpawnLocation(int x, int y, int z) {
        checkThread();
        bridge.setSpawn(x, y, z);
    }

    public long getTime() {
        return bridge.getTime();
    }

    public void setTime(long time) {
        checkThread();
        bridge.setTime(time);
        server.broadcastTime();
    }

    public List<? extends Player> getPlayers() {
        return new ArrayList<Player>(server.getOnlinePlayers());
    }

    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        checkThread();
        return bridge.isChunkLoaded(chunkX, chunkZ);
    }

    public int dropItem(Location location, int typeId, int amount, int data) {
        checkThread();
        if (!isValidItem(typeId) || amount <= 0) {
            return -1;
        }
        return server.spawnNetItem(location.getX(), location.getY(), location.getZ(), 0, 0.2, 0, typeId,
                Math.min(255, amount), data);
    }

    public int getItemCount() {
        checkThread();
        return server.getItemCount();
    }

    public int getItemCount(int chunkX, int chunkZ) {
        checkThread();
        return server.getItemCount(chunkX, chunkZ);
    }

    public int clearItems() {
        checkThread();
        return server.clearItems();
    }

    public int mergeItems(double radius) {
        checkThread();
        return server.mergeItems(radius);
    }

    public int getMobCount() {
        checkThread();
        return server.getLivingMobCount();
    }

    public int clearMobs() {
        checkThread();
        return server.killAllMobs();
    }

    public int trimMobs(int max) {
        checkThread();
        return server.trimMobs(max);
    }

    public int getTntCount() {
        checkThread();
        return server.getTntCount();
    }

    public int clearTnt() {
        checkThread();
        return server.clearTnt();
    }

    public int getMaxStackSize(int typeId) {
        checkThread();
        return server.getMaxStackSize(typeId);
    }

    public boolean isValidItem(int typeId) {
        return bridge.isValidItem(typeId);
    }

    public void save() {
        checkThread();
        bridge.save(true);
    }
}
