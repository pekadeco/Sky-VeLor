package com.arena.pekaclient.render.blockparticles;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Bytecode-level proof that the coremod has one guarded MCP addEffect injection and leaves foreign classes alone. */
public final class BlockParticleTransformerTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        byte[] source = effectRendererClass();
        BlockParticleTransformer transformer = new BlockParticleTransformer();
        byte[] untouched = transformer.transform("example.Other", "example.Other", source);
        check("foreign class is returned unchanged", untouched == source);

        byte[] transformed = transformer.transform("net.minecraft.client.particle.EffectRenderer",
                "net.minecraft.client.particle.EffectRenderer", source);
        check("MCP EffectRenderer is transformed", transformed != source);
        check("addEffect receives exactly one block predicate", hookCalls(transformed) == 1);
        check("transformer diagnostic is active", BlockParticleHookStatus.isActive());

        System.out.println();
        System.out.println("block particle transformer: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static byte[] effectRendererClass() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, "net/minecraft/client/particle/EffectRenderer", null,
                "java/lang/Object", null);
        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();
        MethodVisitor add = writer.visitMethod(Opcodes.ACC_PUBLIC, "addEffect",
                "(Lnet/minecraft/client/particle/EntityFX;)V", null, null);
        add.visitCode();
        add.visitInsn(Opcodes.RETURN);
        add.visitMaxs(0, 0);
        add.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static int hookCalls(byte[] bytes) {
        final int[] calls = {0};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                             String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM5) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method, String desc,
                                                boolean itf) {
                        if (opcode == Opcodes.INVOKESTATIC
                                && "com/arena/pekaclient/render/blockparticles/BlockParticleHooks".equals(owner)
                                && "shouldSuppress".equals(method)
                                && "(Ljava/lang/Object;)Z".equals(desc)) {
                            calls[0]++;
                        }
                    }
                };
            }
        }, 0);
        return calls[0];
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + name); }
    }
}
