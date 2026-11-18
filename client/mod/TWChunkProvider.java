import net.thewellodev.protocol.Protocol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Chunk provider for multiplayer worlds: it does not generate or save chunks; it only keeps the chunks
 * sent by the server. Chunks that have not arrived yet are returned as empty (air) placeholders
 * and are treated as "missing".
 */
public class TWChunkProvider implements ch {

    private final fe world;
    private final Map<Long, jw> chunks = new HashMap<Long, jw>();
    private final Set<Long> received = new HashSet<Long>();
    private jw lastChunk;
    private int lastX = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;

    public TWChunkProvider(fe world) {
        this.world = world;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    /** chunkExists */
    public boolean a(int x, int z) {
        return received.contains(key(x, z));
    }

    /** provideChunk */
    public jw b(int x, int z) {
        if (x == lastX && z == lastZ && lastChunk != null) {
            return lastChunk;
        }
        Long k = key(x, z);
        jw c = chunks.get(k);
        if (c == null) {
            c = new jw(world, new byte[Protocol.CHUNK_BLOCKS], x, z);
            c.n = true;
            chunks.put(k, c);
        }
        lastX = x;
        lastZ = z;
        lastChunk = c;
        return c;
    }

    /** populate: done by the server in multiplayer */
    public void a(ch provider, int x, int z) {
    }

    /** saveChunks: chunks are not written to disk (level.dat is still saved) */
    public boolean a(boolean all, jd progress) {
        return true;
    }

    /** unloadQueuedChunks */
    public boolean a() {
        return false;
    }

    /** canSave: true if level.dat (inventory) should be saved */
    public boolean b() {
        return true;
    }

    public int loadedCount() {
        return received.size();
    }

    public void receive(int cx, int cz, byte[] raw) {
        jw c = b(cx, cz);
        int off = 0;
        System.arraycopy(raw, off, c.b, 0, Protocol.CHUNK_BLOCKS);
        off += Protocol.CHUNK_BLOCKS;
        System.arraycopy(raw, off, c.e.a, 0, Protocol.CHUNK_NIBBLES);
        off += Protocol.CHUNK_NIBBLES;
        System.arraycopy(raw, off, c.f.a, 0, Protocol.CHUNK_NIBBLES);
        off += Protocol.CHUNK_NIBBLES;
        System.arraycopy(raw, off, c.g.a, 0, Protocol.CHUNK_NIBBLES);
        off += Protocol.CHUNK_NIBBLES;
        System.arraycopy(raw, off, c.h, 0, 256);
        int min = 127;
        for (int i = 0; i < 256; i++) {
            int h = c.h[i] & 255;
            if (h < min) {
                min = h;
            }
        }
        c.i = min;
        c.n = true;
        c.r = false;
        received.add(key(cx, cz));
        world.b(cx * 16, 0, cz * 16, cx * 16 + 15, 127, cz * 16 + 15);
    }

    public void unload(int cx, int cz) {
        Long k = key(cx, cz);
        TWClient.onChunkUnloaded(cx, cz);
        received.remove(k);
        jw c = chunks.remove(k);
        if (c == lastChunk) {
            lastChunk = null;
            lastX = Integer.MIN_VALUE;
        }
        if (c != null) {
            try {
                c.d(); // onChunkUnload
            } catch (Throwable ignored) {
            }
        }
    }
}
