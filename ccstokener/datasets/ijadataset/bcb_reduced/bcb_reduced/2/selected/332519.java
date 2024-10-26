package org.dbe.kb.server.common;

import java.io.*;
import java.net.*;

/**
 * <p>The KB connector </p>
 *
 * <p>KB Connector takes over the communication between proxy and server</p>
 *
 * <p>TUC/MUSIC 2004</p>
 *
 * @version 1.0
 */
public abstract class KBconnector {

    protected String _saddr;

    protected boolean _MDRsupported;

    protected java.io.ByteArrayOutputStream _buffer;

    URLConnection _serverCon;

    public KBconnector() {
    }

    protected InputStream getConnection(int command, String value) {
        InputStream in = null;
        try {
            URL url = new URL(_saddr + "?" + command + "=" + value);
            _serverCon = url.openConnection();
            _serverCon.setDoInput(true);
            _serverCon.setUseCaches(false);
            _serverCon.setRequestProperty("Content_Type", "text/xml");
            in = _serverCon.getInputStream();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return in;
    }

    protected OutputStream postConnection(int command) {
        OutputStream out = null;
        try {
            URL url = new URL(_saddr);
            _serverCon = url.openConnection();
            _serverCon.setDoOutput(true);
            _serverCon.setDoInput(true);
            out = _serverCon.getOutputStream();
            PrintWriter pw = new PrintWriter(out);
            pw.print(command + "\n");
            pw.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return out;
    }

    protected InputStream getResponse() {
        InputStream in = null;
        try {
            in = _serverCon.getInputStream();
        } catch (Exception ex) {
        }
        return in;
    }

    protected void write(InputStream in, OutputStream out) throws java.io.IOException {
        int x;
        while ((x = in.read()) != -1) out.write(x);
        out.flush();
    }
}
