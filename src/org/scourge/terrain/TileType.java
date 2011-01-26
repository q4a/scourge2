package org.scourge.terrain;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.image.Texture;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.PickingHint;
import com.ardor3d.scenegraph.shape.Quad;
import org.scourge.Climate;
import org.scourge.util.ShapeUtil;

import java.nio.FloatBuffer;

/**
* User: gabor
* Date: Mar 13, 2010
* Time: 8:55:49 AM
*/
enum TileType {
    NONE {
        @Override
        public Node createNode(float angle, float[] heights, int level, Climate climate, boolean nextToWater) {
            return new Node("empty");
        }

        @Override
        public boolean isTexturePreset() {
            return false;
        }

        @Override
        public void updateHeights(Node node, float[] heights) {
        }
    },
    EDGE_BRIDGE {
        private Model model = Model.edge_bridge;

        @Override
        public Node createNode(float angle, float[] heights, int level, Climate climate, boolean nextToWater) {
            return addEdge(angle, model, level, climate, nextToWater);
        }

        @Override
        public boolean isTexturePreset() {
            return true;
        }

        @Override
        public void updateHeights(Node node, float[] heights) {
        }
    },
    EDGE_CORNER {
        private Model model = Model.edge_corner;

        @Override
        public Node createNode(float angle, float[] heights, int level, Climate climate, boolean nextToWater) {
            return addEdge(angle, model, level, climate, nextToWater);
        }

        @Override
        public boolean isTexturePreset() {
            return true;
        }

        @Override
        public void updateHeights(Node node, float[] heights) {
        }
    },
    EDGE_TIP {
        private Model model = Model.edge_tip;

        @Override
        public Node createNode(float angle, float[] heights, int level, Climate climate, boolean nextToWater) {
            return addEdge(angle, model, level, climate, nextToWater);
        }

        @Override
        public boolean isTexturePreset() {
            return true;
        }

        @Override
        public void updateHeights(Node node, float[] heights) {
        }
    },
    EDGE_SIDE {
        private Model model = Model.edge_side;

        @Override
        public Node createNode(float angle, float[] heights, int level, Climate climate, boolean nextToWater) {
            return addEdge(angle, model, level, climate, nextToWater);
        }

        @Override
        public boolean isTexturePreset() {
            return true;
        }

        @Override
        public void updateHeights(Node node, float[] heights) {
        }
    },
    EDGE_GATE {
        private Model model = Model.edge_gate;

        @Override
        public Node createNode(float angle, float[] heights, int level, Climate climate, boolean nextToWater) {
            return addEdge(angle, model, level, climate, nextToWater);
        }

        @Override
        public boolean isTexturePreset() {
            return true;
        }

        @Override
        public void updateHeights(Node node, float[] heights) {
        }
    },
    EDGE_UP {
        private Model model = Model.edge_up;

        @Override
        public Node createNode(float angle, float[] heights, int level, Climate climate, boolean nextToWater) {
            return addEdge(angle, model, level, climate, nextToWater);
        }

        @Override
        public boolean isTexturePreset() {
            return true;
        }

        @Override
        public void updateHeights(Node node, float[] heights) {
        }
    },
    EDGE_DOWN {
        private Model model = Model.edge_down;

        @Override
        public Node createNode(float angle, float[] heights, int level, Climate climate, boolean nextToWater) {
            return addEdge(angle, model, level, climate, nextToWater);
        }

        @Override
        public boolean isTexturePreset() {
            return true;
        }

        @Override
        public void updateHeights(Node node, float[] heights) {
        }
    },
    QUAD {
        @Override
        public Node createNode(float angle, float[] heights, int level, Climate climate, boolean nextToWater) {
            Quad ground = createQuad(heights);
            Node groundNode = new Node(ShapeUtil.newShapeName("ground_node"));
            groundNode.attachChild(ground);
            return groundNode;
        }

        @Override
        public boolean isTexturePreset() {
            return false;
        }

        @Override
        public void updateHeights(Node node, float[] heights) {
            FloatBuffer vertexBuf = ((Quad) node.getChild(0)).getMeshData().getVertexBuffer();
            vertexBuf.put(1, heights[0]);
            vertexBuf.put(4, heights[1]);
            vertexBuf.put(7, heights[2]);
            vertexBuf.put(10, heights[3]);
//            node.updateModelBound();
//            node.updateWorldBound();
        }
    },
    ;

    public abstract boolean isTexturePreset();
    public abstract Node createNode(float angle, float[] heights, int level, Climate climate, boolean nextToWater);
    public abstract void updateHeights(Node node, float[] heights);

    protected Node addEdge(float angle, Model model, int level, Climate climate, boolean nextToWater) {
        // create a unique cache key for the model
        model.setNamePrefix(model.name() + "." + climate.name() + "." + level);
        
        Spatial edge = model.createSpatial();
        Matrix3 q = new Quaternion().fromAngleAxis(MathUtils.DEG_TO_RAD * angle, Vector3.UNIT_Y).toRotationMatrix((Matrix3)null);
        edge.setRotation(edge.getRotation().multiply(q, null));

        if(climate.isDungeon()) {
            model.assignDungeonTextures(edge, level == 0 ? climate.getBaseTileTex().getTexturePath() : null, climate.getWallTexture());
        }

        Node edgeNode = new Node(ShapeUtil.newShapeName("edge_node"));
        edgeNode.attachChild(edge);

        if(level > 0 && !nextToWater) {
            Quad ground = createQuad(new float[] { 0, 0, 0, 0 });
            //ground.getLocalTranslation().y -= ShapeUtil.WALL_HEIGHT;
            ground.setTranslation(ground.getTranslation().getX(), ground.getTranslation().getY() - ShapeUtil.WALL_HEIGHT, ground.getTranslation().getZ());
            TextureState ts = new TextureState();
            Texture texture = ShapeUtil.loadTexture(climate.getBaseTileTex().getTexturePath());
            texture.setWrap(Texture.WrapMode.Repeat);
            ts.setTexture(texture, 0);
            ground.setRenderState(ts);
            edgeNode.attachChild(ground);
        }

//        edgeNode.setModelBound(new BoundingBox());
//        edgeNode.updateModelBound();
//        edgeNode.updateWorldBound();
        return edgeNode;
    }

    protected Quad createQuad(float[] heights) {
        Quad ground = new Quad(ShapeUtil.newShapeName("ground"), ShapeUtil.WALL_WIDTH, ShapeUtil.WALL_WIDTH);

        Vector3 a = new Vector3(-ShapeUtil.WALL_WIDTH / 2, heights[0], -ShapeUtil.WALL_WIDTH / 2);
        Vector3 b = new Vector3(-ShapeUtil.WALL_WIDTH / 2, heights[1], ShapeUtil.WALL_WIDTH / 2);
        Vector3 c = new Vector3(ShapeUtil.WALL_WIDTH / 2, heights[2], ShapeUtil.WALL_WIDTH / 2);
        Vector3 d = new Vector3(ShapeUtil.WALL_WIDTH / 2, heights[3], -ShapeUtil.WALL_WIDTH / 2);

        FloatBuffer vertexBuf = ground.getMeshData().getVertexBuffer();
        vertexBuf.clear();
        vertexBuf.put((float)a.getX()).put((float)a.getY()).put((float)a.getZ());
        vertexBuf.put((float)b.getX()).put((float)b.getY()).put((float)b.getZ());
        vertexBuf.put((float)c.getX()).put((float)c.getY()).put((float)c.getZ());
        vertexBuf.put((float)d.getX()).put((float)d.getY()).put((float)d.getZ());

        Vector3 e1 = b.subtract(a, null);
        Vector3 e2 = c.subtract(a, null);
        Vector3 normal = e1.cross(e2, null).normalizeLocal();

        FloatBuffer normBuf = ground.getMeshData().getNormalBuffer();
        normBuf.clear();
        normBuf.put((float)normal.getX()).put((float)normal.getY()).put((float)normal.getZ());
        normBuf.put(0).put(1).put(0);
        normBuf.put(0).put(1).put(0);
        normBuf.put(0).put(1).put(0);

        ground.setModelBound(new BoundingBox());
        ground.updateModelBound();

        // w/o this magic line splat textures (PassNode) won't work.
        ground.getMeshData().copyTextureCoordinates(0, 1, 1.0f);
        ground.getMeshData().copyTextureCoordinates(1, 2, 1.0f);
        ground.getMeshData().copyTextureCoordinates(2, 3, 1.0f);

        ground.getSceneHints().setPickingHint(PickingHint.Collidable, false);

        return ground;
    }
}
