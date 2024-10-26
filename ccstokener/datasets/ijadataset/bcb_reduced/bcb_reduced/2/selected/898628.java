package net.learn2develop.PurchaseOrders;

import net.learn2develop.R;
import java.util.List;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONStringer;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import android.util.Log;

public class TransferToServer extends Activity {

    DataManipulator dm;

    List<String[]> ordersToSend = null;

    private static final String SERVICE_URI = "http://192.168.61.3/SalesService/SalesService.svc";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        dm = new DataManipulator(this);
        ordersToSend = dm.selectAllOrders();
        String isNot = new String(" ");
        HttpPost request = new HttpPost(SERVICE_URI + "/json/addorder");
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");
        for (String[] orderToSend : ordersToSend) {
            String idLineToSend = orderToSend[0];
            String clientToSend = orderToSend[1];
            String productToSend = orderToSend[2];
            String piecesToSend = orderToSend[3];
            String discountToSend = orderToSend[4];
            try {
                JSONStringer jsonOrderToSend = new JSONStringer().object().key("od").object().key("ClientName").value(clientToSend).key("ProductName").value(productToSend).key("PiecesNumber").value(piecesToSend).key("DiscountNumber").value(discountToSend).key("LineOrderId").value(idLineToSend).endObject().endObject();
                StringEntity entity = new StringEntity(jsonOrderToSend.toString());
                Log.d(" OrderLine  ", jsonOrderToSend.toString() + "\n");
                request.setEntity(entity);
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpResponse response = httpClient.execute(request);
                Log.d("WebInvoke", " OK if 200 = " + response.getStatusLine().getStatusCode());
            } catch (Exception e) {
                isNot = "NOT ";
            }
        }
        Toast.makeText(this, isNot + " OK ! " + "\n", Toast.LENGTH_LONG).show();
    }
}
