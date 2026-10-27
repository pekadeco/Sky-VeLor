package com.arena.pekaclient.assist.visual;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Fail-closed 1.8.9 render-coordinate transformer for the temporary Aspect camera path.
 *
 * <p>It only replaces local render-coordinate values: EntityRenderer's {@code orientCamera} coordinates before
 * vanilla applies its camera transform, its world frustum origin, and RenderGlobal's terrain/entity renderer origins.
 * It also redirects Forge's existing world-last dispatch through a balanced GL translation so overlay renderers use
 * that same origin. This deliberately does not intercept a final GL translation. It does not transform NetHandler,
 * player entities, movement, input, or packet classes, and it never writes a player/camera coordinate.</p>
 *
 * <p>Method names differ across MCP/SRG/notch environments, so the target is the unique 1.8.9 descriptor shape and
 * an exact local-double-store layout. If either class does not match all expected stores, its original bytecode is
 * returned untouched and the runtime path remains disabled.</p>
 */
public final class AspectTeleportCameraTransformer implements IClassTransformer {
    private static final String ENTITY_RENDERER_MCP = "net.minecraft.client.renderer.EntityRenderer";
    private static final String ENTITY_RENDERER_NOTCH = "bfk";
    private static final String RENDER_GLOBAL_MCP = "net.minecraft.client.renderer.RenderGlobal";
    private static final String RENDER_GLOBAL_NOTCH = "bfr";
    private static final String ORIENT_CAMERA_MCP = "orientCamera";
    private static final String ORIENT_CAMERA_SRG = "func_78467_g";
    private static final String ORIENT_CAMERA_NOTCH = "g";
    private static final String ORIENT_CAMERA_DESC = "(F)V";
    private static final String FORGE_HOOKS = "net/minecraftforge/client/ForgeHooksClient";
    private static final String HOOK_OWNER = "com/arena/pekaclient/assist/visual/AspectTeleportCameraHooks";
    private static final String[] CAMERA_HOOKS = {"renderCameraX", "renderCameraY", "renderCameraZ"};
    private static final String[] ORIENT_CAMERA_HOOKS = {"orientCameraX", "orientCameraY", "orientCameraZ"};

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        if (matches(name, transformedName, ENTITY_RENDERER_MCP, ENTITY_RENDERER_NOTCH)) {
            return transformEntityRenderer(basicClass);
        }
        if (matches(name, transformedName, RENDER_GLOBAL_MCP, RENDER_GLOBAL_NOTCH)) {
            return transformRenderGlobal(basicClass);
        }
        return basicClass;
    }

    private static byte[] transformEntityRenderer(byte[] bytes) {
        try {
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            EntityRendererVisitor visitor = new EntityRendererVisitor(writer);
            reader.accept(visitor, 0);
            if (!visitor.worldPassSeen || visitor.worldPassCoordinateHooks != 3 || visitor.worldLastHooks != 1
                    || !visitor.orientCameraSeen || visitor.orientCameraCoordinateHooks != 3) {
                AspectTeleportCameraHookStatus.entityRendererUnavailable("EntityRenderer camera/world-pass target shape unavailable");
                return bytes;
            }
            AspectTeleportCameraHookStatus.entityRendererInjected();
            return writer.toByteArray();
        } catch (Throwable failure) {
            AspectTeleportCameraHookStatus.entityRendererUnavailable("EntityRenderer transformer error: "
                    + failure.getClass().getSimpleName());
            return bytes;
        }
    }

    private static byte[] transformRenderGlobal(byte[] bytes) {
        try {
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            RenderGlobalVisitor visitor = new RenderGlobalVisitor(writer);
            reader.accept(visitor, 0);
            if (!visitor.setupTerrainSeen || visitor.setupTerrainHooks != 3
                    || !visitor.renderEntitiesSeen || visitor.renderEntitiesHooks != 3) {
                AspectTeleportCameraHookStatus.renderGlobalUnavailable("RenderGlobal coordinate target shape unavailable");
                return bytes;
            }
            AspectTeleportCameraHookStatus.renderGlobalInjected();
            return writer.toByteArray();
        } catch (Throwable failure) {
            AspectTeleportCameraHookStatus.renderGlobalUnavailable("RenderGlobal transformer error: "
                    + failure.getClass().getSimpleName());
            return bytes;
        }
    }

    private static boolean matches(String name, String transformedName, String mcp, String notch) {
        return mcp.equals(name) || mcp.equals(transformedName) || notch.equals(name) || notch.equals(transformedName);
    }

    /** 1.8.9 EntityRenderer has one world pass with int, float, long arguments. */
    private static boolean isWorldPassDescriptor(String descriptor) {
        return "(IFJ)V".equals(descriptor);
    }

    /** The 1.8.9 camera orientation method has stable MCP/SRG/notch names and one float partial-ticks parameter. */
    private static boolean isOrientCameraMethod(String name, String descriptor) {
        return ORIENT_CAMERA_DESC.equals(descriptor) && (ORIENT_CAMERA_MCP.equals(name)
                || ORIENT_CAMERA_SRG.equals(name) || ORIENT_CAMERA_NOTCH.equals(name));
    }

    /** 1.8.9 RenderGlobal setupTerrain: Entity, double partial ticks, ICamera, int, boolean. */
    private static boolean isSetupTerrainDescriptor(String descriptor) {
        int afterEntity = afterReference(descriptor, 1);
        if (afterEntity < 0 || afterEntity >= descriptor.length() || descriptor.charAt(afterEntity) != 'D') return false;
        int afterCamera = afterReference(descriptor, afterEntity + 1);
        return afterCamera >= 0 && "IZ)V".equals(descriptor.substring(afterCamera));
    }

    /** 1.8.9 RenderGlobal renderEntities: Entity, ICamera, float. */
    private static boolean isRenderEntitiesDescriptor(String descriptor) {
        int afterEntity = afterReference(descriptor, 1);
        if (afterEntity < 0) return false;
        int afterCamera = afterReference(descriptor, afterEntity);
        return afterCamera >= 0 && "F)V".equals(descriptor.substring(afterCamera));
    }

    /** Existing stable Forge event call: RenderGlobal and partial ticks, identified before replacement with Object. */
    private static boolean isWorldLastDescriptor(String descriptor) {
        int afterGlobal = afterReference(descriptor, 1);
        return afterGlobal >= 0 && "F)V".equals(descriptor.substring(afterGlobal));
    }

    private static int afterReference(String descriptor, int at) {
        if (descriptor == null || at < 0 || at >= descriptor.length() || descriptor.charAt(at) != 'L') return -1;
        int end = descriptor.indexOf(';', at);
        return end < 0 ? -1 : end + 1;
    }

    private static final class EntityRendererVisitor extends ClassVisitor {
        boolean worldPassSeen;
        boolean orientCameraSeen;
        int worldPassCoordinateHooks;
        int orientCameraCoordinateHooks;
        int worldLastHooks;

        EntityRendererVisitor(ClassVisitor parent) {
            super(Opcodes.ASM5, parent);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (isWorldPassDescriptor(descriptor)) {
                worldPassSeen = true;
                return new EntityWorldPassVisitor(parent, this);
            }
            if (isOrientCameraMethod(name, descriptor)) {
                orientCameraSeen = true;
                return new OrientCameraVisitor(parent, this);
            }
            return parent;
        }
    }

    private static final class EntityWorldPassVisitor extends MethodVisitor {
        private final EntityRendererVisitor owner;
        private int doubleStoreOrdinal;
        private int localCoordinateHooks;
        private int localWorldLastHooks;

        EntityWorldPassVisitor(MethodVisitor parent, EntityRendererVisitor owner) {
            super(Opcodes.ASM5, parent);
            this.owner = owner;
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            super.visitVarInsn(opcode, var);
            if (opcode != Opcodes.DSTORE) return;
            if (doubleStoreOrdinal < 3) {
                emitCoordinateHook(doubleStoreOrdinal, var);
                localCoordinateHooks++;
            }
            doubleStoreOrdinal++;
        }

        private void emitCoordinateHook(int axis, int localVariable) {
            super.visitVarInsn(Opcodes.DLOAD, localVariable);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, CAMERA_HOOKS[axis], "(D)D", false);
            super.visitVarInsn(Opcodes.DSTORE, localVariable);
        }

        @Override
        public void visitMethodInsn(int opcode, String ownerName, String name, String descriptor, boolean itf) {
            if (opcode == Opcodes.INVOKESTATIC && FORGE_HOOKS.equals(ownerName)
                    && "dispatchRenderLast".equals(name) && isWorldLastDescriptor(descriptor)) {
                // Object avoids an injected RenderGlobal descriptor that would differ across MCP/SRG/notch launchers.
                super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, "dispatchRenderLast", "(Ljava/lang/Object;F)V", false);
                localWorldLastHooks++;
                return;
            }
            super.visitMethodInsn(opcode, ownerName, name, descriptor, itf);
        }

        @Override
        public void visitEnd() {
            owner.worldPassCoordinateHooks += localCoordinateHooks;
            owner.worldLastHooks += localWorldLastHooks;
            super.visitEnd();
        }
    }

    /** Modifies orientCamera's first d0/d1/d2 locals, before vanilla's own transform—not a final GL translation. */
    private static final class OrientCameraVisitor extends MethodVisitor {
        private final EntityRendererVisitor owner;
        private int doubleStoreOrdinal;
        private int localCoordinateHooks;

        OrientCameraVisitor(MethodVisitor parent, EntityRendererVisitor owner) {
            super(Opcodes.ASM5, parent);
            this.owner = owner;
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            super.visitVarInsn(opcode, var);
            if (opcode != Opcodes.DSTORE) return;
            if (doubleStoreOrdinal < 3) {
                super.visitVarInsn(Opcodes.DLOAD, var);
                if (doubleStoreOrdinal == 1) {
                    // d1 includes the eye-height offset, so the hook receives partial ticks to preserve that offset.
                    super.visitVarInsn(Opcodes.FLOAD, 1);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, ORIENT_CAMERA_HOOKS[1], "(DF)D", false);
                } else {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER,
                            ORIENT_CAMERA_HOOKS[doubleStoreOrdinal], "(D)D", false);
                }
                super.visitVarInsn(Opcodes.DSTORE, var);
                localCoordinateHooks++;
            }
            doubleStoreOrdinal++;
        }

        @Override
        public void visitEnd() {
            owner.orientCameraCoordinateHooks += localCoordinateHooks;
            super.visitEnd();
        }
    }

    private static final class RenderGlobalVisitor extends ClassVisitor {
        boolean setupTerrainSeen;
        boolean renderEntitiesSeen;
        int setupTerrainHooks;
        int renderEntitiesHooks;

        RenderGlobalVisitor(ClassVisitor parent) {
            super(Opcodes.ASM5, parent);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (isSetupTerrainDescriptor(descriptor)) {
                setupTerrainSeen = true;
                // d0/d1/d2 are motion checks; d3/d4/d5 are the exact render-container origin.
                return new RenderGlobalCoordinateVisitor(parent, this, true, 3);
            }
            if (isRenderEntitiesDescriptor(descriptor)) {
                renderEntitiesSeen = true;
                // d0/d1/d2 are culling distances; d3/d4/d5 feed tile/entity render origins.
                return new RenderGlobalCoordinateVisitor(parent, this, false, 3);
            }
            return parent;
        }
    }

    private static final class RenderGlobalCoordinateVisitor extends MethodVisitor {
        private final RenderGlobalVisitor owner;
        private final boolean terrain;
        private final int firstTargetStore;
        private int doubleStoreOrdinal;
        private int localCoordinateHooks;

        RenderGlobalCoordinateVisitor(MethodVisitor parent, RenderGlobalVisitor owner, boolean terrain,
                                      int firstTargetStore) {
            super(Opcodes.ASM5, parent);
            this.owner = owner;
            this.terrain = terrain;
            this.firstTargetStore = firstTargetStore;
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            super.visitVarInsn(opcode, var);
            if (opcode != Opcodes.DSTORE) return;
            int axis = doubleStoreOrdinal - firstTargetStore;
            if (axis >= 0 && axis < 3) {
                emitCoordinateHook(axis, var);
                localCoordinateHooks++;
            }
            doubleStoreOrdinal++;
        }

        private void emitCoordinateHook(int axis, int localVariable) {
            super.visitVarInsn(Opcodes.DLOAD, localVariable);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, CAMERA_HOOKS[axis], "(D)D", false);
            super.visitVarInsn(Opcodes.DSTORE, localVariable);
        }

        @Override
        public void visitEnd() {
            if (terrain) owner.setupTerrainHooks += localCoordinateHooks;
            else owner.renderEntitiesHooks += localCoordinateHooks;
            super.visitEnd();
        }
    }

}
