package net.sf.solarnetwork.node.impl;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import net.sf.solarnetwork.node.ConsumptionDatum;
import net.sf.solarnetwork.node.DayDatum;
import net.sf.solarnetwork.node.PowerDatum;
import net.sf.solarnetwork.node.PriceDatum;
import net.sf.solarnetwork.node.UploadService;
import net.sf.solarnetwork.node.WeatherDatum;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.xml.sax.InputSource;

/**
 * Implementation of {@link UploadService} that posts HTTP parameters using
 * JavaBean parameter symantics.
 *
 * @author matt.magoffin
 * @version $Revision: 275 $ $Date: 2009-08-07 05:00:11 -0400 (Fri, 07 Aug 2009) $
 */
public class JavaBeanWebPostUploadService extends XmlServiceSupport implements UploadService {

    /** The date format to use for formatting dates with time. */
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSZ";

    /** The date format to use for DayDatum dates. */
    public static final String DAY_DATE_FORMAT = "yyyy-MM-dd";

    /** The date format to use for DayDatum times. */
    public static final String DAY_TIME_FORMAT = "HH:mm";

    /** The default value for the {@code connectionTimeout} property. */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 15000;

    /** The default value for all *DatumIdXPath properties. */
    public static final String DEFAULT_ID_XPATH = "/*/@id";

    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    private String key;

    private String nodeId = null;

    private String powerDatumUrl = null;

    private String powerDatumIdXPath = DEFAULT_ID_XPATH;

    private String consumptionDatumUrl = null;

    private String consumptionDatumIdXPath = DEFAULT_ID_XPATH;

    private String dayDatumUrl = null;

    private String dayDatumIdXPath = DEFAULT_ID_XPATH;

    private String priceDatumUrl = null;

    private String priceDatumIdXPath = DEFAULT_ID_XPATH;

    private String weatherDatumUrl = null;

    private String weatherDatumIdXPath = DEFAULT_ID_XPATH;

    private XPathExpression powerDatumTrackingIdXPath = null;

    private XPathExpression consumptionDatumTrackingIdXPath = null;

    private XPathExpression dayDatumTrackingIdXPath = null;

    private XPathExpression priceDatumTrackingIdXPath = null;

    private XPathExpression weatherDatumTrackingIdXPath = null;

    /**
	 * Initialize this class after properties are set.
	 */
    @Override
    public void init() {
        super.init();
        try {
            XPath xp = getXpathFactory().newXPath();
            if (getNsContext() != null) {
                xp.setNamespaceContext(getNsContext());
            }
            this.powerDatumTrackingIdXPath = xp.compile(this.powerDatumIdXPath);
            this.consumptionDatumTrackingIdXPath = xp.compile(this.consumptionDatumIdXPath);
            this.dayDatumTrackingIdXPath = xp.compile(this.dayDatumIdXPath);
            this.priceDatumTrackingIdXPath = xp.compile(this.priceDatumIdXPath);
            this.weatherDatumTrackingIdXPath = xp.compile(this.weatherDatumIdXPath);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public String getKey() {
        return "JavaBeanWebPostUploadService:" + key;
    }

    public Long uploadDayDatum(DayDatum data) {
        return uploadDayDatum(data, null);
    }

    private Long uploadDayDatum(DayDatum data, Map<String, ?> attributes) {
        BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(data);
        SimpleDateFormat dateFormat = new SimpleDateFormat(DAY_DATE_FORMAT);
        dateFormat.setLenient(false);
        bean.registerCustomEditor(Date.class, "day", new CustomDateEditor(dateFormat, false));
        SimpleDateFormat timeFormat = new SimpleDateFormat(DAY_TIME_FORMAT);
        dateFormat.setLenient(false);
        bean.registerCustomEditor(Date.class, "sunrise", new CustomDateEditor(timeFormat, false));
        bean.registerCustomEditor(Date.class, "sunset", new CustomDateEditor(timeFormat, false));
        return doWebPost(bean, dayDatumUrl, dayDatumTrackingIdXPath, dayDatumIdXPath, attributes);
    }

    public Long uploadPowerDatum(PowerDatum data) {
        BeanWrapper bean = getDefaultBeanWrapper(data);
        return doWebPost(bean, powerDatumUrl, powerDatumTrackingIdXPath, powerDatumIdXPath, null);
    }

    public Long uploadConsumptionDatum(ConsumptionDatum data) {
        BeanWrapper bean = getDefaultBeanWrapper(data);
        return doWebPost(bean, consumptionDatumUrl, consumptionDatumTrackingIdXPath, consumptionDatumIdXPath, null);
    }

    public Long uploadPriceDatum(PriceDatum data) {
        BeanWrapper bean = getDefaultBeanWrapper(data);
        return doWebPost(bean, priceDatumUrl, priceDatumTrackingIdXPath, priceDatumIdXPath, null);
    }

    public Long uploadWeatherDatum(WeatherDatum data) {
        return uploadWeatherDatum(data, null);
    }

    private Long uploadWeatherDatum(WeatherDatum data, Map<String, ?> attributes) {
        BeanWrapper bean = getDefaultBeanWrapper(data);
        return doWebPost(bean, weatherDatumUrl, weatherDatumTrackingIdXPath, weatherDatumIdXPath, attributes);
    }

    public Long[] uploadDayAndWeatherDatum(DayDatum dayData, WeatherDatum weatherData, Map<String, ?> attributes) {
        return new Long[] { uploadDayDatum(dayData, attributes), uploadWeatherDatum(weatherData, attributes) };
    }

    private BeanWrapper getDefaultBeanWrapper(Object data) {
        BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(data);
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
        dateFormat.setLenient(false);
        bean.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
        return bean;
    }

    private Long doWebPost(BeanWrapper bean, String url, XPathExpression trackingIdXPath, String xpath, Map<String, ?> attributes) {
        String respXml = null;
        try {
            URLConnection conn = new URL(url).openConnection();
            if (conn instanceof HttpURLConnection) {
                HttpURLConnection hConn = (HttpURLConnection) conn;
                hConn.setRequestMethod("POST");
            }
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            conn.setRequestProperty("Accept", "text/*");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setConnectTimeout(this.connectionTimeout);
            conn.setReadTimeout(connectionTimeout);
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            PropertyDescriptor[] props = bean.getPropertyDescriptors();
            Object theNodeId = this.nodeId;
            if (attributes != null && attributes.containsKey("node-id")) {
                theNodeId = attributes.get("node-id");
            }
            out.write("nodeId=" + theNodeId);
            for (int i = 0; i < props.length; i++) {
                PropertyDescriptor prop = props[i];
                if (prop.getReadMethod() == null) {
                    continue;
                }
                String propName = prop.getName();
                if ("class".equals(propName)) {
                    continue;
                }
                Object propValue = null;
                if (attributes != null && attributes.containsKey(propName)) {
                    propValue = attributes.get(propName);
                } else {
                    PropertyEditor editor = bean.findCustomEditor(prop.getPropertyType(), prop.getName());
                    if (editor != null) {
                        editor.setValue(bean.getPropertyValue(propName));
                        propValue = editor.getAsText();
                    } else {
                        propValue = bean.getPropertyValue(propName);
                    }
                }
                if (propValue == null) {
                    continue;
                }
                out.write("&");
                out.write(propName);
                out.write("=");
                out.write(URLEncoder.encode(propValue.toString(), "UTF-8"));
            }
            out.flush();
            out.close();
            BufferedReader resp = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder buf = new StringBuilder();
            String str;
            while ((str = resp.readLine()) != null) {
                buf.append(str);
            }
            respXml = buf.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (log.isLoggable(Level.FINER)) {
            log.finer("Response: " + respXml);
        }
        return extractTrackingId(respXml, trackingIdXPath, xpath);
    }

    private Long extractTrackingId(String xml, XPathExpression xp, String xpath) {
        Double tid;
        try {
            tid = (Double) xp.evaluate(new InputSource(new StringReader(xml)), XPathConstants.NUMBER);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        if (tid.isNaN()) {
            log.warning("Unable to extract tracking ID via XPath [" + xpath + ']');
            return null;
        }
        return tid.longValue();
    }

    /**
	 * @return the nodeId
	 */
    public String getNodeId() {
        return nodeId;
    }

    /**
	 * @param nodeId the nodeId to set
	 */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
	 * @return the powerDatumIdXPath
	 */
    public String getPowerDatumIdXPath() {
        return powerDatumIdXPath;
    }

    /**
	 * @param powerDatumIdXPath the powerDatumIdXPath to set
	 */
    public void setPowerDatumIdXPath(String powerDatumIdXPath) {
        this.powerDatumIdXPath = powerDatumIdXPath;
    }

    /**
	 * @return the powerDatumUrl
	 */
    public String getPowerDatumUrl() {
        return powerDatumUrl;
    }

    /**
	 * @param powerDatumUrl the powerDatumUrl to set
	 */
    public void setPowerDatumUrl(String powerDatumUrl) {
        this.powerDatumUrl = powerDatumUrl;
    }

    /**
	 * @return the dayDatumUrl
	 */
    public String getDayDatumUrl() {
        return dayDatumUrl;
    }

    /**
	 * @param dayDatumUrl the dayDatumUrl to set
	 */
    public void setDayDatumUrl(String dayDatumUrl) {
        this.dayDatumUrl = dayDatumUrl;
    }

    /**
	 * @return the dayDatumIdXPath
	 */
    public String getDayDatumIdXPath() {
        return dayDatumIdXPath;
    }

    /**
	 * @param dayDatumIdXPath the dayDatumIdXPath to set
	 */
    public void setDayDatumIdXPath(String dayDatumIdXPath) {
        this.dayDatumIdXPath = dayDatumIdXPath;
    }

    /**
	 * @return the weatherDatumUrl
	 */
    public String getWeatherDatumUrl() {
        return weatherDatumUrl;
    }

    /**
	 * @param weatherDatumUrl the weatherDatumUrl to set
	 */
    public void setWeatherDatumUrl(String weatherDatumUrl) {
        this.weatherDatumUrl = weatherDatumUrl;
    }

    /**
	 * @return the weatherDatumIdXPath
	 */
    public String getWeatherDatumIdXPath() {
        return weatherDatumIdXPath;
    }

    /**
	 * @param weatherDatumIdXPath the weatherDatumIdXPath to set
	 */
    public void setWeatherDatumIdXPath(String weatherDatumIdXPath) {
        this.weatherDatumIdXPath = weatherDatumIdXPath;
    }

    /**
	 * @param key the key to set
	 */
    public void setKey(String key) {
        this.key = key;
    }

    /**
	 * @return the connectionTimeout
	 */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
	 * @param connectionTimeout the connectionTimeout to set
	 */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
	 * @return the consumptionDatumIdXPath
	 */
    public String getConsumptionDatumIdXPath() {
        return consumptionDatumIdXPath;
    }

    /**
	 * @param consumptionDatumIdXPath the consumptionDatumIdXPath to set
	 */
    public void setConsumptionDatumIdXPath(String consumptionDatumIdXPath) {
        this.consumptionDatumIdXPath = consumptionDatumIdXPath;
    }

    /**
	 * @return the consumptionDatumUrl
	 */
    public String getConsumptionDatumUrl() {
        return consumptionDatumUrl;
    }

    /**
	 * @param consumptionDatumUrl the consumptionDatumUrl to set
	 */
    public void setConsumptionDatumUrl(String consumptionDatumUrl) {
        this.consumptionDatumUrl = consumptionDatumUrl;
    }

    /**
	 * @return the priceDatumIdXPath
	 */
    public String getPriceDatumIdXPath() {
        return priceDatumIdXPath;
    }

    /**
	 * @param priceDatumIdXPath the priceDatumIdXPath to set
	 */
    public void setPriceDatumIdXPath(String priceDatumIdXPath) {
        this.priceDatumIdXPath = priceDatumIdXPath;
    }

    /**
	 * @return the priceDatumUrl
	 */
    public String getPriceDatumUrl() {
        return priceDatumUrl;
    }

    /**
	 * @param priceDatumUrl the priceDatumUrl to set
	 */
    public void setPriceDatumUrl(String priceDatumUrl) {
        this.priceDatumUrl = priceDatumUrl;
    }
}
