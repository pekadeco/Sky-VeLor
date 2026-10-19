package com.arena.pekaclient.itemlockswap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Bytecode proof for the pre-packet window-click and Q/Ctrl+Q drop guards. */
public final class ItemLockSwapTransformerTests {

    private static final String CLICK_DESC =
            "(IIIILnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;";
    private static final String DROP_DESC = "(Z)Lnet/minecraft/entity/item/EntityItem;";
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        ItemLockSwapTransformer transformer = new ItemLockSwapTransformer();
        byte[] controller = controllerClass("windowClick", CLICK_DESC);
        byte[] foreign = transformer.transform("example.Other", "example.Other", controller);
        check("foreign class stays byte-identical", foreign == controller);
        byte[] protectedController = transformer.transform("net.minecraft.client.multiplayer.PlayerControllerMP",
                "net.minecraft.client.multiplayer.PlayerControllerMP", controller);
        check("MCP windowClick transforms", protectedController != controller);
        check("windowClick has exactly one pre-packet hook", hooks(protectedController, "shouldBlockWindowClick",
                "(IIIILjava/lang/Object;)Z") == 1);

        byte[] srgController = transformer.transform("bda", "net.minecraft.client.multiplayer.PlayerControllerMP",
                controllerClass("func_78753_a", CLICK_DESC));
        check("SRG windowClick transforms", hooks(srgController, "shouldBlockWindowClick",
                "(IIIILjava/lang/Object;)Z") == 1);
        byte[] notchController = transformer.transform("bda", "bda",
                controllerClass("a", "(IIIILyz;)Ladd;"));
        check("notch windowClick descriptor transforms", hooks(notchController, "shouldBlockWindowClick",
                "(IIIILjava/lang/Object;)Z") == 1);

        byte[] player = playerClass("dropOneItem", DROP_DESC);
        byte[] protectedPlayer = transformer.transform("net.minecraft.client.entity.EntityPlayerSP",
                "net.minecraft.client.entity.EntityPlayerSP", player);
        check("MCP dropOneItem transforms", protectedPlayer != player);
        check("drop hook receives Q/Ctrl-Q flag", hooks(protectedPlayer, "shouldBlockHotbarDrop",
                "(Ljava/lang/Object;Z)Z") == 1);
        byte[] notchPlayer = transformer.transform("bew", "bew", playerClass("a", "(Z)Lwv;"));
        check("notch dropOneItem descriptor transforms", hooks(notchPlayer, "shouldBlockHotbarDrop",
                "(Ljava/lang/Object;Z)Z") == 1);
        check("both hook diagnostics are active", ItemLockSwapHookStatus.isActive());

        System.out.println("item lock/swap transformer: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("item lock/swap transformer failures: " + failed);
    }

    private static byte[] controllerClass(String methodName, String descriptor) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, "net/minecraft/client/multiplayer/PlayerControllerMP", null,
                "java/lang/Object", null);
        constructor(writer);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, methodName, descriptor, null, null);
        method.visitCode();
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] playerClass(String methodName, String descriptor) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, "net/minecraft/client/entity/EntityPlayerSP", null,
                "java/lang/Object", null);
        constructor(writer);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, methodName, descriptor, null, null);
        method.visitCode();
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void constructor(ClassWriter writer) {
        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();
    }

    private static int hooks(byte[] bytes, final String wantedName, final String wantedDescriptor) {
        final int[] count = {0};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                             String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM5) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method, String desc, boolean itf) {
                        if (opcode == Opcodes.INVOKESTATIC
                                && "com/arena/pekaclient/itemlockswap/ItemLockSwapHooks".equals(owner)
                                && wantedName.equals(method) && wantedDescriptor.equals(desc)) {
                            count[0]++;
                        }
                    }
                };
            }
        }, 0);
        return count[0];
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.err.println("FAIL " + name);
        }
    }
}
