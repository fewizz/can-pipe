package fewizz.canpipe.b3d.mixin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.google.common.base.Utf8;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.glsl.GlslCompiler;

import fewizz.canpipe.b3d.GpuDeviceExtended;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

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

        // 1. Search for `OpName`s (https://registry.khronos.org/SPIR-V/specs/unified1/SPIRV.html#OpName)

        Int2ObjectMap<String> idToName = new Int2ObjectOpenHashMap<>();

        for (int i = 5; i < spvWords.capacity();) {
            int wordCountAndOp = spvWords.get(i);

            int wordCount = wordCountAndOp >>> 16;
            int op = wordCountAndOp & ((1 << 16) - 1);

            if (op == 5) {
                int id = spvWords.get(i+1);

                ByteList chars = new ByteArrayList();
                boolean done = false;

                for (int x = 2; !done && x < wordCount; ++x) {
                    int v = spvWords.get(i+x);
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

            i += wordCount;
        }

        // 2. Change storage class (https://registry.khronos.org/SPIR-V/specs/unified1/SPIRV.html#Storage_Class)
        // of unused input `OpVariable`s (https://registry.khronos.org/SPIR-V/specs/unified1/SPIRV.html#OpVariable)
        // from `Input` to `Private`

        IntSet typePointersIDsToPatch = new IntOpenHashSet();
        IntSet variablesIDsToPatch = new IntOpenHashSet();

        for (int i = 5; i < spvWords.capacity();) {
            int wordCountAndOp = spvWords.get(i);

            int wordCount = wordCountAndOp >>> 16;
            int op = wordCountAndOp & ((1 << 16) - 1);

            if (op == 59) {
                int id = spvWords.get(i+2);
                boolean isInput = spvWords.get(i+3) == 1;
                String name = idToName.get(id);

                if (isInput && !attribs.contains(name)) {
                    spvWords.put(i+3, 6);
                    typePointersIDsToPatch.add(spvWords.get(i+1));
                    variablesIDsToPatch.add(spvWords.get(i+2));
                }
            }

            i += wordCount;
        }

        // 3. Patch `OpTypePointer`s (https://registry.khronos.org/SPIR-V/specs/unified1/SPIRV.html#OpTypePointer)

        for (int i = 5; i < spvWords.capacity();) {
            int wordCountAndOp = spvWords.get(i);

            int wordCount = wordCountAndOp >>> 16;
            int op = wordCountAndOp & ((1 << 16) - 1);

            if (op == 32) {
                int id = spvWords.get(i+1);
                if (typePointersIDsToPatch.contains(id)) {
                    int storageClass = spvWords.get(i+2);
                    if (storageClass != 1) { throw new RuntimeException("Expected storage class 1 (Input), but got "+storageClass); }
                    spvWords.put(i+2, 6);
                }
            }

            i += wordCount;
        }

        // 4. Patch `OpEntryPoint`'s interface (https://registry.khronos.org/SPIR-V/specs/unified1/SPIRV.html#OpEntryPoint)

        IntSet wordsToSkip = new IntOpenHashSet();

        for (int i = 5; i < spvWords.capacity();) {
            int wordCountAndOp = spvWords.get(i);

            int wordCount = wordCountAndOp >>> 16;
            int op = wordCountAndOp & ((1 << 16) - 1);

            if (op == 15) {
                int x = 3;
                for (; x < wordCount; ++x) {
                    int v = spvWords.get(i+x);
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

                for (; x < wordCount; ++x) {
                    int id = spvWords.get(i+x);
                    if (!variablesIDsToPatch.contains(id)) {
                        remainingIndices.add(id);
                    }
                }

                // Patch instruction word count
                int newWordCount = wordCount - (x - beginning) + remainingIndices.size();
                spvWords.put(i, (newWordCount << 16) | op);

                for (int j = 0; beginning+j < x; ++j) {
                    if (j < remainingIndices.size()) {
                        spvWords.put(i+beginning+j, remainingIndices.getInt(j));
                    }
                    else {
                        wordsToSkip.add(i+beginning+j);
                    }
                }

                break;
            }

            i += wordCount;
        }

        // 5. Patch decorations

        for (int i = 5; i < spvWords.capacity();) {
            if (wordsToSkip.contains(i)) {
                ++i;
                continue;
            }

            int wordCountAndOp = spvWords.get(i);

            int wordCount = wordCountAndOp >>> 16;
            int op = wordCountAndOp & ((1 << 16) - 1);

            if (op == 71) {
                int id = spvWords.get(i+1);
                boolean isLocation = spvWords.get(i+2) == 30;
                if (isLocation && variablesIDsToPatch.contains(id)) {
                    for (int x = 0; x < wordCount; ++x) wordsToSkip.add(i+x);
                }
            }

            i += wordCount;
        }

        int newSize = spvWords.limit() - wordsToSkip.size();

        if (newSize != spvWords.limit()) {
            ByteBuffer newSpvBytes = MemoryUtil.memAlloc(newSize*4);
            IntBuffer newSpvWords = newSpvBytes.asIntBuffer();

            for (int i = 0, j = 0; i < spvWords.limit(); ++i) {
                int word = spvWords.get(i);
                if (!wordsToSkip.contains(i)) {
                    newSpvWords.put(j, word);
                    ++j;
                }
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
