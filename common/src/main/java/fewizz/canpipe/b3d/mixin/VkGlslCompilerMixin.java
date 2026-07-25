package fewizz.canpipe.b3d.mixin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.glsl.GlslCompiler;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

@Mixin(GlslCompiler.class)
public class VkGlslCompilerMixin {

    @ModifyExpressionValue(
        method = "createIntermediary",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/util/shaderc/Shaderc;shaderc_result_get_error_message(J)Ljava/lang/String;"
        )
    )
    String onCompilationError(String message) {
        GpuDeviceBackend device = ((GpuDeviceAccessor) RenderSystem.getDevice()).canpipe_getBackend();
        ((VkDeviceAccessor) device).set_canpipe_compilationLog(message);
        return message;
    }

    @ModifyArg(
        method = "createIntermediary",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vulkan/glsl/IntermediaryShaderModule;createFromSpirv"
        ),
        index = 1
    )
    ByteBuffer patchSpirv(ByteBuffer spvBytes) {
        var device = (VulkanDevice) ((GpuDeviceAccessor) RenderSystem.getDevice()).canpipe_getBackend();
        var attribs = ((VkDeviceAccessor) device).get_canpipe_expectedInputAttributes();
        if (attribs == null) {
            return spvBytes;
        }

        IntBuffer spvWords = spvBytes.asIntBuffer();

        int bound = spvWords.get(3);

        List<int[]> instrs = new ArrayList<>();
        for (int i = 5; i < spvWords.capacity();) {
            int wordCountAndOp = spvWords.get(i);
            int wordCount = wordCountAndOp >>> 16;

            int[] instr = new int[wordCount];
            spvWords.get(i, instr);
            instrs.add(instr);

            i += wordCount;
        }

        // 1. Search for `OpName`s (https://registry.khronos.org/SPIR-V/specs/unified1/SPIRV.html#OpName)
        // and `OpTypePointer`s (https://registry.khronos.org/SPIR-V/specs/unified1/SPIRV.html#OpTypePointer)

        Int2ObjectMap<String> idToName = new Int2ObjectOpenHashMap<>();
        Int2ObjectMap<int[]> typeIdToInstr = new Int2ObjectOpenHashMap<>();

        for (int[] instr : instrs) {
            int wordCountAndOp = instr[0];

            int wordCount = wordCountAndOp >>> 16;
            int op = wordCountAndOp & ((1 << 16) - 1);

            if (op == 5) {
                int id = instr[1];

                ByteList chars = new ByteArrayList();
                boolean done = false;

                for (int x = 2; !done && x < wordCount; ++x) {
                    int v = instr[x];
                    for (int s = 0; s < 4; ++s) {
                        byte b = (byte)((v >> (s*8)) & 0xFF);
                        if (b == 0) {
                            done = true;
                            break;
                        }
                        chars.add(b);
                    }
                }

                String name;
                try {
                    name = new String(chars.toArray(new byte[0]), "utf-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }

                idToName.put(id, name);
            }

            if (op == 32) {
                int id = instr[1];
                typeIdToInstr.put(id, instr);
            }
        }

        // 2. Change storage class (https://registry.khronos.org/SPIR-V/specs/unified1/SPIRV.html#Storage_Class)
        // of unused input `OpVariable`s (https://registry.khronos.org/SPIR-V/specs/unified1/SPIRV.html#OpVariable)
        // from `Input` to `Private`
        // Also create new type ptrs

        IntSet variablesIDsToPatch = new IntOpenHashSet();

        for (int i = 0; i < instrs.size(); ++i) {
            int[] instr = instrs.get(i);

            int op = instr[0] & ((1 << 16) - 1);

            if (op == 59) {
                int id = instr[2];
                int typeId = instr[1];
                boolean isInput = instr[3] == 1;
                String name = idToName.get(id);

                if (isInput && !attribs.contains(name) && !name.startsWith("gl_")) {
                    instr[3] = 6;  // Private
                    variablesIDsToPatch.add(id);

                    int[] originalTypeInstr = typeIdToInstr.get(typeId);
                    int[] newTypeInstr = new int[originalTypeInstr.length];
                    System.arraycopy(originalTypeInstr, 0, newTypeInstr, 0, originalTypeInstr.length);

                    newTypeInstr[2] = 6;  // Private
                    newTypeInstr[1] = bound;
                    instr[1] = bound;  // Point to new type op
                    ++bound;

                    instrs.add(i, newTypeInstr);
                    ++i;
                }
            }
        }

        // 3. Patch `OpEntryPoint`'s interface (https://registry.khronos.org/SPIR-V/specs/unified1/SPIRV.html#OpEntryPoint)

        for (int i = 0; i < instrs.size(); ++i) {
            int[] instr = instrs.get(i);

            int op = instr[0] & ((1 << 16) - 1);

            if (op != 15) continue;

            int x = 3;
            for (; x < instr.length; ++x) {
                int v = instr[x];
                byte[] arr = new byte[]{
                    (byte)((v >>  0) & 0xFF),
                    (byte)((v >>  8) & 0xFF),
                    (byte)((v >> 16) & 0xFF),
                    (byte)((v >> 24) & 0xFF)
                };
                if (arr[0] == 0 || arr[1] == 0 || arr[2] == 0 || arr[3] == 0) { break; }
            }

            int beginning = x;
            IntList remainingIndices = new IntArrayList();

            for (; x < instr.length; ++x) {
                int id = instr[x];
                if (!variablesIDsToPatch.contains(id)) {
                    remainingIndices.add(id);
                }
            }

            // Patch instruction word count
            int newWordCount = instr.length - (x - beginning) + remainingIndices.size();

            int[] newInstr = new int[newWordCount];
            System.arraycopy(instr, 0, newInstr, 0, beginning);

            newInstr[0] = (newWordCount << 16) | op;

            for (int j = 0; beginning+j < x; ++j) {
                if (j < remainingIndices.size()) {
                    newInstr[beginning+j] = remainingIndices.getInt(j);
                }
            }

            instrs.set(i, newInstr);
            break;
        }

        // 4. Patch decorations

        for (int i = 0; i < instrs.size();) {
            int[] instr = instrs.get(i);

            int op = instr[0] & ((1 << 16) - 1);

            if (op == 71) {
                int id = instr[1];
                boolean isLocation = instr[2] == 30;
                if (isLocation && variablesIDsToPatch.contains(id)) {
                    instrs.remove(i);
                    continue;
                }
            }

            ++i;
        }

        int newSize = 5 + instrs.stream().mapToInt(i -> i.length).sum();

        if (newSize != spvWords.limit()) {
            ByteBuffer newSpvBytes = MemoryUtil.memAlloc(newSize*4);
            IntBuffer newSpvWords = newSpvBytes.asIntBuffer();

            newSpvWords.put(0, spvWords, 0, 5);
            newSpvWords.put(3, bound);

            int i = 5;
            for (int[] instr : instrs) {
                newSpvWords.put(i, instr);
                i += instr.length;
            }

            MemoryUtil.memFree(spvBytes);
            spvBytes = newSpvBytes;
        }

        var bytes = new byte[spvBytes.limit()];
        spvBytes.get(0, bytes);
        try {
            Files.write(Path.of("/tmp/vert.spv"), bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return spvBytes;
    }

}
