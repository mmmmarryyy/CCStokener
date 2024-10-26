package org.mati.geotech.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.Scanner;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.mati.geotech.utils.config.Config;
import org.mati.geotech.utils.config.ConfigUser;

public class ObjectLoader extends Thread implements ConfigUser {

    private Stack<String> _cmd = new Stack<String>();

    private boolean _bUpdateIdList = false;

    private String _serverURL = Config.getInstance().getProperty("geoteck.object_server", "http://127.0.0.1/");

    private Vector<ObjectLoaderListener> _oli = new Vector<ObjectLoaderListener>();

    public void removeListner(ObjectLoaderListener oli) {
        _oli.remove(oli);
    }

    public void addListner(ObjectLoaderListener tci) {
        _oli.add(tci);
    }

    public void loadObject(int oid) {
        _cmd.push(String.valueOf(oid));
        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public void run() {
        boolean bExit = false;
        while (!bExit) {
            if (_bUpdateIdList) {
                _bUpdateIdList = false;
                URL url;
                try {
                    url = new URL(_serverURL + "get.php?cmd=list");
                    URLConnection con = url.openConnection();
                    con.connect();
                    Vector<Integer> ids = new Vector<Integer>();
                    Scanner s = new Scanner(con.getInputStream());
                    while (s.hasNextLine()) {
                        StringTokenizer st = new StringTokenizer(s.nextLine(), ",");
                        while (st.hasMoreTokens()) {
                            ids.add(Integer.parseInt(st.nextToken()));
                        }
                    }
                    for (ObjectLoaderListener ol : _oli) ol.recvObjectIdList(ids);
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
            while (!_cmd.isEmpty()) {
                URL url = null;
                try {
                    url = new URL(_serverURL + "get.php?obj=" + _cmd.pop());
                    URLConnection con = url.openConnection();
                    con.connect();
                    InputStream is = con.getInputStream();
                    GeoObject gobj = parseObject(is);
                    for (ObjectLoaderListener ol : _oli) ol.recvObject(gobj);
                } catch (Exception e) {
                    Logger.getRootLogger().error(e.getMessage() + "(" + url.toString() + ")");
                    System.err.println(e.getMessage());
                }
            }
            waitForTask();
        }
    }

    private GeoObject parseObject(InputStream is) throws UnsupportedEncodingException {
        GeoObject gobj = new GeoObject();
        Scanner s = new Scanner(is);
        while (s.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(s.nextLine(), ":");
            String key = new String(st.nextToken().trim().getBytes(), "UTF8");
            String val = new String(st.nextToken().trim().getBytes(), "UTF8");
            if (key.equalsIgnoreCase("line")) {
                Scanner s_pnt = new Scanner(val);
                s_pnt.useDelimiter(";");
                while (s_pnt.hasNext()) {
                    Scanner s_d = new Scanner(s_pnt.next());
                    s_d.useDelimiter(",");
                    gobj.getPoints().add(new GeoPoint(Double.parseDouble(s_d.next()), Double.parseDouble(s_d.next())));
                }
            } else gobj.getProps().put(key, val);
        }
        gobj.updateRect();
        return gobj;
    }

    private synchronized boolean waitForTask() {
        while (_cmd.isEmpty() && (!_bUpdateIdList)) {
            try {
                wait();
            } catch (InterruptedException e) {
                return true;
            }
        }
        return false;
    }

    public void getObjects() {
        _bUpdateIdList = true;
        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public void coufigChanged(Properties prop) {
        _serverURL = Config.getInstance().getProperty("geoteck.object_server", "http://127.0.0.1/");
    }
}
