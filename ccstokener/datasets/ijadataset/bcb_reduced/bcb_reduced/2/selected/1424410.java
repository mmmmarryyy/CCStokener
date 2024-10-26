package mundo;

import java.io.File;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JOptionPane;

/**
 *
 * @author DMHA
 */
public class Conexion implements Runnable {

    public String agregarUsuario(String nombre, String apellido, String email, String usuario, String contrasena) {
        String responce = "";
        String request = conf.Conf.agregarUsuario;
        OutputStreamWriter wr = null;
        BufferedReader rd = null;
        try {
            URL url = new URL(request);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write("nUsuario=" + usuario + "&contrasena=" + contrasena + "&nombre=" + nombre + "&apellido=" + apellido + "&email=" + email);
            wr.flush();
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                responce += line;
            }
        } catch (Exception e) {
        }
        return responce;
    }

    public String agregarArchivo(String ruta, String usuario) throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost httppost = new HttpPost(conf.Conf.agregarArchivo + usuario);
        File file = new File(ruta);
        MultipartEntity mpEntity = new MultipartEntity();
        ContentBody cbFile = new FileBody(file, "image/jpeg");
        mpEntity.addPart("userfile", cbFile);
        httppost.setEntity(mpEntity);
        System.out.println("executing request " + httppost.getRequestLine());
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        System.out.println(response.getStatusLine());
        if (resEntity != null) {
            System.out.println(EntityUtils.toString(resEntity));
        }
        if (resEntity != null) {
            resEntity.consumeContent();
        }
        httpclient.getConnectionManager().shutdown();
        return "paso";
    }

    public String login(String nUsuario, String contrasena) {
        String responce = "";
        String request = conf.Conf.login;
        OutputStreamWriter wr = null;
        BufferedReader rd = null;
        try {
            URL url = new URL(request);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write("nUsuario=" + nUsuario + "&contrasena=" + contrasena);
            wr.flush();
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                responce += line;
            }
        } catch (Exception e) {
        }
        return responce;
    }

    public String buscarArchivos(String nUsuario) {
        String responce = "";
        String request = conf.Conf.buscarArchivo;
        OutputStreamWriter wr = null;
        BufferedReader rd = null;
        try {
            URL url = new URL(request);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write("nUsuario=" + nUsuario);
            wr.flush();
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                responce += line;
            }
        } catch (Exception e) {
        }
        return responce;
    }

    public String descargarArchivo(String miArchivo, String nUsuario) {
        try {
            URL url = new URL(conf.Conf.descarga + nUsuario + "/" + miArchivo);
            URLConnection urlCon = url.openConnection();
            System.out.println(urlCon.getContentType());
            InputStream is = urlCon.getInputStream();
            FileOutputStream fos = new FileOutputStream("D:/" + miArchivo);
            byte[] array = new byte[1000];
            int leido = is.read(array);
            while (leido > 0) {
                fos.write(array, 0, leido);
                leido = is.read(array);
            }
            is.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "llego";
    }

    public void run() {
    }
}
