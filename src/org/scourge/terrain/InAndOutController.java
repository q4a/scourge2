package org.scourge.terrain;

import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;

public abstract class InAndOutController implements SpatialController {
	private boolean in;
	private static final double FADE_TIME = 250;
	private double time = FADE_TIME;

	@Override
	public void update(double tpf, Spatial level) {
		if(time < FADE_TIME) {
			if(!in && time == 0) {
				beginControl(in, level);
			}

			time += tpf * 1000.0;
			double timePercent = time / FADE_TIME;

			update(tpf, level, in, timePercent);

			// should happen after transition (fade)
			if(time >= FADE_TIME) {
				endControl(in, level);
			}
		}
	}

	protected abstract void endControl(boolean in, Spatial level);

	protected abstract void beginControl(boolean in, Spatial spatial);

	protected abstract void update(double tpf, Spatial spatial, boolean in, double timePercent);

	public void setIn(boolean in) {
		if(in != this.in) {
			this.in = in;
			this.time = 0;
		}
	}

}
