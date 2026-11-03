package com.arena.pekaclient.assist.visual;

import com.arena.pekaclient.config.AssistSettings;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Bytecode-level regression checks for the clean-room tooltip and one-time local hurt-camera amplitude transformer. */
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
                hurtCameraClass("net/minecraft/client/renderer/EntityRenderer", "hurtCameraEffect"));
        check("hurt-camera amplitude hook emitted exactly once", calls(hurt,
                "com/arena/pekaclient/assist/visual/VisualAssistHooks", "scaleHurtCameraTilt") == 1);
        check("hurt-camera status active", VisualAssistHookStatus.isHurtCameraActive());
        check("no early-return suppression hook remains", calls(hurt,
                "com/arena/pekaclient/assist/visual/VisualAssistHooks", "suppressHurtCamera") == 0);
        byte[] hurtSrg = transformer.transform("net.minecraft.client.renderer.EntityRenderer",
                "net.minecraft.client.renderer.EntityRenderer",
                hurtCameraClass("net/minecraft/client/renderer/EntityRenderer", "func_78482_e"));
        byte[] hurtNotch = transformer.transform("bfk", "bfk", hurtCameraClass("bfk", "e"));
        check("hurt-camera SRG and notch amplitude hooks emitted",
                calls(hurtSrg, "com/arena/pekaclient/assist/visual/VisualAssistHooks", "scaleHurtCameraTilt") == 1
                        && calls(hurtNotch, "com/arena/pekaclient/assist/visual/VisualAssistHooks", "scaleHurtCameraTilt") == 1);
        byte[] unsupportedHurt = simpleClass("net/minecraft/client/renderer/EntityRenderer", "other", "(F)V");
        check("unknown hurt-camera shape stays untouched", transformer.transform(
                "net.minecraft.client.renderer.EntityRenderer", "net.minecraft.client.renderer.EntityRenderer", unsupportedHurt)
                == unsupportedHurt);
        byte[] noAmplitude = simpleClass("net/minecraft/client/renderer/EntityRenderer", "hurtCameraEffect", "(F)V");
        check("missing vanilla tilt amplitude stays untouched", transformer.transform(
                "net.minecraft.client.renderer.EntityRenderer", "net.minecraft.client.renderer.EntityRenderer", noAmplitude)
                == noAmplitude);

        AssistSettings.hurtCameraShake = 0;
        check("zero intensity scales the tilt to zero", VisualAssistHooks.scaleHurtCameraTilt(14f) == 0f);
        AssistSettings.hurtCameraShake = 55;
        check("intermediate intensity has one proportional scale", Math.abs(VisualAssistHooks.scaleHurtCameraTilt(14f) - 7.7f) < .00001f);
        AssistSettings.hurtCameraShake = 100;
        check("one hundred intensity preserves vanilla amplitude", VisualAssistHooks.scaleHurtCameraTilt(14f) == 14f);

        System.out.println("visual assist transformer: passed " + passed + ", failed " + failed);
        if (failed != 0) throw new AssertionError(failed + " visual-assist transformer checks failed");
    }

    private static byte[] simpleClass(String owner, String method, String descriptor) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);
        constructor(writer);
        MethodVisitor target = writer.visitMethod(Opcodes.ACC_PUBLIC, method, descriptor, null, null);
        target.visitCode();
        target.visitInsn(Opcodes.RETURN);
        target.visitMaxs(0, 0);
        target.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    /** Fixture mirrors the one 14F operand that vanilla uses for its ordinary hurt-camera roll. */
    private static byte[] hurtCameraClass(String owner, String method) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);
        constructor(writer);
        MethodVisitor target = writer.visitMethod(Opcodes.ACC_PUBLIC, method, "(F)V", null, null);
        target.visitCode();
        target.visitVarInsn(Opcodes.FLOAD, 1);
        target.visitLdcInsn(Float.valueOf(14.0F));
        target.visitInsn(Opcodes.FMUL);
        target.visitInsn(Opcodes.POP);
        target.visitInsn(Opcodes.RETURN);
        target.visitMaxs(0, 0);
        target.visitEnd();
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
