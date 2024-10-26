package tico.updater;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import tico.configuration.TSetup;

public class TUpdater {

    /**
	 * @param args
	 */
    public TUpdater() {
    }

    public String checkUpdate() {
        try {
            URL urlXML = new URL(TSetup.getUpdateURL());
            URLConnection conn = urlXML.openConnection();
            SAXBuilder builder = new SAXBuilder(false);
            Document doc = builder.build(conn.getInputStream());
            Element root = doc.getRootElement();
            double newVersion = Double.parseDouble(root.getChildText("version"));
            double oldVersion = Double.parseDouble(TSetup.getAppVersion());
            if (newVersion > oldVersion) {
                return root.getChildText("url");
            } else {
            }
        } catch (Exception e) {
            System.out.println("No internet connection");
        }
        return "";
    }
}
