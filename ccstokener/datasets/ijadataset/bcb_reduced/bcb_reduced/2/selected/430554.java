package ch.olsen.test.scproject1;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ch.olsen.products.util.Currency;
import ch.olsen.products.util.logging.Logger;
import ch.olsen.servicecontainer.service.HttpFiles;
import ch.olsen.servicecontainer.service.HttpServlet;
import ch.olsen.servicecontainer.service.Logging;
import ch.olsen.servicecontainer.service.Service;
import ch.olsen.servicecontainer.service.ServicePostActivate;
import ch.olsen.servicecontainer.service.Timer;
import ch.olsen.test.scproject1.client.WebIRQuery;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@Service
@HttpFiles(path = "www/ch.olsen.test.scproject1.WebInterestRates/", index = "WebInterestRates.html")
public class OandaInterestRates implements InterestRates {

    Map<Currency, CurrencyIR> lastRates;

    @Logging(name = "InterestRates")
    public Logger log;

    WebIR webir;

    private static class CurrencyIR implements Serializable {

        private static final long serialVersionUID = 1L;

        Currency ccy;

        double borrowing;

        double lending;

        public CurrencyIR(Currency ccy, double borrowing, double lending) {
            this.ccy = ccy;
            this.borrowing = borrowing;
            this.lending = lending;
        }
    }

    @ServicePostActivate
    public void init() {
        lastRates = new HashMap<Currency, CurrencyIR>();
    }

    public double getBorrowingInterestRate(Currency ccy) {
        CurrencyIR last = lastRates.get(ccy);
        if (last != null) return last.borrowing;
        return 0;
    }

    public double getLendingInterestRate(Currency ccy) {
        CurrencyIR last = lastRates.get(ccy);
        if (last != null) return last.lending;
        return 0;
    }

    @Timer(millis = 60000 * 60)
    public void update() {
        fetch();
    }

    private void fetch() {
        try {
            URL url = new URL("https://fx2.oanda.com/mod_perl/user/interestrates.pl");
            URLConnection connection = url.openConnection();
            ((HttpURLConnection) connection).setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            Calendar c = Calendar.getInstance();
            String content = "month=" + Integer.toString(c.get(Calendar.MONTH + 1)) + "&day=" + Integer.toString(c.get(Calendar.DAY_OF_MONTH)) + "&year=" + Integer.toString(c.get(Calendar.YEAR)) + "&startdate=&enddate=" + "&currency=AUD" + "&currency=GBP" + "&currency=CAD" + "&currency=CNY" + "&currency=CZK" + "&currency=DKK" + "&currency=EUR" + "&currency=XAU" + "&currency=HKD" + "&currency=HUF" + "&currency=INR" + "&currency=JPY" + "&currency=MXN" + "&currency=TWD" + "&currency=NZD" + "&currency=NOK" + "&currency=PLN" + "&currency=SAR" + "&currency=XAG" + "&currency=SGD" + "&currency=ZAR" + "&currency=SEK" + "&currency=CHF" + "&currency=THB" + "&currency=TRY" + "&currency=USD";
            DataOutputStream printout = new DataOutputStream(connection.getOutputStream());
            printout.writeBytes(content);
            printout.flush();
            printout.close();
            InputStream is = connection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader r = new BufferedReader(isr);
            String html = "";
            String line;
            while ((line = r.readLine()) != null) {
                html += line;
            }
            char chars[] = html.toCharArray();
            char last = ' ';
            String filtered = "";
            boolean intag = false;
            for (int n = 0; n < chars.length; n++) {
                if (!intag) {
                    if (chars[n] == '<') intag = true; else {
                        if (chars[n] != ' ' || last != ' ') filtered += chars[n];
                        last = chars[n];
                    }
                } else {
                    if (chars[n] == '>') intag = false;
                }
            }
            StringTokenizer tokens = new StringTokenizer(filtered);
            int tot = tokens.countTokens();
            Currency ccy = null;
            double borrowing = 0;
            double lending = 0;
            boolean error = false;
            for (int n = 0; n < tot; n++) {
                String token = tokens.nextToken();
                if (!token.startsWith("&") && n > 9 && (n - 10) % 8 < 3) {
                }
                if (!token.startsWith("&") && n > 9 && (n - 10) % 8 == 0) {
                    try {
                        ccy = Enum.valueOf(Currency.class, token);
                    } catch (Exception e) {
                        error = true;
                    }
                }
                if (!token.startsWith("&") && n > 9 && (n - 10) % 8 == 1) {
                    try {
                        borrowing = Double.parseDouble(token);
                    } catch (Exception e) {
                        error = true;
                    }
                }
                if (!token.startsWith("&") && n > 9 && (n - 10) % 8 == 2) {
                    try {
                        lending = Double.parseDouble(token);
                        if (!error) lastRates.put(ccy, new CurrencyIR(ccy, borrowing, lending));
                    } catch (Exception e) {
                        error = true;
                    }
                }
                if (!token.startsWith("&") && n > 9 && (n - 10) % 8 == 3) {
                    error = false;
                }
                if (n > 9 && (n - 10) % 8 == 3) {
                }
            }
            log.debug("Successfully retrevied oanda interest rates");
        } catch (Exception e) {
            log.warn("Error: " + e.getMessage(), e);
        }
    }

    @HttpServlet(url = "webir")
    public void handleServlet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        synchronized (this) {
            if (webir == null) webir = new WebIR();
        }
        webir.service(request, response);
        response.getWriter().flush();
    }

    public class WebIR extends RemoteServiceServlet implements WebIRQuery {

        private static final long serialVersionUID = 1L;

        public double getBorrowingInterestRate(String ccy) {
            return OandaInterestRates.this.getBorrowingInterestRate(Currency.valueOf(ccy));
        }

        public double getLendingInterestRate(String ccy) {
            return OandaInterestRates.this.getLendingInterestRate(Currency.valueOf(ccy));
        }
    }
}
