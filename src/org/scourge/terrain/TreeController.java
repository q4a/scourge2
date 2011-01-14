package org.scourge.terrain;

import com.ardor3d.math.FastMath;
import com.ardor3d.math.MathUtils;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import org.scourge.util.ShapeUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * User: gabor
 * Date: 1/13/11
 * Time: 1:58 PM
 */
public class TreeController implements SpatialController {
    private float deltaX, deltaZ, stepX, stepZ;
    private boolean dir;
    private List<Mesh> meshes;
    private Float[] bounds;
    private float MAX_DELTA = 0.05f;
    private float STEP = .0025f;
    private float XMUL, ZMUL;

    public TreeController() {
        STEP = .05f + (float)(Math.random() * .02f);
        MAX_DELTA = .1f;
        XMUL = (float)(Math.random() * .5f) + .5f;
        ZMUL = (float)(Math.random() * .5f) + .5f;
    }

    @Override
    public void update(double time, Spatial caller) {
        if(meshes == null) {
            meshes = ShapeUtil.findMeshes(caller, new ArrayList<Mesh>());
            bounds = findBounds(meshes);
        }

        float d = (dir ? 1f : -1f) * STEP * (float)time;
        stepX = d * XMUL;
        stepZ = d * ZMUL;
        deltaX += stepX;
        deltaZ += stepZ;
        if(Math.abs(deltaX) >= MAX_DELTA || Math.abs(deltaZ) >= MAX_DELTA) {
            deltaX -= stepX;
            deltaZ -= stepZ;
            dir = !dir;
            // don't apply this change to the vertices
            return;
        }

        for(Mesh mesh : meshes) {
            shear(mesh);
        }
    }

    private void shear(Mesh mesh) {
        FloatBuffer vertices = mesh.getMeshData().getVertexBuffer();
        float[] point = new float[3];

        if(bounds != null) {
            vertices.rewind();
            while(vertices.position() < vertices.capacity()) {
                vertices.mark();
                vertices.get(point);
                vertices.reset();
                shear(point, bounds[0], bounds[1]);
                vertices.put(point);
            }
        }
    }

    private void shear(float[] point, float minY, float maxY) {
        float dy = (point[1] - minY) / (maxY - minY);
        point[0] += dy * stepX;
        point[2] += dy * stepZ;
    }

    private Float[] findBounds(List<Mesh> meshes) {
        Float minY = null;
        Float maxY = null;

        for(Mesh mesh : meshes) {
            FloatBuffer vertices = mesh.getMeshData().getVertexBuffer();
            float[] point = new float[3];
            vertices.rewind();
            while(vertices.position() < vertices.capacity()) {
                vertices.get(point);
                if(minY == null) {
                    minY = maxY = point[1];
                } else if(point[1] < minY) {
                    minY = point[1];
                } else if(point[1] > maxY) {
                    maxY = point[1];
                }
            }
        }

        return new Float[] { minY, maxY };
    }
}
