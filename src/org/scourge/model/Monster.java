package org.scourge.model;

import org.scourge.config.ModelTemplate;
import org.scourge.terrain.CreatureModel;

/**
 * User: gabor
 * Date: Jun 15, 2010
 * Time: 8:21:39 AM
 */
public enum Monster {
    shade(ModelTemplate.shade, "./data/models/phantom/m10.png", 10.0f),
    ;

    private ModelTemplate modelTemplate;
    private String skinPath;
    private float speed;

    Monster(ModelTemplate modelTemplate, String skinPath, float speed) {
        this.modelTemplate = modelTemplate;
        this.skinPath = skinPath;
        this.speed = speed;
    }

    public ModelTemplate getModelTemplate() {
        return modelTemplate;
    }

    public String getSkinPath() {
        return skinPath;
    }

    public CreatureModel createModel() {
        CreatureModel model = new CreatureModel(modelTemplate, skinPath, name());
//        model.setKeyFrame(Md2Model.Md2Key.stand);

        // create world bounds, etc.
//        model.getNode().updateGeometricState(0,true);

        return model;
    }

    public float getSpeed() {
        return speed;
    }
}
