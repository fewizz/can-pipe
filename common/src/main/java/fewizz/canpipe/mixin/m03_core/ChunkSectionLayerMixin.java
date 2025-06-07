package fewizz.canpipe.mixin.m03_core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

@Mixin(ChunkSectionLayer.class)
public class ChunkSectionLayerMixin {
    
    @ModifyReturnValue(method="pipeline", at=@At("RETURN"))
    public RenderPipeline pipeline(RenderPipeline renderPipeline) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            Minecraft mc = Minecraft.getInstance();
            if (!((LevelRendererExtended) mc.levelRenderer).canpipe_getIsRenderingShadows()) {
                renderPipeline = p.materialPrograms.get(renderPipeline).info();
            }
            else {
                renderPipeline = p.shadows.materialPrograms().get(renderPipeline).info();
            }
            // NOTE: we don't check for shadows here
        }
        return renderPipeline;
	}

}
