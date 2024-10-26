import java.util.ArrayList;
import java.util.Hashtable;
import java.io.Reader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;
import javax.vecmath.Point3f;
import javax.vecmath.Color3f;
import javax.media.j3d.Group;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TriangleArray;
import javax.media.j3d.PolygonAttributes;
import com.sun.j3d.loaders.Loader;
import com.sun.j3d.loaders.Scene;
import com.sun.j3d.loaders.SceneBase;
import com.sun.j3d.loaders.IncorrectFormatException;
import com.sun.j3d.loaders.ParsingErrorException;
import psychomad.log.Log;
import psychomad.resources.Resources;
import psychomad.j3d.util.UtilTriangleArray;

/**
 * The RadScene class is used to make the translation from what we read with
 * the RadObject object to java3D style.
 */
class RadScene extends RadObject {

    /**
   *
   */
    private FacetData getFacetDataFromMaterialIndex(int id) {
        for (int i = 0; i < materialList.size(); i++) {
            Material m = (Material) materialList.get(i);
            if (m.index == id) return new FacetData(m.tabReflection, m.tabRadiosity);
        }
        Log.deb("inconsistente material number reference");
        throw new ParsingErrorException(Resources.getString("parsingErrorException"));
    }

    /**
   *
   */
    private Shape3D createShape3DFromGeomListIndex(int id) {
        Geom geom = (Geom) geomList.get(id);
        TriangleArray tr = new TriangleArray(Raddoom.config.getInt("maxVertexCount"), TriangleArray.COORDINATES | TriangleArray.COLOR_3 | TriangleArray.NORMALS);
        tr.setValidVertexCount(geom.tabPoints.length);
        for (int i = 0; i < geom.tabPoints.length; i++) {
            tr.setCoordinate(i, geom.tabPoints[i]);
        }
        tr.setCapability(TriangleArray.ALLOW_COLOR_READ);
        tr.setCapability(TriangleArray.ALLOW_COLOR_WRITE);
        tr.setCapability(TriangleArray.ALLOW_NORMAL_READ);
        tr.setCapability(TriangleArray.ALLOW_NORMAL_WRITE);
        tr.setCapability(TriangleArray.ALLOW_COUNT_READ);
        tr.setCapability(TriangleArray.ALLOW_COUNT_WRITE);
        tr.setCapability(TriangleArray.ALLOW_COORDINATE_READ);
        tr.setCapability(TriangleArray.ALLOW_COORDINATE_WRITE);
        ArrayList listFacetData = new ArrayList();
        for (int i = 0; i < tr.getValidVertexCount() / 3; i++) {
            listFacetData.add(getFacetDataFromMaterialIndex(geom.materialIndex));
        }
        tr.setUserData(listFacetData);
        Color3f color = ((FacetData) listFacetData.get(0)).getColor();
        for (int i = 0; i < tr.getValidVertexCount(); i++) {
            tr.setColor(i, color);
        }
        for (int i = 0; i < tr.getValidVertexCount(); i++) {
            UtilTriangleArray.computeNormal(tr, i);
        }
        Shape3D shape = new Shape3D(tr, MainWindow.makeMeshOrFilledApp(PolygonAttributes.POLYGON_FILL));
        shape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        shape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
        shape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
        return shape;
    }

    /**
   * Make the scene and return it.
   */
    private SceneBase makeScene() {
        SceneBase scene = new SceneBase();
        BranchGroup group = new BranchGroup();
        scene.setSceneGroup(group);
        for (int i = 0; i < geomList.size(); i++) {
            group.addChild(createShape3DFromGeomListIndex(i));
        }
        group.setCapability(Group.ALLOW_CHILDREN_READ);
        group.setUserData(sceneParams);
        return scene;
    }

    /**
   *
   */
    public Scene load(Reader reader) throws FileNotFoundException, IncorrectFormatException, ParsingErrorException {
        RadFileParser st = new RadFileParser(reader);
        sceneParams = new Hashtable();
        geomList = new ArrayList();
        materialList = new ArrayList();
        readRadFile(st);
        return makeScene();
    }

    /**
   *
   */
    public Scene load(String filename) throws FileNotFoundException, IncorrectFormatException, ParsingErrorException {
        BufferedReader in = new BufferedReader(new FileReader(filename));
        Scene scene = load(in);
        try {
            in.close();
        } catch (IOException exp) {
            Log.err("IO Error : " + exp.getMessage());
            Raddoom.exit(Raddoom.EXIT_CODE_ERROR_LOADING_MAPFILE);
        }
        return scene;
    }

    /**
   *
   */
    public Scene load(URL url) throws FileNotFoundException, IncorrectFormatException, ParsingErrorException {
        try {
            InputStreamReader in = new InputStreamReader(url.openStream());
            return load(in);
        } catch (IOException exp) {
            Log.err("IO Error : " + exp.getMessage());
            Raddoom.exit(Raddoom.EXIT_CODE_ERROR_LOADING_MAPFILE);
        }
        return null;
    }

    /**
   *
   */
    public String getBasePath() {
        return null;
    }

    /**
   *
   */
    public URL getBaseUrl() {
        return null;
    }

    /**
   *
   */
    public int getFlags() {
        return Loader.LOAD_ALL;
    }

    /**
   *
   */
    public void setBasePath(String pathName) {
    }

    /**
   *
   */
    public void setBaseUrl(URL url) {
    }

    /**
   *
   */
    public void setFlags(int flags) {
    }
}
