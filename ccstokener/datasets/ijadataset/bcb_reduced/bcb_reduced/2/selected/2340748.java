package net.learn2develop.PurchaseOrders;

import net.learn2develop.R;
import java.util.List;
import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONStringer;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class Transfer2Server extends Activity {

    DataManipulator dm;

    List<String[]> comanda = null;

    String[] StringOfOrders;

    private static final String SERVICE_URI = "http://192.168.61.3/SalesService/SalesService.svc";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        dm = new DataManipulator(this);
        comanda = dm.selectAllOrders();
        HttpPost request = new HttpPost(SERVICE_URI + "/json/addorder");
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");
        String not = new String(" ");
        String[] ComandaDeTrimis = comanda.get(0);
        String ClientDeTrimis = ComandaDeTrimis[0];
        String ProdusDeTrimis = ComandaDeTrimis[1];
        String NumarBucati = ComandaDeTrimis[2];
        String Discount = ComandaDeTrimis[3];
        try {
            JSONStringer vehicle = new JSONStringer().object().key("od").object().key("ClientName").value(ClientDeTrimis).key("ProductName").value(ProdusDeTrimis).key("PiecesNumber").value(NumarBucati).key("DiscountNumber").value(Discount).key("LineOrderId").value("1000").endObject().endObject();
            StringEntity entity = new StringEntity(vehicle.toString());
            Toast.makeText(this, vehicle.toString() + "\n", Toast.LENGTH_LONG).show();
            request.setEntity(entity);
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpResponse response = httpClient.execute(request);
            Toast.makeText(this, response.getStatusLine().getStatusCode() + "\n", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            not = "NOT ";
        }
        Toast.makeText(this, not + " OK ! " + "\n", Toast.LENGTH_LONG).show();
    }
}
