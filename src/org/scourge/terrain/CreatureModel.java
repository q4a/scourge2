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
import com.ardor3d.intersection.*;
import com.ardor3d.math.*;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.hint.PickingHint;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.visitor.Visitor;
import com.ardor3d.util.geom.BufferUtils;
import org.scourge.Main;
import org.scourge.MovementListener;
import org.scourge.config.CreatureModelTemplate;
import org.scourge.util.ShapeUtil;

import java.util.List;

/**
 * User: gabor
 * Date: Feb 9, 2010
 * Time: 9:51:05 AM
 */
public class CreatureModel implements NodeGenerator {
	private final static double MIN_DISTANCE = 4;
    private final static double[] COLLISION_ANGLES = { -45, -10, 0, 10, 45 };
	private Vector3 tmpLocation = new Vector3();
    private Vector3 tmpLocation2 = new Vector3();
    private Vector3 intersection = new Vector3();
	private Vector3 tempVa = new Vector3();
    private Vector3 backupLocation = new Vector3();
	private Vector3 proposedLocation = new Vector3();
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
    private double speed, lastDistance;
    private Node debugNode = new Node("debug");
    private Vector3[] lineVertex = new Vector3[2 * COLLISION_ANGLES.length];
    private final Line lines = new Line("normLine", lineVertex, null, null, null);
    private int lastStep;
	private boolean moving;
	private MovementListener movementListener;
	private boolean debug;
	private double movementSpeed = 50.0;
	private boolean bounce = true; // if true, the creature will try to 'bounce' off obstacles and keep moving
	private CollisionResults collisionResults = new PrimitiveCollisionResults();
	private Node shape;

	public CreatureModel(CreatureModelTemplate model, String skin, String namePrefix) {
		this(model, skin, namePrefix, false);
	}

	public CreatureModel(CreatureModelTemplate model, String skin, String namePrefix, boolean debug) {
		this.debug = debug;

        this.model = model;

        // point it down
        down.setDirection(new Vector3(0, -1, 0));
        noDistanceResults.setCheckDistance(false);

        distanceResults.setCheckDistance(true);

		// the node used for collision detection
		node = new Node(ShapeUtil.newShapeName(namePrefix));
		Box box = new Box(ShapeUtil.newShapeName("box_" + namePrefix), new Vector3(0, 0, 0), 1, 1, 1);
		box.setModelBound(new BoundingBox());
		CullState cullState = new CullState();
        cullState.setCullFace(CullState.Face.FrontAndBack);
        cullState.setEnabled(true);
        box.setRenderState(cullState);
		node.attachChild(box);

		// the actual animated shape
        final ColladaStorage storage = new ColladaImporter().load(model.getModel());
        final List<SkinData> skinDatas = storage.getSkins();
        shape = skinDatas.get(0).getSkinBaseNode();
        model.transform(shape);
		shape.getSceneHints().setPickingHint(PickingHint.Collidable, false);
		node.setTranslation(0, -0.5, 0); // half the height of 'box' down
		node.attachChild(shape);

        node.updateWorldBound(true);

		setupAnimation(storage);
    }

	private void setupAnimation(ColladaStorage storage) {
        // Make our manager
        manager = new AnimationManager(Main.getMain().getTimer(), storage.getSkins().get(0).getPose());

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

		shape.addController(new SpatialController() {
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

    public void moveToTopOfTerrain(double tpf) {
        Terrain.moveOnTopOfTerrain(node);

        // move forward
        if(isMoving()) {
            speed = movementSpeed * tpf;
            proposedLocation.set(node.getTranslation());
            // don't move more than the last sensed distance (ie. don't move into the wall)
            proposedLocation.addLocal(getDirection().multiply(lastDistance > 0 && lastDistance < speed ? lastDistance : speed, tempVa));
            if(canMoveTo()) {
                movementListener.moved();
            } else {
				movementListener.stopped();
			}
        }

        if(debug) {
            // show debug
            debugNode.setTranslation(node.getWorldTranslation());
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

	protected boolean testWater() {
		boolean retValue = false;
		node.getSceneHints().setPickingHint(PickingHint.Collidable, false);
		node.getSceneHints().setPickingHint(PickingHint.Pickable, false);
		Vector3 v = Vector3.fetchTempInstance();
		v.set(node.getWorldBound().getCenter());
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

		if(debug && !retValue) {
			System.err.println("on water");
		}

		node.getSceneHints().setPickingHint(PickingHint.Collidable, true);
		node.getSceneHints().setPickingHint(PickingHint.Pickable, true);
		return retValue;
	}

	protected int getCollisionIndex(CollisionResults collisionResults) {
		for(int i = 0; i < collisionResults.getNumber(); i++) {
			if(collisionResults.getCollisionData(i).getTargetPrimitives().size() > 0) {
				return i;
			}
		}
		return -1;
	}

	public boolean testCollision() {
		// set scale (for buffer around shape)
		node.setScale(2, 1, 2);
		// elevate Y position so we walk over bridges, etc.
		tmpLocation.set(node.getTranslation().getX(), node.getTranslation().getY() + 4, node.getTranslation().getZ());
		node.setTranslation(tmpLocation);
		// update world position
		node.updateWorldTransform(true);
		node.updateWorldBound(true);

		// initial collision check
		collisionResults.clear();
		PickingUtil.findCollisions(node, Main.getMain().getTerrain().getNode(), collisionResults);
		boolean hasCollision = false;
		int collisionIndex = getCollisionIndex(collisionResults);
		if(collisionIndex > -1) {
			hasCollision = true;

			if(bounce) {
				// Hack: instead of messing with normal vectors, simply try the same operation turning to the left and right.
				// Whichever takes us farther from the wall is the direction we move in.

				// remember the intersection point
				intersection.set(collisionResults.getCollisionData(collisionIndex).getTargetMesh().getWorldBound().getCenter());

				// look to the left
				proposedLocation.set(backupLocation.getX(), backupLocation.getY() + 4, backupLocation.getZ());
				proposedLocation.addLocal(getDirection(90).normalizeLocal().multiplyLocal(speed * .25));
				tmpLocation2.set(proposedLocation); // remember this 'cause proposedLocation will be reused below
				node.setTranslation(proposedLocation);
				node.updateWorldTransform(true);
				node.updateWorldBound(true);

				collisionResults.clear();
				PickingUtil.findCollisions(node, Main.getMain().getTerrain().getNode(), collisionResults);
				int contactA = getCollisionIndex(collisionResults);
				double distA = contactA == -1 ? proposedLocation.distance(intersection) : 0;

				// look to the right
				proposedLocation.set(backupLocation.getX(), backupLocation.getY() + 4, backupLocation.getZ());
				proposedLocation.addLocal(getDirection(-90).normalizeLocal().multiplyLocal(speed * .25));
				node.setTranslation(proposedLocation);
				node.updateWorldTransform(true);
				node.updateWorldBound(true);

				collisionResults.clear();
				PickingUtil.findCollisions(node, Main.getMain().getTerrain().getNode(), collisionResults);
				int contactB = getCollisionIndex(collisionResults);
				double distB = contactB == -1 ? proposedLocation.distance(intersection) : 0;

//                    System.err.println("90=>" + distA + "(" + freeA + ") -90=>" + distB + "(" + freeB + ")");

				// decide which way to go:
				// pick the farther one
				// or, try to use the last step's direction
				int step = 0;
				if(contactB == -1 && distA < distB && distB > MIN_DISTANCE) {
					step = -90;
				} else if(contactA == -1 && distA > distB && distA > MIN_DISTANCE) {
					step = 90;
				} else if(lastStep == -90 && contactB != -1) {
					step = -90;
				} else if(lastStep == 90 && contactA != -1) {
					step = -90;
				}

				// make the step
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
			}
		}

		// reset scale and Y position
		node.setScale(1);
		tmpLocation.set(node.getTranslation().getX(), node.getTranslation().getY() - 4, node.getTranslation().getZ());
		node.setTranslation(tmpLocation);
		return hasCollision;
	}

	private String getFullName(Spatial spatial) {
		StringBuffer sb = new StringBuffer();
		Spatial p = spatial;
		while(p != null) {
			if(sb.length() > 0) sb.append("->");
			sb.append(p.getName());
			p = p.getParent();
		}
		return sb.toString();
	}

    public boolean canMoveTo() {
        boolean retValue = false;

        backupLocation.set(node.getTranslation());
        node.setTranslation(proposedLocation);

        if(!testCollision()) {
			retValue = testWater();
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
        return debug ? debugNode : null;
    }

	public void setBounce(boolean bounce) {
		this.bounce = bounce;
	}

	public enum Animations {
        stand, run
    }

	public boolean isMoving() {
		return moving;
	}

	public void setMoving(boolean moving) {
		this.moving = moving;
	}

	public MovementListener getMovementListener() {
		return movementListener;
	}

	public void setMovementListener(MovementListener movementListener) {
		this.movementListener = movementListener;
	}

	public double getMovementSpeed() {
		return movementSpeed;
	}

	public void setMovementSpeed(double movementSpeed) {
		this.movementSpeed = movementSpeed;
	}





	public boolean testCollisionOld() {
		boolean hasCollision = false;
        lastDistance = 0;
        for(double angle : COLLISION_ANGLES) {
            if(executePick(angle, proposedLocation)) {
                lastDistance = distanceResults.getPickData(0).getIntersectionRecord().getClosestDistance();
                if(lastDistance <= MIN_DISTANCE) {
					// We hit the wall.
					if(debug) {
						System.err.println("collision target=" + distanceResults.getPickData(0).getTarget() + " dist=" + lastDistance);
					}

					if(!bounce) {
						hasCollision = true;
					} else {

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
//						node.updateWorldTransform(false);
						boolean contactA = executePick(angle, proposedLocation);
	//                    double distFromOriginalA = intersection.distance(proposedLocation);
						double distA = contactA && distanceResults.getPickData(0).getIntersectionRecord().getNumberOfIntersections() > 0 ?
									   distanceResults.getPickData(0).getIntersectionRecord().getClosestDistance() :
									   0;

						// look to the right
						proposedLocation.set(backupLocation);
						proposedLocation.addLocal(getDirection(-90).normalizeLocal().multiplyLocal(speed * .25));
						node.setTranslation(proposedLocation);
//						node.updateWorldTransform(false);
						boolean contactB = executePick(angle, proposedLocation);
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
					}

                    break;
                }
            }
        }
		return hasCollision;
	}

	private boolean executePick(double angle, ReadOnlyVector3 pos) {
        tmpLocation.set(pos);
        tmpLocation.setY(tmpLocation.getY() + 8);
        forward.setOrigin(tmpLocation);
        forward.setDirection(getDirection(angle));
        distanceResults.clear();
//		node.getSceneHints().setAllPickingHints(false);
        PickingUtil.findPick(Main.getMain().getTerrain().getNode(), forward, distanceResults);
//		node.getSceneHints().setAllPickingHints(true);

        return (distanceResults.getNumber() != 0 &&
                distanceResults.getPickData(0).getTarget() != null &&
                distanceResults.getPickData(0).getIntersectionRecord() != null);
    }
}
