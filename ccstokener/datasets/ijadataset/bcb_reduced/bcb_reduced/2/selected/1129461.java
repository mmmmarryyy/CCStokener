package jp.co.withone.osgi.gadget.controller;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import jp.co.withone.osgi.gadget.controller.preference.Preference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * @author t.funakky
 */
public class GadgetControlPreference implements Preference, ActionListener {

    private static final String LIST_LOCATION = "http://gadget-on-osgi.googlecode.com/svn/source/2/jp.co.withone.osgi.gadget.controller/trunk/";

    private static final String BUNDLE_LIST_FILE = "bundlelist";

    private static final String BUNDLE_NAME_LIST_FILE = "bundlenamelist";

    public static final String PREFERENCE_CONTROLLER_NAME = "preference.controller.name";

    private static final String PREFERENCE_CONTROLLER_INSTALL = "preference.controller.install";

    private static final BundleInfo[] BUNDLE_INFO_EMPTY_ARRAY = new BundleInfo[0];

    private final BundleContext context;

    private volatile BundleInfo[] infos;

    public GadgetControlPreference(BundleContext context) {
        this.context = context;
    }

    @Override
    public JPanel getPage() {
        try {
            JPanel panel = new JPanel();
            GridBagLayout layout = new GridBagLayout();
            panel.setLayout(layout);
            GridBagConstraints gbc = new GridBagConstraints();
            this.infos = getBundleInfoArray(LIST_LOCATION);
            int i = 0;
            for (BundleInfo info : this.infos) {
                JCheckBox check = new JCheckBox(info.bundleName);
                gbc.gridx = 0;
                gbc.gridy = i++;
                gbc.weightx = 1.0;
                gbc.anchor = GridBagConstraints.WEST;
                layout.setConstraints(check, gbc);
                panel.add(check);
                info.check = check;
                Bundle[] bundles = context.getBundles();
                for (Bundle bundle : bundles) {
                    if (bundle.getSymbolicName().equals(info.bundleSymbolicName)) {
                        info.check.setEnabled(false);
                        break;
                    }
                }
            }
            JButton button = new JButton("install");
            button.addActionListener(this);
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.gridwidth = 2;
            gbc.weightx = 0.0d;
            gbc.weighty = 1.0d;
            gbc.anchor = GridBagConstraints.CENTER;
            layout.setConstraints(button, gbc);
            panel.add(button);
            return panel;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static BundleInfo[] getBundleInfoArray(String location) throws IOException {
        URL url = new URL(location + BUNDLE_LIST_FILE);
        BufferedReader br = null;
        List<BundleInfo> list = new ArrayList<BundleInfo>();
        try {
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                int pos1 = line.indexOf('=');
                if (pos1 < 0) {
                    continue;
                }
                BundleInfo info = new BundleInfo();
                info.bundleSymbolicName = line.substring(0, pos1);
                info.location = line.substring(pos1 + 1);
                list.add(info);
            }
            if (!setBundleInfoName(location + BUNDLE_NAME_LIST_FILE + "_" + Locale.getDefault().getLanguage(), list)) {
                setBundleInfoName(location + BUNDLE_NAME_LIST_FILE, list);
            }
            return list.toArray(BUNDLE_INFO_EMPTY_ARRAY);
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    private static boolean setBundleInfoName(String location, List<BundleInfo> list) {
        try {
            URL url = new URL(location);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                int pos1 = line.indexOf('=');
                if (pos1 < 0) {
                    continue;
                }
                String bundleSymbolicName = line.substring(0, pos1);
                String bundleName = line.substring(pos1 + 1);
                for (BundleInfo info : list) {
                    if (info.bundleSymbolicName.equals(bundleSymbolicName)) {
                        info.bundleName = bundleName;
                        break;
                    }
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static class BundleInfo {

        private String bundleSymbolicName;

        private String bundleName;

        private String location;

        private JCheckBox check;

        public String toString() {
            return this.bundleName;
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        for (BundleInfo info : this.infos) {
            if (info.check.isEnabled() && info.check.isSelected()) {
                try {
                    Bundle bundle = context.installBundle(info.location);
                    info.check.setEnabled(false);
                    bundle.start();
                } catch (BundleException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void canceld() {
    }

    @Override
    public void confirmed() {
    }
}
