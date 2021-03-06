package org.scourge.terrain;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.scenegraph.visitor.Visitor;
import org.scourge.Main;
import org.scourge.editor.MapSymbol;
import org.scourge.io.BlockData;
import org.scourge.io.MapIO;
import org.scourge.io.RegionData;
import org.scourge.util.NodeUtil;
import org.scourge.util.ShapeUtil;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: gabor
 * Date: Feb 17, 2010
 * Time: 10:05:51 AM
 */
public class Region implements NodeGenerator {
    private Terrain terrain;
    private Node region;
    public static final int REGION_SIZE = 20;
    private Tile[][] tiles;
    private int x, y, rows, cols, regionX, regionY;
    private List<House> houses = new ArrayList<House>();
    public static final float MIN_HEIGHT = 2;
    private static Logger logger = Logger.getLogger(Region.class.toString());
    public static final int EDGE_BUFFER = 2;
    private boolean first = true;
    private List<Generator> generators = new ArrayList<Generator>();
    private Vector3 savedTranslation = new Vector3();
	private static final Ray3 down = new Ray3();
	private static PrimitivePickResults noDistanceResults = new PrimitivePickResults();

	static {
		// point it down
        down.setDirection(new Vector3(0, -1, 0));
        noDistanceResults.setCheckDistance(false);
	}

    public Region(Terrain terrain, int x, int y) throws IOException {
        this.terrain = terrain;
        this.x = x;
        this.y = y;
        this.regionX = x / REGION_SIZE;
        this.regionY = y / REGION_SIZE;
        rows = cols = REGION_SIZE;
        load();
    }

    /**
     * When loading a region, an extra buffer of EDGE_BUFFER tiles is loaded around the edges.
     * This is so the edge detection algorithms draw correctly for the section that is displayed.
     * @throws IOException error loading map
     */
    public void load() throws IOException {
        logger.fine("----------------------------------------------------------------");
        logger.fine("Loading region: " + x + "," + y);
        long start = System.currentTimeMillis();
        long firstStart = start;
        this.region = new Node("region_" + regionX + "_" + regionY);
        // this.region.clearRenderState(RenderState.StateType.Texture);
//        region.setModelBound(new BoundingBox());

        start = System.currentTimeMillis();
        MapIO.RegionPoint[][] region = terrain.getMapIO().readRegion(x - EDGE_BUFFER, y - EDGE_BUFFER, rows + EDGE_BUFFER * 2, cols + EDGE_BUFFER * 2);
        RegionData regionData = null;
        try {
            regionData = MapIO.loadRegionData(regionX, regionY);
        } catch (Exception exc) {
            logger.log(Level.SEVERE, "Unable to load region data for coords=" + x + "," + y +
                                     " region=" + regionX + "," + regionY, exc);
        } finally {
            if(regionData == null) {
                regionData = new RegionData(regionX, regionY);
            }
        }
        logger.fine("Loaded data in " + (System.currentTimeMillis() - start) + " millis.");

        start = System.currentTimeMillis();
        tiles = new Tile[rows + EDGE_BUFFER * 2][cols + EDGE_BUFFER * 2];
        makeTiles(region, regionData);
        addDungeonModels();
        logger.fine("makeTiles in " + (System.currentTimeMillis() - start) + " millis.");

        // create some hills
        start = System.currentTimeMillis();
        createHeights();
        logger.fine("createHeights in " + (System.currentTimeMillis() - start) + " millis.");

        // create the shapes and textures
        start = System.currentTimeMillis();
        Map<Direction, TileTexType> around = new HashMap<Direction, TileTexType>();
        for(int x = 0; x < cols + EDGE_BUFFER * 2; x++) {
            for(int y = 0; y < rows + EDGE_BUFFER * 2; y++) {
                Tile tile = tiles[y][x];
                if(tile.isEmpty()) continue;

                Tile eastTile = x < cols + EDGE_BUFFER * 2 - 1 ? tiles[y][x + 1] : null;
                Tile westTile = x > 0 ? tiles[y][x - 1] : null;
                Tile southTile = y < rows + EDGE_BUFFER * 2 - 1 ? tiles[y + 1][x] : null;
                Tile northTile = y > 0 ? tiles[y - 1][x] : null;

                around.put(Direction.EAST, eastTile != null ? eastTile.tex : null);
                around.put(Direction.WEST, westTile != null ? westTile.tex : null);
                around.put(Direction.SOUTH, southTile != null ? southTile.tex : null);
                around.put(Direction.NORTH, northTile != null ? northTile.tex : null);

                tile.createNode(around, tile.getLevel(), tile.getClimate());
                Thread.yield();
            }
        }
        logger.fine("createNodes in " + (System.currentTimeMillis() - start) + " millis.");

        start = System.currentTimeMillis();
        for(int x = EDGE_BUFFER; x < cols + EDGE_BUFFER; x++) {
            for(int y = EDGE_BUFFER; y < rows + EDGE_BUFFER; y++) {
                Tile tile = tiles[y][x];
                if(tile.isEmpty()) continue;

                Node node = tile.getNode();
                node.setTranslation(x * ShapeUtil.WALL_WIDTH, MIN_HEIGHT + (tile.getLevel() * ShapeUtil.WALL_HEIGHT), y * ShapeUtil.WALL_WIDTH);
                this.region.attachChild(node);
                Thread.yield();
            }
        }
        logger.fine("addNodes in " + (System.currentTimeMillis() - start) + " millis.");

        // copy the NW normal of each quad into the adjacent quads
        start = System.currentTimeMillis();
        for(int x = 0; x < cols + EDGE_BUFFER * 2; x++) {
            for(int y = 0; y < rows + EDGE_BUFFER * 2; y++) {
                Tile tile = tiles[y][x];
                if(tile.type == TileType.QUAD) {
                    Tile westTile = x > 0 ? tiles[y][x - 1] : null;
                    Tile nwTile = x > 0 && y > 0 ? tiles[y - 1][x - 1] : null;
                    Tile northTile = y > 0 ? tiles[y - 1][x] : null;

                    copyNormal(tile, westTile, Tile.Edge.NE);
                    copyNormal(tile, westTile, Tile.Edge.SE);
                    copyNormal(tile, northTile, Tile.Edge.SE);
                    copyNormal(tile, northTile, Tile.Edge.SW);
                    copyNormal(tile, nwTile, Tile.Edge.SE);
                }
            }
        }
        logger.fine("copyNormals in " + (System.currentTimeMillis() - start) + " millis.");

        start = System.currentTimeMillis();
        for(int x = EDGE_BUFFER; x < cols + EDGE_BUFFER; x++) {
            for(int y = EDGE_BUFFER; y < rows + EDGE_BUFFER; y++) {
                Tile tile = tiles[y][x];
                if(tile.isEmpty()) continue;
                tile.attachModels();
                Thread.yield();
            }
        }
        logger.fine("attachModels in " + (System.currentTimeMillis() - start) + " millis.");


        // start the generators
        startGenerators();

        this.region.setTranslation(this.region.getTranslation().add(x * ShapeUtil.WALL_WIDTH, 0.0, y * ShapeUtil.WALL_WIDTH, null));

//        Main.getMain().updateRoof();

        if(logger.isLoggable(Level.FINE)) {
            ShapeUtil.debug();
            logger.fine("loaded region in " + (System.currentTimeMillis() - firstStart) + " millis.");
        }
    }

    private void startGenerators() {
        for(int x = EDGE_BUFFER; x < cols + EDGE_BUFFER; x++) {
            for(int y = EDGE_BUFFER; y < rows + EDGE_BUFFER; y++) {
                Tile tile = tiles[y][x];
                if(tile.getBlockData() != null) {
                    Generator generator = Generator.create(tile.getBlockData(), this, x, y);
                    if(generator != null) {
                        generators.add(generator);
                    }
                }
            }
        }
    }

    private void makeTiles(MapIO.RegionPoint[][] region, RegionData regionData) {
        List<Set<Vector2>> housePoints = new ArrayList<Set<Vector2>>();
        Set<Vector2> roadPos = new HashSet<Vector2>();
        Set<Vector2> cobblesPos = new HashSet<Vector2>();
        Set<Vector2> ladderPos = new HashSet<Vector2>();
        Set<Vector2> roomPos = new HashSet<Vector2>();
        Set<Vector2> roomPos2 = new HashSet<Vector2>();
        Set<Vector2> roomPos3 = new HashSet<Vector2>();
        Set<Vector2> roomPos4 = new HashSet<Vector2>();
        Set<Vector2> roomPos5 = new HashSet<Vector2>();

        // create tiles and handle empty tiles with models
        for(int y = 0; y < rows + EDGE_BUFFER * 2; y++) {
            for(int x = 0; x < cols + EDGE_BUFFER * 2; x++) {
                tiles[y][x] = new Tile(terrain.getScourge(),
                                       region[y][x].getC(),
                                       region[y][x].getClimate(),
                                       region[y][x].getLevel(),
                                       regionData.getBlock(regionX * REGION_SIZE + x - EDGE_BUFFER,
                                                           regionY * REGION_SIZE + y - EDGE_BUFFER));

                if(region[y][x].getC() == MapSymbol.bridge.getC()) {
                    if(check(y - 1, x, region[y][x].getLevel(), region) && check(y + 1, x, region[y][x].getLevel(), region)) {
                        tiles[y][x].addModel(Model.bridge, new Vector3(0, 0, 0), 1, 90, Vector3.UNIT_Y);
                    } else {
                        tiles[y][x].addModel(Model.bridge);
                    }
                    region[y][x].setC(MapSymbol.water.getC());
                } else if(region[y][x].getC() == MapSymbol.tree.getC()) {
                    makeForestTile(tiles[y][x]);
                    region[y][x].setC(MapSymbol.ground.getC());
                } else if(region[y][x].getC() == MapSymbol.house.getC()) {
                    storeHousePoisition(housePoints, x, y);
                    region[y][x].setC(MapSymbol.ground.getC());
                } else if(region[y][x].getC() == MapSymbol.road.getC()) {
                    roadPos.add(new Vector2(x, y));
                    region[y][x].setC(MapSymbol.ground.getC());
                } else if(region[y][x].getC() == MapSymbol.room.getC()) {
                    roomPos.add(new Vector2(x, y));
                    region[y][x].setC(MapSymbol.ground.getC());
                } else if(region[y][x].getC() == MapSymbol.room2.getC()) {
                    roomPos2.add(new Vector2(x, y));
                    region[y][x].setC(MapSymbol.ground.getC());
                } else if(region[y][x].getC() == MapSymbol.room3.getC()) {
                    roomPos3.add(new Vector2(x, y));
                    region[y][x].setC(MapSymbol.ground.getC());
                } else if(region[y][x].getC() == MapSymbol.room4.getC()) {
                    roomPos4.add(new Vector2(x, y));
                    region[y][x].setC(MapSymbol.ground.getC());
                } else if(region[y][x].getC() == MapSymbol.room5.getC()) {
                    roomPos5.add(new Vector2(x, y));
                    region[y][x].setC(MapSymbol.ground.getC());
                } else if(region[y][x].getC() == MapSymbol.paved_road.getC()) {
                    cobblesPos.add(new Vector2(x, y));
                    region[y][x].setC(MapSymbol.ground.getC());
                } else if(region[y][x].getC() == MapSymbol.ramp.getC()) {
                    ladderPos.add(new Vector2(x, y));
                    region[y][x].setC(MapSymbol.ground.getC());
                } else if(region[y][x].getC() == MapSymbol.sign.getC()) {
                    addSign(x, y, region, regionData);
                    region[y][x].setC(MapSymbol.ground.getC());
                }
            }
        }

        for(int x = 0; x < cols + EDGE_BUFFER * 2; x++) {
            for(int y = 0; y < rows + EDGE_BUFFER * 2; y++) {
                if(region[y][x].getC() != MapSymbol.water.getC()) {

                    int level = tiles[y][x].getLevel();

                    if(check(y - 1, x, level, region) && check(y, x - 1, level, region) && check(y, x + 1, level, region) && !check(y + 1, x, level, region)) {
                        setEdgeSide(x, y, 180, region);
                    } else if(!check(y - 1, x, level, region) && check(y, x - 1, level, region) && check(y, x + 1, level, region) && check(y + 1, x, level, region)) {
                        setEdgeSide(x, y, 0, region);
                    } else if(check(y - 1, x, level, region) && !check(y, x - 1, level, region) && check(y, x + 1, level, region) && check(y + 1, x, level, region)) {
                        setEdgeSide(x, y, 90, region);
                    } else if(check(y - 1, x, level, region) && check(y, x - 1, level, region) && !check(y, x + 1, level, region) && check(y + 1, x, level, region)) {
                        setEdgeSide(x, y, -90, region);

                    } else if(!check(y - 1, x, level, region) && !check(y, x - 1, level, region) && check(y, x + 1, level, region) && check(y + 1, x, level, region)) {
                        setEdge(x, y, 90, region, TileType.EDGE_CORNER);
                    } else if(check(y - 1, x, level, region) && check(y, x - 1, level, region) && !check(y, x + 1, level, region) && !check(y + 1, x, level, region)) {
                        setEdge(x, y, -90, region, TileType.EDGE_CORNER);
                    } else if(!check(y - 1, x, level, region) && check(y, x - 1, level, region) && !check(y, x + 1, level, region) && check(y + 1, x, level, region)) {
                        setEdge(x, y, 0, region, TileType.EDGE_CORNER);
                    } else if(check(y - 1, x, level, region) && !check(y, x - 1, level, region) && check(y, x + 1, level, region) && !check(y + 1, x, level, region)) {
                        setEdge(x, y, 180, region, TileType.EDGE_CORNER);

                    } else if(!check(y - 1, x, level, region) && !check(y, x - 1, level, region) && !check(y, x + 1, level, region) && check(y + 1, x, level, region)) {
                        setEdge(x, y, 0, region, TileType.EDGE_TIP);
                    } else if(check(y - 1, x, level, region) && !check(y, x - 1, level, region) && !check(y, x + 1, level, region) && !check(y + 1, x, level, region)) {
                        setEdge(x, y, 180, region, TileType.EDGE_TIP);
                    } else if(!check(y - 1, x, level, region) && check(y, x - 1, level, region) && !check(y, x + 1, level, region) && !check(y + 1, x, level, region)) {
                        setEdge(x, y, -90, region, TileType.EDGE_TIP);
                    } else if(!check(y - 1, x, level, region) && !check(y, x - 1, level, region) && check(y, x + 1, level, region) && !check(y + 1, x, level, region)) {
                        setEdge(x, y, 90, region, TileType.EDGE_TIP);

                    } else if(!check(y - 1, x, level, region) && check(y, x - 1, level, region) && check(y, x + 1, level, region) && !check(y + 1, x, level, region)) {
                        setEdge(x, y, 90, region, TileType.EDGE_BRIDGE);
                    } else if(check(y - 1, x, level, region) && !check(y, x - 1, level, region) && !check(y, x + 1, level, region) && check(y + 1, x, level, region)) {
                        setEdge(x, y, 0, region, TileType.EDGE_BRIDGE);

                    } else {
                        Vector2 point = new Vector2(x, y);
                        if(roadPos.contains(point)) {
                            tiles[y][x].set(TileTexType.ROAD, TileType.QUAD, 0);
                        } else if(cobblesPos.contains(point)) {
                            tiles[y][x].set(TileTexType.COBBLES, TileType.QUAD, 0);
                        } else if(roomPos.contains(point)) {
                            tiles[y][x].set(TileTexType.ROOM, TileType.QUAD, 0);
                        } else if(roomPos2.contains(point)) {
                            tiles[y][x].set(TileTexType.ROOM2, TileType.QUAD, 0);
                        } else if(roomPos3.contains(point)) {
                            tiles[y][x].set(TileTexType.ROOM3, TileType.QUAD, 0);
                        } else if(roomPos4.contains(point)) {
                            tiles[y][x].set(TileTexType.ROOM4, TileType.QUAD, 0);
                        } else if(roomPos5.contains(point)) {
                            tiles[y][x].set(TileTexType.ROOM5, TileType.QUAD, 0);
                        } else if(x <= EDGE_BUFFER || y <= EDGE_BUFFER ||
                                  x >= cols + EDGE_BUFFER - 1 || y >= rows + EDGE_BUFFER - 1) {
                            // this is so edges meet on the same type
                            tiles[y][x].set(region[y][x].getClimate().getDefaultGround(), TileType.QUAD, 0);
                        } else {
                            tiles[y][x].set(region[y][x].getClimate().getRandomGround(terrain.getScourge().getRandom()), TileType.QUAD, 0);
                        }
                    }
                }
            }
        }

        for(int x = 1; x < cols + EDGE_BUFFER; x++) {
            for(int y = 1; y < rows + EDGE_BUFFER; y++) {
                Tile tile = tiles[y][x];
                if(tile.isDungeonFloor()) {
                    Map<Direction, Boolean> around = new HashMap<Direction, Boolean>();
                    around.put(Direction.NORTH, !tiles[y - 1][x].isDungeonFloor());
                    around.put(Direction.SOUTH, !tiles[y + 1][x].isDungeonFloor());
                    around.put(Direction.EAST, !tiles[y][x + 1].isDungeonFloor());
                    around.put(Direction.WEST, !tiles[y][x - 1].isDungeonFloor());
                    tile.setDungeonFloor(around);
                }
            }
        }

        addHouses(housePoints);
        addLadders(ladderPos);
    }

    private void addSign(int x, int y, MapIO.RegionPoint[][] region, RegionData regionData) {
        BlockData blockData = regionData.getBlock(regionX * REGION_SIZE + x - EDGE_BUFFER,
                                                  regionY * REGION_SIZE + y - EDGE_BUFFER);
        tiles[y][x].addModel(Model.sign, new Vector3(ShapeUtil.WALL_WIDTH / 2, 0, ShapeUtil.WALL_WIDTH / 2), 2, 0, Vector3.UNIT_Z, blockData);
    }

    private void addDungeonModels() {
        for(int y = 0; y < rows + EDGE_BUFFER * 2; y++) {
            for(int x = 0; x < cols + EDGE_BUFFER * 2; x++) {
                if(checkDungeonFloor(x, y) && !(checkDungeonDoor(x - 1, y) || checkDungeonDoor(x + 1, y) || checkDungeonDoor(x, y - 1) || checkDungeonDoor(x, y + 1))) {
                    List<Direction> dir = new ArrayList<Direction>();
                    if(!checkDungeonFloor(x, y - 1) && !checkDungeonFloor(x - 1, y - 1) && !checkDungeonFloor(x + 1, y - 1)) {
                        dir.add(Direction.NORTH);
                    }
                    if(!checkDungeonFloor(x, y + 1) && !checkDungeonFloor(x - 1, y + 1) && !checkDungeonFloor(x + 1, y + 1)) {
                        dir.add(Direction.SOUTH);
                    }
                    if(!checkDungeonFloor(x - 1, y) && !checkDungeonFloor(x - 1, y - 1) && !checkDungeonFloor(x - 1, y + 1)) {
                        dir.add(Direction.WEST);
                    }
                    if(!checkDungeonFloor(x + 1, y) && !checkDungeonFloor(x + 1, y - 1) && !checkDungeonFloor(x + 1, y + 1)) {
                        dir.add(Direction.EAST);
                    }

                    tiles[y][x].getClimate().decorateWall(dir, this, x, y);
                }
            }
        }
    }

    private boolean checkDungeonDoor(int x, int y) {
        return x >= 0 && y >= 0 && x < cols + EDGE_BUFFER * 2 && y < rows + EDGE_BUFFER * 2 && tiles[y][x].isDungeonDoor();
    }

    private boolean checkDungeonFloor(int x, int y) {
        return x >= 0 && y >= 0 && x < cols + EDGE_BUFFER * 2 && y < rows + EDGE_BUFFER * 2 && tiles[y][x].isDungeonFloor();
    }

    private boolean checkForWater(int x, int y) {
        return x >= 0 && y >= 0 && x < cols + EDGE_BUFFER * 2 && y < rows + EDGE_BUFFER * 2 && tiles[y][x].isWater();
    }

    private void setEdgeSide(int x, int y, int angle, MapIO.RegionPoint[][] region) {
        if(region[y][x].getC() == MapSymbol.gate.getC()) {
            setEdge(x, y, angle, region, TileType.EDGE_GATE);
        } else if(region[y][x].getC() == MapSymbol.up.getC()) {
            setEdge(x, y, angle, region, TileType.EDGE_UP);
        } else if(region[y][x].getC() == MapSymbol.down.getC()) {
            setEdge(x, y, angle, region, TileType.EDGE_DOWN);
        } else {
            setEdge(x, y, angle, region, TileType.EDGE_SIDE);
        }
    }

    private void setEdge(int x, int y, int angle, MapIO.RegionPoint[][] region, TileType tileType) {
        tiles[y][x].set(region[y][x].getClimate().getBaseTileTex(), tileType, angle);
        // hack: do not draw quad at the base of walls next to the water.
        // This works as long as the wall next to water is straight (ie. no corners/tips).
        tiles[y][x].setNextToWater(checkForWater(x - 1, y) || checkForWater(x + 1, y) || checkForWater(x, y - 1) || checkForWater(x, y + 1));
    }

    private boolean check(int y, int x, int level, MapIO.RegionPoint[][] region) {
        return (y >= 0 && x >= 0 && y < REGION_SIZE + EDGE_BUFFER * 2 && x < REGION_SIZE + EDGE_BUFFER * 2 &&
                region[y][x].getC() != MapSymbol.water.getC() && region[y][x].getLevel() >= level);
    }

    private void addLadders(Set<Vector2> ladderPos) {
        for(Vector2 point : ladderPos) {
            int x = (int)point.getX();
            int y = (int)point.getY();
            
            Tile eastTile = x < cols + EDGE_BUFFER * 2 - 1 ? tiles[y][x + 1] : null;
            Tile westTile = x > 0 ? tiles[y][x - 1] : null;
            Tile southTile = y < rows + EDGE_BUFFER * 2 - 1 ? tiles[y + 1][x] : null;
            Tile northTile = y > 0 ? tiles[y - 1][x] : null;

            float rotation = 0.0f;
            Vector3 trans = new Vector3(0, 0, 0);
            if(eastTile != null && eastTile.getLevel() > tiles[y][x].getLevel()) {
                rotation = 180.0f;
                trans.setX(trans.getX() + 2);
            } else if(westTile != null && westTile.getLevel() > tiles[y][x].getLevel()) {
                rotation = 0.0f;
                trans.setX(trans.getX() + 14);
                trans.setZ(trans.getZ() + ShapeUtil.WALL_WIDTH);
            } else if(southTile != null && southTile.getLevel() > tiles[y][x].getLevel()) {
                rotation = 90.0f;
                trans.setX(trans.getX() + ShapeUtil.WALL_WIDTH);
                trans.setZ(trans.getZ() + 2);
            } else if(northTile != null && northTile.getLevel() > tiles[y][x].getLevel()) {
                rotation = 270.0f;
                trans.setZ(trans.getZ() + 14);
            }
            tiles[y][x].addModel(Model.ladder, trans, 1, rotation, Vector3.UNIT_Y);
        }
    }

    private void storeHousePoisition(List<Set<Vector2>> housePoints, int x, int y) {
        boolean found = false;
        for(Set<Vector2> housePoint : housePoints) {
            for(Vector2 point : housePoint) {
                if((point.getX() == x && (point.getY() == y - 1 || point.getY() == y + 1)) ||
                   (point.getY() == y && (point.getX() == x - 1 || point.getX() == x + 1))) {
                    housePoint.add(new Vector2(x, y));
                    found = true;
                    break;
                }
            }
            if(found) break;
        }
        if(!found) {
            Set<Vector2> set = new HashSet<Vector2>();
            set.add(new Vector2(x, y));
            housePoints.add(set);
        }
    }

    private void addHouses(List<Set<Vector2>> housePoints) {
        for(Set<Vector2> housePoint : housePoints) {
            double minx = 10000, miny = 10000, maxx = -1, maxy = -1;
            for(Vector2 point : housePoint) {
                if(point.getX() < minx) {
                    minx = point.getX();
                }
                if(point.getX() > maxx) {
                    maxx = point.getX();
                }
                if(point.getY() < miny) {
                    miny = point.getY();
                }
                if(point.getY() > maxy) {
                    maxy = point.getY();
                }
            }

            if(minx < EDGE_BUFFER || miny < EDGE_BUFFER || maxx >= cols + EDGE_BUFFER || maxy >= rows + EDGE_BUFFER) {
                continue;
            }

            int w = (int)(maxx - minx) + 1;
            int h = (int)(maxy - miny) + 1;
            float height = tiles[(int)miny][(int)minx].getLevel();

            House house = new House(Main.getMain(),
					minx + w / 2.0f - 0.5f,
					height,
					miny + h / 2.0f + 0.5f,
					w,
					h,
					(int)(Main.getMain().getRandom().nextDouble() * 2) + 1,
					Main.getMain().getRandom());
            houses.add(house);
            region.attachChild(house.getNode());
        }
    }

    private void makeForestTile(Tile tile) {
        Model model = tile.getClimate().getRandomTree(terrain.getScourge().getRandom());
        if(model != null) {
            tile.addModel(model,
                          new Vector3(8, 0, 8),
                          (terrain.getScourge().getRandom().nextFloat() * 0.3f) + 1.0f,
                          terrain.getScourge().getRandom().nextFloat() * 360.0f,
                          model.getRotationVector());
        }
    }

    // hack... this should be in TileType.QUAD but I was lazy
    private void copyNormal(Tile from, Tile to, Tile.Edge edge) {
        if(to == null || to.type != TileType.QUAD ||
           from == null || from.type != TileType.QUAD) return;

        Quad fromQuad = (Quad)from.ground.getChild(0);
        Quad toQuad = (Quad)to.ground.getChild(0);
        FloatBuffer fromBuf = fromQuad.getMeshData().getNormalBuffer();
        float[] normal = new float[3];
        // get the NW normal; the only one set explicitly
        normal[0] = fromBuf.get(0);
        normal[1] = fromBuf.get(1);
        normal[2] = fromBuf.get(2);

        FloatBuffer toBuf = toQuad.getMeshData().getNormalBuffer();
        switch(edge) {
            case NE: toBuf.put(9, normal[0]).put(10, normal[1]).put(11, normal[2]); break;
            case SE: toBuf.put(6, normal[0]).put(7, normal[1]).put(8, normal[2]); break;
            case SW: toBuf.put(3, normal[0]).put(4, normal[1]).put(5, normal[2]); break;
            default: throw new IllegalStateException("Can't set NW corner.");
        }
    }

    private void createHeights() {
        // set the heights
        for(int y = EDGE_BUFFER + 1; y < rows + EDGE_BUFFER - 1; y++) {
            for(int x = EDGE_BUFFER + 1; x < cols + EDGE_BUFFER - 1; x++) {
                float h = tiles[y][x].getClimate().isDungeon() ?
                          (0.5f + terrain.getScourge().getRandom().nextFloat() * 2.0f) :
                          (2.0f + terrain.getScourge().getRandom().nextFloat() * 8.0f);
                tiles[y - 1][x - 1].setHeight(Tile.Edge.SE, h);
                tiles[y - 1][x].setHeight(Tile.Edge.SW, h);
                tiles[y][x - 1].setHeight(Tile.Edge.NE, h);
                tiles[y][x].setHeight(Tile.Edge.NW, h);
            }
        }

        // clamp heights around the edges
        for(int x = 0; x < cols + EDGE_BUFFER * 2; x++) {
            for(int y = 0; y < rows + EDGE_BUFFER * 2; y++) {
                Tile tile = tiles[y][x];
                if(tile.type == TileType.NONE) continue;

                Tile eastTile = x < cols + EDGE_BUFFER * 2 - 1 ? tiles[y][x + 1] : null;
                Tile westTile = x > 0 ? tiles[y][x - 1] : null;
                Tile southTile = y < rows + EDGE_BUFFER * 2 - 1 ? tiles[y + 1][x] : null;
                Tile northTile = y > 0 ? tiles[y - 1][x] : null;

                boolean north = northTile != null && northTile.tex == tile.tex && tile.tex != tile.getClimate().getBaseTileTex() && tile.tex != TileTexType.ROCK;
                boolean south = southTile != null && southTile.tex == tile.tex && tile.tex != tile.getClimate().getBaseTileTex() && tile.tex != TileTexType.ROCK;
                boolean east = eastTile != null && eastTile.tex == tile.tex && tile.tex != tile.getClimate().getBaseTileTex() && tile.tex != TileTexType.ROCK;
                boolean west = westTile != null && westTile.tex == tile.tex && tile.tex != tile.getClimate().getBaseTileTex() && tile.tex != TileTexType.ROCK;

                if(!north) {
                    tile.setHeight(Tile.Edge.NW, 0);
                    if(westTile != null) westTile.setHeight(Tile.Edge.NE, 0);
                    tile.setHeight(Tile.Edge.NE, 0);
                    if(eastTile != null) eastTile.setHeight(Tile.Edge.NW, 0);
                }
                if(!west) {
                    tile.setHeight(Tile.Edge.NW, 0);
                    if(northTile != null) northTile.setHeight(Tile.Edge.SW, 0);
                    tile.setHeight(Tile.Edge.SW, 0);
                    if(southTile != null) southTile.setHeight(Tile.Edge.NW, 0);
                }
                if(!south) {
                    tile.setHeight(Tile.Edge.SW, 0);
                    if(westTile != null) westTile.setHeight(Tile.Edge.SE, 0);
                    tile.setHeight(Tile.Edge.SE, 0);
                    if(eastTile != null) eastTile.setHeight(Tile.Edge.SW, 0);
                }
                if(!east) {
                    tile.setHeight(Tile.Edge.SE, 0);
                    if(southTile != null) southTile.setHeight(Tile.Edge.NE, 0);
                    tile.setHeight(Tile.Edge.NE, 0);
                    if(northTile != null) northTile.setHeight(Tile.Edge.SE, 0);
                }
            }
        }
    }

    @Override
    public Node getNode() {
        return region;
    }

    public void flatten(Node node) {
        region.updateWorldBound(true);
//        region.updateWorldVectors();

        BoundingBox boundingBox = (BoundingBox)node.getWorldBound();
        double dx = boundingBox.getCenter().getX() - region.getWorldTranslation().getX();
        double dz = boundingBox.getCenter().getZ() - region.getWorldTranslation().getZ();

        int sx = (int)((dx - boundingBox.getXExtent() / 2) / ShapeUtil.WALL_WIDTH) + EDGE_BUFFER;
        int sy = (int)((dz - boundingBox.getZExtent() / 2) / ShapeUtil.WALL_WIDTH) + EDGE_BUFFER;
        int ex = (int)((dx + boundingBox.getXExtent() / 2) / ShapeUtil.WALL_WIDTH) + EDGE_BUFFER;
        int ey = (int)((dz + boundingBox.getZExtent() / 2) / ShapeUtil.WALL_WIDTH) + EDGE_BUFFER;

        for(int y = sy - 1; y <= ey + 1; y++) {
            for(int x = sx - 1; x <= ex + 1; x++) {
                flattenTile(x, y);
            }
        }
//        region.updateModelBound();
        region.updateWorldBound(true);
    }

    private void flattenTile(int tx, int ty) {
        if(tx >= 0 && ty >= 0 && tx < cols && ty < rows) {
            Tile tile = tiles[ty][tx];
            tile.clearModels();
            if(tile.type != TileType.NONE) {
                Tile eastTile = tx < cols + EDGE_BUFFER - 1 ? tiles[ty][tx + 1] : null;
                Tile westTile = tx > 0 ? tiles[ty][tx - 1] : null;
                Tile southTile = ty < rows + EDGE_BUFFER - 1 ? tiles[ty + 1][tx] : null;
                Tile northTile = ty > 0 ? tiles[ty - 1][tx] : null;
                Tile nwTile = ty > 0 && tx > 0 ? tiles[ty - 1][tx - 1] : null;
                Tile neTile = ty > 0 && tx < cols + EDGE_BUFFER - 1 ? tiles[ty - 1][tx + 1] : null;
                Tile seTile = ty < rows + EDGE_BUFFER - 1 && tx < cols + EDGE_BUFFER - 1 ? tiles[ty + 1][tx + 1] : null;
                Tile swTile = ty < rows + EDGE_BUFFER - 1 && tx > 0 ? tiles[ty + 1][tx - 1] : null;

                tile.setHeight(Tile.Edge.NW, 0);
                tile.setHeight(Tile.Edge.SW, 0);
                tile.setHeight(Tile.Edge.SE, 0);
                tile.setHeight(Tile.Edge.NE, 0);

                if(northTile != null) {
                    northTile.setHeight(Tile.Edge.SW, 0);
                    northTile.setHeight(Tile.Edge.SE, 0);
                }
                if(southTile != null) {
                    southTile.setHeight(Tile.Edge.NE, 0);
                    southTile.setHeight(Tile.Edge.NW, 0);
                }
                if(westTile != null) {
                    westTile.setHeight(Tile.Edge.NE, 0);
                    westTile.setHeight(Tile.Edge.SE, 0);
                }
                if(eastTile != null) {
                    eastTile.setHeight(Tile.Edge.NW, 0);
                    eastTile.setHeight(Tile.Edge.SW, 0);
                }
                if(nwTile != null) nwTile.setHeight(Tile.Edge.SE, 0);
                if(swTile != null) swTile.setHeight(Tile.Edge.NE, 0);
                if(neTile != null) neTile.setHeight(Tile.Edge.SW, 0);
                if(seTile != null) seTile.setHeight(Tile.Edge.NW, 0);
            }
        }
    }

    public void moveToTopOfTerrain() {
        if(first) {
            first = false;
            for(House house : houses) {
                flatten(house.getNode());
            }
            for(Generator generator : generators) {
                generator.generate();
            }
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

//    public String getRegionKey() {
//        return Terrain.getRegionKey(x / REGION_SIZE, y / REGION_SIZE);
//    }

    public Tile getTile(int x, int z) {
        return tiles[z][x];
    }

    public void setRoofVisible(boolean visible) {
        for(int x = 0; x < cols + EDGE_BUFFER * 2; x++) {
            for(int y = 0; y < rows + EDGE_BUFFER * 2; y++) {
                tiles[y][x].setRoofVisible(visible);
            }
        }
//        region.updateRenderState();
    }

	public void setHouseRoofVisible(boolean visible) {
        for(House house : houses) {
			System.err.println("house=" + house.toString() + " visible=" + visible);
			for(Spatial level : house.getNode().getChildren()) {
				final boolean hide = !visible && level.getTranslation().getY() > Main.getMain().getPlayer().getCreatureModel().getNode().getTranslation().getY();
				System.err.println("\tlevel=" + level.getName() + " y=" + level.getTranslation().getY() + " vs player=" + Main.getMain().getPlayer().getCreatureModel().getNode().getTranslation().getY() + " hide=" + hide);
				((ShowHideController)level.getController(0)).setIn(hide);
			}
		}
    }

    /**
     * Find space on this region for this node.
	 * Note: this method is slow.
	 * If the method returns true, the spatial was successfully moved. If false, it is returned to its original position.
	 *
     * @param x coordinates are within the region (0-REGION_SIZE)
     * @param y (0-REGION_SIZE)
     * @param spatial the spatial to use for positioning
     * @param attachee the spatial to attach to the terrain
	 * @return true if space was found, false if it could not fit
     */
    public boolean findSpaceAround(int x, int y, Spatial spatial, Spatial attachee) {
		boolean ret = false;
		savedTranslation.set(attachee.getTranslation());
		double ex, ey, ez;
		if(spatial.getWorldBound() instanceof BoundingBox) {
			ex = ((BoundingBox)spatial.getWorldBound()).getXExtent();
			ey = ((BoundingBox)spatial.getWorldBound()).getYExtent();
			ez = ((BoundingBox)spatial.getWorldBound()).getZExtent();
		} else {
			ex = ey = ez = ((BoundingSphere)spatial.getWorldBound()).getRadius() * 2;
		}
		getNode().attachChild(attachee);
out:
        for(int dist = 1; dist < 3; dist++) {
            for(int dx = -dist; dx < dist; dx++) {
                for(int dy = -dist; dy < dist; dy++) {
                    attachee.setTranslation((x + dx) * ShapeUtil.WALL_WIDTH + (ShapeUtil.WALL_WIDTH - ex) / 2,
							4 + ey / 2,
							(y + dy) * ShapeUtil.WALL_WIDTH + (ShapeUtil.WALL_WIDTH - ez) / 2);
					attachee.updateWorldTransform(true);
					attachee.updateWorldBound(true);
					getNode().updateWorldBound(true);
//					Terrain.moveOnTopOfTerrain(attachee);
                    if(!PickingUtil.hasCollision(spatial, Main.getMain().getTerrain().getNode(), true) &&
							checkOverGround(attachee, Main.getMain().getTerrain().getNode())) {
                        ret = true;
						break out;
                    }
                }
            }
        }
		if(!ret) {
			getNode().detachChild(attachee);
			attachee.setTranslation(savedTranslation);
		} else {
			NodeUtil.nodeAdded(attachee, getNode());
		}
        return ret;
    }

	private boolean checkOverGround(Spatial spatial, Node scene) {
		boolean retValue = false;
		spatial.getSceneHints().setAllPickingHints(false);
		down.setOrigin(spatial.getWorldBound().getCenter());
		noDistanceResults.clear();
		PickingUtil.findPick(scene, down, noDistanceResults);
		if(noDistanceResults.getNumber() > 0) {
			for(int i = 0; i < noDistanceResults.getNumber(); i++) {
				if(noDistanceResults.getPickData(i).getTarget() != null &&
						noDistanceResults.getPickData(i).getTarget().toString().startsWith("ground_")) {
					retValue = true;
					break;
				}
			}
		}
		spatial.getSceneHints().setAllPickingHints(true);
		return retValue;
	}

	public boolean findSpaceAround(int x, int y, Spatial spatial) {
		return findSpaceAround(x, y, spatial, spatial);
	}

//    public boolean findSpaceAround(int x, int y, Spatial spatial, ReadOnlyVector3 location) {
//        proposedLocation.set((getX() + x) * ShapeUtil.WALL_WIDTH + (ShapeUtil.WALL_WIDTH - extent.x) / 2,
//                             4 + extent.y / 2,
//                             (getY() + y) * ShapeUtil.WALL_WIDTH + (ShapeUtil.WALL_WIDTH - extent.z) / 2);
//        if(!checkBoundingCollisions(terrain.getNode(), spatial)) {
//            location.set(proposedLocation);
//            return true;
//        }
//        return false;
//    }
//
//    public boolean checkBoundingCollisions(Node world, Spatial spatial) {
//        collisionResults.clear();
//        spatial.findCollisions(world, collisionResults);
//        return collisionResults.getNumber() > 0;
//    }

    public void update(double tpf) {
        for(Generator generator : generators) {
            generator.update(tpf);
        }
    }

    public void unloading() {
        for(Generator generator : generators) {
            generator.unloading();
        }
    }
}
