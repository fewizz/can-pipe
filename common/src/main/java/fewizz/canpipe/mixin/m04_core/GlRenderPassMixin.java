package fewizz.canpipe.mixin.m04_core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlRenderPass;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTexture;

import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import fewizz.canpipe.pipeline.MaterialProgram;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelManager;

@Mixin(GlRenderPass.class)
public class GlRenderPassMixin {

    @Shadow protected GlRenderPipeline pipeline;
	@Shadow public void bindSampler(String string, GpuTexture gpuTexture) {}

    @WrapMethod(method="bindSampler")
    void onBindSampler(String name, GpuTexture texture, Operation<Void> original) {
        if (pipeline != null && pipeline.program() instanceof MaterialProgram) {
            if (name.equals("Sampler0")) {
                name = "frxs_baseColor";

                var mc = Minecraft.getInstance();
                for (var atlasLoc : ModelManager.VANILLA_ATLASES.keySet()) {
                    var atlas = mc.getModelManager().getAtlas(atlasLoc);
                    if (atlas.getTexture() == texture) {
                        original.call(
                            "canpipe_spritesExtents",
                            ((TextureAtlasExtended) atlas).canpipe_getSpriteData()
                        );
                        break;
                    }
                }
            }
            else if (name.equals("Sampler2")) {
                name = "frxs_lightmap";
            }
        }
        original.call(name, texture);
    }

    @WrapOperation(
        method = "setPipeline",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlDevice;getOrCompilePipeline("+
                "Lcom/mojang/blaze3d/pipeline/RenderPipeline;"+
            ")Lcom/mojang/blaze3d/opengl/GlRenderPipeline;"
        )
    )
    private GlRenderPipeline setPipeline(
        GlDevice instance, RenderPipeline renderPipeline,
        Operation<GlRenderPipeline> operation
    ) {
        GlRenderPipeline result = null;
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            result = p.onRenderPassSetRenderPipeline((RenderPass)(Object)this, renderPipeline);
        }
        return result != null ? result : operation.call(instance, renderPipeline);
    }

}
