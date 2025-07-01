package fewizz.canpipe.mixin.m03_core;

import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.opengl.GlTexture;

import fewizz.canpipe.TextureType;
import fewizz.canpipe.mixin.m02_texture_targets.GlStateManagerAccessor;
import fewizz.canpipe.mixininterface.GpuTextureExtended;

@Mixin(GlTexture.class)
public class GlTextureMixin implements GpuTextureExtended {

    @Shadow @Final protected int id;

    @Override
    public TextureType canpipe_getType() {
        int target = GlStateManagerAccessor.canpipe_getTextureTarget(this.id);
        if (target == GL33C.GL_TEXTURE_2D) { return TextureType.TYPE_2D; }
        else if (target == GL33C.GL_TEXTURE_2D_ARRAY) { return TextureType.TYPE_2D_ARRAY; }
        else if (target == GL33C.GL_TEXTURE_CUBE_MAP) { return TextureType.TYPE_CUBE_MAP; }
        else { throw new RuntimeException("Unexpected texture target: "+target); }
    }

}
