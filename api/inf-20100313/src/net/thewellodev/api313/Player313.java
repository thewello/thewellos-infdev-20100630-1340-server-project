package net.thewellodev.api313;

import net.thewellodev.api.command.CommandSender;

import java.net.InetSocketAddress;

/**
 * A player connected to the server (inf-20100313).
 *
 * This build keeps the inventory on the client and has no item system: the API has no inventory, no items and no
 * ground items. It has health, the game mode (survival or creative with flying), PvP and teleporting.
 */
public interface Player313 extends CommandSender {

    /** Unique entity id on this server. */
    int getEntityId();

    /** Name shown in chat (default: the account name). */
    String getDisplayName();

    void setDisplayName(String name);

    /** The player's location (feet level). */
    Location313 getLocation();

    World313 getWorld();

    /** Teleports the player. */
    void teleport(Location313 location);

    void teleport(double x, double y, double z);

    /** Kicks the player from the server. */
    void kickPlayer(String reason);

    boolean isOnline();

    boolean isOnGround();

    InetSocketAddress getAddress();

    /** Ping (ms), or -1 if unknown. */
    int getPing();

    /** Makes the player say a message or run a command (if it starts with "/"). */
    void chat(String message);

    /**
     * Shows this player a fake block (only this player sees it; the world itself is not changed).
     */
    void sendBlockChange(int x, int y, int z, int typeId);

    /** Health (0-20, 20 = 10 hearts). */
    int getHealth();

    /** Sets health (0 kills the player). */
    void setHealth(int health);

    boolean isDead();

    /** 0 = survival, 1 = creative (flying, no damage). Creative is for operators only. */
    int getGameMode();

    void setGameMode(int mode);
}
