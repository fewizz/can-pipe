package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.MaterialProgramLoader;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.texture.TextureAtlas;

@Mixin(ChunkSectionsToRender.DrawSeparate.class)
public class ChunkSectionsToRenderSeparateMixin {

    @WrapOperation(
        method = "render",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getCompiledPipeline")
    )
    CompiledRenderPipeline onGetCompiledPipeline(
        RenderPipeline pipeline,
        Operation<CompiledRenderPipeline> operation,
        @Share("canpipe_originalRenderPipeline") LocalRef<RenderPipeline> canpipe_originalRenderPipeline
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null && (
            pipeline == RenderPipelines.SOLID_TERRAIN ||
            pipeline == RenderPipelines.CUTOUT_TERRAIN ||
            pipeline == RenderPipelines.TRANSLUCENT_TERRAIN
        )) {
            canpipe_originalRenderPipeline.set(pipeline);
            MaterialProgramLoader loader = p.getMaterialProgramLoader(pipeline);
            if (loader != null) {
                Framebuffer fb = p.shadowFramebufferOr(pipeline != RenderPipelines.TRANSLUCENT_TERRAIN ? p.solidFramebuffer : p.translucentTerrainFramebuffer);
                return loader.getOrCreateCompiledRenderPipeline(fb.getFormats());
            }
        }
        return operation.call(pipeline);
    }

    @Inject(
        method = "render",
        at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/RenderPass;setPipeline", shift = Shift.AFTER)
    )
    void onSetPipeline(
        CallbackInfo ci,
        @Local RenderPass renderPass,
        @Share("canpipe_originalRenderPipeline") LocalRef<RenderPipeline> canpipe_originalRenderPipeline
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            RenderPipeline pipeline = canpipe_originalRenderPipeline.get();
            Framebuffer fb = p.shadowFramebufferOr(pipeline != RenderPipelines.TRANSLUCENT_TERRAIN ? p.solidFramebuffer : p.translucentTerrainFramebuffer);
            Minecraft mc = Minecraft.getInstance();
            @SuppressWarnings("deprecation")
            GpuTextureView blockAtlas = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
            Pipeline.bindSpritesExtentsSampler(renderPass, blockAtlas);
            p.bindUniforms(renderPass, fb, false);
        }
    }

}
