package fewizz.canpipe.pipeline.load;

// https://github.com/vram-guild/canvas/blob/9edb051bf3cba305623e6555c7d3ececb969b215/src/main/java/grondag/canvas/shader/PreReleaseShaderCompat.java#L35
public class ShaderCompat {

    public static String fixFrexUniforms(String src) {
        // material.glsl
        src = src.replace("frx_matEmissive()", "(frx_matEmissive == 1)");
        src = src.replace("frx_matEmissiveFactor()", "float(frx_matEmissive)");
        src = src.replace("frx_matCutout()", "(frx_matCutout == 1)");
        src = src.replace("frx_matCutoutFactor()", "float(frx_matCutout)");
        src = src.replace("frx_matUnmipped()", "(frx_matUnmipped == 1)");
        src = src.replace("frx_matUnmippedFactor()", "float(frx_matUnmipped)");
        src = src.replace("frx_matDisableAo()", "(frx_matDisableAo == 1)");
        src = src.replace("frx_matDisableDiffuse()", "(frx_matDisableDiffuse == 1)");
        src = src.replace("frx_matHurt()", "(frx_matHurt == 1)");
        src = src.replace("frx_matFlash()", "(frx_matFlash == 1)");
        src = src.replace("frx_matGlint()", "float(frx_matGlint)");

        //fog.glsl
        src = src.replace("frxFogStart", "frx_fogStart");
        src = src.replace("frxFogEnd", "frx_fogEnd");
        src = src.replace("frxFogColor", "frx_fogColor");
        src = src.replace("frxFogEnabled", "(frx_fogEnabled == 1)");
        src = src.replace("frx_fogStart()", "frx_fogStart");
        src = src.replace("frx_fogEnd()", "frx_fogEnd");
        src = src.replace("frx_fogColor()", "frx_fogColor");
        src = src.replace("frx_fogEnabled()", "(frx_fogEnabled == 1)");

        // player.glsl
        src = src.replace("frx_effectModifier()", "frx_effectModifier");
        src = src.replace("frx_heldLight()", "frx_heldLight");
        src = src.replace("frx_heldLightInnerRadius()", "frx_heldLightInnerRadius");
        src = src.replace("frx_heldLightOuterRadius()", "frx_heldLightOuterRadius");
        src = src.replace("frx_playerHasEffect", "frx_playerHasEffect");
        src = src.replace("frx_playerFlag", "frx_playerFlag");
        src = src.replace("frx_playerHasNightVision()", "(frx_effectNightVision == 1)");
        src = src.replace("frx_playerMood()", "frx_playerMood");
        src = src.replace("frx_eyePos()", "frx_eyePos");
        src = src.replace("frx_eyeBrightness()", "frx_eyeBrightness");
        src = src.replace("frx_smoothedEyeBrightness()", "frx_smoothedEyeBrightness");

        // view.glsl
        src = src.replace("frx_cameraView()", "frx_cameraView");
        src = src.replace("frx_entityView()", "frx_entityView");
        src = src.replace("frx_cameraPos()", "frx_cameraPos");
        src = src.replace("frx_lastCameraPos()", "frx_lastCameraPos");
        src = src.replace("frx_modelToWorld()", "frx_modelToWorld");
        src = src.replace("frx_modelOriginWorldPos()", "frx_modelToWorld.xyz");
        src = src.replace("frx_modelToCamera()", "frx_modelToCamera");
        src = src.replace("frx_modelOriginType()", "_cvu_model_origin_type");
        src = src.replace("frx_isHand()", "frx_isHand");
        src = src.replace("frx_isGui()", "frx_isGui");
        src = src.replace("frx_guiViewProjectionMatrix()", "frx_guiViewProjectionMatrix");
        src = src.replace("frx_normalModelMatrix()", "frx_normalModelMatrix");
        src = src.replace("frx_viewMatrix()", "frx_viewMatrix");
        src = src.replace("frx_inverseViewMatrix()", "frx_inverseViewMatrix");
        src = src.replace("frx_lastViewMatrix()", "frx_lastViewMatrix");
        src = src.replace("frx_projectionMatrix()", "frx_projectionMatrix");
        src = src.replace("frx_lastProjectionMatrix()", "frx_lastProjectionMatrix");
        src = src.replace("frx_inverseProjectionMatrix()", "frx_inverseProjectionMatrix");
        src = src.replace("frx_viewProjectionMatrix()", "frx_viewProjectionMatrix");
        src = src.replace("frx_inverseViewProjectionMatrix()", "frx_inverseViewProjectionMatrix");
        src = src.replace("frx_lastViewProjectionMatrix()", "frx_lastViewProjectionMatrix");
        src = src.replace("frx_cleanProjectionMatrix()", "frx_cleanProjectionMatrix");
        src = src.replace("frx_lastCleanProjectionMatrix()", "frx_lastCleanProjectionMatrix");
        src = src.replace("frx_inverseCleanProjectionMatrix()", "frx_inverseCleanProjectionMatrix");
        src = src.replace("frx_cleanViewProjectionMatrix()", "frx_cleanViewProjectionMatrix");
        src = src.replace("frx_inverseCleanViewProjectionMatrix()", "frx_inverseCleanViewProjectionMatrix");
        src = src.replace("frx_lastCleanViewProjectionMatrix()", "frx_lastCleanViewProjectionMatrix");
        src = src.replace("frx_shadowViewMatrix()", "frx_shadowViewMatrix");
        src = src.replace("frx_inverseShadowViewMatrix()", "frx_inverseShadowViewMatrix");
        src = src.replace("frx_viewWidth()", "frx_viewWidth");
        src = src.replace("frx_viewHeight()", "frx_viewHeight");
        src = src.replace("frx_viewAspectRatio()", "frx_viewAspectRatio");
        src = src.replace("frx_viewBrightness()", "frx_viewBrightness");
        // src = src.replace("TARGET_SOLID", "0");
        // src = src.replace("TARGET_OUTLINE", "1");
        // src = src.replace("TARGET_TRANSLUCENT", "2");
        // src = src.replace("TARGET_PARTICLES", "3");
        // src = src.replace("TARGET_WEATHER", "4");
        // src = src.replace("TARGET_CLOUDS", "5");
        // src = src.replace("TARGET_ENTITY", "6");
        // src = src.replace("frx_renderTarget()", "_cvu_context[_CV_TARGET_INDEX]");
        // src = src.replace("FRX_CAMERA_IN_FLUID", "22");
        // src = src.replace("FRX_CAMERA_IN_WATER", "23");
        // src = src.replace("FRX_CAMERA_IN_LAVA", "24");
        src = src.replace("frx_viewFlag()", "frx_viewFlag");
        src = src.replace("frx_viewDistance()", "frx_viewDistance");

        // world.glsl
        src = src.replace("frx_renderSeconds()", "frx_renderSeconds");
        src = src.replace("frx_renderFrames()", "frx_renderFrames");
        src = src.replace("frx_worldDay()", "frx_worldDay");
        src = src.replace("frx_worldTime()", "frx_worldTime");
        src = src.replace("frx_moonSize()", "frx_moonSize");
        src = src.replace("frx_skyAngleRadians()", "frx_skyAngleRadians");
        src = src.replace("frx_skyLightVector()", "frx_skyLightVector");
        src = src.replace("frx_skyLightColor()", "frx_skyLightColor");
        src = src.replace("frx_skyLightIlluminance()", "frx_skyLightIlluminance");
        src = src.replace("frx_skyLightAtmosphericColor()", "frx_skyLightAtmosphericColor");
        src = src.replace("frx_skyLightTransitionFactor()", "frx_skyLightTransitionFactor");
        src = src.replace("frx_ambientIntensity()", "frx_ambientIntensity");
        src = src.replace("frx_emissiveColor()", "frx_emissiveColor");
        src = src.replace("frx_rainGradient()", "frx_rainGradient");
        src = src.replace("frx_thunderGradient()", "frx_thunderGradient");
        src = src.replace("frx_smoothedRainGradient()", "frx_smoothedRainGradient");
        src = src.replace("frx_vanillaClearColor()", "frx_vanillaClearColor");
        // src = src.replace("FRX_WORLD_HAS_SKYLIGHT", "0");
        // src = src.replace("FRX_WORLD_IS_OVERWORLD", "1");
        // src = src.replace("FRX_WORLD_IS_NETHER", "2");
        // src = src.replace("FRX_WORLD_IS_END", "3");
        // src = src.replace("FRX_WORLD_IS_RAINING", "4");
        // src = src.replace("FRX_WORLD_IS_THUNDERING", "5");
        // src = src.replace("FRX_WORLD_IS_SKY_DARKENED", "6");
        // src = src.replace("FRX_WORLD_IS_MOONLIT", "21");
        src = src.replace("frx_testCondition", "frx_testCondition");
        src = src.replace("frx_worldFlag", "frx_worldFlag");
        src = src.replace("frx_isThundering()", "(frx_worldIsThundering == 1)");
        src = src.replace("frx_isSkyDarkened()", "(frx_worldIsSkyDarkened == 1)");
        src = src.replace("frx_worldHasSkylight()", "(frx_worldHasSkylight == 1)");
        src = src.replace("frx_isWorldTheOverworld()", "(frx_worldIsOverworld == 1)");
        src = src.replace("frx_isWorldTheNether()", "(frx_worldIsNether == 1)");
        src = src.replace("frx_isWorldTheEnd()", "(frx_worldIsEnd == 1)");
        src = src.replace("frx_isRaining()", "(frx_worldIsRaining == 1)");

        // vertex.glsl
        // src = src.replace("frx_color", "frx_vertexColor");
        // src = src.replace("frx_normal", "frx_vertexNormal");

        // bitwise.glsl
        // src = src.replace("frx_bitValue()", "frx_bitValue");

        return src;
    }

}
