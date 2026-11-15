package fewizz.canpipe.pipeline.load;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.NotImplementedException;
import org.joml.Vector4f;

import com.mojang.renderpearl.api.textures.GpuTexture;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.Texture;
import fewizz.canpipe.util.JanksonUtils;
import net.minecraft.resources.Identifier;

public class FramebufferLoader {

    public static Framebuffer load(
        JsonObject framebufferJson,
        Identifier pipelineLocation,
        Function<String, Texture> getOrLoadTexture
    ) {
        String name = framebufferJson.get(String.class, "name");
        var colorAttachmentJsons = JanksonUtils.listOfObjects(framebufferJson, "colorAttachments");
        JsonObject depthAttachmentJson = framebufferJson.getObject("depthAttachment");

        var colorAttachments = new ArrayList<Framebuffer.ColorAttachment>();
        for (JsonObject colorAttachmentJson : colorAttachmentJsons) {
            String textureName = colorAttachmentJson.get(String.class, "image");
            Texture texture = getOrLoadTexture.apply(textureName);

            int lod = colorAttachmentJson.getInt("lod", 0);
            int baseLayer = colorAttachmentJson.getInt("layer", 0);
            int layerCount = 1;
            int face = colorAttachmentJson.getInt("face", -1);

            boolean cubemap = (texture.getTexture().usage() & GpuTexture.USAGE_CUBEMAP_COMPATIBLE) != 0;

            if (face != -1) {
                if (!cubemap) {
                    throw new RuntimeException("Face can be specified only for cube map textures");
                }
                baseLayer = baseLayer * 6 + face;
            }

            int clearColorInt = 0x00000000;
            JsonElement clearColorJson = colorAttachmentJson.get("clearColor");
            if (clearColorJson != null) {
                Object clearColorRaw = ((JsonPrimitive) clearColorJson).getValue();
                if (clearColorRaw instanceof Long clearColorL) {
                    clearColorInt = (int) (long) clearColorL;
                }
                else {
                    throw new NotImplementedException(clearColorRaw.getClass().getName());
                }
            }

            Vector4f clearColor = new Vector4f((clearColorInt >> 24) & 0xFF, (clearColorInt >> 16) & 0xFF, (clearColorInt >> 8)  & 0xFF, (clearColorInt >> 0)  & 0xFF);

            colorAttachments.add(new Framebuffer.ColorAttachment(texture, lod, baseLayer, layerCount, clearColor));
        }

        Framebuffer.DepthAttachment depthAttachment = null;
        if (depthAttachmentJson != null) {
            String textureName = depthAttachmentJson.get(String.class, "image");
            Texture texture = getOrLoadTexture.apply(textureName);

            var lod = Optional.ofNullable(depthAttachmentJson.get(Integer.class, "lod")).orElse(0);
            var layer = Optional.ofNullable(depthAttachmentJson.get(Integer.class, "layer")).orElse(0);
            var layerCount = 1;

            double clearDepth = depthAttachmentJson.getDouble("clearDepth", 0.0);

            depthAttachment = new Framebuffer.DepthAttachment(texture, lod, layer, layerCount, clearDepth);
        }

        return new Framebuffer(name, colorAttachments, depthAttachment);
    }

}
