package eu.more.core.internal.proxy;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import javax.xml.namespace.QName;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.soda.dpws.DPWSException;
import org.soda.dpws.DeviceProxy;
import org.soda.dpws.ServiceProxy;
import org.soda.dpws.cache.CachedDevice;
import org.soda.dpws.cache.CachedService;
import org.soda.dpws.metadata.DeviceMetadata;
import org.soda.dpws.metadata.MetadataSection;
import org.soda.dpws.metadata.ServiceMetadata;
import eu.more.core.internal.msoa.TypeConversation;

/**
 * Class ProxyUtils contains Util-Methods needed for realizing a automatic proxy
 * server. No constructor is being needed, Class just concentrates the message
 * being used for dynamic generator invokation.
 * 
 * @author georg
 * 
 */
public final class ProxyUtils {

    private static ArrayList<String> instances = new ArrayList<String>();

    public static boolean isnewProxyInstance(String name) {
        if (instances.contains(name)) return false; else {
            instances.add(name);
            return true;
        }
    }

    private static Timer timer = new Timer();

    private static HashMap<String, TimerTask> timermap = new HashMap<String, TimerTask>();

    public static void addTimer(String messageId, TimerTask tt) {
        timermap.put(messageId, tt);
    }

    public static void removeProxyInstance(String messageId) {
        removeEntryMessageId(messageId);
        TimerTask tt = timermap.get(messageId);
        if (tt != null) {
            ((KeepAliveThread) tt).removeBundle();
            tt.cancel();
            timer.purge();
        }
    }

    public static String workingdir = null;

    public static String libdir = null;

    public static String jdkdir = null;

    public static void initialize() {
        Properties properties = readPropertiesFile("Proxy.properties");
        workingdir = properties.getProperty("workingdir");
        libdir = properties.getProperty("libdir");
        jdkdir = properties.getProperty("jdkdir");
    }

    /**
	 * Reads a properties file from eu.more.core.configuration-directory and
	 * returns the PropertiesObject
	 *
	 * @param filename
	 *            Name of the properties file
	 * @return readed {@link Properties}
	 */
    public static Properties readPropertiesFile(String filename) {
        Properties properties = null;
        try {
            String rootDir = System.getProperty("eu.more.core.configuration");
            if (rootDir == null) rootDir = "./configuration/";
            String fileSep = System.getProperty("file.separator");
            String resourceDir = rootDir + ((rootDir.endsWith(fileSep)) ? "" : fileSep);
            properties = new Properties();
            FileInputStream props = new FileInputStream(resourceDir + filename);
            properties.load(props);
            props.close();
            props = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties;
    }

    /**
	 * Messagelist for checking if there is yet another proxy instance to this
	 * device
	 */
    private static ArrayList<String> messageList = new ArrayList<String>();

    /**
	 * Method to check the specified messageId
	 * 
	 * @param messageId
	 * @return
	 */
    public static boolean isDuplicate(String messageId) {
        if (messageList.contains(messageId)) return true; else {
            messageList.add(messageId);
            return false;
        }
    }

    private static void removeEntryMessageId(String messageId) {
        ArrayList<String> list = messageList;
        list.remove(messageId);
    }

    /**
	 * Cleaning up Working Dir ... removing content besides javagenerator.jar
	 */
    public static void cleanupWorkingDir() {
        deleteTree(new File(workingdir));
    }

    /**
	 * Deleting Path Tree
	 * 
	 * @param path
	 */
    private static void deleteTree(File path) {
        for (File file : path.listFiles()) {
            if (file.isDirectory() && (!(file.getName().equals("libs")))) deleteTree(file); else if (!(file.getName().equals("JavaGenerator.jar"))) file.delete();
        }
        path.delete();
    }

    /**
	 * Discovers WSDL Location URL of a given DeviceProxy
	 * 
	 * @param device
	 * @return WSDL Location as an URL String
	 * @throws Exception
	 */
    public static String getwsdllocation(DeviceProxy device) throws Exception {
        String wsdlurl = "";
        Collection services = device.getHostedServices();
        if (services.size() == 1) {
            ServiceProxy service = (ServiceProxy) services.iterator().next();
            ServiceMetadata sMeta = service.getServiceMetadata();
            List wsdls = sMeta.getWsdls();
            if (wsdls.size() == 1) {
                MetadataSection section = (MetadataSection) wsdls.get(0);
                wsdlurl = section.getLocation();
            } else {
                MetadataSection section = (MetadataSection) wsdls.get(wsdls.size() - 1);
                wsdlurl = section.getLocation();
            }
        }
        return wsdlurl;
    }

    /**
	 * Used to write an WSDL File to a local file.
	 * 
	 * @param b
	 *            Input of WSDL File as byte array
	 * @param localFilename
	 *            Filename to store WSDL File at
	 */
    public static void writeWSDLtoFile(byte[] b, String localFileName) {
        createDir("WSDL");
        File file = new File(workingdir + "/WSDL/" + localFileName);
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file));
            out.write(b);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    private static void createDir(String Name) {
        File path = new File(workingdir + "//" + Name);
        if (!(path.exists())) {
            path.mkdir();
        }
    }

    /**
	 * Method to downloads WSDL File to local File Name. Could also be used for
	 * downloading other files.
	 * 
	 * @param address
	 *            Adress of the WSDL File
	 * @param localFileName
	 *            Local File Name to save the WSDL File
	 */
    public static void downloadwsdl(String address, String localFileName) {
        File file = new File(workingdir + "/" + localFileName);
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(address);
            out = new BufferedOutputStream(new FileOutputStream(file));
            conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
            System.out.println(localFileName + "\t" + numWritten);
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    /**
	 * Searches Deviceproxy for Service ID
	 * 
	 * @param devproxy
	 * @return Service ID as a String
	 * @throws DPWSException
	 */
    public static String getServiceIDfromDevice(DeviceProxy devproxy) throws DPWSException {
        System.out.println("\nHosted services information:");
        DeviceMetadata devmeta = devproxy.getDeviceMetadata();
        Iterator it = devproxy.getHostedServices().iterator();
        while (it.hasNext()) {
            ServiceProxy hosted = (ServiceProxy) it.next();
            String id = hosted.getId();
            id = id.substring(id.lastIndexOf("/") + 1, id.length());
            return id;
        }
        return null;
    }

    /**
	 * Encapsulated method to set the Working Directory
	 * 
	 * @param workingdir
	 */
    public static void setWorkingdir(String tworkingdir) {
        workingdir = tworkingdir;
    }

    /**
	 * Encapsulated method to get the current Working Directory
	 * 
	 * @return
	 */
    public static String getWorkingdir() {
        return workingdir;
    }

    public static void startGeneratorThread(String serviceName, String WSDLFile, String endpoint, boolean MSOAenabled, String messageId) {
        Thread generationThread = new Thread(new ProxyUtils.callBundleGeneratorThread(serviceName, WSDLFile, endpoint, MSOAenabled, messageId));
    }

    @SuppressWarnings("unchecked")
    public static void startGeneratorThread(CachedDevice cd) {
        try {
            String host = cd.getDefaultTransportAddress().substring(cd.getDefaultTransportAddress().indexOf("//") + 2, cd.getDefaultTransportAddress().lastIndexOf("/"));
            String port = host.substring(host.indexOf(":") + 1, host.length());
            String ipadresse = host.substring(0, host.indexOf(":"));
            String serviceName = null;
            ServiceProxy csp = null;
            Iterator it = cd.getHostedServices().iterator();
            while (it.hasNext()) {
                ServiceProxy sp = (CachedService) it.next();
                Iterator nit = sp.getTypes().iterator();
                while (nit.hasNext()) {
                    QName q = (QName) nit.next();
                    System.out.println("Found Service " + q.getLocalPart().toLowerCase() + "service");
                    if (serviceName == null) {
                        serviceName = q.getLocalPart().toLowerCase() + "service";
                        csp = sp;
                    } else {
                        throw new DPWSException("Found more then one Service on device ... don't know which one to handle!");
                    }
                }
            }
            if (isDuplicate(cd.getDefaultTransportAddress())) {
                return;
            }
            if (!(ProxyUtils.isnewProxyInstance(serviceName))) {
                int counter = 2;
                boolean newname = ProxyUtils.isnewProxyInstance(serviceName + counter);
                while (!(newname)) {
                    counter++;
                    newname = ProxyUtils.isnewProxyInstance(serviceName + counter);
                }
                serviceName = serviceName + counter;
            }
            boolean MSOAenabled = false;
            if (cd.getScopes().contains("http://www.ist-more.org/MSOADevice")) MSOAenabled = true;
            String WSDLFileName = serviceName + ".wsdl";
            URL url = new URL(csp.getDefaultEndpoint().getAddress() + "/getwsdl");
            URLConnection conn = url.openConnection();
            DataInputStream dis = new DataInputStream(conn.getInputStream());
            String inputLine;
            StringBuffer sb = new StringBuffer();
            while ((inputLine = dis.readLine()) != null) {
                sb.append(inputLine);
            }
            dis.close();
            byte[] b = TypeConversation.toByta(sb.toString());
            ProxyUtils.cleanupWorkingDir();
            ProxyUtils.writeWSDLtoFile(b, WSDLFileName);
            @SuppressWarnings("unused") Thread generationThread = new Thread(new ProxyUtils.callBundleGeneratorThread(serviceName, ProxyUtils.workingdir + "//WSDL//" + WSDLFileName, csp.getDefaultEndpoint().getAddress(), MSOAenabled, csp.getDefaultEndpoint().getAddress()));
        } catch (DPWSException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class callBundleGeneratorThread implements Runnable {

        Thread runner;

        public callBundleGeneratorThread(final String serviceName, final String WSDLFile, final String endpoint, final boolean MSOAenabled, final String messageId) {
            this.serviceName = serviceName;
            this.WSDLFile = WSDLFile;
            this.endpoint = endpoint;
            this.MSOAenabled = MSOAenabled;
            this.messageId = messageId;
            runner = new Thread(this);
            runner.start();
            while (runner.isAlive()) {
            }
            try {
                File f = new File(ProxyUtils.workingdir + "//" + serviceName + ".jar");
                if (!(f.exists())) {
                    System.out.println("GENERATOR FAILED .... ");
                    ProxyUtils.removeEntryMessageId(messageId);
                    ProxyUtils.removeProxyInstance(messageId);
                    return;
                }
                InputStream in = new FileInputStream(f);
                Bundle bundle = eu.more.core.internal.CoreActivator.justforinstall.installBundle(f.getAbsolutePath(), in);
                if (bundle != null) {
                    in.close();
                    bundle.start();
                }
            } catch (BundleException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String serviceName = null;

        private String WSDLFile = null;

        private String endpoint = null;

        private boolean MSOAenabled = false;

        private String messageId = null;

        private boolean debug = false;

        public void run() {
            if ((serviceName != null) && (WSDLFile != null) && (endpoint != null)) {
                StringBuffer buffer = new StringBuffer();
                List<String> liste = new ArrayList<String>();
                liste.add(jdkdir + "//java");
                liste.add("-jar");
                liste.add(libdir + "//BundleGenerator.jar");
                liste.add(workingdir);
                liste.add(serviceName);
                liste.add(WSDLFile);
                liste.add(endpoint);
                liste.add(String.valueOf(MSOAenabled));
                ProcessBuilder pb = new ProcessBuilder(liste);
                try {
                    Process p = pb.start();
                    Scanner s = new Scanner(p.getInputStream()).useDelimiter("\\Z");
                    if (debug) {
                        System.out.println(s.next());
                    } else {
                        buffer.append(s.next());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Wrong Execution!");
            }
        }
    }
}
