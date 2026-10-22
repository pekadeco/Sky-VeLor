package com.arena.pekaclient.assist.visual;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Bytecode-level regression checks for the clean-room tooltip and local hurt-camera transformer. */
public final class VisualAssistTransformerTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        VisualAssistTransformer transformer = new VisualAssistTransformer();
        byte[] foreign = simpleClass("example/Other", "noop", "()V");
        check("foreign class unchanged", transformer.transform("example.Other", "example.Other", foreign) == foreign);

        byte[] tooltip = transformer.transform("net.minecraft.client.gui.GuiScreen", "net.minecraft.client.gui.GuiScreen",
                simpleClass("net/minecraft/client/gui/GuiScreen", "drawHoveringText", "(Ljava/util/List;II)V"));
        check("tooltip hook emitted", calls(tooltip, "com/arena/pekaclient/assist/inventory/InventoryAssistHooks",
                "prepareTooltip") == 1);
        check("tooltip status active", VisualAssistHookStatus.isTooltipActive());

        byte[] hurt = transformer.transform("net.minecraft.client.renderer.EntityRenderer",
                "net.minecraft.client.renderer.EntityRenderer",
                simpleClass("net/minecraft/client/renderer/EntityRenderer", "hurtCameraEffect", "(F)V"));
        check("hurt-camera predicate emitted", calls(hurt, "com/arena/pekaclient/assist/visual/VisualAssistHooks",
                "suppressHurtCamera") == 1);
        check("hurt-camera status active", VisualAssistHookStatus.isHurtCameraActive());

        System.out.println("visual assist transformer: passed " + passed + ", failed " + failed);
        if (failed != 0) throw new AssertionError(failed + " visual-assist transformer checks failed");
    }

    private static byte[] simpleClass(String owner, String method, String descriptor) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);
        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();
        MethodVisitor target = writer.visitMethod(Opcodes.ACC_PUBLIC, method, descriptor, null, null);
        target.visitCode();
        target.visitInsn(Opcodes.RETURN);
        target.visitMaxs(0, 0);
        target.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static int calls(byte[] bytes, final String expectedOwner, final String expectedMethod) {
        final int[] count = {0};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                             String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM5) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method, String desc, boolean itf) {
                        if (opcode == Opcodes.INVOKESTATIC && expectedOwner.equals(owner) && expectedMethod.equals(method)) {
                            count[0]++;
                        }
                    }
                };
            }
        }, 0);
        return count[0];
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.err.println("FAIL " + name); }
    }
}
