package net.thewellodev.server.game;

import net.thewellodev.asm.HookTransformer;
import net.thewellodev.asm.HookTransformer.Hook;
import net.thewellodev.asm.HookTransformer.Mode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * inf-20100630-1340 client jar's game classes (no package, obfuscated)
 * Loads the game classes and adds hooks with ASM. The bridge classes (TWBridge, TWServerHook)
 * are loaded by the same loader from the "bridge/" folder inside the server jar.
 */
public class GameClassLoader extends ClassLoader {

    private final JarFile gameJar;
    private final HookTransformer transformer;

    public GameClassLoader(File gameJarFile, ClassLoader parent) throws IOException {
        super(parent);
        this.gameJar = new JarFile(gameJarFile);
        if (gameJar.getEntry("fe.class") == null || gameJar.getEntry("jw.class") == null
                || gameJar.getEntry("kb.class") == null) {
            throw new IOException(gameJarFile + " does not look like an inf-20100630-1340 jar");
        }
        String hook = "TWServerHook";
        transformer = new HookTransformer();
        transformer.add(new Hook("fe", "a", "(IIIII)Z", hook, "onBlockSet", Mode.TAIL_PASS_BOOL));
        transformer.add(new Hook("fe", "a", "(IIII)Z", hook, "onBlockSet", Mode.TAIL_PASS_BOOL));
        transformer.add(new Hook("fe", "c", "(IIII)Z", hook, "onBlockSet", Mode.TAIL_PASS_BOOL));
        // The game is single-player: mobs look at the "player in the world" field (fe.f). Before every entity tick
        // that field is set to the nearest player's proxy (the call inside fe.c).
        transformer.custom("fe", HookTransformer.redirect("c", "(Leq;)V", "eq", "b_", "()V", hook, "tickEntity"));
        transformer.custom("jw", HookTransformer.silenceMissingEntity());
        transformer.add(new Hook("ka", "a", "(Leq;IDD)V", hook, "skipZeroKnockback", Mode.HEAD_CANCEL_VOID));
        // Mob spawner block (dungeon): sets fe.f to the nearest player; does nothing if there is no player.
        transformer.add(new Hook("cs", "a", "()Z", hook, "noPlayerNearby", Mode.HEAD_CANCEL_FALSE));
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (this) {
            Class<?> c = findLoadedClass(name);
            if (c == null && name.indexOf('.') < 0) {
                byte[] b = readBridge(name);
                if (b == null) {
                    b = readGame(name);
                }
                if (b != null) {
                    c = defineClass(name, b, 0, b.length);
                }
            }
            if (c == null) {
                return super.loadClass(name, resolve);
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    private byte[] readBridge(String name) {
        InputStream in = getParent().getResourceAsStream("bridge/" + name + ".class");
        if (in == null) {
            return null;
        }
        try {
            return readAll(in);
        } catch (IOException e) {
            return null;
        }
    }

    private byte[] readGame(String name) throws ClassNotFoundException {
        JarEntry e = gameJar.getJarEntry(name + ".class");
        if (e == null) {
            return null;
        }
        try {
            byte[] raw = readAll(gameJar.getInputStream(e));
            return transformer.transform(name, raw);
        } catch (IOException ex) {
            throw new ClassNotFoundException(name, ex);
        }
    }

    /** Checks that the hooks were applied correctly (after fe is loaded). */
    public void verifyHooks() throws IOException {
        try {
            loadClass("fe");
            loadClass("cs");
            loadClass("jw");
        } catch (ClassNotFoundException e) {
            throw new IOException("Game classes could not be loaded: " + e.getMessage());
        }
        if (!transformer.missing().isEmpty()) {
            throw new IOException("Server hooks could not be applied (wrong game version?): " + transformer.missing());
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bo.write(buf, 0, n);
        }
        in.close();
        return bo.toByteArray();
    }

    /** Creates the bridge. */
    public GameBridge createBridge() throws Exception {
        Class<?> c = loadClass("TWBridge");
        return (GameBridge) c.newInstance();
    }
}
