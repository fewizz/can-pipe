package fewizz.canpipe.mixin.m04_core;

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

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;

import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import fewizz.canpipe.mixininterface.TextureAtlasSpriteExtended;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin implements TextureAtlasExtended {

    @Shadow private ResourceLocation location;
    @Shadow private Map<ResourceLocation, TextureAtlasSprite> texturesByName;

    @Unique GpuTexture spritesData;

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
            spritesData = RenderSystem
                .getDevice()
			    .createTexture(
                    location.toString()+"-sprites-extents",
                    TextureFormat.valueOf("RGBA32F"),
                    width, height, 1
                );
            // uses GL_UNSIGNED_BYTE internally, too lazy to patch
            /*RenderSystem.getDevice().createCommandEncoder().writeToTexture(
                spritesData, byteBuff.asIntBuffer(), Format.RGBA, 0, 0, 0, width, height
            );*/ //, so:
            GlStateManager._bindTexture(((GlTexture)spritesData).glId());
            GlStateManager._pixelStore(GL33C.GL_UNPACK_ROW_LENGTH, width);
            GlStateManager._pixelStore(GL33C.GL_UNPACK_SKIP_PIXELS, 0);
            GlStateManager._pixelStore(GL33C.GL_UNPACK_SKIP_ROWS, 0);
            GlStateManager._pixelStore(GL33C.GL_UNPACK_ALIGNMENT, 4);
            GlStateManager._texSubImage2D(
                GL33C.GL_TEXTURE_2D, 0, 0, 0, width, height,
                GL33C.GL_RGBA, GL33C.GL_FLOAT, byteBuff.asIntBuffer()
            );
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
    public GpuTexture canpipe_getSpriteData() {
        return this.spritesData;
    }

    @Override
    public Map<ResourceLocation, TextureAtlasSprite> canpipe_getSprites() {
        return texturesByName;
    }

}
