package org.cofax.cms;

import java.io.*;
import java.net.*;

/**
* CofaxToolsClearCache:
* Makes http calls to Cofax Servlet Server to clear article cache or template cache. Expects the complete URl -
* compilation of that information is done within other classes. CofaxToolsClearCache is implemented as a thread
* so users will not have to wait upon completion of cache clearing if multiple servers or VAS exist.
* @author Charles Harvey
*
**/
public class CofaxToolsClearCache extends Thread {

    String URLToGet;

    /**
	* Makes http calls to Cofax Servlet Server to clear article cache or template cache. Expects the complete URl -
	* compilation of that information is done within other classes. CofaxToolsClearCache is implemented as a thread
	* so users will not have to wait upon completion of cache clearing if multiple servers or VAS exist.
	*
	**/
    CofaxToolsClearCache(String URLToGet) {
        this.URLToGet = URLToGet;
    }

    public void run() {
        URL url = null;
        HttpURLConnection connection = null;
        int responseCode = 0;
        try {
            CofaxToolsUtil.log("CofaxToolsClearCache run clearing Cache: " + URLToGet);
            url = new URL(URLToGet);
            connection = (HttpURLConnection) url.openConnection();
            responseCode = connection.getResponseCode();
        } catch (MalformedURLException ex) {
            CofaxToolsUtil.log("CofaxToolsClearCache run ERROR: Malformed URL: " + URLToGet);
        } catch (IOException ex) {
            CofaxToolsUtil.log("CofaxToolsClearCache run ERROR: IO Exception: " + URLToGet);
        }
        if (responseCode != 200) {
            CofaxToolsUtil.log("CofaxToolsClearCache run ERROR: " + responseCode + " " + URLToGet);
        }
    }
}
