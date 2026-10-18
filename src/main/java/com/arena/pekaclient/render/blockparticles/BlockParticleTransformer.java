package com.arena.pekaclient.render.blockparticles;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Narrow, fail-closed coremod transformer for the one vanilla particle-add path.
 *
 * <p>Only EffectRenderer.addEffect(EntityFX) is considered, using the explicit 1.8.9 MCP/SRG names and the notch
 * representation as a launcher fallback. The injected predicate returns only for EntityDiggingFX when the QoL
 * toggle is on; all other particle additions and any unrecognised class/method are left byte-for-byte vanilla.
 */
public final class BlockParticleTransformer implements IClassTransformer {

    private static final String EFFECT_RENDERER_MCP = "net.minecraft.client.particle.EffectRenderer";
    private static final String EFFECT_RENDERER_NOTCH = "bec";
    private static final String ADD_EFFECT_MCP = "addEffect";
    private static final String ADD_EFFECT_SRG = "func_78873_a";
    private static final String ADD_EFFECT_NOTCH = "a";
    private static final String ADD_EFFECT_MCP_DESC = "(Lnet/minecraft/client/particle/EntityFX;)V";
    private static final String ADD_EFFECT_NOTCH_DESC = "(Lbeb;)V";
    private static final String HOOK_OWNER = "com/arena/pekaclient/render/blockparticles/BlockParticleHooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !isEffectRenderer(name, transformedName)) {
            return basicClass;
        }
        BlockParticleHookStatus.noteEffectRendererClass(name == null ? transformedName : name);
        try {
            ClassReader reader = new ClassReader(basicClass);
            // The injected early return has a branch. COMPUTE_FRAMES keeps it verifier-safe on both 1.8.9 mappings.
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            EffectRendererVisitor visitor = new EffectRendererVisitor(writer);
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
            return visitor.changed ? writer.toByteArray() : basicClass;
        } catch (Throwable error) {
            BlockParticleHookStatus.fail("EffectRenderer transformer error: " + error.getClass().getSimpleName());
            return basicClass;
        }
    }

    private static boolean isEffectRenderer(String name, String transformedName) {
        return EFFECT_RENDERER_MCP.equals(name) || EFFECT_RENDERER_MCP.equals(transformedName)
                || EFFECT_RENDERER_NOTCH.equals(name) || EFFECT_RENDERER_NOTCH.equals(transformedName);
    }

    private static boolean isAddEffect(String name, String descriptor) {
        boolean supportedName = ADD_EFFECT_MCP.equals(name) || ADD_EFFECT_SRG.equals(name)
                || ADD_EFFECT_NOTCH.equals(name);
        return supportedName && (ADD_EFFECT_MCP_DESC.equals(descriptor) || ADD_EFFECT_NOTCH_DESC.equals(descriptor));
    }

    private static String variant(String methodName) {
        if (ADD_EFFECT_MCP.equals(methodName)) return "MCP";
        if (ADD_EFFECT_SRG.equals(methodName)) return "SRG";
        return "notch";
    }

    private static final class EffectRendererVisitor extends ClassVisitor {
        private boolean targetSeen;
        private boolean injected;
        private boolean changed;

        EffectRendererVisitor(ClassVisitor parent) {
            super(Opcodes.ASM5, parent);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (!isAddEffect(name, descriptor)) {
                return parent;
            }
            targetSeen = true;
            BlockParticleHookStatus.noteAddEffectTarget(variant(name));
            return new AddEffectVisitor(parent, this);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            if (!targetSeen) {
                BlockParticleHookStatus.fail("EffectRenderer addEffect(EntityFX) target not found (MCP/SRG/notch)");
            } else if (!injected) {
                BlockParticleHookStatus.fail("EffectRenderer addEffect(EntityFX) predicate was not injected");
            } else {
                BlockParticleHookStatus.noteInjection();
            }
        }
    }

    /** Inserts: if (BlockParticleHooks.shouldSuppress(effect)) return; at the method's exact entry. */
    private static final class AddEffectVisitor extends MethodVisitor {
        private final EffectRendererVisitor owner;
        private boolean injected;

        AddEffectVisitor(MethodVisitor parent, EffectRendererVisitor owner) {
            super(Opcodes.ASM5, parent);
            this.owner = owner;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            Label continueVanilla = new Label();
            super.visitVarInsn(Opcodes.ALOAD, 1);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, "shouldSuppress", "(Ljava/lang/Object;)Z", false);
            super.visitJumpInsn(Opcodes.IFEQ, continueVanilla);
            super.visitInsn(Opcodes.RETURN);
            super.visitLabel(continueVanilla);
            injected = true;
            owner.injected = true;
            owner.changed = true;
        }
    }
}
