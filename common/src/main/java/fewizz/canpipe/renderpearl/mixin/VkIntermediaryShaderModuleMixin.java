package fewizz.canpipe.renderpearl.mixin;

import fewizz.canpipe.renderpearl.GpuShaderModule;

// @Mixin(IntermediaryShaderModule.class)  // TODO
public class VkIntermediaryShaderModuleMixin implements GpuShaderModule {
/*
    @Inject(
        method = "createFromSpirv",
        at = @At(value = "INVOKE", target = "Ljava/util/List;add", ordinal = 2, shift = Shift.AFTER)
    )
    private static void fixOutputVarLocation(
        CallbackInfoReturnable<Object> ci,
        @Local SpvcReflectedResource resource,
        @Local(name = "compiler") long compiler,
        @Local(name = "outputs") List<SpvVariable> outputs
    ) {
        canpipe_fixVarWithMultipleLocations(resource, compiler, outputs);
    }

    @Inject(
        method = "createFromSpirv",
        at = @At(value = "INVOKE", target = "Ljava/util/List;add", ordinal = 3, shift = Shift.AFTER)
    )
    private static void fixInputVarLocation(
        CallbackInfoReturnable<Object> ci,
        @Local SpvcReflectedResource resource,
        @Local(name = "compiler") long compiler,
        @Local(name = "inputs") List<SpvVariable> inputs
    ) {
        canpipe_fixVarWithMultipleLocations(resource, compiler, inputs);
    }

    @Unique
    private static void canpipe_fixVarWithMultipleLocations(SpvcReflectedResource resource, long compiler, List<SpvVariable> vars) {
        var type = Spvc.spvc_compiler_get_type_handle(compiler, resource.type_id());
        int dimensions = Spvc.spvc_type_get_num_array_dimensions(type);
        int columns = Spvc.spvc_type_get_columns(type);

        int locations = columns;
        for (int dim = 0; dim < dimensions; ++dim) {
            locations *= Spvc.spvc_type_get_array_dimension(type, dim);
        }

        // from 1, first var is already added to the `vars`
        for (int i = 1; i < locations; ++i) {
            vars.add(vars.getLast());
        }
    }

    @WrapOperation(
        method = "createFromSpirv",
        at = @At(
            value = "INVOKE",
            target = "Ljava/nio/IntBuffer;put"
        )
    )
    private static IntBuffer dontPatchFragmentShaderOutputsAndFixOutputsSpirvPatching(
        IntBuffer buf,
        int index,
        int value,
        Operation<IntBuffer> operation,
        @Local(name = "outputs") List<SpvVariable> outputs,
        @Local(name = "i") int i,
        @Share("isFragmentShader") LocalRef<Boolean> isFragmentShaderShared
    ) {
        // I could instead just pass shader type here...
        if (isFragmentShaderShared.get() == null) {
            for (int j = 5; j < buf.limit();) {
                int opAndSize = buf.get(j);
                int size = opAndSize >>> 16;
                int op = opAndSize & ((1 << 16) - 1);

                // https://registry.khronos.org/SPIR-V/specs/unified1/SPIRV.html#OpEntryPoint
                if (op == 15) {
                    int executionModel = buf.get(j+1);
                    isFragmentShaderShared.set(executionModel == 4);
                    break;
                }
                j += size;
            }
        }

        Boolean isFragmentShader = isFragmentShaderShared.get();
        if (isFragmentShader == null) { throw new RuntimeException("Couldn't find OpEntryPoint"); }

        if (isFragmentShader) {
            return buf;
        }

        // Var was already added, and it takes multiple locations
        if (i > 0 && outputs.get(i-1).name().equals(outputs.get(i).name())) {
            return buf;
        }
        return operation.call(buf, index, value);
    }

    @ModifyExpressionValue(
        method = "rebind",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Set;isEmpty",
            ordinal = 2
        )
    )
    boolean dontCrashOnRemainingSamplers(
        boolean noRemainingSamplers,
        @Local(name="remainingUniformBuffers") Set<String> remainingUniformBuffers
    ) {
        if (!noRemainingSamplers) {
            CanPipe.LOGGER.warn("Shader expects uniform buffers which are not being provided: " + remainingUniformBuffers);
        }
        return true;
    }
*/
}
