package fewizz.canpipe.b3d.mixin;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout;
import com.mojang.blaze3d.vulkan.glsl.GlslCompiler;
import com.mojang.blaze3d.vulkan.glsl.IntermediaryShaderModule;

import fewizz.canpipe.CanPipe;
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

    @WrapOperation(
        method = "createIntermediary",
        at = @At(value = "INVOKE", target = "org.lwjgl.system.MemoryUtil.memCalloc")
    )
    ByteBuffer onAllocMemoryForSpirv(int size, Operation<ByteBuffer> operation) {
        // Make byte buffer for spirv slightly bigger
        // to be able to add more (256/4 = 64, should be enough) `OpTypeFunction`s if needed, for spirv patches below.
        // Cursed. But I can't resize IntermediaryShaderModule.spirv, because IntermediaryShaderModule is a record
        // and org.lwjgl.system.MemoryUtil has no methods to change address of existing ByteBuffer (and that's good)
        // (result of MemoryUtil.memRealloc() may point to a different address)
        ByteBuffer result = operation.call(size + 256);
        result.limit(size);
        return result;
    }

    @WrapOperation(
        method = "compile",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vulkan/glsl/IntermediaryShaderModule;rebind",
            ordinal = 1  // fragment shader
        )
    )
    void patchSpirvBeforeFramentShaderRebind(
        IntermediaryShaderModule fragment,
        List<String> inputVariables,
        List<VulkanBindGroupLayout.Entry> entries,
        Operation<Void> operation,
        @Share("originalFragmentShaderSpirvHolder") LocalRef<ByteBuffer> originalFragmentShaderSpirvHolder
    ) {
        var extraInputs = fragment.inputs().stream().map(v -> v.name()).collect(Collectors.toSet());
        extraInputs.removeAll(inputVariables);
        if (extraInputs.isEmpty()) {
            operation.call(fragment, inputVariables, entries);
            return;
        }

        {
            // avoid `!remainingInputs.isEmpty()` validation in IntermediaryShaderModule.rebind()
            var originalInputs = new ArrayList<>(fragment.inputs());
            fragment.inputs().removeIf(v -> extraInputs.contains(v.name()));
            operation.call(fragment, inputVariables, entries);
            // restore
            fragment.inputs().clear();
            fragment.inputs().addAll(originalInputs);
        }

        originalFragmentShaderSpirvHolder.set(MemoryUtil.memDuplicate(fragment.spirv()));

        IntBuffer spvWords = fragment.spirv().asIntBuffer();

        int bound = spvWords.get(3);

        List<int[]> instrs = new ArrayList<>();
        for (int i = 5; i < spvWords.limit();) {
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

                if (isInput && !inputVariables.contains(name) && !name.startsWith("gl_")) {
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

        // 5. Recreate spirv ByteBuffer

        spvWords.put(3, bound);  // Bound is probably increased

        int newSize = 5 + instrs.stream().mapToInt(i -> i.length).sum();
        spvWords.limit(newSize);  // :pray:
        fragment.spirv().limit(newSize*4);

        int i = 5;
        for (int[] instr : instrs) {
            spvWords.put(i, instr);
            i += instr.length;
        }

        CanPipe.LOGGER.warn("Shader \""+fragment.name()+"\" expects input variables which are not provided: "+extraInputs);
    }

    @Inject(
        method = "compile",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vulkan/glsl/IntermediaryShaderModule;createVulkanShaderModule",
            ordinal = 1,  // fragment shader
            shift = Shift.AFTER
        )
    )
    void restoreSpirv(
        CallbackInfoReturnable<Object> ci,
        @Share("originalFragmentShaderSpirvHolder") LocalRef<ByteBuffer> originalFragmentShaderSpirvHolder,
        @Local(name = "fragment") IntermediaryShaderModule fragment
    ) {
        var originalSpirv = originalFragmentShaderSpirvHolder.get();
        if (originalSpirv != null) {
            fragment.spirv().limit(originalSpirv.limit());
            MemoryUtil.memCopy(originalSpirv, fragment.spirv());
        }
    }


    @ModifyExpressionValue(
        method = "addToBindGroup",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/stream/Stream;noneMatch",
            ordinal = 2  // samplers
        )
    )
    private static boolean dontCrashIfSamplerNotFound(
        boolean noSampler,
        @Share("entriesToFilterOut") LocalRef<Set<String>> entriesToFilterOut,
        @Local(name="name") String name,
        @Local IntermediaryShaderModule shader,
        @Local RenderPipeline pipeline
    ) {
        if (noSampler) {
            var entries = entriesToFilterOut.get();
            if (entries == null) {
                entries = new HashSet<>();
                entriesToFilterOut.set(entries);
            }
            entriesToFilterOut.get().add(name);

            CanPipe.LOGGER.warn("Unable to find sampler \""+name+"\" in shader \""+shader.name()+"\" of pipeline \""+pipeline.getLocation()+"\"");
        }
        return false;
    }

    @Inject(
        method = "addToBindGroup",
        at = @At("RETURN")
    )
    private static void afterAddToBindGroup(
        CallbackInfo ci,
        @Share("entriesToFilterOut") LocalRef<Set<String>> entriesToFilterOut,
        @Local(name="entries") List<VulkanBindGroupLayout.Entry> entries
    ) {
        var es = entriesToFilterOut.get();
        if (es != null) {
            entries.removeIf(e -> es.contains(e.name()));
        }
    }

}
