package fewizz.canpipe.mixin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import static org.lwjgl.opengl.GL33C.*;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.google.common.collect.Streams;

public class Plugin implements IMixinConfigPlugin, Opcodes {

    public record AdditionalTextureFormat(
        int internalFormat,
        int format,
        int type,
        int pixelSize,
        boolean hasColorAspect,
        boolean hasDepthAspect
    ) {}

    private static final Map<String, AdditionalTextureFormat> ADDITIONAL_TEXTURE_FORMATS = new HashMap<>() {{
        put("RGBA16F", new AdditionalTextureFormat(GL_RGBA16F, GL_RGBA, GL_FLOAT, 4*2, true, false));
        put("DEPTH_COMPONENT32F", new AdditionalTextureFormat(GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT, GL_FLOAT, 1*4, false, true));
        put("DEPTH_COMPONENT", new AdditionalTextureFormat(GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT, GL_FLOAT, 1*4, false, true));
        put("DEPTH_COMPONENT32", new AdditionalTextureFormat(GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT, GL_FLOAT, 1*4, false, true));
        put("R8", new AdditionalTextureFormat(GL_R8, GL_RED, GL_UNSIGNED_BYTE, 1*1, true, false));
        put("R16F", new AdditionalTextureFormat(GL_R16F, GL_RED, GL_FLOAT, 1*2, true, false));
        put("R32F", new AdditionalTextureFormat(GL_R32F, GL_RED, GL_FLOAT, 1*4, true, false));
        put("RG8", new AdditionalTextureFormat(GL_RG8, GL_RG, GL_UNSIGNED_BYTE, 2*1, true, false));
        put("RG16", new AdditionalTextureFormat(GL_RG16, GL_RG, GL_UNSIGNED_SHORT, 2*2, true, false));
        put("RGB8", new AdditionalTextureFormat(GL_RGB8, GL_RGB, GL_UNSIGNED_BYTE, 3*1, true, false));
        put("RGB16", new AdditionalTextureFormat(GL_RGB16, GL_RGB, GL_UNSIGNED_SHORT, 3*2, true, false));
        put("RGB16F", new AdditionalTextureFormat(GL_RGB16F, GL_RGB, GL_FLOAT, 3*2, true, false));
        put("RGB32UI", new AdditionalTextureFormat(GL_RGB32UI, GL_RGB, GL_UNSIGNED_INT, 3*4, true, false));
        put("RGBA12", new AdditionalTextureFormat(GL_RGBA12, GL_RGBA, GL_UNSIGNED_BYTE, 4*(12/8), true, false));
        put("RGBA16", new AdditionalTextureFormat(GL_RGBA16, GL_RGBA, GL_UNSIGNED_SHORT, 4*2, true, false));
        put("RGBA32F", new AdditionalTextureFormat(GL_RGBA32F, GL_RGBA, GL_FLOAT, 4*4, true, false));
        put("R11F_G11F_B10F", new AdditionalTextureFormat(GL_R11F_G11F_B10F, GL_RGB, -1, (11+11+10)/8, true, false));
    }};

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return true; }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (mixinClassName.equals("fewizz.canpipe.mixin.TextureFormatMixin")) {
            patchTextureFormat(targetClass);
        } else
        if (mixinClassName.equals("fewizz.canpipe.mixin.GlConstMixin")) {
            patchGlConst(targetClass);
        } else
        if (mixinClassName.equals("fewizz.canpipe.mixin.GlConstSwitchMetaMixin")) {
            patchGlConst$1(targetClass);
        } else
        if (mixinClassName.equals("fewizz.canpipe.mixin.UniformTypeMixin")) {
            patchUniformType(targetClass);
        }
    }

    private static void patchTextureFormat(ClassNode classNode) {
        // hard way, adding new texture types into enum
        // took main logic from my `crawl` mod

        String desc = "L"+classNode.name+";";

        MethodNode arrayInitMethod = classNode.methods.stream()
            .filter(m -> !m.name.equals("values") && m.desc.equals("()["+desc)).findFirst().get();

        MethodNode classInitMethod = classNode.methods.stream()
            .filter(m -> m.name.equals("<clinit>")).findFirst().get();

        TypeInsnNode aNewArrayInsn = (TypeInsnNode) StreamSupport.stream(arrayInitMethod.instructions.spliterator(), false)
            .filter(insn->insn.getOpcode() == ANEWARRAY).findFirst().get();

        InsnNode sizeArgInsn = (InsnNode) aNewArrayInsn.getPrevious();
        if (sizeArgInsn.getOpcode() != ICONST_3) { throw new RuntimeException(); }
        int i = 3;  // TODO actually compute size
        arrayInitMethod.instructions.insert(sizeArgInsn, new IntInsnNode(SIPUSH, i + ADDITIONAL_TEXTURE_FORMATS.size()));
        arrayInitMethod.instructions.remove(sizeArgInsn);

        for (var e : ADDITIONAL_TEXTURE_FORMATS.entrySet()) {
            classNode.fields.add(new FieldNode(ACC_PUBLIC | ACC_FINAL | ACC_STATIC | ACC_ENUM, e.getKey(), desc, null, null));

            InsnList createNewEntry = new InsnList();
            createNewEntry.add(new TypeInsnNode(NEW, classNode.name));
            createNewEntry.add(new InsnNode(DUP));
            createNewEntry.add(new LdcInsnNode(e.getKey()));
            createNewEntry.add(new IntInsnNode(SIPUSH, i));
            createNewEntry.add(new LdcInsnNode(Integer.valueOf(e.getValue().pixelSize)));
            createNewEntry.add(new MethodInsnNode(INVOKESPECIAL, classNode.name, "<init>", "(Ljava/lang/String;II)V"));
            createNewEntry.add(new FieldInsnNode(PUTSTATIC, classNode.name, e.getKey(), desc));
            classInitMethod.instructions.insertBefore(
                StreamSupport.stream(classInitMethod.instructions.spliterator(), false)
                    .filter(insn -> insn.getOpcode() == NEW)
                    .findFirst().get(),
                createNewEntry
            );

            InsnList addNewEntry = new InsnList();
            addNewEntry.add(new InsnNode(DUP)); // dup array
            addNewEntry.add(new IntInsnNode(BIPUSH, i));
            addNewEntry.add(new FieldInsnNode(GETSTATIC, classNode.name, e.getKey(), desc));
            addNewEntry.add(new InsnNode(AASTORE));
            arrayInitMethod.instructions.insert(aNewArrayInsn, addNewEntry);

            ++i;
        }

        {
            MethodNode hasColorAspect = classNode.methods.stream()
                .filter(m -> m.name.equals("hasColorAspect")).findFirst().get();

            LabelNode beforeReturn = (LabelNode) Streams.findLast(
                StreamSupport.stream(hasColorAspect.instructions.spliterator(), false)
                .takeWhile(insn -> insn.getOpcode() != IRETURN)
                .filter(insn -> insn instanceof LabelNode)
            ).get();

            InsnList insns = new InsnList();
            for (var e : ADDITIONAL_TEXTURE_FORMATS.entrySet()) {
                if (!e.getValue().hasColorAspect) continue;
                LabelNode nextCheck = new LabelNode();
                insns.add(new VarInsnNode(ALOAD, 0));
                insns.add(new FieldInsnNode(GETSTATIC, classNode.name, e.getKey(), desc));
                insns.add(new JumpInsnNode(IF_ACMPNE, nextCheck));
                insns.add(new InsnNode(ICONST_1));
                insns.add(new JumpInsnNode(GOTO, beforeReturn));
                insns.add(nextCheck);
            }
            hasColorAspect.instructions.insert(hasColorAspect.instructions.getFirst(), insns);
        }

        {
            MethodNode hasDepthAspect = classNode.methods.stream()
                .filter(m -> m.name.equals("hasDepthAspect")).findFirst().get();

            LabelNode beforeReturn = (LabelNode) Streams.findLast(
                StreamSupport.stream(hasDepthAspect.instructions.spliterator(), false)
                .takeWhile(insn -> insn.getOpcode() != IRETURN)
                .filter(insn -> insn instanceof LabelNode)
            ).get();

            InsnList insns = new InsnList();
            for (var e : ADDITIONAL_TEXTURE_FORMATS.entrySet()) {
                if (!e.getValue().hasDepthAspect) continue;
                LabelNode nextCheck = new LabelNode();
                insns.add(new VarInsnNode(ALOAD, 0));
                insns.add(new FieldInsnNode(GETSTATIC, classNode.name, e.getKey(), desc));
                insns.add(new JumpInsnNode(IF_ACMPNE, nextCheck));
                insns.add(new InsnNode(ICONST_1));
                insns.add(new JumpInsnNode(GOTO, beforeReturn));
                insns.add(nextCheck);
            }
            hasDepthAspect.instructions.insert(hasDepthAspect.instructions.getFirst(), insns);
        }
    }

    private static void patchGlConst(ClassNode classNode) {
        {
            MethodNode toGlInternalIdMethod = classNode.methods.stream()
                .filter(m -> m.name.equals("toGlInternalId")).findFirst().get();
            
            TableSwitchInsnNode switchNode = (TableSwitchInsnNode) StreamSupport.stream(
                toGlInternalIdMethod.instructions.spliterator(), false
            ).filter(insn -> insn.getOpcode() == TABLESWITCH).findFirst().get();

            LabelNode beforeReturn = (LabelNode) Streams.findLast(
                StreamSupport.stream(toGlInternalIdMethod.instructions.spliterator(), false)
                .takeWhile(insn -> insn.getOpcode() != IRETURN)
                .filter(insn -> insn instanceof LabelNode)
            ).get();

            InsnList insns = new InsnList();
            insns.add(new JumpInsnNode(GOTO, beforeReturn));

            for (var e : ADDITIONAL_TEXTURE_FORMATS.entrySet()) {
                LabelNode label = new LabelNode(new Label());
                switchNode.labels.add(label);
                insns.add(label);
                insns.add(new LdcInsnNode(e.getValue().internalFormat));
                insns.add(new JumpInsnNode(GOTO, beforeReturn));
            }
            switchNode.max += ADDITIONAL_TEXTURE_FORMATS.size();
            toGlInternalIdMethod.instructions.insertBefore(beforeReturn, insns);
        }
        {
            MethodNode toGlExternalIdMethod = classNode.methods.stream()
                .filter(m -> m.name.equals("toGlExternalId")).findFirst().get();
            
            TableSwitchInsnNode switchNode = (TableSwitchInsnNode) StreamSupport.stream(
                toGlExternalIdMethod.instructions.spliterator(), false
            ).filter(insn -> insn.getOpcode() == TABLESWITCH).findFirst().get();

            LabelNode beforeReturn = (LabelNode) Streams.findLast(
                StreamSupport.stream(toGlExternalIdMethod.instructions.spliterator(), false)
                .takeWhile(insn -> insn.getOpcode() != IRETURN)
                .filter(insn -> insn instanceof LabelNode)
            ).get();

            InsnList insns = new InsnList();
            insns.add(new JumpInsnNode(GOTO, beforeReturn));

            for (var e : ADDITIONAL_TEXTURE_FORMATS.entrySet()) {
                LabelNode label = new LabelNode(new Label());
                switchNode.labels.add(label);
                insns.add(label);
                insns.add(new LdcInsnNode(e.getValue().format));
                insns.add(new JumpInsnNode(GOTO, beforeReturn));
            }
            switchNode.max += ADDITIONAL_TEXTURE_FORMATS.size();
            toGlExternalIdMethod.instructions.insertBefore(beforeReturn, insns);
        }
        {
            MethodNode toGlExternalIdMethod = classNode.methods.stream()
                .filter(m -> m.name.equals("toGlType")).findFirst().get();
            
            TableSwitchInsnNode switchNode = (TableSwitchInsnNode) StreamSupport.stream(
                toGlExternalIdMethod.instructions.spliterator(), false
            ).filter(insn -> insn.getOpcode() == TABLESWITCH).findFirst().get();

            LabelNode beforeReturn = (LabelNode) Streams.findLast(
                StreamSupport.stream(toGlExternalIdMethod.instructions.spliterator(), false)
                .takeWhile(insn -> insn.getOpcode() != IRETURN)
                .filter(insn -> insn instanceof LabelNode)
            ).get();

            InsnList insns = new InsnList();
            insns.add(new JumpInsnNode(GOTO, beforeReturn));

            for (var e : ADDITIONAL_TEXTURE_FORMATS.entrySet()) {
                LabelNode label = new LabelNode(new Label());
                switchNode.labels.add(label);
                insns.add(label);
                insns.add(new LdcInsnNode(e.getValue().type));
                insns.add(new JumpInsnNode(GOTO, beforeReturn));
            }
            switchNode.max += ADDITIONAL_TEXTURE_FORMATS.size();
            toGlExternalIdMethod.instructions.insertBefore(beforeReturn, insns);
        }
    }

    private static void patchGlConst$1(ClassNode classNode) {
        MethodNode clinit = classNode.methods.stream()
            .filter(m -> m.name.equals("<clinit>")).findFirst().get();
        InsnList insns = new InsnList();

        for (int i = 0; i < ADDITIONAL_TEXTURE_FORMATS.size(); ++i) {
            insns.add(new FieldInsnNode(GETSTATIC, classNode.name, "$SwitchMap$com$mojang$blaze3d$textures$TextureFormat", "[I"));
            insns.add(new LdcInsnNode(i));
            insns.add(new LdcInsnNode(i+1));
            insns.add(new InsnNode(IASTORE));
        }
        clinit.instructions.insertBefore(clinit.instructions.getLast(), insns);
    }

    private static void patchUniformType(ClassNode classNode) {
        String desc = "L"+classNode.name+";";
        MethodNode arrayInitMethod = classNode.methods.stream()
            .filter(m -> !m.name.equals("values") && m.desc.equals("()["+desc)).findFirst().get();

        MethodNode classInitMethod = classNode.methods.stream()
            .filter(m -> m.name.equals("<clinit>")).findFirst().get();

        TypeInsnNode aNewArrayInsn = (TypeInsnNode) StreamSupport.stream(arrayInitMethod.instructions.spliterator(), false)
            .filter(insn->insn.getOpcode() == ANEWARRAY).findFirst().get();

        IntInsnNode sizeArgInsn = (IntInsnNode) aNewArrayInsn.getPrevious();
        // if (sizeArgInsn.getOpcode() != BIPUSH) { throw new RuntimeException(); }
        //int i = 3;  // TODO actually compute size
        sizeArgInsn.operand += 1;

        classNode.fields.add(new FieldNode(ACC_PUBLIC | ACC_FINAL | ACC_STATIC | ACC_ENUM, "IVEC2", desc, null, null));

        InsnList createNewEntry = new InsnList();
        createNewEntry.add(new TypeInsnNode(NEW, classNode.name));
        createNewEntry.add(new InsnNode(DUP));
        createNewEntry.add(new LdcInsnNode("IVEC2"));
        createNewEntry.add(new IntInsnNode(SIPUSH, sizeArgInsn.operand-1));
        createNewEntry.add(new InsnNode(ICONST_2));
        createNewEntry.add(new LdcInsnNode("ivec2"));
        createNewEntry.add(new MethodInsnNode(INVOKESPECIAL, classNode.name, "<init>", "(Ljava/lang/String;IILjava/lang/String;)V"));
        createNewEntry.add(new FieldInsnNode(PUTSTATIC, classNode.name, "IVEC2", desc));
        classInitMethod.instructions.insertBefore(
            StreamSupport.stream(classInitMethod.instructions.spliterator(), false)
                .filter(insn -> insn.getOpcode() == NEW)
                .findFirst().get(),
            createNewEntry
        );

        InsnList addNewEntry = new InsnList();
        addNewEntry.add(new InsnNode(DUP)); // dup array
        addNewEntry.add(new IntInsnNode(BIPUSH, sizeArgInsn.operand-1));
        addNewEntry.add(new FieldInsnNode(GETSTATIC, classNode.name, "IVEC2", desc));
        addNewEntry.add(new InsnNode(AASTORE));
        arrayInitMethod.instructions.insert(aNewArrayInsn, addNewEntry);
    }

}
