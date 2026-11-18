/** Screen shown when the connection is lost or the player is kicked. */
public class TWGuiDisconnected extends dc {

    private final String title;
    private final String reason;

    public TWGuiDisconnected(String title, String reason) {
        this.title = title;
        this.reason = reason == null ? "" : reason;
    }

    public void b() {
        e.clear();
        e.add(new iq(0, c / 2 - 100, d / 4 + 120 + 12, "Return to main menu"));
    }

    protected void a(char ch, int key) {
    }

    protected void a(iq button) {
        if (button.d == 0) {
            b.a(new kb());
        }
    }

    public void a(int mx, int my, float partial) {
        h();
        a(g, title, c / 2, d / 2 - 50, 0xFFFFFF);
        String r = reason;
        int y = d / 2 - 30;
        while (r.length() > 0) {
            int cut = Math.min(50, r.length());
            if (cut < r.length()) {
                int sp = r.lastIndexOf(' ', cut);
                if (sp > 20) {
                    cut = sp;
                }
            }
            a(g, r.substring(0, cut).trim(), c / 2, y, 0xFF8080);
            r = r.substring(cut);
            y += 12;
        }
        super.a(mx, my, partial);
    }
}
