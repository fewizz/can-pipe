package fewizz.canpipe.light;

import blue.endless.jankson.JsonObject;

public class Light {
    public final float intensity;
    public final float red;
    public final float green;
    public final float blue;
    public final boolean worksInFluid;
    public final float innerConeAngle;
    public final float outerConeAngle;

    Light(JsonObject json) {
        this.intensity = json.getFloat("intensity", 0.0F);
        this.red = json.getFloat("red", 0.0F);
        this.green = json.getFloat("green", 0.0F);
        this.blue = json.getFloat("blue", 0.0F);
        this.worksInFluid = json.getBoolean("worksInFluid", false);
        this.innerConeAngle = (float) Math.toRadians(json.getFloat("innerConeAngleDegrees", 360.0F)) * 0.5F;
        this.outerConeAngle = (float) Math.toRadians(json.getFloat("outerConeAngleDegrees", 360.0F)) * 0.5F;
    }

}
