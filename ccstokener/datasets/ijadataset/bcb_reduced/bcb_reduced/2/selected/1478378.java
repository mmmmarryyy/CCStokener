package be.kuleuven.VTKfakbarCWA1.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import be.kuleuven.VTKfakbarCWA1.model.management.User;
import be.kuleuven.VTKfakbarCWA1.model.management.UserManager;
import be.kuleuven.VTKfakbarCWA1.model.sales.Merchandise;
import be.kuleuven.VTKfakbarCWA1.model.sales.MerchandiseItem;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * @author Philippe
 * This class is the link between the database tables Merchandise and MerchandiseItem and the rest of the project.
 * It can get Merchandise objects from the database. It can also add, delete and deactivate Merchandise objects in the database.
 */
public class MerchandiseDAO {

    private final String BASEURL = WebserviceAdressContainer.getInstance().getBASEURL();

    private MerchandiseDAO() {
    }

    private static MerchandiseDAO singletonMerchandiseDAO;

    public static synchronized MerchandiseDAO getSingletonMerchandiseDAO() {
        if (singletonMerchandiseDAO == null) {
            singletonMerchandiseDAO = new MerchandiseDAO();
        }
        return singletonMerchandiseDAO;
    }

    public Merchandise getMerchandise(Integer id) {
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

    public List<Merchandise> listMerchandises() {
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

    public List<MerchandiseItem> getMerchandiseItemsForMerchandise(Merchandise merchandise) {
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

    public boolean addMerchandise(Merchandise merchandiseToAdd) {
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

    public boolean deleteMerchandise(Merchandise merchandiseToDelete) {
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

    public String stringOfUrl(String addr) throws IOException {
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

    private List<Merchandise> merchandiseArrayToList(Merchandise[] array) {
        List<Merchandise> returner = new ArrayList<Merchandise>();
        Collections.addAll(returner, array);
        return returner;
    }

    private List<MerchandiseItem> merchandiseItemArrayToList(MerchandiseItem[] array) {
        List<MerchandiseItem> returner = new ArrayList<MerchandiseItem>();
        Collections.addAll(returner, array);
        return returner;
    }

    private Merchandise mapMerchandise(String jsonstring) {
        Merchandise[] merchandisemapping = new Gson().fromJson(jsonstring, Merchandise[].class);
        return merchandisemapping[0];
    }

    private List<Merchandise> mapMerchandisesToList(String jsonstring) {
        Merchandise[] merchandisemapping = new Gson().fromJson(jsonstring.toString(), Merchandise[].class);
        return merchandiseArrayToList(merchandisemapping);
    }

    private List<MerchandiseItem> mapMerchandiseItemsToList(String jsonstring) {
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
