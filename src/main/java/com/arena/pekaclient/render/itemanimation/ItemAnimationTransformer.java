package com.arena.pekaclient.render.itemanimation;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Injects Item Animation into the two authoritative Minecraft 1.8.9 paths.
 *
 * <ul>
 *   <li>EntityRenderer's hand pass calls {@code before/after} around the first of its two ItemRenderer float
 *       calls. The first is renderItemInFirstPerson; the second is the overlay pass.</li>
 *   <li>ItemRenderer's unique no-argument method which writes two float progress fields is
 *       updateEquippedItem. Its final value is adjusted immediately before return.</li>
 * </ul>
 *
 * <p>The matching is structural, not dependent on MCP names. It therefore survives the normal Forge
 * deobfuscation order and only applies to the real first-person/equipped-progress path on Minecraft 1.8.9.
 */
public final class ItemAnimationTransformer implements IClassTransformer {

    private static final String ENTITY_RENDERER = "net.minecraft.client.renderer.EntityRenderer";
    private static final String ITEM_RENDERER = "net.minecraft.client.renderer.ItemRenderer";
    private static final String HOOK_OWNER = "com/arena/pekaclient/render/itemanimation/ItemAnimationHooks";
    private static final String BEFORE_HOOK = "beforeFirstPersonRender";
    private static final String AFTER_HOOK = "afterFirstPersonRender";
    private static final String EQUIP_HOOK = "scaleEquipProgress";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        if (matches(name, transformedName, ENTITY_RENDERER)) {
            return transformEntityRenderer(basicClass);
        }
        if (matches(name, transformedName, ITEM_RENDERER)) {
            return transformItemRenderer(basicClass);
        }
        return basicClass;
    }

    private static boolean matches(String name, String transformedName, String expected) {
        return expected.equals(name) || expected.equals(transformedName);
    }

    private static byte[] transformEntityRenderer(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        EntityRendererVisitor visitor = new EntityRendererVisitor(writer);
        reader.accept(visitor, 0);
        return visitor.changed ? writer.toByteArray() : bytes;
    }

    private static byte[] transformItemRenderer(byte[] bytes) {
        UpdateTarget target = locateEquipUpdate(bytes);
        if (target == null) {
            return bytes;
        }
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ItemRendererVisitor visitor = new ItemRendererVisitor(writer, target);
        reader.accept(visitor, 0);
        return visitor.changed ? writer.toByteArray() : bytes;
    }

    /**
     * Finds updateEquippedItem without a mapped name. In 1.8.9 it is the sole no-argument instance method that
     * stores previous equipped progress and then current equipped progress as two float fields.
     */
    private static UpdateTarget locateEquipUpdate(byte[] bytes) {
        final UpdateTarget[] found = new UpdateTarget[1];
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM5) {
            private String className;

            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                              String[] interfaces) {
                className = name;
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(final int access, final String name, final String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (found[0] != null || !"()V".equals(descriptor) || "<init>".equals(name)) {
                    return parent;
                }
                return new MethodVisitor(Opcodes.ASM5, parent) {
                    private final List<FieldWrite> floatWrites = new ArrayList<FieldWrite>();

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDescriptor) {
                        if (opcode == Opcodes.PUTFIELD && className != null && className.equals(owner)
                                && "F".equals(fieldDescriptor)) {
                            floatWrites.add(new FieldWrite(fieldName, fieldDescriptor));
                        }
                        super.visitFieldInsn(opcode, owner, fieldName, fieldDescriptor);
                    }

                    @Override
                    public void visitEnd() {
                        super.visitEnd();
                        if (found[0] == null && floatWrites.size() >= 2) {
                            FieldWrite previous = floatWrites.get(0);
                            FieldWrite current = floatWrites.get(floatWrites.size() - 1);
                            // Progress writes must be distinct. This rejects ordinary reset helpers.
                            if (!previous.name.equals(current.name)) {
                                found[0] = new UpdateTarget(className, name, descriptor, previous, current);
                            }
                        }
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG);
        return found[0];
    }

    private static final class EntityRendererVisitor extends ClassVisitor {
        private final Set<String> declaredObjectFieldTypes = new HashSet<String>();
        private boolean changed;

        EntityRendererVisitor(ClassVisitor parent) {
            super(Opcodes.ASM5, parent);
        }

        @Override
        public org.objectweb.asm.FieldVisitor visitField(int access, String name, String descriptor,
                                                         String signature, Object value) {
            if (descriptor != null && descriptor.length() > 2 && descriptor.charAt(0) == 'L'
                    && descriptor.charAt(descriptor.length() - 1) == ';') {
                declaredObjectFieldTypes.add(descriptor.substring(1, descriptor.length() - 1));
            }
            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
            // EntityRenderer.renderHand is (float partialTicks, int pass). Keeping this descriptor structural
            // works in both deobfuscated development and obfuscated 1.8.9 clients.
            if (!"(FI)V".equals(descriptor)) {
                return parent;
            }
            return new MethodVisitor(Opcodes.ASM5, parent) {
                private boolean firstItemRenderCallSeen;
                private boolean alreadyInstrumented;

                @Override
                public void visitMethodInsn(int opcode, String owner, String methodName, String methodDescriptor,
                                            boolean isInterface) {
                    if (HOOK_OWNER.equals(owner)
                            && (BEFORE_HOOK.equals(methodName) || AFTER_HOOK.equals(methodName))) {
                        alreadyInstrumented = true;
                    }
                    boolean itemRenderCall = opcode == Opcodes.INVOKEVIRTUAL
                            && "(F)V".equals(methodDescriptor)
                            && declaredObjectFieldTypes.contains(owner);
                    if (!alreadyInstrumented && !firstItemRenderCallSeen && itemRenderCall) {
                        firstItemRenderCallSeen = true;
                        // At this point the original stack is [ItemRenderer, partialTicks]. DUP2 preserves that
                        // pair while the duplicate feeds our Object/float callback; no locals or mapped type name
                        // are needed and the original virtual invocation remains byte-for-byte semantically intact.
                        super.visitInsn(Opcodes.DUP2);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, BEFORE_HOOK,
                                "(Ljava/lang/Object;F)V", false);
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, AFTER_HOOK, "()V", false);
                        changed = true;
                    } else {
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }
                }
            };
        }
    }

    private static final class ItemRendererVisitor extends ClassVisitor {
        private final UpdateTarget target;
        private boolean changed;

        ItemRendererVisitor(ClassVisitor parent, UpdateTarget target) {
            super(Opcodes.ASM5, parent);
            this.target = target;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (!target.methodName.equals(name) || !target.methodDescriptor.equals(descriptor)) {
                return parent;
            }
            return new MethodVisitor(Opcodes.ASM5, parent) {
                private boolean alreadyInstrumented;

                @Override
                public void visitMethodInsn(int opcode, String owner, String methodName, String methodDescriptor,
                                            boolean isInterface) {
                    if (HOOK_OWNER.equals(owner) && EQUIP_HOOK.equals(methodName)) {
                        alreadyInstrumented = true;
                    }
                    super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                }

                @Override
                public void visitInsn(int opcode) {
                    if (opcode == Opcodes.RETURN && !alreadyInstrumented) {
                        // this.current = scaleEquipProgress(this.previous, this.current)
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitFieldInsn(Opcodes.GETFIELD, target.className, target.previous.name,
                                target.previous.descriptor);
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitFieldInsn(Opcodes.GETFIELD, target.className, target.current.name,
                                target.current.descriptor);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, EQUIP_HOOK, "(FF)F", false);
                        super.visitFieldInsn(Opcodes.PUTFIELD, target.className, target.current.name,
                                target.current.descriptor);
                        changed = true;
                    }
                    super.visitInsn(opcode);
                }
            };
        }
    }

    private static final class FieldWrite {
        final String name;
        final String descriptor;

        FieldWrite(String name, String descriptor) {
            this.name = name;
            this.descriptor = descriptor;
        }
    }

    private static final class UpdateTarget {
        final String className;
        final String methodName;
        final String methodDescriptor;
        final FieldWrite previous;
        final FieldWrite current;

        UpdateTarget(String className, String methodName, String methodDescriptor,
                     FieldWrite previous, FieldWrite current) {
            this.className = className;
            this.methodName = methodName;
            this.methodDescriptor = methodDescriptor;
            this.previous = previous;
            this.current = current;
        }
    }
}
