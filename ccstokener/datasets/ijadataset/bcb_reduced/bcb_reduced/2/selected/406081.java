package com.dongsheng.job;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import com.dongsheng.dao.StockDao;
import com.dongsheng.db.Stock;

public class Datafetcher {

    private static final Logger logger = Logger.getLogger(Datafetcher.class);

    private StockDao stockDao;

    private String dataPath;

    private String dailyStockPriceList;

    private String urlDailyStockPrice;

    private String urlHistoryStockPrice;

    /**
     * fetch each stock history price
     * @param id
     * @throws IOException
     * @throws SAXException
     */
    public void fetchDataByID(String id) throws IOException, SAXException {
        URL url = new URL(urlHistoryStockPrice + id);
        URLConnection con = url.openConnection();
        con.setConnectTimeout(20000);
        InputStream is = con.getInputStream();
        byte[] bs = new byte[1024];
        int len;
        OutputStream os = new FileOutputStream(dataPath + id + ".csv");
        while ((len = is.read(bs)) != -1) {
            os.write(bs, 0, len);
        }
        os.flush();
        os.close();
        is.close();
        con = null;
        url = null;
    }

    /**
	 *fetch all stocks history price 
	 */
    public String fetchAllData() {
        StringBuilder msg = new StringBuilder("sktock history update start\n");
        try {
            stockDao.wakeupConn();
        } catch (Exception ex) {
            logger.error("connect exception..." + ex);
        }
        List<Stock> list = stockDao.getAllStocks(5000);
        if (!new File(dataPath).exists()) {
            try {
                new File(dataPath).mkdirs();
            } catch (Exception e) {
                msg.append(e + "\n");
                logger.error(e);
            }
        }
        for (Stock stock : list) {
            try {
                if (!checkIfExist(stock.getStockNo() + "." + stock.getStockMarket() + ".csv")) {
                    fetchDataByID(stock.getStockNo() + "." + stock.getStockMarket());
                }
            } catch (Exception ex) {
                msg.append("fetch stockhistory error1: " + stock.getStockNo() + ":" + ex + "\n");
                logger.error("error1: " + stock.getStockNo() + ":" + ex);
                try {
                    fetchDataByID(stock.getStockNo() + "." + stock.getStockMarket());
                } catch (Exception e) {
                    msg.append("fetch stockhistory error2: " + stock.getStockNo() + " this item fail to update:" + e + "\n");
                    logger.error("error2:" + stock.getStockNo() + " this item fail to update:" + e);
                }
            }
        }
        return msg.toString();
    }

    /**
	 * fetch each stock daily price
	 * @param StockId
	 * @param market
	 * @return
	 * @throws IOException
	 */
    public String fetchDataDailyByStockId(String StockId, String market) throws IOException {
        URL url = new URL(urlDailyStockPrice.replace("{0}", StockId + "." + market));
        URLConnection con = url.openConnection();
        con.setConnectTimeout(20000);
        InputStream is = con.getInputStream();
        byte[] bs = new byte[1024];
        int len;
        OutputStream os = new FileOutputStream(dailyStockPriceList, true);
        while ((len = is.read(bs)) != -1) {
            os.write(bs, 0, len);
        }
        os.flush();
        os.close();
        is.close();
        con = null;
        url = null;
        return null;
    }

    /**
	 * fetch all stocks daily price
	 */
    public String fetchDataDaily() {
        StringBuilder msg = new StringBuilder();
        File file = new File(dailyStockPriceList);
        if (file.exists()) {
            file.delete();
        }
        try {
            stockDao.wakeupConn();
        } catch (Exception ex) {
            logger.error("connect exception..." + ex);
        }
        List<Stock> list = stockDao.getAllStocks(5000);
        for (Stock stock : list) {
            logger.debug(stock.getStockNo() + "." + stock.getStockMarket());
            try {
                fetchDataDailyByStockId(stock.getStockNo(), stock.getStockMarket());
            } catch (IOException e) {
                msg.append("daily error1: " + stock.getStockNo() + ":" + e + "\n");
                logger.error("daily error1: " + stock.getStockNo() + ":" + e);
                try {
                    fetchDataDailyByStockId(stock.getStockNo(), stock.getStockMarket());
                } catch (IOException e1) {
                    msg.append("daily error2: " + stock.getStockNo() + ":" + e + "\n");
                    logger.error("daily error2:this item fail to update " + stock.getStockNo() + ":" + e1);
                }
            }
        }
        logger.debug("fetch completely.");
        return msg.toString();
    }

    private boolean checkIfExist(String id) {
        File file = new File(dataPath + id);
        if (file.exists()) {
            return true;
        } else {
            return false;
        }
    }

    public StockDao getStockDao() {
        return stockDao;
    }

    public void setStockDao(StockDao stockDao) {
        this.stockDao = stockDao;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getDailyStockPriceList() {
        return dailyStockPriceList;
    }

    public void setDailyStockPriceList(String dailyStockPriceList) {
        this.dailyStockPriceList = dailyStockPriceList;
    }

    public String getUrlDailyStockPrice() {
        return urlDailyStockPrice;
    }

    public void setUrlDailyStockPrice(String urlDailyStockPrice) {
        this.urlDailyStockPrice = urlDailyStockPrice;
    }

    public String getUrlHistoryStockPrice() {
        return urlHistoryStockPrice;
    }

    public void setUrlHistoryStockPrice(String urlHistoryStockPrice) {
        this.urlHistoryStockPrice = urlHistoryStockPrice;
    }

    public static void main(String[] args) {
        try {
        } catch (Exception e) {
        }
    }
}
