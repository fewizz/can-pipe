package fewizz.canpipe.pipeline;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import blue.endless.jankson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public class PipelineRaw {
    @NotNull public final ResourceLocation location;
    @NotNull public final String nameKey;
    @NotNull public final Map<ResourceLocation, Option> options;
    @NotNull public final JsonObject json;

    PipelineRaw(
        ResourceLocation location,
        String nameKey,
        Map<ResourceLocation, Option> options,
        JsonObject json
    ) {
        this.location = location;
        this.nameKey = nameKey;
        this.options = options;
        this.json = json;
    }
}
