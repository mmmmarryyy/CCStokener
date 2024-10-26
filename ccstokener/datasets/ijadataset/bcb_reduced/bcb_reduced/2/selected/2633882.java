package uk.ac.ncl.neresc.dynasoar.deployer;

import org.apache.log4j.Logger;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import java.util.Properties;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import uk.ac.ncl.neresc.dynasoar.constants.DynasoarConstants;
import uk.ac.ncl.neresc.dynasoar.fault.DeploymentException;

/**
 * ***********************************************************************
 * Created On 18-Jul-2007 at 13:38:21
 * Created by nam48
 * <p/>
 * Copyright (C) 2007  Newcastle University, Newcastle Upon Tyne, UK
 * <p/>
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 * <p/>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 * ***********************************************************************
 */
public class TomcatWARDeployer extends ServiceDeployer {

    private static Logger mLog = Logger.getLogger(TomcatWARDeployer.class.getName());

    private String warname = null;

    private String ret = null;

    private String fault = null;

    private String containerAddress = null;

    private String warDesination = null;

    private long timeoutMillis = 30000;

    private String port = null;

    boolean overwriteWarFile = true;

    public TomcatWARDeployer() throws UnknownHostException {
        InputStream inpStream = null;
        Properties hpProp = new Properties();
        try {
            inpStream = this.getClass().getResourceAsStream("/HPConfig.properties");
            mLog.debug("Loading HostProvider properties...");
            if (inpStream != null) {
                hpProp.load(inpStream);
                port = new String(hpProp.getProperty("hostprovider.tomcat.port"));
                inpStream.close();
                mLog.debug("Setting container port to " + port);
            }
        } catch (IOException ix) {
            mLog.error("Error while reading HPConfig.properties file");
            mLog.debug("Setting default port " + DynasoarConstants.DEFAULT_PORT);
            port = DynasoarConstants.DEFAULT_PORT;
        }
        try {
            String hostName = InetAddress.getLocalHost().getCanonicalHostName();
            containerAddress = hostName + ":" + port;
        } catch (UnknownHostException e) {
            throw e;
        }
        String catalinaHome = System.getenv("CATALINA_HOME");
        if (catalinaHome != null) {
            warDesination = catalinaHome + "/webapps";
        }
    }

    public String installCode(String serviceName, String location, String vmName, String serviceLocationVM, String port) throws DeploymentException {
        return "dummy";
    }

    public String installCode(String serviceName, String location) throws DeploymentException {
        FileOutputStream out = null;
        mLog.debug("overwriteWarFile = " + overwriteWarFile);
        String fileData = null;
        String filepath = location;
        String[] splitString = filepath.split("/");
        String filename = splitString[splitString.length - 1];
        int fileNameLength = filename.length();
        warname = filename.substring(0, fileNameLength - 4);
        mLog.debug("WAR file name = " + warname);
        String filepath2 = warDesination + File.separator + filename;
        ret = "http://" + containerAddress + "/" + warname + "/services/" + serviceName;
        mLog.debug("filepath2 = " + filepath2);
        mLog.debug("ret = " + ret);
        mLog.debug("filepath = " + filepath);
        boolean warExists = new File(filepath2).exists();
        boolean webAppExists = true;
        try {
            String webAppName = filepath2.substring(0, (filepath2.length() - 4));
            mLog.debug("Web Application Name = " + webAppName);
            webAppExists = new File(webAppName).isDirectory();
            if (!webAppExists) {
                URL url = new URL(filepath);
                File targetFile = new File(filepath2);
                if (!targetFile.exists()) {
                    targetFile.createNewFile();
                }
                InputStream in = null;
                try {
                    in = url.openStream();
                    out = new FileOutputStream(targetFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new DeploymentException("couldn't open stream due to: " + e.getMessage());
                }
                URLConnection con = url.openConnection();
                int fileLength = con.getContentLength();
                ReadableByteChannel channelIn = Channels.newChannel(in);
                FileChannel channelOut = out.getChannel();
                channelOut.transferFrom(channelIn, 0, fileLength);
                channelIn.close();
                channelOut.close();
                out.flush();
                out.close();
                in.close();
                long time = System.currentTimeMillis();
                check(ret, time, STARTCONTROL);
            }
        } catch (Exception e) {
            webAppExists = false;
        }
        mLog.debug("webAppExists = " + webAppExists);
        return (ret);
    }
}
