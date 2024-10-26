package com.skruk.elvis.applets;

import java.awt.HeadlessException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.swing.JApplet;
import netscape.javascript.JSObject;

/**
 * @author     skruk
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 * @created    19 lipiec 2004
 */
public class IsJavaApplet extends JApplet {

    /**
	 * @exception  HeadlessException        Description of the Exception
	 * @throws  java.awt.HeadlessException
	 */
    public IsJavaApplet() throws HeadlessException {
        super();
    }

    /**  Description of the Method */
    public void init() {
        super.init();
        try {
            URL url = new URL(this.getCodeBase() + "servlet/isJava?installed=true");
            HttpURLConnection huc = (java.net.HttpURLConnection) url.openConnection();
            huc.setRequestProperty("Cookie", "JSESSIONID=" + this.getParameter("jsessionid"));
            InputStream inS = huc.getInputStream();
            byte[] buffer = new byte[100];
            while (inS.read(buffer) >= 0) {
            }
            inS.close();
            JSObject jso = JSObject.getWindow(this);
            jso.eval("j2seExists('2');");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
