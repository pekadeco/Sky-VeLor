package com.arena.pekaclient.itemlockswap;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Fail-closed transformer for exactly two client-authoritative actions:
 * PlayerControllerMP.windowClick before it emits a click packet, and EntityPlayerSP.dropOneItem for Q/Ctrl+Q.
 * It accepts MCP, SRG and obfuscated target names/descriptors without touching unrelated overloads.
 */
public final class ItemLockSwapTransformer implements IClassTransformer {

    private static final String CONTROLLER_MCP = "net.minecraft.client.multiplayer.PlayerControllerMP";
    private static final String CONTROLLER_NOTCH = "bda";
    private static final String PLAYER_MCP = "net.minecraft.client.entity.EntityPlayerSP";
    private static final String PLAYER_NOTCH = "bew";

    private static final String WINDOW_CLICK_MCP = "windowClick";
    private static final String WINDOW_CLICK_SRG = "func_78753_a";
    private static final String DROP_ONE_MCP = "dropOneItem";
    private static final String DROP_ONE_SRG = "func_71040_bB";
    private static final String NOTCH = "a";

    private static final String WINDOW_CLICK_MCP_DESC =
            "(IIIILnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;";
    private static final String DROP_ONE_MCP_DESC = "(Z)Lnet/minecraft/entity/item/EntityItem;";
    private static final String HOOK_OWNER = "com/arena/pekaclient/itemlockswap/ItemLockSwapHooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        if (isController(name, transformedName)) return transformController(name, transformedName, basicClass);
        if (isPlayer(name, transformedName)) return transformPlayer(name, transformedName, basicClass);
        return basicClass;
    }

    private static byte[] transformController(String name, String transformedName, byte[] bytes) {
        ItemLockSwapHookStatus.noteControllerClass(name == null ? transformedName : name);
        try {
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ControllerVisitor visitor = new ControllerVisitor(writer);
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
            return visitor.changed ? writer.toByteArray() : bytes;
        } catch (Throwable error) {
            ItemLockSwapHookStatus.fail("PlayerControllerMP transformer error: " + error.getClass().getSimpleName());
            return bytes;
        }
    }

    private static byte[] transformPlayer(String name, String transformedName, byte[] bytes) {
        ItemLockSwapHookStatus.notePlayerClass(name == null ? transformedName : name);
        try {
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            PlayerVisitor visitor = new PlayerVisitor(writer);
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
            return visitor.changed ? writer.toByteArray() : bytes;
        } catch (Throwable error) {
            ItemLockSwapHookStatus.fail("EntityPlayerSP transformer error: " + error.getClass().getSimpleName());
            return bytes;
        }
    }

    private static boolean isController(String name, String transformedName) {
        return CONTROLLER_MCP.equals(name) || CONTROLLER_MCP.equals(transformedName)
                || CONTROLLER_NOTCH.equals(name) || CONTROLLER_NOTCH.equals(transformedName);
    }

    private static boolean isPlayer(String name, String transformedName) {
        return PLAYER_MCP.equals(name) || PLAYER_MCP.equals(transformedName)
                || PLAYER_NOTCH.equals(name) || PLAYER_NOTCH.equals(transformedName);
    }

    private static boolean isWindowClick(String methodName, String descriptor) {
        if (WINDOW_CLICK_MCP_DESC.equals(descriptor)) {
            return WINDOW_CLICK_MCP.equals(methodName) || WINDOW_CLICK_SRG.equals(methodName) || NOTCH.equals(methodName);
        }
        // Obfuscated EntityPlayer/ItemStack internal names vary by launcher, but this full primitive/object shape is
        // unique to PlayerControllerMP.windowClick in 1.8.9. Keeping it scoped to target class and name is fail-closed.
        int close = descriptor == null ? -1 : descriptor.indexOf(')');
        return (NOTCH.equals(methodName) || WINDOW_CLICK_SRG.equals(methodName)) && descriptor != null
                && descriptor.startsWith("(IIIIL") && close > 6 && descriptor.charAt(close - 1) == ';'
                && descriptor.startsWith(")L", close) && descriptor.endsWith(";");
    }

    private static boolean isDropOne(String methodName, String descriptor) {
        if (DROP_ONE_MCP_DESC.equals(descriptor)) {
            return DROP_ONE_MCP.equals(methodName) || DROP_ONE_SRG.equals(methodName) || NOTCH.equals(methodName);
        }
        // The obfuscated return object's concrete internal name is deliberately not assumed.
        return (NOTCH.equals(methodName) || DROP_ONE_SRG.equals(methodName)) && descriptor != null
                && descriptor.startsWith("(Z)L") && descriptor.endsWith(";");
    }

    private static String variant(String name) {
        if (WINDOW_CLICK_MCP.equals(name) || DROP_ONE_MCP.equals(name)) return "MCP";
        if (WINDOW_CLICK_SRG.equals(name) || DROP_ONE_SRG.equals(name)) return "SRG";
        return "notch";
    }

    private static final class ControllerVisitor extends ClassVisitor {
        private boolean targetSeen;
        private boolean injected;
        private boolean changed;

        ControllerVisitor(ClassVisitor parent) {
            super(Opcodes.ASM5, parent);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (!isWindowClick(name, descriptor)) return parent;
            targetSeen = true;
            ItemLockSwapHookStatus.noteWindowClickTarget(variant(name));
            return new WindowClickVisitor(parent, this);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            if (!targetSeen) {
                ItemLockSwapHookStatus.fail("PlayerControllerMP windowClick target not found (MCP/SRG/notch)");
            } else if (!injected) {
                ItemLockSwapHookStatus.fail("PlayerControllerMP windowClick guard was not injected");
            } else {
                ItemLockSwapHookStatus.noteWindowClickInjected();
            }
        }
    }

    private static final class PlayerVisitor extends ClassVisitor {
        private boolean targetSeen;
        private boolean injected;
        private boolean changed;

        PlayerVisitor(ClassVisitor parent) {
            super(Opcodes.ASM5, parent);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (!isDropOne(name, descriptor)) return parent;
            targetSeen = true;
            ItemLockSwapHookStatus.noteDropTarget(variant(name));
            return new DropVisitor(parent, this);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            if (!targetSeen) {
                ItemLockSwapHookStatus.fail("EntityPlayerSP dropOneItem(boolean) target not found (MCP/SRG/notch)");
            } else if (!injected) {
                ItemLockSwapHookStatus.fail("EntityPlayerSP dropOneItem guard was not injected");
            } else {
                ItemLockSwapHookStatus.noteDropInjected();
            }
        }
    }

    /** Inserts an early null return before the vanilla method can construct/send a click packet. */
    private static final class WindowClickVisitor extends MethodVisitor {
        private final ControllerVisitor owner;

        WindowClickVisitor(MethodVisitor parent, ControllerVisitor owner) {
            super(Opcodes.ASM5, parent);
            this.owner = owner;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            Label continueVanilla = new Label();
            super.visitVarInsn(Opcodes.ILOAD, 1);
            super.visitVarInsn(Opcodes.ILOAD, 2);
            super.visitVarInsn(Opcodes.ILOAD, 3);
            super.visitVarInsn(Opcodes.ILOAD, 4);
            super.visitVarInsn(Opcodes.ALOAD, 5);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, "shouldBlockWindowClick",
                    "(IIIILjava/lang/Object;)Z", false);
            super.visitJumpInsn(Opcodes.IFEQ, continueVanilla);
            super.visitInsn(Opcodes.ACONST_NULL);
            super.visitInsn(Opcodes.ARETURN);
            super.visitLabel(continueVanilla);
            owner.injected = true;
            owner.changed = true;
        }
    }

    /** Inserts an early null return before EntityPlayerSP creates either a one-item or Ctrl+Q drop packet. */
    private static final class DropVisitor extends MethodVisitor {
        private final PlayerVisitor owner;

        DropVisitor(MethodVisitor parent, PlayerVisitor owner) {
            super(Opcodes.ASM5, parent);
            this.owner = owner;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            Label continueVanilla = new Label();
            super.visitVarInsn(Opcodes.ALOAD, 0);
            super.visitVarInsn(Opcodes.ILOAD, 1);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, "shouldBlockHotbarDrop",
                    "(Ljava/lang/Object;Z)Z", false);
            super.visitJumpInsn(Opcodes.IFEQ, continueVanilla);
            super.visitInsn(Opcodes.ACONST_NULL);
            super.visitInsn(Opcodes.ARETURN);
            super.visitLabel(continueVanilla);
            owner.injected = true;
            owner.changed = true;
        }
    }
}
