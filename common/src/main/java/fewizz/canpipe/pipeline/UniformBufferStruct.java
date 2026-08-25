package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;

public class UniformBufferStruct {
    final List<UniformValue> uniformValues = new ArrayList<>();
    final private Std140SizeCalculator sizeCalculator = new Std140SizeCalculator();

    public <T extends UniformValue> T add(T uniformValue) {
        this.uniformValues.add(uniformValue);
        uniformValue.addSize(this.sizeCalculator);
        return uniformValue;
    }

    public void writeTo(Std140Builder std140Builder) {
        for (var uniformValue : this.uniformValues) {
            uniformValue.writeTo(std140Builder);
        }
    }

    public int size() {
        return this.sizeCalculator.get();
    }

    // Same as net.minecraft.client.renderer.UniformValue, but not a record
    public interface UniformValue {
        void writeTo(Std140Builder std140Builder);
        void addSize(Std140SizeCalculator std140SizeCalculator);
    }

    public static class FloatUniform implements UniformValue {
        private float value = 0.0F;

        public void set(float value) {this.value = value;}
        public float get() {return this.value;}
        public void add(float value) {this.value += value;}

        @Override public void writeTo(Std140Builder std140Builder) { std140Builder.putFloat(this.value); }
        @Override public void addSize(Std140SizeCalculator std140SizeCalculator) { std140SizeCalculator.putFloat(); }
    }

    public static class IntUniform implements UniformValue {
        private int value = 0;

        public void set(int value) {this.value = value;}
        public int get() {return this.value;}
        public void add(int value) {this.value += value;}

        @Override public void writeTo(Std140Builder std140Builder) { std140Builder.putInt(this.value); }
        @Override public void addSize(Std140SizeCalculator std140SizeCalculator) { std140SizeCalculator.putInt(); }
    }

    public static class Vec2Uniform extends Vector2f implements UniformValue {
        @Override public void writeTo(Std140Builder std140Builder) { std140Builder.putVec2(this); }
        @Override public void addSize(Std140SizeCalculator std140SizeCalculator) { std140SizeCalculator.putVec2(); }
    }

    public static class Vec3Uniform extends Vector3f implements UniformValue {
        @Override public void writeTo(Std140Builder std140Builder) { std140Builder.putVec3(this); }
        @Override public void addSize(Std140SizeCalculator std140SizeCalculator) { std140SizeCalculator.putVec3(); }
    }

    public static class Vec4Uniform extends Vector4f implements UniformValue {
        @Override public void writeTo(Std140Builder std140Builder) { std140Builder.putVec4(this); }
        @Override public void addSize(Std140SizeCalculator std140SizeCalculator) { std140SizeCalculator.putVec4(); }
    }

    public static class IVec2Uniform extends Vector2i implements UniformValue {
        @Override public void writeTo(Std140Builder std140Builder) { std140Builder.putIVec2(this); }
        @Override public void addSize(Std140SizeCalculator std140SizeCalculator) { std140SizeCalculator.putIVec2(); }
    }

    public static class Mat4Uniform extends Matrix4f implements UniformValue {
        @Override public void writeTo(Std140Builder std140Builder) { std140Builder.putMat4f(this); }
        @Override public void addSize(Std140SizeCalculator std140SizeCalculator) { std140SizeCalculator.putMat4f(); }
    }

}
