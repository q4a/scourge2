package org.scourge;

import org.scourge.model.Creature;
import org.scourge.terrain.Terrain;

import java.util.Random;

/**
 * User: gabor
 * Date: Dec 31, 2010
 * Time: 7:59:40 PM
 */
public interface Scourge {
    public Random getRandom();

    public Creature getPlayer();

    public Terrain getTerrain();

}
