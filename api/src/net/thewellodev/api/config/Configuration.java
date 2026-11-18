package net.thewellodev.api.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple YAML configuration (a subset that is enough for plugin.yml and config.yml):
 * <ul>
 *   <li>{@code key: value}</li>
 *   <li>indented sections</li>
 *   <li>{@code - eleman} listeleri ve {@code [a, b, c]} satir ici listeler</li>
 *   <li>{@code #} yorumlari, tek/cift tirnakli metinler</li>
 * </ul>
 * Anahtarlara noktali yolla erisilir: {@code getString("database.host")}.
 */
public class Configuration {

    private Map<String, Object> root = new LinkedHashMap<String, Object>();
    private File file;

    public Configuration() {
    }

    public Configuration(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    // ------------------------------------------------------------ loading

    public static Configuration load(File file) throws IOException {
        Configuration c = new Configuration(file);
        if (file.exists()) {
            InputStream in = new FileInputStream(file);
            try {
                c.load(in);
            } finally {
                in.close();
            }
        }
        return c;
    }

    public void load(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        List<String> lines = new ArrayList<String>();
        String line;
        while ((line = r.readLine()) != null) {
            lines.add(line.replace("\t", "    "));
        }
        root = new LinkedHashMap<String, Object>();
        int[] pos = {0};
        parseMap(lines, pos, -1, root);
    }

    private static int indentOf(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    private static boolean isBlank(String s) {
        String t = s.trim();
        return t.length() == 0 || t.startsWith("#");
    }

    private static void parseMap(List<String> lines, int[] pos, int parentIndent, Map<String, Object> out) {
        int myIndent = -1;
        while (pos[0] < lines.size()) {
            String raw = lines.get(pos[0]);
            if (isBlank(raw)) {
                pos[0]++;
                continue;
            }
            int ind = indentOf(raw);
            if (ind <= parentIndent) {
                return;
            }
            if (myIndent < 0) {
                myIndent = ind;
            } else if (ind != myIndent) {
                return;
            }
            String t = stripComment(raw.trim());
            int colon = findColon(t);
            if (colon < 0) {
                pos[0]++;
                continue;
            }
            String key = unquote(t.substring(0, colon).trim());
            String val = t.substring(colon + 1).trim();
            pos[0]++;
            if (val.length() == 0) {
                // nested section or list
                int next = nextContent(lines, pos[0]);
                if (next >= 0 && indentOf(lines.get(next)) > ind && lines.get(next).trim().startsWith("- ")) {
                    out.put(key, parseList(lines, pos, ind));
                } else if (next >= 0 && indentOf(lines.get(next)) > ind) {
                    Map<String, Object> child = new LinkedHashMap<String, Object>();
                    parseMap(lines, pos, ind, child);
                    out.put(key, child);
                } else {
                    out.put(key, null);
                }
            } else {
                out.put(key, parseScalar(val));
            }
        }
    }

    private static List<Object> parseList(List<String> lines, int[] pos, int parentIndent) {
        List<Object> list = new ArrayList<Object>();
        while (pos[0] < lines.size()) {
            String raw = lines.get(pos[0]);
            if (isBlank(raw)) {
                pos[0]++;
                continue;
            }
            int ind = indentOf(raw);
            String t = raw.trim();
            if (ind <= parentIndent || !(t.startsWith("- ") || t.equals("-"))) {
                return list;
            }
            list.add(parseScalar(stripComment(t.substring(1).trim())));
            pos[0]++;
        }
        return list;
    }

    private static int nextContent(List<String> lines, int from) {
        for (int i = from; i < lines.size(); i++) {
            if (!isBlank(lines.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int findColon(String t) {
        boolean sq = false, dq = false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '\'' && !dq) {
                sq = !sq;
            } else if (c == '"' && !sq) {
                dq = !dq;
            } else if (c == ':' && !sq && !dq && (i + 1 == t.length() || t.charAt(i + 1) == ' ')) {
                return i;
            }
        }
        return -1;
    }

    private static String stripComment(String t) {
        boolean sq = false, dq = false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '\'' && !dq) {
                sq = !sq;
            } else if (c == '"' && !sq) {
                dq = !dq;
            } else if (c == '#' && !sq && !dq && (i == 0 || t.charAt(i - 1) == ' ')) {
                return t.substring(0, i).trim();
            }
        }
        return t;
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static Object parseScalar(String v) {
        if (v.startsWith("[") && v.endsWith("]")) {
            List<Object> l = new ArrayList<Object>();
            String inner = v.substring(1, v.length() - 1).trim();
            if (inner.length() > 0) {
                for (String part : inner.split(",")) {
                    l.add(parseScalar(part.trim()));
                }
            }
            return l;
        }
        if ((v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2)
                || (v.startsWith("'") && v.endsWith("'") && v.length() >= 2)) {
            return v.substring(1, v.length() - 1);
        }
        if (v.equals("true") || v.equals("yes")) {
            return Boolean.TRUE;
        }
        if (v.equals("false") || v.equals("no")) {
            return Boolean.FALSE;
        }
        if (v.equals("null") || v.equals("~")) {
            return null;
        }
        try {
            if (v.matches("-?\\d+")) {
                long l = Long.parseLong(v);
                if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                    return (int) l;
                }
                return l;
            }
            if (v.matches("-?\\d*\\.\\d+")) {
                return Double.parseDouble(v);
            }
        } catch (NumberFormatException ignored) {
        }
        return v;
    }

    // ------------------------------------------------------------ access

    @SuppressWarnings("unchecked")
    public Object get(String path) {
        String[] parts = path.split("\\.");
        Object cur = root;
        for (String p : parts) {
            if (!(cur instanceof Map)) {
                return null;
            }
            cur = ((Map<String, Object>) cur).get(p);
        }
        return cur;
    }

    public Object get(String path, Object def) {
        Object o = get(path);
        return o == null ? def : o;
    }

    public boolean contains(String path) {
        return get(path) != null;
    }

    @SuppressWarnings("unchecked")
    public void set(String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> cur = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object o = cur.get(parts[i]);
            if (!(o instanceof Map)) {
                o = new LinkedHashMap<String, Object>();
                cur.put(parts[i], o);
            }
            cur = (Map<String, Object>) o;
        }
        if (value == null) {
            cur.remove(parts[parts.length - 1]);
        } else {
            cur.put(parts[parts.length - 1], value);
        }
    }

    /** If the key is missing, the default value is written. */
    public void addDefault(String path, Object value) {
        if (!contains(path)) {
            set(path, value);
        }
    }

    public String getString(String path) {
        return getString(path, null);
    }

    public String getString(String path, String def) {
        Object o = get(path);
        return o == null ? def : String.valueOf(o);
    }

    public int getInt(String path, int def) {
        Object o = get(path);
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return o == null ? def : Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public long getLong(String path, long def) {
        Object o = get(path);
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return o == null ? def : Long.parseLong(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public double getDouble(String path, double def) {
        Object o = get(path);
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        try {
            return o == null ? def : Double.parseDouble(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public boolean getBoolean(String path, boolean def) {
        Object o = get(path);
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        if (o == null) {
            return def;
        }
        String s = String.valueOf(o).trim();
        return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes");
    }

    public List<String> getStringList(String path) {
        Object o = get(path);
        List<String> out = new ArrayList<String>();
        if (o instanceof List) {
            for (Object e : (List<?>) o) {
                out.add(String.valueOf(e));
            }
        } else if (o != null) {
            out.add(String.valueOf(o));
        }
        return out;
    }

    /** Direct child keys of the given section ("" = root). */
    @SuppressWarnings("unchecked")
    public Set<String> getKeys(String path) {
        Object o = path == null || path.length() == 0 ? root : get(path);
        if (o instanceof Map) {
            return new java.util.LinkedHashSet<String>(((Map<String, Object>) o).keySet());
        }
        return new java.util.LinkedHashSet<String>();
    }

    public boolean isSection(String path) {
        return get(path) instanceof Map;
    }


    public void save() throws IOException {
        if (file == null) {
            throw new IOException("This Configuration is not bound to a file");
        }
        save(file);
    }

    public void save(File f) throws IOException {
        File parent = f.getAbsoluteFile().getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        try {
            writeMap(w, root, 0);
        } finally {
            w.close();
        }
    }

    @SuppressWarnings("unchecked")
    private static void writeMap(Writer w, Map<String, Object> m, int indent) throws IOException {
        for (Map.Entry<String, Object> e : m.entrySet()) {
            pad(w, indent);
            w.write(e.getKey());
            w.write(":");
            Object v = e.getValue();
            if (v instanceof Map) {
                w.write("\n");
                writeMap(w, (Map<String, Object>) v, indent + 2);
            } else if (v instanceof List) {
                w.write("\n");
                for (Object o : (List<?>) v) {
                    pad(w, indent + 2);
                    w.write("- " + scalar(o) + "\n");
                }
            } else {
                w.write(" " + scalar(v) + "\n");
            }
        }
    }

    private static void pad(Writer w, int n) throws IOException {
        for (int i = 0; i < n; i++) {
            w.write(' ');
        }
    }

    private static String scalar(Object o) {
        if (o == null) {
            return "null";
        }
        if (o instanceof Number || o instanceof Boolean) {
            return String.valueOf(o);
        }
        String s = String.valueOf(o);
        if (s.length() == 0 || s.contains(": ") || s.contains(" #") || s.startsWith("[") || s.startsWith("'")
                || s.startsWith("\"") || s.startsWith("-") || s.startsWith("&") || s.matches("-?[\\d.]+")
                || s.equals("true") || s.equals("false")) {
            return "\"" + s.replace("\"", "'") + "\"";
        }
        return s;
    }
}
