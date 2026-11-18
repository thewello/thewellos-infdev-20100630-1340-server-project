package net.thewellodev.server.net;

import net.thewellodev.protocol.Protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;

/**
 * The "online-mode" verification used on normal Minecraft servers: the client joins the Mojang session server
 * The client "joins" the Mojang session server; the server then checks with "hasJoined" that the user really joined this server.
 */
public final class SessionVerifier {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SessionVerifier() {
    }

    public static String newServerId() {
        return Long.toHexString(RANDOM.nextLong() & Long.MAX_VALUE);
    }

    /** @return the exact user name confirmed by Mojang */
    public static String hasJoined(String username, String serverId) throws IOException {
        String url = Protocol.sessionServer() + "/session/minecraft/hasJoined?username="
                + URLEncoder.encode(username, "UTF-8") + "&serverId=" + URLEncoder.encode(serverId, "UTF-8");
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        c.setUseCaches(false);
        c.setRequestProperty("User-Agent", "TheWelloDev");
        int code = c.getResponseCode();
        if (code == 204 || code == 404) {
            throw new IOException("Original account could not be verified");
        }
        if (code != 200) {
            throw new IOException("The Mojang session server did not answer (HTTP " + code + ")");
        }
        InputStream in = c.getInputStream();
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0 && bo.size() < 65536) {
            bo.write(buf, 0, n);
        }
        in.close();
        String body = bo.toString("UTF-8");
        String name = Protocol.jsonField(body, "name");
        if (name == null || !name.equalsIgnoreCase(username)) {
            throw new IOException("Account name does not match");
        }
        return name;
    }
}
