package fewizz.canpipe.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.PipelineIODebugScreen;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow abstract public void setScreen(@Nullable Screen guiScreen);

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

        while (CanPipe.PIPELINE_IO_DEBUG.consumeClick()) {
            Pipeline p = Pipelines.getCurrent();
            if (p != null) {
                this.setScreen(new PipelineIODebugScreen(p));
            }
        }
    }

}
