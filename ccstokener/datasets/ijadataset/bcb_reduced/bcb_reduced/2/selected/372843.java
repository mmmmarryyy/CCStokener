package drupal.client.xmlrpc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import drupal.client.content.ContentSimpleImage;
import drupal.client.nodemaps.DrupalField_imageMap;
import drupal.client.nodemaps.DrupalFile;
import drupal.client.nodemaps.DrupalNode;
import drupal.client.nodemaps.DrupalNodeSimpleImage;

public class DrupalHelper {

    /**
	 * @param nodeType
	 * @return NID of all nodes of an certain type 
	 */
    public static ArrayList<String> getNIDsByType(String nodeType) {
        ArrayList<String> result = new ArrayList<String>();
        try {
            DrupalXmlRpcService service = new DrupalXmlRpcService(Constants.serviceDomain, Constants.apiKey, Constants.serviceURL);
            Object o = service.nodeByTypeGet(nodeType);
            HashMap<String, String> nodes = (HashMap<String, String>) o;
            Collection<String> keys = nodes.keySet();
            result.addAll(keys);
            service.logout();
            System.out.println("Service getAllSimpleImageNid done");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
	 * @param viewName
	 * @return NIDs 
	 */
    public static ArrayList<String> getNIDsFromView(String viewName) {
        ArrayList<String> result = new ArrayList<String>();
        try {
            DrupalXmlRpcService service = new DrupalXmlRpcService(Constants.serviceDomain, Constants.apiKey, Constants.serviceURL);
            Object obj_history_nid = service.viewsGet(viewName);
            Object[] obj_history_nid_array = (Object[]) obj_history_nid;
            for (Object obj_history : obj_history_nid_array) {
                HashMap<String, Object> map_history = (HashMap<String, Object>) obj_history;
                String nid = (String) map_history.get("nid");
                result.add(nid);
                System.out.println(nid);
            }
            service.logout();
            System.out.println("Service getNIDsFromView done");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static ArrayList<String> nodeSave(DrupalNode node) {
        ArrayList<String> result = new ArrayList<String>();
        try {
            DrupalXmlRpcService service = new DrupalXmlRpcService(Constants.serviceDomain, Constants.apiKey, Constants.serviceURL);
            service.nodeSave(node);
            service.logout();
            System.out.println("Service saveNode done");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void nodeDelete(DrupalNode node) {
        try {
            DrupalXmlRpcService service = new DrupalXmlRpcService(Constants.serviceDomain, Constants.apiKey, Constants.serviceURL);
            service.nodeDelete(node);
            service.logout();
            System.out.println("Service saveNode done");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * @param args
	 */
    public static ArrayList<ContentSimpleImage> getImages() {
        ArrayList<ContentSimpleImage> result = null;
        try {
            DrupalXmlRpcService service = new DrupalXmlRpcService(Constants.serviceDomain, Constants.apiKey, Constants.serviceURL);
            ArrayList<String> allSimpleImageNid = DrupalHelper.getNIDsByType(DrupalNode.TYPE_SIMPLE_IMAGE);
            ArrayList<ContentSimpleImage> drupalSimpleImages = new ArrayList<ContentSimpleImage>();
            for (String nidStr : allSimpleImageNid) {
                int nid = Integer.parseInt(nidStr);
                DrupalNode nodeImage = new DrupalNode();
                nodeImage.setNid(nid);
                nodeImage.setFields(new String[] { "field_image" });
                Object o = service.nodeGetFields(nodeImage);
                HashMap<String, Object> map = (HashMap<String, Object>) o;
                ContentSimpleImage drupalSimpleImage = new ContentSimpleImage(map);
                drupalSimpleImages.add(drupalSimpleImage);
            }
            result = drupalSimpleImages;
            service.logout();
            System.out.println("done");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
	 * Create a node , node type = Simple Image
	 * @param title
	 * @param file
	 * @return The NID of the created node  
	 */
    public static String nodeSaveSimpleImage(String title, File file) {
        String result = null;
        try {
            DrupalXmlRpcService service = new DrupalXmlRpcService(Constants.serviceDomain, Constants.apiKey, Constants.serviceURL);
            DrupalFile drupalFile = new DrupalFile(file, Constants.path_simple_image);
            String fidStr = (String) service.fileSave(drupalFile);
            drupalFile.setFID(fidStr);
            service.imageCacheGet(Constants.ImageCachePresetMegali, drupalFile.getFILEPATH());
            service.imageCacheGet(Constants.ImageCachePresetMikri, drupalFile.getFILEPATH());
            DrupalField_imageMap field_image_Map = new DrupalField_imageMap(drupalFile);
            DrupalNodeSimpleImage image = new DrupalNodeSimpleImage(title, field_image_Map);
            Object o_ = service.nodeSave(image);
            String nid = (String) o_;
            result = nid;
            service.logout();
            System.out.println("Service saveNode done");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String executeHttpGet(String url) throws Exception {
        String result = "";
        BufferedReader in = null;
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.setURI(new URI(url));
            HttpResponse response = client.execute(request);
            in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuffer sb = new StringBuffer("");
            String line = "";
            String NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                sb.append(line + NL);
            }
            in.close();
            String page = sb.toString();
            result = page;
            System.out.println(page);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public static void main(String[] args) {
        try {
            String strFilePath = "img/ttt.jpg";
            File file = new File(strFilePath);
            nodeSaveSimpleImage("ciao", file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
