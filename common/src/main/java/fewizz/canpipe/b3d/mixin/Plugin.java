package fewizz.canpipe.b3d.mixin;

import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_RED;
import static org.lwjgl.opengl.GL11C.GL_RGB16;
import static org.lwjgl.opengl.GL11C.GL_RGB8;
import static org.lwjgl.opengl.GL11C.GL_RGBA;
import static org.lwjgl.opengl.GL11C.GL_RGBA12;
import static org.lwjgl.opengl.GL11C.GL_RGBA16;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL30C.GL_R11F_G11F_B10F;
import static org.lwjgl.opengl.GL30C.GL_R16;
import static org.lwjgl.opengl.GL30C.GL_R16F;
import static org.lwjgl.opengl.GL30C.GL_R32F;
import static org.lwjgl.opengl.GL30C.GL_R8;
import static org.lwjgl.opengl.GL30C.GL_RG16;
import static org.lwjgl.opengl.GL30C.GL_RG16F;
import static org.lwjgl.opengl.GL30C.GL_RG32F;
import static org.lwjgl.opengl.GL30C.GL_RG8;
import static org.lwjgl.opengl.GL30C.GL_RGB16F;
import static org.lwjgl.opengl.GL30C.GL_RGB32F;
import static org.lwjgl.opengl.GL30C.GL_RGB32UI;
import static org.lwjgl.opengl.GL30C.GL_RGBA16F;
import static org.lwjgl.opengl.GL30C.GL_RGBA32F;
import static org.lwjgl.opengl.GL30C.GL_RGBA_INTEGER;
import static org.lwjgl.opengl.GL31C.GL_R16_SNORM;
import static org.lwjgl.opengl.GL31C.GL_R8_SNORM;
import static org.lwjgl.opengl.GL31C.GL_RG16_SNORM;
import static org.lwjgl.opengl.GL31C.GL_RG8_SNORM;
import static org.lwjgl.opengl.GL31C.GL_RGB16_SNORM;
import static org.lwjgl.opengl.GL31C.GL_RGB8_SNORM;
import static org.lwjgl.opengl.GL31C.GL_RGBA8_SNORM;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.stream.StreamSupport;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
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
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;

import net.minecraft.client.Screenshot;

public class Plugin implements IMixinConfigPlugin, Opcodes {

    /**
     * pixelSize - will be used in {@link TextureFormat#pixelSize()}, which is used in
     * {@link GlCommandEncoder#copyTextureToBuffer(GpuTexture, GpuBuffer, int, Runnable, int, int, int, int, int)},
     * {@link TextureUtil#writeAsPNG(Path, String, GpuTexture, int, IntUnaryOperator)} and
     * {@link Screenshot#takeScreenshot(RenderTarget, Consumer)}
     *
     * hasColorAspect - will be used in {@link TextureFormat#hasColorAspect()}, which is checked in
     * {@link GlCommandEncoder#clearColorTexture(GpuTexture, int)},
     * {@link GlCommandEncoder#clearColorAndDepthTextures(GpuTexture, int, GpuTexture, double)} and
     * {@link GlCommandEncoder#presentTexture(GpuTexture)}
     *
     * hasDepthAspect - will be used in {@link TextureFormat#hasDepthAspect()}, which is checked in
     * {@link GlCommandEncoder#clearColorAndDepthTextures(GpuTexture, int, GpuTexture, double)},
     * {@link GlCommandEncoder#clearDepthTexture(GpuTexture, double)},
     * {@link GlCommandEncoder#copyTextureToTexture(GpuTexture, GpuTexture, int, int, int, int, int, int, int)} and
     * {@link GlDevice#createTexture(String, TextureFormat, int, int, int)}
     *
     * glInternalFormat - will be used in {@link GlConst#toGlInternalId(TextureFormat)}, which in used in
     * {@link GlDevice#createTexture(String, TextureFormat, int, int, int)}
     *
     * glFormat - will be used in {@link GlConst#toGlExternalId(TextureFormat)}, which is used in
     * {@link GlCommandEncoder#copyTextureToBuffer(GpuTexture, GpuBuffer, int, Runnable, int, int, int, int, int)} and
     * {@link GlDevice#createTexture(String, TextureFormat, int, int, int)}
     *
     * glType - will be used in {@link GlConst#toGlType(TextureFormat)}, which is used in
     * {@link GlDevice#createTexture(String, TextureFormat, int, int, int)} and
     * {@link GlCommandEncoder#copyTextureToBuffer(GpuTexture, GpuBuffer, int, Runnable, int, int, int, int, int)}
     **/
    public record TexFormat(
        String name,  // enum name
        int pixelSize, boolean hasColorAspect, boolean hasDepthAspect,  // Will go into TextureFormat
        int glInternalFormat, int glFormat, int glType  // Will go into GlConst
    ) {}

    private static final List<TexFormat> ADDITIONAL_TEXTURE_FORMATS = new ArrayList<>() {{
        // put("DEPTH_COMPONENT32", new TexFormat(1*4, false, true, GL_DEPTH_COMPONENT32, GL_DEPTH_COMPONENT, GL_FLOAT));  // already defined as DEPTH32

        // add(new TexFormat("RED8", 1*4, true, false, GL_RED8, GL_RED, GL_FLOAT));  // already defined
        add(new TexFormat("R8_UNORM", 1*1, true, false, GL_R8, GL_RED, GL_UNSIGNED_BYTE));
        add(new TexFormat("R8_SNORM", 1*1, true, false, GL_R8_SNORM, GL_RED, GL_UNSIGNED_BYTE));
        add(new TexFormat("R16_UNORM", 1*1, true, false, GL_R16, GL_RED, GL_UNSIGNED_BYTE));
        add(new TexFormat("R16_SNORM", 1*1, true, false, GL_R16_SNORM, GL_RED, GL_UNSIGNED_BYTE));
        add(new TexFormat("R16_SFLOAT", 1*4, true, false, GL_R16F, GL_RED, GL_FLOAT));
        add(new TexFormat("R32_SFLOAT", 1*4, true, false, GL_R32F, GL_RED, GL_FLOAT));

        add(new TexFormat("RG8_UNORM", 4*1, true, false, GL_RG8, GL_RGBA, GL_UNSIGNED_BYTE));
        add(new TexFormat("RG8_SNORM", 4*1, true, false, GL_RG8_SNORM, GL_RGBA, GL_UNSIGNED_BYTE));
        add(new TexFormat("RG16_UNORM", 4*1, true, false, GL_RG16, GL_RGBA, GL_UNSIGNED_BYTE));
        add(new TexFormat("RG16_SNORM", 4*1, true, false, GL_RG16_SNORM, GL_RGBA, GL_UNSIGNED_BYTE));
        add(new TexFormat("RG16_SFLOAT", 1*4, true, false, GL_RG16F, GL_RED, GL_FLOAT));
        add(new TexFormat("RG32_SFLOAT", 1*4, true, false, GL_RG32F, GL_RED, GL_FLOAT));

        add(new TexFormat("RGB8_UNORM", 4*1, true, false, GL_RGB8, GL_RGBA, GL_UNSIGNED_BYTE));
        add(new TexFormat("RGB8_SNORM", 4*1, true, false, GL_RGB8_SNORM, GL_RGBA, GL_UNSIGNED_BYTE));
        add(new TexFormat("RGB16_UNORM", 4*1, true, false, GL_RGB16, GL_RGBA, GL_UNSIGNED_BYTE));
        add(new TexFormat("RGB16_SNORM", 4*1, true, false, GL_RGB16_SNORM, GL_RGBA, GL_UNSIGNED_BYTE));
        add(new TexFormat("RGB32_UINT", 4*1, true, false, GL_RGB32UI, GL_RGBA_INTEGER, GL_UNSIGNED_BYTE));
        add(new TexFormat("RGB16_SFLOAT", 4*1, true, false, GL_RGB16F, GL_RGBA, GL_UNSIGNED_BYTE));
        add(new TexFormat("RGB32_SFLOAT", 4*1, true, false, GL_RGB32F, GL_RGBA, GL_UNSIGNED_BYTE));
        add(new TexFormat("B10G11R11_UFLOAT_PACK32", 4*1, true, false, GL_R11F_G11F_B10F, GL_RGBA, GL_UNSIGNED_BYTE));

        // add(new TexFormat("RGBA8_UNORM", 4*1, true, false, GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE)); already defined as RGBA8
        add(new TexFormat("RGBA8_SNORM", 4*1, true, false, GL_RGBA8_SNORM, GL_RGBA, GL_UNSIGNED_BYTE));
        add(new TexFormat("R12X4G12X4B12X4A12X4_UNORM_4PACK16", 4*1, true, false, GL_RGBA12, GL_RGBA, GL_UNSIGNED_BYTE));
        add(new TexFormat("RGBA16_UNORM", 4*1, true, false, GL_RGBA16, GL_RGBA, GL_UNSIGNED_BYTE));
        add(new TexFormat("RGBA16_SFLOAT", 4*1, true, false, GL_RGBA16F, GL_RGBA, GL_UNSIGNED_BYTE));
        add(new TexFormat("RGBA32_SFLOAT", 4*1, true, false, GL_RGBA32F, GL_RGBA, GL_UNSIGNED_BYTE));
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
        switch (mixinClassName) {
            case "fewizz.canpipe.b3d.mixin.TextureFormatMixin" -> patchTextureFormat(targetClass);
            case "fewizz.canpipe.b3d.mixin.GlConstMixin" -> patchGlConst(targetClass);
            case "fewizz.canpipe.b3d.mixin.GlConstSwitchMetaMixin" -> patchGlConst$1(targetClass);
        }
    }

    private static void patchTextureFormat(ClassNode classNode) {
        String desc = "L"+classNode.name+";";

        // $values
        MethodNode arrayInitMethod = classNode.methods.stream()
            .filter(m -> !m.name.equals("values") && m.desc.equals("()["+desc)).findFirst().get();

        MethodNode classInitMethod = classNode.methods.stream()
            .filter(m -> m.name.equals("<clinit>")).findFirst().get();

        TypeInsnNode aNewArrayInsn = (TypeInsnNode) StreamSupport.stream(arrayInitMethod.instructions.spliterator(), false)
            .filter(insn->insn.getOpcode() == ANEWARRAY).findFirst().get();

        AbstractInsnNode sizeArgInsn = (AbstractInsnNode) aNewArrayInsn.getPrevious();

        int formatsCount = switch (sizeArgInsn.getOpcode()) {
            case ICONST_3 -> 3;  // should be 3
            case ICONST_4 -> 4;
            case ICONST_5 -> 5;  // Neo adds DEPTH24_STENCIL8 and DEPTH32F_STENCIL8
            case BIPUSH -> ((IntInsnNode) sizeArgInsn).operand;
            case SIPUSH -> ((IntInsnNode) sizeArgInsn).operand;
            default -> throw new RuntimeException("Unexpected texture formats count");
        };

        arrayInitMethod.instructions.insert(sizeArgInsn, new IntInsnNode(SIPUSH, formatsCount + ADDITIONAL_TEXTURE_FORMATS.size()));
        arrayInitMethod.instructions.remove(sizeArgInsn);

        for (var tex : ADDITIONAL_TEXTURE_FORMATS) {
            classNode.fields.add(new FieldNode(ACC_PUBLIC | ACC_FINAL | ACC_STATIC | ACC_ENUM, tex.name, desc, null, null));

            InsnList createNewEntry = new InsnList();
            createNewEntry.add(new TypeInsnNode(NEW, classNode.name));
            createNewEntry.add(new InsnNode(DUP));
            createNewEntry.add(new LdcInsnNode(tex.name));
            createNewEntry.add(new IntInsnNode(BIPUSH, formatsCount));
            createNewEntry.add(new IntInsnNode(BIPUSH, Integer.valueOf(tex.pixelSize)));
            createNewEntry.add(new MethodInsnNode(INVOKESPECIAL, classNode.name, "<init>", "(Ljava/lang/String;II)V"));
            createNewEntry.add(new FieldInsnNode(PUTSTATIC, classNode.name, tex.name, desc));

            classInitMethod.instructions.insertBefore(
                StreamSupport.stream(classInitMethod.instructions.spliterator(), false)
                    .filter(insn -> insn.getOpcode() == INVOKESTATIC)
                    .findFirst().get(),  // right before callogin arrayInitMethod ($values)
                createNewEntry
            );

            InsnList addNewEntry = new InsnList();
            addNewEntry.add(new InsnNode(DUP)); // dup array
            addNewEntry.add(new IntInsnNode(BIPUSH, formatsCount));
            addNewEntry.add(new FieldInsnNode(GETSTATIC, classNode.name, tex.name, desc));
            addNewEntry.add(new InsnNode(AASTORE));
            arrayInitMethod.instructions.insertBefore(
                StreamSupport.stream(arrayInitMethod.instructions.spliterator(), false)
                    .filter(insn -> insn.getOpcode() == ARETURN)
                    .findFirst().get(),  // before return
                addNewEntry
            );

            ++formatsCount;
        }

        {
            MethodNode hasColorAspect = classNode.methods.stream()
                .filter(m -> m.name.equals("hasColorAspect")).findFirst().get();

            LabelNode trueLabel = (LabelNode) Streams.findLast(
                StreamSupport.stream(hasColorAspect.instructions.spliterator(), false)
                .takeWhile(insn -> insn.getOpcode() != ICONST_1)
                .filter(insn -> insn.getType() == AbstractInsnNode.LABEL)
            ).get();

            InsnList insns = new InsnList();
            for (var tex : ADDITIONAL_TEXTURE_FORMATS) {
                if (!tex.hasColorAspect) continue;
                insns.add(new VarInsnNode(ALOAD, 0));
                insns.add(new FieldInsnNode(GETSTATIC, classNode.name, tex.name, desc));
                insns.add(new JumpInsnNode(IF_ACMPEQ, trueLabel));
            }
            hasColorAspect.instructions.insert(hasColorAspect.instructions.getFirst(), insns);
        }

        {
            MethodNode hasDepthAspect = classNode.methods.stream()
                .filter(m -> m.name.equals("hasDepthAspect")).findFirst().get();

            InsnNode const1 = (InsnNode) StreamSupport.stream(hasDepthAspect.instructions.spliterator(), false)
                .filter(insn -> insn.getOpcode() == ICONST_1).findFirst().get();
            LabelNode trueLabel = new LabelNode(new Label());
            hasDepthAspect.instructions.insertBefore(const1, trueLabel);

            InsnList insns = new InsnList();
            for (var tex : ADDITIONAL_TEXTURE_FORMATS) {
                if (!tex.hasDepthAspect) continue;
                insns.add(new VarInsnNode(ALOAD, 0));
                insns.add(new FieldInsnNode(GETSTATIC, classNode.name, tex.name, desc));
                insns.add(new JumpInsnNode(IF_ACMPEQ, trueLabel));
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

            for (var tex : ADDITIONAL_TEXTURE_FORMATS) {
                LabelNode label = new LabelNode(new Label());
                switchNode.labels.add(label);
                insns.add(label);
                insns.add(new LdcInsnNode(tex.glInternalFormat));
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

            for (var tex : ADDITIONAL_TEXTURE_FORMATS) {
                LabelNode label = new LabelNode(new Label());
                switchNode.labels.add(label);
                insns.add(label);
                insns.add(new LdcInsnNode(tex.glFormat));
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

            for (var tex : ADDITIONAL_TEXTURE_FORMATS) {
                LabelNode label = new LabelNode(new Label());
                switchNode.labels.add(label);
                insns.add(label);
                insns.add(new LdcInsnNode(tex.glType));
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

        FieldInsnNode lastTextureFormatInsn = null;
        int formatsCount = 0;

        for (var insn : clinit.instructions) {
            if (insn instanceof FieldInsnNode fieldInsn && fieldInsn.desc.equals("Lcom/mojang/blaze3d/textures/TextureFormat;")) {
                lastTextureFormatInsn = fieldInsn;
                formatsCount += 1;
            }
        }

        String textureFormatsOffsetFieldName = ((FieldInsnNode) lastTextureFormatInsn.getPrevious()).name;
        AbstractInsnNode astore_0 = lastTextureFormatInsn;
        while (!(astore_0 instanceof VarInsnNode varInsn && varInsn.getOpcode() == ASTORE && varInsn.var == 0)) {
            astore_0 = astore_0.getNext();
        }

        LabelNode label = (LabelNode) astore_0.getNext();

        for (var tex : ADDITIONAL_TEXTURE_FORMATS) {
            insns.add(new FieldInsnNode(GETSTATIC, classNode.name, textureFormatsOffsetFieldName, "[I"));
            insns.add(new FieldInsnNode(GETSTATIC, "com/mojang/blaze3d/textures/TextureFormat", tex.name, "Lcom/mojang/blaze3d/textures/TextureFormat;"));
            insns.add(new MethodInsnNode(INVOKEVIRTUAL, "com/mojang/blaze3d/textures/TextureFormat", "ordinal", "()I"));
            insns.add(new IntInsnNode(BIPUSH, formatsCount+1));
            insns.add(new InsnNode(IASTORE));
            formatsCount += 1;
        }

        clinit.instructions.insert(label, insns);
    }

}
