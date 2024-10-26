package de.forsthaus.backend.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import de.forsthaus.backend.dao.Ip4CountryDAO;
import de.forsthaus.backend.model.Ip4Country;
import de.forsthaus.backend.service.Ip4CountryService;

/**
 * EN: Service implementation for methods that depends on <b>Ip4Country</b>.<br>
 * DE: Service Methoden Implementierung betreffend <b>Ip4Country</b>.<br>
 * 
 * @author bbruhns
 * @author sgerth
 */
public class Ip4CountryServiceImpl implements Ip4CountryService, Serializable {

    private static final long serialVersionUID = 893318843695896685L;

    private static final Logger logger = Logger.getLogger(Ip4CountryServiceImpl.class);

    private final String updateUrl = "ftp://ftp.wayne.edu/hostip.info/csv/hip_ip4_country.csv";

    private Ip4CountryDAO ip4CountryDAO;

    public void setIp4CountryDAO(Ip4CountryDAO ip4CountryDAO) {
        this.ip4CountryDAO = ip4CountryDAO;
    }

    public Ip4CountryDAO getIp4CountryDAO() {
        return this.ip4CountryDAO;
    }

    /**
	 * Converts an ip-address to a long value.<br>
	 * 
	 * @param address
	 * @return
	 */
    private static long inetAddressToLong(InetAddress address) {
        if (address.isAnyLocalAddress()) return 0l;
        final byte[] bs = address.getAddress();
        return bs[0] * 16777216l + bs[1] * 65536 + bs[2] * 256 + bs[3];
    }

    @Override
    public Ip4Country getIp4Country(InetAddress address) {
        final Long lg = Long.valueOf(inetAddressToLong(address));
        return this.ip4CountryDAO.getCountryID(lg);
    }

    public void saveOrUpdate(Ip4Country ip4Country) {
        getIp4CountryDAO().saveOrUpdate(ip4Country);
    }

    @Override
    public int importIP4CountryCSV() {
        try {
            getIp4CountryDAO().deleteAll();
            final URL url = new URL(this.updateUrl);
            final URLConnection conn = url.openConnection();
            final InputStream istream = conn.getInputStream();
            final BufferedReader in = new BufferedReader(new InputStreamReader(istream));
            try {
                final Pattern splitterPattern = Pattern.compile(",");
                int counter = 0;
                String aLine = null;
                while (null != (aLine = in.readLine())) {
                    final String[] array = splitterPattern.split(aLine.trim());
                    final long ip = Long.parseLong(array[0]);
                    final long country = Long.parseLong(array[1]);
                    final Ip4Country tmp = this.ip4CountryDAO.getNewIp4Country();
                    tmp.setI4coCcdId(country);
                    tmp.setI4coIp(ip);
                    getIp4CountryDAO().saveOrUpdate(tmp);
                    if (logger.isDebugEnabled() && ++counter % 100 == 0) {
                        logger.debug("Aktueller Zaehler: " + counter);
                    }
                }
            } finally {
                in.close();
                istream.close();
            }
            return getIp4CountryDAO().getCountAllIp4Countries();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getCountAllIp4Countries() {
        return getIp4CountryDAO().getCountAllIp4Countries();
    }
}
