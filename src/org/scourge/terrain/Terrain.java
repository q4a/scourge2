package org.scourge.terrain;


import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import org.scourge.Main;
import org.scourge.Scourge;
import org.scourge.io.MapIO;
import org.scourge.util.ShapeUtil;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * User: gabor
 * Date: Apr 1, 2010
 * Time: 9:01:16 AM
 */
public class Terrain implements NodeGenerator {
    private Node terrain;
    private MapIO mapIO;
    private Scourge scourge;
    private Region currentRegion;
    private Map<String, Region> loadedRegions = new HashMap<String, Region>();
    private final Map<String, RegionLoaderThread> regionThreads = new HashMap<String, RegionLoaderThread>();
    private byte checkPendingRegions;
    private static Logger logger = Logger.getLogger(Terrain.class.toString());
    private boolean loadAsynchronously;

    public Terrain(Scourge scourge) throws IOException {
        this.scourge = scourge;
        this.terrain = new Node("terrain");
//        terrain.setModelBound(new BoundingBox());
        mapIO = new MapIO();
    }

    public void gotoMainMenu() {
        clearLoadedRegions();
        loadAsynchronously = false;
        loadRegion(449 / Region.REGION_SIZE, 509 / Region.REGION_SIZE);
        loadAsynchronously = true;

        Main.getMain().setFogOnWater(false);
        Main.getMain().setCameraFollowsPlayer(false);
        Vector3 pos = new Vector3(currentRegion.getX() * ShapeUtil.WALL_WIDTH,
                                           2 * ShapeUtil.WALL_WIDTH,
                                           currentRegion.getY() * ShapeUtil.WALL_WIDTH);
        Main.getMain().getCamera().setLocation(pos);
        Main.getMain().getCamera().lookAt(pos.addLocal(new Vector3(10, 1, 10)), Vector3.UNIT_Y);
    }

    public void gotoPlayer() {
        clearLoadedRegions();

        teleport();
        
        Main.getMain().setFogOnWater(true);
        Main.getMain().setCameraFollowsPlayer(true);
    }

    public void teleport() {
        loadAsynchronously = false;
        loadRegion(scourge.getPlayer().getCreatureModel().getX() / Region.REGION_SIZE, scourge.getPlayer().getCreatureModel().getZ() / Region.REGION_SIZE);
        loadRegion();
//        main.checkRoof();
        loadAsynchronously = true;
    }

    private void clearLoadedRegions() {
        synchronized(regionThreads) {
            for (String key : regionThreads.keySet()) {
                RegionLoaderThread thread = regionThreads.get(key);
                try {
                    thread.join();
                } catch (InterruptedException e1) {
                    // eh
                }
            }
            regionThreads.clear();
        }
        for(Region region : loadedRegions.values()) {
            terrain.detachChild(region.getNode());
        }
        loadedRegions.clear();
        currentRegion = null;
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

    public Region getCurrentRegion() {
        return currentRegion;
    }


    protected void switchRegion() {
        // switch current region if needed
        if(scourge.getPlayer() != null) {
            int px = scourge.getPlayer().getCreatureModel().getX() / Region.REGION_SIZE;
            int pz = scourge.getPlayer().getCreatureModel().getZ() / Region.REGION_SIZE;
            if(px != currentRegion.getX() / Region.REGION_SIZE ||
               pz != currentRegion.getY() / Region.REGION_SIZE) {
                Region region = loadedRegions.get(getRegionKey(px, pz));
                if(region != null) currentRegion = region;
            }
        }
    }

    // called from the input-handler thread
    public void loadRegion() {
        switchRegion();
        boolean changed;

        int rx = currentRegion.getX() / Region.REGION_SIZE;
        int ry = currentRegion.getY() / Region.REGION_SIZE;

        int px = scourge.getPlayer().getCreatureModel().getX() % Region.REGION_SIZE;
        int pz = scourge.getPlayer().getCreatureModel().getZ() % Region.REGION_SIZE;
        if(px < Region.REGION_SIZE / 2) {
            changed = loadRegion(rx - 1, ry);
        } else {
            changed = loadRegion(rx + 1, ry);
        }

        if(pz < Region.REGION_SIZE / 2) {
            if(!changed && loadRegion(rx, ry - 1)) changed = true;
        } else {
            if(!changed && loadRegion(rx, ry + 1)) changed = true;
        }

        if(px < Region.REGION_SIZE / 2 && pz < Region.REGION_SIZE / 2) {
            if(!changed && loadRegion(rx - 1, ry - 1)) changed = true;
        } else if(px < Region.REGION_SIZE / 2 && pz >= Region.REGION_SIZE / 2) {
            if(!changed && loadRegion(rx - 1, ry + 1)) changed = true;
        } else if(px >= Region.REGION_SIZE / 2 && pz < Region.REGION_SIZE / 2) {
            if(!changed && loadRegion(rx + 1, ry - 1)) changed = true;
        } else if(px >= Region.REGION_SIZE / 2 && pz >= Region.REGION_SIZE / 2) {
            if(!changed && loadRegion(rx + 1, ry + 1)) changed = true;
        }

        Main.getMain().getMiniMap().update(changed);
    }

    // input-handler thread
    public boolean loadRegion(final int rx, final int ry) {
        boolean changed = false;
        final String key = getRegionKey(rx, ry);

        // non-synchronized check (this could cause synchronization problems...)
        if(!loadedRegions.containsKey(key)) {
            synchronized(regionThreads) {
                if(!regionThreads.containsKey(key)) {
                    changed = true;
                    System.err.println(">>> loading: " + rx + "," + ry);
                    RegionLoaderThread thread = new RegionLoaderThread(this, rx, ry);
                    regionThreads.put(key, thread);
                    thread.start();

                    if(!loadAsynchronously) {
                        try {
                            thread.join();
                            update(0);
                        } catch (InterruptedException e) {
                            // eh
                        }
                    }
                }
            }
        }

        switchRegion();
        return changed;
    }

    // called from the main thread
    public void update(double tpf) {
        // make an unsynchronized check (a small hack: pendingRegions.isEmpty() would have to be synchronized)
        if(checkPendingRegions > 0) {
            // add/remove regions in the main thread to avoid concurrent mod. exceptions
            synchronized(regionThreads) {
                for(Iterator<String> e = regionThreads.keySet().iterator(); e.hasNext();) {
                    String key = e.next();
                    RegionLoaderThread thread = regionThreads.get(key);
                    if(thread.getRegion() != null) {
                        logger.info("Attaching region " + key);

                        e.remove();
                        Region region = thread.getRegion();
                        if(currentRegion == null) {
                            currentRegion = region;
                        }
                        loadedRegions.put(key, region);

                        // keep checking if there are threads out there
                        checkPendingRegions--;


                        // below this line doesn't need to be synchronized
                        terrain.attachChild(region.getNode());

                        // not sure which but one of the following is needed...
                        region.getNode().updateWorldBound(true);
                        region.getNode().updateGeometricState(0);
                        region.getNode().updateWorldTransform(true);
                        region.getNode().updateWorldRenderStates(true);
                        terrain.updateWorldBound(true);
                        terrain.updateGeometricState(0);
                        terrain.updateWorldTransform(true);
                        terrain.updateWorldRenderStates(true);
                    }
                }
            }

            if(loadAsynchronously) {
                removeFarRegions();
            }
        }

        for(Region region : loadedRegions.values()) {
            region.update(tpf);
        }
    }

    // main thread
    private void removeFarRegions() {
        int rx = currentRegion.getX() / Region.REGION_SIZE;
        int ry = currentRegion.getY() / Region.REGION_SIZE;

        Set<String> far = new HashSet<String>();
        for(String s : loadedRegions.keySet()) {
            String[] ss = s.split(",");
            int x = Integer.valueOf(ss[0]);
            int y = Integer.valueOf(ss[1]);
            if(Math.abs(x - rx) > 1 || Math.abs(y - ry) > 1) {
                far.add(s);
            }
        }
        for(String s : far) {
            logger.fine("Unloading region: " + s);
            Region region = loadedRegions.remove(s);
            region.unloading();
            terrain.detachChild(region.getNode());
        }

        logger.info("Current: " + rx + "," + ry + " loaded regions: " + loadedRegions.keySet());
//        System.err.println("terrain has " + terrain.getChildren().size() + " children: ");
//        for(Spatial sp : terrain.getChildren()) {
//            System.err.println("\t" + sp.getName());
//        }
        
        System.gc();
        Thread.yield();
    }

//    public Region getRegionAtPoint(Vector3 v) {
//        int rx = (int)(v.x / ShapeUtil.WALL_WIDTH / Region.REGION_SIZE);
//        int rz = (int)(v.z / ShapeUtil.WALL_WIDTH / Region.REGION_SIZE);
//        return loadedRegions.get(getRegionKey(rx, rz));
//    }

    public static String getRegionKey(int rx, int rz) {
        return "" + rx + "," + rz;
    }

    public void setRegionPending() {
        checkPendingRegions++;
    }

    public Map<String, Region> getLoadedRegions() {
        return loadedRegions;
    }

//    private static TrianglePickResults results = new TrianglePickResults();
//    private static final Ray down = new Ray();
//
//    static {
//        down.getDirection().set(new Vector3(0, -1, 0));
//        results.setCheckDistance(true);
//    }
//

    private static Vector3 topOfTerrain = new Vector3();

    public static boolean moveOnTopOfTerrain(Spatial spatial) {
        boolean ret = false;
        topOfTerrain.set(spatial.getTranslation());
        topOfTerrain.setY(8);
        spatial.setTranslation(topOfTerrain);
        ret = true;
//        spatial.setIsCollidable(false);
//        down.getOrigin().set(spatial.getWorldBound().getCenter());
//        results.clear();
//        Main.getMain().getTerrain().getNode().findPick(down, results);
//        if (results.getNumber() > 0) {
//            float dist = results.getPickData(0).getDistance();
//            if(!Float.isInfinite(dist) && !Float.isNaN(dist)) {
//                // put it a hair above the ground so we don't get stopped by the tops of edge sections
//                spatial.getLocalTranslation().y -= dist - ((BoundingBox)spatial.getWorldBound()).yExtent;
//                ret = true;
//            }
//        }
//        spatial.setIsCollidable(true);
//        spatial.updateModelBound();
//        spatial.updateWorldBound();
        return ret;
    }

    public void setRoofVisible(boolean visible) {
        for(Region region : loadedRegions.values()) {
            region.setRoofVisible(visible);
        }
    }
}
