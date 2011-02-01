package org.scourge.terrain;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: gabor
 * Date: Apr 4, 2010
 * Time: 8:03:16 PM
 */
public class RegionLoaderThread extends Thread {
    private static Logger logger = Logger.getLogger(RegionLoaderThread.class.toString());
    private int rx;
    private int ry;
    private Region region;
    private ThreadedTerrainLoader terrainLoader;

    public RegionLoaderThread(ThreadedTerrainLoader terrainLoader, int rx, int ry) {
        this.terrainLoader = terrainLoader;
        this.rx = rx;
        this.ry = ry;
        setPriority(Thread.MIN_PRIORITY);
        setDaemon(true);
    }

    public void run() {
        String key = ThreadedTerrainLoader.getRegionKey(rx, ry);
        try {
            terrainLoader.getTerrain().getScourge().setLoading(true);
            logger.fine("Loading region: " + key);
            region = new Region(terrainLoader.getTerrain(), rx * Region.REGION_SIZE, ry * Region.REGION_SIZE);
            terrainLoader.setRegionPending();
        } catch(IOException exc) {
            logger.log(Level.SEVERE, exc.getMessage(), exc);
        } finally {
            terrainLoader.getTerrain().getScourge().setLoading(false);
            logger.fine("Loaded region: " + key);
        }
    }

    public Region getRegion() {
        return region;
    }
}
