package org.scourge.config;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import org.scourge.terrain.CreatureModel;

/**
* User: gabor
* Date: 1/23/11
* Time: 2:35 PM
*/
public enum CreatureModelTemplate {
    seymour("./data/models/dae/Seymour/Seymour.dae") {
        @Override
        public void transform(Node node) {
            for(Spatial spatial : node.getChildren()) {
                ((Mesh)spatial).setModelBound(new BoundingBox());
                spatial.setRotation(new Quaternion().fromAngleAxis(MathUtils.HALF_PI, Vector3.UNIT_Y));
                spatial.setScale(0.75);

            }
        }

        @Override
        public String getAnimationName(CreatureModel.Animations animation) {
            return null;
        }
    },
    shade("./data/models/dae/shade/shade.dae") {
        @Override
        public void transform(Node node) {
            for(Spatial spatial : node.getChildren()) {
                ((Mesh)spatial).setModelBound(new BoundingBox());
//                spatial.setRotation(new Quaternion().fromAngleAxis(-MathUtils.HALF_PI, Vector3.UNIT_X));
                spatial.setRotation(new Quaternion().fromEulerAngles(MathUtils.HALF_PI, 0, -MathUtils.HALF_PI));
                spatial.setScale(0.5);
            }
        }

        @Override
        public String getAnimationName(CreatureModel.Animations animation) {
            return "Action";
        }
    };
    private String model;

    CreatureModelTemplate(String model) {
        this.model = model;
    }

    public String getModel() {
        return model;
    }

    public abstract void transform(Node node);

    public abstract String getAnimationName(CreatureModel.Animations animation);
}
