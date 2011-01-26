package org.scourge.terrain;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.blendtree.ClipSource;
import com.ardor3d.extension.animation.skeletal.blendtree.SimpleAnimationApplier;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.JointChannel;
import com.ardor3d.extension.animation.skeletal.state.SteadyState;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.hint.PickingHint;
import com.ardor3d.scenegraph.shape.Box;
import org.omg.CORBA.TypeCodePackage.Bounds;
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
    private PrimitivePickResults  noDistanceResults = new PrimitivePickResults();
    private Quaternion q = new Quaternion();
    private Quaternion p = new Quaternion();
    private Vector3 direction = new Vector3();
    private AnimationManager manager;
    private CreatureModelTemplate model;
    private Box debugNode;
    private Vector3 debugLocation = new Vector3();
    private static final double ABOVE_GROUND = 4;
    private static final boolean DEBUG_ENABLED = false;

    public CreatureModel(CreatureModelTemplate model, String skin, String namePrefix) {
        this.model = model;
        // point it down
        down.setDirection(new Vector3(0, -1, 0));
        noDistanceResults.setCheckDistance(false);

        final ColladaStorage storage = new ColladaImporter().load(model.getModel());
        //Node colladaNode = storage.getScene();
        final List<SkinData> skinDatas = storage.getSkins();
        node = skinDatas.get(0).getSkinBaseNode();
        model.transform(node);
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

        // the debug box
        BoundingBox bb = (BoundingBox)node.getWorldBound();
        debugNode = new Box("box", new Vector3(0, 0, 0), bb.getXExtent(), bb.getYExtent(), bb.getZExtent());
        debugNode.getSceneHints().setPickingHint(PickingHint.Collidable, false);
        debugNode.getSceneHints().setPickingHint(PickingHint.Pickable, false);

        node.addController(new SpatialController() {
            @Override
            public void update(double v, Spatial spatial) {
                manager.update();

                // move the debug box
                if(DEBUG_ENABLED) {
                    debugLocation.set(node.getTranslation().getX(),
                                      node.getTranslation().getY() + ABOVE_GROUND,
                                      node.getTranslation().getZ());
                    debugNode.setTranslation(debugLocation);
    //                debugNode.setRotation(node.getRotation());
                }
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
    }

    @Override
    public Node getNode() {
        return node;
    }

    public void moveToTopOfTerrain() {
        Terrain.moveOnTopOfTerrain(node);
    }

    private Vector3 backupLocation = new Vector3();
    private Vector3 tmpLocation = new Vector3();
    private Vector3 backupScale = new Vector3();
    public boolean canMoveTo(Vector3 proposedLocation) {
        boolean retValue = false;

        backupLocation.set(node.getTranslation());
        tmpLocation.set(proposedLocation);
        tmpLocation.setY(tmpLocation.getY() + ABOVE_GROUND);

        // Make the player 2 units tall. This "lifts" him off the floor so we can walk over floor irregularities, ramps, bridges, etc.
        // Also this makes him shorter so we can fit thru the dungeon entrance. Yes this is a hack.
        backupScale.set(node.getScale());
        BoundingBox bb = (BoundingBox)node.getWorldBound();
        double bx = (2.0f / bb.getXExtent()) * node.getScale().getX();
        double by = (1.0f / bb.getYExtent()) * node.getScale().getY();
        double bz = (2.0f / bb.getZExtent()) * node.getScale().getZ();
        node.setScale(bx, by, bz);

        node.setTranslation(tmpLocation);
        node.updateGeometricState(0, false); // make geometry changes take effect now!
        node.updateWorldBound(true);
        node.updateWorldTransform(true);

        if(DEBUG_ENABLED) {
            bb = (BoundingBox)node.getWorldBound();
            debugNode.setData(new Vector3(), bb.getXExtent(), bb.getYExtent(), bb.getZExtent());
        }

        boolean hasCollision = PickingUtil.hasCollision(Main.getMain().getTerrain().getNode(), node, true);

        // reset the position
        node.setTranslation(proposedLocation);
        node.updateGeometricState(0, false);
        node.updateWorldBound(true);
        node.updateWorldTransform(true);

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

        node.setScale(backupScale);
        if(!retValue) {
            node.setTranslation(backupLocation);
        }

        return retValue;
    }

    public Vector3 getDirection() {
        q.fromRotationMatrix(node.getRotation());
        q.multiplyLocal(p.fromAngleAxis(MathUtils.DEG_TO_RAD * 90.0f, Vector3.UNIT_Y));
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
