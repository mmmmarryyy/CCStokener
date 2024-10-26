package org.chartsy.datafeeds.mrswing;

import java.io.Serializable;
import java.util.prefs.Preferences;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.chartsy.main.datafeed.DataFeed;
import org.chartsy.main.datafeed.Stock;
import org.chartsy.main.intervals.Interval;
import org.chartsy.main.managers.ProxyManager;
import org.chartsy.main.util.StockUtil;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 *
 * @author Viorel
 */
public class MrSwingDataFeed extends DataFeed implements Serializable {

    private Preferences register = NbPreferences.root().node("/org/chartsy/register");

    public MrSwingDataFeed() {
        super(NbBundle.getBundle(MrSwingDataFeed.class), true);
        refreshInterval = 5;
    }

    public String fetchStockCompanyName(Stock stock) {
        String companyName = "";
        String symbol = StockUtil.getStock(stock);
        if (isStockCached(symbol)) {
            return getStockFromCache(symbol);
        }
        String url = NbBundle.getMessage(MrSwingDataFeed.class, "MrSwingDataFeed.stockInfo.url", new String[] { symbol, register.get("username", ""), register.get("password", "") });
        HttpContext context = new BasicHttpContext();
        HttpGet method = new HttpGet(url);
        try {
            HttpResponse response = ProxyManager.httpClient.execute(method, context);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                companyName = EntityUtils.toString(entity).split("\n")[1];
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
    }

    public void fetchLastTrade(String symbol, Interval interval) {
    }

    public String[] fetchAutocomplete(String text) {
        String[] result = new String[0];
        String url = NbBundle.getMessage(MrSwingDataFeed.class, "MrSwingDataFeed.autocomplete.url", text);
        HttpContext context = new BasicHttpContext();
        HttpGet method = new HttpGet(url);
        try {
            HttpResponse response = ProxyManager.httpClient.execute(method, context);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity).split("\n");
                EntityUtils.consume(entity);
            }
        } catch (Exception ex) {
            result = new String[0];
        } finally {
            method.abort();
        }
        return result;
    }
}
