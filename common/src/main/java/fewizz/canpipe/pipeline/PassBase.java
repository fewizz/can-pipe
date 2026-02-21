package fewizz.canpipe.pipeline;

import fewizz.canpipe.b3d.CommandEncoderBackendExtended;

public abstract class PassBase implements AutoCloseable {

    public final String name;

    PassBase(String name) {
        this.name = name;
    }


    public abstract void apply(CommandEncoderBackendExtended commandEncoder);

    @Override
    public abstract void close();

}
