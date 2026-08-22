package fewizz.canpipe.pipeline;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.resources.Identifier;

public class PipelinesFixes {

    public static void fix(Identifier pipelineLocation, JsonObject pipelineJson) {
        // https://github.com/ambrosia13/ForgetMeNot-Shaders/commit/4eaa1e0f3bec07f265c504d760cccf2676c8fef5
        if (pipelineLocation.getNamespace().contains("forgetmenot")) {
            var programs = pipelineJson.get(JsonArray.class, "programs");
            if (programs != null) {
                programs.stream().filter(
                        (JsonElement program) ->
                                program instanceof JsonObject programJson &&
                                        programJson.containsKey("name") &&
                                        JanksonUtils.stringOrThrow(programJson, "name").equals("depth_downsample")
                ).findFirst().ifPresent(program -> {
                    JsonObject programJson = (JsonObject) program;
                    JsonArray samplers = programJson.get(JsonArray.class, "samplers");
                    if (samplers == null || samplers.size() != 1 || !(samplers.getFirst() instanceof JsonPrimitive sampler)) {
                        return;
                    }
                    if (sampler.asString().equals("u_depth")) {
                        CanPipe.LOGGER.warn("replacing sampler \"u_depth\" with \"u_depth_mips\" for program \"depth_downsample\"");
                        samplers.set(0, JsonPrimitive.of("u_depth_mips"));
                    }
                });
            }
        }

        // https://github.com/ambrosia13/Aerie-Shaders/pull/2
        if (pipelineLocation.getNamespace().contains("aerie")) {
            var programs = pipelineJson.get(JsonArray.class, "programs");
            if (programs != null) {
                programs.stream().filter(
                        (JsonElement program) ->
                                program instanceof JsonObject programJson &&
                                        programJson.containsKey("name") &&
                                        JanksonUtils.stringOrThrow(programJson, "name").equals("copy")
                ).findFirst().ifPresent(program -> {
                    JsonObject programJson = (JsonObject) program;
                    JsonArray samplers = programJson.get(JsonArray.class, "samplers");
                    if (samplers == null || samplers.size() != 1 || !(samplers.getFirst() instanceof JsonPrimitive sampler)) {
                        return;
                    }
                    if (sampler.asString().equals("u_composite")) {
                        CanPipe.LOGGER.warn("replacing sampler \"u_composite\" with \"u_color\" for program \"copy\"");
                        samplers.set(0, JsonPrimitive.of("u_color"));
                    }
                });
            }
        }
    }

}
