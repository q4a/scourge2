package org.scourge;

import com.ardor3d.intersection.PickData;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;

/**
 * User: gabor
 * Date: 1/8/11
 * Time: 9:32 AM
 */
public class Selection {
    private Vector3 location = new Vector3();
    private Spatial spatial;
    private Node root;
    private Main main;
    private Vector2 pos = new Vector2();
    private final Ray3 pickRay = new Ray3();
    private final PrimitivePickResults pickResults = new PrimitivePickResults();

    public Selection(Node root, Main main) {
        this.root = root;
        this.main = main;
        pickResults.setCheckDistance(true);
    }

    public boolean testUnderMouse() {
        pos.set(main.getPlayerControl().getLastInputState().getCurrent().getMouseState().getX(),
                main.getPlayerControl().getLastInputState().getCurrent().getMouseState().getY());
        main.getCanvas().getCanvasRenderer().getCamera().getPickRay(pos, false, pickRay);
        doPick(pickRay);
        return spatial != null;
    }

    public void doPick(final Ray3 pickRay) {
        pickResults.clear();
        PickingUtil.findPick(root, pickRay, pickResults);
        processPicks(pickResults);
    }

    protected void processPicks(final PrimitivePickResults pickResults) {
        int i = 0;
        while (pickResults.getNumber() > 0 &&
               pickResults.getPickData(i).getIntersectionRecord().getNumberOfIntersections() == 0 &&
               ++i < pickResults.getNumber()) {
        }
        if (pickResults.getNumber() > i) {
            final PickData pick = pickResults.getPickData(i);
//            System.err.println("picked: " + pick.getTarget() +
//                               " at: " + pick.getIntersectionRecord().getIntersectionPoint(0));
            location.set(pick.getIntersectionRecord().getIntersectionPoint(0));
            if(pick.getTarget() instanceof Spatial) {
                spatial = (Spatial)pick.getTarget();
            }
        } else {
//            System.err.println("picked: nothing");
            location.set(0, 0, 0);
            spatial = null;
        }
    }

    public Vector3 getLocation() {
        return location;
    }

    public Spatial getSpatial() {
        return spatial;
    }
}
