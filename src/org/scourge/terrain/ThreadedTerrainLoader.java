package org.scourge.terrain;

import org.scourge.Main;
import org.scourge.Scourge;
import org.scourge.util.NodeUtil;

import java.util.*;
import java.util.logging.Logger;

/**
 * User: gabor
 * Date: 2/1/11
 * Time: 10:25 AM
 */
public class ThreadedTerrainLoader implements TerrainLoader {
    private Terrain terrain;
    private Region currentRegion;
    private Map<String, Region> loadedRegions = new HashMap<String, Region>();
    private final Map<String, RegionLoaderThread> regionThreads = new HashMap<String, RegionLoaderThread>();
    private byte checkPendingRegions;
    private static Logger logger = Logger.getLogger(Terrain.class.toString());
    private boolean loadAsynchronously;

    public ThreadedTerrainLoader(Terrain terrain) {
        this.terrain = terrain;
    }

    public void clear() {
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
            terrain.getNode().detachChild(region.getNode());
        }
        loadedRegions.clear();
        currentRegion = null;
    }

    protected void switchRegion() {
        // switch current region if needed
        if(terrain.getScourge().getPlayer() != null) {
            int px = terrain.getScourge().getPlayer().getCreatureModel().getX() / Region.REGION_SIZE;
            int pz = terrain.getScourge().getPlayer().getCreatureModel().getZ() / Region.REGION_SIZE;
            if(px != currentRegion.getX() / Region.REGION_SIZE ||
               pz != currentRegion.getY() / Region.REGION_SIZE) {
                Region region = loadedRegions.get(getRegionKey(px, pz));
                if(region != null) currentRegion = region;
            }
        }
    }

    // called from the input-handler thread
    public void loadRegionsForPlayerPosition() {
        switchRegion();
        boolean changed;

        int rx = currentRegion.getX() / Region.REGION_SIZE;
        int ry = currentRegion.getY() / Region.REGION_SIZE;

        int px = terrain.getScourge().getPlayer().getCreatureModel().getX() % Region.REGION_SIZE;
        int pz = terrain.getScourge().getPlayer().getCreatureModel().getZ() % Region.REGION_SIZE;
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

            // remove far regions first, so recursive updates below have less data to deal with
            if(loadAsynchronously) {
                removeFarRegions();
            }

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
                        terrain.getNode().attachChild(region.getNode());

						// magical updates
						NodeUtil.nodeAdded(region.getNode(), terrain.getNode());
                    }
                }
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
            terrain.getNode().detachChild(region.getNode());
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

    public Terrain getTerrain() {
        return terrain;
    }

    public void setRegionPending() {
        checkPendingRegions++;
    }

    public void setLoadAsynchronously(boolean loadAsynchronously) {
        this.loadAsynchronously = loadAsynchronously;
    }

    public Region getCurrentRegion() {
        return currentRegion;
    }

    public Map<String, Region> getLoadedRegions() {
        return loadedRegions;
    }
}
