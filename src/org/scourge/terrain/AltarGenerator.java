package org.scourge.terrain;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.scenegraph.Node;
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
		System.err.println(">>>>> ALTAR: " + type);
        this.type = type;
        try {
            Model model = Model.valueOf(type);
            spatial = model.createSpatial();
//            spatial.updateWorldBound(true);
//            spatial.updateGeometricState(0, true);
        } catch(RuntimeException exc) {
            exc.printStackTrace();
            throw exc;
        }
    }

    @Override
    public void generate() {
        if(spatial != null && !generated) {
            generated = true;
			System.err.println("*** Positioning " + type + " current: " + spatial.getTranslation());
            if(getRegion().findSpaceAround(getX(), getY(), ((Node)spatial).getChild(0), spatial)) {
				System.err.println("\t*** SUCCESS, current: " + spatial.getTranslation());
            } else {
				System.err.println("\t*** FAILURE, current: " + spatial.getTranslation());
			}
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
