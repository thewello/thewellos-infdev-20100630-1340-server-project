package net.thewellodev.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper that inserts small "hook" calls into obfuscated game classes.
 * The game classes use class-file version 49 (Java 5), so stack map frames are not needed;
 * only COMPUTE_MAXS is used.
 */
public final class HookTransformer {

    /** Hook types. */
    public enum Mode {
        /** Calls a void hook at the start of the method. */
        HEAD_CALL,
        /** Calls a boolean hook at the start of the method; if it returns true, exits the void method. */
        HEAD_CANCEL_VOID,
        /** Calls a boolean hook at the start of the method; if it returns true, the method returns false (0). */
        HEAD_CANCEL_FALSE,
        /** Calls an Object-returning hook at method entry; returns its value if non-null. */
        HEAD_RETURN_IF_NONNULL,
        /** Calls a void hook before every return. */
        TAIL_CALL,
        /** For boolean methods: calls a hook before every IRETURN with (result, this, arguments). */
        TAIL_PASS_BOOL
    }

    public static final class Hook {
        final String owner, name, desc, hookOwner, hookName;
        final Mode mode;

        public Hook(String owner, String name, String desc, String hookOwner, String hookName, Mode mode) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
            this.hookOwner = hookOwner;
            this.hookName = hookName;
            this.mode = mode;
        }
    }

    /** A custom ClassNode transformation. */
    public interface Custom {
        /** @return true if the class was changed */
        boolean apply(ClassNode cn);
    }

    /**
     * Replaces "invokevirtual callOwner.callName(callDesc)" calls inside owner.method(desc)
     * with "invokestatic hookOwner.hookName" (the hook receives the object as its first argument).
     */
    public static Custom redirect(final String method, final String desc, final String callOwner,
                                  final String callName, final String callDesc, final String hookOwner,
                                  final String hookName) {
        return new Custom() {
            public boolean apply(ClassNode cn) {
                boolean done = false;
                for (MethodNode m : cn.methods) {
                    if (!m.name.equals(method) || !m.desc.equals(desc)) {
                        continue;
                    }
                    String hd = "(L" + callOwner + ";" + callDesc.substring(1);
                    for (AbstractInsnNode n = m.instructions.getFirst(); n != null; n = n.getNext()) {
                        if (n.getOpcode() == Opcodes.INVOKEVIRTUAL || n.getOpcode() == Opcodes.INVOKESTATIC) {
                            MethodInsnNode mi = (MethodInsnNode) n;
                            // Already redirected earlier (e.g. by the client patcher in the patched jar)
                            boolean prior = n.getOpcode() == Opcodes.INVOKESTATIC && mi.name.equals(hookName)
                                    && mi.desc.equals(hd) && !mi.owner.equals(hookOwner);
                            boolean orig = n.getOpcode() == Opcodes.INVOKEVIRTUAL && mi.owner.equals(callOwner)
                                    && mi.name.equals(callName) && mi.desc.equals(callDesc);
                            if (prior || orig) {
                                MethodInsnNode r = new MethodInsnNode(Opcodes.INVOKESTATIC, hookOwner, hookName, hd, false);
                                m.instructions.set(n, r);
                                n = r;
                                done = true;
                            }
                        }
                    }
                }
                return done;
            }
        };
    }

    /**
     * jw.a(eq,int) (removes an entity from a chunk): skips the line that prints
     * "There's no such entity to remove" when the entity is not in the list. That line was printed every tick.
     */
    public static Custom silenceMissingEntity() {
        return new Custom() {
            public boolean apply(ClassNode cn) {
                for (MethodNode m : cn.methods) {
                    if (!m.name.equals("a") || !m.desc.equals("(Leq;I)V")) {
                        continue;
                    }
                    for (AbstractInsnNode n = m.instructions.getFirst(); n != null; n = n.getNext()) {
                        if (n.getOpcode() == Opcodes.INVOKEINTERFACE && ((MethodInsnNode) n).name.equals("contains")
                                && n.getNext() != null && n.getNext().getOpcode() == Opcodes.POP
                                && n.getNext().getNext() instanceof JumpInsnNode
                                && n.getNext().getNext().getOpcode() == Opcodes.GOTO) {
                            return true; // already applied in a patched jar
                        }
                        if (n.getOpcode() == Opcodes.INVOKEINTERFACE && n.getNext() instanceof JumpInsnNode
                                && n.getNext().getOpcode() == Opcodes.IFNE
                                && ((MethodInsnNode) n).name.equals("contains")) {
                            JumpInsnNode j = (JumpInsnNode) n.getNext();
                            m.instructions.insertBefore(j, new InsnNode(Opcodes.POP));
                            j.setOpcode(Opcodes.GOTO);
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }

    /**
     * The old game's chunk-render comparators (a(dl,dl)) do not return 0 for equal distances. Java 7+ TimSort rejects
     * them with "Comparison method violates its general contract" and crashes the game. The FCMPG+IFGE+ICONST+GOTO+ICONST
     * sequence is replaced with a call to a helper that returns 0 for equal values.
     */
    public static Custom fixDistanceComparator(final String helperOwner, final String helperName) {
        return new Custom() {
            public boolean apply(ClassNode cn) {
                for (MethodNode m : cn.methods) {
                    if (!m.name.equals("a") || !m.desc.endsWith(")I") || !m.desc.startsWith("(L")) {
                        continue;
                    }
                    for (AbstractInsnNode n = m.instructions.getFirst(); n != null; n = n.getNext()) {
                        if (n.getOpcode() == Opcodes.INVOKESTATIC && ((MethodInsnNode) n).name.equals(helperName)) {
                            return true; // already applied
                        }
                        if (n.getOpcode() != Opcodes.FCMPG) {
                            continue;
                        }
                        AbstractInsnNode j = n.getNext();
                        if (!(j instanceof JumpInsnNode) || j.getOpcode() != Opcodes.IFGE) {
                            continue;
                        }
                        AbstractInsnNode c1 = j.getNext();
                        AbstractInsnNode g = c1 == null ? null : c1.getNext();
                        AbstractInsnNode l2 = g == null ? null : g.getNext();
                        AbstractInsnNode c2 = l2 == null ? null : l2.getNext();
                        if (c1 == null || g == null || c2 == null || g.getOpcode() != Opcodes.GOTO) {
                            continue;
                        }
                        // The first constant after IFGE is the value returned when "d1 < d2"
                        boolean ltIsPlusOne = c1.getOpcode() == Opcodes.ICONST_1;
                        m.instructions.insertBefore(n, new InsnNode(ltIsPlusOne ? Opcodes.ICONST_1 : Opcodes.ICONST_M1));
                        m.instructions.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, helperOwner, helperName, "(FFI)I", false));
                        m.instructions.remove(j);
                        m.instructions.remove(c1);
                        m.instructions.remove(g);
                        m.instructions.remove(c2);
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private final List<Hook> hooks = new ArrayList<Hook>();
    private final List<Custom> customs = new ArrayList<Custom>();
    private final List<String> customOwners = new ArrayList<String>();
    private final List<String> applied = new ArrayList<String>();
    private boolean publicize = true;

    public HookTransformer add(Hook h) {
        hooks.add(h);
        return this;
    }

    public HookTransformer custom(String owner, Custom c) {
        customOwners.add(owner);
        customs.add(c);
        return this;
    }

    public HookTransformer setPublicize(boolean p) {
        this.publicize = p;
        return this;
    }

    public List<String> getApplied() {
        return applied;
    }

    /** Checks that every hook in the list was applied. */
    public List<String> missing() {
        List<String> miss = new ArrayList<String>();
        for (Hook h : hooks) {
            String k = h.owner + "." + h.name + h.desc + "->" + h.hookName;
            if (!applied.contains(k)) {
                miss.add(k);
            }
        }
        for (int i = 0; i < customs.size(); i++) {
            String k = "custom:" + customOwners.get(i) + "#" + i;
            if (!applied.contains(k)) {
                miss.add(k);
            }
        }
        return miss;
    }

    /** Whether the class is an obfuscated game class (no package). */
    public static boolean isGameClass(String internalName) {
        return internalName.indexOf('/') < 0 && !internalName.startsWith("TW");
    }

    public byte[] transform(String internalName, byte[] bytes) {
        boolean game = isGameClass(internalName);
        boolean interesting = game && publicize;
        for (Hook h : hooks) {
            if (h.owner.equals(internalName)) {
                interesting = true;
            }
        }
        if (customOwners.contains(internalName)) {
            interesting = true;
        }
        if (!interesting) {
            return bytes;
        }
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, 0);

        if (game && publicize) {
            cn.access = (cn.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
            for (FieldNode f : cn.fields) {
                f.access = (f.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
            }
        }

        for (Hook h : hooks) {
            if (!h.owner.equals(internalName)) {
                continue;
            }
            for (MethodNode m : cn.methods) {
                if (m.name.equals(h.name) && m.desc.equals(h.desc)) {
                    apply(cn, m, h);
                    applied.add(h.owner + "." + h.name + h.desc + "->" + h.hookName);
                }
            }
        }
        for (int i = 0; i < customs.size(); i++) {
            if (customOwners.get(i).equals(internalName)) {
                if (customs.get(i).apply(cn)) {
                    applied.add("custom:" + internalName + "#" + i);
                }
            }
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    // ------------------------------------------------------------------

    private static void apply(ClassNode cn, MethodNode m, Hook h) {
        boolean isStatic = (m.access & Opcodes.ACC_STATIC) != 0;
        Type[] args = Type.getArgumentTypes(m.desc);
        StringBuilder hookArgs = new StringBuilder("(");
        if (h.mode == Mode.TAIL_PASS_BOOL) {
            hookArgs.append('Z');
        }
        if (!isStatic) {
            hookArgs.append('L').append(cn.name).append(';');
        }
        for (Type t : args) {
            hookArgs.append(t.getDescriptor());
        }
        hookArgs.append(')');

        switch (h.mode) {
            case HEAD_CALL: {
                InsnList il = loadArgs(isStatic, args);
                il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, h.hookOwner, h.hookName, hookArgs + "V", false));
                m.instructions.insert(il);
                break;
            }
            case HEAD_CANCEL_VOID:
            case HEAD_CANCEL_FALSE: {
                InsnList il = loadArgs(isStatic, args);
                il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, h.hookOwner, h.hookName, hookArgs + "Z", false));
                LabelNode cont = new LabelNode();
                il.add(new JumpInsnNode(Opcodes.IFEQ, cont));
                if (h.mode == Mode.HEAD_CANCEL_VOID) {
                    il.add(new InsnNode(Opcodes.RETURN));
                } else {
                    il.add(new InsnNode(Opcodes.ICONST_0));
                    il.add(new InsnNode(Opcodes.IRETURN));
                }
                il.add(cont);
                m.instructions.insert(il);
                break;
            }
            case HEAD_RETURN_IF_NONNULL: {
                Type ret = Type.getReturnType(m.desc);
                InsnList il = loadArgs(isStatic, args);
                il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, h.hookOwner, h.hookName, hookArgs + ret.getDescriptor(), false));
                LabelNode cont = new LabelNode();
                il.add(new InsnNode(Opcodes.DUP));
                il.add(new JumpInsnNode(Opcodes.IFNULL, cont));
                il.add(new InsnNode(Opcodes.ARETURN));
                il.add(cont);
                il.add(new InsnNode(Opcodes.POP));
                m.instructions.insert(il);
                break;
            }
            case TAIL_CALL:
            case TAIL_PASS_BOOL: {
                List<AbstractInsnNode> rets = new ArrayList<AbstractInsnNode>();
                for (AbstractInsnNode n = m.instructions.getFirst(); n != null; n = n.getNext()) {
                    int op = n.getOpcode();
                    if (op >= Opcodes.IRETURN && op <= Opcodes.RETURN) {
                        rets.add(n);
                    }
                }
                for (AbstractInsnNode r : rets) {
                    InsnList il = new InsnList();
                    if (h.mode == Mode.TAIL_PASS_BOOL) {
                        il.add(new InsnNode(Opcodes.DUP));
                    }
                    il.add(loadArgs(isStatic, args));
                    il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, h.hookOwner, h.hookName, hookArgs + "V", false));
                    m.instructions.insertBefore(r, il);
                }
                break;
            }
            default:
                throw new IllegalStateException();
        }
    }

    private static InsnList loadArgs(boolean isStatic, Type[] args) {
        InsnList il = new InsnList();
        int idx = 0;
        if (!isStatic) {
            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
            idx = 1;
        }
        for (Type t : args) {
            il.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), idx));
            idx += t.getSize();
        }
        return il;
    }
}
