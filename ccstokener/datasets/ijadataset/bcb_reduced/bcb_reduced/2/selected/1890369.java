package org.chartsy.datafeeds.yahoo;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.chartsy.main.datafeed.DataFeed;
import org.chartsy.main.datafeed.DataItem;
import org.chartsy.main.datafeed.Dataset;
import org.chartsy.main.datafeed.Stock;
import org.chartsy.main.intervals.Interval;
import org.chartsy.main.managers.ProxyManager;
import org.chartsy.main.util.StockUtil;
import org.chartsy.main.util.StringUtil;
import org.chartsy.main.util.VersionUtil;
import org.openide.util.NbBundle;

/**
 *
 * @author Viorel
 */
public class YahooDataFeed extends DataFeed implements Serializable {

    private static final long serialVersionUID = VersionUtil.APPVERSION;

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public YahooDataFeed() {
        super(NbBundle.getBundle(YahooDataFeed.class));
        refreshInterval = 5;
    }

    public String fetchStockCompanyName(Stock stock) {
        String companyName = "";
        String symbol = StockUtil.getStock(stock);
        if (isStockCached(symbol)) {
            return getStockFromCache(symbol);
        }
        String url = NbBundle.getMessage(YahooDataFeed.class, "YahooDataFeed.stockInfo.url", new String[] { symbol });
        HttpContext context = new BasicHttpContext();
        HttpGet method = new HttpGet(url);
        try {
            HttpResponse response = ProxyManager.httpClient.execute(method, context);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                companyName = StringUtil.stringBetween(EntityUtils.toString(entity), "<td width=\"270\" class=\"yfnc_modtitlew1\"><b>", "</b><br>");
                cacheStock(symbol, companyName);
                EntityUtils.consume(entity);
            }
        } catch (Exception ex) {
            companyName = "";
        } finally {
            method.abort();
        }
        return companyName;
    }

    public void fetchDataset(String symbol, Interval interval) {
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTimeInMillis(interval.startTime());
        Calendar endCalendar = Calendar.getInstance();
        String url = NbBundle.getMessage(YahooDataFeed.class, "YahooDataFeed.historicalPrices.url", new String[] { symbol, Integer.toString(startCalendar.get(Calendar.MONTH)), Integer.toString(startCalendar.get(Calendar.DAY_OF_MONTH)), Integer.toString(startCalendar.get(Calendar.YEAR)), Integer.toString(endCalendar.get(Calendar.MONTH)), Integer.toString(endCalendar.get(Calendar.DAY_OF_MONTH)), Integer.toString(endCalendar.get(Calendar.YEAR)), interval.getTimeParam() });
        HttpContext context = new BasicHttpContext();
        HttpGet method = new HttpGet(url);
        try {
            HttpResponse response = ProxyManager.httpClient.execute(method, context);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String get = EntityUtils.toString(entity);
                String[] lines = get.split("\n");
                if (lines.length > 0) {
                    ArrayList<DataItem> items = new ArrayList<DataItem>();
                    for (int i = 1; i < lines.length; i++) {
                        String[] values = lines[i].split(",");
                        DataItem item = new DataItem(dateFormat.parse(values[0]).getTime(), Float.parseFloat(values[1]), Float.parseFloat(values[2]), Float.parseFloat(values[3]), Float.parseFloat(values[4]), Float.parseFloat(values[5]));
                        items.add(item);
                        item = null;
                    }
                    Collections.sort(items);
                    addDataset(symbol, interval, new Dataset(items.toArray(new DataItem[items.size()])));
                    items = null;
                }
                get = null;
                lines = null;
                EntityUtils.consume(entity);
            }
        } catch (Exception ex) {
            return;
        } finally {
            method.abort();
        }
    }

    public void fetchLastTrade(String symbol, Interval interval) {
    }

    public String[] fetchAutocomplete(String text) {
        return new String[0];
    }
}
