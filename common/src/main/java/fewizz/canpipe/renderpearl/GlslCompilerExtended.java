package fewizz.canpipe.renderpearl;

import org.apache.commons.lang3.function.TriConsumer;

import net.minecraft.resources.Identifier;

public interface GlslCompilerExtended {

    void canpipe_setEnableAutoMapLocationsForPendingCompilation(Boolean value);
    void canpipe_setCompilationErrorListenerForPendingCompilation(TriConsumer<String, Identifier, String> listener);

}
