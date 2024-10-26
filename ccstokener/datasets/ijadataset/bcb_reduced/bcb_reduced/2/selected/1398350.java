package sfljtse.custom.quotes.quoteFetcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashSet;
import java.util.StringTokenizer;
import sfljtse.tsf.coreLayer.quotes.Candle;
import sfljtse.tsf.coreLayer.quotes.CsvImport;
import sfljtse.tsf.coreLayer.quotes.fetcher.AQuoteFetcher;
import sfljtse.tsf.coreLayer.quotes.fetcher.IQuoteFetcher;
import sfljtse.tsf.coreLayer.utils.URLrip;
import sfljtse.custom.quotes.timeframes.*;
import sfljtse.tsf.coreLayer.quotes.timeframes.ATimeFrame;
import sfljtse.tsf.coreLayer.utils.FileUtils;
import sfljtse.tsf.coreLayer.settings.Settings;
import sun.management.FileSystem;

/**
 * @title		: YahooFetcher       
 * @description	:  
 * @date		: 27-apr-2006   
 * @author		: Alberto Sfolcini  <a.sfolcini@gmail.com>
 */
public class YahooFetcher extends AQuoteFetcher {

    private boolean debug = true;

    /**
     * The URL string has been built in according with the following page:
     * http://www.gummy-stuff.org/Yahoo-data.htm
     * 
     */
    private String provider = "http://quote.yahoo.com/d/quotes.csv?s=";

    private String tags = "&f=sl1d1t1ohgv&e=.csv";

    private String symbolQuery = "http://finance.yahoo.com/q?s=";

    /**
     * Constructor
     * @throws Exception 
     */
    public YahooFetcher() throws Exception {
        if (testSource() != 0) {
            new Exception().notify();
        }
        super.xmlImportModel = "./jars/config/imports/yahooDaily.xml";
    }

    /**
     *  (non-Javadoc)
     * @see sfljtse.tsf.coreLayer.quotes.fetcher.IQuoteFetcher#getProviderName()
     */
    public String getProviderInfo() {
        return "Yahoo Finance Source Provider <http://finance.yahoo.com/>";
    }

    /**
     * check if symbolCode exists
     */
    public boolean symbolExists(String symbol) {
        InputStreamReader inStream;
        BufferedReader buff;
        boolean ret = true;
        try {
            URL url = new URL(provider + symbol + tags);
            URLConnection urlConn = url.openConnection();
            inStream = new InputStreamReader(urlConn.getInputStream());
            buff = new BufferedReader(inStream);
            String csvString = buff.readLine();
            StringTokenizer tokenizer = new StringTokenizer(csvString, ",");
            for (int i = 1; i < 3; i++) {
                if (tokenizer.hasMoreTokens()) {
                    if (tokenizer.nextToken().trim().equalsIgnoreCase("n/a")) ret = false;
                }
            }
        } catch (Exception e) {
        }
        return ret;
    }

    public Candle getLastQuote() {
        Candle candle = new Candle();
        InputStreamReader inStream = null;
        BufferedReader buff = null;
        try {
            URL url = new URL(provider + symbol.getSymbolCode() + tags);
            URLConnection urlConn = url.openConnection();
            inStream = new InputStreamReader(urlConn.getInputStream());
            buff = new BufferedReader(inStream);
            String csvString = buff.readLine();
            String ticker = "n/a";
            String close = "n/a";
            String date = "n/a";
            String time = "n/a";
            String open = "n/a";
            String high = "n/a";
            String low = "n/a";
            String vol = "n/a";
            try {
                StringTokenizer tokenizer = new StringTokenizer(csvString, ",");
                ticker = tokenizer.nextToken();
                close = tokenizer.nextToken();
                date = tokenizer.nextToken();
                time = tokenizer.nextToken();
                open = tokenizer.nextToken();
                high = tokenizer.nextToken();
                low = tokenizer.nextToken();
                vol = tokenizer.nextToken();
                StringTokenizer dateToken = new StringTokenizer(date.replaceAll("\"", "").trim(), "/");
                String mon = dateToken.nextToken();
                String day = dateToken.nextToken();
                String yyyy = dateToken.nextToken();
                if (mon.length() < 2) mon = "0" + mon;
                if (day.length() < 2) day = "0" + day;
                date = yyyy + mon + day;
            } catch (Exception e) {
            }
            time = time.replace(":", "").replace("am", "").replace("pm", "").trim();
            candle = setCandle(open, close, date, time, high, low, vol, null);
            if (candle.validate(this.timeFrame.isIntraday()) < 0) System.out.println("Quote is wrong!");
        } catch (MalformedURLException e) {
            System.out.println("Please check the spelling of the URL:" + e.toString());
        } catch (IOException e1) {
            System.out.println("Can't read from the Internet: " + e1.toString());
        } finally {
            try {
                inStream.close();
                buff.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return candle;
    }

    /**
     * gets Historical Quotes and import them into database
     */
    public int getHistoricalQuotes(String fromDD, String fromMM, String fromYYYY, String toDD, String toMM, String toYYYY, byte importIntoDB) {
        boolean delete = true;
        if (!this.timeFrame.isIntraday()) {
            String tframeStr = "d";
            switch(timeFrame.getTimeFrame()) {
                case ATimeFrame.TIMEFRAME_DAILY:
                    tframeStr = "d";
                    break;
                case ATimeFrame.TIMEFRAME_WEEKLY:
                    tframeStr = "w";
                    break;
                case ATimeFrame.TIMEFRAME_MONTHLY:
                    tframeStr = "m";
                    break;
            }
            this.sourceProvider = "http://ichart.finance.yahoo.com/table.csv?s=" + symbol.getSymbolCode().replaceAll("_", ".") + "&a=" + fromDD + "&b=" + fromMM + "&c=" + fromYYYY + "&d=" + toDD + "&e=" + toMM + "&f=" + toYYYY + "&g=" + tframeStr + "&ignore=.csv";
            if (debug) System.out.println("HTTP request: " + this.sourceProvider);
            String tmpDir = Settings.getProperty("TempDir");
            String tmpLocation = tmpDir + "temporaryQuoteFile-" + symbol.getSymbolCode() + "-.csv";
            System.out.println("Downloading yahoo " + symbol.getSymbolCode() + " (" + symbol.getSymbolFullName() + ") data...");
            URLrip ripper = new URLrip();
            if (ripper.URLripper(this.sourceProvider, tmpLocation)) {
                switch(importIntoDB) {
                    case AQuoteFetcher.QUOTE_DBMSIMPORT:
                        {
                            System.out.println("Importing yahoo " + symbol.getSymbolCode() + " (" + symbol.getSymbolFullName() + ") data...");
                            CsvImport csv = new CsvImport(tmpLocation, this.xmlImportModel, symbol);
                            break;
                        }
                    case AQuoteFetcher.QUOTE_CVS:
                        {
                            delete = false;
                            System.out.println("File saved in " + tmpLocation);
                            break;
                        }
                    case AQuoteFetcher.QUOTE_STDOUT:
                        {
                            try {
                                BufferedReader in = new BufferedReader(new FileReader(tmpLocation));
                                String str;
                                while ((str = in.readLine()) != null) {
                                    System.out.println(str);
                                }
                                in.close();
                            } catch (IOException e) {
                            }
                            break;
                        }
                }
                if (delete) {
                    if (FileUtils.deleteFile(tmpLocation)) System.out.println("Temporary file deleted"); else System.out.println("Cannot delete temporary file!");
                }
                return SUCCESS;
            } else {
                return ERROR_NOCONNECTION;
            }
        } else {
            return ERROR_NOINTRADAY;
        }
    }

    /**
     * Main, testing purpose only...
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("System init...");
        try {
            YahooFetcher yahooData = new YahooFetcher();
            System.out.print("Checking if symbolCode G.MI exists... ");
            if (yahooData.symbolExists("G.MI")) System.out.println("[oK]."); else System.out.println("[NotExists].");
            System.out.println("Trying getting quotes...");
            yahooData.setSymbol("TIS.MI");
            yahooData.setTimeFrame(new TimeFrame_Daily());
            System.out.println("Timeframe is setted to: " + yahooData.getTimeFrame().getTimeFrameToString());
            Candle price = new Candle();
            price = yahooData.getLastQuote();
            price.print();
            yahooData.setSymbol("TIS.MI");
            yahooData.setXmlImportModel("./jars/config/imports/yahooDaily.xml");
            yahooData.setTimeFrame(new TimeFrame_Daily());
            switch(yahooData.getHistoricalQuotes(2, AQuoteFetcher.QUOTE_STDOUT)) {
                case AQuoteFetcher.SUCCESS:
                    System.out.println("done.");
                    break;
                case AQuoteFetcher.ERROR_NOCONNECTION:
                    System.out.println("No connection available!");
                    break;
                case AQuoteFetcher.ERROR_NOINTRADAY:
                    System.out.println("This source provider does not support intraday quotes!");
                    break;
            }
        } catch (Exception e) {
            System.out.println("Cannot initialising source provider");
        }
        System.out.println("System done.");
    }
}
