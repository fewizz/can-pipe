package fewizz.canpipe.helpers;

import static org.joml.Matrix4fc.CORNER_NXNYNZ;
import static org.joml.Matrix4fc.CORNER_NXNYPZ;
import static org.joml.Matrix4fc.CORNER_NXPYNZ;
import static org.joml.Matrix4fc.CORNER_NXPYPZ;
import static org.joml.Matrix4fc.CORNER_PXNYNZ;
import static org.joml.Matrix4fc.CORNER_PXNYPZ;
import static org.joml.Matrix4fc.CORNER_PXPYNZ;
import static org.joml.Matrix4fc.CORNER_PXPYPZ;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Stream;

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
        Vector3f position,
        Vector3f normal
    ) {}

    record ProjectedFrustum(
        Plane primarePlane,
        Plane[] sidePlanes,
        Vector3f[] corners
    ) {}

    private final ArrayList<ProjectedFrustum> projectedFrustums = new ArrayList<>();

    // Still has some false positives, but it is good enough
    public ShadowFrustum(
        Matrix4f shadowViewMatrix, Matrix4f shadowProjectionView,
        Matrix4f shortendedViewProjectionMatrix, Vector3f toSunDir
    ) {
        super(shadowViewMatrix, shadowProjectionView);

        var mCorners = new Vector3f[8];
        for (int i = 0; i < 8; ++i) {
            mCorners[i] = shortendedViewProjectionMatrix.frustumCorner(i, new Vector3f());
        }

        int[][] planesCornersIds = new int[][] {
            new int[] {CORNER_NXNYPZ, CORNER_NXPYPZ, CORNER_NXPYNZ, CORNER_NXNYNZ},
            new int[] {CORNER_PXNYNZ, CORNER_PXPYNZ, CORNER_PXPYPZ, CORNER_PXNYPZ},
            new int[] {CORNER_PXNYNZ, CORNER_PXNYPZ, CORNER_NXNYPZ, CORNER_NXNYNZ},
            new int[] {CORNER_PXPYPZ, CORNER_PXPYNZ, CORNER_NXPYNZ, CORNER_NXPYPZ},
            new int[] {CORNER_PXNYNZ, CORNER_NXNYNZ, CORNER_NXPYNZ, CORNER_PXPYNZ},
            new int[] {CORNER_PXNYPZ, CORNER_PXPYPZ, CORNER_NXPYPZ, CORNER_NXNYPZ},
        };

        for (int i = 0; i < 6; ++i) {
            var plane = shortendedViewProjectionMatrix.frustumPlane(i, new Vector4f());
            var primaryPlane = new Plane(
                plane.xyz(new Vector3f()).mul(-plane.w),
                plane.xyz(new Vector3f())
            );

            if (primaryPlane.normal.dot(toSunDir) < 0.0F) {
                continue;
            }

            var cornersIds = planesCornersIds[i];

            var frustumCorners = new Vector3f[] {
                mCorners[cornersIds[0]],
                mCorners[cornersIds[1]],
                mCorners[cornersIds[2]],
                mCorners[cornersIds[3]],
                new Vector3f(toSunDir).mul(10000.0F).add(mCorners[cornersIds[0]]),
                new Vector3f(toSunDir).mul(10000.0F).add(mCorners[cornersIds[1]]),
                new Vector3f(toSunDir).mul(10000.0F).add(mCorners[cornersIds[2]]),
                new Vector3f(toSunDir).mul(10000.0F).add(mCorners[cornersIds[3]])
            };

            var sidePlanes = new Plane[4];
            for (int a = 0; a < 4; ++a) {
                int b = (a + 1) % 4;
                var normal = new Vector3f(frustumCorners[a]).sub(frustumCorners[b]).cross(toSunDir).normalize();
                sidePlanes[a] = new Plane(frustumCorners[a], normal);
            }

            var projectedFrustum = new ProjectedFrustum(
                primaryPlane, sidePlanes, frustumCorners
            );

            this.projectedFrustums.add(projectedFrustum);
        }
    }

    

    @Override
    public boolean isVisible(AABB aabb) {  // Used mostly by LevelRenderer.extractVisibleEntities
        /*if (!super.isVisible(aabb)) {
            return false;
        }*/
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
        for (var projectedFrustum: this.projectedFrustums) {
            if (checkFrustumSide(minX, minY, minZ, maxX, maxY, maxZ, projectedFrustum)) {
                return true;
            }
        }
        return false;
    }

    final private boolean checkFrustumSide(
        float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
        ProjectedFrustum projectedFrustum
    ) {
        Function<Plane, Boolean> isInside = (Plane plane) -> {
            for (int x = 0; x <= 1; ++x) {
                for (int y = 0; y <= 1; ++y) {
                    for (int z = 0; z <= 1; ++z) {
                        var aabbCorner = new Vector3f(
                            x == 0 ? minX : maxX,
                            y == 0 ? minY : maxY,
                            z == 0 ? minZ : maxZ
                        );

                        if (aabbCorner.sub(plane.position).dot(plane.normal) >= 0.0F) {
                            return true;
                        }
                    }
                }
            }
            return false;
        };

        if (!isInside.apply(projectedFrustum.primarePlane)) {
            return false;
        }
        for (Plane sidePlane : projectedFrustum.sidePlanes) {
            if (!isInside.apply(sidePlane)) {
                return false;
            }
        }

        return
            Stream.of(projectedFrustum.corners).anyMatch(c -> c.x > minX) &&
            Stream.of(projectedFrustum.corners).anyMatch(c -> c.y > minY) &&
            Stream.of(projectedFrustum.corners).anyMatch(c -> c.z > minZ) &&
            Stream.of(projectedFrustum.corners).anyMatch(c -> c.x < maxX) &&
            Stream.of(projectedFrustum.corners).anyMatch(c -> c.y < maxY) &&
            Stream.of(projectedFrustum.corners).anyMatch(c -> c.z < maxZ);
    }

}
