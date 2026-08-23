package fewizz.canpipe.compat.hdrmod;

import fewizz.canpipe.UniformBufferStruct;
import fewizz.canpipe.UniformBufferStruct.FloatUniform;
import fewizz.canpipe.Uniforms;
import fewizz.canpipe.pipeline.Pipelines;
import fewizz.canpipe.pipeline.Shaders;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import xyz.rrtt217.HDRMod.HDRMod;
import xyz.rrtt217.HDRMod.config.HDRModConfig;

public class HDRModCompat {

    public static void updatePipelineIfHDROptionValueChanged() {
        HDRModConfig config = HDRMod.configHolder.getConfig();

        Identifier globalsShaderLocation = Identifier.tryParse("can-pipe:shaders/compat/hdrmod.glsl");
        String uboName = "hdrmod_globals";

        boolean wasEnabled = Uniforms.externalUBOIsAdded(uboName);
        if (config.enableHDR == wasEnabled) { return; }

        if (config.enableHDR) {
            UniformBufferStruct globalsStruct = new UniformBufferStruct();
            FloatUniform gameMinimumBrightnessUniform = globalsStruct.add(new FloatUniform());
            FloatUniform gamePeakBrightnessUniform = globalsStruct.add(new FloatUniform());
            FloatUniform gamePaperBrightnessUniform = globalsStruct.add(new FloatUniform());
            FloatUniform uiBrightnessUniform = globalsStruct.add(new FloatUniform());

            Uniforms.addExternalUBO(uboName, globalsStruct, "HDRMod globals UBO", () -> {
                var windowHandle = Minecraft.getInstance().getWindow().handle();
                gameMinimumBrightnessUniform.set(HDRMod.colorManagementInfoProvider.getCurrentGameMinimumBrightness(windowHandle));
                gamePeakBrightnessUniform.set(HDRMod.colorManagementInfoProvider.getCurrentGamePeakBrightness(windowHandle));
                gamePaperBrightnessUniform.set(HDRMod.colorManagementInfoProvider.getCurrentGamePaperWhiteBrightness(windowHandle));
                uiBrightnessUniform.set(HDRMod.colorManagementInfoProvider.getCurrentUIBrightness(windowHandle));
            });

            String src = """
            #define HDRMOD

            layout(std140) uniform hdrmod_globals {
                float hdrmod_gameMinimumBrightness;
                float hdrmod_gamePeakBrightness;
                float hdrmod_gamePaperWhiteBrightness;
                float hdrmod_uiBrightness;
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
