package agh.mobile.contactexchange.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import com.google.code.sntpjc.Client;

/**
 * This is an abstract card exchange client. Client implementation may
 * derive from that class and implement missing methods from
 * {@link agh.mobile.contactexchange.client.Connectable} interface.
 * {@link #init()} must be first called method of objects of this class.
 * 
 * @author Witold Sowa <witold.sowa@gmail.com>
 *
 */
public abstract class AbstractClient implements Connectable {

    /**
	 * This file contains exchange server address and port.
	 */
    private static final String ADDRESS_SERVER_URL = "http://student.agh.edu.pl/~wsowa/CEaddr.txt";

    private static final String NTP_SERVER_ADDRESS = "tempus1.gum.gov.pl";

    private static final int NTP_SERVER_TIMEOUT = 1000;

    private static final int NTP_RETRIES_LIMIT = 3;

    ;

    private Connection connection = new Connection();

    private Client ntpClient;

    private double timeOffset;

    /**
	 * Initialize client. During initialization, client obtains NTP time
	 * offset.
	 * 
	 * @throws IOException if error occurred in connection with NTP server
	 */
    public void init() throws IOException {
        ntpClient = new Client(NTP_SERVER_ADDRESS, NTP_SERVER_TIMEOUT);
        int tryNumber = 0;
        while (true) {
            try {
                tryNumber++;
                timeOffset = ntpClient.getLocalOffset();
                break;
            } catch (IOException e) {
                if (tryNumber >= NTP_RETRIES_LIMIT) throw e;
            }
        }
        connection = new Connection();
        connection.registerListener(this);
    }

    /**
	 * Get address of exchange server from file on HTTP server.
	 * 
	 * @return Exchange server address information
	 * @throws IOException if error occurred in connection with address server
	 */
    public InetSocketAddress getServerAddress() throws IOException {
        URL url = new URL(ADDRESS_SERVER_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setDoOutput(true);
        con.setReadTimeout(2000);
        con.connect();
        BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String line = rd.readLine();
        if (line == null) throw new IOException("Cannot read address from address server");
        String addr[] = line.split(" ", 2);
        return new InetSocketAddress(addr[0], Integer.valueOf(addr[1]));
    }

    /**
	 * Return NTP time. Before calling this method, {@link #init()} must be
	 * called first.
	 * 
	 * @return NTP time.
	 */
    public long getNtpTime() {
        return getNtpTime(System.currentTimeMillis());
    }

    /**
	 * Return NTP time. Before calling this method, {@link #init()} must be
	 * called first.
	 * 
	 * @param localTime Local Unix epoch time.
	 * @return NTP time.
	 */
    public long getNtpTime(long localTime) {
        return localTime + (long) timeOffset * 1000;
    }

    /**
	 * @return connection to the server
	 */
    public Connection getConnection() {
        return connection;
    }
}
