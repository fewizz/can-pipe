package fewizz.canpipe.b3d;

import java.util.List;

public interface GpuShaderModule {

    boolean canpipe_isGettingOutputVariablesNamesSupported();
    List<String> canpipe_getOutputVariablesNames();

}
