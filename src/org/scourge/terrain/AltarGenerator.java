package org.scourge.terrain;

import com.ardor3d.scenegraph.Spatial;
import org.scourge.util.ShapeUtil;

/**
 * User: gabor
 * Date: Jul 5, 2010
 * Time: 9:25:15 AM
 */
public class AltarGenerator extends Generator {
    String type;
    private Spatial spatial;
    private boolean first = true;
    private boolean generated;

    public AltarGenerator(Region region, int x, int y, String type) {
        super(region, x, y);
        this.type = type;
        try {
            Model model = Model.valueOf(type);
            spatial = model.createSpatial();
//            spatial.setModelBound(new BoundingBox());
//            spatial.updateModelBound();
            spatial.updateWorldBound(true);
            spatial.updateGeometricState(0, true);
        } catch(RuntimeException exc) {
            exc.printStackTrace();
            throw exc;
        }
    }

    @Override
    public void generate() {
        if(spatial != null && !generated) {
            generated = true;
//            if(getRegion().findSpaceAround(getX(), getY(), spatial, spatial.getTranslation())) {
                spatial.setTranslation(spatial.getTranslation().getX() - getRegion().getX() * ShapeUtil.WALL_WIDTH,
                                       spatial.getTranslation().getY(),
                                       spatial.getTranslation().getZ() - getRegion().getY() * ShapeUtil.WALL_WIDTH);
                getRegion().getNode().attachChild(spatial);
                spatial.updateWorldBound(true);
                getRegion().getNode().updateWorldBound(true);
//            }
        }
    }

    @Override
    public void update(double tpf) {
        if(first) {
            first = false;
            // todo: fountain is not on the ground
            spatial.updateGeometricState(0, true);
            Terrain.moveOnTopOfTerrain(spatial);
        }
    }

    @Override
    public void unloading() {
    }
}
