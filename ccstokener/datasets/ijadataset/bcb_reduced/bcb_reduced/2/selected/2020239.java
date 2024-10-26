package mypodsync.service.calendar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import mypodsync.Config;
import mypodsync.service.AbstractService;
import mypodsync.service.Service;
import sun.misc.BASE64Encoder;

public class GoogleCalendarService extends AbstractService implements Service {

    private GoogleCalendarConfig googleCalendarConfig;

    private Logger log = Logger.getLogger(GoogleCalendarService.class.getName());

    public GoogleCalendarService() {
        googleCalendarConfig = new GoogleCalendarConfig();
    }

    public Config getConfig() {
        return googleCalendarConfig;
    }

    public String getFolderToLookup() {
        return googleCalendarConfig.getFolder();
    }

    public String getName() {
        return "GoogleCalendar";
    }

    public void execute() {
        File saveIn = new File(getFolderToLookup());
        Map<String, String> cals = googleCalendarConfig.getCalendars();
        Proxy proxy = null;
        if (serviceManager.getGeneralConfig().useProxy()) {
            SocketAddress sa = new InetSocketAddress(serviceManager.getGeneralConfig().getProxyHost(), serviceManager.getGeneralConfig().getProxyPort());
            proxy = new Proxy(Proxy.Type.HTTP, sa);
        }
        for (Iterator<String> iter = cals.keySet().iterator(); iter.hasNext(); ) {
            String key = iter.next();
            String val = cals.get(key);
            log.fine("Saving " + key + " in " + saveIn);
            BufferedInputStream bis = null;
            BufferedOutputStream bos = null;
            try {
                URL url = new URL(val);
                HttpURLConnection con;
                if (proxy != null) {
                    con = (HttpURLConnection) url.openConnection(proxy);
                } else {
                    con = (HttpURLConnection) url.openConnection();
                }
                String pwd = serviceManager.getGeneralConfig().getUserName() + ":" + new String(serviceManager.getGeneralConfig().getPassword());
                String encodedPwd = new BASE64Encoder().encode(pwd.getBytes());
                con.setRequestProperty("Proxy-Authorization", encodedPwd);
                con.connect();
                bis = new BufferedInputStream(con.getInputStream());
                byte[] buffer = new byte[1024];
                File ical = new File(saveIn, key + ".ics");
                bos = new BufferedOutputStream(new FileOutputStream(ical));
                int l = 0;
                while ((l = bis.read(buffer)) != -1) {
                    bos.write(buffer, 0, l);
                }
            } catch (Exception e) {
                log.log(Level.SEVERE, "Error in " + key, e);
            } finally {
                try {
                    bis.close();
                    bos.close();
                } catch (Exception e) {
                }
            }
            log.fine(key + " saved");
        }
    }
}
