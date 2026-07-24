package fewizz.canpipe.b3d.mixin;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
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
    ByteBuffer patchSpirv(ByteBuffer spv) {
        var device = (VulkanDevice) ((GpuDeviceAccessor) RenderSystem.getDevice()).canpipe_getBackend();
        var attribs = ((VkDeviceAccessor) device).get_canpipe_expectedInputAttributes();
        if (attribs == null) {
            return spv;
        }

        IntBuffer spvi = spv.asIntBuffer();

        // 1. Search for `OpName`s (https://registry.khronos.org/SPIR-V/specs/unified1/SPIRV.html#OpName)

        Int2ObjectMap<String> idToName = new Int2ObjectOpenHashMap<>();

        for (int i = 5; i < spvi.capacity();) {
            int wordCountAndOp = spvi.get(i);

            int wordCount = wordCountAndOp >>> 16;
            int op = wordCountAndOp & ((1 << 16) - 1);

            if (op == 5) {
                int id = spvi.get(i+1);

                ByteList chars = new ByteArrayList();
                boolean done = false;

                for (int x = 2; !done && x < wordCount; ++x) {
                    int v = spvi.get(i+x);
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
        IntSet variablesIDsToRemoveFromEntryPoint = new IntOpenHashSet();

        for (int i = 5; i < spvi.capacity();) {
            int wordCountAndOp = spvi.get(i);

            int wordCount = wordCountAndOp >>> 16;
            int op = wordCountAndOp & ((1 << 16) - 1);

            if (op == 59) {
                int id = spvi.get(i+2);
                boolean isInput = spvi.get(i+3) == 1;
                String name = idToName.get(id);

                if (isInput && !attribs.contains(name)) {
                    spvi.put(i+3, 6);
                    typePointersIDsToPatch.add(spvi.get(i+1));
                    variablesIDsToRemoveFromEntryPoint.add(spvi.get(i+2));
                }
            }

            i += wordCount;
        }

        // 3. Patch `OpTypePointer`s (https://registry.khronos.org/SPIR-V/specs/unified1/SPIRV.html#OpTypePointer)

        for (int i = 5; i < spvi.capacity();) {
            int wordCountAndOp = spvi.get(i);

            int wordCount = wordCountAndOp >>> 16;
            int op = wordCountAndOp & ((1 << 16) - 1);

            if (op == 32) {
                int id = spvi.get(i+1);
                if (typePointersIDsToPatch.contains(id)) {
                    int storageClass = spvi.get(i+2);
                    if (storageClass != 1) { throw new RuntimeException("Expected storage class 1 (Input), but got "+storageClass); }
                    spvi.put(i+2, 6);
                }
            }

            i += wordCount;
        }

        // 4. Patch `OpEntryPoint`'s interface (https://registry.khronos.org/SPIR-V/specs/unified1/SPIRV.html#OpEntryPoint)

        for (int i = 5; i < spvi.capacity();) {
            int wordCountAndOp = spvi.get(i);

            int wordCount = wordCountAndOp >>> 16;
            int op = wordCountAndOp & ((1 << 16) - 1);

            if (op == 15) {
                int x = 3;
                for (; x < wordCount; ++x) {
                    int v = spvi.get(i+x);
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
                    int id = spvi.get(i+x);
                    if (!variablesIDsToRemoveFromEntryPoint.contains(id)) {
                        remainingIndices.add(id);
                    }
                }

                // Thank god we have `OpNop`, so I don't have to recreate IntBuffer
                // Moving remaining indices to the left, `OpNop`ing others
                for (int j = 0; beginning+j < x; ++j) {
                    spvi.put(
                        i+beginning+j,
                        j < remainingIndices.size() ? remainingIndices.getInt(j) : (1 << 16) | 0
                    );
                }

                // Patch instruction word count
                int newWordCount = wordCount - (x - beginning) + remainingIndices.size();
                spvi.put(i, (newWordCount << 16) | op);

                break;
            }

            i += wordCount;
        }

        return spv;
    }

}
