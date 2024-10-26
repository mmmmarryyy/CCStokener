package edu.uab.project.truckfleetcontrol;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import com.google.android.maps.GeoPoint;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * TruckRouteDirections class.
 *
 * Searches for a direction between two places and calls the TruckControl activity in
 * order to show the route in the map
 *
 *
 */
public class TruckRouteDirections extends ListActivity {

    private static final String TAG = "TruckRouteDirections";

    private ArrayList<Location> locationList = null;

    private ArrayList<String> mStrings = new ArrayList<String>();

    private ArrayList<Address> addressList = new ArrayList<Address>();

    private static final int ACTIVITY_EDIT = 1;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_directions);
        locationList = new ArrayList<Location>();
        Button searchButton = (Button) findViewById(R.id.buscar);
        Button endButton = (Button) findViewById(R.id.tancar);
        Log.i(TAG, "mString size: " + mStrings.size());
        Log.i(TAG, "addressList size: " + addressList.size());
        searchButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                EditText inici = (EditText) findViewById(R.id.inici);
                EditText fi = (EditText) findViewById(R.id.fi);
                String llocInici = inici.getText().toString();
                String llocFi = fi.getText().toString();
                if (!llocInici.equals("") && !llocFi.equals("")) {
                    Log.i(TAG, "Busquem de " + llocInici + " a " + llocFi);
                    Address initialAddress = searchPlace(llocInici);
                    Address endAddress = searchPlace(llocFi);
                    if (null != initialAddress && null != endAddress) {
                        GeoPoint initialPlace = new GeoPoint((int) (initialAddress.getLatitude() * 1E6), (int) (initialAddress.getLongitude() * 1E6));
                        GeoPoint endPlace = new GeoPoint((int) (endAddress.getLatitude() * 1E6), (int) (endAddress.getLongitude() * 1E6));
                        routeDirections(initialPlace, endPlace);
                    } else {
                        showMessage("No s'ha trobat algun dels llocs indicats");
                    }
                } else {
                    showMessage("Cal espec�ficar un lloc d'inici i un lloc de fi");
                }
            }
        });
        endButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Log.i(TAG, "Tanquem");
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    /**
     * Searches the required place
     * @param placeToSearch
     */
    public Address searchPlace(String placeToSearch) {
        Geocoder geoCoder = new Geocoder(getApplicationContext());
        Address searchedAddress = null;
        try {
            addressList = (ArrayList<Address>) geoCoder.getFromLocationName(placeToSearch, 5);
            if (addressList.size() > 0) {
                searchedAddress = addressList.get(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return searchedAddress;
    }

    /**
	 * Searches the direction between two geopoints calling the google maps url.
	 * Receives	and parses the kml returned file and stores the points in a Location list.
	 *
	 * @param origen - source point
	 * @param desti - destination point
	 */
    public void routeDirections(GeoPoint origen, GeoPoint desti) {
        StringBuilder urlString = new StringBuilder();
        urlString.append("http://maps.google.com/maps?f=d&hl=en");
        urlString.append("&saddr=");
        urlString.append(Double.toString((double) origen.getLatitudeE6() / 1.0E6));
        urlString.append(",");
        urlString.append(Double.toString((double) origen.getLongitudeE6() / 1.0E6));
        urlString.append("&daddr=");
        urlString.append(Double.toString((double) desti.getLatitudeE6() / 1.0E6));
        urlString.append(",");
        urlString.append(Double.toString((double) desti.getLongitudeE6() / 1.0E6));
        urlString.append("&ie=UTF8&0&om=0&output=kml");
        Log.i(TAG, "URL=" + urlString.toString());
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
                Location loc = new Location(LocationManager.GPS_PROVIDER);
                locationList.clear();
                String path = doc.getElementsByTagName("GeometryCollection").item(0).getFirstChild().getFirstChild().getFirstChild().getNodeValue();
                Log.i(TAG, "path=" + path);
                String[] pairs = path.split(" ");
                int pairsLength = pairs.length;
                if (pairsLength < 1000) {
                    String[] lngLat = pairs[0].split(",");
                    loc.setLatitude(Double.parseDouble(lngLat[1]));
                    loc.setLongitude(Double.parseDouble(lngLat[0]));
                    locationList.add(loc);
                    for (int i = 1; i < pairsLength; i++) {
                        loc = new Location(LocationManager.GPS_PROVIDER);
                        lngLat = pairs[i].split(",");
                        loc.setLatitude(Double.parseDouble(lngLat[1]));
                        loc.setLongitude(Double.parseDouble(lngLat[0]));
                        locationList.add(loc);
                    }
                    obrirRuta();
                } else {
                    showMessage("Cam� massa llarg per ser mostrat per pantalla");
                }
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

    /**
     * Recovers the positions of the route and sends
     * it to the TruckControl activity in order to show them in the map
     */
    public void obrirRuta() {
        Log.i(TAG, "Obrint ruta");
        Intent intent = new Intent(this, TruckControl.class);
        intent.putParcelableArrayListExtra("llistaPunts", locationList);
        startActivityForResult(intent, ACTIVITY_EDIT);
    }

    /**
     * Shows a 'pop-up' message
     */
    public void showMessage(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setTitle(R.string.rutes).setCancelable(false).setPositiveButton(R.string.tancar, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
            }
        }).show();
    }
}
