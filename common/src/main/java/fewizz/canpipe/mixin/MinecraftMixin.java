package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.light.Lights;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow @Final private ReloadableResourceManager resourceManager;

    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;initRenderer(Lcom/mojang/blaze3d/systems/GpuDevice;)V",
            shift = Shift.AFTER
        )
    )
    void afterRendererInit(CallbackInfo ci) {
        CanPipe.afterRendererInit();
    }

    @Inject(
        method = "close",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;shutdownRenderer()V"
        )
    )
    void beforeRendererClose(CallbackInfo ci) {
        CanPipe.beforeRendererClose();
    }

    @Inject(method = "handleKeybinds", at = @At("RETURN"))
    private void handleKeybinds(CallbackInfo ci) {
        while (CanPipe.PIPELINES_RELOAD_KEY.consumeClick()) {
            Lights.loadRaw(Lights.readRaw(this.resourceManager));
            Materials.loadRaw(Materials.readRaw(this.resourceManager));
            MaterialMaps.loadRaw(MaterialMaps.readRaw(this.resourceManager));
            Pipelines.loadRaw(Pipelines.readRaw(this.resourceManager));
        }
    }

}
