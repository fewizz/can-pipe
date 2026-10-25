package fewizz.canpipe.util;

import org.apache.commons.lang3.NotImplementedException;
import org.jspecify.annotations.Nullable;

import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;

import net.minecraft.resources.Identifier;

public class SimpleShaderSource implements ShaderSource {

    private final Identifier vertexShaderLocation;
    private final String vertexShaderSource;

    private final Identifier fragmentShaderLocation;
    private final String fragmentShaderSource;

    public SimpleShaderSource(
        Identifier vertexShaderLocation, String vertexShaderSource,
        Identifier fragmentShaderLocation, String fragmentShaderSource
    ) {
        this.vertexShaderLocation = vertexShaderLocation;
        this.vertexShaderSource = vertexShaderSource;

        this.fragmentShaderLocation = fragmentShaderLocation;
        this.fragmentShaderSource = fragmentShaderSource;
    }

    @Override
    public @Nullable String getShader(Identifier id, ShaderType type) {
        if (type == ShaderType.VERTEX) {
            if (id != this.vertexShaderLocation) {
                throw new RuntimeException("Expected vertex shader location to be "+vertexShaderLocation+", but got "+id);
            }
            return this.vertexShaderSource;
        }
        if (type == ShaderType.FRAGMENT) {
            if (id != fragmentShaderLocation) {
                throw new RuntimeException("Expected fragment shader location to be "+fragmentShaderLocation+", but got "+id);
            }
            return this.fragmentShaderSource;
        }
        throw new NotImplementedException("Unsupported shader type \""+type.getName()+"\"");
    }

    @Override
    public @Nullable CachedIncludeSource getInclude(Identifier id) { return null; }

    @Override
    public void close() {}
    
}
