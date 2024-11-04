package fewizz.canpipe;

import net.minecraft.resources.ResourceLocation;

public class Material {

    final ResourceLocation vertexShaderSourceLocation;
    final ResourceLocation fragmentShaderSourceLocation;

    Material(ResourceLocation vertexShaderSourceLocation, ResourceLocation fragmentShaderSourceLocation) {
        this.vertexShaderSourceLocation = vertexShaderSourceLocation;
        this.fragmentShaderSourceLocation = fragmentShaderSourceLocation;
    }

}
