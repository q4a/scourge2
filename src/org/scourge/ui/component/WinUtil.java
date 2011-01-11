package org.scourge.ui.component;

import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.*;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.ui.text.BasicText;
import org.scourge.util.ShapeUtil;

import java.awt.*;
import java.io.File;
import java.nio.FloatBuffer;

/**
 * User: gabor
 * Date: Apr 11, 2010
 * Time: 11:29:00 PM
 */
public class WinUtil {
    public static void makeNodeOrtho(Spatial node) {
        ZBufferState zbs = new ZBufferState();
		zbs.setFunction(ZBufferState.TestFunction.Always);
        node.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
		//node.setRenderQueueMode(Renderer.QUEUE_ORTHO);
		node.setRenderState(zbs);
        node.getSceneHints().setCullHint(CullHint.Never);
//		node.updateRenderState();

        CullState cullState = new CullState();
        cullState.setCullFace(CullState.Face.None);
        cullState.setEnabled(true);
        node.setRenderState(cullState);

        FogState fs = new FogState();
        fs.setEnabled(false);
        node.setRenderState(fs);

        node.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        node.getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);
    }

    public enum ScourgeFont {
        regular(12, "data/fonts/DejaVuLGCSans.ttf", 2, true, 0),
        mono(12, "data/fonts/DejaVuLGCSansMono.ttf", 2, false, 0),
        text(12, "data/fonts/DejaVuLGCSans.ttf", 2, false, 0),
        large(32, "data/fonts/GentiumArchaic.ttf", 1, false, 0),
        rune(16, "data/fonts/ScourgeRunes.ttf", 1, false, 0),
        ;

        private float size;
        private String file;
        private GFont gfont;
        private float kerning;
        private int repeat;
        private boolean shadow;

        ScourgeFont(float size, String file, int repeat, boolean shadow, float kerning) {
            this.size = size;
            this.file = file;
            this.repeat = repeat;
            this.shadow = shadow;
            this.kerning = kerning;
        }

        public float getSize() {
            return size;
        }

        public String getFile() {
            return file;
        }

        public GFont getGFont() {
            if(gfont == null) {
                try {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, new File(file));
                    gfont = new GFont(font, size, repeat, shadow);
                } catch(Exception exc) {
                    throw new RuntimeException(exc);
                }
            }
            return gfont;
        }

        public float getKerning() {
            return kerning;
        }
    }

    public static GText createLabel(int x, int y, String text, ColorRGBA color, ScourgeFont scourgeFont, boolean centered) {
        GText label = new GText(scourgeFont.getGFont(), scourgeFont.getKerning(), color);
        label.setText(text);
        Vector3 v = new Vector3(label.getTranslation());
        v.addLocal(x - (centered ? label.getWidth() / 2 : 0), y, 0);
        label.setTranslation(v);
        return label;
    }

    public static BasicText createText(int x, int y, String text, float size, int flags, ColorRGBA color, float scale) {
        BasicText label = BasicText.createDefaultTextLabel("Text", "", 16);
        label.setScale(scale);
		label.setTextColor(color);
        Vector3 v = new Vector3(label.getTranslation());
        v.addLocal(x - label.getWidth() / 2, y - label.getHeight() / 2, 0);
        label.setTranslation(v);
        return label;
    }

    public static Quad createQuad(String namePrefix, int w, int h) {
        return createQuad(namePrefix, w, h, (Texture)null);
    }

    public static Quad createQuad(String namePrefix, int w, int h, String texturePath) {
        Texture texture = null;
        if(texturePath != null) {
            texture = ShapeUtil.loadTexture(texturePath);
            texture.setWrap(Texture.WrapMode.Repeat);
            texture.setHasBorder(false);
            texture.setApply(Texture.ApplyMode.Modulate);
        }
        return createQuad(namePrefix, w, h, texture);
    }

    public static Quad createQuad(String namePrefix, int w, int h, Texture texture) {
        Quad q = new Quad(ShapeUtil.newShapeName(namePrefix), w, h);

        FloatBuffer normBuf = q.getMeshData().getNormalBuffer();
        normBuf.clear();
        normBuf.put(0).put(1).put(0);
        normBuf.put(0).put(1).put(0);
        normBuf.put(0).put(1).put(0);
        normBuf.put(0).put(1).put(0);

        if(texture != null) {
            TextureState ts = new TextureState();
            ts.setTexture(texture, 0);
            q.setRenderState(ts);
        }

        BlendState as = new BlendState();
        as.setBlendEnabled(true);
        as.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        as.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        as.setTestEnabled(true);
        as.setTestFunction(BlendState.TestFunction.GreaterThan);
        as.setEnabled(true);
        q.setRenderState(as);

        return q;
    }
}
