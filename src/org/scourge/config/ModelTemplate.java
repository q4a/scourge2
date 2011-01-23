package org.scourge.config;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Spatial;

/**
* User: gabor
* Date: 1/23/11
* Time: 2:35 PM
*/
public enum ModelTemplate {
    seymour("./data/models/dae/Seymour/Seymour.dae") {
        @Override
        public void transform(Spatial spatial) {
            spatial.setRotation(new Quaternion().fromAngleAxis(MathUtils.HALF_PI, Vector3.UNIT_Y));
        }
    },
    shade("./data/models/dae/shade/shade.dae") {
        @Override
        public void transform(Spatial spatial) {
            spatial.setRotation(new Quaternion().fromAngleAxis(-MathUtils.HALF_PI, Vector3.UNIT_X));
        }
    };
    private String model;

    ModelTemplate(String model) {
        this.model = model;
    }

    public String getModel() {
        return model;
    }

    public abstract void transform(Spatial spatial);
}
