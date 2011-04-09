package org.scourge.terrain;


import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.extension.effect.particle.ParticleFactory;
import com.ardor3d.extension.effect.particle.ParticleSystem;
import com.ardor3d.extension.effect.particle.RampEntry;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.hint.PickingHint;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import org.scourge.util.ShapeUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * User: gabor
 * Date: Mar 27, 2010
 * Time: 8:59:30 PM
 */
public enum Model implements Savable {
    bridge("./data/3ds/bridge.3ds", true) {
        public Spatial createSpatial() {
            Spatial sp = getNoAlphaSpatial();
            sp.setTranslation(sp.getTranslation().add(ShapeUtil.WALL_WIDTH / 2, 0, ShapeUtil.WALL_WIDTH / 2, null));
//            sp.updateModelBound();
            return sp;
        }
    },
    fir("./data/3ds/fir.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(5);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    oak("./data/3ds/tree02.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(7);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    willow("./data/3ds/tree03.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(7);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    birch("./data/3ds/birch.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(4);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    birch2("./data/3ds/tree13.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(7);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    redOak("./data/3ds/tree14.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(7);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    bigOak("./data/3ds/tree15.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(7);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    redLeaf("./data/3ds/tree16.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(7);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    bush("./data/3ds/tree17.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(3);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    deadTree("./data/3ds/tree18.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(7);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    palm1("./data/3ds/tree19.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(7);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    multiColor("./data/3ds/tree20.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(7);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    cypress("./data/3ds/tree21.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(7);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    bushtree("./data/3ds/bushtree.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(3);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    cactus("./data/3ds/cactus.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(7);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    fern("./data/3ds/fern.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(6);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    palm2("./data/3ds/palm2.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(4);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    bigpalm("./data/3ds/palm.3ds") {
        public Spatial createSpatial() {
            return getAlphaSpatial(4);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    oldoak("./data/md3/oak/oak1.md3") {
        @Override
        public Spatial createSpatial() {
            return getAlphaSpatial(0.12f, 0, 0, 90);
        }

        @Override
        public ReadOnlyVector3 getRotationVector() {
            return Vector3.UNIT_Y;
        }

        @Override
        public void onLoad(Spatial spatial) {
            Map<String, String> textures = new HashMap<String, String>();
            textures.put("stamm", "data/md3/oak/oakstamm.jpg");
            textures.put("bruch", "data/md3/oak/oakstamm.jpg");
            textures.put("blaetter", "data/md3/oak/oakblaetter.tga");
            assignTextures(spatial, textures);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    oldoak2("./data/md3/oak/oak2.md3") {
        @Override
        public Spatial createSpatial() {
            return getAlphaSpatial(0.12f, 0, 0, 90);
        }

        @Override
        public ReadOnlyVector3 getRotationVector() {
            return Vector3.UNIT_Y;
        }

        @Override
        public void onLoad(Spatial spatial) {
            Map<String, String> textures = new HashMap<String, String>();
            textures.put("stamm", "data/md3/oak/oakstamm2.jpg");
            textures.put("bruch", "data/md3/oak/oakstamm2.jpg");
            textures.put("blaetter", "data/md3/oak/oakblaetter2.tga");
            assignTextures(spatial, textures);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    bigfir("./data/md3/jkm_trees/nadelbaum.md3") {
        @Override
        public Spatial createSpatial() {
            return getAlphaSpatial(0.15f, 0, 0, 90);
        }

        @Override
        public ReadOnlyVector3 getRotationVector() {
            return Vector3.UNIT_Y;
        }

        @Override
        public void onLoad(Spatial spatial) {
            Map<String, String> textures = new HashMap<String, String>();
            textures.put("stumpf", "data/md3/jkm_trees/stamm.jpg");
            textures.put("aeste", "data/md3/jkm_trees/nadel.tga");
            textures.put("stumpf2", "data/md3/jkm_trees/stamm.jpg");
            assignTextures(spatial, textures);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    normal_yellow("./data/md3/jkm_trees/tree1.md3") {
        @Override
        public Spatial createSpatial() {
            return getAlphaSpatial(0.11f, 0, 0, 90);
        }

        @Override
        public ReadOnlyVector3 getRotationVector() {
            return Vector3.UNIT_Y;
        }

        @Override
        public void onLoad(Spatial spatial) {
            Map<String, String> textures = new HashMap<String, String>();
            textures.put("stumpf", "data/md3/jkm_trees/stamm.jpg");
            textures.put("aeste", "data/md3/jkm_trees/tree.tga");
            textures.put("stumpf2", "data/md3/jkm_trees/stamm.jpg");
            assignTextures(spatial, textures);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    normal_green("./data/md3/jkm_trees/tree2.md3") {
        @Override
        public Spatial createSpatial() {
            return getAlphaSpatial(0.11f, 0, 0, 90);
        }

        @Override
        public ReadOnlyVector3 getRotationVector() {
            return Vector3.UNIT_Y;
        }

        @Override
        public void onLoad(Spatial spatial) {
            Map<String, String> textures = new HashMap<String, String>();
            textures.put("stumpf", "data/md3/jkm_trees/stamm.jpg");
            textures.put("aeste", "data/md3/jkm_trees/tree6.tga");
            textures.put("stumpf2", "data/md3/jkm_trees/stamm.jpg");
            assignTextures(spatial, textures);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    normal_red("./data/md3/jkm_trees/tree3.md3") {
        @Override
        public Spatial createSpatial() {
            return getAlphaSpatial(0.11f, 0, 0, 90);
        }

        @Override
        public ReadOnlyVector3 getRotationVector() {
            return Vector3.UNIT_Y;
        }

        @Override
        public void onLoad(Spatial spatial) {
            Map<String, String> textures = new HashMap<String, String>();
            textures.put("stumpf", "data/md3/jkm_trees/stamm.jpg");
            textures.put("aeste", "data/md3/jkm_trees/tree3.tga");
            textures.put("stumpf2", "data/md3/jkm_trees/stamm.jpg");
            assignTextures(spatial, textures);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },
    normal_green2("./data/md3/jkm_trees/tree4.md3") {
        @Override
        public Spatial createSpatial() {
            return getAlphaSpatial(0.11f, 0, 0, 90);
        }

        @Override
        public ReadOnlyVector3 getRotationVector() {
            return Vector3.UNIT_Y;
        }

        @Override
        public void onLoad(Spatial spatial) {
            Map<String, String> textures = new HashMap<String, String>();
            textures.put("stumpf", "data/md3/jkm_trees/stamm.jpg");
            textures.put("aeste", "data/md3/jkm_trees/tree4.tga");
            textures.put("stumpf2", "data/md3/jkm_trees/stamm.jpg");
            assignTextures(spatial, textures);
        }

        protected Class getControllerClass() {
            return TreeController.class;
        }
    },

    ladder("./data/3ds/ladder.3ds") {
        @Override
        public Spatial createSpatial() {
            return getAlphaSpatial(1, 0, 0, 0);
        }
    },
    mountain("./data/3ds/mtn.3ds") {
        @Override
        public Spatial createSpatial() {
            return getNoAlphaSpatial();
        }
    },
    dungeonColumn("./data/3ds/col2.3ds", true) {
        @Override
        public Spatial createSpatial() {
            return getNoAlphaSpatial();
        }
    },
    dungeonColumn2("./data/3ds/col.3ds", true) {
        @Override
        public Spatial createSpatial() {
            return getNoAlphaSpatial();
        }
    },
    edge_corner("./data/3ds/edge-c.3ds", true) {
        @Override
        public Spatial createSpatial() {
            return getNoAlphaSpatial();
        }
    },
    edge_tip("./data/3ds/edge-t.3ds", true) {
        @Override
        public Spatial createSpatial() {
            return getNoAlphaSpatial();
        }
    },
    edge_side("./data/3ds/edge-s.3ds", true) {
        @Override
        public Spatial createSpatial() {
            return getNoAlphaSpatial();
        }
    },
    edge_gate("./data/3ds/edge-g.3ds", true) {
        @Override
        public Spatial createSpatial() {
            return getNoAlphaSpatial();
        }
    },
    edge_up("./data/3ds/edge-u.3ds", true) {
        @Override
        public Spatial createSpatial() {
            return getNoAlphaSpatial();
        }
    },
    edge_down("./data/3ds/edge-d.3ds", true) {
        @Override
        public Spatial createSpatial() {
            return getNoAlphaSpatial();
        }
    },
    edge_bridge("./data/3ds/edge-b.3ds", true) {

        @Override
        public Spatial createSpatial() {
            return getNoAlphaSpatial();
        }
    },
    sign("./data/3ds/sign.3ds", false) {
        @Override
        public Spatial createSpatial() {
            return getAlphaSpatial(1f);
        }

//        @Override
//        public ReadOnlyVector3 getRotationVector() {
//            return Vector3.UNIT_X;
//        }
    },
    torch("./data/3ds/torch2.3ds", true) {
        @Override
        public Spatial createSpatial() {
            Node node = new Node(ShapeUtil.newShapeName("torch"));
            Spatial spatial = getNoAlphaSpatial();
            node.attachChild(spatial);
            node.attachChild(addTorchFlame(new FlameTypeConfig() {
                public void configure(ParticleSystem particles) {
                    particles.setStartColor(new ColorRGBA(1, 1, 0.5f, 1));
                    particles.setStartSize(5f);

                    {
                        final RampEntry entry = new RampEntry(0.15f);
                        entry.setColor(new ColorRGBA(1, 0.33f, 0, 0.5f));
                        entry.setSize(10f);
                        particles.getRamp().addEntry(entry);
                    }

                    {
                        final RampEntry entry = new RampEntry(0.10f);
                        entry.setColor(new ColorRGBA(1, 0, 0, 0.25f));
                        entry.setSize(6f);
                        particles.getRamp().addEntry(entry);
                    }

                    {
                        final RampEntry entry = new RampEntry(0.5f);
                        entry.setColor(new ColorRGBA(0f, 0f, 0f, 0.05f));
                        entry.setSize(3f);
                        particles.getRamp().addEntry(entry);
                    }

                    // End color
                    particles.setEndColor(new ColorRGBA(0f, 0f, 0f, 0.05f));
                    particles.setEndSize(2f);
                }
            }));
            return node;
        }
    },
    torchGreen("./data/3ds/torch2.3ds", true) {
        @Override
        public Spatial createSpatial() {
            Node node = new Node(ShapeUtil.newShapeName("torch"));
            Spatial spatial = getNoAlphaSpatial();
            node.attachChild(spatial);
            node.attachChild(addTorchFlame(new FlameTypeConfig() {

                @Override
                public void configure(ParticleSystem particles) {
                    particles.setStartColor(new ColorRGBA(0.5f, 1, 1, 1));
                    particles.setStartSize(5f);

                    {
                        final RampEntry entry = new RampEntry(0.15f);
                        entry.setColor(new ColorRGBA(0, 1, 0.33f, 0.5f));
                        entry.setSize(10f);
                        particles.getRamp().addEntry(entry);
                    }

                    {
                        final RampEntry entry = new RampEntry(0.10f);
                        entry.setColor(new ColorRGBA(0, 1, 0, 0.25f));
                        entry.setSize(6f);
                        particles.getRamp().addEntry(entry);
                    }

                    {
                        final RampEntry entry = new RampEntry(0.5f);
                        entry.setColor(new ColorRGBA(0f, 0f, 0f, 0.05f));
                        entry.setSize(3f);
                        particles.getRamp().addEntry(entry);
                    }

                    // End color
                    particles.setEndColor(new ColorRGBA(0f, 0f, 0f, 0.05f));
                    particles.setEndSize(2f);
                }
            }));
            return node;
        }
    },
    fountain("./data/3ds/pool.3ds") {
        @Override
        public Spatial createSpatial() {
            Node node = new Node(ShapeUtil.newShapeName("fountain"));
            Spatial spatial = getAlphaSpatial(0.15f, -90, 0, 0);
            spatial.setTranslation(spatial.getTranslation().getX(), spatial.getTranslation().getY(), spatial.getTranslation().getZ() - 8);
            node.attachChild(spatial);
            node.attachChild(addTorchFlame(new FlameTypeConfig() {

                @Override
                public void configure(ParticleSystem particles) {
                    particles.setStartColor(new ColorRGBA(0, 1, 1, 1));
                    particles.setStartSize(5f);
                    particles.setEndColor(new ColorRGBA(0.85f, 0.95f, 1f, 0.5f));
                    particles.setEndSize(12f);
                    particles.setMinimumLifeTime(800);
                    particles.setMaximumLifeTime(500);
                }
            }));
            return node;
        }
	},
	door("./data/3ds/door.3ds") {
		@Override
		public Spatial createSpatial() {
			return getNoAlphaSpatial();
		}
	},
	wall("./data/3ds/wall.3ds") {
		@Override
		public Spatial createSpatial() {
			return getAlphaSpatial(1, 0, 0, 0);
		}
	},
	window("./data/3ds/win.3ds") {
		@Override
		public Spatial createSpatial() {
			return getAlphaSpatial(1, 0, 0, 0);
		}
	},
	doorFrame("./data/3ds/dframe.3ds") {
		@Override
		public Spatial createSpatial() {
			return getAlphaSpatial(1, 0, 0, 0);
		}
	},
	houseStairs("./data/3ds/h-stairs.3ds") {
		@Override
		public Spatial createSpatial() {
			return getAlphaSpatial(1, 0, 0, 0);
		}
	}



    ;

    private static Spatial addTorchFlame(FlameTypeConfig config) {
        final ParticleSystem particles = ParticleFactory.buildParticles("particles", 20);
        particles.setEmissionDirection(new Vector3(0, 1, 0));
        particles.setInitialVelocity(0.05f);
        particles.setMinimumLifeTime(1000);
        particles.setMaximumLifeTime(1000);
        particles.setMaximumAngle(15 * MathUtils.DEG_TO_RAD);
        particles.getParticleController().setControlFlow(true);
        particles.getParticleController().setSpeed(0.4f);
        particles.setParticlesInWorldCoords(true);
        particles.setTranslation(0, 5.5f, -2);
        //particles.setScale(0.4);
        particles.setStartSize(0.4);
        particles.setEndSize(0.4);

        config.configure(particles);

        particles.warmUp(60);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        blend.setDestinationFunction(BlendState.DestinationFunction.One);
        particles.setRenderState(blend);

        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("data/textures/flaresmall.jpg", Texture.MinificationFilter.Trilinear,
                TextureStoreFormat.GuessCompressedFormat, true));
        ts.setEnabled(true);
        particles.setRenderState(ts);

        final ZBufferState zstate = new ZBufferState();
        zstate.setWritable(false);
        particles.setRenderState(zstate);

        particles.getParticleGeometry().setModelBound(new BoundingBox());

        particles.getSceneHints().setAllPickingHints(false);

        return particles;
    }
    
    private static final Model[] BOREAL_TREES = new Model[] {
//        normal_green, normal_green, normal_green, normal_green, normal_green, normal_green, normal_green, normal_green,
//        normal_green2, normal_green2, normal_green2, normal_green2, normal_green2, normal_green2, normal_green2, normal_green2,
//        normal_yellow, normal_yellow,
//        normal_red,
//        oldoak, oldoak, oldoak, oldoak, oldoak, oldoak, oldoak, oldoak,
//        oldoak2, oldoak2, oldoak2,
        fir, fir, fir, fir, fir, fir, fir,
        birch, birch, birch, birch, birch, birch, birch, birch, birch,
        birch2, birch2, birch2, birch2,
        fern, fern, fern, fern, fern, fern, fern, fern,
        redOak,
        bush, bush,
        deadTree,
        cypress, cypress, cypress, cypress, cypress, cypress, cypress, cypress, cypress, cypress, cypress, cypress, cypress
    };
    private String namePrefix;

    public static Model[] getBorealTrees() {
        return BOREAL_TREES;
    }

    private static final Model[] ALPINE_TREES = new Model[] {
        fir, fir, fir, fir, fir, fir, fir, fir, fir, fir, fir, fir, fir, fir,
//        bigfir, bigfir, bigfir, bigfir, bigfir, bigfir, bigfir, bigfir, bigfir, bigfir, bigfir, bigfir,
        birch, birch, birch, birch,
        bush, bush,
        deadTree,deadTree,deadTree,
    };
    public static Model[] getAlpineTrees() {
        return ALPINE_TREES;
    }

    private static final Model[] TEMPERATE_TREES = new Model[] {
//        oldoak, oldoak, oldoak, oldoak, oldoak, oldoak, oldoak, oldoak, oldoak, oldoak,
//        oldoak2, oldoak2, oldoak2,
//        normal_green, normal_green, normal_green, normal_green, normal_green, normal_green, normal_green, normal_green,
//        normal_green2, normal_green2, normal_green2, normal_green2, normal_green2, normal_green2, normal_green2, normal_green2,
//        normal_yellow, normal_yellow,
//        normal_red,
        oak, oak, oak, oak, oak, oak, oak, oak, oak, oak, oak, oak, oak, oak, oak, oak, oak,
        fir, fir, fir, fir, fir, fir, fir,
        willow, willow,
        birch, birch, birch, birch, birch, birch, birch, birch, birch,
        birch2, birch2, birch2, birch2,
        redOak,
        bigOak, bigOak, bigOak, bigOak, bigOak, bigOak, bigOak, bigOak,
        bush, bush, bush, bush, bush, bush, bush, bush,
        deadTree,
        cypress, cypress, cypress, cypress, cypress, cypress, cypress, cypress, cypress, cypress, cypress, cypress, cypress,
        fern, fern, fern, fern, fern, fern, fern
    };
    public static Model[] getTemperateTrees() {
        return TEMPERATE_TREES;
    }


    private static final Model[] SUBTROPICAL_TREES = new Model[] {
        deadTree,
        bush, bush, bush, bush, bush, bush, bush, bush,
        palm1,palm1,palm1,palm1,palm1,palm1,
        palm2,palm2,palm2,palm2,palm2,palm2,palm2,palm2,
        bigpalm,bigpalm,bigpalm,bigpalm,bigpalm,bigpalm,bigpalm,bigpalm,bigpalm,
        cactus,cactus,cactus,cactus,cactus,cactus,cactus,cactus,cactus,cactus,cactus,
        bushtree, bushtree, bushtree, bushtree, bushtree, bushtree, bushtree, bushtree,  
    };
    public static Model[] getSubtropicalTrees() {
        return SUBTROPICAL_TREES;
    }

    private static final Model[] TROPICAL_TREES = new Model[] {
        bush, bush, bush,
        palm1,palm1,palm1,palm1,palm1,palm1,
        palm2,palm2,palm2,palm2,palm2,palm2,palm2,palm2,
        bigpalm,bigpalm,bigpalm,bigpalm,bigpalm,bigpalm,bigpalm,bigpalm,bigpalm,
    };

    private static Map<String, SpatialController> controllers = new HashMap<String, SpatialController>();

    public static Model[] getTropicalTrees() {
        return TROPICAL_TREES;
    }

    private boolean ignoreHeightMap;
    private String modelPath;
    private String texturePath;

    Model(String modelPath) {
        this(modelPath, "./data/textures");
    }

    Model(String modelPath, String texturePath) {
        this(modelPath, texturePath, false);
    }

    Model(String modelPath, boolean ignoreHeightMap) {
        this(modelPath, "./data/textures", ignoreHeightMap);
    }

    Model(String modelPath, String texturePath, boolean ignoreHeightMap) {
        this.modelPath = modelPath;
        this.texturePath = texturePath;
        this.ignoreHeightMap = ignoreHeightMap;
    }

    public boolean isIgnoreHeightMap() {
        return ignoreHeightMap;
    }

    public String getModelPath() {
        return modelPath;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public abstract Spatial createSpatial();

	protected Spatial getNoAlphaSpatial() {
        return ShapeUtil.importModel(getModelPath(), getTexturePath(), namePrefix == null ? name() : namePrefix, this, 0, 0, 0);
    }

    protected Spatial getAlphaSpatial(float scale) {
        return getAlphaSpatial(scale, -90.0f, 0, 0);
    }

    protected Spatial getAlphaSpatial(float scale, float rotateX, float rotateY, float rotateZ) {
        Spatial spatial = ShapeUtil.importModel(
                getModelPath(), getTexturePath(), name(), this,
                rotateX, rotateY, rotateZ,
                getControllerClass());
        spatial.setScale(scale);

        BlendState as = new BlendState();
        as.setBlendEnabled(true);
        as.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        as.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        as.setReference(0);
        as.setTestEnabled(true);
        as.setTestFunction(BlendState.TestFunction.NotEqualTo);
        as.setEnabled(true);
        spatial.setRenderState(as);

//        spatial.setModelBound(new BoundingBox());
//        spatial.updateModelBound();
        return spatial;

    }

    protected Class getControllerClass() {
        return null;
    }

    public ReadOnlyVector3 getRotationVector() {
        return Vector3.UNIT_Y;
    }

    public void onLoad(Spatial spatial) {
        // implement to do post-load processing on model
    }

    protected void assignTextures(Spatial spatial, Map<String, String> textures) {
        Node node = (Node)spatial;
        for(String name : textures.keySet()) {
            TextureState ts = new TextureState();
            ts.setEnabled(true);
            Texture t = ShapeUtil.loadTexture(textures.get(name));
            t.setWrap(Texture.WrapMode.Repeat);
            t.setHasBorder(false);
            t.setApply(Texture.ApplyMode.Modulate);
            //ts.setTexture(t);
            ts.setTexture(t, 0);
            Spatial child = findChild(node, name);
            if(child != null) {
                child.setRenderState(ts);
            } else {
                Logger.getLogger(getClass().toString()).severe("Can't find node child named " + name + " in model " + name());
//                ShapeUtil.debugNode(node, "");
            }
        }
//        node.updateRenderState();
    }

    private static Spatial findChild(Node node, String name) {
        for(Spatial spatial : node.getChildren()) {
            if(spatial.getName().startsWith(name)) {
                return spatial;
            } else if(spatial instanceof Node) {
                Spatial s = findChild((Node)spatial, name);
                if(s != null) return s;
            }
        }
        return null;
    }

    protected void assignDungeonTextures(Spatial spatial, String topTexturePath, String wallTexturePath) {
        Map<String, String> textures = new HashMap<String, String>();

        if(topTexturePath != null) {
            // top
            textures.put("block##0", topTexturePath);
        }

        // walls
        textures.put("wall##0", wallTexturePath);
        assignTextures(spatial, textures);
    }

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    @Override
    public void write(OutputCapsule out) throws IOException {
        throw new RuntimeException("not used");
    }
    @Override
    public void read(InputCapsule in) throws IOException {
        throw new RuntimeException("not used");
    }
    @Override
    public Class getClassTag() {
        throw new RuntimeException("not used");
    }

    interface FlameTypeConfig {
        public void configure(ParticleSystem particles);
    }
}
