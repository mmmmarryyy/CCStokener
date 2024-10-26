package dshub.plugin;

import dshub.Vars;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.Proxy;

/**
 *
 * @author Pietricica
 */
public class HubtrackerConnection extends Thread {

    String user, pass, e_mail;

    HubtrackerCmd curCmd;

    private boolean done = false;

    /** Creates a new instance of HubtrackerConnection */
    public HubtrackerConnection(HubtrackerCmd curCmd, String user, String pass, String e_mail) {
        this.user = user;
        this.e_mail = e_mail;
        this.curCmd = curCmd;
        this.pass = pass;
        start();
    }

    public void run() {
        BufferedReader inp = null;
        try {
            String urlString = "http://www.hubtracker.com/query.php?action=add&username=" + user + "&password=" + pass + "&email=" + e_mail + "&address=" + Vars.Hub_Host;
            URL url = new URL(urlString);
            URLConnection conn;
            if (!Vars.Proxy_Host.equals("")) conn = url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Vars.Proxy_Host, Vars.Proxy_Port))); else conn = url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.connect();
            inp = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String xx;
            while ((xx = inp.readLine()) != null) PluginMain.result += "\n" + xx;
            if (curCmd != null) this.curCmd.cur_client.sendFromBot("[hubtracker:] " + PluginMain.result); else PluginMain.curFrame.showMsg();
            inp.close();
            inp = null;
        } catch (MalformedURLException ue) {
            PluginMain.result = ue.toString();
        } catch (Exception e) {
            PluginMain.result = e.toString();
        }
        done = true;
    }

    public boolean isDone() {
        try {
            this.sleep(100);
        } catch (InterruptedException ex) {
        }
        return done;
    }
}
