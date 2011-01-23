package org.scourge.util;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.blendtree.SimpleAnimationApplier;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
import com.ardor3d.extension.model.md2.Md2DataStore;
import com.ardor3d.extension.model.md2.Md2Importer;
import com.ardor3d.image.Texture;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;
import org.newdawn.ardor3d.loader.max.TDSFile;
import org.scourge.terrain.Model;

import javax.swing.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: gabor
 * Date: Feb 7, 2010
 * Time: 5:45:08 PM
 */
public class ShapeUtil {
//    private static final Md2ToJme CONVERTER_MD2 = new Md2ToJme();
//    private static final FormatConverter CONVERTER_3DS = new MaxToJme();
//    private static final FormatConverter CONVERTER_OBJ = new ObjToJme();
//    private static final FormatConverter CONVERTER_MD3 = new Md3ToJme();
    private static int shapeCount = 0;
    public static final float WALL_WIDTH = 16.0f;
    public static final float WALL_HEIGHT = 24.0f;
    private static Logger logger = Logger.getLogger(ShapeUtil.class.toString());
    private static WeakHashMap<String, Texture> textures = new WeakHashMap<String, Texture>();
    private static WeakHashMap<String, ImageIcon> images = new WeakHashMap<String, ImageIcon>();
    private static final Map<String, Spatial> staticModelPrototypes = new HashMap<String, Spatial>();
    private static final Map<String, Spatial> animatedModelPrototypes = new HashMap<String, Spatial>();
    private static final Map<String, Md2DataStore> md2prototypes = new HashMap<String, Md2DataStore>();

    public static String newShapeName(String prefix) {
        return prefix + "_" + (shapeCount++);
    }

    public static Node loadMd2(String modelPath, String texturePath, String name_prefix, boolean invertNormals, Map<String, Integer[]> frames) {
        synchronized(md2prototypes) {
            String key = modelPath + "." + texturePath;
            Md2DataStore storage = md2prototypes.get(key);
            Spatial prototype;
            if(storage == null) {
                final Md2Importer importer = new Md2Importer();
                storage = importer.load(modelPath);
                md2prototypes.put(key, storage);

                // store frame info in frames
                // todo: seems to be no way of doing this

                prototype = storage.getMesh();
                prototype.setName(newShapeName(name_prefix));
                // md2 models are usually z-up - switch to y-up
                prototype.setRotation(new Quaternion().fromAngleAxis(-MathUtils.HALF_PI, Vector3.UNIT_X));

                // todo: not sure how to do this
//                for(int i = 0; i < ((Node)prototype).getChild(0).getControllerCount(); i++) {
//                    KeyframeController kc = (KeyframeController)((Node)prototype).getChild(0).getController(i);
//                    if(invertNormals) {
//                        for(KeyframeController.PointInTime pit : kc.) {
//                            pit.newShape.rotateNormals(new Quaternion().fromAngleAxis(180.0f * FastMath.DEG_TO_RAD, Vector3.UNIT_Z));
//                        }
//                    }
//                }

                ((Mesh)prototype).getMeshData().rotateNormals(new Quaternion().fromAngleAxis(180.0f * MathUtils.DEG_TO_RAD, Vector3.UNIT_Z));
                prototype.updateGeometricState(0);
                prototype.updateWorldRenderStates(true);

                animatedModelPrototypes.put(key, prototype);

                if(invertNormals) {
                    CullState cs = new CullState();
                    cs.setCullFace(CullState.Face.Front);
                    cs.setEnabled(true);
                    prototype.setRenderState(cs);
                }

                System.err.println(modelPath);
                debugNode(prototype, "");

//                    prototype.setModelBound(new BoundingBox());
//                    prototype.updateModelBound();

            } else {
                prototype = animatedModelPrototypes.get(key);
            }

//                // copy the frames
//                frames.putAll(prototypeFrames.get(key));
//
//                // clone the prototype (can't use sharedmesh b/c the animation changes the mesh)
//                CloneImportExport ie = new CloneImportExport();
//                ie.saveClone(prototype);
//                Node copy = (Node) ie.loadClone();
//                copy.setModelBound(new BoundingBox());
//                copy.updateModelBound();
//                copy.updateWorldBound();
//                copy.updateGeometricState(0, true);
//
//                // Share the keyframe shapes (otherwise we run out of memory and there is no reason to have copies of these)
//                for(int i = 0; i < ((Node)prototype).getChild(0).getControllerCount(); i++) {
//                    KeyframeController kc = (KeyframeController)((Node)prototype).getChild(0).getController(i);
//                    KeyframeController copyKc = (KeyframeController)copy.getChild(0).getController(i);
//                    for(int t = 0; t < kc.keyframes.size(); t++) {
//                        KeyframeController.PointInTime pit = kc.keyframes.get(t);
//                        KeyframeController.PointInTime pitCopy = copyKc.keyframes.get(t);
//                        pitCopy.newShape = pit.newShape;
//                    }
//                }

            Node copyNode = new Node(newShapeName(name_prefix));
            Spatial copy = prototype.makeCopy(true);
            copyNode.attachChild(copy);

            TextureState ts = new TextureState();
            ts.setEnabled(true);
            //Texture texture = TextureManager.loadTexture(texturePath, Texture.MinificationFilter.Trilinear, Texture.MagnificationFilter.Bilinear, 0.0f, false);
            Texture texture = loadTexture(texturePath, texturePath, false);
            ts.setTexture(texture);
            copy.setRenderState(ts);
//                copy.updateRenderState();

            return copyNode;
        }
    }

    public static Spatial importModel(String modelPath, String textureDir, String name_prefix, Model model,
                                      float rotateX, float rotateY, float rotateZ) {
        return importModel(modelPath, textureDir, name_prefix, model, rotateX, rotateY, rotateZ, null);
    }

    public static Spatial importModel(String modelPath, String textureDir, String name_prefix, Model model,
                                      float rotateX, float rotateY, float rotateZ,
                                      Class controllerClass) {
        synchronized(staticModelPrototypes) {
            try {
                String key = modelPath + "." + name_prefix;
                Spatial prototype = staticModelPrototypes.get(key);
                if(prototype == null) {
                    prototype = TDSFile.load3DSModel(modelPath);

                    // todo: why only x? Adding y,z causes trees to show up flat...
                    if(rotateX != 0) {
                        Quaternion modelRotation = new Quaternion();
                        modelRotation.fromEulerAngles(0, 0, rotateX * MathUtils.DEG_TO_RAD);

                        // apply the rotation directly to the data, so Y points "up"
                        List<Mesh> meshes = ShapeUtil.findMeshes(prototype, new ArrayList<Mesh>());
                        for(Mesh mesh : meshes) {
                            mesh.getMeshData().rotatePoints(modelRotation);
                            mesh.getMeshData().rotateNormals(modelRotation);
                        }
                    }

                    if(modelPath.endsWith(".md3")) {
                        invertNormals((Node)prototype);
                    }
                    if(model != null) {
                        model.onLoad(prototype);
                    }

                    if(controllerClass != null) {
                        prototype.addController((SpatialController)controllerClass.newInstance());
                    }

                    prototype.updateGeometricState(0, true);
//                    System.err.println(">>> " + key + " <<<");
//                    debugNode(prototype, "");
                    staticModelPrototypes.put(key, prototype);
                }
                return copyPrototype(prototype, name_prefix, modelPath);
            } catch (Exception exc) {
                logger.log(Level.SEVERE, "Error loading model:" + modelPath + " error=" + exc.getMessage(), exc);
                throw new RuntimeException(exc);
            }
        }
	}

    private static Spatial copyPrototype(Spatial prototype, String name_prefix, String modelPath) {
        try {
            // Remove and re-add the controller. This is b/c our controller operates on the shared data,
            // so there is no need to have multiple copies of it. In fact, multiple copies kills performance.
            SpatialController sc = null;
            if(prototype.getControllerCount() > 0) {
                sc = prototype.getController(0);
                prototype.removeController(0);
            }
            Spatial copy = prototype.makeCopy(true);
            if(sc != null) {
                prototype.addController(sc);
            }
            if(modelPath.endsWith(".md3")) {
                CullState cs = new CullState();
                cs.setCullFace(CullState.Face.Front);
                cs.setEnabled(true);
                copy.setRenderState(cs);
            }
            return copy;
        } catch(Throwable exc) {
            logger.log(Level.SEVERE, "Unable to clone " + modelPath, exc);
            debugNode(prototype, "  ");
            throw new RuntimeException(exc);
        }
    }

    private static void invertNormals(Node node) {
        for(Spatial child : node.getChildren()) {
            if(child instanceof Mesh) {
                flipNormals((Mesh)child);
            }
        }
    }

    public static void flipNormals(Mesh n) {
        Vector3 store = new Vector3();
        for (int x = 0; x < n.getMeshData().getVertexCount(); x++) {
            BufferUtils.populateFromBuffer(store, n.getMeshData().getNormalBuffer(), x);
            store.multiply(-1f, store);
            BufferUtils.setInBuffer(store, n.getMeshData().getNormalBuffer(), x);
        }
    }

    public static void debugNode(Spatial node, String indent) {
        System.err.println(indent + node.getName() + "," + node.getClass() + "," + node.toString());
        if(node instanceof Node) {
            for(Spatial child : ((Node)node).getChildren()) {
                debugNode(child, indent + "  ");
            }
        }
    }

    public static ImageIcon loadImageIcon(String path) {
        ImageIcon icon = images.get(path);
        if(icon == null) {
            icon = new ImageIcon(path);
            images.put(path, icon);
        }
        return icon;
    }

    public static Texture loadTexture(String path) {
        return loadTexture(path, path);
    }

    public static Texture loadTexture(String path, String textureKey) {
        return loadTexture(path, textureKey, true);
    }

    public static Texture loadTexture(String path, String textureKey, boolean flip) {
        Texture t0 = textures.get(textureKey);
        if (t0 == null) {
        	//t0 = TextureManager.load(path, Texture.MinificationFilter.Trilinear, Texture.MagnificationFilter.Bilinear, 0, flip);
            t0 = TextureManager.load(path, Texture.MinificationFilter.Trilinear, flip);
	        textures.put(textureKey, t0);
        }
        return t0;
    }

    public static void debug() {
        logger.info("loaded " + textures.size() + " textures and " + images.size() + " images.");
    }

    public static boolean isTextureLoaded(String key) {
        return textures.keySet().contains(key);
    }

    public static void storeTexture(String key, Texture texture) {
        textures.put(key, texture);
    }

    public static Texture getTexture(String key) {
        return textures.get(key);
    }

    public static List<Mesh> findMeshes(Spatial node, ArrayList<Mesh> meshes) {
        if(node instanceof Mesh) {
            meshes.add((Mesh)node);
        } else if(node instanceof Node) {
            for(Spatial child : ((Node)node).getChildren()) {
                findMeshes(child, meshes);
            }
        }
        return meshes;
    }

    // Why is this necessary? Because our tree controllers operator on shared vertex data, so the controller is only
    // assigned to the prototype, not the individual trees. And since the prototype is never on screen, its controller
    // would never be updated.
    //
    // todo: optimization to only update those prototypes' controllers that have instances on screen
    public static void updateControllers(double time) {
        synchronized(staticModelPrototypes) {
            for(Spatial s : staticModelPrototypes.values()) {
                s.updateControllers(time);
            }
        }
    }
}
