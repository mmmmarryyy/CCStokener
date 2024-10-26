package be.kuleuven.VTKfakbarCWA1.model.sales;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.io.IOUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import be.kuleuven.VTKfakbarCWA1.data.WebserviceAdressContainer;

public class MerchandiseDAO {

    private static final String BASEURL = WebserviceAdressContainer.getInstance().getBASEURL();

    private static MerchandiseManager merchandiseManager = MerchandiseManager.getSingletonMerchandiseManager();

    public static ArrayList<Merchandise> getAll() {
        return null;
    }

    private ArrayList<Merchandise> allMerchandise;

    private static MerchandiseDAO singletonMerchandiseDAO;

    public static synchronized MerchandiseDAO getSingletonMerchandiseDAO() {
        if (singletonMerchandiseDAO == null) {
            singletonMerchandiseDAO = new MerchandiseDAO();
        }
        return singletonMerchandiseDAO;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public MerchandiseDAO() {
        allMerchandise = new ArrayList<Merchandise>();
        fillMerchandiseListWithDummies();
    }

    private void fillMerchandiseListWithDummies() {
        ArrayList<MerchandiseItem> map = new ArrayList<MerchandiseItem>();
    }

    public static Merchandise getMerchandise(String name) {
        if (name != null) {
            try {
                String url = BASEURL + "MerchandiseDAO/getMerchandise?name=" + name.replace(" ", "%20");
                String json = stringOfUrl(url);
                if (json != null) {
                    List<MerchandiseItem> merchan = mapMerchandiseToList(json);
                    Merchandise merchandise = new Merchandise();
                    merchandise.setComponents(merchan);
                    int id = merchan.get(0).getId();
                    BigDecimal pr = new BigDecimal(priceStringToJson(json));
                    merchandise.setPrice(pr);
                    merchandise.setName(nameStringToJson(json));
                    merchandise.setId(id);
                    return merchandise;
                }
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

    public static List<Merchandise> getMerchandiseList() {
        List<Merchandise> returner = new ArrayList<Merchandise>();
        try {
            String json = stringOfUrl(BASEURL + "MerchandiseDAO/listMerchandises");
            returner = mapMerchandisesToList(json);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returner;
    }

    private static List<Merchandise> mapMerchandisesToList(String json) {
        List<Merchandise> merch = new ArrayList<Merchandise>();
        String string = "{\"merchs\":" + json + "}";
        JsonParser parser = new JsonParser();
        JsonObject jsonobj = parser.parse(string).getAsJsonObject();
        JsonArray merchs = jsonobj.getAsJsonArray("merchs");
        for (JsonElement element : merchs) {
            String str = element.getAsJsonObject().get("name").toString();
            boolean test = false;
            for (Merchandise m : merch) {
                if (m.getName().equals(str) == true) {
                    MerchandiseItem item = mapMerchandiseItem(element.toString());
                    m.addComponent(item);
                    test = true;
                }
            }
            if (test == false) {
                Merchandise merchandise = new Merchandise();
                merchandise.setName(str);
                MerchandiseItem item = mapMerchandiseItem(element.toString());
                merchandise.addComponent(item);
                BigDecimal pr = new BigDecimal(priceStringToJson(json));
                merchandise.setPrice(pr);
                merch.add(merchandise);
            }
        }
        return merch;
    }

    public static boolean addMerchandise(Merchandise merchandiseToAdd) {
        if (merchandiseToAdd != null) {
            try {
                for (MerchandiseItem item : merchandiseToAdd.getComponents()) {
                    String url = BASEURL + "MerchandiseDAO/addMerchandise?name=" + merchandiseToAdd.getName().replace(" ", "%20") + "&prodtype_id=" + item.getProduct().getID() + "&quantityPerProduct=" + item.getQuantity() + "&price=" + merchandiseToAdd.getPrice();
                    String resultString = stringOfUrl(url);
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

    public static boolean removeMerchandise(Merchandise merchandiseToDelete) {
        if (merchandiseToDelete != null) {
            try {
                String name = merchandiseToDelete.getName().replace(" ", "%20");
                String url = BASEURL + "MerchandiseDAO/deleteMerchandiseName?name=" + name;
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

    /**
	public static boolean deleteMerchandise(Merchandise merchandiseToDelete) {
		if (merchandiseToDelete != null) {
			try {
				String jsonmerchandise = new Gson().toJson(merchandiseToDelete);
				//printMerchandise(merchandiseToDelete);

				HttpClient client = new HttpClient();

				PostMethod method = new PostMethod(BASEURL
						+ "MerchandiseDAO/deleteMerchandise");
				method.addParameter("merchandise", jsonmerchandise);
				int returnCode = client.executeMethod(method);

				//System.out.println(method.getResponseBodyAsString());
				return true;
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (HttpException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}
	*/
    public static String stringOfUrl(String addr) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        URL url = new URL(addr);
        IOUtils.copy(url.openStream(), output);
        return output.toString();
    }

    public static String streamToString(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IOUtils.copy(stream, output);
        return output.toString();
    }

    private static List<MerchandiseItem> arrayToList(MerchandiseItem[] merchandisemapping) {
        List<MerchandiseItem> returner = new ArrayList<MerchandiseItem>();
        Collections.addAll(returner, merchandisemapping);
        return returner;
    }

    private static List<MerchandiseItem> mapMerchandiseToList(String jsonstring) {
        MerchandiseItem[] merchandisemapping = new Gson().fromJson(jsonstring.toString(), MerchandiseItem[].class);
        for (MerchandiseItem item : merchandisemapping) {
            item.getProduct().setName(item.getProduct().getName());
        }
        return arrayToList(merchandisemapping);
    }

    private static MerchandiseItem mapMerchandiseItem(String jsonstring) {
        MerchandiseItem item = new Gson().fromJson(jsonstring, MerchandiseItem.class);
        item.getProduct().setName(item.getProduct().getName());
        return item;
    }

    private static void printMerchandises(List<Merchandise> merchandises) {
        for (Merchandise merchandise : merchandises) {
            printMerchandise(merchandise);
        }
    }

    private static void printMerchandise(Merchandise merchandiseToPrint) {
        System.out.println(merchandiseToPrint.getName());
        System.out.println(merchandiseToPrint.getPrice());
        for (MerchandiseItem item : merchandiseToPrint.getComponents()) {
            System.out.println(item.getProduct().getName());
        }
    }

    private static String priceStringToJson(String str) {
        String string2 = "{\"merchs\":" + str + "}";
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(string2).getAsJsonObject();
        JsonArray merchs = json.getAsJsonArray("merchs");
        JsonElement result = merchs.get(0).getAsJsonObject().get("price");
        return result.toString();
    }

    private static String nameStringToJson(String str) {
        String string2 = "{\"merchs\":" + str + "}";
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(string2).getAsJsonObject();
        JsonArray merchs = json.getAsJsonArray("merchs");
        JsonElement result = merchs.get(0).getAsJsonObject().get("name");
        return result.toString();
    }
}
