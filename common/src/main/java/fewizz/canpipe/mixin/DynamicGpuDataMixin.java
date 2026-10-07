package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.DynamicGpuData;

@Mixin(DynamicGpuData.class)
public class DynamicGpuDataMixin {

    @ModifyConstant(method = "writeChunkSectionCommands", constant = @Constant(intValue = 20))
    int onWriteChunkSectionCommands(int size) {
        if (Pipelines.getCurrent() != null) {
            // size = CanPipe.VertexFormats.BLOCK.getVertexSize();
        }
        return size;
    }

}
