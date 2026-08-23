package fewizz.canpipe.compat.hdrmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fewizz.canpipe.UniformBufferStruct;
import fewizz.canpipe.UniformBufferStruct.FloatUniform;
import fewizz.canpipe.Uniforms;
import fewizz.canpipe.pipeline.Pipelines;
import fewizz.canpipe.pipeline.Shaders;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import xyz.rrtt217.HDRMod.HDRMod;
import xyz.rrtt217.HDRMod.config.HDRModConfig;

@Mixin(HDRMod.class)
public class HDRModMixin {

    @Inject(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lme/shedaniel/autoconfig/ConfigHolder;getConfig"
        )
    )
    private static void registerSaveListener(CallbackInfo ci) {
        HDRMod.configHolder.registerSaveListener((configHolder, config) -> {
            canpipe_onHDROptionValueChanged();
            return InteractionResult.PASS;
        });
        Minecraft.getInstance().schedule(HDRModMixin::canpipe_onHDROptionValueChanged);
    }

    @Unique
    private static void canpipe_onHDROptionValueChanged() {
        HDRModConfig config = HDRMod.configHolder.getConfig();

        Identifier globalsShaderLocation = Identifier.tryParse("hdrmod:shaders/globals.glsl");
        String uboName = "hdrmod_globals";

        boolean wasEnabled = Uniforms.externalUBOIsAdded(uboName);
        if (config.enableHDR == wasEnabled) { return; }

        if (config.enableHDR) {
            UniformBufferStruct GLOBALS = new UniformBufferStruct();
            FloatUniform GAME_MINIMUM_BRIGHTNESS = GLOBALS.add(new FloatUniform());
            FloatUniform GAME_PEAK_BRIGHTNESS = GLOBALS.add(new FloatUniform());
            FloatUniform GAME_PAPER_BRIGHTNESS = GLOBALS.add(new FloatUniform());
            FloatUniform UI_BRIGHTNESS = GLOBALS.add(new FloatUniform());

            Uniforms.addExternalUBO(uboName, GLOBALS, "HDRMod globals UBO", () -> {
                var windowHandle = Minecraft.getInstance().getWindow().handle();
                GAME_MINIMUM_BRIGHTNESS.set(HDRMod.colorManagementInfoProvider.getCurrentGameMinimumBrightness(windowHandle));
                GAME_PEAK_BRIGHTNESS.set(HDRMod.colorManagementInfoProvider.getCurrentGamePeakBrightness(windowHandle));
                GAME_PAPER_BRIGHTNESS.set(HDRMod.colorManagementInfoProvider.getCurrentGamePaperWhiteBrightness(windowHandle));
                UI_BRIGHTNESS.set(HDRMod.colorManagementInfoProvider.getCurrentUIBrightness(windowHandle));
            });

            String src = """
            #define HDRMOD

            layout(std140) uniform hdrmod_globals {
                float hdrmod_gameMinimumBrightness;
                float hdrmod_gamePeakBrightness;
                float hdrmod_gamePaperWhiteBrightness;
                float hdrmod_UIBrightness;
            };
            """;
            Shaders.VIRTUAL_INCLUDES.put(globalsShaderLocation, src);
        }
        else {
            Uniforms.removeExternalUBO(uboName);
            Shaders.VIRTUAL_INCLUDES.remove(globalsShaderLocation);
        }

        if (Pipelines.getCurrentRaw() != null) {
            Pipelines.reloadCurrent();
        }
    }

}
