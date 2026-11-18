/** Screen shown while connecting. */
public class TWGuiConnecting extends dc {

    private final String host;
    private final int port;
    private int ticks;

    public TWGuiConnecting(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void b() {
        e.clear();
        e.add(new iq(0, c / 2 - 100, d / 4 + 120 + 12, "Cancel"));
    }

    public void c() {
        ticks++;
    }

    protected void a(char ch, int key) {
    }

    protected void a(iq button) {
        if (button.d == 0) {
            TWClient.disconnectQuietly();
            b.a(new kb());
        }
    }

    public boolean d() {
        return false;
    }

    public void a(int mx, int my, float partial) {
        h();
        TWConnection c0 = TWClient.conn;
        String status = c0 != null ? c0.status : "No connection";
        String dots = "";
        for (int i = 0; i < (ticks / 5) % 4; i++) {
            dots += ".";
        }
        a(g, "Connecting to server" + dots, c / 2, d / 2 - 50, 0xFFFFFF);
        a(g, port > 0 ? host + ":" + port : host, c / 2, d / 2 - 30, 0xA0A0A0);
        a(g, status, c / 2, d / 2 - 10, 0xFFFF55);
        super.a(mx, my, partial);
    }
}
