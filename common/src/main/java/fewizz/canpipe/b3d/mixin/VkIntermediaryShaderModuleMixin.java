package fewizz.canpipe.b3d.mixin;

import java.nio.IntBuffer;
import java.util.List;

import org.lwjgl.util.spvc.Spvc;
import org.lwjgl.util.spvc.SpvcReflectedResource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vulkan.glsl.IntermediaryShaderModule;
import com.mojang.blaze3d.vulkan.glsl.SpvVariable;

import fewizz.canpipe.b3d.GpuShaderModule;

@Mixin(IntermediaryShaderModule.class)
public class VkIntermediaryShaderModuleMixin implements GpuShaderModule {

    @Shadow @Final private List<SpvVariable> outputs;

    @Override
    public boolean canpipe_isGettingOutputVariablesNamesSupported() {
        return true;
    }

    @Override
    public List<String> canpipe_getOutputVariablesNames() {
        return this.outputs.stream().map(x -> x.name()).toList();
    }

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
    private static IntBuffer dontPatchFragmentShaderOutputsAndfixOutputsSpirvPatching(
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

}
