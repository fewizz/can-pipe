package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.renderer.entity.ItemRenderer;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
/* TODO
    @ModifyVariable(method = "renderItem", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static ItemStackRenderState.FoilType beforeRenderItem(
        ItemStackRenderState.FoilType foilType,
        @Local(argsOnly = true) MultiBufferSource bufferSource,
        @Local(argsOnly = true) RenderType renderType
    ) {
        if (Pipelines.getCurrent() != null && foilType != ItemStackRenderState.FoilType.NONE) {
            VertexConsumerExtended vce = (VertexConsumerExtended) bufferSource.getBuffer(renderType);
            vce.canpipe_setSharedGlint(true);
            foilType = null;
        }
        return foilType;
    }

    @Inject(
        method = "renderItem",
        at = @At("TAIL")
    )
    private static void afterRenderItem(CallbackInfo ci, @Local VertexConsumer vertexConsumer) {
        if (vertexConsumer instanceof VertexConsumerExtended vce) {
            vce.canpipe_setSharedGlint(false);
        }
    }
*/
}
