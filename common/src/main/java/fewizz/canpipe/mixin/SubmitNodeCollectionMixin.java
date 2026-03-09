package fewizz.canpipe.mixin;

import fewizz.canpipe.helpers.ItemSubmitExtra;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin implements SubmitNodeCollectorExtended {

    @Shadow @Final private List<SubmitNodeStorage.ItemSubmit> itemSubmits;

    @Unique private ItemSubmitExtra canpipe_pendingItemSubmitExtra = null;
    @Unique private final Map<SubmitNodeStorage.ItemSubmit, ItemSubmitExtra> canpipe_itemSubmitExtras = new HashMap<>();

    @Override
    public void canpipe_setPendingItemSubmitExtra(ItemSubmitExtra extra) {
        this.canpipe_pendingItemSubmitExtra = extra;
    }

    @Override
    public Map<SubmitNodeStorage.ItemSubmit, ItemSubmitExtra> canpipe_getItemSubmitExtras() {
        return this.canpipe_itemSubmitExtras;
    }

    @Inject(method = "submitItem", at = @At("RETURN"))
    void onItemSubmit(CallbackInfo ci) {
        this.canpipe_itemSubmitExtras.put(this.itemSubmits.getLast(), this.canpipe_pendingItemSubmitExtra);
        this.canpipe_pendingItemSubmitExtra = null;
    }

    @Inject(method = "clear", at = @At("RETURN"))
    void onClear(CallbackInfo ci) {
        this.canpipe_pendingItemSubmitExtra = null;  // Should be null here
        this.canpipe_itemSubmitExtras.clear();
    }

}
