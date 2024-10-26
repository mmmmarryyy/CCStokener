package ws4java6.model;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.net.URL;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

@WebService
@SOAPBinding(style = Style.RPC)
public class StockTicker {

    private String urlDataServer = "http://finance.yahoo.com/d/quotes.csv?f=sl1d1t1c1ohgv&e=.csv&s=";

    /**
	 * This method returns the price of a stock
	 * 
	 * 
	 * @param Stock
	 *            identifier
	 * 
	 * @return Actual price
	 */
    public float getPrice(String id) {
        try {
            URL url = new URL(urlDataServer + id);
            InputStream inputStream = url.openStream();
            Reader reader = new InputStreamReader(inputStream);
            StreamTokenizer streamTokenizer = new StreamTokenizer(reader);
            streamTokenizer.nextToken();
            String outSymbol = streamTokenizer.sval;
            if (!id.equalsIgnoreCase(outSymbol)) {
                throw new RuntimeException("Incorrect ID: " + outSymbol);
            }
            streamTokenizer.nextToken();
            streamTokenizer.nextToken();
            float price = (float) streamTokenizer.nval;
            reader.close();
            System.out.println("SERVER - the following price was found: " + price);
            return price;
        } catch (Exception e) {
            System.out.println("SERVER - Error: " + e.getMessage());
            return -999.0F;
        }
    }
}
