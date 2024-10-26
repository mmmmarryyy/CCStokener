package net.learn2develop.PurchaseOrders;

import net.learn2develop.R;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class CautaProdus extends ListActivity {

    private static final String SERVICE_URI = "http://192.168.61.3/SalesService/SalesService.svc";

    EditText txtItem;

    ListView listItems;

    ArrayList<String> toDoItems;

    ArrayAdapter<String> aa;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cautaprodus);
        HttpGet request = new HttpGet(SERVICE_URI + "/json/getproducts");
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");
        DefaultHttpClient httpClient = new DefaultHttpClient();
        String theString = new String("");
        try {
            HttpResponse response = httpClient.execute(request);
            HttpEntity responseEntity = response.getEntity();
            InputStream stream = responseEntity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            Vector<String> vectorOfStrings = new Vector<String>();
            String tempString = new String();
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            stream.close();
            theString = builder.toString();
            JSONObject json = new JSONObject(theString);
            Log.i("_GetPerson_", "<jsonobject>\n" + json.toString() + "\n</jsonobject>");
            JSONArray nameArray = json.getJSONArray("getProductsResult");
            for (int i = 0; i < nameArray.length(); i++) {
                Log.i("_GetProducts_", "<ID" + i + ">" + nameArray.getJSONObject(i).getString("ID") + "</ID" + i + ">\n");
                Log.i("_GetProducts_", "<Name" + i + ">" + nameArray.getJSONObject(i).getString("Name") + "</Name" + i + ">\n");
                Log.i("_GetProducts_", "<Price" + i + ">" + nameArray.getJSONObject(i).getString("Price") + "</Price" + i + ">\n");
                Log.i("_GetProducts_", "<Symbol" + i + ">" + nameArray.getJSONObject(i).getString("Symbol") + "</Symbol" + i + ">\n");
                tempString = nameArray.getJSONObject(i).getString("Name") + "\n" + nameArray.getJSONObject(i).getString("Price") + "\n" + nameArray.getJSONObject(i).getString("Symbol");
                vectorOfStrings.add(new String(tempString));
            }
            int orderCount = vectorOfStrings.size();
            String[] orderTimeStamps = new String[orderCount];
            vectorOfStrings.copyInto(orderTimeStamps);
            setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, orderTimeStamps));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem item;
        item = menu.add("Adauga Produs");
        item = menu.add("Sterge Produs");
        item = menu.add("Termen plata, discount");
        item = menu.add("Validare Comanda");
        item = menu.add("Sterge Comanda");
        return true;
    }
}
