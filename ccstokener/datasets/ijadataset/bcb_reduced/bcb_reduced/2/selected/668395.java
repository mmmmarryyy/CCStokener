package yahoofinance.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.IndexColumn;
import yahoofinance.util.NullReturningArrayList;

@Entity
public class Quote implements YahooFinancePage {

    @CollectionOfElements(fetch = FetchType.EAGER)
    @IndexColumn(name = "value_index")
    List<String> values;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    long id;

    @Transient
    private static final Logger logger = Logger.getLogger(Quote.class);

    private String symbol;

    public Quote(final String symbol) {
        setSymbol(symbol);
        loadData();
    }

    public Quote() {
    }

    public void loadData() {
        try {
            String tags = "aa2a5";
            tags += "bb2b3";
            tags += "b4b6c";
            tags += "c1c3c6";
            tags += "c8dd1";
            tags += "d2ee1";
            tags += "e7e8e9";
            tags += "gh";
            tags += "jkg1";
            tags += "g3g4g5";
            tags += "g6ii5";
            tags += "j1j3j4";
            tags += "j5j6k1";
            tags += "k2k3k4";
            tags += "k5ll1";
            tags += "l2l3m";
            tags += "m2m3m4";
            tags += "m5m6m7";
            tags += "m8nn4";
            tags += "opp1";
            tags += "p2p5p6qrr1r2r5r6";
            tags += "r7ss1s7t1t6t7t8vv1v7ww1w4xy";
            final String url = "http://finance.yahoo.com/d/quotes.csv?s=" + symbol + "&f=" + tags;
            final URLConnection conn = (new URL(url)).openConnection();
            conn.connect();
            final BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            final String str = in.readLine();
            values = new NullReturningArrayList<String>();
            final String[] split = str.split(",");
            for (String s : split) {
                if (s.length() > 254) {
                    s = s.substring(0, 254);
                }
                values.add(s);
            }
            in.close();
        } catch (final IOException ioe) {
            logger.error("Error parsing quote csv", ioe);
        }
    }

    public String getAsk() {
        return values.get(0);
    }

    public String getAverageDailyVolume() {
        return values.get(1);
    }

    public String getAskSize() {
        return values.get(2);
    }

    public String getBid() {
        return values.get(3);
    }

    public String getAskRealTime() {
        return values.get(4);
    }

    public String getBidRealTime() {
        return values.get(5);
    }

    public String getBookValue() {
        return values.get(6);
    }

    public String getBidSize() {
        return values.get(7);
    }

    public String getPercentChange() {
        final String[] split = values.get(8).split(" - ");
        if (split.length > 1) return split[1].substring(0, split[1].length() - 1);
        return values.get(8);
    }

    public String getChange() {
        return values.get(9);
    }

    public String getCommission() {
        return values.get(10);
    }

    public String getChangeRealTime() {
        if (values.get(11).length() > 2) return values.get(11).substring(1, values.get(11).length() - 1);
        return values.get(11);
    }

    public String getAfterHoursChangeRealTime() {
        return values.get(12).substring(1, values.get(12).length() - 1);
    }

    public String getDividendPerShare() {
        return values.get(13);
    }

    public String getLastTradeDate() {
        return values.get(14).substring(1, values.get(14).length() - 1);
    }

    public String getTradeDate() {
        return values.get(15);
    }

    public String getEarningsPerShare() {
        return values.get(16);
    }

    public String getErrorIndication() {
        return values.get(17).substring(1, values.get(17).length() - 1);
    }

    public String getEPSEstimateCurrentYear() {
        return values.get(18);
    }

    public String getEPSEstimateNextYear() {
        return values.get(19);
    }

    public String getEPSEstimateNextQuarter() {
        return values.get(20);
    }

    public String getDayLow() {
        return values.get(21);
    }

    public String getDayHigh() {
        return values.get(22);
    }

    public String get52WeekLow() {
        return values.get(23);
    }

    public String get52WeekHigh() {
        return values.get(24);
    }

    public String getHoldingsGainPercent() {
        return values.get(25).substring(1, values.get(25).length() - 1);
    }

    public String getAnnualizedGain() {
        return values.get(26).substring(1, values.get(26).length() - 1);
    }

    public String getHoldingsGain() {
        return values.get(27);
    }

    public String getHoldingsGainPercentRealTime() {
        return values.get(28).substring(1, values.get(28).length() - 1);
    }

    public String getHoldingsGainRealTime() {
        return values.get(29);
    }

    public String getMoreInfo() {
        return values.get(30).substring(1, values.get(30).length() - 1);
    }

    public String getOrderBookRealTime() {
        return values.get(31).substring(1, values.get(31).length() - 1);
    }

    public String getMarketCapitalization() {
        return values.get(32);
    }

    public String getMarketCapRealTime() {
        return values.get(33);
    }

    public String getEBITDA() {
        return values.get(34);
    }

    public String getChangeFrom52WeekLow() {
        return values.get(35);
    }

    public String getPercentChangeFrom52WeekLow() {
        return values.get(36);
    }

    public String getLastTradeRealTime() {
        final String lastTrade = values.get(37).split(" - ")[1].replace("<b>", "").replace("</b>", "");
        return lastTrade.substring(0, lastTrade.length() - 1);
    }

    public String getChangePercentRealTime() {
        final String str = values.get(38).split(" - ")[1];
        return str.substring(0, str.length() - 1);
    }

    public String getLastTradeSize() {
        return values.get(39);
    }

    public String getChangeFrom52WeekHigh() {
        return values.get(40);
    }

    public String getPercentChangeFrom52WeekHigh() {
        return values.get(41);
    }

    public String getLastTrade() {
        return values.get(42).split(" - ")[0].substring(1);
    }

    public String getLastTradePrice() {
        return values.get(43).replace("<b>", "").replace("</b>", "");
    }

    public String getHighLimit() {
        return values.get(44);
    }

    public String getLowLimit() {
        return values.get(45);
    }

    public String getDayRange() {
        if (values.get(46).length() > 1) {
            return values.get(46).substring(1, values.get(46).length() - 1);
        } else {
            return values.get(46);
        }
    }

    public String getDayRangeRealTime() {
        return values.get(47).substring(1, values.get(47).length() - 1);
    }

    public String get50DayMovingAverage() {
        return values.get(48);
    }

    public String get200DayMovingAverage() {
        return values.get(49);
    }

    public String getChangeFrom200DayMovingAverage() {
        return values.get(50);
    }

    public String getPercentChangeFrom200DayMovingAverage() {
        return values.get(51);
    }

    public String getChangeFrom50DayMovingAverage() {
        return values.get(52);
    }

    public String getPercentChangeFrom50DayMovingAverage() {
        return values.get(53);
    }

    public String getName() {
        return values.get(54).substring(1, values.get(54).length() - 1);
    }

    public String getNotes() {
        return values.get(55).substring(1, values.get(55).length() - 1);
    }

    public String getOpen() {
        return values.get(56);
    }

    public String getPreviousClose() {
        return values.get(57);
    }

    public String getPricePaid() {
        return values.get(58);
    }

    public String getChangeInPercent() {
        if (values.get(59).length() > 1) {
            return values.get(59).substring(1, values.get(59).length() - 1);
        } else {
            return values.get(59);
        }
    }

    public String getPricePerSales() {
        return values.get(60);
    }

    public String getPricePerBook() {
        return values.get(61);
    }

    public String getExDividendDate() {
        return values.get(62).substring(1, values.get(62).length() - 1);
    }

    public String getPERatio() {
        return values.get(63);
    }

    public String getDividendPayDate() {
        return values.get(64).substring(1, values.get(64).length() - 1);
    }

    public String getPERatioRealTime() {
        return values.get(65);
    }

    public String getPEGRatio() {
        return values.get(66);
    }

    public String getPricePerEPSEstimateCurrentYear() {
        return values.get(67);
    }

    public String getPricePerEPSEstimateNextYear() {
        return values.get(68);
    }

    public String getQuoteSymbol() {
        return values.get(69).substring(1, values.get(69).length() - 1);
    }

    public String getSharesOwned() {
        return values.get(70);
    }

    public String getShortRatio() {
        return values.get(71);
    }

    public String getLastTradeTime() {
        return values.get(72).substring(1, values.get(72).length() - 1);
    }

    public String getTickerTrend() {
        return values.get(74).substring(1, values.get(74).length() - 1).replace("&nbsp;", "");
    }

    public String get1YearTargetPrice() {
        return values.get(75).replace("\"&nbsp;", "").replace("&nbsp;\"", "");
    }

    public String getVolume() {
        return values.get(76);
    }

    public String getHoldingsValue() {
        return values.get(77);
    }

    public String getHoldingsValueRealTime() {
        return values.get(78);
    }

    public String get52WeekRange() {
        return values.get(79).substring(1, values.get(79).length() - 1);
    }

    public String getDayValuechange() {
        return values.get(80).substring(1, values.get(80).length() - 1);
    }

    public String getDayValueChangeRealTime() {
        return values.get(81).substring(1, values.get(81).length() - 1);
    }

    public String getStockExchange() {
        return values.get(82).substring(1, values.get(82).length() - 1);
    }

    public String getDividendYield() {
        return values.get(83);
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(final List<String> values) {
        this.values = values;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(final long id) {
        this.id = id;
    }

    @Override
    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
