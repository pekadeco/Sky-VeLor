package com.arena.pekaclient.assist.visual;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Bytecode regression coverage for the fail-closed, mapping-neutral render-coordinate hook. */
public final class AspectTeleportCameraTransformerTests {
    private static final String HOOK_OWNER = "com/arena/pekaclient/assist/visual/AspectTeleportCameraHooks";
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        foreignAndIncompleteShapesStayUntouched();
        mcpRenderTargetsReceiveOnlyTheExpectedCoordinateHooks();
        notchClassNamesUseTheSameDescriptorRules();
        realForgeClassesMatchTheFailClosedShape();
        done();
    }

    private static void foreignAndIncompleteShapesStayUntouched() {
        AspectTeleportCameraTransformer transformer = new AspectTeleportCameraTransformer();
        byte[] foreign = simpleClass("example/Other", "noop", "()V");
        check("foreign class unchanged", transformer.transform("example.Other", "example.Other", foreign) == foreign);

        AspectTeleportCameraHookStatus.resetForTests();
        byte[] incomplete = entityRendererClass("net/minecraft/client/renderer/EntityRenderer", false,
                "Lnet/minecraft/client/renderer/RenderGlobal;");
        check("world pass without existing Forge callback stays untouched", transformer.transform(
                "net.minecraft.client.renderer.EntityRenderer", "net.minecraft.client.renderer.EntityRenderer", incomplete)
                == incomplete);
        check("incomplete EntityRenderer never enables a render path", !AspectTeleportCameraHookStatus.isReady());

        AspectTeleportCameraHookStatus.resetForTests();
        byte[] missingOrient = entityRendererClass("net/minecraft/client/renderer/EntityRenderer", true,
                "Lnet/minecraft/client/renderer/RenderGlobal;", "unrelatedCameraMethod");
        check("world pass without the exact orientCamera method stays untouched", transformer.transform(
                "net.minecraft.client.renderer.EntityRenderer", "net.minecraft.client.renderer.EntityRenderer", missingOrient)
                == missingOrient);
        check("missing orientCamera never enables a render path", !AspectTeleportCameraHookStatus.isReady());

        AspectTeleportCameraHookStatus.resetForTests();
        byte[] incompleteGlobal = renderGlobalClass("net/minecraft/client/renderer/RenderGlobal", false,
                "Lnet/minecraft/entity/Entity;", "Lnet/minecraft/client/renderer/culling/ICamera;");
        check("RenderGlobal without both coordinate surfaces stays untouched", transformer.transform(
                "net.minecraft.client.renderer.RenderGlobal", "net.minecraft.client.renderer.RenderGlobal", incompleteGlobal)
                == incompleteGlobal);
        check("incomplete RenderGlobal never enables a render path", !AspectTeleportCameraHookStatus.isReady());
    }

    private static void mcpRenderTargetsReceiveOnlyTheExpectedCoordinateHooks() {
        AspectTeleportCameraHookStatus.resetForTests();
        AspectTeleportCameraTransformer transformer = new AspectTeleportCameraTransformer();
        byte[] entity = transformer.transform("net.minecraft.client.renderer.EntityRenderer",
                "net.minecraft.client.renderer.EntityRenderer", entityRendererClass(
                        "net/minecraft/client/renderer/EntityRenderer", true,
                        "Lnet/minecraft/client/renderer/RenderGlobal;"));
        check("EntityRenderer replaces exactly one world-pass local x", calls(entity, HOOK_OWNER, "renderCameraX") == 1);
        check("EntityRenderer replaces exactly one world-pass local y", calls(entity, HOOK_OWNER, "renderCameraY") == 1);
        check("EntityRenderer replaces exactly one world-pass local z", calls(entity, HOOK_OWNER, "renderCameraZ") == 1);
        check("EntityRenderer replaces all three orientCamera locals before vanilla camera setup",
                calls(entity, HOOK_OWNER, "orientCameraX") == 1 && calls(entity, HOOK_OWNER, "orientCameraY") == 1
                        && calls(entity, HOOK_OWNER, "orientCameraZ") == 1);
        check("orientCamera eye Y retains its partial-ticks input", calls(entity, HOOK_OWNER, "orientCameraY", "(DF)D") == 1);
        check("EntityRenderer routes only the existing world-last callback through the balanced bridge",
                calls(entity, HOOK_OWNER, "dispatchRenderLast") == 1);
        check("world-last bridge uses a mapping-neutral Object descriptor",
                calls(entity, HOOK_OWNER, "dispatchRenderLast", "(Ljava/lang/Object;F)V") == 1);
        check("EntityRenderer hook alone does not enable partial smoothing", !AspectTeleportCameraHookStatus.isReady()
                && AspectTeleportCameraHookStatus.isEntityRendererActive());

        byte[] global = transformer.transform("net.minecraft.client.renderer.RenderGlobal",
                "net.minecraft.client.renderer.RenderGlobal", renderGlobalClass(
                        "net/minecraft/client/renderer/RenderGlobal", true,
                        "Lnet/minecraft/entity/Entity;", "Lnet/minecraft/client/renderer/culling/ICamera;"));
        check("RenderGlobal terrain and entities each replace one x coordinate", calls(global, HOOK_OWNER, "renderCameraX") == 2);
        check("RenderGlobal terrain and entities each replace one y coordinate", calls(global, HOOK_OWNER, "renderCameraY") == 2);
        check("RenderGlobal terrain and entities each replace one z coordinate", calls(global, HOOK_OWNER, "renderCameraZ") == 2);
        check("both complete render targets enable the visual path", AspectTeleportCameraHookStatus.isReady()
                && AspectTeleportCameraHookStatus.isRenderGlobalActive());
    }

    private static void notchClassNamesUseTheSameDescriptorRules() {
        AspectTeleportCameraHookStatus.resetForTests();
        AspectTeleportCameraTransformer transformer = new AspectTeleportCameraTransformer();
        byte[] entity = transformer.transform("bfk", "bfk", entityRendererClass("bfk", true, "Lbfr;"));
        byte[] global = transformer.transform("bfr", "bfr", renderGlobalClass("bfr", true, "Lpk;", "Lbiu;"));
        check("notch EntityRenderer receives all three world-pass coordinate hooks", calls(entity, HOOK_OWNER, "renderCameraX") == 1
                && calls(entity, HOOK_OWNER, "renderCameraY") == 1 && calls(entity, HOOK_OWNER, "renderCameraZ") == 1);
        check("notch EntityRenderer receives all three orientCamera hooks", calls(entity, HOOK_OWNER, "orientCameraX") == 1
                && calls(entity, HOOK_OWNER, "orientCameraY") == 1 && calls(entity, HOOK_OWNER, "orientCameraZ") == 1);
        check("notch world-last bridge stays mapping-neutral", calls(entity, HOOK_OWNER, "dispatchRenderLast",
                "(Ljava/lang/Object;F)V") == 1);
        check("notch RenderGlobal receives terrain and entity hooks", calls(global, HOOK_OWNER, "renderCameraX") == 2
                && calls(global, HOOK_OWNER, "renderCameraY") == 2 && calls(global, HOOK_OWNER, "renderCameraZ") == 2);
        check("notch complete targets still enable the visual path", AspectTeleportCameraHookStatus.isReady());

        AspectTeleportCameraHookStatus.resetForTests();
        byte[] srg = transformer.transform("net.minecraft.client.renderer.EntityRenderer",
                "net.minecraft.client.renderer.EntityRenderer", entityRendererClass(
                        "srg/EntityRendererFixture", true, "Lnet/minecraft/client/renderer/RenderGlobal;", "func_78467_g"));
        check("SRG orientCamera name receives all three local hooks", calls(srg, HOOK_OWNER, "orientCameraX") == 1
                && calls(srg, HOOK_OWNER, "orientCameraY") == 1 && calls(srg, HOOK_OWNER, "orientCameraZ") == 1);
    }

    /**
     * Fixture tests prove the visitor rules; this checks the actual deobfuscated Forge 1.8.9 classes supplied to the
     * standalone suite. It never defines or executes the transformed classes, so no game/GL client is started.
     */
    private static void realForgeClassesMatchTheFailClosedShape() {
        byte[] entityOriginal = resource("net/minecraft/client/renderer/EntityRenderer.class");
        byte[] globalOriginal = resource("net/minecraft/client/renderer/RenderGlobal.class");
        check("Forge test classpath exposes EntityRenderer bytecode", entityOriginal != null && entityOriginal.length > 0);
        check("Forge test classpath exposes RenderGlobal bytecode", globalOriginal != null && globalOriginal.length > 0);
        if (entityOriginal == null || globalOriginal == null) return;

        AspectTeleportCameraHookStatus.resetForTests();
        AspectTeleportCameraTransformer transformer = new AspectTeleportCameraTransformer();
        byte[] entity = transformer.transform("net.minecraft.client.renderer.EntityRenderer",
                "net.minecraft.client.renderer.EntityRenderer", entityOriginal);
        byte[] global = transformer.transform("net.minecraft.client.renderer.RenderGlobal",
                "net.minecraft.client.renderer.RenderGlobal", globalOriginal);
        check("real EntityRenderer receives the three world-pass coordinate hooks", calls(entity, HOOK_OWNER, "renderCameraX") == 1
                && calls(entity, HOOK_OWNER, "renderCameraY") == 1 && calls(entity, HOOK_OWNER, "renderCameraZ") == 1);
        check("real EntityRenderer receives the three pre-transform orientCamera hooks", calls(entity, HOOK_OWNER,
                "orientCameraX") == 1 && calls(entity, HOOK_OWNER, "orientCameraY") == 1
                && calls(entity, HOOK_OWNER, "orientCameraZ") == 1);
        check("real EntityRenderer routes Forge world-last once", calls(entity, HOOK_OWNER, "dispatchRenderLast") == 1);
        check("real RenderGlobal receives terrain and entity coordinate hooks", calls(global, HOOK_OWNER, "renderCameraX") == 2
                && calls(global, HOOK_OWNER, "renderCameraY") == 2 && calls(global, HOOK_OWNER, "renderCameraZ") == 2);
        check("real 1.8.9 render targets enable the visual path together", AspectTeleportCameraHookStatus.isReady());
    }

    private static byte[] entityRendererClass(String owner, boolean includeWorldLast, String globalDescriptor) {
        return entityRendererClass(owner, includeWorldLast, globalDescriptor,
                "bfk".equals(owner) ? "g" : "orientCamera");
    }

    private static byte[] entityRendererClass(String owner, boolean includeWorldLast, String globalDescriptor,
                                              String orientName) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);
        constructor(writer);
        MethodVisitor target = writer.visitMethod(Opcodes.ACC_PUBLIC, "renderWorldPass", "(IFJ)V", null, null);
        target.visitCode();
        storeDouble(target, 1D, 5);
        storeDouble(target, 2D, 7);
        storeDouble(target, 3D, 9);
        if (includeWorldLast) {
            target.visitInsn(Opcodes.ACONST_NULL);
            target.visitVarInsn(Opcodes.FLOAD, 2);
            target.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraftforge/client/ForgeHooksClient",
                    "dispatchRenderLast", "(" + globalDescriptor + "F)V", false);
        }
        target.visitInsn(Opcodes.RETURN);
        target.visitMaxs(0, 0);
        target.visitEnd();
        MethodVisitor orient = writer.visitMethod(Opcodes.ACC_PRIVATE, orientName, "(F)V", null, null);
        orient.visitCode();
        storeDouble(orient, 1D, 2);
        storeDouble(orient, 2D, 4);
        storeDouble(orient, 3D, 6);
        orient.visitInsn(Opcodes.RETURN);
        orient.visitMaxs(0, 0);
        orient.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] renderGlobalClass(String owner, boolean includeEntities, String entityDescriptor,
                                            String cameraDescriptor) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);
        constructor(writer);
        MethodVisitor terrain = writer.visitMethod(Opcodes.ACC_PUBLIC, "setupTerrain", "(" + entityDescriptor
                + "D" + cameraDescriptor + "IZ)V", null, null);
        terrain.visitCode();
        storeSixDoubles(terrain, 7);
        terrain.visitInsn(Opcodes.RETURN);
        terrain.visitMaxs(0, 0);
        terrain.visitEnd();
        if (includeEntities) {
            MethodVisitor entities = writer.visitMethod(Opcodes.ACC_PUBLIC, "renderEntities", "(" + entityDescriptor
                    + cameraDescriptor + "F)V", null, null);
            entities.visitCode();
            storeSixDoubles(entities, 4);
            entities.visitInsn(Opcodes.RETURN);
            entities.visitMaxs(0, 0);
            entities.visitEnd();
        }
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void storeSixDoubles(MethodVisitor visitor, int firstLocal) {
        for (int i = 0; i < 6; i++) storeDouble(visitor, i + 1D, firstLocal + i * 2);
    }

    private static void storeDouble(MethodVisitor visitor, double value, int local) {
        visitor.visitLdcInsn(Double.valueOf(value));
        visitor.visitVarInsn(Opcodes.DSTORE, local);
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

    private static byte[] resource(String path) {
        InputStream input = AspectTeleportCameraTransformerTests.class.getClassLoader().getResourceAsStream(path);
        if (input == null) return null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) out.write(buffer, 0, read);
            return out.toByteArray();
        } catch (IOException ignored) {
            return null;
        } finally {
            try { input.close(); } catch (IOException ignored) { }
        }
    }

    private static int calls(byte[] bytes, final String expectedOwner, final String expectedMethod,
                             final String expectedDescriptor) {
        final int[] count = {0};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                             String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM5) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method, String desc, boolean itf) {
                        if (opcode == Opcodes.INVOKESTATIC && expectedOwner.equals(owner) && expectedMethod.equals(method)
                                && expectedDescriptor.equals(desc)) {
                            count[0]++;
                        }
                    }
                };
            }
        }, 0);
        return count[0];
    }

    private static void check(String label, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + label); }
    }

    private static void done() {
        System.out.println("Aspect teleport camera transformer: passed " + passed + ", failed " + failed);
        if (failed != 0) System.exit(1);
    }
}
