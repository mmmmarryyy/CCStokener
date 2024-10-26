package mercury;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import mercury.gui.MainGUI;
import mercury.util.ClientHttpRequest;
import mercury.util.CustomAuthenticator;
import mercury.util.Log;
import org.apache.log4j.PropertyConfigurator;

public class Main {

    private Log logger = new Log(this);

    private TimexProperties timexfrmk = new TimexProperties();

    private Properties properties = new Properties();

    private String user = "";

    private String password = "";

    private String adminfeedkey = "";

    private String version = "v1.0";

    public Main() {
        super();
        String loggerProps = "props/logger-console.properties";
        String props = "props/application.properties";
        System.out.println("Loading logger properties from: " + loggerProps);
        if (!(new File(loggerProps)).exists()) {
            System.out.println("Could not find logger properties, cannot continue: " + loggerProps);
            System.exit(1);
        }
        PropertyConfigurator.configure(loggerProps);
        logger.info("Loading properties from: " + props);
        if (!(new File(props)).exists()) {
            logger.error("Could not find properties, cannot continue: " + props);
            System.exit(1);
        }
        try {
            properties.load(new FileInputStream(props));
        } catch (FileNotFoundException e) {
            logger.error(e);
            logger.trace(e, e);
            System.exit(1);
        } catch (IOException e) {
            logger.error(e);
            logger.trace(e, e);
            System.exit(1);
        }
        String timexdao = properties.getProperty("timexdao");
        if (timexdao == null || timexdao.equals("")) {
            logger.error("Could not load property timexdao.  Application not continue.");
            System.exit(1);
        }
        Enumeration enumr = properties.keys();
        Map timexdaoprops = new HashMap();
        timexdaoprops.put("dao", timexdao);
        for (; enumr.hasMoreElements(); ) {
            String name = (String) enumr.nextElement();
            if (name.startsWith("timex" + timexdao)) {
                String value = properties.getProperty(name);
                timexdaoprops.put(name.substring(("timex" + timexdao).length()), value);
            }
        }
        logger.debug("Setting Timex DAO (" + timexdao + ") properties: " + timexdaoprops.toString());
        timexfrmk.init(timexdaoprops);
        user = properties.getProperty("mercuriususer");
        password = properties.getProperty("mercuriuspassword");
        adminfeedkey = properties.getProperty("mercuriusadminfeedkey");
    }

    public boolean isPasswordSet() {
        if (password != null && password.length() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isAdminFeedKeySet() {
        if (adminfeedkey != null && adminfeedkey.length() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public void setAdminFeedKeySet(String k) {
        adminfeedkey = k;
    }

    public void setPassword(String pwd) {
        password = pwd;
    }

    public boolean isUserSet() {
        if (user != null && user.length() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public void setUser(String usr) {
        user = usr;
    }

    public void run() {
        long time = System.currentTimeMillis();
        logger.info("Version: " + version);
        String hostname = properties.getProperty("mercuriushost");
        String protocol = properties.getProperty("mercuriusprotocol");
        String port = properties.getProperty("mercuriusport");
        String path = properties.getProperty("mercuriuspath");
        String action = properties.getProperty("mercuriusaction");
        logger.info("Getting Timex Data --- " + getTimeDifferent(time));
        String xml = timexfrmk.getUnsynchedSessionsXMLFormat();
        logger.info("Done getting Timex Data --- " + getTimeDifferent(time));
        if (timexfrmk.getSessionCount() > 0) {
            try {
                logger.info("Sending Timex Data to Mercurius --- " + getTimeDifferent(time));
                String data = URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode(action, "UTF-8");
                data += "&" + URLEncoder.encode("data", "UTF-8") + "=" + URLEncoder.encode(xml, "UTF-8");
                if (isAdminFeedKeySet()) {
                    data += "&" + URLEncoder.encode("adminfeedkey", "UTF-8") + "=" + URLEncoder.encode(adminfeedkey, "UTF-8");
                    logger.debug("Using adminfeedkey to authenticate");
                } else {
                    Authenticator.setDefault(new CustomAuthenticator(user, password));
                    logger.debug("Using user/pwd to authenticate");
                }
                String u = protocol + "://" + hostname + ":" + port + path;
                logger.debug("Posting xml data to: " + u);
                URL url = new URL(u);
                URLConnection conn = url.openConnection();
                conn.setRequestProperty("User-Agent", "TimexMercurius/" + version);
                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(data);
                wr.flush();
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                StringBuffer s = new StringBuffer();
                while ((line = rd.readLine()) != null) {
                    s.append(line);
                }
                if (s.length() > 0) {
                    logger.debug(s.toString());
                    if (s.toString().indexOf("Result:") != -1) {
                        logger.info(s.toString().substring(s.toString().indexOf("Result:")));
                        if (s.toString().indexOf("Result: Successful") != -1) {
                            timexfrmk.updateSessionsStatus();
                        }
                    } else {
                        logger.error(s.toString());
                    }
                } else {
                    logger.info("No data returned");
                }
                wr.close();
                rd.close();
            } catch (MalformedURLException e) {
                logger.error(e);
                logger.trace(e, e);
            } catch (IOException e) {
                logger.error(e);
                logger.trace(e, e);
            } catch (Exception e) {
                logger.error(e);
                logger.trace(e, e);
            }
        } else {
            logger.info("There is nothing to send.  Everything has already been synchronized");
        }
        timexfrmk.close();
        logger.info("Done!!! Total Time: " + getTimeDifferent(time));
    }

    public String getTimeDifferent(long millis) {
        try {
            DateFormat secsfrm = new SimpleDateFormat("ss");
            Date d = (Date) secsfrm.parse(String.valueOf((System.currentTimeMillis() - millis) / 1000));
            DateFormat timefrm = new SimpleDateFormat("HH:mm:ss");
            return timefrm.format(d);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }
}
