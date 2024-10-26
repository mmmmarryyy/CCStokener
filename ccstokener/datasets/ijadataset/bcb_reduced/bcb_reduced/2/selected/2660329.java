package br.furb.inf.tcc.util.scene;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import md5reader.MD5MeshReader;
import model.Model;
import com.jme.bounding.BoundingBox;
import com.jme.bounding.BoundingSphere;
import com.jme.scene.Node;
import com.jme.util.TextureKey;
import com.jme.util.export.binary.BinaryExporter;
import com.jme.util.export.binary.BinaryImporter;
import com.jmex.effects.particles.ParticleMesh;
import com.jmex.model.XMLparser.XMLtoBinary;
import com.jmex.model.XMLparser.Converters.Md3ToJme;
import com.jmex.model.XMLparser.Converters.ObjToJme;

/**
 * This class contains some base methods for graphics model manipulations.
 * @author Erick B Passos (Adapted by Germano Fronza)
 * @version 1.0.0.0 
 */
public class ModelUtils {

    public static void main(String[] args) {
    }

    public static Node getNodeByObj(String filePathSrc) {
        try {
            ByteArrayOutputStream BO = ModelResourceManager.getBinaryResourceByObj(filePathSrc);
            Node model = (Node) BinaryImporter.getInstance().load(new ByteArrayInputStream(BO.toByteArray()));
            model.setModelBound(new BoundingSphere());
            model.updateModelBound();
            return model;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Node carregaModeloJME(String caminho) {
        try {
            URL urlModelo = ModelUtils.class.getClassLoader().getResource(caminho);
            BufferedInputStream leitorBinario = new BufferedInputStream(urlModelo.openStream());
            Node modelo = (Node) BinaryImporter.getInstance().load(leitorBinario);
            modelo.setModelBound(new BoundingBox());
            modelo.updateModelBound();
            return modelo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public static Node getNodeByXMLJmeModel(String filePathSrc) {
        String rsrcPath = filePathSrc.substring(0, filePathSrc.lastIndexOf("/") + 1);
        com.jmex.model.XMLparser.JmeBinaryReader jbr = new com.jmex.model.XMLparser.JmeBinaryReader();
        jbr.setProperty("texclasspath", rsrcPath);
        jbr.setProperty("bound", "box");
        ByteArrayOutputStream binaryOutput = ModelResourceManager.getBinaryResourceByXML(filePathSrc);
        try {
            return jbr.loadBinaryFormat(new ByteArrayInputStream(binaryOutput.toByteArray()));
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Node getNodeByMd3(String filePathSrc) {
        Md3ToJme converter = new Md3ToJme();
        try {
            URL objFile = ModelUtils.class.getClassLoader().getResource(filePathSrc);
            converter.setProperty("texdir", ModelUtils.class.getClassLoader().getResource("data/model/"));
            ByteArrayOutputStream BO = new ByteArrayOutputStream();
            converter.convert(objFile.openStream(), BO);
            Node model = (Node) BinaryImporter.getInstance().load(new ByteArrayInputStream(BO.toByteArray()));
            model.setModelBound(new BoundingSphere());
            model.updateModelBound();
            return model;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    public static void convertJmeXML(String filePathSrc, String filePathOut) {
        URL rsrcPath = ModelUtils.class.getClassLoader().getResource(filePathSrc.substring(0, filePathSrc.lastIndexOf("/") + 1));
        try {
            br.furb.inf.tcc.util.jme.JmeBinaryReader jbr = new br.furb.inf.tcc.util.jme.JmeBinaryReader();
            jbr.setProperty("texurl", rsrcPath);
            jbr.setProperty("bound", "box");
            URL modelURL = ModelUtils.class.getClassLoader().getResource(filePathSrc);
            XMLtoBinary converter = new XMLtoBinary();
            ByteArrayOutputStream binaryOutput = new ByteArrayOutputStream();
            converter.sendXMLtoBinary(modelURL.openStream(), binaryOutput);
            Node n = jbr.loadBinaryFormat(new ByteArrayInputStream(binaryOutput.toByteArray()));
            BinaryExporter.getInstance().save(n, new File(filePathOut));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void convertJmeObj(String filePath, String filePathOut) {
        String rsrcPath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
        URL textureFolderPath = ModelUtils.class.getClassLoader().getResource("./" + rsrcPath);
        TextureKey.setOverridingLocation(textureFolderPath);
        ObjToJme converter = new ObjToJme();
        try {
            URL objFile = ModelUtils.class.getClassLoader().getResource(filePath);
            converter.setProperty("mtllib", ModelUtils.class.getClassLoader().getResource(rsrcPath));
            converter.setProperty("texdir", ModelUtils.class.getClassLoader().getResource(rsrcPath));
            ByteArrayOutputStream binaryOutput = new ByteArrayOutputStream();
            converter.convert(objFile.openStream(), binaryOutput);
            Node model = (Node) BinaryImporter.getInstance().load(new ByteArrayInputStream(binaryOutput.toByteArray()));
            model.setModelBound(new BoundingSphere());
            model.updateModelBound();
            BinaryExporter.getInstance().save(model, new File(filePathOut));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Loads a model in JME format creating your bounding box.
	 * @param filePathSrc Path and file name of the JME model.
	 * @return Node The loaded model.
	 */
    public static Node loadJmeModel(String fileNameSrc) {
        try {
            URL urlModel = ModelUtils.class.getClassLoader().getResource(fileNameSrc);
            BufferedInputStream binaryReader = new BufferedInputStream(urlModel.openStream());
            Node model = (Node) BinaryImporter.getInstance().load(binaryReader);
            model.setModelBound(new BoundingBox());
            model.updateModelBound();
            return model;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Loads a model in JME format creating your bounding box.
	 * @param filePathSrc Path and file name of the JME model.
	 * @return Node The loaded model.
	 */
    public static ParticleMesh loadJmeParticleMesh(String fileNameSrc) {
        try {
            URL urlModel = ModelUtils.class.getClassLoader().getResource(fileNameSrc);
            BufferedInputStream binaryReader = new BufferedInputStream(urlModel.openStream());
            ParticleMesh model = (ParticleMesh) BinaryImporter.getInstance().load(binaryReader);
            model.setModelBound(new BoundingBox());
            model.updateModelBound();
            return model;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Model loadModelMd5(String path) {
        InputStream in = ModelUtils.class.getResourceAsStream("/" + path);
        MD5MeshReader reader = new MD5MeshReader();
        reader.setProperty(MD5MeshReader.CLASSLOADER, ModelUtils.class.getClassLoader());
        try {
            return reader.readModel(in);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
