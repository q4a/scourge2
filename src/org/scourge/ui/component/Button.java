package org.scourge.ui.component;

import com.ardor3d.math.*;
import com.ardor3d.scenegraph.shape.Quad;
import org.scourge.util.ShapeUtil;

/**
 * User: gabor
 * Date: Apr 11, 2010
 * Time: 10:14:46 PM
 */
class Button extends Component {
    private Label label;
    private Quad quad;
    private static final String BUTTON_BACKGROUND = "./data/textures/ui/button.png";
    private static final ColorRGBA BUTTON_TEXT_COLOR = new ColorRGBA(1, 0.90f, 0.75f, 1);
    private static final Matrix3 CLICK_TEXTURE_ROTATE = new Matrix3();
    static {
        new Quaternion().fromAngleAxis(MathUtils.DEG_TO_RAD * 180, Vector3.UNIT_Y).toRotationMatrix(CLICK_TEXTURE_ROTATE);
    }
    private static final Vector3 BUTTON_PRESS_TRANS = new Vector3(2, -2, 0);

    public Button(Window window, String name, int x, int y, int w, int h, String text) {
        super(window, name, x, y, w, h);

        quad = WinUtil.createQuad("button_quad", w, h, BUTTON_BACKGROUND);
        getNode().attachChildAt(quad, 0);

        label = new Label(window, ShapeUtil.newShapeName("button_label"), 0, 0, text,
                          BUTTON_TEXT_COLOR,
                          true,
                          WinUtil.ScourgeFont.regular);
        getNode().attachChildAt(label.getNode(), 0);
    }

    public void pressButton() {
        Matrix3 m = new Matrix3(quad.getRotation());
        m.multiplyLocal(CLICK_TEXTURE_ROTATE);
        quad.setRotation(m);
        Vector3 v = new Vector3(label.getNode().getTranslation());
        v.addLocal(BUTTON_PRESS_TRANS);
        label.getNode().setTranslation(v);
    }

    public void releaseButton() {
        Matrix3 m = new Matrix3(quad.getRotation());
        m.multiplyLocal(CLICK_TEXTURE_ROTATE);
        quad.setRotation(m);
        Vector3 v = new Vector3(label.getNode().getTranslation());
        v.subtractLocal(BUTTON_PRESS_TRANS);
        label.getNode().setTranslation(v);
    }

    @Override
    public void setText(String text) {
        label.setText(text);
    }

    @Override
    public String getText() {
        return label.getText();
    }
}
