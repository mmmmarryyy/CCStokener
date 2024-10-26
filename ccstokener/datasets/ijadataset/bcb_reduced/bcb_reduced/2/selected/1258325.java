package uit.upis.manager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import uit.server.model.LayerDef;
import uit.server.model.Model;
import uit.upis.model.Layer;

public class MapManagerTest extends BaseManagerTestCase {

    private MapManager mapManager;

    public void setMapManager(MapManager mapManager) {
        this.mapManager = mapManager;
    }

    public void testGetMap() {
        try {
            List<Layer> layerDefList = new ArrayList<Layer>();
            for (int i = 0; i < 90; i++) {
                Layer def = new Layer();
                def.setId(i);
                layerDefList.add(def);
            }
            Model model = new Model();
            model.setTempLayer(layerDefList);
            model = mapManager.getMap(model);
            assertNotNull(model);
            URL u = model.getUrl();
            System.out.println(model.getResponseArcXML());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isReachableURL(URL url) throws MalformedURLException, IOException, Exception {
        System.out.println("Testing to see if URL connects");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        System.out.println("Created HttpURLConnection object");
        conn.connect();
        System.out.println("connecting..");
        boolean isConnected = (conn.getContentLength() > 0);
        System.out.println("disconnecting..");
        conn.disconnect();
        System.out.println("disconnected");
        return isConnected;
    }

    @Test
    public void testParcelSearch() {
        String whereExpression = "pnu='" + "4482525021100010000" + "'";
        List<LayerDef> visibleLayerList = new ArrayList<LayerDef>();
        long width = 500, height = 600;
        String xml = "";
        try {
            xml = mapManager.searchPacel(whereExpression, visibleLayerList, width, height);
        } catch (Exception e) {
        }
        assertNotNull(xml);
        System.out.println(xml);
    }
}
