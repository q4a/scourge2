package org.scourge.ui.component;

import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Node;
import org.scourge.Main;

import java.awt.*;

/**
 * User: gabor
 * Date: Apr 19, 2010
 * Time: 12:05:31 PM
 */
public abstract class Component {
    private Node node;
    private int x, y, w, h;
    private Rectangle rectangle;
    private Window window;
    private String name;

    public Component(Window window, String name, int x, int y, int w, int h) {
        this.window = window;
        node = new Node(name);
        Vector3 v = new Vector3(node.getTranslation());
        v.addLocal(x, y, 0);
        node.setTranslation(v);
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.name = name;
        rectangle = createRectangle();
    }

    protected Rectangle createRectangle() {
        return new Rectangle(window.getX() + x - w / 2, window.getY() + y - h / 2, w, h);
    }

    public Node getNode() {
        return node;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }

    public Window getWindow() {
        return window;
    }

    public String getName() {
        return name;
    }

    // subclass to implement
    public void setText(String value) {
    }

    public String getText() {
        return null;
    }

    public String getImage() {
        return null;
    }

    public void setImage(String imagePath) {
    }

    public void setValue(float value) {
    }

    public float getValue() {
        return 0f;
    }
}
