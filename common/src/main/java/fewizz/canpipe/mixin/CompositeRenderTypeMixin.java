package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import fewizz.canpipe.mixininterface.CompositeRenderTypeExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;

@Mixin(RenderType.CompositeRenderType.class)
public class CompositeRenderTypeMixin implements CompositeRenderTypeExtended {

    @Shadow @Final private RenderPipeline renderPipeline;

    @Unique
    private RenderPipeline getReplacedRenderPipeline(RenderPipeline renderPipeline) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            Minecraft mc = Minecraft.getInstance();
            if (!((LevelRendererExtended) mc.levelRenderer).canpipe_getIsRenderingShadows()) {
                renderPipeline = p.materialPrograms.getOrDefault(renderPipeline, renderPipeline);
            }
            else {
                renderPipeline = p.shadows.materialPrograms().getOrDefault(renderPipeline, renderPipeline);
            }
        }
        return renderPipeline;
    }

    @ModifyExpressionValue(
        method = {"format", "mode", "draw"},
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/RenderType$CompositeRenderType;"+
                "renderPipeline:"+
                "Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
        )
    )
    RenderPipeline replaceRenderPipeline(RenderPipeline original) {
        return this.getReplacedRenderPipeline(original);
    }

    @Override
    public RenderPipeline canpipe_getRenderPipeline() {
        return this.getReplacedRenderPipeline(this.renderPipeline);
    }

}
