package fewizz.canpipe.pipeline.load;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.joml.Vector2i;

import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.pipeline.ClearPass;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.Pass;
import fewizz.canpipe.pipeline.ProgramPass;
import fewizz.canpipe.util.JanksonUtils;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

public class PassLoader {

    public static CompletableFuture<Optional<Pass>> load(
        Identifier pipelineId,
        JsonObject json,
        Function<String, Object> optionValueByName,
        Function<String, Optional<Framebuffer>> getOrLoadOptionalFramebuffer,
        BiFunction<String, Framebuffer, CompletableFuture<CompiledRenderPipeline>> getOrLoadProgram,
        Function<String, Optional<AbstractTexture>> getOrLoadPipelineOrResourcepackTexture,
        Executor executor
    ) {
        String toggleConfig = json.get(String.class, "toggleConfig");

        // pass is disabled, skipping
        if (toggleConfig != null && !(boolean) optionValueByName.apply(toggleConfig)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        String passName = json.get(String.class, "name");
        Objects.requireNonNull(passName);
        String framebufferName = json.get(String.class, "framebuffer");
        Optional<Framebuffer> framebuffer = getOrLoadOptionalFramebuffer.apply(framebufferName);
        if (framebuffer.isEmpty()) {
            // canvas behaviour
            CanPipe.LOGGER.warn("Couldn't find framebuffer \""+framebufferName +"\", pass \""+passName+"\" will be skipped");
            return CompletableFuture.completedFuture(Optional.empty());
        }

        String programName = json.get(String.class, "program");
        Objects.requireNonNull(programName);
        var id = Identifier.fromNamespaceAndPath(pipelineId.getNamespace(), passName);

        if (programName.equals("frex_clear")) {
            return CompletableFuture.completedFuture(Optional.of(new ClearPass(id, framebuffer.get())));
        }

        List<Optional<AbstractTexture>> samplerTextures = new ArrayList<>();
        for (String s : JanksonUtils.listOfStrings(json, "samplerImages")) {
            samplerTextures.add(getOrLoadPipelineOrResourcepackTexture.apply(s));
        }

        int size = json.getInt("size", 0);
        Vector2i extent = new Vector2i(
            json.getInt("width", size),
            json.getInt("height", size)
        );
        int lod = json.getInt("lod", 0);
        int layer = json.getInt("layer", 0);

        var futureCompiledRenderPipeline = getOrLoadProgram.apply(programName, framebuffer.get());

        return futureCompiledRenderPipeline.thenApplyAsync(
            (CompiledRenderPipeline compiledRenderPipeline) -> {
                return Optional.of(new ProgramPass(id, framebuffer.get(), compiledRenderPipeline, samplerTextures, extent, lod, layer));
            },
            executor
        );
    }

}
