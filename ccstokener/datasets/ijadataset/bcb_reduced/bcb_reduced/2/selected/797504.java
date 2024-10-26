package net.sourceforge.ecldbtool.connect;

import java.io.FileNotFoundException;
import java.net.URL;
import java.sql.Connection;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import net.sourceforge.ecldbtool.connect.view.DialogConnect;

public class ConnectionManager {

    private static ConnectionProfile connectionProfile;

    public static boolean isConnected() {
        return connectionProfile != null;
    }

    public static ConnectionProfile getConnectionProfile(Shell shell) {
        if (!isConnected()) {
            ConnectionConfiguration cc = getConnectionConfiguration();
            DialogConnect cd = new DialogConnect(shell, cc);
            cd.open();
            connectionProfile = cd.getConnectionProfile();
        }
        return connectionProfile;
    }

    private static ConnectionConfiguration connectionConfiguration;

    public static ConnectionConfiguration getConnectionConfiguration() {
        if (connectionConfiguration == null) {
            connectionConfiguration = new ConnectionConfiguration();
            DriverDescriptorPluginReader r2 = new DriverDescriptorPluginReader();
            r2.readDriverDescriptors(connectionConfiguration);
            ConnectionProfileXMLReader r = new ConnectionProfileXMLReader();
            try {
                URL url = new URL(Platform.getPlugin("net.sourceforge.ecldbtool.connect").getDescriptor().getInstallURL(), "ConnectionConfiguration.xml");
                r.setInputStream(url.openConnection().getInputStream());
            } catch (Throwable e) {
                System.out.println("Using hardcoded path");
                try {
                    r.setInputStream(new java.io.FileInputStream("C:/Program Files/oti/eclipse/plugins/net.sourceforge.ecldbtool.connect/ConnectionConfiguration.xml"));
                } catch (FileNotFoundException e2) {
                    e2.printStackTrace();
                }
            }
            r.readConnectionProfiles(connectionConfiguration);
        }
        return connectionConfiguration;
    }

    public static void disconnect() {
        connectionProfile = null;
    }

    public static void showerror(Shell shell, String message) {
        MessageDialog d = new MessageDialog(shell, "", null, message, MessageDialog.INFORMATION, new String[] { "OK" }, 0);
        d.open();
    }
}
