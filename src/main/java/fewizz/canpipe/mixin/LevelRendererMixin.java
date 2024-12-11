package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;

import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import fewizz.canpipe.pipeline.ProgramBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private LevelTargetBundle targets = new LevelTargetBundle();

    @WrapOperation(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;getTransparencyChain()Lnet/minecraft/client/renderer/PostChain;"
        )
    )
    PostChain onPostChainCreation(
        LevelRenderer instance,
        Operation<PostChain> original,
        @Local RenderTargetDescriptor renderTargetDescriptor,
        @Local FrameGraphBuilder frameGraphBuilder
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) {
            return original.call(instance);  // Initialise post chain normally
        }
        // Don't create post chain, selected pipeline will handle that

        this.targets.main = frameGraphBuilder.importExternal("main", p.solidTerrainFramebuffer);
        this.targets.translucent = frameGraphBuilder.importExternal("translucent", p.translucentTerrainFramebuffer);
        this.targets.itemEntity = frameGraphBuilder.importExternal("item_entity", p.translucentEntityFramebuffer);
        this.targets.particles = frameGraphBuilder.importExternal("particles", p.translucentParticlesFramebuffer);
        this.targets.weather = frameGraphBuilder.importExternal("weather", p.weatherFramebuffer);
        this.targets.clouds = frameGraphBuilder.importExternal("clouds", p.cloudsFramebuffer);

        return null;
    }

    @Inject(
        method = "renderSectionLayer",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;bind()V"
        )
    )
    void beforeSectionLayerDraw(
        CallbackInfo ci,
        @Local CompiledShaderProgram program,
        @Local SectionRenderDispatcher.RenderSection section
    ) {
        if (program instanceof ProgramBase pb) {
            if (pb.FRX_MODEL_TO_WORLD != null) {
                BlockPos pos = section.getOrigin();
                pb.FRX_MODEL_TO_WORLD.set(pos.getX(), pos.getY(),pos.getZ(), 1.0F);
                pb.FRX_MODEL_TO_WORLD.upload();
                pb.FRX_MODEL_TO_WORLD.set(0.0F, 0.0F, 0.0F, 1.0F);
            }
            if (pb.CANPIPE_ORIGIN_TYPE != null) {
                pb.CANPIPE_ORIGIN_TYPE.set(1); // region
                pb.CANPIPE_ORIGIN_TYPE.upload();
                pb.CANPIPE_ORIGIN_TYPE.set(0);  // camera
            }
        }
    }

}
