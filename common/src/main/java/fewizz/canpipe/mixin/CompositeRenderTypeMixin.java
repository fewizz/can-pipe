package fewizz.canpipe.mixin;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;

import fewizz.canpipe.mixininterface.CompositeRenderTypeExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;

@Mixin(RenderType.CompositeRenderType.class)
public class CompositeRenderTypeMixin implements CompositeRenderTypeExtended {

    @Unique private Map<Pipeline, RenderPipeline> canpipe_materialRenderPipeline;
    @Unique private Function<Pipeline, RenderPipeline> canpipe_materialRenderPipelineCreationFunction;

    @Unique private Map<Pipeline, RenderPipeline> canpipe_materialShadowRenderPipeline;
    @Unique private Function<Pipeline, RenderPipeline> canpipe_materialShadowRenderPipelineCreationFunction;

    @Override
    public void canpipe_setMaterialRenderPipelineCreationFunction(Function<Pipeline, RenderPipeline> pipeline) {
        this.canpipe_materialRenderPipelineCreationFunction = pipeline;
        this.canpipe_materialRenderPipeline = Collections.synchronizedMap(new WeakHashMap<>());
    }

    @Override
    public void canpipe_setMaterialShadowRenderPipelineCreationFunction(Function<Pipeline, RenderPipeline> pipeline) {
        this.canpipe_materialShadowRenderPipelineCreationFunction = pipeline;
        this.canpipe_materialShadowRenderPipeline = Collections.synchronizedMap(new WeakHashMap<>());
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
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            Minecraft mc = Minecraft.getInstance();
            if (
                this.canpipe_materialShadowRenderPipelineCreationFunction != null &&
                ((LevelRendererExtended) mc.levelRenderer).canpipe_getIsRenderingShadows()
            ) {
                return this.canpipe_materialShadowRenderPipeline.computeIfAbsent(
                    p, this.canpipe_materialShadowRenderPipelineCreationFunction
                );
            }
            if (
                this.canpipe_materialRenderPipelineCreationFunction != null &&
                !((LevelRendererExtended) mc.levelRenderer).canpipe_getIsRenderingShadows()
            ) {
                return this.canpipe_materialRenderPipeline.computeIfAbsent(
                    p, this.canpipe_materialRenderPipelineCreationFunction
                );
            }
        }
        return original;
    }

}
