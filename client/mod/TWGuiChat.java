import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

/** In-game chat input screen (opened with T or /). */
public class TWGuiChat extends dc {

    private static final List<String> history = new ArrayList<String>();
    private String text;
    private int ticks;
    private int historyIndex = -1;

    public TWGuiChat(String initial) {
        this.text = initial == null ? "" : initial;
    }

    public void b() {
        Keyboard.enableRepeatEvents(true);
        e.clear();
    }

    public void a() {
        Keyboard.enableRepeatEvents(false);
    }

    public void c() {
        ticks++;
    }

    public boolean d() {
        return false;
    }

    private void close() {
        b.a((dc) null);
        b.e();
    }

    protected void a(char ch, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            close();
            return;
        }
        if (key == Keyboard.KEY_RETURN) {
            String msg = text.trim();
            if (msg.length() > 0) {
                if (history.isEmpty() || !history.get(history.size() - 1).equals(msg)) {
                    history.add(msg);
                }
                if (TWClient.conn != null) {
                    TWClient.conn.sendChat(msg);
                }
            }
            close();
            return;
        }
        if (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN) {
            if (history.isEmpty()) {
                return;
            }
            if (historyIndex < 0) {
                historyIndex = history.size();
            }
            historyIndex += key == Keyboard.KEY_UP ? -1 : 1;
            if (historyIndex < 0) {
                historyIndex = 0;
            }
            if (historyIndex >= history.size()) {
                historyIndex = history.size();
                text = "";
            } else {
                text = history.get(historyIndex);
            }
            return;
        }
        if (key == Keyboard.KEY_BACK) {
            if (text.length() > 0) {
                text = text.substring(0, text.length() - 1);
            }
            return;
        }
        if (ch == 22) {
            try {
                String clip = (String) java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                        .getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                if (clip != null) {
                    text += clip.replace('\n', ' ');
                }
            } catch (Exception ignored) {
            }
        } else if (ch >= 32 && ch != 127 && ch < 256) {
            text += ch;
        }
        if (text.length() > 100) {
            text = text.substring(0, 100);
        }
    }

    public void a(int mx, int my, float partial) {
        a(2, d - 14, c - 2, d - 2, 0x80000000, 0x80000000);
        b(g, "> " + text + ((ticks / 6) % 2 == 0 ? "_" : ""), 4, d - 12, 0xE0E0E0);
        super.a(mx, my, partial);
    }
}
