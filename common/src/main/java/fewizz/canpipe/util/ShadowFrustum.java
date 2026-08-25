package fewizz.canpipe.util;

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

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NonNull;


// Inspired by https://iquilezles.org/articles/frustumcorrect/
public class ShadowFrustum extends Frustum {

    private record Plane(
        // Vector3f position,
        Vector3f normal,
        float positionDotNormal  // Some position on the plane, not necessary normal * -d
    ) {

        private Plane(Vector3f position, Vector3f normal) {
            this(normal, position.dot(normal));
        }

        private boolean pointIsInside(float x, float y, float z) {
            // return new Vector3f(x, y, z).sub(this.position).dot(this.normal) >= 0.0F;
            return Math.fma(  // Same as above
                x, this.normal.x,
                Math.fma(
                    y, this.normal.y,
                    z * this.normal.z
                )
            ) >= this.positionDotNormal;
        }

    }

    private final Plane[] planes;

    // Projected frustum AABB
    private final Vector3f projectedFrustumMin = new Vector3f();
    private final Vector3f projectedFrustumMax = new Vector3f();

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

        Set<IntIntPair> edgeCornerIndices = new HashSet<>();

        int[][] planesCornersIndices = new int[][] {
            new int[] {CORNER_NXNYPZ, CORNER_NXPYPZ, CORNER_NXPYNZ, CORNER_NXNYNZ},  // PLANE_NX
            new int[] {CORNER_PXNYNZ, CORNER_PXPYNZ, CORNER_PXPYPZ, CORNER_PXNYPZ},  // PLANE_PX
            new int[] {CORNER_PXNYNZ, CORNER_PXNYPZ, CORNER_NXNYPZ, CORNER_NXNYNZ},  // PLANE_NY
            new int[] {CORNER_PXPYPZ, CORNER_PXPYNZ, CORNER_NXPYNZ, CORNER_NXPYPZ},  // PLANE_PY
            new int[] {CORNER_PXNYNZ, CORNER_NXNYNZ, CORNER_NXPYNZ, CORNER_PXPYNZ},  // PLANE_NZ
            new int[] {CORNER_PXNYPZ, CORNER_PXPYPZ, CORNER_NXPYPZ, CORNER_NXNYPZ},  // PLANE_PZ
        };

        List<Plane> planes = new ArrayList<>();
        Set<Vector3f> corners = new HashSet<>();  // Btw It's possible to deduplicate them without using Set, but I'm too stupid for that

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

                var k = IntIntPair.of(aIdx, bIdx);
                var kReverse = IntIntPair.of(bIdx, aIdx);

                // If edge is shared by two planes,
                // then side plane won't be created for it
                if (!edgeCornerIndices.remove(kReverse)) {
                    edgeCornerIndices.add(k);
                    corners.add(frustumCorners[aIdx]);
                    corners.add(frustumCorners[bIdx]);
                }
            }
        }

        for (var e : edgeCornerIndices) {
            var cornerA = frustumCorners[e.firstInt()];
            var cornerB = frustumCorners[e.secondInt()];

            var normal = new Vector3f(cornerA).sub(cornerB).cross(toSunDir).normalize();
            var sidePlane = new Plane(cornerA, normal);
            planes.add(sidePlane);

            // corners.add(new Vector3f(toSunDir).mul(10000.0F).add(a));  Replaced with single corner below
            // corners.add(new Vector3f(toSunDir).mul(10000.0F).add(b));
        }

        // Close enough?
        corners.add(new Vector3f(toSunDir).mul(10000.0F));

        this.planes = planes.toArray(new Plane[]{});

        this.projectedFrustumMin.set(Float.MAX_VALUE);
        this.projectedFrustumMax.set(Float.MIN_VALUE);

        for (var c : corners) {
            this.projectedFrustumMin.min(c);
            this.projectedFrustumMax.max(c);
        }
    }

    @Override
    public boolean pointInFrustum(double x, double y, double z) {  // Used mostly by QuadParticleGroup.extractRenderState
        if (!super.pointInFrustum(x, y, z)) { return false; }
        float xf = (float) (x - this.getCamX());
        float yf = (float) (y - this.getCamY());
        float zf = (float) (z - this.getCamZ());

        for (Plane plane : this.planes) {
            if (!plane.pointIsInside(xf, yf, zf)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isVisible(@NonNull AABB aabb) {  // Used mostly by LevelRenderer.extractVisibleEntities
        if (!super.isVisible(aabb)) { return false; }
        return this.check(
            (float) (aabb.minX - this.getCamX()),
            (float) (aabb.minY - this.getCamY()),
            (float) (aabb.minZ - this.getCamZ()),
            (float) (aabb.maxX - this.getCamX()),
            (float) (aabb.maxY - this.getCamY()),
            (float) (aabb.maxZ - this.getCamZ()),
            false
        ) != FrustumIntersection.OUTSIDE;
    }

    @Override
    public int cubeInFrustum(@NonNull BoundingBox bb) {  // Used mostly by SectionOcclusionGraph.addSectionsInFrustum
        int result = super.cubeInFrustum(bb);
        if (result == FrustumIntersection.OUTSIDE) { return FrustumIntersection.OUTSIDE; }

        return this.check(
            (float) (bb.minX() - this.getCamX()),
            (float) (bb.minY() - this.getCamY()),
            (float) (bb.minZ() - this.getCamZ()),
            (float) (bb.maxX() + 1 - this.getCamX()),
            (float) (bb.maxY() + 1 - this.getCamY()),
            (float) (bb.maxZ() + 1 - this.getCamZ()),
            true  // Also check for FrustumIntersection.INSIDE
        );
    }

    final private int check(
        float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
        boolean checkIfFullyInside
    ) {
        if (!(  // Takes care of some false positives
            maxX >= projectedFrustumMin.x && minX <= projectedFrustumMax.x &&
            maxY >= projectedFrustumMin.y && minY <= projectedFrustumMax.y &&
            maxZ >= projectedFrustumMin.z && minZ <= projectedFrustumMax.z
        )) {
            return FrustumIntersection.OUTSIDE;
        }

        boolean fullyInside = true;

        for (Plane plane : this.planes) {
            // Check if farthest AABB point is still in "inner" side of plane
            if (!plane.pointIsInside(
                plane.normal.x > 0 ? maxX : minX,
                plane.normal.y > 0 ? maxY : minY,
                plane.normal.z > 0 ? maxZ : minZ
            )) {
                return FrustumIntersection.OUTSIDE;
            }

            // Now check closest point, if checkIfFullyInside is true
            if (checkIfFullyInside && !plane.pointIsInside(
                plane.normal.x > 0 ? minX : maxX,
                plane.normal.y > 0 ? minY : maxY,
                plane.normal.z > 0 ? minZ : maxZ
            )) {
                fullyInside = false;
            }
        }

        return checkIfFullyInside && fullyInside ? FrustumIntersection.INSIDE : FrustumIntersection.INTERSECT;
    }

}
