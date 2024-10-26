package org.openmobster.core.moblet.deployment;

import java.util.List;
import java.util.ArrayList;
import java.net.URL;
import java.io.InputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.openmobster.core.common.ServiceManager;
import org.openmobster.core.common.XMLUtilities;
import org.openmobster.core.moblet.MobletApp;
import org.openmobster.core.moblet.registry.Registry;

/**
 * @author openmobster@gmail.com
 */
public class MobletDeployer {

    public static final String uri = "moblet-management://deployer";

    private Registry registry;

    public MobletDeployer() {
    }

    public static MobletDeployer getInstance() {
        return (MobletDeployer) ServiceManager.locate(uri);
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public void deploy(URL url) throws Throwable {
        InputStream is = url.openStream();
        List<MobletApp> apps = this.parseMobletApps(is);
        this.registry.register(apps);
    }

    private List<MobletApp> parseMobletApps(InputStream is) throws Exception {
        List<MobletApp> apps = new ArrayList<MobletApp>();
        Document root = XMLUtilities.parse(is);
        NodeList mobletAppNodes = root.getElementsByTagName("moblet-app");
        if (mobletAppNodes != null && mobletAppNodes.getLength() > 0) {
            int size = mobletAppNodes.getLength();
            for (int i = 0; i < size; i++) {
                Element mobletAppElem = (Element) mobletAppNodes.item(i);
                MobletApp app = this.parseMobletApp(mobletAppElem);
                apps.add(app);
            }
        }
        return apps;
    }

    private MobletApp parseMobletApp(Element mobletAppElem) throws Exception {
        MobletApp app = new MobletApp();
        Element name = (Element) mobletAppElem.getElementsByTagName("name").item(0);
        app.setName(name.getFirstChild().getTextContent());
        Element description = (Element) mobletAppElem.getElementsByTagName("description").item(0);
        app.setDescription(description.getFirstChild().getTextContent());
        Element binLocation = (Element) mobletAppElem.getElementsByTagName("bin-loc").item(0);
        String binaryLocation = binLocation.getFirstChild().getTextContent();
        if (!binaryLocation.startsWith("/")) {
            binaryLocation = "/" + binaryLocation;
        }
        app.setBinaryLocation(binaryLocation);
        NodeList configNodes = mobletAppElem.getElementsByTagName("config-loc");
        if (configNodes != null && configNodes.getLength() > 0) {
            Element configLocation = (Element) configNodes.item(0);
            String confLocation = configLocation.getFirstChild().getTextContent();
            if (!confLocation.startsWith("/")) {
                confLocation = "/" + confLocation;
            }
            app.setConfigLocation(confLocation);
        } else {
            app.setConfigLocation(binaryLocation);
        }
        String uri = app.getBinaryLocation();
        if (!uri.endsWith(".apk")) {
            int startIndex = uri.lastIndexOf('/');
            if (startIndex != -1) {
                uri = uri.substring(startIndex + 1);
            }
            int endIndex = -1;
            if ((endIndex = uri.indexOf('.')) != -1) {
                uri = uri.substring(0, endIndex);
            }
            app.setUri(uri);
        } else {
            app.setUri(binaryLocation);
        }
        return app;
    }
}
