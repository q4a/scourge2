package org.scourge.terrain;

import java.util.Map;

/**
 * User: gabor
 * Date: 2/1/11
 * Time: 10:37 AM
 */
public interface TerrainLoader {
    public void clear();

    public Region getCurrentRegion();

    public void loadRegionsForPlayerPosition();

    public boolean loadRegion(final int regionX, final int regionY);

    public Map<String, Region> getLoadedRegions();

    public void update(double tpf);

    void setLoadAsynchronously(boolean b);
}
