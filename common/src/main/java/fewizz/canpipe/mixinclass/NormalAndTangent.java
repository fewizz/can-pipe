package fewizz.canpipe.mixinclass;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3f;

public class NormalAndTangent extends Vector3f {

    public float tangentX;
    public float tangentY;
    public float tangentZ;
    public boolean inverseBitangent;

    public NormalAndTangent(Vector3f other) {
        super(other);
    }

    /**
     * Taken from
     * <a href="https://github.com/vram-guild/frex/blob/1.19/common/src/main/java/io/vram/frex/base/renderer/mesh/BaseQuadView.java#L261">
     * BaseQuadView.computePackedFaceTangent
     * </a>
     * method, but i have so many questions...
     * <p>
     * Why {@code inverseLength} is named like that?
     * Resulting {@code vec3(tx, ty, tz)} is almost never has length 1.0, and
     * {@code PackedVector3f.pack(tx, ty, tz, inverted)} packs unnormalized vector, clamping components
     * <p>
     * Why bitangent isn't provided to shaders? tangent and bitanget are not necessarily orthogonal
    */
    public static Pair<Vector3f, Boolean> computeTangent(
        Vector3f normal,
        float x0, float y0, float z0, float u0, float v0,
        float x1, float y1, float z1, float u1, float v1,
        float x2, float y2, float z2, float u2, float v2
    ) {
        float du0 = u2 - u1;
        float dv0 = -(v2 - v1);
        float du1 = u0 - u1;
        float dv1 = -(v0 - v1);

        float dx0 = x2 - x1;
        float dy0 = y2 - y1;
        float dz0 = z2 - z1;
        float dx1 = x0 - x1;
        float dy1 = y0 - y1;
        float dz1 = z0 - z1;

        // we don't care about magnitudes, assume that TBN has orthonormal basis
        float determinantSign = Math.signum(du0*dv1 - du1*dv0);

        float tx = determinantSign * ( dv1*dx0 + -dv0*dx1);
        float ty = determinantSign * ( dv1*dy0 + -dv0*dy1);
        float tz = determinantSign * ( dv1*dz0 + -dv0*dz1);

        float bx = determinantSign * (-du1*dx0 +  du0*dx1);
        float by = determinantSign * (-du1*dy0 +  du0*dy1);
        float bz = determinantSign * (-du1*dz0 +  du0*dz1);

        // cross product of tangent and bitangent
        float cx = ty*bz - tz*by;
        float cy = tz*bx - tx*bz;
        float cz = tx*by - ty*bx;

        return Pair.of(
            new Vector3f(tx, ty, tz).normalize(),
            // if true, then bitangent should be inversed
            normal.x*cx + normal.y*cy + normal.z*cz < 0.0
        );
    }

    public static Vector3f computeNormal(
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2
    ) {
        float dx0 = x2 - x1;
        float dy0 = y2 - y1;
        float dz0 = z2 - z1;
        float dx1 = x0 - x1;
        float dy1 = y0 - y1;
        float dz1 = z0 - z1;

        float nx = dy0*dz1 - dz0*dy1;
        float ny = dz0*dx1 - dx0*dz1;
        float nz = dx0*dy1 - dy0*dx1;

        return new Vector3f(nx, ny, nz).normalize();
    }

}
