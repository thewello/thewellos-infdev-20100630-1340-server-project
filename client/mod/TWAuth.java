import net.thewellodev.protocol.Protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Verifies the original (Microsoft) account. When BetaCraft logs in with a Microsoft account,
 * Passes the Minecraft access token to the game as the "sessionid". As on normal Minecraft servers:
 * the client "joins" the session server and the server checks it with "hasJoined".
 * The token is only sent to Mojang/Microsoft's official addresses.
 */
public final class TWAuth {

    private TWAuth() {
    }

    /**
     * Extracts the access token from the session data. BetaCraft gives it as is; the official
     * Minecraft Launcher gives old versions the form "token:ACCESS_TOKEN:UUID".
     * @return token ya da yoksa null
     */
    public static String extractToken(String session) {
        if (session == null) {
            return null;
        }
        String t = session.trim();
        if (t.startsWith("token:")) {
            String[] parts = t.split(":");
            t = parts.length >= 2 ? parts[1] : "";
        }
        if (t.length() < 20 || t.equals("-") || t.indexOf(' ') >= 0) {
            return null;
        }
        return t;
    }

    /** @return the verified profile name */
    public static String join(String session, String serverId) throws IOException {
        String token = extractToken(session);
        if (token == null) {
            throw new IOException("Not logged in with an original account. Start the game from BetaCraft or the Minecraft Launcher with your Microsoft account.");
        }
        // 1) Profile (uuid) info
        HttpURLConnection c = open(Protocol.profileServer() + "/minecraft/profile");
        c.setRequestProperty("Authorization", "Bearer " + token);
        int code = c.getResponseCode();
        String body = read(c);
        if (code == 401 || code == 403) {
            throw new IOException("The account session has expired. Close and reopen the launcher, or log out and back in.");
        }
        if (code != 200) {
            throw new IOException("Could not read the profile (HTTP " + code + "). This account may not own Minecraft.");
        }
        String id = Protocol.jsonField(body, "id");
        String name = Protocol.jsonField(body, "name");
        if (id == null || name == null) {
            throw new IOException("Could not read profile data");
        }
        // 2) Tell the session server that the player joined
        c = open(Protocol.sessionServer() + "/session/minecraft/join");
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        String json = "{\"accessToken\":\"" + token + "\",\"selectedProfile\":\"" + id + "\",\"serverId\":\""
                + serverId + "\"}";
        OutputStream o = c.getOutputStream();
        o.write(json.getBytes("UTF-8"));
        o.close();
        code = c.getResponseCode();
        if (code != 204 && code != 200) {
            throw new IOException("Session could not be verified (HTTP " + code + "): " + read(c));
        }
        return name;
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        c.setUseCaches(false);
        c.setRequestProperty("User-Agent", "TheWelloDev");
        return c;
    }

    private static String read(HttpURLConnection c) {
        try {
            InputStream in = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
            if (in == null) {
                return "";
            }
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0 && bo.size() < 65536) {
                bo.write(buf, 0, n);
            }
            in.close();
            return bo.toString("UTF-8");
        } catch (IOException e) {
            return "";
        }
    }
}
