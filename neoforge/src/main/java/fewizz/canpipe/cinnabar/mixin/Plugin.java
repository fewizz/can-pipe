package fewizz.canpipe.cinnabar.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.lwjgl.vulkan.VK12;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.google.common.collect.Streams;

public class Plugin implements IMixinConfigPlugin, Opcodes {

    public record TexFormat(
        String name,  // enum name
        int vkCode
    ) {}

    /**
     * Copy of {@link fewizz.canpipe.b3d.mixin.Plugin#ADDITIONAL_TEXTURE_FORMATS}
     **/
    private static final List<TexFormat> ADDITIONAL_TEXTURE_FORMATS = new ArrayList<>() {{
        add(new TexFormat("R8_UNORM", VK12.VK_FORMAT_R8_UNORM));
        add(new TexFormat("R8_SNORM", VK12.VK_FORMAT_R8_SNORM));
        add(new TexFormat("R16_UNORM", VK12.VK_FORMAT_R16_UNORM));
        add(new TexFormat("R16_SNORM", VK12.VK_FORMAT_R16_SNORM));
        add(new TexFormat("R16_SFLOAT", VK12.VK_FORMAT_R16_SFLOAT));
        add(new TexFormat("R32_SFLOAT", VK12.VK_FORMAT_R32_SFLOAT));

        add(new TexFormat("RG8_UNORM", VK12.VK_FORMAT_R8G8_UNORM));
        add(new TexFormat("RG8_SNORM", VK12.VK_FORMAT_R8G8_SNORM));
        add(new TexFormat("RG16_UNORM", VK12.VK_FORMAT_R16G16_UNORM));
        add(new TexFormat("RG16_SNORM", VK12.VK_FORMAT_R16G16_SNORM));
        add(new TexFormat("RG16_SFLOAT", VK12.VK_FORMAT_R16G16_SFLOAT));
        add(new TexFormat("RG32_SFLOAT", VK12.VK_FORMAT_R32G32_SFLOAT));

        add(new TexFormat("RGB8_UNORM", VK12.VK_FORMAT_R8G8B8_UNORM));
        add(new TexFormat("RGB8_SNORM", VK12.VK_FORMAT_R8G8B8_SNORM));
        add(new TexFormat("RGB16_UNORM", VK12.VK_FORMAT_R16G16B16_UNORM));
        add(new TexFormat("RGB16_SNORM", VK12.VK_FORMAT_R16G16B16_SNORM));
        add(new TexFormat("RGB32_UINT", VK12.VK_FORMAT_R32G32B32_UINT));
        add(new TexFormat("RGB16_SFLOAT", VK12.VK_FORMAT_R16G16B16_SFLOAT));
        add(new TexFormat("RGB32_SFLOAT", VK12.VK_FORMAT_R32G32B32_SFLOAT));
        add(new TexFormat("B10G11R11_UFLOAT_PACK32", VK12.VK_FORMAT_B10G11R11_UFLOAT_PACK32));

        // add(new TexFormat("RGBA8_UNORM", ...); already defined as RGBA8
        add(new TexFormat("RGBA8_SNORM", VK12.VK_FORMAT_R8G8B8A8_UNORM));
        add(new TexFormat("R12X4G12X4B12X4A12X4_UNORM_4PACK16", VK12.VK_FORMAT_R12X4G12X4B12X4A12X4_UNORM_4PACK16));
        add(new TexFormat("RGBA16_UNORM", VK12.VK_FORMAT_R16G16B16A16_UNORM));
        add(new TexFormat("RGBA16_SFLOAT", VK12.VK_FORMAT_R16G16B16A16_SFLOAT));
        add(new TexFormat("RGBA32_SFLOAT", VK12.VK_FORMAT_R32G32B32A32_SFLOAT));
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
            case "fewizz.canpipe.cinnabar.mixin.CinnabarGpuTextureMixin" -> patchCinnabarGpuTexture(targetClass);
            case "fewizz.canpipe.cinnabar.mixin.CinnabarGpuTextureSwitchMetaMixin" -> patchCinnabarGpuTexture$1(targetClass);
        }
    }

    void patchCinnabarGpuTexture(ClassNode classNode) {
        MethodNode toVkMethod = classNode.methods.stream()
            .filter(m -> m.name.equals("toVk")).findFirst().get();

        TableSwitchInsnNode switchNode = (TableSwitchInsnNode) StreamSupport.stream(
            toVkMethod.instructions.spliterator(), false
        ).filter(insn -> insn.getOpcode() == TABLESWITCH).findFirst().get();

        LabelNode beforeReturn = (LabelNode) Streams.findLast(
            StreamSupport.stream(toVkMethod.instructions.spliterator(), false)
            .takeWhile(insn -> insn.getOpcode() != IRETURN)
            .filter(insn -> insn instanceof LabelNode)
        ).get();

        InsnList insns = new InsnList();
        insns.add(new JumpInsnNode(GOTO, beforeReturn));

        for (var tex : ADDITIONAL_TEXTURE_FORMATS) {
            LabelNode label = new LabelNode(new Label());
            switchNode.labels.add(label);
            insns.add(label);
            insns.add(new LdcInsnNode(tex.vkCode));
            insns.add(new JumpInsnNode(GOTO, beforeReturn));
        }
        switchNode.max += ADDITIONAL_TEXTURE_FORMATS.size();
        toVkMethod.instructions.insertBefore(beforeReturn, insns);
    }

    void patchCinnabarGpuTexture$1(ClassNode classNode) {
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
