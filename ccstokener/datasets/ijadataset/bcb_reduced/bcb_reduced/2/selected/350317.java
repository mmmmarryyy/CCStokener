package laboratorio.prueba;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import laboratorio.model.BeanDefinicion;

public class Colo {

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        try {
            URL url = new URL("http://localhost:8080/laboratorio/servletCrucigramas");
            URLConnection con = url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            OutputStream out = con.getOutputStream();
            ObjectOutputStream xObj = new ObjectOutputStream(out);
            xObj.writeObject(new String("doAltaCrucigrama"));
            out.flush();
            out.close();
            con.getInputStream();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
