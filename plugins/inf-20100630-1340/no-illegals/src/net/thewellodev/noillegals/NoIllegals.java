package net.thewellodev.noillegals;

import net.thewellodev.api.ItemStack;
import net.thewellodev.api.Material;
import net.thewellodev.api.entity.Player;
import net.thewellodev.api.event.EventHandler;
import net.thewellodev.api.event.EventPriority;
import net.thewellodev.api.event.Listener;
import net.thewellodev.api.event.block.BlockPlaceEvent;
import net.thewellodev.api.event.player.PlayerPickupItemEvent;
import net.thewellodev.api.plugin.java.JavaPlugin;
import net.thewellodev.api.util.ChatColor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Prevents items and blocks that cannot be obtained in normal survival (bedrock, water, lava, ore blocks, fire,
 * spawners, chainmail armour) and removes them:
 * <ul>
 *   <li>these blocks and items cannot be placed,</li>
 *   <li>cannot be picked up from the ground,</li>
 *   <li>they cannot be picked up from the ground,</li>
 * </ul>
 * Operators are exempt from all checks. The list can be changed in {@code plugins/NoIllegals/config.yml}
 * with {@code illegal-items} (IDs or MATERIAL names).
 */
public class NoIllegals extends JavaPlugin implements Listener {

    /**
     * Items that cannot be obtained in survival: blocks that drop nothing when broken (grass, leaves, glass,
     * ores...), world-only block forms (water, lava, fire, door and sign blocks, crops...) and items without a recipe
     * (chainmail armour). Iron and gold ore are allowed because they drop their item when broken.
     * is allowed.
     */
    private static final Integer[] DEFAULT_ILLEGAL = {
        2, 7, 8, 9, 10, 11, 16, 18, 19, 20, 43, 51, 52, 55, 56, 59, 60, 62, 63, 64,
        302, 303, 304, 305
    };

    private final Set<Integer> illegal = new HashSet<Integer>();
    private int removedTotal;

    @Override
    public void onEnable() {
        getConfig().addDefault("illegal-items", defaultNames());
        getConfig().addDefault("scan-interval-seconds", 1);
        getConfig().addDefault("message", "&cYou cannot have this item in survival; it was removed from your inventory.");
        saveConfig();
        load();
        getServer().getPluginManager().registerEvents(this, this);
        long period = 20L * Math.max(1, getConfig().getInt("scan-interval-seconds", 1));
        getServer().getScheduler().runTaskTimer(this, new Runnable() {
            public void run() {
                for (Player p : getServer().getOnlinePlayers()) {
                    scan(p);
                }
            }
        }, period, period);
        getLogger().info("NoIllegals enabled (" + illegal.size() + " banned items).");
    }

    @Override
    public void onDisable() {
        getLogger().info("NoIllegals disabled (" + removedTotal + " items removed in total).");
    }

    private static java.util.List<String> defaultNames() {
        java.util.List<String> names = new java.util.ArrayList<String>();
        for (Integer id : DEFAULT_ILLEGAL) {
            Material m = Material.getMaterial(id);
            names.add(m == null ? String.valueOf(id) : m.name());
        }
        return names;
    }

    private void load() {
        illegal.clear();
        for (String s : getConfig().getStringList("illegal-items")) {
            Material m = Material.matchMaterial(s);
            if (m != null) {
                illegal.add(m.getId());
                continue;
            }
            try {
                illegal.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException e) {
                getLogger().warning("Unknown item (ignored): " + s);
            }
        }
    }

    private boolean exempt(Player p) {
        return p.isOp();
    }

    private boolean isIllegal(int typeId) {
        return illegal.contains(typeId);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (p != null && !exempt(p) && isIllegal(e.getNewTypeId())) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "You cannot place this block.");
            getLogger().warning(p.getName() + " tried to place a banned block: " + describe(e.getNewTypeId()));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent e) {
        Player p = e.getPlayer();
        if (!exempt(p) && isIllegal(e.getTypeId())) {
            e.setCancelled(true);
        }
    }

    /** Scans the inventory, removes banned items and notifies the player. */
    private void scan(Player p) {
        if (exempt(p) || p.isDead()) {
            return;
        }
        ItemStack[] inv = p.getInventoryContents();
        int removed = 0;
        for (int slot = 0; slot < inv.length; slot++) {
            ItemStack it = inv[slot];
            if (it != null && isIllegal(it.getTypeId())) {
                p.setInventorySlot(slot, null);
                removed++;
                removedTotal++;
                getLogger().warning(p.getName() + " had a banned item in the inventory, removed: " + it);
            }
        }
        if (removed > 0) {
            p.sendMessage(getConfig().getString("message"));
        }
    }

    private static String describe(int id) {
        Material m = Material.getMaterial(id);
        return m == null ? String.valueOf(id) : m.name();
    }
}
