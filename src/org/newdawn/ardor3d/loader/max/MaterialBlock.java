/*
 * Copyright (c) 2003-2009 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.newdawn.ardor3d.loader.max;

import java.io.DataInput;
import java.io.IOException;
import java.util.logging.Logger;

import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;

/**
 * Started Date: Jul 2, 2004<br><br>
 *
 *
 * type == afff == MAT_BLOCK<br>
 * parent == 3d3d == EDIT_3DS<br>
 *
 * @author Jack Lindamood
 */
class MaterialBlock extends ChunkerClass {
    private static final Logger logger = Logger.getLogger(MaterialBlock.class
            .getName());

    String name;
    MaterialState myMatState;
    TextureState myTexState;
    WireframeState myWireState;

    public MaterialBlock(DataInput myIn, ChunkHeader i) throws IOException {
        super (myIn);

        setHeader(i);
        initializeVariables();
        chunk();
    }

    protected void initializeVariables(){
        myMatState=new MaterialState();
        myMatState.setEnabled(false);
        myWireState=new WireframeState();
        myWireState.setEnabled(false);
        myTexState=new TextureState();
        myTexState.setEnabled(false);
    }

    protected boolean processChildChunk(ChunkHeader i) throws IOException {
        switch (i.type){
            case MAT_NAME:
                readMatName();
                return true;

            case MAT_AMB_COLOR:
                myMatState.setAmbient(new ColorChunk(myIn,i).getBestColor());
                myMatState.setEnabled(true);
                if (DEBUG || DEBUG_LIGHT) logger.info("Ambient color:" + myMatState.getAmbient());
                return true;

            case MAT_DIF_COLOR:
                new PercentChunk(myIn,i);   // ignored: scourge doesn't use it
//                myMatState.setDiffuse(new ColorChunk(myIn,i).getBestColor());
//                myMatState.setColorMaterial(ColorMaterial.Diffuse);
//                myMatState.setEnabled(true);
//                if (DEBUG || DEBUG_LIGHT) logger.info("Diffuse color:" + myMatState.getDiffuse());
                return true;

            case MAT_SPEC_CLR:
                new PercentChunk(myIn,i);   // ignored: scourge doesn't use it
//                myMatState.setSpecular(new ColorChunk(myIn,i).getBestColor());
//                myMatState.setEnabled(true);
//                if (DEBUG || DEBUG_LIGHT) logger.info("Diffuse color:" + myMatState.getSpecular());
                return true;
            case MAT_SHINE:
                myMatState.setShininess(128*new PercentChunk(myIn,i).percent);
                myMatState.setEnabled(true);
                if (DEBUG || DEBUG_LIGHT) logger.info("Shinniness:" + myMatState.getShininess());
                return true;
            case MAT_SHINE_STR:
                new PercentChunk(myIn,i);   // ignored / Unknown use
                return true;
            case MAT_ALPHA:
                float alpha = 1 - new PercentChunk(myIn, i).percent;
                ColorRGBA color = new ColorRGBA();
                myMatState.getDiffuse().add(0,0,0,alpha, color);
                myMatState.setDiffuse(color);

                color = new ColorRGBA();
                myMatState.getEmissive().add(0,0,0,alpha, color);
                myMatState.setEmissive(color);

                color = new ColorRGBA();
                myMatState.getAmbient().add(0,0,0,alpha, color);
                myMatState.setAmbient(color);

                myMatState.setEnabled(true);
                if (DEBUG || DEBUG_LIGHT) logger.info("Alpha:" + alpha);
                return true;
            case MAT_ALPHA_FAL:
                new PercentChunk(myIn,i);   // ignored / Unknown use
                return true;
            case MAT_REF_BLUR:
                new PercentChunk(myIn,i);   // Reflective ignored
                return true;
            case MAT_SHADING:
                myIn.readShort();           // Shading ignored
                return true;
            case MAT_SELF_ILUM:
                new PercentChunk(myIn,i);   // Self illumination ignored
                return true;
            case MAT_WIRE_SIZE:
                myWireState.setLineWidth(myIn.readFloat());
                if (DEBUG || DEBUG_LIGHT) logger.info("Wireframe size:" + myWireState.getLineWidth());
                return true;
            case IN_TRANC_FLAG:
                return true;    //  Unknown use for this flag
            case TEXMAP_ONE:
                readTextureMapOne(i);
                return true;
            case MAT_TEX_BUMPMAP:
                readTextureBumpMap(i);
                return true;
            case MAT_SOFTEN:
                //if (DEBUG) logger.info("Material soften is true");
                return true;    // Unknown flag
            case MAT_SXP_TEXT_DATA:
                myIn.readFully(new byte[i.length]);   // unknown
                return true;
            case MAT_REFL_BLUR:
                if (DEBUG) logger.info("Material blur present");
                // Unused Flag
                return true;
            case MAT_WIRE_ABS:
                if (DEBUG) logger.info("Using absolute wire in units");
                // Unknown Flag
                return true;
            case MAT_REFLECT_MAP:
                readReflectMap(i);
                return true;
            case MAT_SXP_BUMP_DATA:
                myIn.readFully(new byte[i.length]);   // unknown
                return true;
            case MAT_TWO_SIDED:
				new PercentChunk(myIn,i);
                //myMatState.setColorMaterialFace(MaterialState.MaterialFace.FrontAndBack);
                // On by default
                return true;
            case MAT_FALLOFF:
                if (DEBUG) logger.info("Using material falloff");
                // Unknown flag
                return true;
            case MAT_WIREFRAME_ON:
                if (DEBUG) logger.info("Material wireframe is active");
                myWireState.setEnabled(true);
                return true;
            case MAT_TEX2MAP:
                readTextureMapTwo(i);
                return true;
            default:
                return false;
        }
    }

    private void readTextureBumpMap(ChunkHeader i) throws IOException {
        TextureChunk tc = new TextureChunk(myIn, i);
        if(!DISABLE_BUMP_MAPPING) {
                        Texture t = createTexture(tc);
                        t.setApply(Texture.ApplyMode.Combine);
                        t.setCombineFuncRGB(Texture.CombinerFunctionRGB.Dot3RGB);
                        t.setCombineSrc0RGB(Texture.CombinerSource.CurrentTexture);
                        t.setCombineSrc1RGB(Texture.CombinerSource.PrimaryColor);
                        myTexState.setTexture(t, 0); // Set as fourth texture-unit
        }
    }

    private void readTextureMapOne(ChunkHeader i) throws IOException {
        TextureChunk tc=new TextureChunk(myIn,i);
                Texture t = createTexture(tc);
                if(!DISABLE_BUMP_MAPPING) {
                        t.setApply(Texture.ApplyMode.Combine);
                        t.setCombineFuncRGB(Texture.CombinerFunctionRGB.Modulate);
                        t.setCombineSrc0RGB(Texture.CombinerSource.Previous);
                        t.setCombineSrc1RGB(Texture.CombinerSource.CurrentTexture);
                }
        myTexState.setTexture(t, 1); // Set as first texture-unit
    }

    private void readTextureMapTwo(ChunkHeader i) throws IOException {
        TextureChunk tc=new TextureChunk(myIn,i);
                Texture t = createTexture(tc);
        myTexState.setTexture(t, 2); // Set as the second texture-unit
    }

    private void readReflectMap(ChunkHeader i) throws IOException {
        TextureChunk tc=new TextureChunk(myIn,i);
                Texture t = createTexture(tc);
        myTexState.setTexture(t, 3); // Set as third texture-unit
    }

    private Texture createTexture(TextureChunk tc) {
        ResourceSource resourceSource = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, tc.texName);
//        Texture t = TextureManager.load(resourceSource, Texture.MinificationFilter.BilinearNearestMipMap, TextureStoreFormat.GuessNoCompressedFormat, true);
        Texture t = TextureManager.load(resourceSource, Texture.MinificationFilter.Trilinear, TextureStoreFormat.GuessNoCompressedFormat, true);
//        t.setAnisotropicFilterPercent(0.0f);
        t.setMagnificationFilter(Texture.MagnificationFilter.Bilinear);

                t.setWrap(Texture.WrapMode.Repeat);
                float vScale = tc.vScale;
                float uScale = tc.uScale;
                if (uScale == 0) {
                        uScale = 1;
                }
                if (vScale == 0) {
                        vScale = 1;
                }
                Transform transform = new Transform();
                transform.setScale(uScale, vScale, 1);
                t.setTextureMatrix(transform.getHomogeneousMatrix(null));
//                t.setScale(new Vector3(uScale, vScale, 1));

                myTexState.setEnabled(true);
                return t;
        }

    private void readMatName() throws IOException{
        name=readcStr();
        if (DEBUG || DEBUG_LIGHT) logger.info("read material name:" + name);
    }
}
 