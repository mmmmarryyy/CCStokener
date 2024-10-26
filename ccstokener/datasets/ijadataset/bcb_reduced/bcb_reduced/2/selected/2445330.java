package uk.ac.ncl.neresc.dynasoar.hostprovider;

import org.apache.log4j.Logger;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import java.util.Properties;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import uk.ac.ncl.neresc.dynasoar.constants.DynasoarConstants;
import uk.ac.ncl.neresc.dynasoar.fault.DeploymentException;
import com.ibm.wsdl.xml.WSDLReaderImpl;
import javax.wsdl.WSDLException;
import javax.wsdl.Definition;

/**
 * Created by IntelliJ IDEA.
 * User: nam48
 * Date: 22-Mar-2006
 * Time: 14:08:57
 * To change this template use File | Settings | File Templates.
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
