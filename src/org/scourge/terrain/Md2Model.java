package org.scourge.terrain;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.extension.model.util.KeyframeController;
import com.ardor3d.intersection.BoundingCollisionResults;
import com.ardor3d.intersection.CollisionResults;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.shape.Box;
import org.scourge.util.ShapeUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * User: gabor
 * Date: Feb 9, 2010
 * Time: 9:51:05 AM
 */
public class Md2Model implements NodeGenerator {
    private Node node;
    private Map<Md2Key, Integer[]> keyframes = new HashMap<Md2Key, Integer[]>();
//    private final Ray down = new Ray();
//    private final Ray forward = new Ray();
//    private TrianglePickResults noDistanceResults;
    private Quaternion q = new Quaternion();
    private Quaternion p = new Quaternion();
    private Vector3 direction = new Vector3();
    private CollisionResults collisionResults;
    private static final float MD2_SCALE = .2f;

    public enum Md2Key {
        crpain, death, pain, crstand, run, crdeath, jump, salute, point, stand, crattack, wave, attack, taunt, flip, crwalk, crstnd, crattak
    }


    public Md2Model(String model, String skin, String namePrefix) {
        collisionResults = new BoundingCollisionResults();

//        // point it down
//        down.getDirection().set(new Vector3(0, -1, 0));
//        noDistanceResults = new TrianglePickResults();
//        noDistanceResults.setCheckDistance(false);

        Map<String, Integer[]> frames = new HashMap<String, Integer[]>();
        node = ShapeUtil.loadMd2(model, skin, namePrefix, true, frames);
        node.setScale(MD2_SCALE);

        for(String s : frames.keySet()) {
            keyframes.put(Md2Key.valueOf(s), frames.get(s));
        }
    }

    public void moveTo(Vector3 pos) {
        node.setTranslation(new Vector3(pos.getX() * ShapeUtil.WALL_WIDTH,
                                        pos.getY() * ShapeUtil.WALL_WIDTH,
                                        pos.getZ() * ShapeUtil.WALL_WIDTH));
    }

    @Override
    public Node getNode() {
        return node;
    }

    public void moveToTopOfTerrain() {
        Terrain.moveOnTopOfTerrain(node);
    }

    private Vector3 backupLocation = new Vector3();
//    private double backupScaleY;
    public boolean canMoveTo(Vector3 proposedLocation) {
        boolean retValue = false;

        // collisions are sooo simple in jme...
        backupLocation.set(node.getTranslation());
        node.setTranslation(proposedLocation);
//        backupScaleY = node.getScale().getY();

        return true;

//        // Make the player 2 units tall. This "lifts" him off the floor so we can walk over floor irregularities, ramps, bridges, etc.
//        // Also this makes him shorter so we can fit thru the dungeon entrance. Yes this is a hack
//        node.getLocalScale().y = (2.0f / ((BoundingBox)node.getWorldBound()).yExtent) * node.getLocalScale().y;
//        node.updateGeometricState(0, false); // make geometry changes take effect now!
//
////        System.err.println("proposedLocation=" + proposedLocation + " node=" + node.getName() +
////                           " region=" + ((proposedLocation.x / ShapeUtil.WALL_WIDTH) / Region.REGION_SIZE) + "," + ((proposedLocation.z / ShapeUtil.WALL_WIDTH) / Region.REGION_SIZE) +
////                           " offset=" + ((proposedLocation.x / ShapeUtil.WALL_WIDTH) % Region.REGION_SIZE) + "," + ((proposedLocation.z / ShapeUtil.WALL_WIDTH) % Region.REGION_SIZE));
//
//        // check where the bounding box is
//        boolean collisions = checkBoundingCollisions(Main.getMain().getTerrain().getNode());
////        System.err.println("\tbound collisions found anything? " + collisions);
//
//        // check for triangles within the bounding box
//        for(int i = 0; i < collisionResults.getNumber(); i++) {
//            Geometry g = collisionResults.getCollisionData(i).getTargetMesh();
//            Node parentNode = g.getParent();
//            if(parentNode != null) {
//                collisions = hasTriangleCollision(node, parentNode);
//                if(collisions) break;
//            }
//        }
////        System.err.println("\ttri collisions ok? " + !collisions);
//
//        // check for water below
//        if(!collisions) {
//            down.getOrigin().set(getNode().getLocalTranslation());
//            down.getOrigin().addLocal(getDirection().normalizeLocal().multLocal(2.0f));
//            noDistanceResults.clear();
//            Main.getMain().getTerrain().getNode().findPick(down, noDistanceResults);
//            for(int i = 0; i < noDistanceResults.getNumber(); i++) {
//                if(noDistanceResults.getPickData(i).getTargetTris().size() > 0) {
//                    retValue = true;
//                    break;
//                }
//            }
////            System.err.println("\ton water? " + !retValue);
//        }
//
//        // reset the node's shape and position
//        node.getLocalScale().y = backupScaleY;
//        if(!retValue) {
//            node.getLocalTranslation().set(backupLocation);
//        }
//        node.updateModelBound();
//        node.updateGeometricState(0, false); // make geometry changes take effect now!
//
//        return retValue;
    }


//    public boolean hasTriangleCollision(Node n1,Node nodeWithSharedNodes) {
//		List<Spatial> geosN1 = n1.descendantMatches(TriMesh.class);
//		for (Spatial triN1 : geosN1) {
//			if (hasTriangleCollision((TriMesh)triN1, nodeWithSharedNodes))
//				return true;
//		}
//		return false;
//	}
//
//	public boolean hasTriangleCollision(TriMesh sp,Node nodeWithSharedNodes) {
//		List<Spatial> geosN2 = nodeWithSharedNodes.descendantMatches(TriMesh.class);
//
//		for (Spatial triN2 : geosN2) {
//            if (((TriMesh)triN2).hasTriangleCollision(sp)) {
////                    System.err.println("collision: " + triN2.getName());
//                return true;
//            }
//		}
//		return false;
//	}
//
//    public boolean checkBoundingCollisions(Node world) {
//        collisionResults.clear();
//        node.findCollisions(world, collisionResults);
//        return collisionResults.getNumber() > 0;
//    }
//
    public Vector3 getDirection() {
        q.fromRotationMatrix(node.getRotation());
        q.multiplyLocal(p.fromAngleAxis(MathUtils.DEG_TO_RAD * 90.0f, Vector3.UNIT_Y));
        q.getRotationColumn( 2, direction );
        return direction;
    }
//
    public void setKeyFrame(Md2Key key) {
        setKeyFrame(key, 10);
    }

    public void setKeyFrame(Md2Key key, float speed) {
        Mesh mesh = (Mesh)node.getChild(0);
        KeyframeController<Mesh> c = (KeyframeController<Mesh>)mesh.getController(0);

        c.setSpeed(speed);
        c.setActive(true);
//        c.setRepeatType(SpatialController.RT_WRAP);
        Integer[] times = keyframes.get(key);
        if(times != null) {
            c.setMinTime(times[0]);
            c.setMaxTime(times[1] - 1);
        }
    }

    public int getX() {
        return (int)Math.round(getNode().getTranslation().getX() / ShapeUtil.WALL_WIDTH);
    }
        
    public int getZ() {
        return (int)Math.round(getNode().getTranslation().getZ() / ShapeUtil.WALL_WIDTH);
    }
}
