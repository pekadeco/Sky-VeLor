package com.arena.pekaclient.assist.visual;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Two deliberately small, fail-closed 1.8.9 hooks: one local tooltip-list preparation point and one multiplier at
 * vanilla's local hurt-camera tilt amplitude. Every unfamiliar class or method returns untouched vanilla bytecode.
 */
public final class VisualAssistTransformer implements IClassTransformer {

    private static final String GUI_SCREEN_MCP = "net.minecraft.client.gui.GuiScreen";
    private static final String GUI_SCREEN_NOTCH = "axu";
    private static final String ENTITY_RENDERER_MCP = "net.minecraft.client.renderer.EntityRenderer";
    private static final String ENTITY_RENDERER_NOTCH = "bfk";
    private static final String TOOLTIP_DESC = "(Ljava/util/List;II)V";
    private static final String HURT_CAMERA_DESC = "(F)V";
    private static final String TOOLTIP_OWNER = "com/arena/pekaclient/assist/inventory/InventoryAssistHooks";
    private static final String HURT_OWNER = "com/arena/pekaclient/assist/visual/VisualAssistHooks";
    /** Vanilla 1.8.9 applies this factor only to the normal hurt-camera roll, never to its death roll. */
    private static final float VANILLA_HURT_TILT_AMPLITUDE = 14.0F;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        if (matches(name, transformedName, GUI_SCREEN_MCP, GUI_SCREEN_NOTCH)) return transformTooltips(basicClass);
        if (matches(name, transformedName, ENTITY_RENDERER_MCP, ENTITY_RENDERER_NOTCH)) return transformHurtCamera(basicClass);
        return basicClass;
    }

    private static byte[] transformTooltips(byte[] bytes) {
        try {
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            TooltipVisitor visitor = new TooltipVisitor(writer);
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
            if (visitor.injected) VisualAssistHookStatus.tooltipInjected(visitor.mapping);
            else VisualAssistHookStatus.tooltipUnavailable("GuiScreen drawHoveringText target not found");
            return visitor.changed ? writer.toByteArray() : bytes;
        } catch (Throwable failure) {
            VisualAssistHookStatus.tooltipUnavailable("GuiScreen tooltip transformer error: " + failure.getClass().getSimpleName());
            return bytes;
        }
    }

    private static byte[] transformHurtCamera(byte[] bytes) {
        try {
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            HurtCameraVisitor visitor = new HurtCameraVisitor(writer);
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
            if (visitor.injected) VisualAssistHookStatus.hurtCameraInjected(visitor.mapping);
            else VisualAssistHookStatus.hurtCameraUnavailable("EntityRenderer hurtCameraEffect amplitude target not found");
            return visitor.changed ? writer.toByteArray() : bytes;
        } catch (Throwable failure) {
            VisualAssistHookStatus.hurtCameraUnavailable("EntityRenderer transformer error: " + failure.getClass().getSimpleName());
            return bytes;
        }
    }

    /**
     * Forge's reobfuscation can rewrite a literal used as an ASM method name even though this is a Lunaria hook,
     * so construct each local hook name at runtime. The transformer itself runs once at class load, making these tiny
     * allocations irrelevant while preserving the public hook names in the production jar.
     */
    private static String tooltipHookMethod() {
        return new String(new char[]{'p', 'r', 'e', 'p', 'a', 'r', 'e', 'T', 'o', 'o', 'l', 't', 'i', 'p'});
    }

    private static String hurtCameraHookMethod() {
        return new String(new char[]{'s', 'c', 'a', 'l', 'e', 'H', 'u', 'r', 't', 'C', 'a', 'm', 'e', 'r', 'a', 'T', 'i', 'l', 't'});
    }

    private static boolean matches(String name, String transformedName, String mcp, String notch) {
        return mcp.equals(name) || mcp.equals(transformedName) || notch.equals(name) || notch.equals(transformedName);
    }

    private static boolean tooltipMethod(String name, String descriptor) {
        return TOOLTIP_DESC.equals(descriptor) && ("drawHoveringText".equals(name) || "func_146283_a".equals(name)
                || "a".equals(name));
    }

    private static boolean hurtMethod(String name, String descriptor) {
        return HURT_CAMERA_DESC.equals(descriptor) && ("hurtCameraEffect".equals(name) || "func_78482_e".equals(name)
                || "e".equals(name));
    }

    private static String mapping(String name) {
        if (name != null && name.startsWith("func_")) return "SRG";
        if ("a".equals(name) || "e".equals(name)) return "notch";
        return "MCP";
    }

    private static final class TooltipVisitor extends ClassVisitor {
        boolean injected;
        boolean changed;
        String mapping = "unknown";

        TooltipVisitor(ClassVisitor parent) {
            super(Opcodes.ASM5, parent);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (!tooltipMethod(name, descriptor)) return parent;
            mapping = mapping(name);
            return new MethodVisitor(Opcodes.ASM5, parent) {
                @Override
                public void visitCode() {
                    super.visitCode();
                    // this + vanilla's List parameter, preserving all x/y semantics and vanilla rendering.
                    super.visitVarInsn(Opcodes.ALOAD, 0);
                    super.visitVarInsn(Opcodes.ALOAD, 1);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, TOOLTIP_OWNER, tooltipHookMethod(),
                            "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
                    injected = true;
                    changed = true;
                }
            };
        }
    }

    private static final class HurtCameraVisitor extends ClassVisitor {
        boolean injected;
        boolean changed;
        String mapping = "unknown";

        HurtCameraVisitor(ClassVisitor parent) {
            super(Opcodes.ASM5, parent);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (!hurtMethod(name, descriptor)) return parent;
            mapping = mapping(name);
            return new MethodVisitor(Opcodes.ASM5, parent) {
                @Override
                public void visitLdcInsn(Object value) {
                    super.visitLdcInsn(value);
                    // In 1.8.9 this exact literal is multiplied into the regular hurt roll. It is not used by the
                    // death roll, so multiplying it is narrower than changing EntityLivingBase.hurtTime or returning
                    // early from hurtCameraEffect.
                    if (!injected && value instanceof Float
                            && Float.compare(((Float) value).floatValue(), VANILLA_HURT_TILT_AMPLITUDE) == 0) {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, HURT_OWNER, hurtCameraHookMethod(), "(F)F", false);
                        injected = true;
                        changed = true;
                    }
                }
            };
        }
    }
}
