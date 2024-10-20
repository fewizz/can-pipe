package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;

import fewizz.canpipe.Mod;
import fewizz.canpipe.Pipeline;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    private PostChain entityEffect;
    @Shadow
    private PostChain transparencyChain;

    @Shadow
    private RenderTarget translucentTarget;
    @Shadow
    private RenderTarget itemEntityTarget;
    @Shadow
    private RenderTarget particlesTarget;
    @Shadow
    private RenderTarget weatherTarget;
    @Shadow
    private RenderTarget cloudsTarget;

    @Inject(
        method = "initTransparency",
        cancellable = true,
        at = @At(
            value = "NEW",
            target = "Lnet/minecraft/client/renderer/PostChain;"
        )
    )
    void onPostChainCreation(CallbackInfo ci) {
        Pipeline p = Mod.getCurrentPipeline();
        if (p == null) {
            return;  // initialise post chain normally
        }

        ci.cancel();  // don't create post chain

        this.translucentTarget = p.framebuffers.get(p.translucentTerrainFramebufferName);
        this.itemEntityTarget = p.framebuffers.get(p.translucentEntityFramebufferName);
        this.particlesTarget = p.framebuffers.get(p.translucentParticlesFramebufferName);
        this.weatherTarget = p.framebuffers.get(p.weatherFramebufferName);
        this.cloudsTarget = p.framebuffers.get(p.cloudsFramebufferName);
    }

    /* Replacing if (transparency != null) */
    @ModifyExpressionValue(
        method = "renderLevel",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;transparencyChain:Lnet/minecraft/client/renderer/PostChain;"
        )
    )
    PostChain onTransparencyCheck(PostChain transparency) {
        Pipeline p = Mod.getCurrentPipeline();
        if (p != null) {
            // Expecting that entityEffect is always non-null
            return entityEffect;
        }
        return transparency;
    }

    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;process(F)V"))
    void onPostChainProcess(PostChain instance, float f, Operation<Void> original) {
        if (instance == null) {  // Pipeline is active
            return;
        }
        original.call(instance, f);
    }

}
