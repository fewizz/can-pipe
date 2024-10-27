package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;

import fewizz.canpipe.Mod;
import fewizz.canpipe.Pipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;

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
        Pipeline p = Mod.getCurrentPipeline();
        if (p == null) {
            return original.call(instance);  // initialise post chain normally
        }

        // don't create post chain

        this.targets.translucent = frameGraphBuilder.importExternal("translucent", p.framebuffers.get(p.translucentTerrainFramebufferName));
        this.targets.itemEntity = frameGraphBuilder.importExternal("item_entity", p.framebuffers.get(p.translucentEntityFramebufferName));
        this.targets.particles = frameGraphBuilder.importExternal("particles", p.framebuffers.get(p.translucentParticlesFramebufferName));
        this.targets.weather = frameGraphBuilder.importExternal("weather", p.framebuffers.get(p.weatherFramebufferName));
        this.targets.clouds = frameGraphBuilder.importExternal("clouds", p.framebuffers.get(p.cloudsFramebufferName));

        return null;
    }

}
