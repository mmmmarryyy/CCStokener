package org.creavi.engine.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import org.creavi.engine.resource.ResourceLocator;
import org.creavi.engine.util.StringRecord;
import com.jme.math.Quaternion;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.scene.TexCoords;
import com.jme.scene.TriMesh;
import com.jme.util.export.binary.BinaryExporter;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.geom.BufferUtils;

public class ModelDefinitionFactory {

    private static ModelDefinitionFactory modelDefinitionFactory;

    public static synchronized ModelDefinitionFactory getInstance() {
        if (modelDefinitionFactory == null) {
            modelDefinitionFactory = new ModelDefinitionFactory();
        }
        return modelDefinitionFactory;
    }

    private HashMap<String, ModelDefinition> models;

    private ModelDefinitionFactory() {
        models = new HashMap<String, ModelDefinition>();
    }

    public ModelDefinition getModelDefinition(String name) {
        if (models.containsKey(name)) {
            return models.get(name);
        } else {
            ModelDefinition model = buildModel(name);
            models.put(name, model);
            return model;
        }
    }

    private ModelDefinition buildModel(String name) {
        ModelDefinition model = null;
        URL url = ResourceLocator.locateBinaryModel(name);
        InputStream is = null;
        if (url == null) {
            url = ResourceLocator.locateTextModel(name);
            try {
                is = url.openStream();
                model = buildModelFromText(name, is);
                File file = ResourceLocator.replaceExtension(url, ResourceLocator.BINARY_MODEL_EXTENSION);
                BinaryExporter.getInstance().save(model, file);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            try {
                is = url.openStream();
                model = (ModelDefinition) BinaryImporter.getInstance().load(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return model;
    }

    private ModelDefinition buildModelFromText(String name, InputStream is) throws IOException {
        ModelDefinition model = new ModelDefinition(name);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String linea = br.readLine();
        assert (linea.startsWith("model"));
        StringRecord record = new StringRecord(linea);
        int totalMeshes = record.getInt(1);
        int totalJoins = record.getInt(2);
        int keyframes = record.getInt(3);
        float frameMultiplier = record.getFloat(4);
        for (int meshIdx = 0; meshIdx < totalMeshes; meshIdx++) {
            MeshDefinition mesh = new MeshDefinition(meshIdx);
            linea = br.readLine();
            assert (linea.startsWith("mesh-def"));
            record = new StringRecord(linea);
            int faceCount = record.getInt(1);
            for (int keyframeIdx = 0; keyframeIdx < keyframes; keyframeIdx++) {
                linea = br.readLine();
                assert (linea.startsWith("keyframe"));
                record = new StringRecord(linea);
                float keyframeTime = record.getFloat(1);
                Vector3f[] vertexes = new Vector3f[3 * faceCount];
                int[] faces = new int[3 * faceCount];
                Vector3f[] normals = new Vector3f[3 * faceCount];
                Vector2f[] uvcoords = new Vector2f[3 * faceCount];
                for (int faceIdx = 0; faceIdx < faceCount; faceIdx++) {
                    for (int vertexIdx = 0; vertexIdx < 3; vertexIdx++) {
                        linea = br.readLine();
                        assert (linea.startsWith("vertex"));
                        record = new StringRecord(linea);
                        float x = record.getFloat(1);
                        float y = record.getFloat(2);
                        float z = record.getFloat(3);
                        float nX = record.getFloat(4);
                        float nY = record.getFloat(5);
                        float nZ = record.getFloat(6);
                        float u = record.getFloat(7);
                        float v = record.getFloat(8);
                        vertexes[faceIdx * 3 + vertexIdx] = new Vector3f(x, y, z);
                        normals[faceIdx * 3 + vertexIdx] = new Vector3f(nX, nY, nZ);
                        uvcoords[faceIdx * 3 + vertexIdx] = new Vector2f(u, v);
                    }
                    int v1 = faceIdx * 3;
                    int v2 = faceIdx * 3 + 1;
                    int v3 = faceIdx * 3 + 2;
                    faces[faceIdx * 3 + 0] = v1;
                    faces[faceIdx * 3 + 1] = v2;
                    faces[faceIdx * 3 + 2] = v3;
                }
                String meshName = name + "/mesh" + meshIdx + "/" + keyframeTime;
                TriMesh triMesh = new TriMesh(meshName, BufferUtils.createFloatBuffer(vertexes), BufferUtils.createFloatBuffer(normals), null, new TexCoords(BufferUtils.createFloatBuffer(uvcoords)), BufferUtils.createIntBuffer(faces));
                mesh.getKeyframes().add(new MeshPart(keyframeTime, triMesh));
            }
            mesh.setTexture(name);
            model.getMeshes().add(mesh);
        }
        for (int joinIdx = 0; joinIdx < totalJoins; joinIdx++) {
            linea = br.readLine();
            assert (linea.startsWith("join-def"));
            record = new StringRecord(linea);
            String joinName = record.getString(1);
            JoinPointDefinition join = new JoinPointDefinition(joinName);
            for (int keyframeIdx = 0; keyframeIdx < keyframes; keyframeIdx++) {
                linea = br.readLine();
                assert (linea.startsWith("keyframe"));
                record = new StringRecord(linea);
                float keyframeTime = record.getFloat(1);
                float lx = record.getFloat(2);
                float ly = record.getFloat(3);
                float lz = record.getFloat(4);
                float rx = record.getFloat(5);
                float ry = record.getFloat(6);
                float rz = record.getFloat(7);
                Quaternion q = new Quaternion();
                q.fromAngles(rx, ry, rz);
                JoinPointPart jpp = new JoinPointPart(keyframeTime, new Vector3f(lx, ly, lz), q);
                join.getKeyframes().add(jpp);
            }
            model.getJoinPoints().add(join);
            model.setFrameMultiplier(frameMultiplier);
        }
        return model;
    }
}
