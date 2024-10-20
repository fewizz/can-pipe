package fewizz.canpipe;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.vertex.VertexFormat;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;


/** Variant of `ShaderInstance`, that is able to
 * load it's json and vertex/frament shader sources from arguments
 */
public class MockedProgram extends ShaderInstance {

    public MockedProgram(
        ResourceLocation location,
        VertexFormat vertexFormat,
        List<String> samplers,
        List<JsonObject> uniforms,
        String vertexShaderName,
        String fragmentShaderName
    ) throws IOException {
        super(
            mockedResourceProvider(location, samplers, uniforms, vertexShaderName, fragmentShaderName),
            location.toString(),
            vertexFormat
        );
    }

    private static ResourceProvider mockedResourceProvider(
        ResourceLocation location,
        List<String> samplers,
        List<JsonObject> uniforms,
        String vertexShaderName,
        String fragmentShaderName
    ) {
        JsonObject json = new JsonObject();
        json.put("vertex", new JsonPrimitive(vertexShaderName));
        json.put("fragment", new JsonPrimitive(fragmentShaderName));

        JsonArray samplersArray = new JsonArray();
        samplersArray.addAll(samplers.stream().map(s -> new JsonObject(){{ put("name", new JsonPrimitive(s)); }} ).toList());
        json.put("samplers", samplersArray);
        JsonArray uniformsArray = new JsonArray();
        uniformsArray.addAll(uniforms);
        json.put("uniforms", uniformsArray);

        return new ResourceProvider() {

            @Override
            public Optional<Resource> getResource(ResourceLocation loc) {
                Minecraft c = Minecraft.getInstance();
                var pack = c.getResourceManager().listPacks().findFirst().get();

                if (loc.equals(location)) {//if (loc.toString().equals(name)) {  // not "minecraft:shaders/core/" + name + ".json", changed by mixin
                    String jsonString = json.toJson();
                    return Optional.of(new Resource(pack, () -> new ByteArrayInputStream(jsonString.getBytes())));
                }

                throw new UnsupportedOperationException("Unexpected resource request at " + loc);
            }

        };
    }

}
