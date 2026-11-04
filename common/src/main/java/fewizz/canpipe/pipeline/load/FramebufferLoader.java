package fewizz.canpipe.pipeline.load;

import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.Texture;
import fewizz.canpipe.renderpearl.GpuDeviceExtended;
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

        Vector4f[] colorTextureClearColors = new Vector4f[colorAttachmentJsons.size()];

        for (int i = 0; i < colorTextureClearColors.length; ++i) {
            int clearColor = 0x00000000;
            JsonElement clearColorJson = colorAttachmentJsons.get(i).get("clearColor");
            if (clearColorJson != null) {
                Object clearColorRaw = ((JsonPrimitive) clearColorJson).getValue();
                if (clearColorRaw instanceof Long clearColorL) {
                    clearColor = (int) (long) clearColorL;
                }
                else {
                    throw new NotImplementedException(clearColorRaw.getClass().getName());
                }
            }
            colorTextureClearColors[i] = new Vector4f(
                (clearColor >> 24) & 0xFF,
                (clearColor >> 16) & 0xFF,
                (clearColor >> 8)  & 0xFF,
                (clearColor >> 0)  & 0xFF
            );
        }

        Double depthClearDepth = null;
        JsonObject depthAttachmentJson = framebufferJson.getObject("depthAttachment");

        if (depthAttachmentJson != null) {
            depthClearDepth = depthAttachmentJson.getDouble("clearDepth", 0.0);
        }

        return new Framebuffer(
            pipelineLocation, name,
            (int idx) -> {
                JsonObject colorAttachmentJson = colorAttachmentJsons.get(idx);
                int lod = colorAttachmentJson.getInt("lod", 0);
                int baseLayer = colorAttachmentJson.getInt("layer", 0);
                int layerCount = 1;
                int face = colorAttachmentJson.getInt("face", -1);
                String textureName = colorAttachmentJson.get(String.class, "image");
                GpuTexture texture = getOrLoadTexture.apply(textureName).getTexture();

                boolean cubemap = (texture.usage() & GpuTexture.USAGE_CUBEMAP_COMPATIBLE) != 0;

                if (face != -1) {
                    if (!cubemap) {
                        throw new RuntimeException("Face can be specified only for cube map textures");
                    }
                    baseLayer = baseLayer * 6 + face;
                }

                var textureView = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
                    texture, lod, 1, baseLayer, layerCount
                );
                return Pair.of(texture, textureView);
            },
            colorTextureClearColors,
            () -> {
                GpuTexture texture = null;
                GpuTextureView textureView = null;
                if (depthAttachmentJson != null) {
                    var lod = Optional.ofNullable(depthAttachmentJson.get(Integer.class, "lod"));
                    var layer = Optional.ofNullable(depthAttachmentJson.get(Integer.class, "layer"));
                    texture = getOrLoadTexture.apply(depthAttachmentJson.get(String.class, "image")).getTexture();
                    textureView = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
                        texture, lod.orElse(0), 1, layer.orElse(0), 1
                    );
                }
                return Pair.of(texture, textureView);
            },
            depthClearDepth
        );
    }

}
