package be.kuleuven.cw.peno3.client;

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
import be.kuleuven.VTKfakbarCWA1.data.WebserviceAdressContainer;
import be.kuleuven.VTKfakbarCWA1.model.sales.Till;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public class TillDAO {

    private static TillDAO singletonTillDAO;

    private static final String BASEURL = WebserviceAdressContainer.getInstance().getBASEURL();

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public List<Till> listTills() {
        List<Till> returner = new ArrayList<Till>();
        try {
            String json = stringOfUrl(BASEURL + "TillDAO/listTillContents");
            returner = mapTillsToList(json);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returner;
    }

    public boolean addTill(Till tillToAdd) {
        if (tillToAdd != null) {
            try {
                String jsontill = new Gson().toJson(tillToAdd);
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod(BASEURL + "TillDAO/addTill");
                method.addParameter("till", jsontill);
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

    public boolean deleteTill(Till tillToDelete) {
        if (tillToDelete != null) {
            Integer tillid = tillToDelete.getId();
            try {
                stringOfUrl(BASEURL + "TillDAO/deleteTill?id=" + tillid);
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

    private List<Till> merchandiseArrayToList(Till[] array) {
        List<Till> returner = new ArrayList<Till>();
        Collections.addAll(returner, array);
        return returner;
    }

    private List<Till> mapTillsToList(String jsonstring) {
        Till[] merchandisemapping = new Gson().fromJson(jsonstring.toString(), Till[].class);
        return merchandiseArrayToList(merchandisemapping);
    }
}
