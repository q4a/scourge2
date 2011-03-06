package org.scourge.terrain;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.visitor.Visitor;

public class ShowHideController extends InAndOutController {
	private ColorRGBA color = new ColorRGBA(1, 1, 1, 1);

	@Override
	protected void update(double tpf, Spatial spatial, boolean in, double timePercent) {
		float alpha = in ? (float)(1 - timePercent) : (float)timePercent;
		color.setAlpha(alpha);

		spatial.acceptVisitor(new Visitor() {
			  @Override
			  public void visit(Spatial spatial) {
				  if(spatial instanceof Mesh) {
					  ((Mesh)spatial).getMeshData().setColorBuffer(null);
					  ((Mesh)spatial).getMeshData().setColorCoords(null);
					  ((Mesh)spatial).setDefaultColor(color);
				  }
			   }
			}, true);
	}

	@Override
	protected void beginControl(boolean in, Spatial spatial) {
		if(!in) {
			spatial.getSceneHints().setCullHint(CullHint.Dynamic);
		}
	}

	@Override
	protected void endControl(boolean in, Spatial level) {
		if(in) {
			level.getSceneHints().setCullHint(CullHint.Always);
		}
	}
}
