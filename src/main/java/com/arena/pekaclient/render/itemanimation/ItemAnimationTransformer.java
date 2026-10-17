package com.arena.pekaclient.render.itemanimation;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Direct Forge 1.8.9 transformer for {@code ItemRenderer.renderItemInFirstPerson(float)}.
 *
 * <p>It intentionally names every supported 1.8.9 representation instead of guessing a caller:
 * MCP {@code renderItemInFirstPerson}, SRG {@code func_78440_a}, and notch {@code a}, all with {@code (F)V}.
 * The same explicit rule targets the equip method ({@code updateEquippedItem}/{@code func_78441_a}/{@code a},
 * {@code ()V}). The descriptor disambiguates the short notch name. No EntityRenderer call-site, renderer swap,
 * reflection, entity update, world render, or gameplay code is touched.
 */
public final class ItemAnimationTransformer implements IClassTransformer {

    private static final String ITEM_RENDERER_MCP = "net.minecraft.client.renderer.ItemRenderer";
    private static final String ITEM_RENDERER_NOTCH = "bfn";

    private static final String RENDER_MCP = "renderItemInFirstPerson";
    private static final String RENDER_SRG = "func_78440_a";
    private static final String RENDER_NOTCH = "a";
    private static final String RENDER_DESC = "(F)V";

    private static final String EQUIP_MCP = "updateEquippedItem";
    private static final String EQUIP_SRG = "func_78441_a";
    private static final String EQUIP_NOTCH = "a";
    private static final String EQUIP_DESC = "()V";

    private static final String HOOK_OWNER = "com/arena/pekaclient/render/itemanimation/ItemAnimationHooks";
    private static final String BEFORE_HOOK = "beforeFirstPersonRender";
    private static final String AFTER_HOOK = "afterFirstPersonRender";
    private static final String SWING_HOOK = "visualSwingProgress";
    private static final String EQUIP_HOOK = "scaleEquipProgress";

    private static final String GL_MCP = "net/minecraft/client/renderer/GlStateManager";
    private static final String GL_NOTCH = "bfl";

    private static final String LIVING_MCP = "net/minecraft/entity/EntityLivingBase";
    private static final String LIVING_NOTCH = "pr";
    private static final String CLIENT_PLAYER_MCP = "net/minecraft/client/entity/AbstractClientPlayer";
    private static final String CLIENT_PLAYER_NOTCH = "bet";
    private static final String PLAYER_SP_MCP = "net/minecraft/client/entity/EntityPlayerSP";
    private static final String PLAYER_SP_NOTCH = "bew";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !isItemRenderer(name, transformedName)) {
            return basicClass;
        }
        ItemAnimationHookStatus.noteItemRendererClass(name == null ? transformedName : name);
        try {
            ClassReader reader = new ClassReader(basicClass);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            ItemRendererVisitor visitor = new ItemRendererVisitor(writer);
            reader.accept(visitor, 0);
            return visitor.changed ? writer.toByteArray() : basicClass;
        } catch (Throwable error) {
            // A failed transformer must leave the original class available and expose a useful launcher diagnosis.
            ItemAnimationHookStatus.fail("ItemRenderer transformer error: "
                    + error.getClass().getSimpleName());
            return basicClass;
        }
    }

    private static boolean isItemRenderer(String name, String transformedName) {
        return ITEM_RENDERER_MCP.equals(name) || ITEM_RENDERER_MCP.equals(transformedName)
                || ITEM_RENDERER_NOTCH.equals(name) || ITEM_RENDERER_NOTCH.equals(transformedName);
    }

    private static boolean isRenderMethod(String name, String descriptor) {
        return RENDER_DESC.equals(descriptor) && (RENDER_MCP.equals(name) || RENDER_SRG.equals(name)
                || RENDER_NOTCH.equals(name));
    }

    private static boolean isEquipMethod(String name, String descriptor) {
        return EQUIP_DESC.equals(descriptor) && (EQUIP_MCP.equals(name) || EQUIP_SRG.equals(name)
                || EQUIP_NOTCH.equals(name));
    }

    private static String variant(String methodName) {
        if (RENDER_MCP.equals(methodName) || EQUIP_MCP.equals(methodName)) return "MCP";
        if (RENDER_SRG.equals(methodName) || EQUIP_SRG.equals(methodName)) return "SRG";
        return "notch";
    }

    private static boolean isGlOwner(String owner) {
        return GL_MCP.equals(owner) || GL_NOTCH.equals(owner);
    }

    private static boolean isVanillaPush(String owner, String name, String descriptor) {
        return isGlOwner(owner) && "()V".equals(descriptor)
                && ("pushMatrix".equals(name) || "func_179094_E".equals(name) || "E".equals(name));
    }

    private static boolean isVanillaPop(String owner, String name, String descriptor) {
        return isGlOwner(owner) && "()V".equals(descriptor)
                && ("popMatrix".equals(name) || "func_179121_F".equals(name) || "F".equals(name));
    }

    /**
     * getSwingProgress is inherited, so bytecode can name EntityLivingBase or AbstractClientPlayer depending on
     * the launcher mapping. Restricting the owner prevents notch MathHelper.a(F)F from being mistaken for swing.
     */
    private static boolean isSwingProgress(String owner, String name, String descriptor) {
        boolean ownerMatches = LIVING_MCP.equals(owner) || LIVING_NOTCH.equals(owner)
                || CLIENT_PLAYER_MCP.equals(owner) || CLIENT_PLAYER_NOTCH.equals(owner)
                || PLAYER_SP_MCP.equals(owner) || PLAYER_SP_NOTCH.equals(owner);
        return ownerMatches && "(F)F".equals(descriptor)
                && ("getSwingProgress".equals(name) || "func_70678_g".equals(name) || "l".equals(name));
    }

    private static boolean isItemStackDescriptor(String descriptor) {
        return "Lnet/minecraft/item/ItemStack;".equals(descriptor) || "Lzx;".equals(descriptor);
    }

    private static final class ItemRendererVisitor extends ClassVisitor {
        private String className;
        private String itemStackField;
        private String itemStackDescriptor;
        private boolean renderTargetSeen;
        private boolean renderMatrixInjected;
        private boolean renderCleanupInjected;
        private boolean renderSwingInjected;
        private boolean equipTargetSeen;
        private boolean equipInjected;
        private boolean changed;

        ItemRendererVisitor(ClassVisitor parent) {
            super(Opcodes.ASM5, parent);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                          String[] interfaces) {
            className = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public org.objectweb.asm.FieldVisitor visitField(int access, String name, String descriptor,
                                                         String signature, Object value) {
            // 1.8.9 ItemRenderer has one ItemStack field: itemToRender. Recording it from the class declaration
            // lets the hook inspect the exact stack being rendered without reflection or a player-state guess.
            if (itemStackField == null && isItemStackDescriptor(descriptor)) {
                itemStackField = name;
                itemStackDescriptor = descriptor;
            }
            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (isRenderMethod(name, descriptor)) {
                renderTargetSeen = true;
                ItemAnimationHookStatus.noteRenderTarget(variant(name));
                return new RenderMethodVisitor(parent, this, itemStackField, itemStackDescriptor);
            }
            if (isEquipMethod(name, descriptor)) {
                equipTargetSeen = true;
                ItemAnimationHookStatus.noteEquipTarget(variant(name));
                return new EquipMethodVisitor(parent, this, className);
            }
            return parent;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            if (!renderTargetSeen) {
                ItemAnimationHookStatus.fail("renderItemInFirstPerson(F)V target not found (MCP/SRG/notch)");
            } else if (!renderMatrixInjected || !renderCleanupInjected) {
                ItemAnimationHookStatus.fail("balanced render matrix hooks were not injected around vanilla push/pop");
            } else if (!renderSwingInjected) {
                ItemAnimationHookStatus.fail("getSwingProgress(F) hook was not injected");
            } else {
                ItemAnimationHookStatus.noteRenderInjection(true, true);
            }

            if (!equipTargetSeen) {
                ItemAnimationHookStatus.fail("updateEquippedItem()V target not found (MCP/SRG/notch)");
            } else if (!equipInjected) {
                ItemAnimationHookStatus.fail("equip-progress hook was not injected");
            } else {
                ItemAnimationHookStatus.noteEquipInjection(true);
            }
        }
    }

    /** Weave a nested matrix immediately after vanilla's outer hand matrix and close it before vanilla pops. */
    private static final class RenderMethodVisitor extends MethodVisitor {
        private final ItemRendererVisitor owner;
        private final String itemStackField;
        private final String itemStackDescriptor;
        private boolean matrixOpen;
        private boolean matrixInjected;
        private boolean swingInjected;

        RenderMethodVisitor(MethodVisitor parent, ItemRendererVisitor owner, String itemStackField,
                            String itemStackDescriptor) {
            super(Opcodes.ASM5, parent);
            this.owner = owner;
            this.itemStackField = itemStackField;
            this.itemStackDescriptor = itemStackDescriptor;
        }

        @Override
        public void visitMethodInsn(int opcode, String methodOwner, String methodName, String methodDescriptor,
                                    boolean isInterface) {
            if (HOOK_OWNER.equals(methodOwner)) {
                if (BEFORE_HOOK.equals(methodName)) {
                    matrixInjected = true;
                    matrixOpen = true;
                    owner.renderMatrixInjected = true;
                } else if (AFTER_HOOK.equals(methodName)) {
                    matrixOpen = false;
                    owner.renderCleanupInjected = true;
                } else if (SWING_HOOK.equals(methodName)) {
                    swingInjected = true;
                    owner.renderSwingInjected = true;
                }
                super.visitMethodInsn(opcode, methodOwner, methodName, methodDescriptor, isInterface);
                return;
            }

            if (!swingInjected && isSwingProgress(methodOwner, methodName, methodDescriptor)) {
                super.visitMethodInsn(opcode, methodOwner, methodName, methodDescriptor, isInterface);
                // Original invocation leaves vanilla swing progress on the stack. Add partialTicks as the second
                // argument so the hook can advance a visual-only timeline without an EntityLivingBase mutation.
                super.visitVarInsn(Opcodes.FLOAD, 1);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, SWING_HOOK, "(FF)F", false);
                swingInjected = true;
                owner.renderSwingInjected = true;
                owner.changed = true;
                return;
            }

            if (!matrixInjected && isVanillaPush(methodOwner, methodName, methodDescriptor)) {
                if (itemStackField == null || itemStackDescriptor == null) {
                    // Do not fall back to player state: a missing exact ItemStack field must be diagnosed, not
                    // accidentally transform an arm or map while the renderer is switching items.
                    super.visitMethodInsn(opcode, methodOwner, methodName, methodDescriptor, isInterface);
                    return;
                }
                super.visitMethodInsn(opcode, methodOwner, methodName, methodDescriptor, isInterface);
                // This runs after ItemRenderer's vanilla rotations/light/rescale setup and its own pushMatrix.
                // Pass its actual itemToRender field as Object so map/null branches can remain untouched.
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitFieldInsn(Opcodes.GETFIELD, owner.className, itemStackField, itemStackDescriptor);
                super.visitVarInsn(Opcodes.FLOAD, 1);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, BEFORE_HOOK,
                        "(Ljava/lang/Object;F)V", false);
                matrixInjected = true;
                matrixOpen = true;
                owner.renderMatrixInjected = true;
                owner.changed = true;
                return;
            }

            if (matrixOpen && isVanillaPop(methodOwner, methodName, methodDescriptor)) {
                // Pop our nested matrix first; vanilla then pops exactly the matrix it originally pushed.
                super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, AFTER_HOOK, "()V", false);
                matrixOpen = false;
                owner.renderCleanupInjected = true;
                super.visitMethodInsn(opcode, methodOwner, methodName, methodDescriptor, isInterface);
                return;
            }
            super.visitMethodInsn(opcode, methodOwner, methodName, methodDescriptor, isInterface);
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN && matrixOpen) {
                // Defensive coverage for an unexpected return before ItemRenderer's normal popMatrix cleanup.
                super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, AFTER_HOOK, "()V", false);
                matrixOpen = false;
                owner.renderCleanupInjected = true;
            }
            super.visitInsn(opcode);
        }
    }

    /** Injects a post-vanilla visual equip interpolation inside the explicitly named update method. */
    private static final class EquipMethodVisitor extends MethodVisitor {
        private final ItemRendererVisitor owner;
        private final String itemRendererClass;
        private String previousField;
        private String currentField;
        private boolean injected;

        EquipMethodVisitor(MethodVisitor parent, ItemRendererVisitor owner, String itemRendererClass) {
            super(Opcodes.ASM5, parent);
            this.owner = owner;
            this.itemRendererClass = itemRendererClass;
        }

        @Override
        public void visitFieldInsn(int opcode, String fieldOwner, String name, String descriptor) {
            super.visitFieldInsn(opcode, fieldOwner, name, descriptor);
            if (opcode == Opcodes.PUTFIELD && itemRendererClass.equals(fieldOwner) && "F".equals(descriptor)) {
                if (previousField == null) {
                    previousField = name;
                }
                currentField = name;
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String methodOwner, String methodName, String methodDescriptor,
                                    boolean isInterface) {
            if (HOOK_OWNER.equals(methodOwner) && EQUIP_HOOK.equals(methodName)) {
                injected = true;
                owner.equipInjected = true;
            }
            super.visitMethodInsn(opcode, methodOwner, methodName, methodDescriptor, isInterface);
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN && !injected && previousField != null && currentField != null
                    && !previousField.equals(currentField)) {
                // this.current = scaleEquipProgress(this.previous, this.current); only ItemRenderer fields move.
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitFieldInsn(Opcodes.GETFIELD, itemRendererClass, previousField, "F");
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitFieldInsn(Opcodes.GETFIELD, itemRendererClass, currentField, "F");
                super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, EQUIP_HOOK, "(FF)F", false);
                super.visitFieldInsn(Opcodes.PUTFIELD, itemRendererClass, currentField, "F");
                injected = true;
                owner.equipInjected = true;
                owner.changed = true;
            }
            super.visitInsn(opcode);
        }
    }
}
