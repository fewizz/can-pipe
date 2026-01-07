package fewizz.canpipe.helpers;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import static org.joml.Matrix4fc.*;
import org.joml.Vector3f;
import org.joml.Vector4f;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;


// Inspired by https://iquilezles.org/articles/frustumcorrect/
public class ShadowFrustum extends Frustum {

    private final Camera camera;
    private final Vector4f[] mPlanes = new Vector4f[6];
    private final Vector3f[] mCorners = new Vector3f[8];
    private final Vector3f toSunDir;

    // Still has some false positives, but it is good enough
    public ShadowFrustum(
        Matrix4f shadowViewMatrix, Matrix4f shadowProjectionView,
        Matrix4f shortendedViewProjectionMatrix, Camera camera,Vector3f toSunDir
    ) {
        super(shadowViewMatrix, shadowProjectionView);
        this.prepare(camera.position().x, camera.position().y, camera.position().z);
        this.camera = camera;
        this.toSunDir = toSunDir;
        for (int i = 0; i < 6; ++i) {
            this.mPlanes[i] = shortendedViewProjectionMatrix.frustumPlane(i, new Vector4f());
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
            (float) (aabb.minX - this.camera.position().x),
            (float) (aabb.minY - this.camera.position().y),
            (float) (aabb.minZ - this.camera.position().z),
            (float) (aabb.maxX - this.camera.position().x),
            (float) (aabb.maxY - this.camera.position().y),
            (float) (aabb.maxZ - this.camera.position().z)
        );
    }

    @Override
    public int cubeInFrustum(BoundingBox bb) {  // Used mostly by SectionOcclusionGraph.addSectionsInFrustum
        /*int result = super.cubeInFrustum(boundingBox);
        if (!(result == FrustumIntersection.INSIDE || result == FrustumIntersection.INTERSECT)) {
            return result;
        }*/
        boolean result = this.check(
            (float) (bb.minX() - this.camera.position().x),
            (float) (bb.minY() - this.camera.position().y),
            (float) (bb.minZ() - this.camera.position().z),
            (float) (bb.maxX() + 1 - this.camera.position().x),
            (float) (bb.maxY() + 1 - this.camera.position().y),
            (float) (bb.maxZ() + 1 - this.camera.position().z)
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
        var plane = this.mPlanes[planeIdx];

        if (plane.xyz(new Vector3f()).dot(this.toSunDir) < 0.0F) {
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

        if (!isInside.apply(
            plane.xyz(new Vector3f()).mul(-plane.w),
            plane.xyz(new Vector3f())
        )) {
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
            int v = (k+1) % 4;
            var normal = new Vector3f(frustumCorners[k]).sub(frustumCorners[v]).cross(this.toSunDir).normalize();

            if (!isInside.apply(frustumCorners[k], normal)) {
                return false;
            }
        }
        
        Function<Function<Vector3f, Boolean>, Boolean> anyForEachFrustumCorner = (
            Function<Vector3f, Boolean> exp
        ) -> {
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
