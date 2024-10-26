package net.sf.imca.model;

import java.awt.Color;
import java.awt.Paint;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import net.webservicex.www.currencyconvertor.Currency;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ImcaBO {

    /**
     * Apache Commons Logger.
     */
    protected Log log = LogFactory.getLog(this.getClass());

    protected String getAbsoluteUrlFromStringUrl(String url) {
        if (url != null && url.length() != 0) {
            if (url.startsWith("http")) {
                return url;
            } else {
                if (url.startsWith("/")) {
                    return ImcaObjectInterface.IMCA_WEBSITE_URL + url;
                } else {
                    return ImcaObjectInterface.IMCA_WEBSITE_URL + "/" + url;
                }
            }
        }
        return "";
    }

    public String getAbsoluteUrl() {
        return getAbsoluteUrlFromStringUrl("");
    }

    public String getRelativeUrl() {
        if (this.getAbsoluteUrl().startsWith(ImcaObjectInterface.IMCA_WEBSITE_URL)) {
            return this.getAbsoluteUrl().substring(ImcaObjectInterface.IMCA_WEBSITE_URL.length());
        } else {
            return this.getAbsoluteUrl();
        }
    }

    public static String getMessage(String messageKey, Locale locale) {
        ResourceBundle i18n = ResourceBundle.getBundle(ImcaObjectInterface.IMCA_MESSAGE_BUNDLE_BASE, locale);
        try {
            return i18n.getString(messageKey);
        } catch (java.util.MissingResourceException mre) {
            try {
                i18n = ResourceBundle.getBundle(ImcaObjectInterface.IMCA_MESSAGE_BUNDLE_BASE, Locale.ENGLISH);
                return i18n.getString(messageKey);
            } catch (java.util.MissingResourceException mre2) {
                return messageKey;
            }
        }
    }

    public static String[] getCountryCodes() {
        return Locale.getISOCountries();
    }

    public static String[] getCurrencyCodes() {
        return new String[] { Currency._AED, Currency._AUD, Currency._CHF, Currency._EUR, Currency._GBP, Currency._USD };
    }

    private static final SimpleDateFormat sdfXML = new SimpleDateFormat(ImcaObjectInterface.XML_DATE_PATTERN);

    public static final String MAIL_ENCODING = "UTF-8";

    private static final SimpleDateFormat sdf = new SimpleDateFormat(ImcaObjectInterface.IMCA_DATE_PATTERN);

    public static final Paint COLOR_RED = new Color(202, 3, 3);

    public static MimeMessage createEmailMessage() throws MessagingException {
        Properties props = System.getProperties();
        InternetAddress from = new InternetAddress("imcaadmin@moth-sailing.org");
        props.put("mail.smtp.host", ImcaObjectInterface.MAIL_SERVER);
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(from);
        message.setSentDate(new Date());
        return message;
    }

    public static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return sdf.format(date);
    }

    public static Date parseDate(String dateStirng) throws ParseException {
        if (dateStirng == null || dateStirng.trim().equals("")) {
            return null;
        }
        return sdf.parse(dateStirng);
    }

    public static Date parseXMLDate(String dateStirng) throws ParseException {
        if (dateStirng == null) {
            return null;
        }
        return sdfXML.parse(dateStirng);
    }

    /**
     * This is to fix XML data problems.
     *
     * @param oldXmlCode
     * @return
     */
    public static String convertOldXmlCountryCodesToISO(final String oldXmlCode) {
        String code = oldXmlCode.toUpperCase(Locale.ENGLISH);
        if ("UK".equals(code) || "ENGLAND".equals(code)) {
            return "GB";
        } else if ("USA".equals(code)) {
            return "US";
        } else if ("JAPAN".equals(code)) {
            return "JP";
        } else if ("SOUTH AFRICA".equals(code)) {
            return "ZA";
        } else if ("FRANCE".equals(code)) {
            return "FR";
        } else if ("AUSTRALIA".equals(code)) {
            return "AU";
        } else if ("UNITED KINGDOM".equals(code)) {
            return "GB";
        } else if ("SINGAPORE".equals(code)) {
            return "SG";
        } else if ("GERMANY".equals(code)) {
            return "DE";
        } else if ("HOLLAND".equals(code)) {
            return "NL";
        } else if ("ITALIA".equals(code)) {
            return "IT";
        } else if ("SWEDEN".equals(code)) {
            return "SE";
        } else if ("FINLAND".equals(code)) {
            return "FI";
        } else if ("ITALY".equals(code)) {
            return "IT";
        } else if ("DENMARK".equals(code)) {
            return "DK";
        } else if ("AUSTRIA".equals(code)) {
            return "AT";
        }
        return oldXmlCode.toUpperCase();
    }

    public String getCountry(String countryCode) {
        try {
            Locale loc = new Locale("en", countryCode);
            String country = loc.getDisplayCountry(loc);
            return country;
        } catch (Exception e) {
            log.error("Can not get country for person: " + countryCode, e);
            return "";
        }
    }

    public String getTitle() {
        return toString();
    }

    public String[] doGeoQuery(String query) throws IOException {
        String baseURL = "http://maps.google.com/maps/geo?output=csv&keyABQIAAAAct2NN7QKbyiMr1rfhB6UGBQn1DChMmG6tCCZd3aXbcL03vlL5hSUZpyoaGCXRwjbRTSBi0L89DeYeg&q=";
        URL url = new URL(baseURL + URLEncoder.encode(query, "UTF-8"));
        URLConnection connection = url.openConnection();
        StringBuffer buf = new StringBuffer();
        InputStream is = (InputStream) connection.getContent();
        int b = -1;
        while ((b = is.read()) != -1) {
            buf.append((char) b);
        }
        log.info("Geo Query " + url.toExternalForm() + " => " + buf.toString());
        return buf.toString().split(",");
    }
}
