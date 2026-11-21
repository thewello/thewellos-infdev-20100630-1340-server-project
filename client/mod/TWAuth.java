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
     * @return the token, or null if there is none
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

    /** Escapes a value for use inside a JSON string. */
    private static String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c < 32) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Waits before trying again after "too many requests" (429) or a server error (5xx). */
    private static final int[] RETRY_DELAYS = {1500, 3000, 6000};

    /** @return the verified profile name */
    public static String join(String session, String serverId) throws IOException {
        String token = extractToken(session);
        if (token == null) {
            throw new IOException("Not logged in with an original account. Start the game from BetaCraft or the Minecraft Launcher with your Microsoft account.");
        }
        // 1) Profile (uuid) info
        HttpURLConnection c = send(Protocol.profileServer() + "/minecraft/profile", token, null);
        int code = c.getResponseCode();
        String body = read(c);
        if (code == 401 || code == 403) {
            throw new IOException("The account session has expired. Close and reopen the launcher, or log out and back in.");
        }
        if (code != 200) {
            throw new IOException("Could not read the profile: " + describe(code, body));
        }
        String id = Protocol.jsonField(body, "id");
        String name = Protocol.jsonField(body, "name");
        if (id == null || name == null) {
            throw new IOException("Could not read profile data");
        }
        // 2) Tell the session server that the player joined
        String json = "{\"accessToken\":\"" + jsonEscape(token) + "\",\"selectedProfile\":\"" + jsonEscape(id)
                + "\",\"serverId\":\"" + jsonEscape(serverId) + "\"}";
        c = send(Protocol.sessionServer() + "/session/minecraft/join", null, json);
        code = c.getResponseCode();
        if (code != 204 && code != 200) {
            throw new IOException("Session could not be verified: " + describe(code, read(c)));
        }
        return name;
    }

    /** Sends a request (POST when json is given). On 429 or 5xx it waits and tries again a few times. */
    private static HttpURLConnection send(String url, String bearer, String json) throws IOException {
        for (int attempt = 0; ; attempt++) {
            HttpURLConnection c = open(url);
            if (bearer != null) {
                c.setRequestProperty("Authorization", "Bearer " + bearer);
            }
            if (json != null) {
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                OutputStream o = c.getOutputStream();
                o.write(json.getBytes("UTF-8"));
                o.close();
            }
            int code = c.getResponseCode();
            if ((code != 429 && code < 500) || attempt >= RETRY_DELAYS.length) {
                return c;
            }
            read(c);
            long wait = RETRY_DELAYS[attempt];
            String retryAfter = c.getHeaderField("Retry-After");
            if (retryAfter != null) {
                try {
                    wait = Math.max(wait, Math.min(10000L, Long.parseLong(retryAfter.trim()) * 1000L));
                } catch (NumberFormatException ignored) {
                }
            }
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                throw new IOException("Interrupted");
            }
        }
    }

    /** A short reason for an HTTP error (no HTML pages in the message). */
    static String describe(int code, String body) {
        if (code == 429) {
            return "Mojang's login service is busy right now (too many requests). Wait a minute and connect again.";
        }
        if (code >= 500) {
            return "Mojang's login service is not available right now (HTTP " + code + "). Try again later.";
        }
        String b = body == null ? "" : body.trim();
        if (b.length() > 160 || b.indexOf('<') >= 0) {
            b = "";
        }
        return "HTTP " + code + (b.length() > 0 ? " " + b : "");
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        c.setUseCaches(false);
        c.setRequestProperty("User-Agent", "TheWellosInfdevServer");
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
