package fewizz.canpipe.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.helpers.ShadowFrustum;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import fewizz.canpipe.mixininterface.LevelRenderStateExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.util.profiling.ProfilerFiller;

@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private LevelRenderState levelRenderState;
    @Shadow @Final private LevelRenderer levelRenderer;

    @Shadow private void extractVisibleEntities(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState output) {}
    @Shadow private void extractVisibleBlockEntities(final Camera camera, final float deltaPartialTick, final LevelRenderState levelRenderState) {}
    @Shadow private void applyFrustum(Frustum frustum) {}

    @Inject(
        method = "extract",
        /*at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;getCullFrustum()Lnet/minecraft/client/renderer/culling/Frustum;"
        )*/
        at = @At("RETURN")
    )
    void extractShadowedEntities(
        CallbackInfo ci,
        @Local Camera camera,
        @Local DeltaTracker deltaTracker,
        @Local ProfilerFiller profiler,
        @Local(ordinal = 0) float dt
        // @Local(ordinal = 0) Matrix4f viewMatrix
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null || p.shadows == null) { return; }

        GameRendererExtended gre = (GameRendererExtended) this.minecraft.gameRenderer;
        LevelRendererExtended lre = (LevelRendererExtended) this.minecraft.levelRenderer;
        LevelRenderStateExtended lrse = ((LevelRenderStateExtended) this.levelRenderState);

        try {
            lre.canpipe_setCurrentShadowCascadeIdx(0);
            for (; lre.canpipe_getCurrentShadowCascadeIdx() < p.shadows.cascadeRadii().size()+1; lre.canpipe_setCurrentShadowCascadeIdx(lre.canpipe_getCurrentShadowCascadeIdx()+1)) {
                profiler.popPush("can-pipe cascade "+lre.canpipe_getCurrentShadowCascadeIdx());

                ShadowFrustum frustum = gre.canpipe_getShadowFrustums()[lre.canpipe_getCurrentShadowCascadeIdx()];
                this.applyFrustum(frustum);

                profiler.popPush("shadowed entities");
                this.extractVisibleEntities(camera, frustum, deltaTracker, this.levelRenderState);

                profiler.popPush("shadowed block entities");
                this.extractVisibleBlockEntities(camera, dt, this.levelRenderState);

                profiler.popPush("particles");
                ParticlesRenderState state = lrse.canpipe_getParticlesRenderStates()[lre.canpipe_getCurrentShadowCascadeIdx()];
                this.minecraft.particleEngine.extract(state, frustum, camera, dt);

                profiler.pop();
            }
        } finally {
            lre.canpipe_setCurrentShadowCascadeIdx(-1);
        }
    }

    @WrapMethod(method = "extractVisibleEntities")
    void suppressEntityShadows(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState output, Operation<Void> operation) {
        Pipeline p = Pipelines.getCurrent();
        boolean disableEntityShadows = p != null && p.shadows != null;
        Minecraft mc = Minecraft.getInstance();

        boolean originalEntityShadowsOptionValue = mc.options.entityShadows().get();

        try {
            if (disableEntityShadows) {
                mc.options.entityShadows().set(false);
            }
            operation.call(camera, frustum, deltaTracker, output);
        }
        finally {
            if (disableEntityShadows) {
                mc.options.entityShadows().set(originalEntityShadowsOptionValue);
            }
        }
    }

    @ModifyExpressionValue(
        method = "extractVisibleEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;isDetached()Z"
        )
    )
    private boolean addPlayerWhenCollectingVisibleEntities(boolean original) {
        return original || ((LevelRendererExtended) this.levelRenderer).canpipe_getCurrentShadowCascadeIdx() >= 0;
    }

    @ModifyExpressionValue(
        method = {"extractVisibleEntities"},
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/state/level/LevelRenderState;entityRenderStates:Ljava/util/List;"
        )
    )
    List<EntityRenderState> replaceEntityRenderStates(List<EntityRenderState> entityRenderStates) {
        int cascade = ((LevelRendererExtended) this.levelRenderer).canpipe_getCurrentShadowCascadeIdx();
        if (cascade >= 0) {
            return ((LevelRenderStateExtended) this.levelRenderState).canpipe_getEntityRenderStates()[cascade];
        }
        return entityRenderStates;
    }

}
