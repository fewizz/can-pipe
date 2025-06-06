package fewizz.canpipe.mixin.m03_core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import fewizz.canpipe.mixininterface.CompositeRenderTypeExtended;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

@Mixin(ChunkSectionLayer.class)
public class ChunkSectionLayerMixin {
    
    @ModifyReturnValue(method="pipeline", at=@At("RETURN"))
    public RenderPipeline pipeline(RenderPipeline renderPipeline) {
        if (renderPipeline == RenderPipelines.SOLID) {
            renderPipeline = ((CompositeRenderTypeExtended) RenderType.SOLID).canpipe_getRenderPipeline();
        }
        else if (renderPipeline == RenderPipelines.CUTOUT_MIPPED) {
            renderPipeline = ((CompositeRenderTypeExtended) RenderType.CUTOUT_MIPPED).canpipe_getRenderPipeline();
        }
        else if (renderPipeline == RenderPipelines.CUTOUT) {
            renderPipeline = ((CompositeRenderTypeExtended) RenderType.CUTOUT).canpipe_getRenderPipeline();
        }
        else if (renderPipeline == RenderPipelines.TRANSLUCENT) {
            renderPipeline = ((CompositeRenderTypeExtended) RenderType.CUTOUT).canpipe_getRenderPipeline();
        }
        return renderPipeline;
	}

}
