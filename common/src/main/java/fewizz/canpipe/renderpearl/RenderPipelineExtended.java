package fewizz.canpipe.renderpearl;

import java.util.Set;

import org.jspecify.annotations.Nullable;

public interface RenderPipelineExtended {

    // Used for GL backend, to suppress warnings about unused samplers
    @Nullable Set<String> canpipe_getOptionalSamplers();

}
