package fewizz.canpipe.helpers;

import static org.joml.Matrix4fc.CORNER_NXNYNZ;
import static org.joml.Matrix4fc.CORNER_NXNYPZ;
import static org.joml.Matrix4fc.CORNER_NXPYNZ;
import static org.joml.Matrix4fc.CORNER_NXPYPZ;
import static org.joml.Matrix4fc.CORNER_PXNYNZ;
import static org.joml.Matrix4fc.CORNER_PXNYPZ;
import static org.joml.Matrix4fc.CORNER_PXPYNZ;
import static org.joml.Matrix4fc.CORNER_PXPYPZ;
import static org.joml.Matrix4fc.PLANE_NX;
import static org.joml.Matrix4fc.PLANE_NY;
import static org.joml.Matrix4fc.PLANE_NZ;
import static org.joml.Matrix4fc.PLANE_PX;
import static org.joml.Matrix4fc.PLANE_PY;
import static org.joml.Matrix4fc.PLANE_PZ;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;


// Inspired by https://iquilezles.org/articles/frustumcorrect/
public class ShadowFrustum extends Frustum {

    record Plane(
        Vector3f normal,
        float d
    ) {}

    private final Vector3f[] mCorners = new Vector3f[8];
    private final Vector3f toSunDir;

    private final Plane[] primaryPlanes = new Plane[6];

    // Still has some false positives, but it is good enough
    public ShadowFrustum(
        Matrix4f shadowViewMatrix, Matrix4f shadowProjectionView,
        Matrix4f shortendedViewProjectionMatrix, Vector3f toSunDir
    ) {
        super(shadowViewMatrix, shadowProjectionView);

        this.toSunDir = toSunDir;
        for (int i = 0; i < 6; ++i) {
            var plane = shortendedViewProjectionMatrix.frustumPlane(i, new Vector4f());;
            this.primaryPlanes[i] = new Plane(
                plane.xyz(new Vector3f()),
                plane.w
            );
        }
        for (int i = 0; i < 8; ++i) {
            this.mCorners[i] = shortendedViewProjectionMatrix.frustumCorner(i, new Vector3f());
        }
    }

    

    @Override
    public boolean isVisible(AABB aabb) {  // Used mostly by LevelRenderer.extractVisibleEntities
        if (!super.isVisible(aabb)) {
            return false;
        }
        return this.check(
            (float) (aabb.minX - this.getCamX()),
            (float) (aabb.minY - this.getCamY()),
            (float) (aabb.minZ - this.getCamZ()),
            (float) (aabb.maxX - this.getCamX()),
            (float) (aabb.maxY - this.getCamY()),
            (float) (aabb.maxZ - this.getCamZ())
        );
    }

    @Override
    public int cubeInFrustum(BoundingBox bb) {  // Used mostly by SectionOcclusionGraph.addSectionsInFrustum
        /*int result = super.cubeInFrustum(boundingBox);
        if (!(result == FrustumIntersection.INSIDE || result == FrustumIntersection.INTERSECT)) {
            return result;
        }*/
        boolean result = this.check(
            (float) (bb.minX() - this.getCamX()),
            (float) (bb.minY() - this.getCamY()),
            (float) (bb.minZ() - this.getCamZ()),
            (float) (bb.maxX() + 1 - this.getCamX()),
            (float) (bb.maxY() + 1 - this.getCamY()),
            (float) (bb.maxZ() + 1 - this.getCamZ())
        );
        return result ? FrustumIntersection.INTERSECT : FrustumIntersection.OUTSIDE;
    }

    final private boolean check(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        float xSize = (maxX - minX);
        float ySize = (maxY - minY);
        float zSize = (maxZ - minZ);

        Vector3f[] aabbCorners = new Vector3f[8];
        for (int x = 0; x <= 1; ++x) {
            for (int y = 0; y <= 1; ++y) {
                for (int z = 0; z <= 1; ++z) {
                    aabbCorners[x + y*2 + z*4] =
                        new Vector3f()
                        .set(xSize, ySize, zSize)
                        .mul(x, y, z)
                        .add(minX, minY, minZ);
                }
            }
        }

        return (
            checkFrustumSide(minX, minY, minZ, maxX, maxY, maxZ, aabbCorners, PLANE_NX, CORNER_NXNYPZ, CORNER_NXPYPZ, CORNER_NXPYNZ, CORNER_NXNYNZ) ||
            checkFrustumSide(minX, minY, minZ, maxX, maxY, maxZ, aabbCorners, PLANE_PX, CORNER_PXNYNZ, CORNER_PXPYNZ, CORNER_PXPYPZ, CORNER_PXNYPZ) ||
            checkFrustumSide(minX, minY, minZ, maxX, maxY, maxZ, aabbCorners, PLANE_NY, CORNER_PXNYNZ, CORNER_PXNYPZ, CORNER_NXNYPZ, CORNER_NXNYNZ) ||
            checkFrustumSide(minX, minY, minZ, maxX, maxY, maxZ, aabbCorners, PLANE_PY, CORNER_PXPYPZ, CORNER_PXPYNZ, CORNER_NXPYNZ, CORNER_NXPYPZ) ||
            checkFrustumSide(minX, minY, minZ, maxX, maxY, maxZ, aabbCorners, PLANE_NZ, CORNER_PXNYNZ, CORNER_NXNYNZ, CORNER_NXPYNZ, CORNER_PXPYNZ) ||
            checkFrustumSide(minX, minY, minZ, maxX, maxY, maxZ, aabbCorners, PLANE_PZ, CORNER_PXNYPZ, CORNER_PXPYPZ, CORNER_NXPYPZ, CORNER_NXNYPZ)
        );
    }

    final private boolean checkFrustumSide(
        float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
        Vector3f[] aabbCorners, int planeIdx, int corner0, int corner1, int corner2, int corner3
    ) {
        Plane plane = this.primaryPlanes[planeIdx];

        if (plane.normal.dot(this.toSunDir) < 0.0F) {
            return false;
        }

        BiFunction<Vector3f, Vector3f, Boolean> isInside = (Vector3f pos, Vector3f normal) -> {
            for (var aabbCorner : aabbCorners) {
                if (new Vector3f(aabbCorner).sub(pos).dot(normal) >= 0.0F) {
                    return true;
                }
            }
            return false;
        };

        if (!isInside.apply(new Vector3f(plane.normal).mul(-plane.d), plane.normal)) {
            return false;
        }

        var frustumCorners = new Vector3f[] {
            this.mCorners[corner0],
            this.mCorners[corner1],
            this.mCorners[corner2],
            this.mCorners[corner3],
            new Vector3f(this.toSunDir).mul(10000.0F).add(this.mCorners[corner0]),
            new Vector3f(this.toSunDir).mul(10000.0F).add(this.mCorners[corner1]),
            new Vector3f(this.toSunDir).mul(10000.0F).add(this.mCorners[corner2]),
            new Vector3f(this.toSunDir).mul(10000.0F).add(this.mCorners[corner3])
        };

        for (int k = 0; k < 4; ++k) {
            int v = (k + 1) % 4;
            var normal = new Vector3f(frustumCorners[k]).sub(frustumCorners[v]).cross(this.toSunDir).normalize();

            if (!isInside.apply(frustumCorners[k], normal)) {
                return false;
            }
        }
        
        Function<Function<Vector3f, Boolean>, Boolean> anyForEachFrustumCorner = (Function<Vector3f, Boolean> exp) -> {
            for (var frustumCorner : frustumCorners) {
                if (exp.apply(frustumCorner)) {
                    return true;
                }
            }
            return false;
        };

        return
            anyForEachFrustumCorner.apply(c -> c.x > minX) &&
            anyForEachFrustumCorner.apply(c -> c.y > minY) &&
            anyForEachFrustumCorner.apply(c -> c.z > minZ) &&
            anyForEachFrustumCorner.apply(c -> c.x < maxX) &&
            anyForEachFrustumCorner.apply(c -> c.y < maxY) &&
            anyForEachFrustumCorner.apply(c -> c.z < maxZ);
    }

}
