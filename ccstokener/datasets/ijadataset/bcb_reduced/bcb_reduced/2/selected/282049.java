package be.kuleuven.cw.peno3.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import be.kuleuven.VTKfakbarCWA1.data.WebserviceAdressContainer;
import be.kuleuven.VTKfakbarCWA1.model.product.Product;
import be.kuleuven.VTKfakbarCWA1.model.product.Type;
import be.kuleuven.VTKfakbarCWA1.model.sales.Merchandise;
import be.kuleuven.VTKfakbarCWA1.model.sales.MerchandiseItem;
import be.kuleuven.VTKfakbarCWA1.model.sales.MerchandiseManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * @author Inge
 *
 */
public class MerchandiseDAO {

    private static final String BASEURL = WebserviceAdressContainer.getInstance().getBASEURL();

    private static MerchandiseManager merchandiseManager = MerchandiseManager.getSingletonMerchandiseManager();

    public static void main(String[] args) {
        Product p1 = new Product();
        p1.setID(2);
        p1.setName("water");
        p1.setQuantity(new BigDecimal(0.2));
        p1.setType(Type.SODA);
        MerchandiseItem item1 = new MerchandiseItem();
        item1.setProduct(p1);
        item1.setQuantityPerProduct(new BigDecimal(0.25));
        Product p2 = new Product();
        p2.setID(3);
        p2.setName("Rum");
        p2.setQuantity(new BigDecimal(0.7));
        p2.setType(Type.LIQUOR);
        MerchandiseItem item2 = new MerchandiseItem();
        item2.setProduct(p2);
        item2.setQuantityPerProduct(new BigDecimal(0.05));
        Product p3 = new Product();
        p3.setID(1);
        p3.setName("Cola Light");
        p3.setQuantity(new BigDecimal(0.2));
        p3.setType(Type.SODA);
        MerchandiseItem item3 = new MerchandiseItem();
        item3.setProduct(p3);
        item3.setQuantityPerProduct(new BigDecimal(0.25));
        Product p4 = new Product();
        p4.setID(1);
        p4.setName("Passoa");
        p4.setQuantity(new BigDecimal(1));
        p4.setType(Type.LIQUOR);
        MerchandiseItem item4 = new MerchandiseItem();
        item4.setProduct(p4);
        item4.setQuantityPerProduct(new BigDecimal(0.1));
        Product p5 = new Product();
        p5.setID(1);
        p5.setName("Appelsiensap");
        p5.setQuantity(new BigDecimal(1));
        p5.setType(Type.SODA);
        MerchandiseItem item5 = new MerchandiseItem();
        item5.setProduct(p5);
        item5.setQuantityPerProduct(new BigDecimal(0.20));
        Merchandise merch = new Merchandise();
        List<MerchandiseItem> components = new ArrayList<MerchandiseItem>();
        components.add(item3);
        merch.setName("Cola Light");
        merch.setPrice(new BigDecimal(1));
        merch.setActivated(true);
        merch.setComponents(components);
        merch.setId(10);
        deleteMerchandise(merch);
    }

    public static Merchandise getMerchandise(Integer id) {
        if (id != null && id > 0) {
            try {
                String url = BASEURL + "MerchandiseDAO/getMerchandise?id=" + id;
                String json = stringOfUrl(url);
                Merchandise merchandise = mapMerchandise(json);
                List<MerchandiseItem> components = getMerchandiseItemsForMerchandise(merchandise);
                merchandise.setComponents(components);
                return merchandise;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (JsonParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static List<Merchandise> listMerchandises() {
        List<Merchandise> returner = new ArrayList<Merchandise>();
        try {
            String json = stringOfUrl(BASEURL + "MerchandiseDAO/listMerchandises");
            List<Merchandise> merchandises = mapMerchandisesToList(json);
            List<MerchandiseItem> components = new ArrayList<MerchandiseItem>();
            for (Merchandise merchandise : merchandises) {
                components = getMerchandiseItemsForMerchandise(merchandise);
                merchandise.setComponents(components);
                returner.add(merchandise);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returner;
    }

    public List<Merchandise> listAll() {
        List<Merchandise> returner = new ArrayList<Merchandise>();
        try {
            String json = stringOfUrl(BASEURL + "MerchandiseDAO/listAll");
            List<Merchandise> merchandises = mapMerchandisesToList(json);
            List<MerchandiseItem> components = new ArrayList<MerchandiseItem>();
            for (Merchandise merchandise : merchandises) {
                components = getMerchandiseItemsForMerchandise(merchandise);
                merchandise.setComponents(components);
                returner.add(merchandise);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returner;
    }

    public static List<MerchandiseItem> getMerchandiseItemsForMerchandise(Merchandise merchandise) {
        List<MerchandiseItem> returner = new ArrayList<MerchandiseItem>();
        try {
            String url = BASEURL + "MerchandiseDAO/getMerchandiseItems?merchandiseId=" + merchandise.getId();
            String json = stringOfUrl(url);
            returner = mapMerchandiseItemsToList(json);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returner;
    }

    public static boolean addMerchandise(Merchandise merchandiseToAdd) {
        if (merchandiseToAdd != null) {
            try {
                String jsonmerchandise = new Gson().toJson(merchandiseToAdd);
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod(BASEURL + "MerchandiseDAO/addMerchandise");
                method.addParameter("merchandise", jsonmerchandise);
                int returnCode = client.executeMethod(method);
                if (returnCode != 200) {
                    return false;
                }
                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean deleteMerchandise(Merchandise merchandiseToDelete) {
        if (merchandiseToDelete != null) {
            try {
                String jsonmerchandise = new Gson().toJson(merchandiseToDelete);
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod(BASEURL + "MerchandiseDAO/deleteMerchandise");
                method.addParameter("merchandise", jsonmerchandise);
                int returnCode = client.executeMethod(method);
                if (returnCode != 200) {
                    return false;
                }
                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean deactivateMerchandise(Merchandise merchandiseToDeactivate) {
        if (merchandiseToDeactivate != null) {
            try {
                String url = BASEURL + "MerchandiseDAO/deactivateMerchandise?id=" + merchandiseToDeactivate.getId();
                String resultString = stringOfUrl(url);
                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static String stringOfUrl(String addr) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        URL url = new URL(addr);
        IOUtils.copy(url.openStream(), output);
        return output.toString();
    }

    public String streamToString(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IOUtils.copy(stream, output);
        return output.toString();
    }

    private static List<Merchandise> merchandiseArrayToList(Merchandise[] array) {
        List<Merchandise> returner = new ArrayList<Merchandise>();
        Collections.addAll(returner, array);
        return returner;
    }

    private static List<MerchandiseItem> merchandiseItemArrayToList(MerchandiseItem[] array) {
        List<MerchandiseItem> returner = new ArrayList<MerchandiseItem>();
        Collections.addAll(returner, array);
        return returner;
    }

    private static Merchandise mapMerchandise(String jsonstring) {
        Merchandise[] merchandisemapping = new Gson().fromJson(jsonstring, Merchandise[].class);
        return merchandisemapping[0];
    }

    private static List<Merchandise> mapMerchandisesToList(String jsonstring) {
        Merchandise[] merchandisemapping = new Gson().fromJson(jsonstring.toString(), Merchandise[].class);
        return merchandiseArrayToList(merchandisemapping);
    }

    private static List<MerchandiseItem> mapMerchandiseItemsToList(String jsonstring) {
        MerchandiseItem[] merchandiseItemMapping = new Gson().fromJson(jsonstring.toString(), MerchandiseItem[].class);
        return merchandiseItemArrayToList(merchandiseItemMapping);
    }

    private void printMerchandises(List<Merchandise> merchandises) {
        for (Merchandise merchandise : merchandises) {
            printMerchandise(merchandise);
        }
    }

    private void printMerchandise(Merchandise merchandise) {
        System.out.println(merchandise);
    }
}
