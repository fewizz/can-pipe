package fewizz.canpipe.pipeline;

import org.joml.Matrix4f;

public abstract class PassBase {

    public abstract void apply(Matrix4f view, Matrix4f projection);

}
