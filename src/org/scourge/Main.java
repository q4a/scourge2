package org.scourge;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.extension.effect.water.WaterNode;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.Texture;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.MouseState;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.math.*;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.pass.BasicPassManager;
import com.ardor3d.renderer.pass.RenderPass;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.FogState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.extension.CameraNode;
import com.ardor3d.scenegraph.extension.Skybox;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.*;
import com.ardor3d.util.Timer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import org.scourge.editor.MapSymbol;
import org.scourge.io.BlockData;
import org.scourge.model.Creature;
import org.scourge.terrain.Model;
import org.scourge.terrain.Region;
import org.scourge.terrain.Terrain;
import org.scourge.terrain.Tile;
import org.scourge.ui.MiniMap;
import org.scourge.ui.component.DragSource;
import org.scourge.ui.component.Dragable;
import org.scourge.ui.component.WinUtil;
import org.scourge.ui.component.Window;
import org.scourge.util.ShapeUtil;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.*;

public class Main extends ExampleBase implements Scourge {
    private Random random = new Random(17L);
    private Terrain terrain;
    private final float farPlane = 3500.0f;
    private final float heightOffset = 3.0f;
    private WaterNode waterNode;
    private Quad waterQuad;
    private Skybox skybox;
    private final double textureScale = 0.05;
    private Creature player;
    private Node cameraHolder;
    private CameraNode camNode;
    private boolean fogOnWater;
    private boolean inDungeon;
    private FogState fogState;
    private GameState gameState;
    private BasicText loadingLabel;
    private PlayerControl playerControl;

    // drag-n-drop
    private Dragable dragging;
    private DragSource dragSource;
    private Quad draggingIcon;
    private int dragOffsetX, dragOffsetY;
    private Selection dropSelection, dragSelection;
    private Map<String, Dragable> dragables = new HashMap<String, Dragable>();
    private Set<Dragable> firsts = new HashSet<Dragable>();

    // minimap
    private MiniMap miniMap;
    private RenderPass mapPass;

    private static Main main;
    private BasicPassManager _passManager;
    private MouseState mouse;
    private boolean skyboxEnabled = true;
    private boolean updateRoof;
    private boolean inUpDown;
	public static boolean SKIP_MENU;

	public Main() {
        main = this;
    }

    public static Main getMain() {
        return main;
    }

    /**
     * The main method.
     *
     * @param args
     *            the arguments
     */
    public static void main(final String[] args) {
		for(String arg : args) {
			if("--skip-menu".equals(arg)) SKIP_MENU = true;
		}
        start(Main.class);
    }

    private double counter = 0;
    private int frames = 0;

    /**
     * Update the PassManager, skybox, camera position, etc.
     *
     * @param timer
     *            the application timer
     */
    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        if(updating) {
        final Camera camera = _canvas.getCanvasRenderer().getCamera();

        _passManager.updatePasses(timer.getTimePerFrame());

        if(updateRoof) {
            updateRoof = false;
            terrain.setRoofVisible(!inDungeon);
            updateFog();
        }
        terrain.update(timer.getTimePerFrame());

        ShapeUtil.updateControllers(timer.getTimePerFrame());

        // the world vectors aren't computed until the first update :-/
        if(terrain.getCurrentRegion() != null) {
            terrain.getCurrentRegion().moveToTopOfTerrain();

            if(player != null) {
                player.getCreatureModel().moveToTopOfTerrain(_timer.getTimePerFrame());
            }
        }

        // so lame... this can't be done until the bounding box is calculated
        for(Dragable d : firsts) {
            d.scaleModel();
            Terrain.moveOnTopOfTerrain(d.getModel());
        }
        firsts.clear();

        if(draggingIcon != null) {
            draggingIcon.setTranslation(mouse.getX() + dragOffsetX,
                                        mouse.getY() - dragOffsetY,
                                        0);
        }

        if(skyboxEnabled) {
            skybox.setTranslation(camera.getLocation());
        }

        counter += timer.getTimePerFrame();
        frames++;
        if (counter > 1) {
            final double fps = (frames / counter);
            counter = 0;
            frames = 0;
            System.out.printf("%7.1f FPS\n", fps);
        }

        final Vector3 transVec = new Vector3(camera.getLocation().getX(), waterNode.getWaterHeight(), camera.getLocation().getZ());

        setTextureCoords(0, transVec.getX(), -transVec.getZ(), textureScale);

        // vertex coords
        setVertexCoords(transVec.getX(), transVec.getY(), transVec.getZ());

        waterNode.update(timer.getTimePerFrame());
        } else {
            terrain.update(timer.getTimePerFrame());
        }
    }

    @Override
    protected void renderExample(final Renderer renderer) {
        _passManager.renderPasses(renderer);
    }

    /**
     * Initialize pssm pass and scene.
     * Testing hg.
     */
    @Override
    protected void initExample() {
        try {
            // todo: needs to be dynamic
            String basePath = ".";
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE,
                                                   new FileResourceLocator(basePath));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE,
                                                   new FileResourceLocator(basePath + "/data/textures"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MODEL,
                                                   new FileResourceLocator(basePath));
        } catch(Throwable exc) {
            System.err.println("Error starting: " + exc);
            exc.printStackTrace();
            exit();
        }

        // Setup main camera.
        _canvas.setTitle("Scourge II");
        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 100, 0));
        _canvas.getCanvasRenderer().getCamera().lookAt(new Vector3(0, 100, 1), Vector3.UNIT_Y);
//        _canvas.getCanvasRenderer().getCamera().setFrustumPerspective(65.0, (float) _canvas.getCanvasRenderer().getCamera().getWidth()/ _canvas.getCanvasRenderer().getCamera().getHeight(), 1.0f, farPlane);
        _canvas.getCanvasRenderer().getCamera().setFrustumPerspective(30.0f, getScreenWidth() / getScreenHeight(), 1, 1000);
        _canvas.getCanvasRenderer().getCamera().update();

//        _controlHandle.setMoveSpeed(50);

        _lightState.detachAll();
        final DirectionalLight dLight = new DirectionalLight();
        dLight.setEnabled(true);
        dLight.setAmbient(new ColorRGBA(0.4f, 0.4f, 0.5f, 1));
        dLight.setDiffuse(new ColorRGBA(0.6f, 0.6f, 0.5f, 1));
        dLight.setSpecular(new ColorRGBA(0.3f, 0.3f, 0.2f, 1));
        dLight.setDirection(new Vector3(-1, -1, -1).normalizeLocal());
        _lightState.attach(dLight);
        _lightState.setEnabled(true);

        final CullState cs = new CullState();
        cs.setEnabled(true);
        cs.setCullFace(CullState.Face.Back);
//        _root.setRenderState(cs);

        fogState = new FogState();
        fogState.setDensity(1.0f);
        fogState.setEnabled(true);
        fogState.setDensityFunction(FogState.DensityFunction.Linear);
        fogState.setQuality(FogState.Quality.PerVertex);
        _root.setRenderState(fogState);

        loadingLabel = BasicText.createDefaultTextLabel("loading", "Loading...", 16);
		loadingLabel.setTextColor(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
        loadingLabel.setTranslation((getScreenWidth() - loadingLabel.getWidth()) / 2,
                                    (getScreenHeight() - loadingLabel.getHeight()) / 2,
                                    0);
        loadingLabel.setVisible(false);
        _root.attachChild(loadingLabel);

        miniMap = new MiniMap();

        gameState = new GameState();
        try {
            terrain = new Terrain(this);
        } catch(IOException exc) {
            throw new RuntimeException(exc);
        }

        cameraHolder = new Node("cam_holder");

        playerControl = new PlayerControl(this);
        playerControl.setupTriggers();

        dropSelection = new Selection(_root, this);
        dragSelection = new Selection(_root, this);

        final Node reflectedNode = new Node("reflectNode");
        reflectedNode.attachChild(terrain.getNode());
        skybox = buildSkyBox();
        // fixme: Adding the skybox causes passnode to stop working.
        // It's ok the sky is not visible anyway, but it's still reflected in the water.
        reflectedNode.attachChild(skybox);

        final Camera camera = _canvas.getCanvasRenderer().getCamera();

        // Create a new WaterNode with refraction enabled.
        waterNode = new WaterNode(camera, 2, false, true);
        // Setup textures to use for the water.
        waterNode.setNormalMapTextureString("data/textures/water/normalmap3.dds");
        waterNode.setDudvMapTextureString("data/textures/water/dudvmap.png");
        waterNode.setFallbackMapTextureString("data/textures/water/water2.png");
        waterNode.useFadeToFogColor(true);

        waterNode.setSpeedReflection(0.02);
        waterNode.setSpeedReflection(-0.01);

        // setting to default value just to show
        waterNode.setWaterPlane(new Plane(new Vector3(0.0, 1.0, 0.0), 0.0));

        // Create a quad to use as geometry for the water.
        waterQuad = new Quad("waterQuad", 1, 1);
        // Hack the quad normals to point up in the y-axis. Since we are manipulating the vertices as
        // we move this is more convenient than rotating the quad.
        final FloatBuffer normBuf = waterQuad.getMeshData().getNormalBuffer();
        normBuf.clear();
        normBuf.put(0).put(1).put(0);
        normBuf.put(0).put(1).put(0);
        normBuf.put(0).put(1).put(0);
        normBuf.put(0).put(1).put(0);
        waterNode.attachChild(waterQuad);

        waterNode.addReflectedScene(reflectedNode);
        waterNode.setSkybox(skybox);

        _root.attachChild(reflectedNode);
        _root.attachChild(waterNode);

        _passManager = new BasicPassManager();

        RenderPass rootPass = new RenderPass();
        rootPass.add(_root);
        _passManager.add(rootPass);

        mapPass = new RenderPass();
        mapPass.setEnabled(false);
        mapPass.add(miniMap.getNode());
        _passManager.add(mapPass);

        _root.getSceneHints().setCullHint(CullHint.Dynamic);
//        rootNode.setRenderQueueMode(Renderer.QUEUE_OPAQUE);

        setFogOnWater(true);

        if(!"true".equalsIgnoreCase(System.getProperty("test.mode"))) {
            gameState.showMainMenu();
        }
    }

    /**
     * Sets the vertex coords of the quad.
     *
     * @param x
     *            the x
     * @param y
     *            the y
     * @param z
     *            the z
     */
    private void setVertexCoords(final double x, final double y, final double z) {
        final FloatBuffer vertBuf = waterQuad.getMeshData().getVertexBuffer();
        vertBuf.clear();

        vertBuf.put((float) (x - farPlane)).put((float) y).put((float) (z - farPlane));
        vertBuf.put((float) (x - farPlane)).put((float) y).put((float) (z + farPlane));
        vertBuf.put((float) (x + farPlane)).put((float) y).put((float) (z + farPlane));
        vertBuf.put((float) (x + farPlane)).put((float) y).put((float) (z - farPlane));
    }

    /**
     * Sets the texture coords of the quad.
     *
     * @param buffer
     *            the buffer
     * @param x
     *            the x
     * @param y
     *            the y
     * @param textureScale
     *            the texture scale
     */
    private void setTextureCoords(final int buffer, double x, double y, double textureScale) {
        x *= textureScale * 0.5f;
        y *= textureScale * 0.5f;
        textureScale = farPlane * textureScale;
        FloatBuffer texBuf;
        texBuf = waterQuad.getMeshData().getTextureBuffer(buffer);
        texBuf.clear();
        texBuf.put((float) x).put((float) (textureScale + y));
        texBuf.put((float) x).put((float) y);
        texBuf.put((float) (textureScale + x)).put((float) y);
        texBuf.put((float) (textureScale + x)).put((float) (textureScale + y));
    }

    /**
     * Builds the sky box.
     */
    private Skybox buildSkyBox() {
        final Skybox skybox = new Skybox("skybox", 10, 10, 10);

        final String dir = "data/textures/skybox1/";
        final Texture north = TextureManager
                .load(dir + "1.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);
        final Texture south = TextureManager
                .load(dir + "3.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);
        final Texture east = TextureManager.load(dir + "2.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);
        final Texture west = TextureManager.load(dir + "4.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);
        final Texture up = TextureManager.load(dir + "6.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);
        final Texture down = TextureManager.load(dir + "5.jpg", Texture.MinificationFilter.BilinearNearestMipMap, true);

        skybox.setTexture(Skybox.Face.North, north);
        skybox.setTexture(Skybox.Face.West, west);
        skybox.setTexture(Skybox.Face.South, south);
        skybox.setTexture(Skybox.Face.East, east);
        skybox.setTexture(Skybox.Face.Up, up);
        skybox.setTexture(Skybox.Face.Down, down);

        return skybox;
    }

    private final Vector3 heightCalc = new Vector3();

    private float getHeightAt(final double x, final double z) {
//        heightCalc.set(x, 0, z);
//        terrain.worldToLocal(heightCalc, heightCalc);
//        return terrain.getClipmaps().get(0).getCache().getSubHeight(heightCalc.getXf(), heightCalc.getZf());
        return 0;
    }

    public Random getRandom() {
        return random;
    }

    public org.scourge.terrain.Terrain getTerrain() {
        return terrain;
    }

    public DisplaySettings getDisplaySettings() {
        return _settings;
    }

    public int getScreenWidth() {
        return getDisplaySettings().getWidth();
    }

    public int getScreenHeight() {
        return getDisplaySettings().getHeight();
    }
    
    /**
     * The ui released the dragged object outside of a window.
     * @param dragging the dragged object
     * @return true if the drop succeeded, false otherwise
     */
    public boolean drop(Dragable dragging) {
        if(dropSelection.testUnderMouse()) {
            Spatial model = dragging.getModel();
            dragables.put(model.getName(), dragging);
            Vector3 pos = dropSelection.getLocation();
            if(pos != null) {
                Vector3 v = new Vector3(pos);
                v.setY(9);
                model.setTranslation(v);
                model.updateWorldBound(true);
                model.updateGeometricState(0, true);
                _root.attachChild(model);
                firsts.add(dragging);
                return true;
            }
        }
        return false;
    }

    /**
     * A drag is started on the map.
     * @return true if the drag is started, false is there is nothing to drag
     */
    public boolean drag() {
        if(dragSelection.testUnderMouse()) {
            Spatial spatial = dragSelection.getSpatial();
            while(spatial.getParent() != null) {

                // check for items
                Dragable dragable = dragables.remove(spatial.getName());
                if(dragable != null) {
                    _root.detachChild(spatial);
                    setDragging(dragable, new DragSource() {
                        @Override
                        public void returnDragable(Dragable dragable) {
                            // it still has the world position
                            _root.attachChild(dragable.getModel());
                        }
                    });
                    return true;
                }
                spatial = spatial.getParent();
            }
        }

        return false;
    }

    public boolean mouseReleased() {
        Spatial spatial = findInteractiveSpatialClicked();
        if(spatial != null) {
            Model model = (Model)getUserData(spatial, Tile.MODEL);
            BlockData blockData = (BlockData)getUserData(spatial, Tile.BLOCK_DATA);
            if(model == Model.sign) {
                Window.showMessage(blockData.getData().get(MapSymbol.sign.getBlockDataKeys()[0]),
                                   blockData.getData().get(MapSymbol.sign.getBlockDataKeys()[1]));
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({"unchecked"})
    public static Object getUserData(Spatial spatial, String key) {
        Map<String, Object> map = (Map<String, Object>)spatial.getUserData();
        return map == null ? null : map.get(key);
    }

    private Spatial findInteractiveSpatialClicked() {
        if(dragSelection.testUnderMouse()) {
            Spatial spatial = dragSelection.getSpatial();
            while(spatial.getParent() != null) {
                if(getUserData(spatial, Tile.MODEL) != null && getUserData(spatial, Tile.BLOCK_DATA) != null) {
                    return spatial;
                }
                spatial = spatial.getParent();
            }
        }
        return null;
    }    

    public void returnDragable() {
        dragSource.returnDragable(dragging);
        setDragging(null, null);
    }

    public void setDragging(Dragable dragging, DragSource dragSource) {
        this.dragging = dragging;
        this.dragSource = dragSource;
        if(draggingIcon != null) {
            _root.detachChild(draggingIcon);
            draggingIcon = null;
        }
        if(dragging != null) {
            draggingIcon = WinUtil.createQuad("dragging", dragging.getIconWidth(), dragging.getIconHeight(), dragging.getIconTexture());
            WinUtil.makeNodeOrtho(draggingIcon);
//            draggingIcon.setZOrder(-5000);
            dragOffsetX = dragging.getIconWidth() / 2;
            dragOffsetY = dragging.getIconHeight() / 2;
            _root.attachChild(draggingIcon);
        }
        _root.updateGeometricState(0, true);
    }

    public Dragable getDragging() {
        return dragging;
    }

    public Quad getDraggingIcon() {
        return draggingIcon;
    }

    public void showWindow(Window win) {
        _root.attachChildAt(win.getNode(), 0);
    }

    public void hideWindow(Window win) {
        _root.detachChild(win.getNode());
    }

    public Creature getPlayer() {
        return player;
    }

    public void setPlayer(Creature newPlayer) {
        if(player != null) {
            player.getCreatureModel().getNode().detachChild(cameraHolder);
            _root.detachChild(player.getCreatureModel().getNode());
        }
        player = newPlayer;
        player.getCreatureModel().getNode().attachChild(cameraHolder);

        // todo: this is not the right place for this
        Spatial sp = player.getCreatureModel().getDebugNode();
        if(sp != null) {
            _root.attachChild(sp);
        }
        _root.attachChild(player.getCreatureModel().getNode());
        player.getCreatureModel().getNode().updateWorldBound(true);
//        player.getCreatureModel().getNode().updateRenderState();
    }

    public void setCameraFollowsPlayer(boolean follows) {
        if(follows) {
            if(camNode == null) {
                toggleCameraAttached();
            }
            playerControl.setEnabled(true);
//            input = playerController;
        } else {
            if(camNode != null) {
                toggleCameraAttached();
            }
            playerControl.setEnabled(false);
//            input = null;
        }
    }

    public void toggleCameraAttached() {
        if(camNode != null) {
            skyboxEnabled = true;
            cameraHolder.detachChild(camNode);
            camNode.setCamera(null);
            camNode = null;
        } else {
            skyboxEnabled = false;
            Camera camera = _canvas.getCanvasRenderer().getCamera();
            camNode = new CameraNode("camera node", camera);
            camNode.setTranslation(new Vector3(-90, 85, 0));
            //            camNode.setTranslation(new Vector3(-380, 350, 0));
            Quaternion q = new Quaternion().fromAngleAxis(MathUtils.DEG_TO_RAD * 90.0f, Vector3.UNIT_Y);
            q.multiplyLocal(new Quaternion().fromAngleAxis(MathUtils.DEG_TO_RAD * 35.0f, Vector3.UNIT_X));
            camNode.setRotation(q);
            cameraHolder.attachChild(camNode);
        }
    }

    public Node getCameraHolder() {
        return cameraHolder;
    }

    public Camera getCamera() {
        return _canvas.getCanvasRenderer().getCamera();
    }

    public CameraNode getCameraNode() {
        return camNode;
    }

    public void setFogOnWater(boolean b) {
        fogOnWater = b;
        updateFog();
    }

    public void updateFog() {
        waterNode.useFadeToFogColor(fogOnWater);
        Camera camera = _canvas.getCanvasRenderer().getCamera();
        if(fogOnWater) {
            if(inDungeon) {
                fogState.setColor(ColorRGBA.BLACK);
                fogState.setEnd((float)camera.getFrustumFar() * 0.16f);
                fogState.setStart((float)camera.getFrustumFar() * 0.02f);
            } else {
                fogState.setColor(new ColorRGBA(0.65f, 0.65f, 0.75f, 1.0f));
                fogState.setEnd((float)camera.getFrustumFar() * 0.25f);
                fogState.setStart((float)camera.getFrustumFar() * 0.175f);
            }
        } else {
            fogState.setColor(new ColorRGBA(0.65f, 0.65f, 0.75f, 1.0f));
            fogState.setEnd((float)camera.getFrustumFar() * 0.2f);
            fogState.setStart((float)camera.getFrustumFar() * 0.15f);
        }
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setMiniMapVisible(boolean b) {
        mapPass.setEnabled(b);
    }

    public MiniMap getMiniMap() {
        return miniMap;
    }

    private int loadingCounter;
    public void setLoading(boolean loading) {
        if(loading) {
            loadingCounter++;
            if(!loadingLabel.isVisible()) {
                loadingLabel.setVisible(true);
                loadingLabel.updateWorldRenderStates(false);
            }
            updating = false;
        } else {
            loadingCounter--;
            if(loadingCounter == 0) {
                loadingLabel.setVisible(false);
                updating = true;
            }
        }
    }

    public boolean isLoading() {
        return loadingCounter > 0;
    }

    protected void escapePressed() {
        if(gameState.escapePressed()) exit();
    }

    public GameTaskQueueManager getGameTaskQueueManager() {
        return GameTaskQueueManager.getManager(_canvas.getCanvasRenderer().getRenderContext());
    }

    public LogicalLayer getLogicalLayer() {
        return _logicalLayer;
    }

    public void mouseMoved(MouseState mouse) {
        this.mouse = mouse;
    }

    public void setMouseGrabbed(boolean grabbed) {
        if (_mouseManager.isSetGrabbedSupported()) {
            _mouseManager.setGrabbed(grabbed ? GrabbedState.GRABBED : GrabbedState.NOT_GRABBED);
        }
    }

    public PlayerControl getPlayerControl() {
        return playerControl;
    }

    public Canvas getCanvas() {
        return _canvas;
    }

    public Timer getTimer() {
        return _timer;
    }
    
    public void checkRoof() {
        Tile tile = player.getTile();
        if(tile != null) {
            boolean inDungeon = tile.getClimate().isDungeon();
            if(inDungeon != this.inDungeon) {
                this.inDungeon = inDungeon;
                updateRoof();
            }
            boolean inUpDown = (tile.getC() == MapSymbol.up.getC() || tile.getC() == MapSymbol.down.getC());
            if(inUpDown != this.inUpDown) {
                System.err.println("teleporting...");
                this.inUpDown = inUpDown;

                // teleport to the location
                BlockData blockData = tile.getBlockData();
                System.err.println("\tblockData=" + blockData);
                if(blockData != null) {
                    String location = blockData.getData().get(tile.getC() == MapSymbol.up.getC() ? MapSymbol.up.getBlockDataKeys()[0] : MapSymbol.down.getBlockDataKeys()[0]);
                    System.err.println("\tlocation=" + location);
                    try {
                        String[] s = location.trim().split(",");
                        getPlayer().getCreatureModel().moveTo(new Vector3(Float.parseFloat(s[0]) + Region.EDGE_BUFFER, 1f, Float.parseFloat(s[1]) + Region.EDGE_BUFFER));
                        getTerrain().teleport();
                    } catch(RuntimeException exc) {
                        exc.printStackTrace();
                    }
                }
                System.err.println("\tdone.");
            }
        }
    }
    
    public void updateRoof() {
        updateRoof = true;
    }

	public Node getRoot() {
		return _root;
	}
}
