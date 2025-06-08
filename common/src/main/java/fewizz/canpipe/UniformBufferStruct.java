package fewizz.canpipe;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.util.Mth;

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
        std140Builder.align(RenderSystem.getDevice().getUniformOffsetAlignment());
    }

    public int size() {
        return Mth.roundToward(this.sizeCalculator.get(), RenderSystem.getDevice().getUniformOffsetAlignment());
    }

    // Same as net.minecraft.client.renderer.UniformValue, but not a record
    public static abstract class UniformValue {
		abstract public void writeTo(Std140Builder std140Builder);
		abstract public void addSize(Std140SizeCalculator std140SizeCalculator);
    };

    public static class FloatUniform extends UniformValue {
        public float value = 0.0F;
        @Override public void writeTo(Std140Builder std140Builder) { std140Builder.putFloat(this.value); }
        @Override public void addSize(Std140SizeCalculator std140SizeCalculator) { std140SizeCalculator.putFloat(); }
    }

    public static class IntUniform extends UniformValue {
        public int value = 0;
        @Override public void writeTo(Std140Builder std140Builder) { std140Builder.putInt(this.value); }
        @Override public void addSize(Std140SizeCalculator std140SizeCalculator) { std140SizeCalculator.putInt(); }
    }

    public static class Vec2Uniform extends UniformValue {
        public final Vector2f value = new Vector2f();
        @Override public void writeTo(Std140Builder std140Builder) { std140Builder.putVec2(this.value); }
        @Override public void addSize(Std140SizeCalculator std140SizeCalculator) { std140SizeCalculator.putVec2(); }
    }

    public static class Vec3Uniform extends UniformValue {
        public final Vector3f value = new Vector3f();
        @Override public void writeTo(Std140Builder std140Builder) { std140Builder.putVec3(this.value); }
        @Override public void addSize(Std140SizeCalculator std140SizeCalculator) { std140SizeCalculator.putVec3(); }
    }

    public static class Vec4Uniform extends UniformValue {
        public final Vector4f value = new Vector4f();
        @Override public void writeTo(Std140Builder std140Builder) { std140Builder.putVec4(this.value); }
        @Override public void addSize(Std140SizeCalculator std140SizeCalculator) { std140SizeCalculator.putVec4(); }
    }

    public static class IVec2Uniform extends UniformValue {
        public final Vector2i value = new Vector2i();
        @Override public void writeTo(Std140Builder std140Builder) { std140Builder.putIVec2(this.value); }
        @Override public void addSize(Std140SizeCalculator std140SizeCalculator) { std140SizeCalculator.putIVec2(); }
    }

    public static class Mat4Uniform extends UniformValue {
        public final Matrix4f value = new Matrix4f();
        @Override public void writeTo(Std140Builder std140Builder) { std140Builder.putMat4f(this.value); }
        @Override public void addSize(Std140SizeCalculator std140SizeCalculator) { std140SizeCalculator.putMat4f(); }
    }

}
