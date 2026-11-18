import net.thewellodev.protocol.Protocol;
import org.lwjgl.input.Keyboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * The "Multiplayer" screen: only the server address is entered. The player name comes from the original
 * account logged in through BetaCraft (if the server uses online-mode, it is verified with Mojang).
 */
public class TWGuiConnect extends dc {

    private static final String ALLOWED_ADDR = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-:_[]";

    private final dc parent;
    private String address = "";
    private int ticks;
    private String error = "";

    public TWGuiConnect(dc parent) {
        this.parent = parent;
        load();
    }

    private File settingsFile() {
        return new File(aw.b(), "thewellodev.txt");
    }

    private void load() {
        try {
            File f = settingsFile();
            if (f.exists()) {
                BufferedReader r = new BufferedReader(new FileReader(f));
                String a = r.readLine();
                r.close();
                if (a != null) {
                    address = a.trim();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void save() {
        try {
            FileWriter w = new FileWriter(settingsFile());
            w.write(address + "\n");
            w.close();
        } catch (Exception ignored) {
        }
    }

    private String accountName() {
        return b != null && b.i != null && b.i.b != null && b.i.b.length() > 0 ? b.i.b : null;
    }

    private boolean hasToken() {
        return TWAuth.extractToken(b != null && b.i != null ? b.i.c : null) != null;
    }

    public void b() {
        Keyboard.enableRepeatEvents(true);
        e.clear();
        e.add(new iq(0, c / 2 - 100, d / 4 + 96 + 12, "Connect"));
        e.add(new iq(1, c / 2 - 100, d / 4 + 120 + 12, "Cancel"));
    }

    public void a() {
        Keyboard.enableRepeatEvents(false);
    }

    public void c() {
        ticks++;
    }

    protected void a(iq button) {
        if (!button.e) {
            return;
        }
        if (button.d == 1) {
            b.a(parent);
        } else if (button.d == 0) {
            doConnect();
        }
    }

    private void doConnect() {
        String addr = address.trim();
        String user = accountName();
        if (addr.length() == 0) {
            error = "Enter the server address (e.g. 192.168.1.5 or example.com:25565)";
            return;
        }
        if (user == null) {
            error = "No account found. Start the game from the launcher with your account.";
            return;
        }
        String host = addr;
        int port = -1; // if not written, use the SRV record, otherwise 25565
        int colon = addr.lastIndexOf(':');
        if (colon > 0 && addr.indexOf(']') < colon && addr.indexOf(':') == colon) {
            host = addr.substring(0, colon);
            try {
                port = Integer.parseInt(addr.substring(colon + 1));
            } catch (NumberFormatException e) {
                error = "Invalid port";
                return;
            }
        } else if (addr.startsWith("[") && addr.contains("]:")) {
            int end = addr.indexOf("]:");
            host = addr.substring(1, end);
            try {
                port = Integer.parseInt(addr.substring(end + 2));
            } catch (NumberFormatException e) {
                error = "Invalid port";
                return;
            }
        }
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        save();
        TWClient.connect(b, host, port, user);
    }

    protected void a(char ch, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            b.a(parent);
            return;
        }
        if (key == Keyboard.KEY_RETURN) {
            doConnect();
            return;
        }
        String cur = address;
        if (key == Keyboard.KEY_BACK) {
            if (cur.length() > 0) {
                cur = cur.substring(0, cur.length() - 1);
            }
        } else if (ch == 22) { // Ctrl+V
            try {
                String clip = (String) java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                        .getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                if (clip != null) {
                    for (char x : clip.trim().toCharArray()) {
                        if (ALLOWED_ADDR.indexOf(x) >= 0) {
                            cur += x;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        } else if (ALLOWED_ADDR.indexOf(ch) >= 0) {
            cur = cur + ch;
        }
        if (cur.length() > 64) {
            cur = cur.substring(0, 64);
        }
        address = cur;
        error = "";
    }

    public void a(int mx, int my, float partial) {
        h();
        a(g, "Multiplayer - TheWelloDev", c / 2, d / 4 - 40, 0xFFFFFF);
        String acc = accountName();
        if (acc == null) {
            a(g, "No account - start the game from the launcher", c / 2, d / 4 - 24, 0xFF5555);
        } else if (hasToken()) {
            a(g, "Account: " + acc + " (original)", c / 2, d / 4 - 24, 0x55FF55);
        } else {
            a(g, "Account: " + acc + " (offline - only for servers with online-mode off)", c / 2, d / 4 - 24, 0xFFFF55);
        }
        int x = c / 2 - 100, y = d / 4 + 30;
        b(g, "Server address (ip or ip:port)", x, y - 10, 0xA0A0A0);
        a(x - 1, y - 1, x + 201, y + 21, 0xFFFFFFFF, 0xFFFFFFFF);
        a(x, y, x + 200, y + 20, 0xFF000000, 0xFF000000);
        b(g, address + ((ticks / 6) % 2 == 0 ? "_" : ""), x + 4, y + 6, 0xE0E0E0);
        if (error.length() > 0) {
            a(g, error, c / 2, d / 4 + 60, 0xFF5555);
        }
        super.a(mx, my, partial);
    }
}
