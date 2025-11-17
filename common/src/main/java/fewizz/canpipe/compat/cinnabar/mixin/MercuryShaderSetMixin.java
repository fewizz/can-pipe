package fewizz.canpipe.compat.cinnabar.mixin;

import org.lwjgl.util.spvc.Spvc;
import org.lwjgl.util.spvc.SpvcReflectedResource;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import graphics.cinnabar.core.mercury.MercuryShaderSet;

@Mixin(MercuryShaderSet.class)
public class MercuryShaderSetMixin {

    @ModifyExpressionValue(
        method = "<init>",
        at = @At(
            value = "FIELD",
            target = "Lgraphics/cinnabar/core/mercury/MercuryShaderSet;attachmentCount:I",
            opcode = Opcodes.PUTFIELD,
            shift = Shift.BEFORE
        )
    )
    int onSetAttachemntCount(
        int attachmentCount,
        @Local(ordinal = 0) SpvcReflectedResource.Buffer fraAttachments,
        @Local(name = "spvcFraCompiler") long spvcFraCompiler
    ) {
        attachmentCount = 0;

        // Recalculating, handling cases like `out vec4[7] fragColor;`

        for (SpvcReflectedResource fraAttach : fraAttachments) {
            final var type = Spvc.spvc_compiler_get_type_handle(spvcFraCompiler, fraAttach.type_id());
            final var dimensions = Spvc.spvc_type_get_num_array_dimensions(type);

            int attachmentsCountLocal = 1;

            for (int dim = 0; dim < dimensions; ++dim) {
                attachmentsCountLocal *= Spvc.spvc_type_get_array_dimension(type, dim);
            }

            attachmentCount += attachmentsCountLocal;
        }

        return attachmentCount;
    }

}
