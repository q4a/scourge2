/**
 * Version 1.1
 *
 * Copyright (c) New Dawn Software 2009 All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation must include acknowledgment of the original
 *    author(s) and information for obtaining the original distribution package.
 *    Alternately, this acknowledgment may appear in the software itself, if and
 *    wherever such third-party acknowledgments normally appear.
 *
 * 4. The name of the author(s) must not be used to endorse or promote products
 *    derived from this software without prior written permission.
 *
 * 5. Works derived from the software covered by this license must be free of
 *    charge, unless written permission has been obtained from the author(s).
 *    The derived work must include acknowledgment of the original author(s)
 *    and information for obtaining the original distribution package.
 *    Alternately, this acknowledgment may appear in the software itself, if and
 *    wherever such third-party acknowledgments normally appear. The original
 *    must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.newdawn.ardor3d.loader.max;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import javax.swing.text.NumberFormatter;

import org.apache.commons.io.FileUtils;
//import org.newdawn.ardor3d.controller.AlwaysUpCameraControl;
//import org.newdawn.ardor3d.loader.max.TDSFile;
//import org.newdawn.ardor3d.resource.ClasspathResourceLocator;
//import org.newdawn.ardor3d.ui.FPSCounter;

import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.NativeCanvas;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.lwjgl.LwjglCanvas;
import com.ardor3d.framework.lwjgl.LwjglCanvasRenderer;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.image.util.AWTImageLoader;
import com.ardor3d.input.Key;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.DummyControllerWrapper;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.lwjgl.LwjglKeyboardWrapper;
import com.ardor3d.input.lwjgl.LwjglMouseWrapper;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.extension.CameraNode;
import com.ardor3d.scenegraph.extension.Skybox;
import com.ardor3d.scenegraph.extension.Skybox.Face;
import com.ardor3d.scenegraph.hint.DataMode;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.AxisRods;
import com.ardor3d.scenegraph.visitor.DeleteVBOsVisitor;
import com.ardor3d.ui.text.BMFont;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.LittleEndianDataInput;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.Timer;
import com.ardor3d.util.export.binary.BinaryExporter;
import com.ardor3d.util.geom.Debugger;
import com.ardor3d.util.resource.ResourceLocator;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;
import com.ardor3d.util.resource.SimpleResourceLocator;
import com.ardor3d.util.scenegraph.CompileOptions;
import com.ardor3d.util.scenegraph.SceneCompiler;

public class MaxModelViewer implements Scene {

    private NativeCanvas canvas;
    private final Node rootNode = new Node();
    private final Timer timer = new Timer();
    private boolean exit = false;
    private LogicalLayer logicalLayer;
    private PhysicalLayer physicalLayer;
    private double angle;
//        private AlwaysUpCameraControl cameraController;
    private CameraNode cameraNode;
    private List<Node> models = new ArrayList<Node>();
    private int vboMode;

    public MaxModelViewer() throws URISyntaxException, IOException {
        AWTImageLoader.registerLoader();
    }

    public void start() throws URISyntaxException, IOException {
        DisplaySettings windowSettings = new DisplaySettings(1024,768,24,2);

        final LwjglCanvasRenderer canvasRenderer = new LwjglCanvasRenderer(this);
        canvas = new LwjglCanvas(canvasRenderer, windowSettings);

        logicalLayer = new LogicalLayer();
        physicalLayer = new PhysicalLayer(new LwjglKeyboardWrapper(), new LwjglMouseWrapper(), new DummyControllerWrapper(), (LwjglCanvas)canvas);
        logicalLayer.registerInput(canvas, physicalLayer);

        canvas.setTitle("DVSF Model converter");

        canvas.init();
//        cameraNode = new CameraNode("Cam node" , canvas.getCanvasRenderer().getCamera());
    //        cameraController = new AlwaysUpCameraControl(logicalLayer, Vector3.UNIT_Z, 0, 0, 10);
//        rootNode.attachChild(cameraNode);

//        ResourceLocator modelResourceLoader = new ClasspathResourceLocator("models");
//        ResourceLocator textureLoader = new ResourceLocator() {
//        private ResourceLocator delegateLoader = new ClasspathResourceLocator("models");
//                public ResourceSource locateResource(String resourceName) {
//                        //resourceName = resourceName.toLowerCase().replace(".jpg", ".tga");
//                        resourceName = resourceName.toLowerCase().replace("sf-bump", "sf-norm");
//                        return delegateLoader.locateResource(resourceName);
//                }
//        };
//        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MODEL, modelResourceLoader);
//        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, textureLoader);
        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE,
                                               new SimpleResourceLocator(new URL("file:///Users/gabor/ardor3d-svn/trunk/data/textures")));
        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MODEL,
                                               new SimpleResourceLocator(new URL("file:///Users/gabor/ardor3d-svn/trunk")));


        Node model = load3DSModel("./data/3ds/vase.3ds");
        rootNode.attachChild(model);

        canvas.getCanvasRenderer().getCamera().setLocation(5, 5, 10);
        canvas.getCanvasRenderer().getCamera().lookAt(model.getTranslation(), Vector3.UNIT_Y);

//        rootNode.attachChild(new AxisRods("AxisRods", true, 30, 0.05));

//        SimpleResourceLocator backgroundResourceLoader = new SimpleResourceLocator(Thread.currentThread().getContextClassLoader().getResource("backgrounds"));
//        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, backgroundResourceLoader);

        final ZBufferState buf = new ZBufferState();
        buf.setEnabled(true);
        buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        rootNode.setRenderState(buf);

        // ---- LIGHTS
        /** Set up a basic, default light. */
        final PointLight light = new PointLight();
    //        light.setSpecular(ColorRGBA.GREEN);
    //        light.setDiffuse(ColorRGBA.GREEN);
    //        light.setAmbient(ColorRGBA.GREEN);
        light.setSpecular(ColorRGBA.GREEN);
        light.setDiffuse(new ColorRGBA(0.5f, 0.75f, 0.5f, 0.75f));
        light.setAmbient(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
        light.setLocation(new Vector3(900, 900, 900));

    //        DirectionalLight light = new DirectionalLight();
    //        light.setAmbient(new ColorRGBA(0.75f, 0.75f, 0.75f, 1));
    //        light.setDiffuse(new ColorRGBA(1, 1, 1, 1));
    //        light.setEnabled(true);

        light.setEnabled(true);

        /** Attach the light to a lightState and the lightState to rootNode. */
        LightState lightState = new LightState();
        lightState.setEnabled(true);
        lightState.attach(light);
        rootNode.setRenderState(lightState);

        CullState cs = new CullState();
        cs.setCullFace(CullState.Face.Back);
        cs.setEnabled(true);
        rootNode.setRenderState(cs);

        //rootNode.getSceneHints().setDataMode(DataMode.VBO);
        Debugger.setBoundsColor(new ColorRGBA(ColorRGBA.WHITE));
        Debugger.NORMAL_COLOR_BASE.set(ColorRGBA.WHITE);
        Debugger.NORMAL_COLOR_TIP.set(ColorRGBA.WHITE);

        while(!exit) {
            if (canvas.isClosing()) {
                exit = true;
            }
            timer.update();
            logicalLayer.checkTriggers(timer.getTimePerFrame());

                //Force this to be called first
    //                cameraController.update(timer.getTimePerFrame(), cameraNode);

    //              angle += 2 * timer.getTimePerFrame();
    //              LightState lightState = (LightState) rootNode.getLocalRenderState(StateType.Light);
    //              ((DirectionalLight)lightState.get(0)).setDirection(new Vector3(2.0f *
    //                              Math.cos(angle), 2.0f * Math.sin(angle), 1.5f));

            // Update controllers/render states/transforms/bounds for rootNode.
            rootNode.updateGeometricState(timer.getTimePerFrame(), true);
            canvas.draw(null);
            Thread.yield();
        }
//        ContextGarbageCollector.doFinalCleanup(canvas.getCanvasRenderer().getRenderer());
        canvas.close();
    }

    private Node load3DSModel(String modelName) throws IOException {
        LittleEndianDataInput dataInput = new LittleEndianDataInput(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL,modelName).openStream());
        TDSFile tdsFile = new TDSFile(dataInput);
        Node modelNode = tdsFile.buildScene();
        Quaternion modelRotation = new Quaternion();
        modelRotation.fromEulerAngles(MathUtils.PI,0,-MathUtils.HALF_PI);
        modelNode.setRotation(modelRotation);
        modelNode.setScale(0.01);
        Node transformNode = new Node("modelName transform node");
        transformNode.attachChild(modelNode);
        //transformNode.attachChild(new AxisRods("AxisRods", true, 3, 0.05));
        return transformNode;
    }

        /**
         * @param args
         */
        public static void main(String[] args) {
            try {
                new MaxModelViewer().start();
            } catch (URISyntaxException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
            } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
            }
        }

        public PickResults doPick(Ray3 pickRay) {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean renderUnto(Renderer renderer) {
            if (!canvas.isClosing()) {

                // Draw the root and all its children.
                renderer.draw(rootNode);
//                Debugger.drawAxis(rootNode, renderer);
//                Debugger.drawNormals(rootNode, renderer);
//                Debugger.drawBounds(rootNode, renderer);

                return true;
            }
            return false;
        }
}
