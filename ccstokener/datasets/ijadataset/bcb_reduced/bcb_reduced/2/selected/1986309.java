package net.sourceforge.pyrus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import net.sourceforge.pyrus.hal.Discovery;
import net.sourceforge.pyrus.hal.MObject;

/**
 * @author ivan
 *
 */
class DiscoveryImpl implements Discovery {

    private List<MObjectDesc> mobjectDescs = new ArrayList<MObjectDesc>();

    private List<String> themes = new ArrayList<String>();

    private int nextAutoId;

    @SuppressWarnings("unchecked")
    public DiscoveryImpl() throws IOException, ClassNotFoundException {
        Enumeration<URL> urls = getClass().getClassLoader().getResources("META-INF/pyrus/discovery.inf");
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts[0].equals("mobject")) {
                    if (parts[1].equals("")) {
                        parts[1] = parts[2];
                    }
                    String ifaceName, subId;
                    String[] subparts1 = parts[1].split("-");
                    if (subparts1.length == 2) {
                        ifaceName = subparts1[0];
                        subId = subparts1[1];
                        if (subId.equals("auto")) {
                            subId = Integer.toString(nextAutoId++);
                        }
                    } else {
                        ifaceName = parts[1];
                        subId = null;
                    }
                    mobjectDescs.add(new MObjectDesc((Class<? extends MObject>) Class.forName(ifaceName), subId, (Class<MObject>) Class.forName(parts[2])));
                } else if (parts[0].equals("theme")) {
                    themes.add(parts[1]);
                }
            }
        }
        mobjectDescs = Collections.unmodifiableList(mobjectDescs);
        themes = Collections.unmodifiableList(themes);
    }

    public List<MObjectDesc> getMObjectDescs() {
        return mobjectDescs;
    }

    public List<String> getThemes() {
        return themes;
    }
}
