package com.example.hello;

import net.thewellodev.api.Material;
import net.thewellodev.api.command.Command;
import net.thewellodev.api.command.CommandSender;
import net.thewellodev.api.entity.Player;
import net.thewellodev.api.event.EventHandler;
import net.thewellodev.api.event.Listener;
import net.thewellodev.api.event.block.BlockBreakEvent;
import net.thewellodev.api.event.block.BlockPlaceEvent;
import net.thewellodev.api.event.player.PlayerChatEvent;
import net.thewellodev.api.event.player.PlayerJoinEvent;
import net.thewellodev.api.plugin.java.JavaPlugin;
import net.thewellodev.api.util.ChatColor;

/**
 * Example plugin.
 * - /hello [name]  : greets the player
 * - /top: teleports to the highest block above you (the game has no /heal-style commands here)
 * - Welcome message when a player joins
 * - Blocks TNT placement (can be turned on or off in the config)
 * - Announcement every 5 minutes
 */
public class HelloPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // config.yml defaults
        getConfig().addDefault("welcome-message", "&aWelcome, %player%! &fThere are %online% players online.");
        getConfig().addDefault("block-tnt", true);
        getConfig().addDefault("announcement", "&6[Announcement] &fType /hello to see the rules!");
        getConfig().addDefault("announcement-minutes", 5);
        saveConfig();

        getServer().getPluginManager().registerEvents(this, this);

        long period = 20L * 60 * getConfig().getInt("announcement-minutes", 5);
        getServer().getScheduler().runTaskTimer(this, new Runnable() {
            public void run() {
                if (!getServer().getOnlinePlayers().isEmpty()) {
                    getServer().broadcastMessage(getConfig().getString("announcement"));
                }
            }
        }, period, period);

        getLogger().info("HelloPlugin enabled! Server: " + getServer().getName() + " " + getServer().getVersion());
    }

    @Override
    public void onDisable() {
        getLogger().info("HelloPlugin disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("hello")) {
            String who = args.length > 0 ? args[0] : sender.getName();
            sender.sendMessage(ChatColor.YELLOW + "Hello " + ChatColor.WHITE + who + ChatColor.YELLOW + "!");
            return true;
        }
        if (command.getName().equals("top")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            Player p = (Player) sender;
            int x = p.getLocation().getBlockX();
            int z = p.getLocation().getBlockZ();
            int y = p.getWorld().getHighestBlockYAt(x, z);
            p.teleport(x + 0.5, y + 0.1, z + 0.5);
            p.sendMessage(ChatColor.GREEN + "Teleported to the top (y=" + y + ")");
            return true;
        }
        return false;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        String msg = getConfig().getString("welcome-message")
                .replace("%player%", e.getPlayer().getName())
                .replace("%online%", String.valueOf(getServer().getOnlinePlayers().size()));
        e.getPlayer().sendMessage(msg);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (e.getNewTypeId() == Material.TNT.getId() && getConfig().getBoolean("block-tnt", true)
                && !e.getPlayer().isOp()) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "Placing TNT is not allowed on this server!");
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (e.getBlock().getTypeId() == Material.DIAMOND_ORE.getId()) {
            getServer().broadcastMessage(ChatColor.AQUA + e.getPlayer().getName() + " found a diamond!");
        }
    }

    @EventHandler
    public void onChat(PlayerChatEvent e) {
        if (e.getPlayer().isOp()) {
            e.setFormat("&c[OP] &f<%1$s> %2$s");
        }
    }
}
