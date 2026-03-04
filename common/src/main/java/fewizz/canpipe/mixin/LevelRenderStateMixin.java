package fewizz.canpipe.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fewizz.canpipe.mixininterface.LevelRenderStateExtended;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;

@Mixin(LevelRenderState.class)
public class LevelRenderStateMixin implements LevelRenderStateExtended {

    @SuppressWarnings("unchecked")
    public final List<EntityRenderState>[] canpipe_entityRenderStates =
        Stream.generate(() -> new ArrayList<EntityRenderState>())
        .limit(4).toArray(List[]::new);

    public List<EntityRenderState>[] canpipe_getEntityRenderStates() {
        return this.canpipe_entityRenderStates;
    }

    @Inject(method = "reset", at = @At("RETURN"))
    void onReset(CallbackInfo ci) {
        for (List<EntityRenderState> canpipe_cascadeEntityRenderStates : this.canpipe_entityRenderStates) {
            canpipe_cascadeEntityRenderStates.clear();
        }
    }

}
