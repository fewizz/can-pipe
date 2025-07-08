package fewizz.canpipe.b3d.mixin;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.GpuOutOfMemoryException;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlDebugLabel;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.opengl.GlShaderModule;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;

import fewizz.canpipe.b3d.GpuDeviceExtended;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.ResourceLocation;

@Mixin(GlDevice.class)
public abstract class GlDeviceMixin implements GpuDeviceExtended {

    @Shadow @Final private GlDebugLabel debugLabels;
    @Shadow abstract GlRenderPipeline compilePipeline(RenderPipeline pipeline, BiFunction<ResourceLocation, ShaderType, String> shaderSource);

    @Unique private Consumer<String> canpipe_onCompilationError = null;
    @Unique private String canpipe_compilationLog = null;
    @Unique private Object2IntMap<Pair<List<GlTextureView>, GlTextureView>> canpipe_framebufferCache = new Object2IntOpenHashMap<>();
    @Unique private int canpipe_pendingTextureViewBaseLayer = -1;
    @Unique private int canpipe_pendingTextureViewLayerCount = -1;

    @Override
    public CompiledRenderPipeline canpipe_compilePipeline(
        RenderPipeline pipeline,
        BiFunction<ResourceLocation, ShaderType, String> shaderSource,
        Consumer<String> onCompilationError
    ) {
        try {
            this.canpipe_onCompilationError = onCompilationError;
            return compilePipeline(pipeline, shaderSource);
        } finally {
            this.canpipe_onCompilationError = null;
            this.canpipe_compilationLog = null;
        }
    }

    @ModifyExpressionValue(
        method = "compileShader",
        at = @At(
            value = "CONSTANT",
            args = "intValue=32768"
        )
    )
    int extendMaxLogLength(int original, @Local(index = 0) int id) {
        int logLength = GlStateManager.glGetShaderi(id, GL33C.GL_INFO_LOG_LENGTH);
        return logLength;
    }

    @ModifyExpressionValue(
        method = "compileShader",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlStateManager;glGetShaderInfoLog(II)Ljava/lang/String;"
        )
    )
    String catchLog(String log) {
        this.canpipe_compilationLog = log;
        return log;
    }

    @ModifyReturnValue(method = "compileShader", at = @At("RETURN"))
    GlShaderModule onCompilationError(GlShaderModule module) {
        if (module == GlShaderModule.INVALID_SHADER && this.canpipe_onCompilationError != null) {
            this.canpipe_onCompilationError.accept(this.canpipe_compilationLog);
        }
        return module;
    }

    @Overwrite
    @Override
    public GpuTexture createTexture(@Nullable String string, int usage, TextureFormat textureFormat, int w, int h, int depthOrLayers, int mipLevels) {
        if (mipLevels < 1) {
            throw new IllegalArgumentException("mipLevels must be at least 1");
        } else if (depthOrLayers < 1) {
            throw new IllegalArgumentException("depthOrLayers must be at least 1");
        } else {
            boolean bl = (usage & 16) != 0;
            if (bl) {
                if (w != h) {
                    throw new IllegalArgumentException("Cubemap compatible textures must be square, but size is " + w + "x" + h);
                }

                if (depthOrLayers % 6 != 0) {
                    throw new IllegalArgumentException("Cubemap compatible textures must have a layer count with a multiple of 6, was " + depthOrLayers);
                }

                if (depthOrLayers > 6) {
                    throw new UnsupportedOperationException("Array textures are not yet supported");
                }
            } else if (depthOrLayers > 1) {
                // throw new UnsupportedOperationException("Array or 3D textures are not yet supported");
            }

            GlStateManager.clearGlErrors();
            int id = GlStateManager._genTexture();
            int target = GL33C.GL_TEXTURE_2D;

            if (bl) {
                target = GL33C.GL_TEXTURE_CUBE_MAP;
            }
            else if (depthOrLayers > 1) {
                target = GL33C.GL_TEXTURE_2D_ARRAY;
            }

            if (string == null) {
                string = String.valueOf(id);
            }

            GlStateManagerAccessor.canpipe_setTextureTarget(id, target);
            GlStateManager._bindTexture(id);

            GlStateManager._texParameter(target, GL33C.GL_TEXTURE_MAX_LEVEL, mipLevels - 1);
            GlStateManager._texParameter(target, GL33C.GL_TEXTURE_MIN_LOD, 0);
            GlStateManager._texParameter(target, GL33C.GL_TEXTURE_MAX_LOD, mipLevels - 1);
            if (textureFormat.hasDepthAspect()) {
                GlStateManager._texParameter(target, GL33C.GL_TEXTURE_COMPARE_MODE, 0);
            }

            if (bl) {
                for (int p : GlConst.CUBEMAP_TARGETS) {
                    for (int lod = 0; lod < mipLevels; lod++) {
                        GlStateManager._texImage2D(
                            p, lod, GlConst.toGlInternalId(textureFormat), w >> lod, h >> lod, 0, GlConst.toGlExternalId(textureFormat), GlConst.toGlType(textureFormat), null
                        );
                    }
                }
            } else {
                for (int lod = 0; lod < mipLevels; lod++) {
                    if (target == GL33C.GL_TEXTURE_2D) {
                        GlStateManager._texImage2D(
                            target, lod, GlConst.toGlInternalId(textureFormat), w >> lod, h >> lod, 0, GlConst.toGlExternalId(textureFormat), GlConst.toGlType(textureFormat), null
                        );
                    }
                    else {
                        GL33C.glTexImage3D(GL33C.GL_TEXTURE_2D_ARRAY, lod, GlConst.toGlInternalId(textureFormat), w, h, depthOrLayers, 0, GlConst.toGlExternalId(textureFormat), GlConst.toGlType(textureFormat), (ByteBuffer) null);
                    }
                }
            }

            int r = GlStateManager._getError();
            if (r == 1285) {
                throw new GpuOutOfMemoryException("Could not allocate texture of " + w + "x" + h + " for " + string);
            } else if (r != 0) {
                throw new IllegalStateException("OpenGL error " + r);
            } else {
                GlTexture glTexture = new GlTexture(usage, string, textureFormat, w, h, depthOrLayers, mipLevels, id);
                this.debugLabels.applyLabel(glTexture);
                return glTexture;
            }
        }
    }

    @Override
    public GpuTextureView canpipe_createTextureView(
        GpuTexture gpuTexture, int baseMip, int levelCount,
        int baseLayer, int layerCount // added
    ) {
        try {
            this.canpipe_pendingTextureViewBaseLayer = baseLayer;
            this.canpipe_pendingTextureViewLayerCount = layerCount;
            return this.createTextureView(gpuTexture, baseMip, levelCount);
        }
        finally {
            this.canpipe_pendingTextureViewBaseLayer = -1;
            this.canpipe_pendingTextureViewLayerCount = -1;
        }
    }

}
