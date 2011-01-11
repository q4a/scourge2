package org.scourge;

import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;

/**
 * User: gabor
 * Date: 1/8/11
 * Time: 9:32 AM
 */
public class Selection {
    private Vector3 location;
    private Spatial[] spatials;
    private Node root;

    public Selection(Node root) {
        this.root = root;
    }

    public boolean testUnderMouse() {
        return false;
    }

    public Vector3 getLocation() {
        return location;
    }

    public Spatial[] getSpatials() {
        return spatials;
    }
}
