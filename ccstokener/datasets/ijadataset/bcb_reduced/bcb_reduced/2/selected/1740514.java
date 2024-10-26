package emailtray;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Hashtable;
import javax.swing.JOptionPane;
import javax.mail.Folder;
import javax.mail.Store;

/**
 *
 * @author luca 
 */
public class Funzioni {

    static boolean statocon = true;

    static String indmac = "";

    static String release = "0.1.5";

    static String version = "free";

    static String[] imapconf = new String[20];

    public static void ChkVersion(String version) {
        String url = "http://www.reteglobale.com/v" + version + ".php";
        String res = UrlReadPage(url);
        if (!res.trim().equals(release)) {
            String messaggio = "<html>Please update your EmailTray from release " + release + " to release " + res + " <br>" + "Update now?";
            int response = JOptionPane.showOptionDialog(null, messaggio, "New EmailTray update", 2, 1, null, null, null);
            System.out.println(response);
            if (response == 0) {
                try {
                    String[] osname = System.getProperty("os.name").toLowerCase().split(" ");
                    java.awt.Desktop.getDesktop().browse(new URI("https://sourceforge.net/projects/emailtray/files/EmailTray-" + osname[0].trim() + "-" + res + ".exe/download"));
                    emailtray.EmailTrayApp.getApplication().exit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected static Hashtable salvaConfigurazione(Hashtable hash) {
        String Sep = System.getProperty("file.separator");
        try {
            FileOutputStream fos = new FileOutputStream(appDataPath() + Sep + "EmailTray" + Sep + "datajm.asc");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(hash);
            oos.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error saving configuration");
        }
        return hash;
    }

    protected static Hashtable caricaConfigurazione() {
        Hashtable hash = null;
        String Sep = System.getProperty("file.separator");
        try {
            FileInputStream fis = new FileInputStream(appDataPath() + Sep + "EmailTray" + Sep + "datajm.asc");
            ObjectInputStream ois = new ObjectInputStream(fis);
            hash = (Hashtable) ois.readObject();
            ois.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Config error:" + appDataPath() + Sep + "EmailTray" + Sep + "datajm.asc");
        }
        return hash;
    }

    protected static Hashtable salvaMessaggi(Hashtable hash) {
        String Sep = System.getProperty("file.separator");
        try {
            FileOutputStream fos = new FileOutputStream(appDataPath() + Sep + "EmailTray" + Sep + "datams.dat");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(hash);
            oos.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error saving messages");
        }
        return hash;
    }

    protected static Hashtable caricaMessaggi() {
        Hashtable hash = null;
        String Sep = System.getProperty("file.separator");
        try {
            FileInputStream fis = new FileInputStream(appDataPath() + Sep + "EmailTray" + Sep + "datams.dat");
            ObjectInputStream ois = new ObjectInputStream(fis);
            hash = (Hashtable) ois.readObject();
            ois.close();
            if (hash.size() == 0) {
                hash.put("sub_0", "sorry empty mail folder, please standby for new emails");
                hash.put("sen_0", "no new messages");
                hash.put("last", "1");
            } else {
                hash.put("last", (hash.size() / 2));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Messages config error:" + appDataPath() + Sep + "EmailTray" + Sep + "datams.dat");
        }
        return hash;
    }

    public static String appDataPath() {
        return System.getenv("APPDATA");
    }

    public static void apriClient() {
        try {
            Hashtable daticonf = Funzioni.caricaConfigurazione();
            String[] indirizzo = new String[2];
            indirizzo = daticonf.get("u").toString().split("@");
            if (daticonf.get("s").toString().equals("0")) {
                if (indirizzo[1].equals("gmail.com")) {
                    java.awt.Desktop.getDesktop().browse(new URI("http://mail.google.com/"));
                } else {
                    java.awt.Desktop.getDesktop().browse(new URI("http://mail.google.com/a/" + indirizzo[1] + "/"));
                }
            }
            if (daticonf.get("s").toString().equals("1")) {
                java.awt.Desktop.getDesktop().browse(new URI("http://webmail1.webmail.aol.com"));
            }
            if (daticonf.get("s").toString().equals("2")) {
                java.awt.Desktop.getDesktop().browse(new URI("http://mail.yahoo.com"));
            }
        } catch (Exception er) {
            JOptionPane.showMessageDialog(null, "Error: " + er);
        }
    }

    public static boolean checkConnection() {
        try {
            URL url = new URL("http://www.google.com");
            URLConnection urlConnection = url.openConnection();
            urlConnection.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String MacAddress() {
        String indirizzo = "";
        try {
            InetAddress[] addresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
            for (int j = 0; j < addresses.length; j++) {
                if (addresses[j].toString().contains(":")) {
                    indirizzo = addresses[j].toString();
                }
            }
            System.out.println();
        } catch (Exception e) {
        }
        return indirizzo;
    }

    public static String UrlReadPage(String url) {
        String ret = "";
        try {
            URL ind = new URL(url);
            BufferedReader reader = new BufferedReader(new InputStreamReader(ind.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                ret = line;
            }
            reader.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static String[] listaFolder(Store store) {
        String[] lista = null;
        try {
            Folder rf = null;
            boolean recursive = true;
            ArrayList<String> listaval = new ArrayList<String>();
            rf = store.getDefaultFolder();
            EmailTrayUtilities.dumpFolder(rf, false, "", listaval);
            if ((rf.getType() & Folder.HOLDS_FOLDERS) != 0) {
                Folder[] f = rf.list("*");
                for (int i = 0; i < f.length; i++) EmailTrayUtilities.dumpFolder(f[i], recursive, "    ", listaval);
            }
            lista = new String[listaval.size()];
            for (int ss = 0; ss < listaval.size(); ss++) {
                lista[ss] = listaval.get(ss);
            }
        } catch (Exception ex) {
        }
        return lista;
    }
}
