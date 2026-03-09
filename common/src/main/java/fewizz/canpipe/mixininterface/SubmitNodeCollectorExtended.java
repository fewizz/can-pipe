package fewizz.canpipe.mixininterface;

import fewizz.canpipe.helpers.ItemSubmitExtra;
import net.minecraft.client.renderer.SubmitNodeStorage;

import java.util.Map;

public interface SubmitNodeCollectorExtended /*extends OrderedSubmitNodeCollector*/ {

    void canpipe_setPendingItemSubmitExtra(ItemSubmitExtra extra);
    Map<SubmitNodeStorage.ItemSubmit, ItemSubmitExtra> canpipe_getItemSubmitExtras();

}
