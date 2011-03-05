package org.scourge.terrain;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.visitor.Visitor;

public class ShowHideController implements SpatialController {
	private boolean hide;
	private static final double FADE_TIME = 250;
	private double time = FADE_TIME;
	private ColorRGBA color = new ColorRGBA(1, 1, 1, 1);

	@Override
	public void update(double tpf, Spatial level) {
		if(time < FADE_TIME) {
			if(!hide && time == 0) {
				level.getSceneHints().setCullHint(CullHint.Dynamic);
			}

			time += tpf * 1000.0;
			double timePercent = time / FADE_TIME;
			float alpha = hide ? (float)(1 - timePercent) : (float)timePercent;
			color.setAlpha(alpha);

			level.acceptVisitor(new Visitor() {
				  @Override
				  public void visit(Spatial spatial) {
					  if(spatial instanceof Mesh) {
						  ((Mesh)spatial).getMeshData().setColorBuffer(null);
						  ((Mesh)spatial).getMeshData().setColorCoords(null);
						  ((Mesh)spatial).setDefaultColor(color);
					  }
				   }
				}, true);

			// should happen after transition (fade)
			if(hide && time >= FADE_TIME) {
				level.getSceneHints().setCullHint(CullHint.Always);
			}
		}
	}

	public void setHiding(boolean hide) {
		if(hide != this.hide) {
			this.hide = hide;
			this.time = 0;
		}
	}
}
