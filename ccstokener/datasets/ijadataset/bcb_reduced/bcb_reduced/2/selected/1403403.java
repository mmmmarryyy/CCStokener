package yahoofinance.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.IndexColumn;
import yahoofinance.data.model.Dividend;
import yahoofinance.data.model.HistoricalPrice;

@Entity
public class HistoricalPrices implements YahooFinancePage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    long id;

    @OneToMany(cascade = { CascadeType.ALL })
    @JoinTable(name = "HistoricalPricesHistoricalPrice", joinColumns = @JoinColumn(name = "historical_prices_id"), inverseJoinColumns = @JoinColumn(name = "historical_price_id"))
    @IndexColumn(name = "HistoricalPrice_list_index")
    private List<HistoricalPrice> historicalPrices;

    @OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
    @JoinTable(name = "HistoricalPricesDividends", joinColumns = @JoinColumn(name = "historical_prices_id"), inverseJoinColumns = @JoinColumn(name = "dividend_id"))
    @IndexColumn(name = "dividend_list_index")
    private List<Dividend> dividends;

    @Transient
    private static final Logger logger = Logger.getLogger(HistoricalPrices.class);

    private String symbol;

    public HistoricalPrices(final String symbol) {
        setSymbol(symbol);
        historicalPrices = new ArrayList<HistoricalPrice>();
        dividends = new ArrayList<Dividend>();
        loadData();
    }

    public HistoricalPrices() {
    }

    @Override
    public void loadData() {
        try {
            String url = "http://ichart.finance.yahoo.com/table.csv?s=" + symbol + "&a=00&b=2&c=1962&d=11&e=11&f=2099&g=d&ignore=.csv";
            URLConnection conn = (new URL(url)).openConnection();
            conn.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            in.readLine();
            String str = "";
            while ((str = in.readLine()) != null) {
                final String[] split = str.split(",");
                final String date = split[0];
                final double open = Double.parseDouble(split[1]);
                final double high = Double.parseDouble(split[2]);
                final double low = Double.parseDouble(split[3]);
                final double close = Double.parseDouble(split[4]);
                final int volume = Integer.parseInt(split[5]);
                final double adjClose = Double.parseDouble(split[6]);
                final HistoricalPrice price = new HistoricalPrice(date, open, high, low, close, volume, adjClose);
                historicalPrices.add(price);
            }
            in.close();
            url = "http://ichart.finance.yahoo.com/table.csv?s=" + symbol + "&a=00&b=2&c=1962&d=11&e=17&f=2008&g=v&ignore=.csv";
            conn = (new URL(url)).openConnection();
            conn.connect();
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            in.readLine();
            str = "";
            while ((str = in.readLine()) != null) {
                final String[] split = str.split(",");
                final String date = split[0];
                final double amount = Double.parseDouble(split[1]);
                final Dividend dividend = new Dividend(date, amount);
                dividends.add(dividend);
            }
            in.close();
        } catch (final IOException ioe) {
            logger.error("Error parsing historical prices for " + getSymbol(), ioe);
        }
    }

    /**
	 * Gets a List of HistoricalPrice objects for the symbol given in the
	 * constructor
	 * 
	 * @return the list of HistoricalPrice objects for the symbol given in the
	 *         constructor
	 */
    public List<HistoricalPrice> getHistoricalPrices() {
        return historicalPrices;
    }

    /**
	 * Gets a List of Dividend objects for the symbol given in the constructor
	 * 
	 * @return the List of Dividend objects for the symbol given in the
	 *         constructor
	 */
    public List<Dividend> getDividends() {
        return dividends;
    }

    public void setHistoricalPrices(final List<HistoricalPrice> historicalPrices) {
        this.historicalPrices = historicalPrices;
    }

    public void setDividends(final List<Dividend> dividends) {
        this.dividends = dividends;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public void setId(final long id) {
        this.id = id;
    }

    @Override
    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }
}
