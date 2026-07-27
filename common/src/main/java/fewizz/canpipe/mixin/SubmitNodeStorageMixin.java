package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import fewizz.canpipe.material.EntityMaterialMap;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.resources.model.sprite.SpriteId;

@Mixin(SubmitNodeStorage.class)
public class SubmitNodeStorageMixin implements SubmitNodeCollectorExtended {

    @Shadow @Final private Int2ObjectAVLTreeMap<SubmitNodeCollection> submitsPerOrder;

    @Shadow public SubmitNodeCollection order(final int order) { return null; }

    @Unique static private EntityMaterialMap canpipe_modelSubmitMaterialMap = null;

    @ModifyReturnValue(method = "lambda$order$0", at = @At("RETURN"))
    static private SubmitNodeCollection onSubmitNodeCollectionInit(SubmitNodeCollection snc) {
        ((SubmitNodeCollectorExtended) snc).canpipe_setScopedModelMaterialMap(canpipe_modelSubmitMaterialMap);
        return snc;
    }

    @Override
    public void canpipe_setPendingItemSubmitMaterialMap(MaterialMap materialMap) {
        ((SubmitNodeCollectorExtended) this.order(0)).canpipe_setPendingItemSubmitMaterialMap(materialMap);
    }

    @Override
    public void canpipe_setPendingBlockSubmitMaterialMap(MaterialMap materialMap) {
        ((SubmitNodeCollectorExtended) this.order(0)).canpipe_setPendingBlockSubmitMaterialMap(materialMap);
    }

    @Override
    public void canpipe_setScopedModelMaterialMap(EntityMaterialMap materialMap) {
        canpipe_modelSubmitMaterialMap = materialMap;
        for (SubmitNodeCollection snc : this.submitsPerOrder.values()) {
            ((SubmitNodeCollectorExtended) snc).canpipe_setScopedModelMaterialMap(materialMap);
        }
    }

    @Override
    public void canpipe_setPendingModelEntityGlint() {
        ((SubmitNodeCollectorExtended) this.order(0)).canpipe_setPendingModelEntityGlint();
    }

    @Override
    public void canpipe_setPendingSpriteID(SpriteId spriteId) {
        ((SubmitNodeCollectorExtended) this.order(0)).canpipe_setPendingSpriteID(spriteId);
    }

}
