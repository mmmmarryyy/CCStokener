package org.pnp.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.pnp.db.StockDBAdapter;
import org.pnp.main.R;
import org.pnp.vo.Stock;
import org.pnp.vo.StockConfigration;
import android.app.Notification;
import android.content.Context;
import android.database.Cursor;
import android.widget.TextView;

public class StockAnalysisHelper {

    private StockDBAdapter dbHelper = null;

    private Cursor cursor;

    public StockAnalysisHelper(Context context) {
        dbHelper = new StockDBAdapter(context);
        dbHelper = dbHelper.open();
    }

    public String getAllStockCodes() {
        cursor = dbHelper.fetchAllStocks();
        String stockCodes = "";
        cursor.moveToFirst();
        while (cursor.moveToNext()) {
            if (stockCodes == "") {
                stockCodes = cursor.getString(1);
            } else {
                stockCodes = stockCodes + "," + cursor.getString(1);
            }
        }
        cursor.close();
        return stockCodes;
    }

    public Map<String, StockConfigration> getAllStockConfigs() {
        Map<String, StockConfigration> persistStocks = new HashMap<String, StockConfigration>();
        cursor = dbHelper.fetchAllStocks();
        String stockCode = "";
        String stockName = "";
        Double ytdClosePrice;
        Double highWarnPrice;
        Double lowWarnPrice;
        StockConfigration stockConfig = new StockConfigration();
        cursor.moveToFirst();
        while (cursor.moveToNext()) {
            stockCode = cursor.getString(1);
            stockConfig.setStockCode(stockCode);
            stockName = cursor.getString(2);
            stockConfig.setStockName(stockName);
            ytdClosePrice = cursor.getDouble(3);
            stockConfig.setYtdClosingPrice(ytdClosePrice);
            highWarnPrice = cursor.getDouble(4);
            stockConfig.setHighWarnPrice(highWarnPrice);
            lowWarnPrice = cursor.getDouble(5);
            stockConfig.setLowWarnPrice(lowWarnPrice);
            persistStocks.put(stockCode, stockConfig);
        }
        cursor.close();
        return persistStocks;
    }

    public synchronized Map<String, Stock> getStockInfo(String urlStr) {
        Map<String, Stock> stockInfos = null;
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(urlStr);
            HttpResponse response = client.execute(request);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "GBK"));
            String line = "";
            String allLines = "";
            while ((line = rd.readLine()) != null) {
                allLines = allLines + line;
            }
            stockInfos = StringParser.praseStockString(allLines);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stockInfos;
    }
}
