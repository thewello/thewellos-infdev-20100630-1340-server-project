package net.thewellodev.server.gui;

import net.thewellodev.api.ItemStack;
import net.thewellodev.api.Material;
import net.thewellodev.server.TWServer;
import net.thewellodev.server.entity.TWPlayer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Locale;

/**
 * Window that shows a player's inventory in the server panel. Items are grouped as hotbar, bag and armour;
 * click a slot to select it and delete it. The data is the last summary sent by the player's client,
 * refreshed every second.
 */
final class InventoryView {

    private static final int CELL = 58;
    private static final int HOTBAR_END = 9;     // 0-8: hotbar
    private static final int MAIN_END = 36;      // 9-35: bag
    private static final String[] ARMOR = {"Helmet", "Chest", "Leggings", "Boots"};

    private final TWServer server;
    private final String name;
    private final JDialog dialog;
    private final Timer timer;
    private final JLabel header = new JLabel(" ");
    private final JLabel chips = new JLabel(" ");
    private final JLabel details = new JLabel("Click a slot.");
    private final JLabel status = new JLabel(" ");
    private final JButton deleteSlot = button("Delete selected slot", new Color(0xED4245));
    private final JButton deleteType = button("Delete all of this type", new Color(0xF0A232));
    private final Cell[] cells = new Cell[40];
    private ItemStack[] shown = new ItemStack[0];
    private int selected = -1;
    private boolean gone;

    private InventoryView(JFrame owner, TWServer server, String name) {
        this.server = server;
        this.name = name;
        dialog = new JDialog(owner, "Inventory - " + name, false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(680, 720);
        dialog.setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(ServerGui.PANEL);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildSlots(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        dialog.setContentPane(root);

        timer = new Timer(1000, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                refresh();
            }
        });
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent e) {
                timer.stop();
            }
        });
        refresh();
        timer.start();
    }

    /** Opens the inventory window of a player. Brings the existing window for that player to the front. */
    static void open(JFrame owner, TWServer server, String name) {
        TWPlayer p = find(server, name);
        if (p == null) {
            JOptionPane.showMessageDialog(owner, name + " is not online.", "Inventory", JOptionPane.WARNING_MESSAGE);
            return;
        }
        InventoryView v = new InventoryView(owner, server, p.getName());
        v.dialog.setVisible(true);
    }

    private static TWPlayer find(TWServer server, String name) {
        for (TWPlayer p : server.getPlayerList()) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    private JComponent buildHeader() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        header.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.setForeground(Color.WHITE);
        chips.setFont(new Font("SansSerif", Font.PLAIN, 12));
        chips.setForeground(ServerGui.MUTED);
        p.add(header, BorderLayout.NORTH);
        p.add(chips, BorderLayout.SOUTH);
        return p;
    }

    private JComponent buildSlots() {
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new Cell(i);
        }
        JPanel col = new JPanel(new GridBagLayout());
        col.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, 4, 0);
        int y = 0;
        c.gridy = y++;
        col.add(section("Hotbar"), c);
        c.gridy = y++;
        col.add(row(0, HOTBAR_END), c);
        c.gridy = y++;
        c.insets = new Insets(12, 0, 4, 0);
        col.add(section("Bag"), c);
        for (int r = 0; r < 3; r++) {
            c.gridy = y++;
            c.insets = new Insets(0, 0, 4, 0);
            col.add(row(HOTBAR_END + r * 9, HOTBAR_END + r * 9 + 9), c);
        }
        c.gridy = y++;
        c.insets = new Insets(12, 0, 4, 0);
        col.add(section("Armour"), c);
        JPanel armor = new JPanel(new GridLayout(1, 9, 4, 4));
        armor.setOpaque(false);
        for (int i = 0; i < 4; i++) {
            armor.add(labelled(cells[MAIN_END + i], ARMOR[i]));
        }
        for (int i = 4; i < 9; i++) {
            JPanel gap = new JPanel();
            gap.setOpaque(false);
            armor.add(gap);
        }
        c.gridy = y++;
        c.insets = new Insets(0, 0, 4, 0);
        col.add(armor, c);
        // the spare space goes to the bottom
        c.gridy = y;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        col.add(filler, c);
        return col;
    }

    private JComponent section(String text) {
        JLabel l = new JLabel(text.toUpperCase(Locale.ENGLISH));
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(ServerGui.MUTED);
        l.setBorder(new EmptyBorder(0, 0, 4, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JComponent row(int from, int to) {
        JPanel p = new JPanel(new GridLayout(1, to - from, 4, 4));
        p.setOpaque(false);
        for (int i = from; i < to; i++) {
            p.add(cells[i]);
        }
        return p;
    }

    private static JComponent labelled(JComponent cell, String text) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        p.add(cell, BorderLayout.CENTER);
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setForeground(ServerGui.MUTED);
        l.setFont(new Font("SansSerif", Font.PLAIN, 10));
        p.add(l, BorderLayout.SOUTH);
        return p;
    }

    private JComponent buildFooter() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        JPanel info = new JPanel(new BorderLayout(0, 4));
        info.setOpaque(false);
        details.setFont(new Font("SansSerif", Font.PLAIN, 13));
        details.setForeground(ServerGui.TEXT);
        status.setFont(new Font("SansSerif", Font.PLAIN, 11));
        status.setForeground(ServerGui.MUTED);
        info.add(details, BorderLayout.NORTH);
        info.add(status, BorderLayout.SOUTH);
        p.add(info, BorderLayout.NORTH);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        deleteSlot.setEnabled(false);
        deleteType.setEnabled(false);
        deleteSlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                deleteSelected(false);
            }
        });
        deleteType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                deleteSelected(true);
            }
        });
        JButton close = button("Close", ServerGui.PANEL2);
        close.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dialog.dispose();
            }
        });
        btns.add(deleteType);
        btns.add(deleteSlot);
        btns.add(close);
        p.add(btns, BorderLayout.SOUTH);
        return p;
    }

    private static JButton button(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ---------------------------------------------------------------- data

    private void refresh() {
        TWPlayer p = find(server, name);
        if (p == null) {
            if (!gone) {
                gone = true;
                status.setText("Player left; you can close this window.");
                deleteSlot.setEnabled(false);
                deleteType.setEnabled(false);
                header.setText(name + "  (offline)");
                timer.stop();
            }
            return;
        }
        boolean op = server.isOp(p.getName());
        header.setText(p.getName() + (op ? "  [OP]" : ""));
        String mode = p.getGameMode() == 1 ? "Creative" : "Survival";
        chips.setText("Health " + p.getHealth() + "/20   |   Mode: " + mode + "   |   Ping: "
                + (p.getPing() >= 0 ? p.getPing() + " ms" : "-") + "   |   Holding: " + itemName(p.getItemInHand()));

        ItemStack[] now = p.getInventoryContents();
        if (!signature(now).equals(signature(shown))) {
            shown = now;
            for (int i = 0; i < cells.length; i++) {
                cells[i].setItem(i < now.length ? now[i] : null);
            }
            if (selected >= 0 && (selected >= now.length || now[selected] == null)) {
                select(-1);
            }
        }
        if (now.length == 0) {
            status.setText("Inventory not reported to the server yet.");
        } else {
            status.setText("Last update: " + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
        }
    }

    private static String signature(ItemStack[] items) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack it : items) {
            sb.append(it == null ? "-" : it.getTypeId() + ":" + it.getAmount() + ":" + it.getData()).append(';');
        }
        return sb.toString();
    }

    private void select(int index) {
        if (selected >= 0) {
            cells[selected].setSelected(false);
        }
        selected = index;
        if (index >= 0) {
            cells[index].setSelected(true);
        }
        ItemStack it = index >= 0 && index < shown.length ? shown[index] : null;
        boolean has = it != null && !gone;
        deleteSlot.setEnabled(has);
        deleteType.setEnabled(has);
        if (index < 0) {
            details.setText("Click a slot.");
        } else if (it == null) {
            details.setText("Slot " + index + " (" + slotName(index) + "): empty");
        } else {
            details.setText("Slot " + index + " (" + slotName(index) + "): " + itemName(it.getTypeId()) + " x" + it.getAmount()
                    + (it.getData() != 0 ? "  data " + it.getData() : ""));
        }
    }

    private void deleteSelected(boolean wholeType) {
        if (selected < 0 || selected >= shown.length || shown[selected] == null) {
            return;
        }
        ItemStack it = shown[selected];
        String what = wholeType ? "All " + itemName(it.getTypeId()) + " items in the inventory will be deleted."
                : "Slot " + selected + " (" + itemName(it.getTypeId()) + " x" + it.getAmount() + ") will be deleted.";
        int ok = JOptionPane.showConfirmDialog(dialog, what + "\nThis cannot be undone. Continue?",
                "Delete items", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        if (wholeType) {
            server.submitInventoryDelete("GUI", name, -1, it.getTypeId());
        } else {
            server.submitInventoryDelete("GUI", name, selected, -1);
        }
        // The view is refreshed on the next update; clear it now
        shown = Arrays.copyOf(shown, shown.length);
        if (wholeType) {
            for (int i = 0; i < shown.length; i++) {
                if (shown[i] != null && shown[i].getTypeId() == it.getTypeId()) {
                    shown[i] = null;
                    cells[i].setItem(null);
                }
            }
        } else {
            shown[selected] = null;
            cells[selected].setItem(null);
        }
        select(-1);
    }

    static String itemName(int id) {
        if (id < 0) {
            return "-";
        }
        Material m = Material.getMaterial(id);
        return m == null ? "#" + id : m.name().toLowerCase(Locale.ENGLISH);
    }

    private static String slotName(int i) {
        if (i < HOTBAR_END) {
            return "hotbar";
        }
        if (i < MAIN_END) {
            return "inventory";
        }
        return "armour";
    }

    /** Colour by item type: block, tool or weapon, food, armour, other. */
    static Color colorOf(int id) {
        if (id < 256) {
            return new Color(0x4E7A9E);
        }
        if (id >= 298 && id <= 317) {
            return new Color(0x7A8594);
        }
        switch (id) {
            case 260: case 282: case 297: case 319: case 320: case 322:
                return new Color(0xB5523B);
            case 256: case 257: case 258: case 267: case 268: case 269: case 270: case 271: case 272: case 273:
            case 274: case 275: case 276: case 277: case 278: case 279: case 283: case 284: case 285: case 286:
                return new Color(0xB08D57);
            default:
                return new Color(0x6B7280);
        }
    }

    // ---------------------------------------------------------------- cell

    /** One inventory slot. */
    private final class Cell extends JComponent {
        private final int index;
        private ItemStack item;
        private boolean selectedCell;

        Cell(final int index) {
            this.index = index;
            setPreferredSize(new Dimension(CELL, CELL));
            setMinimumSize(new Dimension(CELL, CELL));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    select(index);
                }
            });
        }

        void setItem(ItemStack it) {
            item = it;
            setToolTipText(it == null ? null : itemName(it.getTypeId()) + " x" + it.getAmount());
            repaint();
        }

        void setSelected(boolean s) {
            selectedCell = s;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(0x1E1F22));
            g.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
            if (selectedCell) {
                g.setColor(ServerGui.BLUE);
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(1, 1, w - 3, h - 3, 8, 8);
            } else {
                g.setColor(new Color(0x3A3C41));
                g.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
            }
            if (item != null) {
                g.setColor(colorOf(item.getTypeId()));
                g.fillRoundRect(10, 8, w - 20, h - 26, 6, 6);
                g.setColor(new Color(255, 255, 255, 60));
                g.fillRoundRect(12, 10, w - 24, (h - 26) / 3, 6, 6);
                g.setFont(new Font("SansSerif", Font.PLAIN, 9));
                g.setColor(ServerGui.TEXT);
                String label = shortName(itemName(item.getTypeId()));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(label, (w - fm.stringWidth(label)) / 2, h - 12);
                if (item.getAmount() > 1) {
                    g.setFont(new Font("SansSerif", Font.BOLD, 12));
                    String n = String.valueOf(item.getAmount());
                    FontMetrics nm = g.getFontMetrics();
                    g.setColor(Color.BLACK);
                    g.drawString(n, w - nm.stringWidth(n) - 6, 12 + 1);
                    g.setColor(Color.WHITE);
                    g.drawString(n, w - nm.stringWidth(n) - 7, 12);
                }
            }
            g.dispose();
        }

        private String shortName(String s) {
            return s.length() <= 8 ? s : s.substring(0, 7) + ".";
        }
    }
}
