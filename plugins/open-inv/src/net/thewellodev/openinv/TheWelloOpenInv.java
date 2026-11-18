package net.thewellodev.openinv;

import net.thewellodev.api.ItemStack;
import net.thewellodev.api.Material;
import net.thewellodev.api.World;
import net.thewellodev.api.command.Command;
import net.thewellodev.api.command.CommandSender;
import net.thewellodev.api.entity.Player;
import net.thewellodev.api.plugin.java.JavaPlugin;

/**
 * TheWelloOpenInv - lets operators view and edit the inventory of online players.
 *
 * <p>The server does not store inventories; each client keeps its own. The server only keeps the last summary
 * the client sent, so the inventory is listed in chat rather than shown as a chest window. Edits are applied
 * to the player's client. Offline players cannot be inspected because their inventory is not stored on the server.</p>
 * only online players can be inspected.</p>
 *
 * <ul>
 *   <li>{@code /openinv <player>}: shows the inventory in rows of 9.</li>
 *   <li>{@code /openinv <player> set <slot> <item> [amount] [data]}: puts an item in a slot.</li>
 *   <li>{@code /openinv <player> clear <slot>}: empties a slot.</li>
 *   <li>{@code /searchinv <item> [minimum amount]}: searches online players for an item.</li>
 * </ul>
 * All edits are logged and the target player is notified.
 */
public class TheWelloOpenInv extends JavaPlugin {

    private static final int ROW = 9;

    @Override
    public void onEnable() {
        getLogger().info("TheWelloOpenInv etkin.");
    }

    @Override
    public void onDisable() {
        getLogger().info("TheWelloOpenInv kapandi.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("openinv")) {
            return openInv(sender, args);
        }
        if (command.getName().equals("searchinv")) {
            return searchInv(sender, args);
        }
        return false;
    }

    private Player find(String name) {
        for (Player p : getServer().getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    private static String itemName(int id, int amount) {
        Material m = Material.getMaterial(id);
        String n = m == null ? "#" + id : m.name().toLowerCase(java.util.Locale.ENGLISH);
        return n + " x" + amount;
    }

    /** Finds an item by name or numeric id; -1 if not found. */
    private int resolveItem(World w, String text) {
        Material m = Material.matchMaterial(text);
        int id = m != null ? m.getId() : -1;
        if (id < 0) {
            try {
                id = Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return w.isValidItem(id) ? id : -1;
    }

    private boolean openInv(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return false;
        }
        Player target = find(args[0]);
        if (target == null) {
            sender.sendMessage("&cPlayer is not online or not found: " + args[0]);
            return true;
        }
        if (args.length == 1) {
            show(sender, target);
            return true;
        }
        String action = args[1].toLowerCase(java.util.Locale.ENGLISH);
        if (action.equals("set")) {
            return set(sender, target, args);
        }
        if (action.equals("clear") && args.length == 3) {
            return clear(sender, target, args[2]);
        }
        return false;
    }

    private void show(CommandSender sender, Player target) {
        ItemStack[] items = target.getInventoryContents();
        sender.sendMessage("&6--- " + target.getName() + "'s inventory ---");
        sender.sendMessage("&7Can: &f" + target.getHealth() + "/20  &7Mod: &f" + (target.getGameMode() == 1 ? "yaratici" : "survival")
                + "  &7Elde: &f" + (target.getItemInHand() < 0 ? "-" : itemName(target.getItemInHand(), 1)));
        if (items.length == 0) {
            sender.sendMessage("&eThe inventory has not been reported to the server yet.");
            return;
        }
        for (int row = 0; row * ROW < items.length; row++) {
            StringBuilder sb = new StringBuilder("&e" + (row * ROW) + "-" + Math.min(items.length - 1, row * ROW + ROW - 1) + ": &f");
            for (int i = row * ROW; i < Math.min(items.length, row * ROW + ROW); i++) {
                if (i > row * ROW) {
                    sb.append(" | ");
                }
                sb.append(items[i] == null ? "-" : itemName(items[i].getTypeId(), items[i].getAmount()));
            }
            sender.sendMessage(sb.toString());
        }
        getLogger().info(sender.getName() + " inspected " + target.getName() + "'s inventory.");
    }

    private boolean set(CommandSender sender, Player target, String[] args) {
        if (args.length < 4) {
            return false;
        }
        int slot = parseSlot(target, args[2]);
        if (slot < 0) {
            sender.sendMessage("&cInvalid slot: " + args[2]);
            return true;
        }
        int id = resolveItem(getServer().getWorld(), args[3]);
        if (id < 0) {
            sender.sendMessage("&cUnknown item: " + args[3]);
            return true;
        }
        int amount = 1;
        int data = 0;
        try {
            if (args.length > 4) {
                amount = Integer.parseInt(args[4]);
            }
            if (args.length > 5) {
                data = Integer.parseInt(args[5]);
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("&cAmount and data must be numbers.");
            return true;
        }
        if (amount < 1 || amount > 64 || data < 0 || data > 5000) {
            sender.sendMessage("&cAmount must be 1-64, data 0-5000.");
            return true;
        }
        if (!apply(sender, target, slot, new ItemStack(id, amount, data))) {
            return true;
        }
        sender.sendMessage("&a" + target.getName() + " slot " + slot + " -> " + itemName(id, amount));
        return true;
    }

    private boolean clear(CommandSender sender, Player target, String slotText) {
        int slot = parseSlot(target, slotText);
        if (slot < 0) {
            sender.sendMessage("&cInvalid slot: " + slotText);
            return true;
        }
        if (apply(sender, target, slot, null)) {
            sender.sendMessage("&a" + target.getName() + " slot " + slot + " emptied.");
        }
        return true;
    }

    /** @return the slot number, or -1 if invalid */
    private int parseSlot(Player target, String text) {
        int size = target.getInventoryContents().length;
        try {
            int slot = Integer.parseInt(text);
            return size > 0 && slot >= 0 && slot < size ? slot : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean apply(CommandSender sender, Player target, int slot, ItemStack item) {
        if (target.getInventoryContents().length == 0) {
            sender.sendMessage("&cThe inventory has not been reported to the server yet; it cannot be edited.");
            return false;
        }
        target.setInventorySlot(slot, item);
        getLogger().warning(sender.getName() + " edited the inventory of " + target.getName() + ": slot " + slot
                + " -> " + (item == null ? "empty" : itemName(item.getTypeId(), item.getAmount())));
        target.sendMessage("&eAn operator edited your inventory (slot " + slot + ").");
        return true;
    }

    private boolean searchInv(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return false;
        }
        int id = resolveItem(getServer().getWorld(), args[0]);
        if (id < 0) {
            sender.sendMessage("&cUnknown item: " + args[0]);
            return true;
        }
        int min = 1;
        try {
            if (args.length > 1) {
                min = Math.max(1, Integer.parseInt(args[1]));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("&cMinimum amount must be a number.");
            return true;
        }
        sender.sendMessage("&6--- " + itemName(id, 1).replace(" x1", "") + " aramasi (en az " + min + ") ---");
        int found = 0;
        for (Player p : getServer().getOnlinePlayers()) {
            int count = 0;
            for (ItemStack it : p.getInventoryContents()) {
                if (it != null && it.getTypeId() == id) {
                    count += it.getAmount();
                }
            }
            if (count >= min) {
                sender.sendMessage("&f" + p.getName() + ": &e" + count + " adet");
                found++;
            }
        }
        sender.sendMessage(found == 0 ? "&7No matching players (only online players are searched)." : "&7Total: " + found + " players.");
        return true;
    }
}
