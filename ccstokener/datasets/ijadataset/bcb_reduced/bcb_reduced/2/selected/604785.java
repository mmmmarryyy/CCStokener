package org.pnp.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;
import android.os.Message;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.app.Activity;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.database.Cursor;
import android.os.Bundle;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import org.pnp.db.StockDBAdapter;
import org.pnp.services.BackGroundService;
import org.pnp.util.StringParser;
import org.pnp.vo.Stock;

public class MainActivity extends ListActivity {

    private static final int ACTIVITY_CREATE = 0;

    private static final int ACTIVITY_EDIT = 1;

    private static int NOTIFICATIONS_ID = R.layout.main;

    private TextView textView;

    private ListView listView;

    private String urlStr = "http://idc-hq-tj.sinajs.cn/list=";

    private String stockCodes = null;

    private boolean isTurnOn = false;

    private StockDBAdapter dbHelper = null;

    private Cursor cursor;

    private NotificationManager mNotificationManager;

    public String getUrlStr() {
        return urlStr;
    }

    public void setUrlStr(String urlStr) {
        this.urlStr = urlStr;
    }

    public String getStockCodes() {
        return stockCodes;
    }

    public void setStockCodes(String stockCodes) {
        this.stockCodes = stockCodes;
    }

    private Timer timer = new Timer();

    Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            switch(msg.what) {
                case 1:
                    getStockInfo(urlStr);
                    break;
            }
            super.handleMessage(msg);
        }
    };

    TimerTask task = new TimerTask() {

        public void run() {
            Message message = new Message();
            message.what = 1;
            handler.sendMessage(message);
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        if (isTurnOn == true) {
            mNotificationManager.cancel(NOTIFICATIONS_ID);
        }
        dbHelper = new StockDBAdapter(this);
        dbHelper = dbHelper.open();
        fillData();
        String allStockCodes = getAllStockCodes();
        this.listView = (ListView) this.getListView();
        this.textView = (TextView) findViewById(R.id.TextView01);
        this.textView.setText("");
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        this.urlStr = "http://idc-hq-tj.sinajs.cn/list=" + allStockCodes;
        timer.schedule(task, 1000, 10000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.listmenu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case R.id.insert:
                createStockInfo();
                return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void createStockInfo() {
        Intent intent = new Intent(this, StockInfo.class);
        startActivityForResult(intent, ACTIVITY_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == ACTIVITY_CREATE && resultCode == RESULT_OK) {
            String allStockCodes = getAllStockCodes();
            setUrlStr(allStockCodes);
            fillData();
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    private void fillData() {
        cursor = dbHelper.fetchAllStocks();
        startManagingCursor(cursor);
        String[] from = new String[] { StockDBAdapter.KEY_ROWID, StockDBAdapter.KEY_STOCKCODE, StockDBAdapter.KEY_STOCKNAME, StockDBAdapter.KEY_UPWARNPRICE, StockDBAdapter.KEY_DOWNWARNPRICE };
        int[] to = new int[] { R.id.Record_Id, R.id.StockCodeView, R.id.StockNameView, R.id.StockHighWarnPriceView, R.id.StockLowWarnPriceView };
        SimpleCursorAdapter stocks = new SimpleCursorAdapter(this, R.layout.stock_row, cursor, from, to);
        this.setListAdapter(stocks);
        String stockCodes = "";
        cursor.moveToFirst();
        while (cursor.moveToNext()) {
            stockCodes = stockCodes + cursor.getString(1);
        }
        this.setStockCodes(stockCodes);
    }

    private String getAllStockCodes() {
        cursor = dbHelper.fetchAllStocks();
        startManagingCursor(cursor);
        String stockCodes = "";
        cursor.moveToFirst();
        while (cursor.moveToNext()) {
            if (stockCodes == "") {
                stockCodes = cursor.getString(1);
            } else {
                stockCodes = stockCodes + "," + cursor.getString(1);
            }
        }
        return stockCodes;
    }

    public void refreshInfo(View view) {
        switch(view.getId()) {
            case R.id.Button01:
                getStockInfo(this.urlStr);
                break;
        }
    }

    public void maintainService(View view) {
        switch(view.getId()) {
            case R.id.Button02:
                this.startService(new Intent(this, org.pnp.services.BackGroundService.class));
                break;
            case R.id.Button03:
                this.stopService(new Intent(this, org.pnp.services.BackGroundService.class));
                break;
        }
    }

    private synchronized void getStockInfo(String urlStr) {
        try {
            textView.setText(null);
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(urlStr);
            HttpResponse response = client.execute(request);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "GBK"));
            String line = "";
            String allLines = "";
            while ((line = rd.readLine()) != null) {
                textView.append(line);
                allLines = allLines + line;
            }
            Map<String, Stock> stockInfos = StringParser.praseStockString(allLines);
            String stockCode;
            TextView stockCodeView;
            TextView currentPriceView;
            TextView stockHighWarnPriceView;
            TextView stockLowWarnPriceView;
            Stock stock;
            double currentPrice;
            double highWarnPrice;
            double lowWarnPrice;
            for (int i = 0; i < this.listView.getChildCount(); i++) {
                stockCodeView = (TextView) this.listView.getChildAt(i).findViewById(R.id.StockCodeView);
                currentPriceView = (TextView) this.listView.getChildAt(i).findViewById(R.id.StockCurrentPriceView);
                stockHighWarnPriceView = (TextView) this.listView.getChildAt(i).findViewById(R.id.StockHighWarnPriceView);
                stockLowWarnPriceView = (TextView) this.listView.getChildAt(i).findViewById(R.id.StockLowWarnPriceView);
                highWarnPrice = Double.parseDouble(stockHighWarnPriceView.getText().toString().trim());
                lowWarnPrice = Double.parseDouble(stockLowWarnPriceView.getText().toString().trim());
                stockCode = stockCodeView.getText().toString().trim();
                stock = stockInfos.get(stockCode);
                currentPrice = stock.getCurrentPrice();
                currentPriceView.setText(String.valueOf(currentPrice));
                if (currentPrice >= highWarnPrice) {
                    System.out.println("UP");
                }
                if (currentPrice <= lowWarnPrice) {
                    System.out.println("DOWN");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            textView.setText(ex.getMessage());
        }
    }

    public void playNotifies(String title, String content, int drawable, int defaults) {
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        final Notification notification = new Notification(drawable, content, System.currentTimeMillis());
        notification.setLatestEventInfo(this, title, content, contentIntent);
        notification.defaults = defaults;
        notification.number++;
        mNotificationManager.notify(NOTIFICATIONS_ID, notification);
        isTurnOn = true;
    }

    private void showNotification() {
        Intent toLaunch = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent intentBack = PendingIntent.getActivity(getApplicationContext(), 0, toLaunch, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification(R.drawable.icon, "ST", System.currentTimeMillis());
        ;
        notification.setLatestEventInfo(getApplicationContext(), "ST", "Stock is runing", intentBack);
        notification.number++;
        mNotificationManager.notify(0, notification);
    }

    @Override
    protected void onPause() {
        super.onPause();
        showNotification();
        timer.cancel();
    }

    protected void onResume() {
        super.onResume();
        if (isTurnOn == true) {
            mNotificationManager.cancel(NOTIFICATIONS_ID);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
