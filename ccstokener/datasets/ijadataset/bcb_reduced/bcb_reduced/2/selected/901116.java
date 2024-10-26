package si.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import si.chart.*;
import si.comm.StockData.StockList;
import si.comm.StockData.Stock;
import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ListStock extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new ArrayAdapter<String>(this, R.layout.list_stock, STOCK));
        new StockQuotesConnection().execute("");
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        lv.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mCharts[0].setStockForPlot(STOCK_O[position]);
                Intent intent = mCharts[0].execute(ListStock.this);
                startActivity(intent);
            }
        });
    }

    private IChart[] mCharts = new IChart[] { new SalesGrowthChart() };

    static final String[] STOCK = new String[] {};

    public class StockQuotesConnection extends AsyncTask<String, Boolean, StockList> {

        @Override
        protected StockList doInBackground(String... params) {
            try {
                URL url = new URL("http://10.0.2.2:8080/StockServerWeb/StockQuotesServlet");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("POST");
                c.connect();
                c.getInputStream();
                StockList sL = StockList.parseFrom(c.getInputStream());
                return sL;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("IDException");
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(StockList result) {
            if (result != null) {
                String[] STOCK_A = new String[result.getStockCount()];
                Stock[] STOCK_O = new Stock[result.getStockCount()];
                for (int i = 0; i < result.getStockCount(); i++) {
                    STOCK_A[i] = result.getStock(i).getName() + "  " + result.getStock(i).getCurrentPrice();
                    STOCK_O[i] = result.getStock(i);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(ListStock.this, R.layout.list_stock, STOCK_A);
                setListAdapter(adapter);
                ListStock.this.setSTOCK_O(STOCK_O);
            }
        }
    }

    Stock[] STOCK_O;

    public Stock[] getSTOCK_O() {
        return STOCK_O;
    }

    public void setSTOCK_O(Stock[] sTOCK_O) {
        STOCK_O = sTOCK_O;
    }
}
