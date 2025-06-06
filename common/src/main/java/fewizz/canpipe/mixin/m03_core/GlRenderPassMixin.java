package fewizz.canpipe.mixin.m03_core;

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
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import fewizz.canpipe.pipeline.MaterialProgram;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelManager;

@Mixin(GlRenderPass.class)
public class GlRenderPassMixin {

    @Shadow protected GlRenderPipeline pipeline;

    @SuppressWarnings("deprecation")
    @WrapMethod(method = "bindSampler")
    void onBindSampler(String name, GpuTextureView textureView, Operation<Void> original) {
        if (pipeline != null && pipeline.program() instanceof MaterialProgram materialProgram) {
            if (name.equals("Sampler0")) {
                name = "frxs_baseColor";

                var mc = Minecraft.getInstance();
                TextureAtlas atlas = null;
                for (var atlasLoc : ModelManager.VANILLA_ATLASES.keySet()) {
                    var possibleAtlas = mc.getModelManager().getAtlas(atlasLoc);
                    if (possibleAtlas.getTexture() == textureView.texture()) {
                        atlas = possibleAtlas;
                        break;
                    }
                }
                if (atlas == null) {
                    // we just need to bind something,
                    // nothin will be read from it
                    atlas = mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
                }
                original.call(
                    "canpipe_spritesExtents",
                    ((TextureAtlasExtended) atlas).canpipe_getSpriteData()
                );
            }
            else if (name.equals("Sampler2")) {
                name = "frxs_lightmap";
            }

            for (var e : materialProgram.samplerToTexture.entrySet()) {
                original.call(e.getKey(), e.getValue());
            }
        }
        original.call(name, textureView);
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
