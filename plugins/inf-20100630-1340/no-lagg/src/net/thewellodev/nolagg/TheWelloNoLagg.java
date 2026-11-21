package net.thewellodev.nolagg;

import net.thewellodev.api.Location;
import net.thewellodev.api.World;
import net.thewellodev.api.command.Command;
import net.thewellodev.api.command.CommandSender;
import net.thewellodev.api.entity.Player;
import net.thewellodev.api.event.EventHandler;
import net.thewellodev.api.event.EventPriority;
import net.thewellodev.api.event.Listener;
import net.thewellodev.api.event.world.ItemSpawnEvent;
import net.thewellodev.api.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * TheWelloNoLagg: keeps a server responsive when too many entities pile up.
 *
 * Features (all configurable in config.yml):
 * - a per-chunk cap on items lying on the ground; surplus drops are held and released when space frees up
 * - periodic merging of nearby stacks of the same item
 * - a cap on the number of mobs (the farthest ones are removed first)
 * - a cap on burning TNT
 * - a TPS guard that clears entities when the server stays slow for too long
 *
 * Operators can check the state and clean up with /nolagg.
 */
public class TheWelloNoLagg extends JavaPlugin implements Listener {

    private static final int STACK_LIMIT = 64;
    private static final int RELEASE_BATCH = 40;

    // ---------------------------------------------------------------- held surplus drops

    /** A drop that was cancelled because its chunk was full. It is spawned later. */
    private static final class HeldDrop {
        final int typeId;
        final int data;
        int amount;
        final double x, y, z;

        HeldDrop(int typeId, int amount, int data, double x, double y, double z) {
            this.typeId = typeId;
            this.amount = amount;
            this.data = data;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /** Holds surplus drops per chunk. */
    private static final class HeldDrops {
        private final Map<Long, List<HeldDrop>> byChunk = new HashMap<Long, List<HeldDrop>>();
        private int count;

        static long key(int cx, int cz) {
            return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
        }

        int size() {
            return count;
        }

        void clear() {
            byChunk.clear();
            count = 0;
        }

        /** Adds a drop, topping up existing stacks of the same kind first. */
        void hold(int cx, int cz, HeldDrop drop, int capacity) {
            List<HeldDrop> list = byChunk.get(key(cx, cz));
            if (list == null) {
                list = new ArrayList<HeldDrop>();
                byChunk.put(key(cx, cz), list);
            }
            for (HeldDrop h : list) {
                if (drop.amount <= 0) {
                    break;
                }
                if (h.typeId == drop.typeId && h.data == drop.data && h.amount < STACK_LIMIT) {
                    int room = Math.min(STACK_LIMIT - h.amount, drop.amount);
                    h.amount += room;
                    drop.amount -= room;
                }
            }
            if (drop.amount <= 0) {
                return;
            }
            if (count >= capacity) {
                return; // memory guard: nothing more is held
            }
            list.add(drop);
            count++;
        }

        /** Spawns held drops in chunks that have room again. Returns how many drops were released. */
        int release(World world, int itemCap, int budget) {
            int released = 0;
            Iterator<Map.Entry<Long, List<HeldDrop>>> it = byChunk.entrySet().iterator();
            while (it.hasNext() && budget > 0) {
                Map.Entry<Long, List<HeldDrop>> entry = it.next();
                int cx = (int) (entry.getKey() >> 32);
                int cz = (int) (long) entry.getKey();
                if (!world.isChunkLoaded(cx, cz)) {
                    continue;
                }
                List<HeldDrop> list = entry.getValue();
                while (!list.isEmpty() && budget > 0 && world.getItemCount(cx, cz) < Math.max(1, itemCap)) {
                    HeldDrop h = list.remove(0);
                    count--;
                    budget--;
                    released++;
                    spawn(world, h);
                }
                if (list.isEmpty()) {
                    it.remove();
                }
            }
            return released;
        }

        /** Spawns everything, ignoring the cap. Used on shutdown so nothing is lost. */
        void spawnAll(World world) {
            for (List<HeldDrop> list : byChunk.values()) {
                for (HeldDrop h : list) {
                    spawn(world, h);
                }
            }
            clear();
        }

        private void spawn(World world, HeldDrop h) {
            world.dropItem(new Location(world, h.x, h.y, h.z), h.typeId, h.amount, h.data);
        }
    }

    // ---------------------------------------------------------------- settings and state

    private final HeldDrops held = new HeldDrops();
    private boolean spawningHeld;

    private int itemCap;
    private boolean holdSurplus;
    private int holdCapacity;
    private boolean combine;
    private int combineEvery;
    private double combineRadius;
    private int mobCap;
    private int tntCap;
    private boolean tpsGuard;
    private double tpsFloor;
    private int tpsSecondsBeforeAction;
    private int tpsCooldownSeconds;

    private int slowSeconds;
    private long lastGuardAction;
    private long tickCounter;

    private long totalHeld;
    private long totalCombined;
    private long totalMobsRemoved;
    private long totalTntRemoved;
    private int guardActions;

    // ---------------------------------------------------------------- lifecycle

    @Override
    public void onEnable() {
        getConfig().addDefault("max-items-per-chunk", 40);
        getConfig().addDefault("buffer-overflow-items", true);
        getConfig().addDefault("max-buffered-stacks", 300);
        getConfig().addDefault("merge-items", true);
        getConfig().addDefault("merge-interval-seconds", 5);
        getConfig().addDefault("merge-radius", 2.5);
        getConfig().addDefault("max-mobs", 200);
        getConfig().addDefault("max-active-tnt", 200);
        getConfig().addDefault("watchdog.enabled", true);
        getConfig().addDefault("watchdog.min-tps", 14.0);
        getConfig().addDefault("watchdog.low-seconds", 10);
        getConfig().addDefault("watchdog.cooldown-seconds", 120);
        saveConfig();
        readSettings();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, new Runnable() {
            public void run() {
                onSecond();
            }
        }, 20L, 20L);
        getLogger().info("TheWelloNoLagg enabled: " + itemCap + " items per chunk, " + mobCap + " mobs at most.");
    }

    @Override
    public void onDisable() {
        World world = getServer().getWorld();
        if (world != null) {
            spawningHeld = true;
            try {
                held.spawnAll(world);
            } finally {
                spawningHeld = false;
            }
        }
        getLogger().info("TheWelloNoLagg disabled.");
    }

    private void readSettings() {
        itemCap = getConfig().getInt("max-items-per-chunk", 40);
        holdSurplus = getConfig().getBoolean("buffer-overflow-items", true);
        holdCapacity = Math.max(0, getConfig().getInt("max-buffered-stacks", 300));
        combine = getConfig().getBoolean("merge-items", true);
        combineEvery = Math.max(1, getConfig().getInt("merge-interval-seconds", 5));
        combineRadius = Math.max(0.5, getConfig().getDouble("merge-radius", 2.5));
        mobCap = getConfig().getInt("max-mobs", 200);
        tntCap = getConfig().getInt("max-active-tnt", 200);
        tpsGuard = getConfig().getBoolean("watchdog.enabled", true);
        tpsFloor = getConfig().getDouble("watchdog.min-tps", 14.0);
        tpsSecondsBeforeAction = Math.max(1, getConfig().getInt("watchdog.low-seconds", 10));
        tpsCooldownSeconds = Math.max(10, getConfig().getInt("watchdog.cooldown-seconds", 120));
    }

    // ---------------------------------------------------------------- item cap

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (spawningHeld || itemCap < 0) {
            return;
        }
        World world = getServer().getWorld();
        Location at = event.getLocation();
        int cx = (int) Math.floor(at.getX()) >> 4;
        int cz = (int) Math.floor(at.getZ()) >> 4;
        if (itemCap == 0 || world.getItemCount(cx, cz) < itemCap) {
            return;
        }
        event.setCancelled(true);
        totalHeld++;
        if (holdSurplus && holdCapacity > 0) {
            held.hold(cx, cz, new HeldDrop(event.getTypeId(), event.getAmount(), event.getData(),
                    at.getX(), at.getY(), at.getZ()), holdCapacity);
        }
    }

    // ---------------------------------------------------------------- once per second

    private void onSecond() {
        tickCounter++;
        World world = getServer().getWorld();
        if (world == null) {
            return;
        }
        if (held.size() > 0) {
            spawningHeld = true;
            try {
                held.release(world, itemCap, RELEASE_BATCH);
            } finally {
                spawningHeld = false;
            }
        }
        if (combine && tickCounter % combineEvery == 0) {
            totalCombined += world.mergeItems(combineRadius);
        }
        if (mobCap >= 0 && world.getMobCount() > mobCap) {
            totalMobsRemoved += world.trimMobs(mobCap);
        }
        if (tntCap >= 0 && world.getTntCount() > tntCap) {
            int removed = world.clearTnt();
            totalTntRemoved += removed;
            tellOperators("&e[NoLagg] &fToo much TNT was burning (" + removed + " removed).");
        }
        if (tpsGuard) {
            runTpsGuard(world);
        }
    }

    private void runTpsGuard(World world) {
        double tps = getServer().getTps();
        boolean someoneOnline = !getServer().getOnlinePlayers().isEmpty();
        slowSeconds = (tps < tpsFloor && someoneOnline) ? slowSeconds + 1 : 0;

        long now = System.currentTimeMillis();
        boolean cooledDown = now - lastGuardAction > tpsCooldownSeconds * 1000L;
        if (slowSeconds < tpsSecondsBeforeAction || !cooledDown) {
            return;
        }
        lastGuardAction = now;
        slowSeconds = 0;
        guardActions++;

        int items = world.clearItems();
        held.clear();
        int tnt = world.clearTnt();
        int mobs = world.trimMobs(Math.max(20, Math.max(0, mobCap) / 2));
        totalMobsRemoved += mobs;
        totalTntRemoved += tnt;

        String message = String.format(Locale.ENGLISH,
                "&e[NoLagg] &fTPS was low (%.1f): removed %d items, %d mobs and %d TNT.", tps, items, mobs, tnt);
        getLogger().warning(message.replaceAll("&[0-9a-fk-or]", ""));
        tellOperators(message);
    }

    private void tellOperators(String message) {
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.isOp()) {
                player.sendMessage(message);
            }
        }
    }

    // ---------------------------------------------------------------- /nolagg

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equals("nolagg")) {
            return false;
        }
        World world = getServer().getWorld();
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ENGLISH);

        if (sub.equals("status")) {
            Runtime runtime = Runtime.getRuntime();
            long usedMb = (runtime.totalMemory() - runtime.freeMemory()) >> 20;
            long maxMb = runtime.maxMemory() >> 20;
            sender.sendMessage("&6--- TheWelloNoLagg ---");
            sender.sendMessage(String.format(Locale.ENGLISH, "&eTPS: &f%.1f  &eTick: &f%.1f ms  &eMemory: &f%d/%d MB",
                    getServer().getTps(), getServer().getMspt(), usedMb, maxMb));
            sender.sendMessage("&ePlayers: &f" + getServer().getOnlinePlayers().size()
                    + "  &eItems: &f" + world.getItemCount() + "  &eHeld: &f" + held.size()
                    + "  &eMobs: &f" + world.getMobCount() + "  &eTNT: &f" + world.getTntCount());
            sender.sendMessage("&eSince start: &f" + totalHeld + " items held back, " + totalCombined + " stacks merged, "
                    + totalMobsRemoved + " mobs, " + totalTntRemoved + " TNT removed, " + guardActions + " TPS guard actions");
            return true;
        }

        if (sub.equals("clear")) {
            String target = args.length > 1 ? args[1].toLowerCase(Locale.ENGLISH) : "all";
            boolean all = target.equals("all");
            if (!all && !target.equals("items") && !target.equals("mobs") && !target.equals("tnt")) {
                sender.sendMessage("&cUsage: /nolagg clear [items|mobs|tnt|all]");
                return true;
            }
            int items = 0;
            int mobs = 0;
            int tnt = 0;
            if (all || target.equals("items")) {
                items = world.clearItems();
                held.clear();
            }
            if (all || target.equals("mobs")) {
                mobs = world.clearMobs();
            }
            if (all || target.equals("tnt")) {
                tnt = world.clearTnt();
            }
            sender.sendMessage("&aRemoved: &f" + items + " items, " + mobs + " mobs, " + tnt + " TNT.");
            return true;
        }

        return false;
    }
}
