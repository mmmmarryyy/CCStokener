package orcajo.azada.toolSchema.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import orcajo.azada.toolSchema.Activator;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.osgi.framework.Bundle;

class Discoverer {

    Discoverer() {
        super();
    }

    static List<String> discover() {
        return new Discoverer().discoverclass();
    }

    private List<String> discoverclass() {
        List<String> list = new ArrayList<String>();
        Bundle[] bundles = Activator.getDefault().getBundle().getBundleContext().getBundles();
        for (int i = 0; i < bundles.length; i++) {
            Bundle bundle = bundles[i];
            String key = "Fragment-Host";
            String host = (String) bundle.getHeaders().get(key);
            if (host != null) {
                String[] sHost = host.split(";");
                if (sHost != null && sHost[0] != null && sHost[0].equals("org.apache.commons.dbcp")) {
                    String cName = getClassname(bundle);
                    if (cName != null && !list.contains(cName)) {
                        list.add(cName);
                    }
                }
            }
        }
        return list;
    }

    private String getClassname(Bundle bundle) {
        URL urlEntry = bundle.getEntry("jdbcBundleInfo.xml");
        InputStream in = null;
        try {
            in = urlEntry.openStream();
            try {
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("<!DOCTYPE")) {
                        sb.append(line);
                    }
                }
                SAXBuilder builder = new SAXBuilder(false);
                Document doc = builder.build(new StringReader(sb.toString()));
                Element eRoot = doc.getRootElement();
                if ("jdbcBundleInfo".equals(eRoot.getName())) {
                    Attribute atri = eRoot.getAttribute("className");
                    if (atri != null) {
                        return atri.getValue();
                    }
                }
            } catch (JDOMException e) {
            }
        } catch (IOException e) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }
}
