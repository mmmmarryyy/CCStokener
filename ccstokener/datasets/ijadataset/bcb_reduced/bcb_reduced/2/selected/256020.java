package org.twyna.midget.util;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Enumeration;
import org.twyna.midget.Midget;

/**
 * This class provides some networking abstractions.
 * @author Tim Franssen
 * 
 ***** License
 * Twyna (Touch With Your Noodly Appendage) is an open source distribution
 * system to install and manage your machines.
 * Copyright (C) 2009  R. Hijmans & T.K.C. Franssen
 * 
 * This file is part of Twyna.
 * 
 * Twyna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Twyna is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Twyna. If not, see <http://www.gnu.org/licenses/>.
 */
public class Network {

    private final boolean verbose = false;

    /**
	 * TODO
	 * @return
	 */
    public String getIPAddress() {
        try {
            Enumeration<NetworkInterface> ni = NetworkInterface.getNetworkInterfaces();
            String ret = new String();
            while (ni.hasMoreElements()) {
                Enumeration<InetAddress> adr = ni.nextElement().getInetAddresses();
                while (adr.hasMoreElements()) {
                    InetAddress address = adr.nextElement();
                    if (!address.isLoopbackAddress() && !address.isAnyLocalAddress() && !address.isLinkLocalAddress()) {
                        ret = address.toString().substring(1);
                        if (verbose) Logger.log("Network", "IP address found: " + ret);
                    }
                }
            }
            return ret;
        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * Gets the last found MAC address (this may not be what we want
	 * but I have no better solution at the moment :))
	 * @return MAC address
	 */
    public String getMACAddress() {
        try {
            Enumeration<NetworkInterface> ni = NetworkInterface.getNetworkInterfaces();
            String ret = new String();
            while (ni.hasMoreElements()) {
                NetworkInterface intf = ni.nextElement();
                if (!intf.isLoopback() && !intf.isVirtual() && intf.isUp()) {
                    byte[] mac = intf.getHardwareAddress();
                    if (mac == null) continue;
                    ret = new String();
                    for (int i = 0; i < mac.length; i++) {
                        ret += String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : "");
                    }
                    if (verbose) Logger.log("Network", "MAC detected: " + ret);
                }
            }
            return ret;
        } catch (SocketException e) {
            Logger.log("Network", "Couldn't determine MAC address");
            return null;
        }
    }

    /**
	 * Does an HTTP POST request. Feed it XML data and it
	 * will be sent as the POST variable "xml". Will return
	 * the reply from the server.
	 * @param data XML data to send
	 * @return The reply from the server
	 */
    public String doPost(String data) {
        String request;
        try {
            request = URLEncoder.encode("xml", "UTF-8") + "=" + URLEncoder.encode(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        URL url;
        try {
            url = new URL(Midget.APPENDAGE + "/view/clientEndpoint.php");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        URLConnection conn;
        OutputStreamWriter wr;
        try {
            conn = url.openConnection();
            conn.setDoOutput(true);
            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(request);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            String result = new String();
            while ((line = rd.readLine()) != null) {
                result += line + System.getProperty("line.separator");
                if (verbose) Logger.log("Network", "Reading: " + line);
            }
            wr.close();
            rd.close();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * Implementation of platform independant "wget". Give it
	 * a URL and a location to store the file and it'll download
	 * it for you.
	 * @param url Location of file to download ("http://server/file...")
	 * @param location Local location to store file to
	 * @return Success
	 */
    public boolean wget(String url, String location) {
        URL urlo;
        try {
            urlo = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
        String path = getFileNameFromURL(urlo.getPath());
        if (location.substring(location.length() - 1).equals("/")) path = location + path; else path = location + "/" + path;
        URLConnection conn;
        try {
            conn = urlo.openConnection();
            InputStream in = conn.getInputStream();
            OutputStream out = new FileOutputStream(path);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
	 * TODO
	 * @param url
	 * @return
	 */
    public String getFileNameFromURL(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }
}
