package fewizz.canpipe.mixin;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderTarget;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.light.Lights;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.mixininterface.MinecraftExtended;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.resources.ReloadableResourceManager;

@Mixin(Minecraft.class)
public class MinecraftMixin implements MinecraftExtended {

    @Shadow @Final private RenderTarget mainRenderTarget;
    @Shadow @Final private ReloadableResourceManager resourceManager;

    @Shadow public void setScreen(@Nullable Screen screen) {}

    @Unique private RenderTarget canpipe_mainRenderTargetOverride;

    @Override
    public void canpipe_setMainRenderTargetOverride(RenderTarget renderTarget) {
        this.canpipe_mainRenderTargetOverride = renderTarget;
    }

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
            target = "Lcom/mojang/blaze3d/systems/SamplerCache;close()V"
        )
    )
    void beforeRendererClose(CallbackInfo ci) {
        CanPipe.beforeRendererClose();
    }

    @ModifyReturnValue(
        method = "useShaderTransparency",
        at = @At("RETURN")
    )
    private static boolean useShaderTransparency(boolean original) {
        Pipeline p = Pipelines.getCurrent();
        return original || (p != null && p.fabulousTargets != null);
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

    @ModifyReturnValue(method = "getMainRenderTarget", at = @At("RETURN"))
    RenderTarget onGetMainTarget(RenderTarget original) {
        if (this.canpipe_mainRenderTargetOverride != null) {
            original = this.canpipe_mainRenderTargetOverride;
        }
        return original;
    }

    @ModifyExpressionValue(
        method = "renderFrame",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;mainRenderTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;",
            opcode = Opcodes.GETFIELD
        )
    )
    RenderTarget getGetOverridenMainRenderTarget(RenderTarget renderTarget) {
        if (this.canpipe_mainRenderTargetOverride != null) {
            renderTarget = this.canpipe_mainRenderTargetOverride;
        }
        return renderTarget;
    }

}
