package net.thewellodev.server.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * So that friends can connect over the internet:
 * - finds the LAN IP and lists VPN addresses (Radmin, Hamachi, ZeroTier)
 * - opens the port on the router automatically with UPnP (searches every real network adapter; skips VPN adapters)
 * - Dis IP'yi ogrenir, CGNAT'i tespit eder
 */
public final class NetworkHelper {

    private final int port;
    public volatile String lanIp = "?";
    public volatile String publicIp = null;
    /** Lines such as "Radmin VPN: 26.1.2.3". */
    public volatile List<String> vpnAddresses = Collections.emptyList();
    public volatile String status = "Fetching network info...";
    public volatile boolean portOpened;
    public volatile boolean cgnat;
    private String controlUrl;
    private String serviceType;
    private String mappedClient;
    private volatile boolean closed;

    public NetworkHelper(int port) {
        this.port = port;
    }

    /** Arka planda calistirilir. */
    public void run(boolean useUpnp) {
        List<InetAddress> locals = physicalIpv4();
        lanIp = findLanIp(locals);
        vpnAddresses = findVpnAddresses();
        String upnpIp = null;
        boolean gatewayFound = false;
        String upnpError = null;
        if (useUpnp) {
            status = "Opening the port on the router with UPnP...";
            try {
                gatewayFound = discover(locals);
                if (gatewayFound) {
                    upnpIp = getExternalIp();
                    portOpened = addPortMapping(mappedClient != null ? mappedClient : lanIp);
                    if (!portOpened) {
                        upnpError = "the router refused the port mapping request";
                    }
                }
            } catch (Exception e) {
                upnpError = e.getMessage();
            }
        }
        String web = null;
        try {
            web = fetchPublicIp();
        } catch (Exception ignored) {
        }
        publicIp = web != null ? web : upnpIp;
        cgnat = upnpIp != null && isPrivateOrCgnat(upnpIp) || (upnpIp != null && web != null && !upnpIp.equals(web));

        StringBuilder st = new StringBuilder("Server running; players on the same network can join. ");
        if (cgnat) {
            st.append("WARNING: your internet provider uses CGNAT (the router's public IP is ").append(upnpIp)
                    .append("), a direct internet connection is not possible. Use Radmin VPN/ZeroTier with your friends and share the VPN address.");
        } else if (portOpened) {
            st.append("Port ").append(port).append(" was opened on the router automatically (UPnP): friends can join with the Internet address.");
        } else if (!useUpnp) {
            st.append("UPnP is off (server.properties). For internet access, forward port ").append(port)
                    .append(" TCP port to ").append(lanIp).append(" or use the VPN address.");
        } else if (!gatewayFound) {
            st.append("No UPnP router found (enable UPnP in the router settings) or port ").append(port)
                    .append(" TCP to ").append(lanIp).append(" manually. Alternative: use the VPN address.");
        } else {
            st.append("The router answered UPnP but did not open the port (").append(upnpError)
                    .append("). Forward port ").append(port).append(" TCP to ").append(lanIp)
                    .append(" to this address, or use the VPN address.");
        }
        status = st.toString();
        if (portOpened && leaseSeconds > 0) {
            startRenewer();
        }
    }

    public void close() {
        closed = true;
        if (portOpened && controlUrl != null) {
            try {
                soap("DeletePortMapping", "<NewRemoteHost></NewRemoteHost><NewExternalPort>" + port
                        + "</NewExternalPort><NewProtocol>TCP</NewProtocol>");
            } catch (Exception ignored) {
            }
            portOpened = false;
        }
    }

    // ------------------------------------------------------------------ ag kartlari

    private static boolean isVpnName(String name) {
        String n = name.toLowerCase(java.util.Locale.ENGLISH);
        return n.contains("radmin") || n.contains("hamachi") || n.contains("zerotier") || n.contains("vpn")
                || n.contains("virtual") || n.contains("vmware") || n.contains("virtualbox") || n.contains("hyper-v")
                || n.contains("vethernet") || n.contains("tap-") || n.contains("wireguard") || n.contains("tailscale");
    }

    /** Real (not VPN or virtual) IPv4 addresses on active network adapters; private LAN addresses first. */
    private static List<InetAddress> physicalIpv4() {
        List<InetAddress> priv = new ArrayList<InetAddress>();
        List<InetAddress> other = new ArrayList<InetAddress>();
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en != null && en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();
                try {
                    if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                        continue;
                    }
                } catch (Exception e) {
                    continue;
                }
                String name = ni.getDisplayName() + " " + ni.getName();
                if (isVpnName(name)) {
                    continue;
                }
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && !a.isLoopbackAddress() && !a.isLinkLocalAddress()) {
                        if (a.isSiteLocalAddress()) {
                            priv.add(a);
                        } else {
                            other.add(a);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        priv.addAll(other);
        return priv;
    }

    private static String findLanIp(List<InetAddress> locals) {
        // Use the adapter that the default route goes through (no packet is sent)
        DatagramSocket s = null;
        try {
            s = new DatagramSocket();
            s.connect(InetAddress.getByName("8.8.8.8"), 53);
            InetAddress a = s.getLocalAddress();
            if (a != null && !a.isAnyLocalAddress() && locals.contains(a)) {
                return a.getHostAddress();
            }
        } catch (Exception ignored) {
        } finally {
            if (s != null) {
                s.close();
            }
        }
        if (!locals.isEmpty()) {
            return locals.get(0).getHostAddress();
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    /** Radmin VPN (26.x), Hamachi (25.x) and ZeroTier addresses. */
    private static List<String> findVpnAddresses() {
        List<String> out = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en != null && en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();
                if (!ni.isUp() || ni.isLoopback()) {
                    continue;
                }
                String name = (ni.getDisplayName() + " " + ni.getName()).toLowerCase(java.util.Locale.ENGLISH);
                String label = name.contains("radmin") ? "Radmin VPN" : name.contains("hamachi") ? "Hamachi"
                        : name.contains("zerotier") ? "ZeroTier" : name.contains("tailscale") ? "Tailscale" : null;
                if (label == null) {
                    continue;
                }
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address) {
                        out.add(label + ": " + a.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static boolean isPrivateOrCgnat(String ip) {
        try {
            InetAddress a = InetAddress.getByName(ip);
            if (a.isSiteLocalAddress() || a.isLoopbackAddress() || a.isAnyLocalAddress()) {
                return true;
            }
            byte[] b = a.getAddress();
            return b.length == 4 && (b[0] & 255) == 100 && (b[1] & 0xC0) == 64; // 100.64.0.0/10
        } catch (Exception e) {
            return false;
        }
    }

    private static String fetchPublicIp() throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL("https://checkip.amazonaws.com/").openConnection();
        c.setConnectTimeout(5000);
        c.setReadTimeout(5000);
        String ip = readAll(c.getInputStream()).trim();
        return ip.matches("[0-9a-fA-F.:]{3,45}") ? ip : null;
    }

    // ------------------------------------------------------------------ UPnP

    private boolean discover(List<InetAddress> locals) throws IOException {
        Set<String> locations = new LinkedHashSet<String>();
        List<InetAddress> tryFrom = new ArrayList<InetAddress>(locals);
        if (tryFrom.isEmpty()) {
            tryFrom.add(null);
        }
        for (InetAddress from : tryFrom) {
            DatagramSocket sock = from == null ? new DatagramSocket() : new DatagramSocket(new InetSocketAddress(from, 0));
            try {
                sock.setSoTimeout(800);
                String[] targets = {"urn:schemas-upnp-org:device:InternetGatewayDevice:1",
                        "urn:schemas-upnp-org:device:InternetGatewayDevice:2",
                        "urn:schemas-upnp-org:service:WANIPConnection:1", "urn:schemas-upnp-org:service:WANPPPConnection:1"};
                for (int round = 0; round < 2; round++) {
                    for (String st : targets) {
                        String msg = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nST: " + st
                                + "\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\n\r\n";
                        byte[] b = msg.getBytes("US-ASCII");
                        sock.send(new DatagramPacket(b, b.length, new InetSocketAddress("239.255.255.250", 1900)));
                    }
                    long until = System.currentTimeMillis() + 2500;
                    byte[] buf = new byte[2048];
                    while (System.currentTimeMillis() < until) {
                        DatagramPacket p = new DatagramPacket(buf, buf.length);
                        try {
                            sock.receive(p);
                        } catch (SocketTimeoutException e) {
                            continue;
                        }
                        String resp = new String(p.getData(), 0, p.getLength(), "US-ASCII");
                        Matcher m = Pattern.compile("(?im)^location:\\s*(\\S+)").matcher(resp);
                        if (m.find() && trustedLocation(m.group(1), p.getAddress())) {
                            locations.add(m.group(1));
                        }
                    }
                    if (!locations.isEmpty()) {
                        break;
                    }
                }
            } catch (IOException ignored) {
            } finally {
                sock.close();
            }
            for (String loc : locations) {
                if (readGateway(loc)) {
                    if (from != null) {
                        mappedClient = from.getHostAddress();
                        lanIp = mappedClient;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Security: only accept HTTP addresses that belong to the device that answered (the router on the local network),
     * so another device on the network cannot make the server send requests elsewhere.
     */
    private static boolean trustedLocation(String loc, InetAddress sender) {
        try {
            URL u = new URL(loc);
            if (!"http".equalsIgnoreCase(u.getProtocol()) || sender == null || !sender.isSiteLocalAddress()) {
                return false;
            }
            return InetAddress.getByName(u.getHost()).equals(sender);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean readGateway(String loc) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(loc).openConnection();
            c.setConnectTimeout(3000);
            c.setReadTimeout(3000);
            String xml = readAll(c.getInputStream());
            for (String type : new String[]{"urn:schemas-upnp-org:service:WANIPConnection:2",
                    "urn:schemas-upnp-org:service:WANIPConnection:1", "urn:schemas-upnp-org:service:WANPPPConnection:1"}) {
                int i = xml.indexOf(type);
                if (i < 0) {
                    continue;
                }
                int end = xml.indexOf("</service>", i);
                String svc = xml.substring(i, end < 0 ? xml.length() : end);
                Matcher cm = Pattern.compile("<controlURL>\\s*([^<]+?)\\s*</controlURL>").matcher(svc);
                if (cm.find()) {
                    URL base = new URL(loc);
                    URL ctl = new URL(base, cm.group(1));
                    if (!ctl.getHost().equalsIgnoreCase(base.getHost()) || !"http".equalsIgnoreCase(ctl.getProtocol())) {
                        continue; // the control address must be on the router itself
                    }
                    controlUrl = ctl.toString();
                    serviceType = type;
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private String getExternalIp() {
        try {
            String r = soap("GetExternalIPAddress", "");
            Matcher m = Pattern.compile("<NewExternalIPAddress>\\s*([^<]*?)\\s*</NewExternalIPAddress>").matcher(r);
            if (m.find() && m.group(1).length() > 0) {
                return m.group(1);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private int leaseSeconds;

    private boolean addPortMapping(String internalIp) {
        // Try a permanent mapping (0) first, then a temporary one; if there is a conflict, remove the old mapping and retry
        int[] leases = {0, 7200};
        for (int attempt = 0; attempt < 2; attempt++) {
            for (int lease : leases) {
                try {
                    soap("AddPortMapping", "<NewRemoteHost></NewRemoteHost><NewExternalPort>" + port
                            + "</NewExternalPort><NewProtocol>TCP</NewProtocol><NewInternalPort>" + port
                            + "</NewInternalPort><NewInternalClient>" + internalIp
                            + "</NewInternalClient><NewEnabled>1</NewEnabled><NewPortMappingDescription>TheWelloDev Minecraft"
                            + "</NewPortMappingDescription><NewLeaseDuration>" + lease + "</NewLeaseDuration>");
                    leaseSeconds = lease;
                    return true;
                } catch (Exception ignored) {
                }
            }
            try {
                soap("DeletePortMapping", "<NewRemoteHost></NewRemoteHost><NewExternalPort>" + port
                        + "</NewExternalPort><NewProtocol>TCP</NewProtocol>");
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private void startRenewer() {
        Thread t = new Thread("TW UPnP renew") {
            public void run() {
                while (!closed) {
                    try {
                        Thread.sleep(Math.max(60, leaseSeconds / 2) * 1000L);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (!closed) {
                        addPortMapping(mappedClient != null ? mappedClient : lanIp);
                    }
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    private String soap(String action, String args) throws IOException {
        String body = "<?xml version=\"1.0\"?>\r\n<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:" + action + " xmlns:u=\""
                + serviceType + "\">" + args + "</u:" + action + "></s:Body></s:Envelope>";
        HttpURLConnection c = (HttpURLConnection) new URL(controlUrl).openConnection();
        c.setConnectTimeout(3000);
        c.setReadTimeout(5000);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
        c.setRequestProperty("SOAPAction", "\"" + serviceType + "#" + action + "\"");
        OutputStream o = c.getOutputStream();
        o.write(body.getBytes("UTF-8"));
        o.close();
        int code = c.getResponseCode();
        if (code != 200) {
            String err = "";
            try {
                InputStream es = c.getErrorStream();
                if (es != null) {
                    Matcher m = Pattern.compile("<errorDescription>([^<]*)</errorDescription>").matcher(readAll(es));
                    if (m.find()) {
                        err = " " + m.group(1);
                    }
                }
            } catch (Exception ignored) {
            }
            throw new IOException("UPnP " + action + " HTTP " + code + err);
        }
        return readAll(c.getInputStream());
    }

    /** Read-only: finds the router and its public IP without opening a port. For diagnostics. */
    public String probe() {
        List<InetAddress> locals = physicalIpv4();
        lanIp = findLanIp(locals);
        try {
            if (!discover(locals)) {
                return "No UPnP router found (local " + lanIp + ")";
            }
            String ext = getExternalIp();
            return "UPnP router found: " + serviceType + " @ " + controlUrl + ", external IP " + ext
                    + (ext != null && isPrivateOrCgnat(ext) ? " (CGNAT/private!)" : "") + ", local " + lanIp;
        } catch (Exception e) {
            return "UPnP error: " + e;
        }
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0 && bo.size() < 262144) {
            bo.write(buf, 0, n);
        }
        in.close();
        return bo.toString("UTF-8");
    }
}
