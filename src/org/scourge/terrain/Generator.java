package org.scourge.terrain;

import com.ardor3d.math.Vector3;
import org.scourge.io.BlockData;
import org.scourge.util.ShapeUtil;

/**
 * User: gabor
 * Date: Jun 15, 2010
 * Time: 8:16:25 AM
 */
public abstract class Generator {
    private Region region;
    private int x, y;
    private Vector3 location;

    public Generator(Region region, int x, int y) {
        this.region = region;
        this.x = x;
        this.y = y;
        this.location = new Vector3((getRegion().getX() + x) * ShapeUtil.WALL_WIDTH, 0, (getRegion().getY() + y) * ShapeUtil.WALL_WIDTH);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Region getRegion() {
        return region;
    }

    public Vector3 getLocation() {
        return location;
    }

    public abstract void generate();

    public abstract void update(float tpf);

    public abstract void unloading();

    public static Generator create(BlockData blockData, Region region, int x, int y) {
//        String type = blockData.getData().get("generate.monster");
//        if(type != null) return new MonsterGenerator(region, x, y, type);
//        type = blockData.getData().get("generate.altar");
//        if(type != null) return new AltarGenerator(region, x, y, type);
        return null;
    }
}
