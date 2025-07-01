package fewizz.canpipe.mixin.m01_texture_formats;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.stream.StreamSupport;

import static org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_R3_G3_B2;
import static org.lwjgl.opengl.GL11C.GL_RED;
import static org.lwjgl.opengl.GL11C.GL_RGB10;
import static org.lwjgl.opengl.GL11C.GL_RGB10_A2;
import static org.lwjgl.opengl.GL11C.GL_RGB12;
import static org.lwjgl.opengl.GL11C.GL_RGB16;
import static org.lwjgl.opengl.GL11C.GL_RGB4;
import static org.lwjgl.opengl.GL11C.GL_RGB5;
import static org.lwjgl.opengl.GL11C.GL_RGB5_A1;
import static org.lwjgl.opengl.GL11C.GL_RGB8;
import static org.lwjgl.opengl.GL11C.GL_RGBA;
import static org.lwjgl.opengl.GL11C.GL_RGBA12;
import static org.lwjgl.opengl.GL11C.GL_RGBA16;
import static org.lwjgl.opengl.GL11C.GL_RGBA2;
import static org.lwjgl.opengl.GL11C.GL_RGBA4;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL14C.GL_DEPTH_COMPONENT32;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_COMPONENT32F;
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
import static org.lwjgl.opengl.GL31C.GL_R16_SNORM;
import static org.lwjgl.opengl.GL31C.GL_R8_SNORM;
import static org.lwjgl.opengl.GL31C.GL_RG16_SNORM;
import static org.lwjgl.opengl.GL31C.GL_RG8_SNORM;
import static org.lwjgl.opengl.GL31C.GL_RGB16_SNORM;
import static org.lwjgl.opengl.GL31C.GL_RGB8_SNORM;
import static org.lwjgl.opengl.GL31C.GL_RGBA8_SNORM;
import static org.lwjgl.opengl.GL33C.*;
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
     * internalFormat - will be used in {@link GlConst#toGlInternalId(TextureFormat)}, which in used in
     * {@link GlDevice#createTexture(String, TextureFormat, int, int, int)}
     * 
     * format - will be used in {@link GlConst#toGlExternalId(TextureFormat)}, which is used in
     * {@link GlCommandEncoder#copyTextureToBuffer(GpuTexture, GpuBuffer, int, Runnable, int, int, int, int, int)} and
     * {@link GlDevice#createTexture(String, TextureFormat, int, int, int)}
     * 
     * type - will be used in {@link GlConst#toGlType(TextureFormat)}, which is used in
     * {@link GlDevice#createTexture(String, TextureFormat, int, int, int)} and
     * {@link GlCommandEncoder#copyTextureToBuffer(GpuTexture, GpuBuffer, int, Runnable, int, int, int, int, int)}
     * 
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
     */
    public record TexFormat(
        int internalFormat,
        int format,
        int type,
        int pixelSize,
        boolean hasColorAspect,
        boolean hasDepthAspect
    ) {}

    private static final Map<String, TexFormat> ADDITIONAL_TEXTURE_FORMATS = new HashMap<>() {{
        put("DEPTH_COMPONENT32F", new TexFormat(GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT, GL_FLOAT, 1*4, false, true));
        put("DEPTH_COMPONENT", new TexFormat(GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT, GL_FLOAT, 1*4, false, true));
        put("DEPTH_COMPONENT32", new TexFormat(GL_DEPTH_COMPONENT32, GL_DEPTH_COMPONENT, GL_FLOAT, 1*4, false, true));  // already defined as DEPTH32

        put("RED", new TexFormat(GL_RED, GL_RED, GL_FLOAT, 1*4, true, false));
        // put("RED8", new TexFormat(GL_RED8, GL_RED, GL_FLOAT, 1*4, true, false));
        put("R8", new TexFormat(GL_R8, GL_RED, GL_UNSIGNED_BYTE, 1*1, true, false));
        put("R8_SNORM", new TexFormat(GL_R8_SNORM, GL_RED, GL_UNSIGNED_BYTE, 1*1, true, false));
        put("R16", new TexFormat(GL_R16, GL_RED, GL_UNSIGNED_BYTE, 1*1, true, false));
        put("R16_SNORM", new TexFormat(GL_R16_SNORM, GL_RED, GL_UNSIGNED_BYTE, 1*1, true, false));
        put("R16F", new TexFormat(GL_R16F, GL_RED, GL_FLOAT, 1*4, true, false));
        put("R32F", new TexFormat(GL_R32F, GL_RED, GL_FLOAT, 1*4, true, false));

        put("RG8", new TexFormat(GL_RG8, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RG8_SNORM", new TexFormat(GL_RG8_SNORM, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RG16", new TexFormat(GL_RG16, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RG16_SNORM", new TexFormat(GL_RG16_SNORM, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RG16F", new TexFormat(GL_RG16F, GL_RED, GL_FLOAT, 1*4, true, false));
        put("RG32F", new TexFormat(GL_RG32F, GL_RED, GL_FLOAT, 1*4, true, false));

        put("R3_G3_B2", new TexFormat(GL_R3_G3_B2, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGB4", new TexFormat(GL_RGB4, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGB5", new TexFormat(GL_RGB5, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGB8", new TexFormat(GL_RGB8, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGB8_SNORM", new TexFormat(GL_RGB8_SNORM, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGB10", new TexFormat(GL_RGB10, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGB12", new TexFormat(GL_RGB12, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGB16", new TexFormat(GL_RGB16, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGB16_SNORM", new TexFormat(GL_RGB16_SNORM, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGB32UI", new TexFormat(GL_RGB32UI, GL_RGBA_INTEGER, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGB16F", new TexFormat(GL_RGB16F, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGB32F", new TexFormat(GL_RGB32F, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("R11F_G11F_B10F", new TexFormat(GL_R11F_G11F_B10F, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));

        put("RGBA2", new TexFormat(GL_RGBA2, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGBA4", new TexFormat(GL_RGBA4, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGB5_A1", new TexFormat(GL_RGB5_A1, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        // put("RGBA8", new TexFormat(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false)); already defined
        put("RGBA8_SNORM", new TexFormat(GL_RGBA8_SNORM, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGB10_A2", new TexFormat(GL_RGB10_A2, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGB10_A2UI", new TexFormat(GL_RGB10_A2UI, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGBA12", new TexFormat(GL_RGBA12, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGBA16", new TexFormat(GL_RGBA16, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGBA16F", new TexFormat(GL_RGBA16F, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
        put("RGBA32F", new TexFormat(GL_RGBA32F, GL_RGBA, GL_UNSIGNED_BYTE, 4*1, true, false));
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
        if (mixinClassName.equals("fewizz.canpipe.mixin.m01_texture_formats.TextureFormatMixin")) {
            patchTextureFormat(targetClass);
        } else
        if (mixinClassName.equals("fewizz.canpipe.mixin.m01_texture_formats.GlConstMixin")) {
            patchGlConst(targetClass);
        } else
        if (mixinClassName.equals("fewizz.canpipe.mixin.m01_texture_formats.GlConstSwitchMetaMixin")) {
            patchGlConst$1(targetClass);
        }
    }

    private static void patchTextureFormat(ClassNode classNode) {
        // hard way, adding new texture types into enum

        String desc = "L"+classNode.name+";";

        // $values
        MethodNode arrayInitMethod = classNode.methods.stream()
            .filter(m -> !m.name.equals("values") && m.desc.equals("()["+desc)).findFirst().get();

        MethodNode classInitMethod = classNode.methods.stream()
            .filter(m -> m.name.equals("<clinit>")).findFirst().get();

        TypeInsnNode aNewArrayInsn = (TypeInsnNode) StreamSupport.stream(arrayInitMethod.instructions.spliterator(), false)
            .filter(insn->insn.getOpcode() == ANEWARRAY).findFirst().get();

        AbstractInsnNode sizeArgInsn = (AbstractInsnNode) aNewArrayInsn.getPrevious();

        int i = switch (sizeArgInsn.getOpcode()) {
            case ICONST_3 -> 3;  // should be 3
            case ICONST_4 -> 4;
            case ICONST_5 -> 5;  // Neo adds DEPTH24_STENCIL8 and DEPTH32F_STENCIL8
            case BIPUSH -> ((IntInsnNode) sizeArgInsn).operand;
            case SIPUSH -> ((IntInsnNode) sizeArgInsn).operand;
            default -> -1;
        };

        if (i == -1) {
            throw new RuntimeException("Unexpected texture formats count");
        }

        arrayInitMethod.instructions.insert(sizeArgInsn, new IntInsnNode(SIPUSH, i + ADDITIONAL_TEXTURE_FORMATS.size()));
        arrayInitMethod.instructions.remove(sizeArgInsn);

        for (var e : ADDITIONAL_TEXTURE_FORMATS.entrySet()) {
            classNode.fields.add(new FieldNode(ACC_PUBLIC | ACC_FINAL | ACC_STATIC | ACC_ENUM, e.getKey(), desc, null, null));

            InsnList createNewEntry = new InsnList();
            createNewEntry.add(new TypeInsnNode(NEW, classNode.name));
            createNewEntry.add(new InsnNode(DUP));
            createNewEntry.add(new LdcInsnNode(e.getKey()));
            createNewEntry.add(new IntInsnNode(BIPUSH, i));
            createNewEntry.add(new IntInsnNode(BIPUSH, Integer.valueOf(e.getValue().pixelSize)));
            createNewEntry.add(new MethodInsnNode(INVOKESPECIAL, classNode.name, "<init>", "(Ljava/lang/String;II)V"));
            createNewEntry.add(new FieldInsnNode(PUTSTATIC, classNode.name, e.getKey(), desc));

            classInitMethod.instructions.insertBefore(
                StreamSupport.stream(classInitMethod.instructions.spliterator(), false)
                    .filter(insn -> insn.getOpcode() == INVOKESTATIC)
                    .findFirst().get(),  // right before callogin arrayInitMethod ($values)
                createNewEntry
            );

            InsnList addNewEntry = new InsnList();
            addNewEntry.add(new InsnNode(DUP)); // dup array
            addNewEntry.add(new IntInsnNode(BIPUSH, i));
            addNewEntry.add(new FieldInsnNode(GETSTATIC, classNode.name, e.getKey(), desc));
            addNewEntry.add(new InsnNode(AASTORE));
            arrayInitMethod.instructions.insertBefore(
                StreamSupport.stream(arrayInitMethod.instructions.spliterator(), false)
                    .filter(insn -> insn.getOpcode() == ARETURN)
                    .findFirst().get(),  // before return
                addNewEntry
            );

            ++i;
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
            for (var e : ADDITIONAL_TEXTURE_FORMATS.entrySet()) {
                if (!e.getValue().hasColorAspect) continue;
                insns.add(new VarInsnNode(ALOAD, 0));
                insns.add(new FieldInsnNode(GETSTATIC, classNode.name, e.getKey(), desc));
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
            for (var e : ADDITIONAL_TEXTURE_FORMATS.entrySet()) {
                if (!e.getValue().hasDepthAspect) continue;
                insns.add(new VarInsnNode(ALOAD, 0));
                insns.add(new FieldInsnNode(GETSTATIC, classNode.name, e.getKey(), desc));
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

        String textureFormatsOffsetFieldName =
            ((FieldInsnNode) StreamSupport.stream(clinit.instructions.spliterator(), false)
                .filter(insn -> insn instanceof FieldInsnNode fin && fin.name.equals("RGBA8"))
                .findFirst().get().getPrevious()
            ).name;

        int offset = 4;
        for (int i = offset; i < ADDITIONAL_TEXTURE_FORMATS.size()+offset; ++i) {
            insns.add(new FieldInsnNode(GETSTATIC, classNode.name, textureFormatsOffsetFieldName, "[I"));
            insns.add(new IntInsnNode(BIPUSH, i));
            insns.add(new IntInsnNode(BIPUSH, i+1));
            insns.add(new InsnNode(IASTORE));
        }
        clinit.instructions.insertBefore(clinit.instructions.getLast(), insns);
    }

}
