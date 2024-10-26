package br.com.geostore.activities;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import br.com.geostore.entity.Produto;
import br.com.geostore.gps.GpsGS;
import br.com.geostore.map.CamadaGS;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class RotaActivity extends MapActivity {

    private static final String TAG = "RotaActivity";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rota);
        Produto p = (Produto) getIntent().getSerializableExtra("produto");
        GpsGS g = new GpsGS(this);
        boolean emulator = true;
        GeoPoint origem = null;
        if (emulator) {
            origem = g.DoubleToGeoPoint(-25.494805, -49.2922);
        } else {
            origem = g.getLastPosition();
        }
        GeoPoint destino = g.DoubleToGeoPoint(p.getLoja().getEndereco().getLatitude(), p.getLoja().getEndereco().getLongitude());
        if (origem != null && destino != null) {
            MapView mapa = (MapView) findViewById(R.id.mapa);
            RotaDraw(origem, destino, Color.GREEN, mapa);
            mapa.getController().animateTo(origem);
            mapa.getController().setZoom(15);
            mapa.getController().setCenter(destino);
            mapa.setBuiltInZoomControls(true);
            mapa.invalidate();
        } else {
            if (origem == null) {
                Toast.makeText(this, "N�o foi poss�vel identificar as coordenadas de origem, por favor, verifique seu " + "GPS est� ligado ou se voc� tem acesso a uma rede de dados.", Toast.LENGTH_LONG).show();
            } else if (destino == null) {
                Toast.makeText(this, "N�o foi poss�vel determinar a localiza��o deste produto.", Toast.LENGTH_LONG).show();
            }
        }
    }

    protected boolean isRouteDisplayed() {
        return false;
    }

    private void RotaDraw(GeoPoint orig, GeoPoint dest, int color, MapView mapa) {
        StringBuilder urlString = new StringBuilder();
        urlString.append("http://maps.google.com/maps?f=d&hl=en");
        urlString.append("&saddr=");
        urlString.append(Double.toString((double) orig.getLatitudeE6() / 1.0E6));
        urlString.append(",");
        urlString.append(Double.toString((double) orig.getLongitudeE6() / 1.0E6));
        urlString.append("&daddr=");
        urlString.append(Double.toString((double) dest.getLatitudeE6() / 1.0E6));
        urlString.append(",");
        urlString.append(Double.toString((double) dest.getLongitudeE6() / 1.0E6));
        urlString.append("&ie=UTF8&0&om=0&output=kml");
        Document doc = null;
        HttpURLConnection urlConnection = null;
        URL url = null;
        try {
            url = new URL(urlString.toString());
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.connect();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(urlConnection.getInputStream());
            if (doc.getElementsByTagName("GeometryCollection").getLength() > 0) {
                String path = doc.getElementsByTagName("GeometryCollection").item(0).getFirstChild().getFirstChild().getFirstChild().getNodeValue();
                Log.d("xxx", "path=" + path);
                String[] pairs = path.split(" ");
                String[] lngLat = pairs[0].split(",");
                GeoPoint startGP = new GeoPoint((int) (Double.parseDouble(lngLat[1]) * 1E6), (int) (Double.parseDouble(lngLat[0]) * 1E6));
                mapa.getOverlays().add(new CamadaGS(startGP, startGP, 1));
                GeoPoint gp1;
                GeoPoint gp2 = startGP;
                for (int i = 1; i < pairs.length; i++) {
                    lngLat = pairs[i].split(",");
                    gp1 = gp2;
                    gp2 = new GeoPoint((int) (Double.parseDouble(lngLat[1]) * 1E6), (int) (Double.parseDouble(lngLat[0]) * 1E6));
                    mapa.getOverlays().add(new CamadaGS(gp1, gp2, 2, color));
                    Log.d("xxx", "pair:" + pairs[i]);
                }
                mapa.getOverlays().add(new CamadaGS(dest, dest, 3));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }
}
