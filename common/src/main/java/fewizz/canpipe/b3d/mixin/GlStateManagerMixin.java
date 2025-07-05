package fewizz.canpipe.b3d.mixin;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.opengl.GlStateManager;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

@Mixin(value = GlStateManager.class, remap = false, priority = 1000)
public class GlStateManagerMixin {

    @Shadow @Final private static GlStateManager.TextureState[] TEXTURES;
    @Shadow private static int activeTexture;

    @Shadow public static void _bindTexture(int id) {}

    @Unique static private final Int2IntMap idToTargetMap = new Int2IntOpenHashMap();

    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 12))
    private static int increaseTextureCount(int original) {
        return 16;
    }

    @SuppressWarnings("unused")
    private static void canpipe_setTextureTarget(int id, int target) {
        idToTargetMap.put(id, target);
    }

    @SuppressWarnings("unused")
    private static int canpipe_getTextureTarget(int id) {
        return idToTargetMap.getOrDefault(id, GL33C.GL_TEXTURE_2D);
    }

    @Inject(
        method = "_bindTexture",
        at = @At(
            value = "FIELD",
            target = "Lcom/mojang/blaze3d/opengl/GlStateManager$TextureState;binding:I",
            ordinal = 1
        )
    )
    private static void unbindIfDifferentTextureTarget(int newID, CallbackInfo ci) {
        int prevTarget = idToTargetMap.getOrDefault(TEXTURES[activeTexture].binding, GL33C.GL_TEXTURE_2D);
        int newTarget = idToTargetMap.getOrDefault(newID, GL33C.GL_TEXTURE_2D);
        // new texture has different target, reset prev target to 0
        // otherwise we may have a situation where multiple texture targets are bound to same texture unit
        // https://community.khronos.org/t/binding-different-targets-to-same-unit/76935
        if (prevTarget != newTarget) {
            GL11C.glBindTexture(prevTarget, 0);
        }
    }

    @ModifyArg(
        method = "_bindTexture",
        at = @At(
            value = "INVOKE",
            target = "org.lwjgl.opengl.GL11.glBindTexture(II)V"
        ),
        index = 0
    )
    private static int specifyTextureTargetOnBind(int target, @Local(argsOnly = true, index = 0) int id) {
        return idToTargetMap.getOrDefault(id, target);
    }

    @Inject(method = "_deleteTexture", at = @At("RETURN"))
    private static void deleteTextureTarget(int id, CallbackInfo ci) {
        idToTargetMap.remove(id);
    }

}
