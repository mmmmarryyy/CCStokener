package es.truscoandrisco.discotecasmadrid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

public class MyJSONParser {

    public ArrayList<Discoteca> arrayDisco = null;

    public Discoteca disco = null;

    public String doGet(String url) {
        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            HttpResponse response = httpclient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String str = convertStreamToString(entity.getContent());
            return str;
        } catch (IOException e) {
            return null;
        }
    }

    public ArrayList<Discoteca> parse(String str) {
        try {
            arrayDisco = new ArrayList<Discoteca>();
            JSONArray jsonArray = new JSONArray(str);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                JSONObject jsonChild = json.getJSONObject("fields");
                String nombre = jsonChild.getString("nombre").toString();
                String descripcion = jsonChild.getString("descripcion").toString();
                String direccion = jsonChild.getString("direccion").toString();
                String evento1 = jsonChild.getString("evento1").toString();
                String evento2 = jsonChild.getString("evento2").toString();
                String logo = jsonChild.getString("logo").toString();
                String imagen = jsonChild.getString("imagen").toString();
                String longitud = jsonChild.getString("longitud").toString();
                String latitud = jsonChild.getString("latitud").toString();
                String fecha = jsonChild.getString("fecha").toString();
                String premium = jsonChild.getString("premium").toString();
                double lon = Double.valueOf(longitud);
                double lat = Double.valueOf(latitud);
                disco = new Discoteca(nombre, direccion, descripcion, evento1, evento2, logo, imagen, fecha, premium, (int) lon, (int) lat, null, null);
                arrayDisco.add(disco);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i("Dentro", String.valueOf(arrayDisco.size()));
        return arrayDisco;
    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8 * 1024);
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) sb.append(line + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
