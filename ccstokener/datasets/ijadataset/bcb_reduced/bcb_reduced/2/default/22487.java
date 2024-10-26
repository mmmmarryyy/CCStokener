import java.io.*;
import java.lang.*;
import java.util.*;
import java.net.*;
import javax.swing.*;

class CConfigFile {

    public CConfigFile(JPanel pan, URL documentBase) {
        try {
            panel = pan;
            URL in_url = new URL(documentBase, "tobedone.conf");
            InputStream ins = in_url.openStream();
            InputStreamReader insr = new InputStreamReader(ins);
            BufferedReader in = new BufferedReader(insr);
            readData(in);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(panel, "Couldn't load " + "config file :\n" + e);
        }
    }

    private void readData(BufferedReader in) {
        try {
            String data;
            while ((data = in.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(data);
                if (st.hasMoreTokens()) {
                    String command = st.nextToken();
                    if (command.equals("host")) {
                        if (st.hasMoreTokens()) {
                            conf_host = st.nextToken();
                        } else {
                            JOptionPane.showMessageDialog(panel, "Config file has a bad " + "'host' entry!");
                        }
                    } else if (command.equals("configdb")) {
                        if (st.hasMoreTokens()) {
                            conf_configdb = st.nextToken();
                        } else {
                            JOptionPane.showMessageDialog(panel, "Config file has a bad " + "'configdb' entry!");
                        }
                    }
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(panel, "Couldn't load " + "config file :\n" + e);
        }
    }

    private JPanel panel;

    public String conf_host = "";

    public String conf_configdb;
}
