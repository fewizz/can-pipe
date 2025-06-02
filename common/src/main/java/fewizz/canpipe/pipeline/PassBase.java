package fewizz.canpipe.pipeline;

import org.joml.Matrix4f;

public abstract class PassBase {

    public final String name;

    PassBase(String name) {
        this.name = name;
    }

    public abstract void apply(Matrix4f view, Matrix4f projection);

}
