package unico.net;

import unico.*;
import unico.gui.*;
import jLop.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class NapManager extends TimerTask {

    public static String status;

    public static Networks networks;

    public static Timer timer = new Timer();

    public NapManager() {
        if (!(new File("servers.wsx")).isFile()) getOpennapServerList();
        JLopSuper.init();
        networks = JLopSuper.getNetworks();
        networks.loadFromWsxFile("servers.wsx");
        networks.connect();
        StatusBar.update();
        timer.schedule(this, 0, JLopSuper.getInterfaceRefreshMillis());
    }

    public void getOpennapServerList() {
        try {
            URL url = new URL("http://www.gotnap.com/servers.wsx");
            URLConnection connection = url.openConnection();
            InputStream stream = connection.getInputStream();
            BufferedInputStream in = new BufferedInputStream(stream);
            FileOutputStream file = new FileOutputStream("servers.wsx");
            BufferedOutputStream out = new BufferedOutputStream(file);
            int i;
            while ((i = in.read()) != -1) {
                out.write(i);
            }
            out.flush();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * jLop updater timertask 
	 */
    public void run() {
        status = Integer.toString(JLopSuper.getNetworks().getOpenServers());
        StatusBar.update();
        int sel = DownloadTable.getInstance().getSelectedRow();
        if (sel <= DownloadTableModel.getInstance().getRowCount()) {
            DownloadTableModel.getInstance().fireTableDataChanged();
            DownloadTable.getInstance().setRowSelectionInterval(sel, sel);
        }
        Iterator<unico.Search> i = Unico.searches.iterator();
        while (i.hasNext()) {
            unico.Search s = i.next();
            if (s.opennapSearch.isRunning() > 1 || s.opennapSearchJustStarted) {
                if (s.opennapSearch.count() > s.opennapResultCount) {
                    for (int x = s.opennapResultCount; x < s.opennapSearch.count(); x++) {
                        OpennapResult sr = new OpennapResult(s.opennapSearch.get(x));
                        s.addResult(sr);
                    }
                    s.opennapResultCount = s.opennapSearch.count();
                }
            }
        }
    }
}
