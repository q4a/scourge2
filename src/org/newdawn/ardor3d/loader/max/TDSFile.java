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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

//import org.newdawn.ardor3d.controller.BumpMapColourController;

import com.ardor3d.light.Light;
import com.ardor3d.light.PointLight;
import com.ardor3d.light.SpotLight;
import com.ardor3d.math.*;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.scenegraph.FloatBufferDataUtil;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.LittleEndianDataInput;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.geom.NormalGenerator;
import com.ardor3d.util.resource.ResourceLocatorTool;

/**
 * Started Date: Jul 2, 2004<br><br>
 *
 * type=4d4d=MAIN_3DS
 * parent=nothing
 * @author Jack Lindamood
 */
public class TDSFile extends ChunkerClass{
    private static final Logger logger = Logger.getLogger(TDSFile.class
            .getName());

    private EditableObjectChunk objects=null;
    private KeyframeChunk keyframes=null;
    private List<Spatial> spatialNodes;
    private List<String> spatialNodesNames;
    private List<Light> spatialLights;
    private BlendState alpha;


    public static Node load3DSModel(String modelName) throws IOException {
        LittleEndianDataInput dataInput = new LittleEndianDataInput(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL,modelName).openStream());
        TDSFile tdsFile = new TDSFile(dataInput);
        Node modelNode = tdsFile.buildScene();
//        Quaternion modelRotation = new Quaternion();
//        modelRotation.fromEulerAngles(MathUtils.PI,0,-MathUtils.HALF_PI);
//        modelNode.setRotation(modelRotation);
//        modelNode.setScale(0.01);
        return modelNode;
    }

    public TDSFile(DataInput myIn) throws IOException {
        super(myIn);
        ChunkHeader c=new ChunkHeader(myIn);
        if (c.type!=MAIN_3DS)
            throw new IOException("Header doesn't match 0x4D4D; Header=" + Integer.toHexString(c.type));
        c.length-=6;
        setHeader(c);

        chunk();
    }


    protected boolean processChildChunk(ChunkHeader i) throws IOException {
        switch(i.type){
            case TDS_VERSION:
                readVersion();
                return true;
            case EDIT_3DS:
                objects=new EditableObjectChunk(myIn, i);
                return true;
            case KEYFRAMES:
                keyframes=new KeyframeChunk(myIn,i);
                return true;
            default:
                return false;
            }
    }


    private void readVersion() throws IOException{
        int version=myIn.readInt();
        if (DEBUG || DEBUG_LIGHT) logger.info("Version:" + version);
    }

    public Node buildScene() throws IOException {
        buildObject();
        putTranslations();
        Node uberNode=new Node("TDS Scene");
        for ( Spatial spatialNode : spatialNodes ) {
            if ( spatialNode != null ) {
                Spatial toAttach = spatialNode;
                if ( toAttach.getParent() == null ) {
                    uberNode.attachChild( toAttach );
                }
            }
        }
        LightState ls = null;
        for ( Light spatialLight : spatialLights ) {
            if ( ls == null ) {
                ls = new LightState();
                ls.setEnabled( true );
            }
            ls.attach( spatialLight );
        }
        if (ls!=null)
            uberNode.setRenderState(ls);


        if (keyframes!=null){
            //st.interpolateMissing();
            //if (st.keyframes.size() == 1) {
                // one keyframe: update controller once and disregard it
            //    st.update(0);
            //}
            //else {
            //    // multiple keyframes: add controller to node
            //    uberNode.addController(st);
            //    st.setActive(true);
            //}

        }

//        if(!DISABLE_BUMP_MAPPING) {
//                uberNode.addController(new BumpMapColourController());
//        }
        return uberNode;
    }

    private void putTranslations() {
        if (keyframes==null) return;
        int spatialCount=0;
        for ( Spatial spatialNode : spatialNodes ) {
            if ( spatialNode != null ) {
                spatialCount++;
            }
        }
        //st=new SpatialTransformer(spatialCount);
        spatialCount=0;
        for (int i=0;i<spatialNodes.size();i++){
            if (spatialNodes.get(i) != null ){
                // hand the Spatial over to the SpatialTransformer
                // the parent ID is not passed here, as that would produce wrong results
                // because of the ST applying hierarchichal transformations, which the
                // scene graph applies anyway
                //st.setObject( spatialNodes.get(i),spatialCount++,-1);//getParentIndex(i));
            }
        }
        Object[] keysetKeyframe=keyframes.objKeyframes.keySet().toArray();
        for ( Object aKeysetKeyframe : keysetKeyframe ) {
            KeyframeInfoChunk thisOne = keyframes.objKeyframes.get( aKeysetKeyframe );
            if ( "$$$DUMMY".equals( thisOne.name ) ) {
                continue;
            }
            //int indexInST = findIndex( thisOne.name );
            for ( Object aTrack : thisOne.track ) {
                KeyframeInfoChunk.KeyPointInTime thisTime = (KeyframeInfoChunk.KeyPointInTime) aTrack;
                //KeyFrame keyFrame = new KeyFrame(thisTime.frame, InterpolationMethod.LINEAR, )
                if ( thisTime.rot != null ) {
                    //st.setRotation( indexInST, thisTime.frame, thisTime.rot );
                }
                if ( thisTime.position != null ) {
                    //st.setPosition( indexInST, thisTime.frame, thisTime.position );
                }
                if ( thisTime.scale != null ) {
                    //st.setScale( indexInST, thisTime.frame, thisTime.scale );
                }
            }
        }
        //st.setSpeed(10);

    }


//    private int findIndex(String name) {
//        int j=0;
//        for (int i=0;i<spatialNodesNames.size();i++){
//            if (spatialNodesNames.get(i).equals(name)) return j;
//            if (spatialNodes.get(i) != null ) j++;
//        }
//        throw new RuntimeException("Logic error.  Unknown keyframe name " + name);
//    }

//    private int getParentIndex(int objectIndex) {
//        if (keyframes.objKeyframes.get(spatialNodesNames.get(objectIndex)) ==null)
//            return -2;
//        short parentID=keyframes.objKeyframes.get(spatialNodesNames.get(objectIndex)).parent;
//        if (parentID==-1) return -1;
//        Object[] objs=keyframes.objKeyframes.keySet().toArray();
//        for (int i=0;i<objs.length;i++){
//            if (keyframes.objKeyframes.get(objs[i]).myID==parentID)
//                return i;
//        }
//        throw new RuntimeException("Logic error.  Unknown parent ID for " + objectIndex);
//    }

    private void buildObject() throws IOException {
        spatialNodes=new ArrayList<Spatial>();   // An ArrayList of Nodes
        spatialLights=new ArrayList<Light>();
        spatialNodesNames=new ArrayList<String>();   // Their names
        Map<Short, Node> nodesByID = new HashMap<Short, Node>();
        if ( keyframes != null ) {
            for ( Entry<String, KeyframeInfoChunk> entry : keyframes.objKeyframes.entrySet() ) {
                String name = entry.getKey();
                if ( !objects.namedObjects.containsKey( name ) ) {
                    KeyframeInfoChunk info = entry.getValue();
                    Node node = new Node( info.name );
                    nodesByID.put( info.myID, node );
                    spatialNodesNames.add( name );
                    spatialNodes.add( node );
                }
            }
        }
        for ( Entry<String, NamedObjectChunk> entry : objects.namedObjects.entrySet() ) {
            String objectKey = entry.getKey();
            NamedObjectChunk noc = entry.getValue();

            KeyframeInfoChunk kfInfo = null;
            if ( keyframes != null && keyframes.objKeyframes != null ) {
                kfInfo = keyframes.objKeyframes.get( objectKey );
            }
            if ( noc.whatIAm instanceof TriMeshChunk ) {
                Node myNode = new Node( objectKey );

                Spatial spatial;
                if ( kfInfo == null ) {
                    putChildMeshes( myNode, (TriMeshChunk) noc.whatIAm, new Vector3( 0, 0, 0 ) );
                    spatial = usedSpatial( myNode );
                } else {
                    putChildMeshes( myNode, (TriMeshChunk) noc.whatIAm, kfInfo.pivot == null ? new Vector3(0, 0, 0) : kfInfo.pivot );
                    spatial = myNode;
                    nodesByID.put( kfInfo.myID, myNode );
                }

                spatialNodesNames.add( noc.name );
                spatialNodes.add( spatial );

            } else if ( noc.whatIAm instanceof LightChunk ) {
                spatialLights.add( createChildLight( (LightChunk) noc.whatIAm ) );
            }
        }

        // build hierarchy
        if ( keyframes != null ) {
            for ( Entry<String,KeyframeInfoChunk> entry : keyframes.objKeyframes.entrySet() ) {
                KeyframeInfoChunk kfInfo = entry.getValue();
                if ( kfInfo.parent != -1 ) {
                    Node node = nodesByID.get( kfInfo.myID );
                    if ( node != null ) {
                        Node parentNode = nodesByID.get( kfInfo.parent );
                        if ( parentNode != null ) {
                            parentNode.attachChild( node );
                        } else {
                            throw new RuntimeException( "Parent node (id=" + kfInfo.parent + ") not foudn!" );
                        }
                    }
                }
            }
        }
    }

    private Spatial usedSpatial(Node myNode) {
        Spatial spatial;
        if (myNode.getChildren().size()==1){
            myNode.getChild(0).setName(myNode.getName());
            spatial = myNode.getChild(0);
            myNode.detachChild( spatial );
        } else {
            spatial = myNode;
        }
        return spatial;
    }

    private Light createChildLight(LightChunk lightChunk) {
        // Light attenuation does not work right.
        if (lightChunk.spotInfo!=null){
            SpotLight toReturn=new SpotLight();
            toReturn.setLocation(lightChunk.myLoc);
            toReturn.setDiffuse(lightChunk.lightColor);
            toReturn.setAmbient(ColorRGBA.BLACK);
            toReturn.setSpecular(ColorRGBA.WHITE);
            Vector3 tempDir=new Vector3();
            lightChunk.myLoc.subtract(lightChunk.spotInfo.target,tempDir);
            tempDir.multiplyLocal(-1);
            tempDir.normalizeLocal();
            toReturn.setDirection(tempDir);
//            toReturn.setAngle(lightChunk.spotInfo.fallOff);  // Get this working correctly
            toReturn.setAngle(180);  // FIXME: Get this working correctly, it's just a hack
            toReturn.setEnabled(true);
            return toReturn;
        }

        PointLight toReturn=new PointLight();
        toReturn.setLocation(lightChunk.myLoc);
        toReturn.setDiffuse(lightChunk.lightColor);
        toReturn.setAmbient(ColorRGBA.BLACK);
        toReturn.setSpecular(ColorRGBA.WHITE);
        toReturn.setEnabled(true);
        return toReturn;

    }

    private void putChildMeshes(Node parentNode, TriMeshChunk whatIAm,Vector3 pivotLoc) throws IOException {
        FacesChunk myFace=whatIAm.face;
        if (myFace==null) return;
        boolean[] faceHasMaterial=new boolean[myFace.nFaces];
        int noMaterialCount=myFace.nFaces;
        ArrayList<Vector3> normals=new ArrayList<Vector3>(myFace.nFaces);
        ArrayList<Vector3> vertexes=new ArrayList<Vector3>(myFace.nFaces);
        Vector3 tempNormal=new Vector3();
        ArrayList<Vector2> texCoords=new ArrayList<Vector2>(myFace.nFaces);
        if (whatIAm.coordSystem==null)
            whatIAm.coordSystem=new Transform();
        whatIAm.coordSystem.invert(whatIAm.coordSystem).invert(whatIAm.coordSystem);
        for ( Vector3 vertexe : whatIAm.vertexes ) {
            whatIAm.coordSystem.applyForward( vertexe );
            vertexe.subtractLocal( pivotLoc );
        }
        Vector3[] faceNormals=new Vector3[myFace.nFaces];
        calculateFaceNormals(faceNormals,whatIAm.vertexes,whatIAm.face.faces);

        // Precalculate nextTo[vertex][0...i] <--->
        // whatIAm.vertexes[vertex] is next to face nextTo[vertex][0] & nextTo[vertex][i]
        if (DEBUG || DEBUG_LIGHT) logger.info("Precaching");
        int[] vertexCount=new int[whatIAm.vertexes.length];
        for (int i=0;i<myFace.nFaces;i++){
            for (int j=0;j<3;j++){
                vertexCount[myFace.faces[i][j]]++;
            }
        }
        int[][] realNextFaces=new int[whatIAm.vertexes.length][];
        for (int i=0;i<realNextFaces.length;i++)
            realNextFaces[i]=new int[vertexCount[i]];
        int vertexIndex;
        for (int i=0;i<myFace.nFaces;i++){
            for (int j=0;j<3;j++){
                vertexIndex=myFace.faces[i][j];
                realNextFaces[vertexIndex][--vertexCount[vertexIndex]]=i;
            }
        }


        if (DEBUG || DEBUG_LIGHT) logger.info("Precaching done");



        int[] indexes=new int[myFace.nFaces*3];

        for (int i=0;i<myFace.materialIndexes.size();i++){  // For every original material
            String matName=myFace.materialNames.get(i);
            int[] appliedFacesIndexes=myFace.materialIndexes.get(i);
            if (DEBUG_LIGHT || DEBUG) logger.info("On material " + matName + " with " + appliedFacesIndexes.length + " faces.");
            if (appliedFacesIndexes.length!=0){ // If it's got something make a new trimesh for it
                Mesh mesh = new Mesh( parentNode.getName() + "##" + i );
                MeshData meshData = mesh.getMeshData();
                meshData.setIndexMode(IndexMode.Triangles);
                normals.clear();
                vertexes.clear();
                texCoords.clear();
                int curPosition = 0;
                for (int j=0;j<appliedFacesIndexes.length;j++){ // Look thru every face in that new TriMesh
                    if (DEBUG) if (j%500==0) logger.info("Face:" + j);
                    int actuallFace=appliedFacesIndexes[j];
                    if ( !faceHasMaterial[actuallFace] ){
                        faceHasMaterial[actuallFace]=true;
                        noMaterialCount--;
                    }
                    for (int k=0;k<3;k++){                      //   and every vertex in that face
                        // what faces contain this vertex index? If they do and are in the same SG, average
                        vertexIndex=myFace.faces[actuallFace][k];
                        tempNormal.set(faceNormals[actuallFace]);
                        calcFacesWithVertexAndSmoothGroup(realNextFaces[vertexIndex],faceNormals,myFace,tempNormal,actuallFace);
                        // Now can I just index this Vertex/tempNormal combination?
//                        int l;
//                        Vector3f vertexValue=whatIAm.vertexes[vertexIndex];
//                        for (l=0;l<normals.size();l++) {
//                            if (normals.get(l).equals(tempNormal) && vertexes.get(l).equals(vertexValue)) {
//                                break;
//                            }
//                        }
//                        if (l==normals.size()){ // if new
                            normals.add(new Vector3(tempNormal));
                            vertexes.add(whatIAm.vertexes[vertexIndex]);
                            if (whatIAm.texCoords!=null)
                                texCoords.add(whatIAm.texCoords[vertexIndex]);
                            indexes[curPosition++]=normals.size()-1;
//                        } else { // if old
//                            indexes[curPosition++]=l;
//                        }
                    }
                }
                Vector3[] newVerts=new Vector3[vertexes.size()];
                for (int indexV=0;indexV<newVerts.length;indexV++)
                    newVerts[indexV]=vertexes.get(indexV);
                meshData.setVertexBuffer(BufferUtils.createFloatBuffer(newVerts));
                meshData.setNormalBuffer(BufferUtils.createFloatBuffer(normals
                        .toArray(new Vector3[] {})));
                if (whatIAm.texCoords != null) {
                        meshData.setTextureCoords(FloatBufferDataUtil.makeNew(texCoords
                            .toArray(new Vector2[] {})),0);
                        meshData.copyTextureCoordinates(0, 1, 1f);
                        meshData.copyTextureCoordinates(0, 2, 1f);
                        meshData.copyTextureCoordinates(0, 3, 1f);
                }
                int[] intIndexes = new int[curPosition];
                System.arraycopy(indexes,0,intIndexes,0,curPosition);
                meshData.setIndexBuffer(BufferUtils.createIntBuffer(intIndexes));

                MaterialBlock myMaterials=objects.materialBlocks.get(matName);
                if (matName==null)
                    throw new IOException("Couldn't find the correct name of " + myMaterials);
                if (myMaterials.myMatState.isEnabled()) {
                        mesh.setRenderState(myMaterials.myMatState);
                    if ( myMaterials.myMatState.getDiffuse().getAlpha() < 1.0f ) {

                        if ( alpha == null ) {
                            alpha = new BlendState();
                            alpha.setEnabled( true );
                            alpha.setBlendEnabled( true );
                            alpha.setSourceFunction( BlendState.SourceFunction.SourceAlpha );
                            alpha.setDestinationFunction( BlendState.DestinationFunction.OneMinusSourceAlpha );
                            alpha.setTestEnabled( true );
                            alpha.setTestFunction( BlendState.TestFunction.GreaterThan );
                        }
                        mesh.setRenderState( alpha );
                    }
                }
                if (myMaterials.myTexState.isEnabled()) {
                        mesh.setRenderState(myMaterials.myTexState);
                        //mesh.addController(new BumpMapColorController());
                }
                if (myMaterials.myWireState.isEnabled())
                        mesh.setRenderState(myMaterials.myWireState);
                parentNode.attachChild(mesh);
                mesh.updateModelBound();
            }
        }
        if (noMaterialCount!=0){    // attach materialless parts
            int[] noMaterialIndexes=new int[noMaterialCount*3];
            int partCount=0;
            for (int i=0;i<whatIAm.face.nFaces;i++){
                if (!faceHasMaterial[i]){
                    noMaterialIndexes[partCount++]=myFace.faces[i][0];
                    noMaterialIndexes[partCount++]=myFace.faces[i][1];
                    noMaterialIndexes[partCount++]=myFace.faces[i][2];
                }
            }
            Mesh noMaterials=new Mesh(parentNode.getName()+"-1");
            MeshData noMaterialsData = noMaterials.getMeshData();
            noMaterialsData.setVertexBuffer(BufferUtils.createFloatBuffer(whatIAm.vertexes));
            noMaterialsData.setIndexBuffer(BufferUtils.createIntBuffer(noMaterialIndexes));
            parentNode.attachChild(noMaterials);
        }
    }

    private void calculateFaceNormals(Vector3[] faceNormals,Vector3[] vertexes,int[][] faces) {
        Vector3 tempa=new Vector3(),tempb=new Vector3();
        // Face normals
        for (int i=0;i<faceNormals.length;i++){
            tempa.set(vertexes[faces[i][0]]);  // tempa=a
            tempa.subtractLocal(vertexes[faces[i][1]]);    // tempa-=b (tempa=a-b)
            tempb.set(vertexes[faces[i][0]]);  // tempb=a
            tempb.subtractLocal(vertexes[faces[i][2]]);    // tempb-=c (tempb=a-c)
            faceNormals[i] = tempa.cross(tempb,faceNormals[i]);
            faceNormals[i].normalizeLocal();
        }
    }

    // Find all face normals for faces that contain that vertex AND are in that smoothing group.
    private void calcFacesWithVertexAndSmoothGroup(int[] thisVertexTable,Vector3[] faceNormals,FacesChunk myFace, Vector3 tempNormal, int faceIndex) {
        // tempNormal starts out with the face normal value
        int smoothingGroupValue=myFace.smoothingGroups[faceIndex];
        if (smoothingGroupValue==0)
            return; // 0 smoothing group values don't have smooth edges anywhere
        for ( int arrayFace : thisVertexTable ) {
            if ( arrayFace == faceIndex ) {
                continue;
            }
            if ( ( myFace.smoothingGroups[arrayFace] & smoothingGroupValue ) != 0 ) {
                tempNormal.addLocal( faceNormals[arrayFace] );
            }
        }
        tempNormal.normalizeLocal();
    }
}
