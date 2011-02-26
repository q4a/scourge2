package org.scourge.terrain;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.image.Texture;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;
import org.scourge.Main;
import org.scourge.util.ShapeUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;

/**
 * User: gabor
 * Date: Feb 7, 2010
 * Time: 5:44:11 PM
 */
public class House implements NodeGenerator {
    protected static final float FINAL_TRANS = 3;
    protected static final float FINAL_SCALE = (ShapeUtil.WALL_WIDTH - FINAL_TRANS) / ShapeUtil.WALL_WIDTH;
    protected static final float ROOF_OVERHANG = 0.2f;
    private Node house;
    private Main main;
    private Random random;

    public House(Main main, double x, double y, double z, double w, double h, double levels, Random random) {
        this.main = main;
        this.random = random;
        house = new Node(ShapeUtil.newShapeName("house_"));
        for(int i = 0; i < levels; i++) {
            drawLevel(house, 0, i, 0, w, h, i == 0);
        }
        drawRoof(house, 0, 0 + levels, 0, w, h);
		Vector3 v = new Vector3(x * ShapeUtil.WALL_WIDTH, y * ShapeUtil.WALL_HEIGHT + Region.MIN_HEIGHT, z * ShapeUtil.WALL_WIDTH);
        house.setTranslation(v);
        Quaternion q = new Quaternion();
        q.fromAngleAxis(MathUtils.DEG_TO_RAD * (25.0f * random.nextFloat()), Vector3.UNIT_Y);
		Quaternion p = new Quaternion().fromRotationMatrix(house.getRotation());
		p.multiplyLocal(q);
        house.setRotation(p);
    }

    public Node getNode() {
        return house;
    }

    private void drawRoof(Node house, double x, double y, double z, double w, double h) {
        Node roofNode = new Node(ShapeUtil.newShapeName("roof_"));

		FloatBuffer tex = BufferUtils.createVector2Buffer(4);
		tex.put(0).put(0);
		tex.put((float)h / 2).put(0);
		tex.put((float)h / 2).put((float)w/2);
		tex.put(0).put((float)w/2);

        Texture texture = TextureManager.load("./data/textures/roof-red.png",
				Texture.MinificationFilter.Trilinear,
				true);
		texture.setMagnificationFilter(Texture.MagnificationFilter.Bilinear);
        texture.setWrap(Texture.WrapMode.Repeat);
        TextureState ts = new TextureState();
        ts.setTexture(texture);

        Texture texture2 = texture.createSimpleClone();
		Quaternion q = new Quaternion().fromAngleAxis(MathUtils.DEG_TO_RAD * 90.0f, Vector3.UNIT_Z);
		texture2.setTextureMatrix(new Matrix4().set(q));
        TextureState ts2 = new TextureState();
        ts2.setTexture(texture2);

        boolean orientation = h > w;
        Quad roof = new Quad(ShapeUtil.newShapeName("roof_side_"));
        FloatBuffer fb = BufferUtils.createFloatBuffer(12);
        if(orientation) {
            fb.put(new float[] {
                    (float)(x - w / 2 - ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH, (float)y * ShapeUtil.WALL_WIDTH, (float)(z - h / 2 - 1 - ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH,
                    (float)(x - w / 2 - ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH, (float)y * ShapeUtil.WALL_WIDTH, (float)(z + h / 2 - 1 + ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH,
                    (float)(x) * ShapeUtil.WALL_WIDTH, (float)(y + 1) * ShapeUtil.WALL_WIDTH, (float)(z + h / 2 - 1 + ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH,
                    (float)(x) * ShapeUtil.WALL_WIDTH, (float)(y + 1) * ShapeUtil.WALL_WIDTH, (float)(z - h / 2 - 1 - ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH,
            });
        } else {
            fb.put(new float[] {
                    (float)(x - w / 2 - ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH, (float)y * ShapeUtil.WALL_WIDTH, (float)(z - h / 2 - 1 - ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH,
                    (float)(x - w / 2 - ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH, (float)(y + 1) * ShapeUtil.WALL_WIDTH, (float)(z - 1) * ShapeUtil.WALL_WIDTH,
                    (float)(x + w / 2 + ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH, (float)(y + 1) * ShapeUtil.WALL_WIDTH, (float)(z - 1) * ShapeUtil.WALL_WIDTH,
                    (float)(x + w / 2 + ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH, (float)y * ShapeUtil.WALL_WIDTH, (float)(z - h / 2 - 1 - ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH,
            });
        }
        roof.getMeshData().setVertexBuffer(fb);
        roof.getMeshData().setTextureCoords(new FloatBufferData(tex, 2), 0);
        roof.getMeshData().rotateNormals(new Quaternion().fromAngleAxis(MathUtils.DEG_TO_RAD * 180, Vector3.UNIT_X));
        roof.setRenderState(orientation ? ts : ts2);
        roof.setModelBound(new BoundingBox());
        roofNode.attachChild(roof);

        roof = new Quad(ShapeUtil.newShapeName("roof_side_"));
        fb = BufferUtils.createFloatBuffer(12);
        if(orientation) {
            fb.put(new float[] {
                    (float)(x) * ShapeUtil.WALL_WIDTH, (float)(y + 1) * ShapeUtil.WALL_WIDTH, (float)(z - h / 2 - 1 - ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH,
                    (float)(x) * ShapeUtil.WALL_WIDTH, (float)(y + 1) * ShapeUtil.WALL_WIDTH, (float)(z + h / 2 - 1 + ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH,
                    (float)(x + w / 2 + ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH, (float)y * ShapeUtil.WALL_WIDTH, (float)(z + h / 2 - 1 + ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH,
                    (float)(x + w / 2 + ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH, (float)y * ShapeUtil.WALL_WIDTH, (float)(z - h / 2 - 1 - ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH,
            });
        } else {
            fb.put(new float[] {
                    (float)(x - w / 2 - ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH, (float)(y + 1) * ShapeUtil.WALL_WIDTH, (float)(z - 1) * ShapeUtil.WALL_WIDTH,
                    (float)(x - w / 2 - ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH, (float)y * ShapeUtil.WALL_WIDTH, (float)(z + h / 2 - 1 + ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH,
                    (float)(x + w / 2 + ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH, (float)y * ShapeUtil.WALL_WIDTH, (float)(z + h / 2 - 1 + ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH,
                    (float)(x + w / 2 + ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH, (float)(y + 1) * ShapeUtil.WALL_WIDTH, (float)(z - 1) * ShapeUtil.WALL_WIDTH,
            });
        }
        roof.getMeshData().setVertexBuffer(fb);
        roof.getMeshData().setTextureCoords(new FloatBufferData(tex, 2), 0);
        //roof.rotateNormals(new Quaternion().fromAngleAxis(MathUtils.DEG_TO_RAD * 180, Vector3.UNIT_X));
        roof.setRenderState(orientation ? ts : ts2);
        roof.setModelBound(new BoundingBox());
        roofNode.attachChild(roof);

        texture = TextureManager.load("./data/textures/floor2.png",
				Texture.MinificationFilter.Trilinear, true);
		texture.setMagnificationFilter(Texture.MagnificationFilter.Bilinear);
        texture.setWrap(Texture.WrapMode.Repeat);
        ts = new TextureState();
        ts.setTexture(texture);
        if(orientation) {
            fb = BufferUtils.createFloatBuffer(9);
            fb.put(new float[] {
                    (float)(x - w / 2 - ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH, (float)y * ShapeUtil.WALL_WIDTH, (float)(z - h / 2 - 1) * ShapeUtil.WALL_WIDTH,
                    (float)(x) * ShapeUtil.WALL_WIDTH, (float)(y + 1) * ShapeUtil.WALL_WIDTH, (float)(z - h / 2 - 1) * ShapeUtil.WALL_WIDTH,
                    (float)(x + w / 2 + ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH, (float)y * ShapeUtil.WALL_WIDTH, (float)(z - h / 2 - 1) * ShapeUtil.WALL_WIDTH,
            });

			tex = BufferUtils.createVector2Buffer(3);
			tex.put(0).put(0);
			tex.put((float)h / 2).put(0);
			tex.put(0).put((float)w/2);

            IntBuffer ib = BufferUtils.createIntBuffer(3);
            ib.put(new int[] { 0, 1, 2 });
            FloatBuffer nb = fb.duplicate();
            Mesh tri = new Mesh(ShapeUtil.newShapeName("roof_tri_"));
			tri.getMeshData().setVertexBuffer(fb);
			tri.getMeshData().setNormalBuffer(nb);
			tri.getMeshData().setIndexBuffer(ib);
			tri.getMeshData().setTextureCoords(new FloatBufferData(tex, 2), 0);
            tri.setRenderState(ts);
            tri.setModelBound(new BoundingBox());
            roofNode.attachChild(tri);

            fb = BufferUtils.createFloatBuffer(9);
            fb.put(new float[] {
                    (float)(x - w / 2 - ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH, (float)y * ShapeUtil.WALL_WIDTH, (float)(z + h / 2 - 1) * ShapeUtil.WALL_WIDTH,
                    (float)(x) * ShapeUtil.WALL_WIDTH, (float)(y + 1) * ShapeUtil.WALL_WIDTH, (float)(z + h / 2 - 1) * ShapeUtil.WALL_WIDTH,
                    (float)(x + w / 2 + ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH, (float)y * ShapeUtil.WALL_WIDTH, (float)(z + h / 2 - 1) * ShapeUtil.WALL_WIDTH,
            });
            ib = BufferUtils.createIntBuffer(3);
            ib.put(new int[] { 2, 1, 0 });
            nb = fb.duplicate();
			tri = new Mesh(ShapeUtil.newShapeName("roof_tri_"));
			tri.getMeshData().setVertexBuffer(fb);
			tri.getMeshData().setNormalBuffer(nb);
			tri.getMeshData().setIndexBuffer(ib);
			tri.getMeshData().setTextureCoords(new FloatBufferData(tex, 2), 0);
            tri.setRenderState(ts);
            tri.setModelBound(new BoundingBox());
            roofNode.attachChild(tri);
        } else {
            fb = BufferUtils.createFloatBuffer(9);
            fb.put(new float[] {
                    (float)(x - w / 2) * ShapeUtil.WALL_WIDTH, (float)y * ShapeUtil.WALL_WIDTH, (float)(z - h / 2 - 1 - ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH,
                    (float)(x - w / 2) * ShapeUtil.WALL_WIDTH, (float)(y + 1) * ShapeUtil.WALL_WIDTH, (float)(z / 2 - 1) * ShapeUtil.WALL_WIDTH,
                    (float)(x - w / 2) * ShapeUtil.WALL_WIDTH, (float)y * ShapeUtil.WALL_WIDTH, (float)(z + h / 2 - 1 + ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH,
            });
			tex = BufferUtils.createVector2Buffer(3);
			tex.put(0).put(0);
			tex.put((float)w / 2).put((float)h/2);
			tex.put(0).put((float)h/2);
            IntBuffer ib = BufferUtils.createIntBuffer(3);
            ib.put(new int[] { 2, 1, 0 });
            FloatBuffer nb = fb.duplicate();
            Mesh tri = new Mesh(ShapeUtil.newShapeName("roof_tri_"));
			tri.getMeshData().setVertexBuffer(fb);
			tri.getMeshData().setNormalBuffer(nb);
			tri.getMeshData().setIndexBuffer(ib);
			tri.getMeshData().setTextureCoords(new FloatBufferData(tex, 2), 0);
            tri.setRenderState(ts);
            tri.setModelBound(new BoundingBox());
            roofNode.attachChild(tri);

            fb = BufferUtils.createFloatBuffer(9);
            fb.put(new float[] {
                    (float)(x + w / 2) * ShapeUtil.WALL_WIDTH, (float)y * ShapeUtil.WALL_WIDTH, (float)(z - h / 2 - 1 - ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH,
                    (float)(x + w / 2) * ShapeUtil.WALL_WIDTH, (float)(y + 1) * ShapeUtil.WALL_WIDTH, (float)(z / 2 - 1) * ShapeUtil.WALL_WIDTH,
                    (float)(x + w / 2) * ShapeUtil.WALL_WIDTH, (float)y * ShapeUtil.WALL_WIDTH, (float)(z + h / 2 - 1 + ROOF_OVERHANG) * ShapeUtil.WALL_WIDTH,
            });
            ib = BufferUtils.createIntBuffer(3);
            ib.put(new int[] { 0, 1, 2 });
            nb = fb.duplicate();
            tri = new Mesh(ShapeUtil.newShapeName("roof_tri_"));
			tri.getMeshData().setVertexBuffer(fb);
			tri.getMeshData().setNormalBuffer(nb);
			tri.getMeshData().setIndexBuffer(ib);
			tri.getMeshData().setTextureCoords(new FloatBufferData(tex, 2), 0);
            tri.setRenderState(ts);
            tri.setModelBound(new BoundingBox());
            roofNode.attachChild(tri);
        }

        house.attachChild(roofNode);
    }

    private void drawLevel(Node house, double x, double y, double z, double w, double h, boolean has_door) {
        Direction door = Direction.values()[(int)((float)Direction.values().length * random.nextFloat())];
        drawWall(house, x - (w / 2),     y, z - (h / 2 + 1), Direction.EAST, w, w - 1, door == Direction.EAST && has_door);
        drawWall(house, x - (w / 2 - 1), y, z + (h / 2 - 1), Direction.WEST, w, 0, door == Direction.WEST && has_door);
        drawWall(house, x - (w / 2),     y, z - (h / 2), Direction.SOUTH, h, 0, door == Direction.SOUTH && has_door);
        drawWall(house, x + (w / 2),     y, z - (h / 2 + 1), Direction.NORTH, h, h - 1, door == Direction.NORTH && has_door);
        drawFloor(house, x, y, z, w, h);
    }


    private void drawFloor(Node house, double x, double y, double z, double w, double h) {
        Quad floor = new Quad(ShapeUtil.newShapeName("floor_"), w * ShapeUtil.WALL_WIDTH, h * ShapeUtil.WALL_WIDTH);
		Vector3 v = new Vector3(x * ShapeUtil.WALL_WIDTH,
				y * ShapeUtil.WALL_WIDTH + 0.25,
				(z - 1) * ShapeUtil.WALL_WIDTH);
        floor.setTranslation(v);

		Quaternion q = new Quaternion();
        q.fromAngleAxis(MathUtils.DEG_TO_RAD * 90, Vector3.UNIT_X);
		floor.setRotation(q);

		final FloatBuffer tex = BufferUtils.createVector2Buffer(4);
		floor.getMeshData().setTextureCoords(new FloatBufferData(tex, 2), 0);
		tex.put(0).put(0);
		tex.put((float)h / 2).put(0);
		tex.put((float)h / 2).put((float)w/2);
		tex.put(0).put((float)w/2);

        Texture texture = TextureManager.load("./data/textures/floor.png",
				Texture.MinificationFilter.Trilinear, true);
		texture.setMagnificationFilter(Texture.MagnificationFilter.Bilinear);
        texture.setWrap(Texture.WrapMode.Repeat);
        TextureState ts = new TextureState();
        ts.setTexture(texture);
        floor.setRenderState(ts);
        floor.setModelBound(new BoundingBox());
        house.attachChild(floor);

    }


    private void drawWall(Node house, double x, double y, double z, Direction dir, double length, double finalPos, boolean has_door) {
        int door_pos = has_door ? 1 + (int)((float)(length - 2) * random.nextFloat()) : -1;
        for(int i = 0; i < length; i++) {
            if(i == door_pos) {
                addWallPiece(getDoorFrame(), i, house, x, y, z, dir, length, finalPos);
                addWallPiece(getDoor(), i, house, x, y, z, dir, length, finalPos);
            } else {
                addWallPiece(0 == (int)(8.0f * random.nextFloat()) ? getWindow() : getWall(), i, house, x, y, z, dir, length, finalPos);
            }
        }
    }


    protected void addWallPiece(Spatial spatial, int i, Node house, double x, double y, double z, Direction dir, double length, double finalPos) {
        Quaternion q = new Quaternion();
        q.fromAngleAxis(MathUtils.DEG_TO_RAD * dir.getAngle(), Vector3.UNIT_Y);
		Quaternion p = new Quaternion().fromRotationMatrix(spatial.getRotation());
		p.multiplyLocal(q);
        spatial.setRotation(p);
            switch(dir) {
                case EAST:
                case WEST:
					spatial.setTranslation((x + i) * ShapeUtil.WALL_WIDTH, y * ShapeUtil.WALL_WIDTH, z * ShapeUtil.WALL_WIDTH);
                    break;
                case NORTH:
                case SOUTH:
					spatial.setTranslation(x * ShapeUtil.WALL_WIDTH, y * ShapeUtil.WALL_WIDTH, (z + i) * ShapeUtil.WALL_WIDTH);
                    break;
            }

            if(i == finalPos) {
                spatial.setScale(FINAL_SCALE, 1, 1);
            }
            house.attachChild(spatial);
    }


    protected Spatial getWall() {
        return ShapeUtil.importModel("./data/3ds/wall.3ds", "./data/textures", "wall", null, 0, 0, 0);
    }

    protected Spatial getWindow() {
        return ShapeUtil.importModel("./data/3ds/win.3ds", "./data/textures", "wall", null, 0, 0, 0);
    }

    protected Spatial getDoorFrame() {
        return ShapeUtil.importModel("./data/3ds/dframe.3ds", "./data/textures", "wall", null, 0, 0, 0);
    }

    protected Spatial getDoor() {
        return ShapeUtil.importModel("./data/3ds/door.3ds", "./data/textures", "wall", null, 0, 0, 0);
    }
}
