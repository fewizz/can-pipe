package fewizz.canpipe.mixin;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.GlStateManager;

import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin implements TextureAtlasExtended {

    @Shadow
    private ResourceLocation location;

    @Shadow
    private Map<ResourceLocation, TextureAtlasSprite> texturesByName;

    @Unique
    Map<TextureAtlasSprite, Integer> spriteToIndex;

    @Unique
    AbstractTexture spritesData;

    @Inject(method = "upload", at = @At("TAIL"))
    void onUploadEnd(CallbackInfo ci) {
        this.spriteToIndex = new HashMap<>(texturesByName.size());

        int width = 1024;
        int height = Math.ceilDiv(texturesByName.size(), width);
        FloatBuffer buff = MemoryUtil.memAllocFloat(width*height*4);

        {
            int i = 0;
            for (TextureAtlasSprite s : texturesByName.values()) {
                spriteToIndex.put(s, i);
                buff.put(i*4+0, s.getU0());
                buff.put(i*4+1, s.getV0());
                buff.put(i*4+2, s.getU1());
                buff.put(i*4+3, s.getV1());
                i += 1;
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
                    GL33C.glTexImage2D(GL33C.GL_TEXTURE_2D, 0, GL33C.GL_RGBA32F, width, height, 0, GL33C.GL_RGBA, GL33C.GL_FLOAT, buff);
                    KHRDebug.glObjectLabel(GL33C.GL_TEXTURE, getId(), location.toString()+"-sprites-extents");
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

    @Override
    public int indexBySprite(TextureAtlasSprite sprite) {
        return this.spriteToIndex.get(sprite);
    }

}
