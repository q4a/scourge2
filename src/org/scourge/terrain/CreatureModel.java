package org.scourge.terrain;

import com.ardor3d.bounding.CollisionTree;
import com.ardor3d.bounding.CollisionTreeManager;
import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.blendtree.ClipSource;
import com.ardor3d.extension.animation.skeletal.blendtree.SimpleAnimationApplier;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.JointChannel;
import com.ardor3d.extension.animation.skeletal.state.SteadyState;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
import com.ardor3d.intersection.*;
import com.ardor3d.math.*;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.hint.PickingHint;
import com.ardor3d.util.geom.BufferUtils;
import org.scourge.Main;
import org.scourge.config.CreatureModelTemplate;
import org.scourge.util.ShapeUtil;

import java.util.List;

/**
 * User: gabor
 * Date: Feb 9, 2010
 * Time: 9:51:05 AM
 */
public class CreatureModel implements NodeGenerator {
    private Node node;
    private final Ray3 down = new Ray3();
    private final Ray3 forward = new Ray3();
    private PrimitivePickResults  noDistanceResults = new PrimitivePickResults();
    private PrimitivePickResults  distanceResults = new PrimitivePickResults();
    private Quaternion q = new Quaternion();
    private Quaternion p = new Quaternion();
    private Vector3 direction = new Vector3();
    private AnimationManager manager;
    private CreatureModelTemplate model;
    private static final double MIN_DISTANCE = 4;
    private PrimitiveCollisionResults results;
    private double speed, lastDistance;
    private double[] COLLISION_ANGLES = { -45, -10, 0, 10, 45 };
    private double[] POST_COLLISION_ANGLES = { -90, 90 };
    private static boolean DEBUG_ENABLED = false;
    private Node debugNode = new Node("debug");
    private Vector3[] lineVertex = new Vector3[2 * COLLISION_ANGLES.length];
    private final Line lines = new Line("normLine", lineVertex, null, null, null);
    private int lastStep;

    public CreatureModel(CreatureModelTemplate model, String skin, String namePrefix) {

        CollisionTreeManager.getInstance().setTreeType(CollisionTree.Type.AABB);
        CollisionTreeManager.getInstance().setDoSort(true);

        results = new PrimitiveCollisionResults();

        this.model = model;
        // point it down
        down.setDirection(new Vector3(0, -1, 0));
        noDistanceResults.setCheckDistance(false);

        distanceResults.setCheckDistance(true);

        final ColladaStorage storage = new ColladaImporter().load(model.getModel());
        //Node colladaNode = storage.getScene();
        final List<SkinData> skinDatas = storage.getSkins();
        node = skinDatas.get(0).getSkinBaseNode();
        model.transform(node);
//        node = new Node() {
//            @Override
//            public void updateWorldBound(boolean recurse) {
//                super.updateWorldBound(recurse);
//
//                // hack the world bound to exclude some of the creatures legs
//                BoundingBox bb = (BoundingBox)getWorldBound();
//                bb.setYExtent(bb.getYExtent() - ABOVE_GROUND);
//                bb.setCenter(bb.getCenter().getX(), bb.getCenter().getY() + ABOVE_GROUND / 2, bb.getCenter().getZ());
//                _worldBound = bb;
//                clearDirty(DirtyType.Bounding);
//            }
//        };
//        node.attachChild(skinNode);
        node.updateWorldBound(true);

        // Make our manager
        manager = new AnimationManager(Main.getMain().getTimer(), skinDatas.get(0).getPose());

        final AnimationClip clipA = new AnimationClip("clipA");
        for (final JointChannel channel : storage.getJointChannels()) {
            // add it to a clip
            clipA.addChannel(channel);
        }

        // Set some clip instance specific data - repeat, time scaling
        manager.getClipInstance(clipA).setLoopCount(Integer.MAX_VALUE);

        // Add our "applier logic".
        manager.setApplier(new SimpleAnimationApplier());

        // Add our clip as a state in the default animation layer
        final SteadyState animState = new SteadyState("anim_state");
        animState.setSourceTree(new ClipSource(clipA, manager));
        manager.getBaseAnimationLayer().addSteadyState(animState);

        // Set the current animation state on default layer
        manager.getBaseAnimationLayer().setCurrentState("anim_state", true);

//        lines.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
        lines.setRenderState(new ZBufferState());
        lines.setLineWidth(3.0f);
        lines.getMeshData().setIndexMode(IndexMode.Lines);
//        lines.getMeshData().setVertexBuffer(BufferUtils.createVector3Buffer(500));
//        lines.getMeshData().setColorBuffer(BufferUtils.createColorBuffer(500));
//        lines.updateWorldRenderStates(false);

        for(int i = 0; i < lineVertex.length; i++) {
            lineVertex[i] = new Vector3(0, 0, 0);
        }
        debugNode.attachChild(lines);

        node.addController(new SpatialController() {
            @Override
            public void update(double v, Spatial spatial) {
                manager.update();
            }
        });
    }

    public void setAnimation(Animations animation) {
        String ss_name = model.getAnimationName(animation);
        if(ss_name != null) {
            manager.getBaseAnimationLayer().setCurrentState(ss_name, true);
        }
    }

    public void moveTo(Vector3 pos) {
        node.setTranslation(new Vector3(pos.getX() * ShapeUtil.WALL_WIDTH,
                                        pos.getY() * ShapeUtil.WALL_WIDTH,
                                        pos.getZ() * ShapeUtil.WALL_WIDTH));
		node.updateWorldTransform(true);
		node.updateWorldBound(true);
    }

    @Override
    public Node getNode() {
        return node;
    }

    public final static float PLAYER_SPEED = 50.0f;

    private Vector3 tempVa = new Vector3();
    private Vector3 proposedLocation = new Vector3();

    public void moveToTopOfTerrain(double tpf) {
        Terrain.moveOnTopOfTerrain(node);

        // move forward
        Main main = Main.getMain();
        if(main.getPlayerControl().isMoving()) {
            speed = PLAYER_SPEED * tpf;
            proposedLocation.set(node.getTranslation());
            // don't move more than the last sensed distance (ie. don't move into the wall)
            proposedLocation.addLocal(getDirection().multiply(lastDistance > 0 && lastDistance < speed ? lastDistance : speed, tempVa));
            if(canMoveTo(proposedLocation)) {
                main.getTerrain().loadRegion();
                main.checkRoof();
            }
        }

        if(DEBUG_ENABLED) {
            // show debug
            debugNode.setTranslation(node.getTranslation());
            for(int i = 0; i < lineVertex.length / 2; i++) {
                lineVertex[i * 2].set(0, 0, 0);
                lineVertex[i * 2].setY(lineVertex[i * 2].getY() + 8);
                lineVertex[i * 2 + 1].set(getDirection(COLLISION_ANGLES[i]).multiplyLocal(10));
                lineVertex[i * 2 + 1].setY(lineVertex[i * 2 + 1].getY() + 8);
            }
            lines.getMeshData().setVertexBuffer(BufferUtils.createFloatBuffer(lineVertex));
            debugNode.updateGeometricState(0);
            debugNode.updateWorldTransform(true);
            debugNode.updateWorldBound(true);
        }
    }

    private boolean executePick(double angle, Vector3 proposedLocation, double distance) {
        tmpLocation.set(proposedLocation);
        tmpLocation.setY(proposedLocation.getY() + 8);
        forward.setOrigin(tmpLocation);
        forward.setDirection(getDirection(angle));
        distanceResults.clear();
        PickingUtil.findPick(Main.getMain().getTerrain().getNode(), forward, distanceResults);

        return (distanceResults.getNumber() != 0 &&
                distanceResults.getPickData(0).getTarget() != null &&
                distanceResults.getPickData(0).getIntersectionRecord() != null);
    }

    private Vector3 backupLocation = new Vector3();
    private Vector3 tmpLocation = new Vector3();
    private Vector3 tmpLocation2 = new Vector3();
    private Vector3 intersection = new Vector3();
    public boolean canMoveTo(Vector3 proposedLocation) {
        boolean retValue = false;

        backupLocation.set(node.getTranslation());
        node.setTranslation(proposedLocation);

        boolean hasCollision = false;
        lastDistance = 0;
        for(double angle : COLLISION_ANGLES) {
            if(executePick(angle, proposedLocation, 4)) {
                lastDistance = distanceResults.getPickData(0).getIntersectionRecord().getClosestDistance();
                if(lastDistance <= MIN_DISTANCE) {
                    // We hit the wall.
                    // Hack: instead of messing with normal vectors, simply try the same operation turning to the left and right.
                    // Whichever takes us farther from the wall is the direction we move in.

                    // remember the intersection point
                    intersection.set(distanceResults.getPickData(0).getIntersectionRecord().getIntersectionPoint(
                            distanceResults.getPickData(0).getIntersectionRecord().getClosestIntersection()));

                    // look to the left
                    proposedLocation.set(backupLocation);
                    proposedLocation.addLocal(getDirection(90).normalizeLocal().multiplyLocal(speed * .25));
                    tmpLocation2.set(proposedLocation); // remember this 'cause proposedLocation will be reused below
                    node.setTranslation(proposedLocation);
                    boolean contactA = executePick(angle, proposedLocation, 0);
//                    double distFromOriginalA = intersection.distance(proposedLocation);
                    double distA = contactA && distanceResults.getPickData(0).getIntersectionRecord().getNumberOfIntersections() > 0 ?
                                   distanceResults.getPickData(0).getIntersectionRecord().getClosestDistance() :
                                   0;

                    // look to the right
                    proposedLocation.set(backupLocation);
                    proposedLocation.addLocal(getDirection(-90).normalizeLocal().multiplyLocal(speed * .25));
                    node.setTranslation(proposedLocation);
                    boolean contactB = executePick(angle, proposedLocation, 0);
//                    double distFromOriginalB = intersection.distance(proposedLocation);
                    double distB = contactB && distanceResults.getPickData(0).getIntersectionRecord().getNumberOfIntersections() > 0 ?
                                   distanceResults.getPickData(0).getIntersectionRecord().getClosestDistance() :
                                   0;

//                    System.err.println("90=>" + distA + "(" + freeA + ") -90=>" + distB + "(" + freeB + ")");

                    // decide which way to go:
                    // if A or B hits a wall, pick the farther one
                    // otherwise, try to use the last step's direction
                    int step = 0;
                    if(contactB && distA < distB && distB > MIN_DISTANCE) {
                        step = -90;
                    } else if(contactA && distA > distB && distA > MIN_DISTANCE) {
                        step = 90;
                    } else if(lastStep == -90 && !contactB) {
                        step = -90;
                    } else if(lastStep == 90 && !contactA) {
                        step = -90;
                    }

                    // select the direction that is farther
                    if(step == -90) {
                        lastStep = -90;
                        lastDistance = distB;
                        hasCollision = false;
                    } else if(step == 90) {
                        lastStep = 90;
                        lastDistance = distA;
                        hasCollision = false;
                        node.setTranslation(tmpLocation2);
                    } else {
                        // can't move, we're stuck
                        lastStep = 0;
                        hasCollision = true;
                    }

                    break;
                }
            }
        }

        // check if we're on water
        if(!hasCollision) {
            node.getSceneHints().setPickingHint(PickingHint.Collidable, false);
            node.getSceneHints().setPickingHint(PickingHint.Pickable, false);
            Vector3 v = Vector3.fetchTempInstance();
            v.set(node.getTranslation());
            v.addLocal(getDirection().normalizeLocal().multiplyLocal(2.0f));
            v.addLocal(0, 8, 0);
            down.setOrigin(v);
            Vector3.releaseTempInstance(v);

            // if we hit something we're not on water
            noDistanceResults.clear();
            PickingUtil.findPick(Main.getMain().getTerrain().getNode(), down, noDistanceResults);
            for(int i = 0; i < noDistanceResults.getNumber(); i++) {
                if(noDistanceResults.getPickData(i).getTarget() != null) {
                    retValue = true;
                    break;
                }
            }

            node.getSceneHints().setPickingHint(PickingHint.Collidable, true);
            node.getSceneHints().setPickingHint(PickingHint.Pickable, true);
        }

        if(!retValue) {
            node.setTranslation(backupLocation);
        }

        return retValue;
    }

    private String printVector(Vector3 v) {
        return "" + round(v.getX()) + "," + round(v.getY()) + "," + round(v.getZ());
    }

    private String round(double z) {
        return "" + Math.round(z * 100) / 100.0;
    }

    public Vector3 getDirection() {
        return getDirection(0);
    }

    public Vector3 getDirection(double delta) {
        q.fromRotationMatrix(node.getRotation());
        q.multiplyLocal(p.fromAngleAxis(MathUtils.DEG_TO_RAD * (90.0f + delta), Vector3.UNIT_Y));
        q.getRotationColumn( 2, direction );
        return direction;
    }

    public int getX() {
        return (int)Math.round(node.getTranslation().getX() / ShapeUtil.WALL_WIDTH);
    }
        
    public int getZ() {
        return (int)Math.round(node.getTranslation().getZ() / ShapeUtil.WALL_WIDTH);
    }

    public Spatial getDebugNode() {
        return DEBUG_ENABLED ? debugNode : null;
    }

    public enum Animations {
        stand, run
    }
}
