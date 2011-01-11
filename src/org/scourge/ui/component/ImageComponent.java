package org.scourge.ui.component;


import com.ardor3d.scenegraph.shape.Quad;

/**
 * User: gabor
 * Date: Apr 19, 2010
 * Time: 9:03:04 PM
 */
public class ImageComponent extends Component {
    private Quad quad;
    private String imagePath;

    public ImageComponent(Window window, String name, String imagePath, int x, int y, int w, int h) {
        super(window, name, x, y, w, h);
        this.imagePath = imagePath;
        quad = WinUtil.createQuad("image_quad", w, h, imagePath);
        getNode().attachChildAt(quad, 0);
    }

    @Override
    public String getImage() {
        return imagePath;
    }

    @Override
    public void setImage(String imagePath) {
        getWindow().unpack();
        getNode().detachChild(quad);
        this.imagePath = imagePath;
        quad = WinUtil.createQuad("image_quad", getW(), getH(), imagePath);
        getNode().attachChildAt(quad, 0);
        getWindow().pack();
    }
}
