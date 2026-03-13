package fewizz.canpipe.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderTarget;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.light.Lights;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.mixininterface.MinecraftExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.resources.ReloadableResourceManager;

@Mixin(Minecraft.class)
public class MinecraftMixin implements MinecraftExtended {

    @Shadow @Final private RenderTarget mainRenderTarget;
    @Shadow @Final private ReloadableResourceManager resourceManager;

    @Shadow public void setScreen(@Nullable Screen guiScreen) {}

    @Unique private RenderTarget canpipe_mainRenderTargetOverride;

    @Override
    public void canpipe_setMainRenderTargetOverride(RenderTarget renderTarget) {
        this.canpipe_mainRenderTargetOverride = renderTarget;
    }

    @ModifyReturnValue(
        method = "useShaderTransparency",
        at = @At("RETURN")
    )
    private static boolean useShaderTransparency(boolean original) {
        return original || Pipelines.getCurrent() != null;
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
            target = "Lnet/minecraft/client/Minecraft;mainRenderTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;"
        )
    )
    RenderTarget getGetOverridenMainRenderTarget(RenderTarget renderTarget) {
        if (this.canpipe_mainRenderTargetOverride != null) {
            renderTarget = this.canpipe_mainRenderTargetOverride;
        }
        return renderTarget;
    }

}
