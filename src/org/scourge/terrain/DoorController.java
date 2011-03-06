package org.scourge.terrain;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Spatial;

public class DoorController extends InAndOutController {
	private static final double MAX_ANGLE = 125.0;
	private static final double MAX_X = 2;
	private static final double MAX_Z = 4;
	private Quaternion q = new Quaternion();
	private Vector3 v = new Vector3();

	@Override
	protected void update(double tpf, Spatial spatial, boolean in, double timePercent) {
		setDoorTransform(in ? timePercent : (1 - timePercent), spatial);
	}

	@Override
	protected void endControl(boolean in, Spatial spatial) {
		setDoorTransform(in ? 1 : 0, spatial);
	}

	private void setDoorTransform(double percent, Spatial spatial) {
		setDoorTransform(percent * MAX_ANGLE,
				percent * MAX_X,
				percent * MAX_Z,
				spatial);
	}

	private void setDoorTransform(double angle, double x, double z, Spatial spatial) {
		q.fromAngleAxis(MathUtils.DEG_TO_RAD * angle, Vector3.UNIT_Y);
		v.set(x, 0, z);
		spatial.setTranslation(v);
		spatial.setRotation(q);
	}

	@Override
	protected void beginControl(boolean in, Spatial spatial) {
	}
}
