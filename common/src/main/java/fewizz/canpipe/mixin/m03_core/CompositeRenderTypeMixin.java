package fewizz.canpipe.mixin.m03_core;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
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
            GlRenderPipeline glRenderPipeline = null;

            {
                Minecraft mc = Minecraft.getInstance();
                if (!((LevelRendererExtended) mc.levelRenderer).canpipe_getIsRenderingShadows()) {
                    glRenderPipeline = p.materialPrograms.get(renderPipeline);
                }
                else if (p.shadows != null) {
                    glRenderPipeline = p.shadows.materialPrograms().get(renderPipeline);
                }
            }

            if (glRenderPipeline != null) {
                renderPipeline = glRenderPipeline.info();
            }
        }
        return renderPipeline;
    }

    @ModifyExpressionValue(
        method = {"format", "mode", "getRenderPipeline"},
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
        return getReplacedRenderPipeline(this.renderPipeline);
    }

}
