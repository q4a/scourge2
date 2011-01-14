package org.scourge.model;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.image.Texture;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.TextureManager;
import org.scourge.config.ItemTemplate;
import org.scourge.config.Items;
import org.scourge.util.ShapeUtil;
import org.scourge.ui.component.Dragable;
import org.scourge.util.ImageUtil;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import javax.swing.*;
import java.util.logging.Logger;

/**
 * User: gabor
 * Date: May 3, 2010
 * Time: 8:24:55 PM
 */
@Root(name="item")
public class Item implements Dragable {
    @Element
    private String name;

    @Element
    private int charges;

    @Element(required = false)
    private int[] containerPosition;

    private ItemTemplate template;
    private Logger logger = Logger.getLogger(Item.class.toString());
    private ImageIcon icon;
    private Spatial spatial;
    private Texture iconTexture;
    private boolean scaled;

    // explicit default constructor for simple.xml
    public Item() {
    }

    // constructor to create a new item in game
    public Item(String name) {
        this.name = name;
        afterLoad();
    }

    public void afterLoad() {
        this.template = Items.getInstance().getItem(name);
        if(template == null) {
            throw new IllegalArgumentException("Can't find item template " + name);
        }
        this.charges = template.getMaxCharges();
    }

    public ItemTemplate getTemplate() {
        return template;
    }

    public int getCharges() {
        return charges;
    }

    public int[] getContainerPosition() {
        return containerPosition;
    }

    public void setContainerPosition(int[] containerPosition) {
        this.containerPosition = containerPosition;
    }

    public ImageIcon getIcon() {
        if(icon == null) {
            icon = ShapeUtil.loadImageIcon(getTemplate().getModel().getIcon());
            if(icon == null) {
                logger.severe("Can't load icon for " + getTemplate().getName());
            }
        }
        return icon;
    }

    @Override
    public Texture getIconTexture() {
        if(iconTexture == null) {
            ImageIcon icon = getIcon();
            iconTexture = ShapeUtil.getTexture(getTemplate().getIcon());
            if(iconTexture == null) {
                iconTexture = TextureManager.loadFromImage(ImageUtil.getArdorImage(icon, true),
                                                           Texture.MinificationFilter.NearestNeighborNearestMipMap);
                iconTexture.setWrap(Texture.WrapMode.Repeat);
                iconTexture.setHasBorder(false);
                iconTexture.setApply(Texture.ApplyMode.Modulate);
                ShapeUtil.storeTexture(getTemplate().getIcon(), iconTexture);
            }
        }
        return iconTexture;
    }

    @Override
    public int getIconWidth() {
        return getIcon().getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return getIcon().getIconHeight();
    }

    @Override
    public Spatial getModel() {
        if(spatial == null) {
            spatial = ShapeUtil.importModel(getTemplate().getModel().getPath(), "./data/textures", getTemplate().getName(), null, -90, 0, 0);
//            if(getTemplate().getModel().getRotate() != null) {
//                spatial.getLocalRotation().set(new Quaternion().fromAngles(getTemplate().getModel().getRotate()[0] * 90 * FastMath.DEG_TO_RAD,
//                                                                           getTemplate().getModel().getRotate()[1] * 90 * FastMath.DEG_TO_RAD,
//                                                                           getTemplate().getModel().getRotate()[2] * 90 * FastMath.DEG_TO_RAD));
//            }

//            Matrix3 m = new Matrix3();
//            new Quaternion().fromAngleAxis(-90 * MathUtils.DEG_TO_RAD, Vector3.UNIT_X).toRotationMatrix(m);
//            spatial.getRotation().multiply(m, m);
//            spatial.setRotation(m);

            BlendState as = new BlendState();
            as.setBlendEnabled(true);
            as.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
            as.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
            as.setReference(0);
            as.setTestEnabled(true);
            as.setTestFunction(BlendState.TestFunction.NotEqualTo);
            as.setEnabled(true);
            spatial.setRenderState(as);

//            BoundingBox bb = new BoundingBox();
//            spatial.setModelBound(bb);
//            spatial.updateModelBound();
        }
        return spatial;
    }

    public void scaleModel() {
        if(!scaled) {
            scaled = true;
            BoundingBox bb = (BoundingBox)spatial.getWorldBound();
            float[] size = getTemplate().getModel().getDimensions();
            if(size != null) {
                double sx = 1;
                double sy = 1;
                double sz = 1;
                if(size[0] > 0) sx = size[0] / bb.getXExtent();
                if(size[2] > 0) sy = size[2] / bb.getYExtent();
                if(size[1] > 0) sz = size[1] / bb.getZExtent();
                spatial.setScale(sx, sy, sz);
            }
//            spatial.updateModelBound();
            spatial.updateWorldBound(true);
        }

    }
}
