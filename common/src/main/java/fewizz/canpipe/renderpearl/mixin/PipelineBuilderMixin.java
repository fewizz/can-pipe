package fewizz.canpipe.renderpearl.mixin;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.api.SpvModule;
import com.mojang.renderpearl.frontend.shaders.GlslCompiler;
import com.mojang.renderpearl.frontend.shaders.PipelineBuilder;
import com.mojang.renderpearl.frontend.shaders.SPIRVModule;
import com.mojang.renderpearl.util.ShaderCompileException;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.renderpearl.GlslCompilerExtended;
import fewizz.canpipe.renderpearl.PipelineBuilderExtended;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.resources.Identifier;

@Mixin(PipelineBuilder.class)
public abstract class PipelineBuilderMixin implements PipelineBuilderExtended {

    @Shadow @Final private GlslCompiler compiler;

    @Shadow abstract public CompletableFuture<CompiledRenderPipeline.Pending> compilePipeline(RenderPipeline pipeline, ShaderSource shaderSource, Executor executor);
    @Shadow private BackendRenderPipeline.CreateInfo generateBackendCreateInfo(RenderPipeline pipeline, ShaderSource shaderSource, ReferenceArrayList<BackendRenderPipeline.CreateInfo.Shader> shaderCreateInfos, Object2IntOpenHashMap<String> uniformBindings) { return null; }

    @Unique private ThreadLocal<Consumer<SpvModule.Reflection.Descriptor>> canpipe_onUnhandledUniform = new ThreadLocal<>();
    @Unique private ThreadLocal<Boolean> canpipe_computeLocations = new ThreadLocal<>();
    @Unique private ThreadLocal<TriConsumer<String, Identifier, String>> canpipe_spvCompilationErrorListener = new ThreadLocal<>();

    @Override
    public CompletableFuture<CompiledRenderPipeline.Pending> canpipe_compilePipeline(
        RenderPipeline pipeline, ShaderSource shaderSource, Executor executor,
        @Nullable Consumer<SpvModule.Reflection.Descriptor> onUnhandledUniform,
        boolean computeLocations,
        TriConsumer<String, Identifier, String> spvCompilationErrorListener
    ) {
        try {
            this.canpipe_onUnhandledUniform.set(onUnhandledUniform);
            this.canpipe_computeLocations.set(computeLocations);
            this.canpipe_spvCompilationErrorListener.set(spvCompilationErrorListener);
            return this.compilePipeline(pipeline, shaderSource, executor);
        }
        finally {
            this.canpipe_onUnhandledUniform.remove();
            this.canpipe_computeLocations.remove();
            this.canpipe_spvCompilationErrorListener.remove();
        }
    }

    @Override
    public BackendRenderPipeline.CreateInfo canpipe_generateBackendCreateInfo(
        RenderPipeline pipeline, ShaderSource shaderSource,
        ReferenceArrayList<BackendRenderPipeline.CreateInfo.Shader> shaderCreateInfos,
        Object2IntOpenHashMap<String> uniformBindings,
        boolean computeLocations,  // added
        TriConsumer<String, Identifier, String> spvCompilationErrorListener
    ) {
        try {
            this.canpipe_computeLocations.set(computeLocations);
            this.canpipe_spvCompilationErrorListener.set(spvCompilationErrorListener);
            return this.generateBackendCreateInfo(pipeline, shaderSource, shaderCreateInfos, uniformBindings);
        }
        finally {
            this.canpipe_computeLocations.remove();
            this.canpipe_spvCompilationErrorListener.remove();
        }
    }

    @ModifyExpressionValue(
        method = "generateBackendCreateInfo",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/renderpearl/backend/api/SpvModule$Reflection;descriptors()Ljava/util/List;"
        )
    )
    List<SpvModule.Reflection.Descriptor> handleUndefinedUniform(
        List<SpvModule.Reflection.Descriptor> result,
        @Local Map<String, BindGroupLayout.UniformDescription> pipelineUniformsByName
    ) {
        var handler = this.canpipe_onUnhandledUniform.get();
        if (handler != null) {
            result = result.stream().filter(d -> {
                if (!pipelineUniformsByName.containsKey(d.name())) {
                    handler.accept(d);
                    return false;
                }
                return true;
            }).toList();
        }
        return result;
    }

    @Inject(
        method = "generateBackendCreateInfo",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/renderpearl/frontend/shaders/GlslCompiler;compileToSpv"
        )
    )
    void extendedCompileToSpv(CallbackInfoReturnable<BackendRenderPipeline.CreateInfo> ci) {
        Boolean computeLocations = this.canpipe_computeLocations.get();
        if (computeLocations != null) ((GlslCompilerExtended) this.compiler).canpipe_setEnableAutoMapLocationsForPendingCompilation(computeLocations);

        var listener = this.canpipe_spvCompilationErrorListener.get();
        if (listener != null) ((GlslCompilerExtended) this.compiler).canpipe_setCompilationErrorListenerForPendingCompilation(listener);
    }

    @ModifyArg(
        method = "compilePipeline",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/concurrent/CompletableFuture;supplyAsync"
        ),
        index = 0
    )
    Supplier<CompiledRenderPipeline.Pending> onSupplyAsync(Supplier<CompiledRenderPipeline.Pending> supplier) {
        Boolean computeLocations = this.canpipe_computeLocations.get();
        TriConsumer<String, Identifier, String> spvCompilationErrorListener = this.canpipe_spvCompilationErrorListener.get();
        return () -> {
            this.canpipe_computeLocations.set(computeLocations);
            this.canpipe_spvCompilationErrorListener.set(spvCompilationErrorListener);
            try {
                return supplier.get();
            }
            finally {
                this.canpipe_computeLocations.remove();
                this.canpipe_spvCompilationErrorListener.remove();
            }
        };
    }

    @Inject(
        method = "generateBackendCreateInfo",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/objects/ReferenceArrayList;<init>",
            ordinal = 0
        )
    )
    void fixVertexShaderLocationsInSpirv(
        CallbackInfoReturnable<BackendRenderPipeline.CreateInfo> cir,
        @Local EnumMap<ShaderType, SpvModule> shaderSources,
        @Local ReferenceArrayList<BackendRenderPipeline.CreateInfo.Shader> shaderCreateInfos,
        @Local RenderPipeline pipeline
    ) throws ShaderCompileException {
        Boolean computeLocations = this.canpipe_computeLocations.get();
        if (computeLocations == null || !computeLocations) {
            return;
        }

        var vertexSpv = shaderSources.get(ShaderType.VERTEX);
        var fragmentSpv = shaderSources.get(ShaderType.FRAGMENT);

        var inputVariables = vertexSpv.reflect().outputs().stream().map(v -> v.name()).collect(Collectors.toSet());
        var extraInputs = fragmentSpv.reflect().inputs().stream().map(v -> v.name()).collect(Collectors.toSet());
        extraInputs.removeAll(inputVariables);

        if (!extraInputs.isEmpty()) {
            CanPipe.LOGGER.warn("Shader \""+pipeline.getShaders().get(ShaderType.FRAGMENT)+"\" expects input variables which are not provided: "+extraInputs);

            var prevFragmentSpv = fragmentSpv;
            ByteBuffer newSpv = this.canpipe_disableExtraInterfaceVariablesInFragmentShader(prevFragmentSpv.spv(), extraInputs);
            SpvModule newSpvModule = new SPIRVModule(newSpv, ShaderType.FRAGMENT);

            shaderSources.put(ShaderType.FRAGMENT, newSpvModule);
            BackendRenderPipeline.CreateInfo.Shader prevSci = shaderCreateInfos.stream().filter(sci -> sci.module() == prevFragmentSpv).findFirst().get();
            shaderCreateInfos.removeIf(sci -> sci.module() == prevFragmentSpv);
            shaderCreateInfos.add(new BackendRenderPipeline.CreateInfo.Shader(prevSci.name(), prevSci.entryPoint(), newSpvModule));

            // delete previous one
            fragmentSpv.close();
        }

        vertexSpv = shaderSources.get(ShaderType.VERTEX);
        fragmentSpv = shaderSources.get(ShaderType.FRAGMENT);

        {
            var vertexOutputs = vertexSpv.reflect().outputs().stream().sorted((v0, v1) -> v0.location() - v1.location()).toList();
            var fragmentInputs = fragmentSpv.reflect().inputs().stream().sorted((v0, v1) -> v0.location() - v1.location()).toList();

            var vertexOutputsByName = vertexOutputs.stream().collect(Collectors.toMap(v -> v.name(), v -> v));
            var fragmentInputsByName = fragmentInputs.stream().collect(Collectors.toMap(v -> v.name(), v -> v));

            var commonVars = vertexOutputs.stream().filter(v -> fragmentInputsByName.containsKey(v.name())).map(v -> v.name()).toList();
            var vertexOutputsToIgnore = vertexOutputs.stream().filter(v -> !fragmentInputsByName.containsKey(v.name())).map(v -> v.name()).toList();
            var fragmentInputsToIgnore = fragmentInputs.stream().filter(v -> !vertexOutputsByName.containsKey(v.name())).map(v -> v.name()).toList();

            Function<SpvModule.Reflection.InterfaceVariable, Integer> interfaceVarSlots = v -> {
                int slots = 1;
                for (int d = 0; d < v.type().arrayDimensions(); ++d) {
                    slots *= v.type().arrayLength(d);
                }
                return slots;
            };

            int location = 0;

            for (var commonVarName : commonVars) {
                var v = vertexOutputsByName.get(commonVarName);
                v.location(location);
                fragmentInputsByName.get(commonVarName).location(location);
                location += interfaceVarSlots.apply(v);
            }

            int vertexLocation = location;

            for (var vertexVarName : vertexOutputsToIgnore) {
                var v = vertexOutputsByName.get(vertexVarName);
                v.location(vertexLocation);
                vertexLocation += interfaceVarSlots.apply(v);
            }

            int fragmentLocation = location;

            for (var fragmentVarName : fragmentInputsToIgnore) {
                var v = fragmentInputsByName.get(fragmentVarName);
                v.location(fragmentLocation);
                fragmentLocation += interfaceVarSlots.apply(v);
            }
        }

    }

    @Unique
    ByteBuffer canpipe_disableExtraInterfaceVariablesInFragmentShader(
        ByteBuffer fragmentSpv, Set<String> extraVars
    ) {
        IntBuffer spvWords = fragmentSpv.asIntBuffer();

        int bound = spvWords.get(3);

        List<int[]> instrs = new ArrayList<>();
        for (int i = 5; i < spvWords.limit();) {
            int wordCountAndOp = spvWords.get(i);
            int wordCount = wordCountAndOp >>> 16;

            int[] instr = new int[wordCount];
            spvWords.get(i, instr);
            instrs.add(instr);

            i += wordCount;
            if (wordCount == 0) {
                throw new RuntimeException("Word count is 0?!");
            }
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

                if (isInput && extraVars.contains(name)) {
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

        int newSizeWords = 5 + instrs.stream().mapToInt(i -> i.length).sum();

        ByteBuffer newSpv = MemoryUtil.memAlloc(newSizeWords*Integer.BYTES);
        IntBuffer newSpvWords = newSpv.asIntBuffer();

        for (int i = 0; i < 5; ++i) {
            newSpvWords.put(i, spvWords.get(i));
        }
        newSpvWords.put(3, bound);  // Bound is probably increased

        int i = 5;
        for (int[] instr : instrs) {
            newSpvWords.put(i, instr);
            i += instr.length;
        }

        return newSpv;
    }

}
