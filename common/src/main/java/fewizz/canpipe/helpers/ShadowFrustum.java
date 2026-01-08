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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;


// Inspired by https://iquilezles.org/articles/frustumcorrect/
public class ShadowFrustum extends Frustum {

    record Plane(
        Vector3f position,
        Vector3f normal
    ) {}

    private final Plane[] planes;
    private final Vector3f[] corners;

    // Still has some false positives, but it is good enough
    public ShadowFrustum(
        Matrix4f shadowViewMatrix, Matrix4f shadowProjectionView,
        Matrix4f shortendedViewProjectionMatrix, Vector3f toSunDir
    ) {
        super(shadowViewMatrix, shadowProjectionView);

        var frustumCorners = new Vector3f[8];
        for (int cornerIdx = 0; cornerIdx < 8; ++cornerIdx) {
            frustumCorners[cornerIdx] = shortendedViewProjectionMatrix.frustumCorner(cornerIdx, new Vector3f());
        }

        Set<IntIntPair> twoCornerIndicesToPlainIndex = new HashSet<>();

        int[][] planesCornersIndices = new int[][] {
            new int[] {CORNER_NXNYPZ, CORNER_NXPYPZ, CORNER_NXPYNZ, CORNER_NXNYNZ},  // PLANE_NX
            new int[] {CORNER_PXNYNZ, CORNER_PXPYNZ, CORNER_PXPYPZ, CORNER_PXNYPZ},  // PLANE_PX
            new int[] {CORNER_PXNYNZ, CORNER_PXNYPZ, CORNER_NXNYPZ, CORNER_NXNYNZ},  // PLANE_NY
            new int[] {CORNER_PXPYPZ, CORNER_PXPYNZ, CORNER_NXPYNZ, CORNER_NXPYPZ},  // PLANE_PY
            new int[] {CORNER_PXNYNZ, CORNER_NXNYNZ, CORNER_NXPYNZ, CORNER_PXPYNZ},  // PLANE_NZ
            new int[] {CORNER_PXNYPZ, CORNER_PXPYPZ, CORNER_NXPYPZ, CORNER_NXNYPZ},  // PLANE_PZ
        };

        List<Plane> planes = new ArrayList<>();
        Set<Vector3f> corners = new HashSet<>();

        for (int planeIdx = 0; planeIdx < 6; ++planeIdx) {
            var plane = shortendedViewProjectionMatrix.frustumPlane(planeIdx, new Vector4f());
            var primaryPlane = new Plane(
                plane.xyz(new Vector3f()).mul(-plane.w),
                plane.xyz(new Vector3f())
            );

            if (primaryPlane.normal.dot(toSunDir) < 0.0F) {
                continue;
            }

            planes.add(primaryPlane);
            var cornersIndices = planesCornersIndices[planeIdx];

            for (int a = 0; a < 4; ++a) {
                int b = (a + 1) % 4;

                int aIdx = cornersIndices[a];
                int bIdx = cornersIndices[b];

                corners.add(frustumCorners[aIdx]);
                corners.add(frustumCorners[bIdx]);

                var k = IntIntPair.of(aIdx, bIdx);
                var kReverse = IntIntPair.of(bIdx, aIdx);

                if (!twoCornerIndicesToPlainIndex.remove(kReverse)) {
                    twoCornerIndicesToPlainIndex.add(k);
                }
            }
        }

        for (var e : twoCornerIndicesToPlainIndex) {
            var aIdx = e.firstInt();
            var bIdx = e.secondInt();

            var normal = new Vector3f(frustumCorners[aIdx]).sub(frustumCorners[bIdx]).cross(toSunDir).normalize();
            planes.add(new Plane(frustumCorners[aIdx], normal));

            corners.add(new Vector3f(toSunDir).mul(10000.0F).add(frustumCorners[aIdx]));
            corners.add(new Vector3f(toSunDir).mul(10000.0F).add(frustumCorners[bIdx]));
        }

        this.planes = planes.toArray(new Plane[]{});
        this.corners = corners.toArray(new Vector3f[]{});
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
        // Can't (?) use FrustumIntersection.INSIDE, for faster occlusion graph traversal
        return result ? FrustumIntersection.INTERSECT : FrustumIntersection.OUTSIDE;
    }

    final private boolean check(
        float minX, float minY, float minZ, float maxX, float maxY, float maxZ
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

        for (Plane sidePlane : this.planes) {
            if (!isInside.apply(sidePlane)) {
                return false;
            }
        }

        Function<Function<Vector3f, Boolean>, Boolean> anyForEachFrustumCorner = (Function<Vector3f, Boolean> exp) -> {
            for (var frustumCorner : this.corners) {
                if (exp.apply(frustumCorner)) { return true; }
            }
            return false;
        };

        // It is possible for AABB to be "inside" of all planes, but still not inside frustum projection
        // This avoids most (not all) such false positives
        return
            anyForEachFrustumCorner.apply(c -> c.x > minX) &&
            anyForEachFrustumCorner.apply(c -> c.y > minY) &&
            anyForEachFrustumCorner.apply(c -> c.z > minZ) &&
            anyForEachFrustumCorner.apply(c -> c.x < maxX) &&
            anyForEachFrustumCorner.apply(c -> c.y < maxY) &&
            anyForEachFrustumCorner.apply(c -> c.z < maxZ);
    }

}
