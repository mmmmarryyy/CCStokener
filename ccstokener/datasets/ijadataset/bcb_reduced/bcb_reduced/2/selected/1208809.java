package bgpanalyzer.functions.downloadFile;

import bgpanalyzer.functions.configuration.Settings;
import bgpanalyzer.util.Patterns;
import bgpanalyzer.resources.Constants;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.SocketAddress;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;

public class ObtainServerFiles extends Thread {

    private Settings set = new Settings();

    private ObtainServerFilesView obtainServerFilesView = null;

    private DownloadFileController downloadFileController = null;

    private byte option;

    private LinkedHashMap<String, ArrayList<String>> index = new LinkedHashMap<String, ArrayList<String>>();

    private String URL_ROUTE_VIEWS = set.getUrl();

    /** Creates a new instance of ObtainServerFiles */
    public ObtainServerFiles(ObtainServerFilesView obtainServerFilesView, byte option) {
        this.obtainServerFilesView = obtainServerFilesView;
        this.downloadFileController = downloadFileController;
        this.option = option;
        start();
    }

    public void run() {
        ArrayList<String> data = null;
        switch(option) {
            case ObtainServerFilesView.YEARS:
                data = getYearsAndMonths();
                for (String item : data) {
                    String year = item.substring(0, item.indexOf("."));
                    String month = item.substring(item.indexOf(".") + 1);
                    if (!index.containsKey(year)) {
                        index.put(year, new ArrayList<String>());
                    }
                    ArrayList<String> months = index.get(year);
                    months.add(month);
                }
                obtainServerFilesView.setIndex(index);
                break;
            case ObtainServerFilesView.FILES:
                data = getFiles(obtainServerFilesView.getDate());
                obtainServerFilesView.setFiles(data);
                break;
        }
    }

    private ArrayList<String> getFiles(String date) {
        ArrayList<String> files = new ArrayList<String>();
        String info = "";
        try {
            obtainServerFilesView.setLblProcessText(java.util.ResourceBundle.getBundle("bgpanalyzer/resources/Bundle").getString("ObtainServerFilesView.Label.Progress.Obtaining_Data"));
            URL url = new URL(URL_ROUTE_VIEWS + date + "/");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(false);
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                if (!line.equals("")) info += line + "%";
            }
            obtainServerFilesView.setLblProcessText(java.util.ResourceBundle.getBundle("bgpanalyzer/resources/Bundle").getString("ObtainServerFilesView.Label.Progress.Processing_Data"));
            info = Patterns.removeTags(info);
            StringTokenizer st = new StringTokenizer(info, "%");
            info = "";
            boolean alternador = false;
            int index = 1;
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (!token.trim().equals("")) {
                    int pos = token.indexOf(".bz2");
                    if (pos != -1) {
                        token = token.substring(1, pos + 4);
                        files.add(token);
                    }
                }
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }

    private ArrayList<String> getYearsAndMonths() {
        String info = "";
        ArrayList<String> items = new ArrayList<String>();
        try {
            obtainServerFilesView.setLblProcessText(java.util.ResourceBundle.getBundle("bgpanalyzer/resources/Bundle").getString("ObtainServerFilesView.Label.Progress.Obtaining_Data"));
            URL url = new URL(URL_ROUTE_VIEWS);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(false);
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                if (!line.equals("")) info += line + "%";
            }
            obtainServerFilesView.setLblProcessText(java.util.ResourceBundle.getBundle("bgpanalyzer/resources/Bundle").getString("ObtainServerFilesView.Label.Progress.Processing_Data"));
            info = Patterns.removeTags(info);
            StringTokenizer st = new StringTokenizer(info, "%");
            info = "";
            boolean alternador = false;
            int index = 1;
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (!token.trim().equals("")) {
                    int pos = token.indexOf("/");
                    if (pos != -1) {
                        token = token.substring(1, pos);
                        if (Patterns.hasFormatYYYYdotMM(token)) {
                            items.add(token);
                        }
                    }
                }
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }
}
