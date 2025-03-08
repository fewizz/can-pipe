package fewizz.canpipe.mixin;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Map;

import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.GlStateManager;

import fewizz.canpipe.GFX;
import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import fewizz.canpipe.mixininterface.TextureAtlasSpriteExtended;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin implements TextureAtlasExtended {

    @Shadow private ResourceLocation location;
    @Shadow private Map<ResourceLocation, TextureAtlasSprite> texturesByName;

    @Unique AbstractTexture spritesData;

    @Inject(method = "upload", at = @At("TAIL"))
    void onUploadEnd(CallbackInfo ci) {

        int width = 1024;
        int height = Math.ceilDiv(texturesByName.size(), width);
        ByteBuffer byteBuff = MemoryUtil.memAlloc(width*height*4*Float.BYTES);
        FloatBuffer buff = byteBuff.asFloatBuffer();

        {
            int index = 0;
            for (TextureAtlasSprite s : texturesByName.values()) {
                ((TextureAtlasSpriteExtended) s).setIndex(index);
                buff.put(index*4+0, s.getU0());
                buff.put(index*4+1, s.getV0());
                buff.put(index*4+2, s.getU1());
                buff.put(index*4+3, s.getV1());
                index += 1;
            }
        }

        try {
            spritesData = new AbstractTexture() {
                {
                    bind();
                    GlStateManager._texParameter(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_NEAREST);
                    GlStateManager._texParameter(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_NEAREST);
                    GlStateManager._texParameter(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_CLAMP_TO_EDGE);
                    GlStateManager._texParameter(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_CLAMP_TO_EDGE);
                    GlStateManager._texImage2D(GL33C.GL_TEXTURE_2D, 0, GL33C.GL_RGBA32F, width, height, 0, GL33C.GL_RGBA, GL33C.GL_FLOAT, byteBuff.asIntBuffer());
                    GFX.glObjectLabel(GL33C.GL_TEXTURE, getId(), location.toString()+"-sprites-extents");
                }
            };
        } finally {
            MemoryUtil.memFree(buff);
        }
    }

    @Inject(method = "clearTextureData", at = @At("TAIL"))
    public void onClearTextureData(CallbackInfo ci) {
        if (this.spritesData != null) {
            this.spritesData.close();
        }
    }

    @Override
    public AbstractTexture getSpriteData() {
        return this.spritesData;
    }

}
