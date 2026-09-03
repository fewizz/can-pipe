package fewizz.canpipe.compat.hdrmod;

import fewizz.canpipe.PlatformHelperService;
import fewizz.canpipe.pipeline.Pipelines;
import fewizz.canpipe.pipeline.Shaders;
import fewizz.canpipe.pipeline.UniformBufferStruct;
import fewizz.canpipe.pipeline.Uniforms;
import fewizz.canpipe.pipeline.UniformBufferStruct.FloatUniform;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import xyz.rrtt217.HDRMod.api.HDRModApi;

public class HDRModCompat {

    public static void init() {
        if (PlatformHelperService.isModLoaded("hdr_mod")) {
            HDRModApi.getInstance().addHDRStateChangeListener(
                _isHdrEnabled /* Доверяй, но проверяй */ -> updatePipelineIfHDROptionValueChanged()
            );
            updatePipelineIfHDROptionValueChanged();
        }
    }

    private static void updatePipelineIfHDROptionValueChanged() {
        HDRModApi api = HDRModApi.getInstance();

        Identifier globalsShaderLocation = Identifier.tryParse("can-pipe:shaders/compat/hdrmod.glsl");
        String uboName = "hdrmod_globals";

        boolean wasEnabled = Uniforms.externalUBOIsAdded(uboName);
        if (api.isHDREnabled() == wasEnabled) { return; }

        if (api.isHDREnabled()) {
            UniformBufferStruct globalsStruct = new UniformBufferStruct();
            FloatUniform gameMinimumBrightnessUniform = globalsStruct.add(new FloatUniform());
            FloatUniform gamePeakBrightnessUniform = globalsStruct.add(new FloatUniform());
            FloatUniform gamePaperBrightnessUniform = globalsStruct.add(new FloatUniform());
            FloatUniform uiBrightnessUniform = globalsStruct.add(new FloatUniform());
            FloatUniform uiHudBrightnessUniform = globalsStruct.add(new FloatUniform());
            FloatUniform uiNonHudBrightnessUniform = globalsStruct.add(new FloatUniform());

            Uniforms.addExternalUBO(uboName, globalsStruct, "HDRMod globals UBO", () -> {
                var windowHandle = Minecraft.getInstance().getWindow().handle();
                gameMinimumBrightnessUniform.set(api.getColorManagementInfo().getCurrentGameMinimumBrightness(windowHandle));
                gamePeakBrightnessUniform.set(api.getColorManagementInfo().getCurrentGamePeakBrightness(windowHandle));
                gamePaperBrightnessUniform.set(api.getColorManagementInfo().getCurrentGamePaperWhiteBrightness(windowHandle));
                uiBrightnessUniform.set(api.getColorManagementInfo().getCurrentUIBrightness(windowHandle));
                uiHudBrightnessUniform.set(api.getColorManagementInfo().getCurrentUIBrightness(windowHandle));
                uiNonHudBrightnessUniform.set(api.getColorManagementInfo().getCurrentUIBrightness(windowHandle));
            });

            String src = """
            #define HDRMOD

            layout(std140) uniform hdrmod_globals {
                float hdrmod_gameMinimumBrightness;
                float hdrmod_gamePeakBrightness;
                float hdrmod_gamePaperWhiteBrightness;
                float hdrmod_uiBrightness;
                float hdrmod_uiHudBrightness;
                float hdrmod_uiNonHudBrightness;
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
