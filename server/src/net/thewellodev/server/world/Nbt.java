package net.thewellodev.server.world;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Minimal NBT reader and writer for the world files of the builds: a GZIP-compressed NBT tree with a named
 * root compound, the same container the inf-20100630-1340 saves use.
 *
 * Values: Byte, Short, Integer, Long, Float, Double, byte[], int[], String, List (of values of one type),
 * Map&lt;String,Object&gt; (compound). Same code as TW227Nbt (the bridges load without the server classes).
 */
public final class Nbt {

    static final int END = 0, BYTE = 1, SHORT = 2, INT = 3, LONG = 4, FLOAT = 5, DOUBLE = 6,
            BYTE_ARRAY = 7, STRING = 8, LIST = 9, COMPOUND = 10, INT_ARRAY = 11;

    private Nbt() {
    }

    /** Writes the root compound (with an empty root name) to a file, replacing any old file. */
    public static void writeFile(File file, Map<String, Object> root) throws IOException {
        File tmp = new File(file.getPath() + ".tmp");
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                new GZIPOutputStream(new FileOutputStream(tmp))));
        try {
            out.writeByte(COMPOUND);
            out.writeUTF("");
            writePayload(out, root);
        } finally {
            out.close();
        }
        if (file.exists() && !file.delete()) {
            throw new IOException("Could not replace " + file);
        }
        if (!tmp.renameTo(file)) {
            throw new IOException("Could not write " + file);
        }
    }

    /** Reads the root compound of a file. */
    public static Map<String, Object> readFile(File file) throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(new FileInputStream(file))));
        try {
            int type = in.readUnsignedByte();
            if (type != COMPOUND) {
                throw new IOException("Root tag must be a compound: " + file);
            }
            in.readUTF();
            return readCompound(in);
        } finally {
            in.close();
        }
    }

    private static void writePayload(DataOutputStream out, Map<String, Object> map) throws IOException {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            int type = typeOf(e.getValue());
            out.writeByte(type);
            out.writeUTF(e.getKey());
            writeValue(out, type, e.getValue());
        }
        out.writeByte(END);
    }

    private static int typeOf(Object v) throws IOException {
        if (v instanceof Byte) {
            return BYTE;
        }
        if (v instanceof Short) {
            return SHORT;
        }
        if (v instanceof Integer) {
            return INT;
        }
        if (v instanceof Long) {
            return LONG;
        }
        if (v instanceof Float) {
            return FLOAT;
        }
        if (v instanceof Double) {
            return DOUBLE;
        }
        if (v instanceof byte[]) {
            return BYTE_ARRAY;
        }
        if (v instanceof int[]) {
            return INT_ARRAY;
        }
        if (v instanceof String) {
            return STRING;
        }
        if (v instanceof List) {
            return LIST;
        }
        if (v instanceof Map) {
            return COMPOUND;
        }
        throw new IOException("Unsupported NBT value: " + (v == null ? "null" : v.getClass().getName()));
    }

    private static void writeValue(DataOutputStream out, int type, Object v) throws IOException {
        switch (type) {
            case BYTE:
                out.writeByte((Byte) v);
                break;
            case SHORT:
                out.writeShort((Short) v);
                break;
            case INT:
                out.writeInt((Integer) v);
                break;
            case LONG:
                out.writeLong((Long) v);
                break;
            case FLOAT:
                out.writeFloat((Float) v);
                break;
            case DOUBLE:
                out.writeDouble((Double) v);
                break;
            case BYTE_ARRAY: {
                byte[] a = (byte[]) v;
                out.writeInt(a.length);
                out.write(a);
                break;
            }
            case INT_ARRAY: {
                int[] a = (int[]) v;
                out.writeInt(a.length);
                for (int i : a) {
                    out.writeInt(i);
                }
                break;
            }
            case STRING:
                out.writeUTF((String) v);
                break;
            case LIST: {
                List<?> list = (List<?>) v;
                int et = list.isEmpty() ? END : typeOf(list.get(0));
                out.writeByte(et);
                out.writeInt(list.size());
                for (Object o : list) {
                    writeValue(out, et, o);
                }
                break;
            }
            case COMPOUND:
                writePayload(out, castMap(v));
                break;
            default:
                throw new IOException("Unknown NBT tag " + type);
        }
    }

    private static Map<String, Object> readCompound(DataInputStream in) throws IOException {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        while (true) {
            int type = in.readUnsignedByte();
            if (type == END) {
                return map;
            }
            String name = in.readUTF();
            map.put(name, readValue(in, type));
        }
    }

    private static Object readValue(DataInputStream in, int type) throws IOException {
        switch (type) {
            case BYTE:
                return in.readByte();
            case SHORT:
                return in.readShort();
            case INT:
                return in.readInt();
            case LONG:
                return in.readLong();
            case FLOAT:
                return in.readFloat();
            case DOUBLE:
                return in.readDouble();
            case BYTE_ARRAY: {
                byte[] a = new byte[readLength(in)];
                in.readFully(a);
                return a;
            }
            case INT_ARRAY: {
                int[] a = new int[readLength(in)];
                for (int i = 0; i < a.length; i++) {
                    a[i] = in.readInt();
                }
                return a;
            }
            case STRING:
                return in.readUTF();
            case LIST: {
                int et = in.readUnsignedByte();
                int n = readLength(in);
                List<Object> list = new ArrayList<Object>(n);
                for (int i = 0; i < n; i++) {
                    list.add(readValue(in, et));
                }
                return list;
            }
            case COMPOUND:
                return readCompound(in);
            default:
                throw new IOException("Unknown NBT tag " + type);
        }
    }

    private static int readLength(DataInputStream in) throws IOException {
        int n = in.readInt();
        if (n < 0 || n > 64 * 1024 * 1024) {
            throw new IOException("Bad NBT length " + n);
        }
        return n;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object v) {
        return (Map<String, Object>) v;
    }
}
