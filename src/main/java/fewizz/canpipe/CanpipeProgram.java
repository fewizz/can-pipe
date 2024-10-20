package fewizz.canpipe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import fewizz.canpipe.mixininterface.GameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class CanpipeProgram extends MockedProgram {

    static final List<JsonObject> DEFAULT_UNIFORMS = new ArrayList<>() {{
        add(new JsonObject() {{
            put("name", new JsonPrimitive("frx_viewMatrix"));
            put("type", new JsonPrimitive("matrix4x4"));
            put("count", new JsonPrimitive(16));
            put("values", new JsonArray(List.of(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0), Mod.JANKSON.getMarshaller()));
        }});
        add(new JsonObject() {{
            put("name", new JsonPrimitive("frx_projectionMatrix"));
            put("type", new JsonPrimitive("matrix4x4"));
            put("count", new JsonPrimitive(16));
            put("values", new JsonArray(List.of(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0), Mod.JANKSON.getMarshaller()));
        }});
        add(new JsonObject() {{
            put("name", new JsonPrimitive("frx_modelToCamera_3"));
            put("type", new JsonPrimitive("float"));
            put("count", new JsonPrimitive(3));
            put("values", new JsonArray(List.of(0.0, 0.0, 0.0), Mod.JANKSON.getMarshaller()));
        }});
        add(new JsonObject() {{
            put("name", new JsonPrimitive("frx_fogStart"));
            put("type", new JsonPrimitive("float"));
            put("count", new JsonPrimitive(1));
            put("values", new JsonArray(List.of(0.0), Mod.JANKSON.getMarshaller()));
        }});
        add(new JsonObject() {{
            put("name", new JsonPrimitive("frx_fogEnd"));
            put("type", new JsonPrimitive("float"));
            put("count", new JsonPrimitive(1));
            put("values", new JsonArray(List.of(0.0), Mod.JANKSON.getMarshaller()));
        }});
        add(new JsonObject() {{
            put("name", new JsonPrimitive("frx_fogColor"));
            put("type", new JsonPrimitive("float"));
            put("count", new JsonPrimitive(4));
            put("values", new JsonArray(List.of(0.0, 0.0, 0.0, 0.0), Mod.JANKSON.getMarshaller()));
        }});
        add(new JsonObject() {{
            put("name", new JsonPrimitive("_screenSize"));
            put("type", new JsonPrimitive("float"));
            put("count", new JsonPrimitive(2));
            put("values", new JsonArray(List.of(0.0, 0.0), Mod.JANKSON.getMarshaller()));
        }});
        add(new JsonObject() {{
            put("name", new JsonPrimitive("frx_renderSeconds"));
            put("type", new JsonPrimitive("float"));
            put("count", new JsonPrimitive(1));
            put("values", new JsonArray(List.of(0.0), Mod.JANKSON.getMarshaller()));
        }});
        add(new JsonObject() {{
            put("name", new JsonPrimitive("_frx_renderFrames"));
            put("type", new JsonPrimitive("int"));
            put("count", new JsonPrimitive(1));
            put("values", new JsonArray(List.of(0), Mod.JANKSON.getMarshaller()));
        }});
    }};

    final Uniform FRX_RENDER_FRAMES;


    final List<String> samplers;

    CanpipeProgram(
        ResourceLocation location,
        VertexFormat vertexFormat,
        List<String> samplers,
        List<JsonObject> uniforms,
        CanvasShader vertexShader,
        CanvasShader fragmentShader
    ) throws IOException {
        super(
            location,
            vertexFormat,
            samplers,
            Stream.concat(
                DEFAULT_UNIFORMS.stream(),
                uniforms.stream()
            ).toList(),
            vertexShader.getName(),
            fragmentShader.getName()
        );
        this.samplers = samplers;
        this.FRX_RENDER_FRAMES = getUniform("_frx_renderFrames");
    }

    @Override
    public Uniform getUniform(String name) {
        if (name.equals("ModelViewMat")) { name = "frx_viewMatrix"; }
        if (name.equals("ProjMat")) { name = "frx_projectionMatrix"; }
        if (name.equals("FogStart")) { name = "frx_fogStart"; }
        if (name.equals("FogEnd")) { name = "frx_fogEnd"; }
        if (name.equals("FogColor")) { name = "frx_fogColor"; }
        if (name.equals("ScreenSize")) { name = "_screenSize"; }
        if (name.equals("GameTime")) { name = "frx_renderSeconds"; }

        return super.getUniform(name);
    }

    @Override
    public void setDefaultUniforms(Mode mode, Matrix4f matrix4f, Matrix4f matrix4f2, Window window) {
        super.setDefaultUniforms(mode, matrix4f, matrix4f2, window);

        Minecraft mc = Minecraft.getInstance();

        if (this.FRX_RENDER_FRAMES != null) {
            this.FRX_RENDER_FRAMES.set(
                ((GameRendererAccessor) mc.gameRenderer).canpipe_getFrame()
            );
        }
    }

}
