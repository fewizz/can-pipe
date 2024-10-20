package fewizz.canpipe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;


public class Program extends CanpipeProgram {

    static final List<JsonObject> DEFAULT_UNIFORMS = new ArrayList<>() {{
        add(new JsonObject() {{
            put("name", new JsonPrimitive("frxu_size"));
            put("type", new JsonPrimitive("int"));
            put("count", new JsonPrimitive(2));
            put("values", new JsonArray(List.of(0, 0), Mod.JANKSON.getMarshaller()));
        }});
        add(new JsonObject() {{
            put("name", new JsonPrimitive("frxu_lod"));
            put("type", new JsonPrimitive("int"));
            put("count", new JsonPrimitive(1));
            put("values", new JsonArray(List.of(0), Mod.JANKSON.getMarshaller()));
        }});
        add(new JsonObject() {{
            put("name", new JsonPrimitive("frxu_frameProjectionMatrix"));
            put("type", new JsonPrimitive("matrix4x4"));
            put("count", new JsonPrimitive(16));
            put("values", new JsonArray(List.of(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0), Mod.JANKSON.getMarshaller()));
        }});
    }};

    final Uniform FRXU_SIZE;
    final Uniform FRXU_LOD;
    final Uniform FRXU_FRAME_PROJECTION_MATRIX;

    public Program(
        ResourceLocation location,
        List<String> samplers,
        CanvasShader vertexShader,
        CanvasShader fragmentShader
    ) throws IOException {
        super(
            location,
            DefaultVertexFormat.POSITION_TEX,
            samplers,
            DEFAULT_UNIFORMS,
            vertexShader,
            fragmentShader
        );
        this.FRXU_SIZE = getUniform("frxu_size");
        this.FRXU_LOD = getUniform("frxu_lod");
        this.FRXU_FRAME_PROJECTION_MATRIX = getUniform("frxu_frameProjectionMatrix");
    }

}
