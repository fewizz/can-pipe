package fewizz.canpipe.mixin;

import org.jetbrains.annotations.Nullable;
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
import fewizz.canpipe.mixininterface.MinecraftExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

@Mixin(Minecraft.class)
public class MinecraftMixin implements MinecraftExtended {

    private @Unique RenderTarget canpipe_mainRenderTargetOverride;

    @Shadow public void setScreen(@Nullable Screen guiScreen) {}

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
            Pipelines.loadRawPipelines(Pipelines.readRawPipelines());
        }
    }

    @ModifyReturnValue(method = "getMainRenderTarget", at = @At("RETURN"))
    RenderTarget onGetMainTarget(RenderTarget original) {
        if (this.canpipe_mainRenderTargetOverride != null) {
            original = this.canpipe_mainRenderTargetOverride;
        }
        return original;
    }

}
