package org.scourge.util;

import com.ardor3d.scenegraph.Spatial;

public class NodeUtil {
	/**
	 * Sometimes when a node is added you need to immediately update the scene.
	 * Note: this is an expensive call; do not call in a loop
	 *
	 * @param child the child node
	 * @param scene the scene (parent) node
	 */
	public static void nodeAdded(Spatial child, Spatial scene) {
		// not sure which but one of the following is needed...
		child.updateWorldBound(true);
		child.updateGeometricState(0);
		child.updateWorldTransform(true);
		child.updateWorldRenderStates(true);
		scene.updateWorldBound(true);
		scene.updateGeometricState(0);
		scene.updateWorldTransform(true);
		scene.updateWorldRenderStates(true);
	}
}
