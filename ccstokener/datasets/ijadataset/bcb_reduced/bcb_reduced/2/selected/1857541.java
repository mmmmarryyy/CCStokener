package octlight.scene.loader.nwn.test;

import octlight.base.Color;
import octlight.material.texture.TextureProvider;
import octlight.material.texture.TextureResourcePath;
import octlight.math.Transform;
import octlight.math.Vector3;
import octlight.scene.Node;
import octlight.scene.TransformGroup;
import octlight.scene.light.AmbientLight;
import octlight.scene.light.DirectionalLight;
import octlight.scene.loader.nwn.TextMDLFile;
import octlight.util.MainFrame;
import octlight.util.ResourceUtil;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * @author $Author: creator $
 * @version $Revision: 1.3 $
 */
public class TestMDLLoader extends MainFrame {

    private TestMDLLoader() throws IOException {
        TextureProvider textureProvider = new TextureResourcePath("octlight/textures");
        URL url = ResourceUtil.getResource("octlight/models/iron_golem.mdl");
        Node node = TextMDLFile.parseModel(new InputStreamReader(url.openStream()), textureProvider);
        System.out.println("Model loaded...");
        getRootNode().addLight(new DirectionalLight(new Color(100, 100, 100), new Vector3(100, -100, -100)));
        getRootNode().addLight(new AmbientLight(new Color(1f, 1f, 1f)));
        getMouseGroup().addChild(new TransformGroup(node, Transform.createTranslateTransform(0, -20, 0).scaleThis(20).yRotateThis((float) Math.PI).xRotateThis((float) -Math.PI / 2)));
    }

    public static void main(String[] args) throws Exception {
        new TestMDLLoader().run();
    }
}
