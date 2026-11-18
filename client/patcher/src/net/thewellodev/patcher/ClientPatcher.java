package net.thewellodev.patcher;

import net.thewellodev.asm.HookTransformer;
import net.thewellodev.asm.HookTransformer.Hook;
import net.thewellodev.asm.HookTransformer.Mode;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Patches the inf-20100630-1340 client jar with TheWelloDev multiplayer support.
 *
 * Usage:
 *   java -jar TheWelloDev-ClientPatcher.jar inf-20100630-1340.jar inf-20100630-1340-TheWelloDev.jar
 */
public class ClientPatcher {

    public static final String HOOK = "TWClient";

    public static HookTransformer createTransformer() {
        HookTransformer t = new HookTransformer();
        // Minecraft (aw)
        t.add(new Hook("aw", "i", "()V", HOOK, "preTick", Mode.HEAD_CALL));
        t.add(new Hook("aw", "a", "(I)V", HOOK, "actionBegin", Mode.HEAD_CALL));
        t.add(new Hook("aw", "a", "(I)V", HOOK, "actionEnd", Mode.TAIL_CALL));
        t.add(new Hook("aw", "a", "(IZ)V", HOOK, "actionBegin", Mode.HEAD_CALL));
        t.add(new Hook("aw", "a", "(IZ)V", HOOK, "actionEnd", Mode.TAIL_CALL));
        t.add(new Hook("aw", "a", "(Lfe;Ljava/lang/String;)V", HOOK, "onChangeWorld", Mode.HEAD_CALL));
        // World (fe)
        t.add(new Hook("fe", "a", "(IIIII)Z", HOOK, "onBlockSet", Mode.TAIL_PASS_BOOL));
        t.add(new Hook("fe", "a", "(IIII)Z", HOOK, "onBlockSet", Mode.TAIL_PASS_BOOL));
        t.add(new Hook("fe", "c", "(IIII)Z", HOOK, "onBlockSet", Mode.TAIL_PASS_BOOL));
        t.add(new Hook("fe", "a", "(Ljava/io/File;)Lch;", HOOK, "createChunkProvider", Mode.HEAD_RETURN_IF_NONNULL));
        t.add(new Hook("fe", "a", "(Z)Z", HOOK, "skipTickUpdates", Mode.HEAD_CANCEL_FALSE));
        t.add(new Hook("fe", "i", "()V", HOOK, "worldTick", Mode.HEAD_CANCEL_VOID));
        // Entity spawning: share dropped items through the server
        t.add(new Hook("fe", "a", "(Leq;)V", HOOK, "onSpawnEntity", Mode.HEAD_CANCEL_VOID));
        // Spawn-point search: in multiplayer chunks arrive from the server, so it could loop forever
        t.add(new Hook("fe", "a", "()V", HOOK, "skipSpawnSearch", Mode.HEAD_CANCEL_VOID));
        // Picking up an item from the ground: ask the server first
        t.add(new Hook("gx", "a", "(Lgi;)V", HOOK, "onItemCollide", Mode.HEAD_CANCEL_VOID));
        // Respawn after death
        t.add(new Hook("aw", "o", "()V", HOOK, "onRespawn", Mode.TAIL_CALL));
        // Server mobs: position from the server instead of AI/physics; damage is sent to the server
        final String MOBS = "TWMobs";
        t.custom("fe", HookTransformer.redirect("c", "(Leq;)V", "eq", "b_", "()V", MOBS, "tickEntity"));
        t.custom("ka", HookTransformer.redirect("b_", "()V", "ka", "e", "()V", MOBS, "livingUpdate"));
        t.add(new Hook("ka", "a", "(Leq;I)Z", MOBS, "netDamage", Mode.HEAD_CANCEL_FALSE));
        t.add(new Hook("gr", "a", "(Leq;I)Z", MOBS, "netDamage", Mode.HEAD_CANCEL_FALSE));
        t.add(new Hook("dn", "a", "(Leq;I)Z", MOBS, "netDamage", Mode.HEAD_CANCEL_FALSE));
        t.add(new Hook("cy", "a", "(Leq;I)Z", MOBS, "netDamage", Mode.HEAD_CANCEL_FALSE));
        t.add(new Hook("fe", "a", "(Leq;Ljava/lang/String;FF)V", MOBS, "skipSound", Mode.HEAD_CANCEL_VOID));
        t.add(new Hook("jp", "a", "(Leq;I)Z", MOBS, "netDamage", Mode.HEAD_CANCEL_FALSE));
        t.add(new Hook("ea", "b", "(Lhw;Lka;)V", MOBS, "onSaddle", Mode.HEAD_CALL));
        // Difficulty: in multiplayer the server's difficulty is used, not the one in the options (aw.i: fe.g = ja.x)
        t.custom("aw", new HookTransformer.Custom() {
            public boolean apply(ClassNode cn) {
                for (MethodNode m : cn.methods) {
                    if (!m.name.equals("i") || !m.desc.equals("()V")) {
                        continue;
                    }
                    for (AbstractInsnNode n = m.instructions.getFirst(); n != null; n = n.getNext()) {
                        if (n.getOpcode() == Opcodes.GETFIELD && n.getNext() != null
                                && n.getNext().getOpcode() == Opcodes.PUTFIELD) {
                            FieldInsnNode a = (FieldInsnNode) n;
                            FieldInsnNode b = (FieldInsnNode) n.getNext();
                            if (a.owner.equals("ja") && a.name.equals("x") && b.owner.equals("fe") && b.name.equals("g")) {
                                m.instructions.insert(n, new MethodInsnNode(Opcodes.INVOKESTATIC, MOBS,
                                        "filterDifficulty", "(I)I", false));
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        });
        t.custom("jw", HookTransformer.silenceMissingEntity());
        t.add(new Hook("dd", "a", "(Lkd;)V", HOOK, "openChest", Mode.HEAD_CANCEL_VOID));
        t.add(new Hook("dd", "a", "(Lel;)V", HOOK, "openFurnace", Mode.HEAD_CANCEL_VOID));
        t.custom("c", HookTransformer.fixDistanceComparator(HOOK, "cmpDist"));
        t.custom("if", HookTransformer.fixDistanceComparator(HOOK, "cmpDist"));
        t.add(new Hook("aw", "i", "()V", HOOK, "postTick", Mode.TAIL_CALL));
        t.add(new Hook("ka", "a", "(Leq;IDD)V", HOOK, "skipZeroKnockback", Mode.HEAD_CANCEL_VOID));
        // Mob spawner (ck)
        t.add(new Hook("ck", "a", "(Lfe;)V", HOOK, "skipSpawner", Mode.HEAD_CANCEL_VOID));
        // Mob spawner block (dungeons): no local mob spawning in multiplayer
        t.add(new Hook("cs", "a", "()Z", HOOK, "skipSpawnerBlock", Mode.HEAD_CANCEL_FALSE));
        // Main menu (kb)
        t.add(new Hook("kb", "b", "()V", HOOK, "titleInit", Mode.TAIL_CALL));
        t.add(new Hook("kb", "a", "(Liq;)V", HOOK, "titleAction", Mode.HEAD_CANCEL_VOID));
        // Other players' block-breaking cracks: drawn in cb.b(F) after the particles (dp.a(eq,F))
        t.custom("cb", new HookTransformer.Custom() {
            public boolean apply(ClassNode cn) {
                for (MethodNode m : cn.methods) {
                    if (!m.name.equals("b") || !m.desc.equals("(F)V")) {
                        continue;
                    }
                    for (AbstractInsnNode n = m.instructions.getFirst(); n != null; n = n.getNext()) {
                        if (n.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                            MethodInsnNode mi = (MethodInsnNode) n;
                            if (mi.owner.equals("dp") && mi.name.equals("a") && mi.desc.equals("(Leq;F)V")) {
                                InsnList il = new InsnList();
                                il.add(new VarInsnNode(Opcodes.ALOAD, 3));
                                il.add(new VarInsnNode(Opcodes.ALOAD, 2));
                                il.add(new VarInsnNode(Opcodes.FLOAD, 1));
                                il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "renderDigs", "(Lm;Lgi;F)V", false));
                                m.instructions.insert(n, il);
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        });
        // Chat key: in the keyboard loop of aw.i(), right before the ESC check
        t.custom("aw", new HookTransformer.Custom() {
            public boolean apply(ClassNode cn) {
                for (MethodNode m : cn.methods) {
                    if (!m.name.equals("i") || !m.desc.equals("()V")) {
                        continue;
                    }
                    for (AbstractInsnNode n = m.instructions.getFirst(); n != null; n = n.getNext()) {
                        if (n.getOpcode() == Opcodes.INVOKESTATIC) {
                            MethodInsnNode mi = (MethodInsnNode) n;
                            AbstractInsnNode nx = n.getNext();
                            if (mi.owner.equals("org/lwjgl/input/Keyboard") && mi.name.equals("getEventKey")
                                    && nx != null && nx.getOpcode() == Opcodes.ICONST_1
                                    && nx.getNext() != null && nx.getNext().getOpcode() == Opcodes.IF_ICMPNE) {
                                InsnList il = new InsnList();
                                il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                                il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "onKey", "(Law;)V", false));
                                m.instructions.insertBefore(n, il);
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        });
        return t;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("TheWelloDev Client Patcher");
            System.out.println("Usage: java -jar TheWelloDev-ClientPatcher.jar <inf-20100630-1340.jar> <output.jar>");
            System.exit(1);
        }
        boolean publicizeOnly = args.length > 2 && args[2].equals("--publicize-only");
        File in = new File(args[0]);
        File out = new File(args[1]);
        patch(in, out, publicizeOnly);
        System.out.println("Done: " + out.getAbsolutePath());
    }

    public static void patch(File in, File out, boolean publicizeOnly) throws IOException {
        HookTransformer t = publicizeOnly ? new HookTransformer() : createTransformer();
        JarFile jar = new JarFile(in);
        if (jar.getEntry("kb.class") == null || jar.getEntry("fe.class") == null || jar.getEntry("aw.class") == null) {
            throw new IOException("This file does not look like an inf-20100630-1340 client jar: " + in);
        }
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (!publicizeOnly) {
            mf.getMainAttributes().putValue("TheWelloDev-Client", "1.0");
        }
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(out), mf);
        Set<String> written = new HashSet<String>();
        written.add("META-INF/MANIFEST.MF");
        try {
            Enumeration<JarEntry> en = jar.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                String name = e.getName();
                if (name.startsWith("META-INF/") || e.isDirectory()) {
                    continue;
                }
                byte[] data = readAll(jar.getInputStream(e));
                if (name.endsWith(".class")) {
                    String internal = name.substring(0, name.length() - 6);
                    data = t.transform(internal, data);
                }
                put(jos, written, name, data);
            }
            if (!publicizeOnly) {
                List<String> miss = t.missing();
                if (!miss.isEmpty()) {
                    throw new IOException("Some hooks could not be applied (wrong version?): " + miss);
                }
                // Mod siniflarini ekle
                InputStream idx = ClientPatcher.class.getResourceAsStream("/mod/index.txt");
                if (idx == null) {
                    throw new IOException("mod/index.txt not found in the patcher");
                }
                BufferedReader br = new BufferedReader(new InputStreamReader(idx, "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.length() == 0) {
                        continue;
                    }
                    InputStream res = ClientPatcher.class.getResourceAsStream("/mod/" + line);
                    if (res == null) {
                        throw new IOException("Missing mod file: " + line);
                    }
                    put(jos, written, line, readAll(res));
                }
                br.close();
            }
        } finally {
            jos.close();
            jar.close();
        }
    }

    private static void put(JarOutputStream jos, Set<String> written, String name, byte[] data) throws IOException {
        if (!written.add(name)) {
            return;
        }
        JarEntry je = new JarEntry(name);
        jos.putNextEntry(je);
        jos.write(data);
        jos.closeEntry();
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bo.write(buf, 0, n);
        }
        in.close();
        return bo.toByteArray();
    }

    @SuppressWarnings("unused")
    private static byte[] readFile(File f) throws IOException {
        return readAll(new FileInputStream(f));
    }
}
