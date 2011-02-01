package org.scourge.terrain;


import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.intersection.PickData;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import org.scourge.Main;
import org.scourge.Scourge;
import org.scourge.io.MapIO;
import org.scourge.util.ShapeUtil;
import com.ardor3d.scenegraph.hint.PickingHint;

import java.io.IOException;
import java.util.*;

/**
 * User: gabor
 * Date: Apr 1, 2010
 * Time: 9:01:16 AM
 */
public class Terrain implements NodeGenerator {
    private Node terrain;
    private MapIO mapIO;
    private Scourge scourge;
    private TerrainLoader terrainLoader;

    public Terrain(Scourge scourge) throws IOException {
        this.scourge = scourge;
        this.terrain = new Node("terrain");
//        terrain.setModelBound(new BoundingBox());
        mapIO = new MapIO();
        terrainLoader = new ThreadedTerrainLoader(this);
    }

    public void gotoMainMenu() {
        terrainLoader.clear();
        terrainLoader.setLoadAsynchronously(false);
        terrainLoader.loadRegion(449 / Region.REGION_SIZE, 509 / Region.REGION_SIZE);
        terrainLoader.setLoadAsynchronously(true);

        Main.getMain().setFogOnWater(false);
        Main.getMain().setCameraFollowsPlayer(false);
        Vector3 pos = new Vector3(terrainLoader.getCurrentRegion().getX() * ShapeUtil.WALL_WIDTH,
                                           2 * ShapeUtil.WALL_WIDTH,
                                           terrainLoader.getCurrentRegion().getY() * ShapeUtil.WALL_WIDTH);
        Main.getMain().getCamera().setLocation(pos);
        Main.getMain().getCamera().lookAt(pos.addLocal(new Vector3(10, 1, 10)), Vector3.UNIT_Y);
    }

    public void gotoPlayer() {
        terrainLoader.clear();

        teleport();
        
        Main.getMain().setFogOnWater(true);
        Main.getMain().setCameraFollowsPlayer(true);
    }

    public void teleport() {
        terrainLoader.setLoadAsynchronously(false);
        terrainLoader.loadRegion(scourge.getPlayer().getCreatureModel().getX() / Region.REGION_SIZE, scourge.getPlayer().getCreatureModel().getZ() / Region.REGION_SIZE);
        terrainLoader.loadRegionsForPlayerPosition();
        Main.getMain().checkRoof();
        terrainLoader.setLoadAsynchronously(true);
    }

    @Override
    public Node getNode() {
        return terrain;
    }

    public MapIO getMapIO() {
        return mapIO;
    }

    public Scourge getScourge() {
        return scourge;
    }

    private static final double ABOVE_GROUND = 2;
    private static final Ray3 down = new Ray3();
    private static PrimitivePickResults results = new PrimitivePickResults();
    private static Vector3 tmpVector = new Vector3();

    static {
        down.setDirection(new Vector3(0, -1, 0));
        results.setCheckDistance(true);
    }

    public static boolean moveOnTopOfTerrain(Spatial spatial) {
        boolean ret = false;
        spatial.getSceneHints().setPickingHint(PickingHint.Pickable, false);
        tmpVector.set(spatial.getWorldBound().getCenter());
        tmpVector.setY(tmpVector.getY() + ABOVE_GROUND);
        down.setOrigin(tmpVector);
        results.clear();
        PickingUtil.findPick(Main.getMain().getTerrain().getNode(), down, results);
        for(int i = 0; i < results.getNumber(); i++) {
            if(spatial != results.getPickData(i).getTarget()) {
                PickData pickData = results.getPickData(i);
                ReadOnlyVector3 v = spatial.getTranslation();
                spatial.setTranslation(v.getX(),
                                       v.getY() -
                                       ((pickData.getIntersectionRecord().getClosestDistance() - ABOVE_GROUND) -
                                        ((BoundingBox)spatial.getWorldBound()).getYExtent()),
                                       v.getZ());
                ret = true;
                break;
            }
        }
//        spatial.updateGeometricState(0);
//        spatial.updateWorldTransform(true);
        spatial.getSceneHints().setPickingHint(PickingHint.Pickable, true);
        return ret;
    }

    public void setRoofVisible(boolean visible) {
        for(Region region : terrainLoader.getLoadedRegions().values()) {
            region.setRoofVisible(visible);
        }
    }

    public Region getCurrentRegion() {
        return terrainLoader.getCurrentRegion();
    }

    public void loadRegion() {
        terrainLoader.loadRegionsForPlayerPosition();
    }

    public Map<String, Region> getLoadedRegions() {
        return terrainLoader.getLoadedRegions();
    }

    public void update(double tpf) {
        terrainLoader.update(tpf);
    }
}
