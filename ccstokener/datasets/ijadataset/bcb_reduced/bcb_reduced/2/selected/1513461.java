package uk.ac.ncl.neresc.dynasoar.hostProvider.tomcatWarInstaller;

import uk.ac.ncl.neresc.dynasoar.Interfaces.ServiceProvider.ServiceInstaller;
import uk.ac.ncl.neresc.dynasoar.client.CodeStoreClient;
import uk.ac.ncl.neresc.dynasoar.client.codestore.codestore.CodeStorePortType;
import uk.ac.ncl.neresc.dynasoar.client.codestore.codestore.CodeStoreService;
import uk.ac.ncl.neresc.dynasoar.client.codestore.codestore.CodeStoreServiceLocator;
import uk.ac.ncl.neresc.dynasoar.client.codestore.faults.DynasoarExceptionType;
import uk.ac.ncl.neresc.dynasoar.config.hpConfig.HPConfigValues;
import uk.ac.ncl.neresc.dynasoar.config.hpConfig.HostProviderConfigParser;
import uk.ac.ncl.neresc.dynasoar.dataObjects.ServiceObject;
import uk.ac.ncl.neresc.dynasoar.exceptions.ConfigurationException;
import uk.ac.ncl.neresc.dynasoar.exceptions.UnableToDeployException;
import uk.ac.ncl.neresc.dynasoar.utils.InetUtils;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * Exactly the same WAR installer from Janus as implemented by John.
 */
public class TomcatWarInstaller implements ServiceInstaller {

    private String warname = null;

    private String ret = null;

    private String fault = null;

    private final int STARTCONTROL = 8000;

    private String containerAddress = null;

    private String warDesination = null;

    private HPConfigValues hpConfigValues = null;

    private long timeoutMillis = 30000;

    public TomcatWarInstaller() throws ConfigurationException {
        hpConfigValues = new HostProviderConfigParser().getValues();
        containerAddress = InetUtils.getMyHostName() + ":" + hpConfigValues.getContainer().getPort();
        warDesination = hpConfigValues.getTomcatWarConfig().getTomcatWarDest();
        timeoutMillis = hpConfigValues.getTomcatWarConfig().getTimeoutMillis();
    }

    /**
   * @param id
   * @param codeStoreLocation we assume it has been expanded in the webapps directory and is
   *                          running.
   * @return
   *
   * @throws DynasoarExceptionType
   */
    public String installCode(String id, String codeStoreLocation) throws DynasoarExceptionType {
        System.out.println("TomcatWarInstaller.installCode, id = " + id);
        try {
            FileOutputStream out = null;
            uk.ac.ncl.neresc.dynasoar.client.codestore.messages.ServiceCodeType serviceCodeType = null;
            boolean overwriteWarFile = hpConfigValues.getTomcatWarConfig().isOverwriteWars();
            System.out.println("TomcatWarInstaller.installCode, overwriteWarFile = " + overwriteWarFile);
            System.out.println("id = " + id);
            String fileData = null;
            try {
                CodeStoreClient codeStoreClient = new CodeStoreClient(codeStoreLocation);
                String location = codeStoreClient.getServiceCodeLocation(id);
                System.out.println("location = " + location);
                CodeStoreService css = new CodeStoreServiceLocator();
                CodeStorePortType cspt = css.getCodeStoreService(new URL(codeStoreLocation));
                System.out.println("TomcatWarInstaller.installCode, codeStoreLocation = " + new URL(codeStoreLocation));
                serviceCodeType = cspt.getServiceCode(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (serviceCodeType == null) {
                DynasoarExceptionType exception = new DynasoarExceptionType();
                exception.setDescription("no service for id " + id);
                throw exception;
            }
            fileData = serviceCodeType.getCodeStoreEndpoint().toString();
            if (fileData.equals("ERROR")) {
                DynasoarExceptionType exception = new DynasoarExceptionType();
                exception.setDescription("file not found");
                throw exception;
            }
            String name = id;
            String filepath = fileData;
            String[] splitString = filepath.split("/");
            String filename = splitString[splitString.length - 1];
            int fileNameLength = filename.length();
            warname = filename.substring(0, fileNameLength - 4);
            System.out.println("TomcatWarInstaller.installCode, warname = " + warname);
            String filepath2 = warDesination + File.separator + filename;
            ret = "http://" + containerAddress + "/" + warname + "/services/" + warname;
            System.out.println("TomcatWarInstaller.installCode, filepath2 = " + filepath2);
            System.out.println("TomcatWarInstaller.installCode, ret = " + ret);
            System.out.println("TomcatWarInstaller.installCode, filepath = " + filepath);
            boolean warExisits = new File(filepath2).exists();
            boolean webAppExists = true;
            try {
                String webAppName = filepath2.substring(0, (filepath2.length() - 4));
                System.out.println("TomcatWarInstaller.installCode, webAppName = " + webAppName);
                webAppExists = new File(webAppName).isDirectory();
            } catch (Exception e) {
                webAppExists = false;
            }
            System.out.println("TomcatWarInstaller.installCode, webAppExists = " + webAppExists);
            if (!webAppExists) {
                URL url = new URL(filepath);
                File targetFile = new File(filepath2);
                if (!targetFile.exists()) targetFile.createNewFile();
                InputStream in = null;
                try {
                    in = url.openStream();
                    out = new FileOutputStream(targetFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new DynasoarExceptionType(0, null, "couldn't open stream due to: " + e.getMessage());
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
            return (ret);
        } catch (IOException e) {
            e.printStackTrace();
            throw new DynasoarExceptionType(0, null, e.getMessage());
        }
    }

    private void check(String endpointURL, long startTime, int controlValue) throws DynasoarExceptionType {
        System.out.println("TomcatWarInstaller.check, endpointURL = " + endpointURL);
        try {
            long newTime = System.currentTimeMillis();
            URL url = new URL(endpointURL);
            url.openStream();
            System.out.println("FINISH " + (newTime - startTime) / 1000);
        } catch (IOException e) {
            System.out.println(e.toString());
            long time = System.currentTimeMillis();
            System.out.println((time - startTime) / 1000);
            do {
                time = System.currentTimeMillis();
            } while (time < startTime + controlValue);
            if (controlValue < timeoutMillis) {
                check(endpointURL, startTime, (controlValue + 2000));
            } else {
                throw new DynasoarExceptionType();
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            throw new DynasoarExceptionType();
        }
    }

    private String readProfile() throws DynasoarExceptionType {
        try {
            String usrHome = System.getProperty("user.home");
            String profile = usrHome + "/.profile";
            BufferedReader in = new BufferedReader(new FileReader(profile));
            String inString;
            while ((inString = in.readLine()) != null) {
                String[] splitString = inString.split("=");
                if (!splitString[0].equals(null) && !inString.startsWith("#")) {
                    if (splitString[0].endsWith("CATALINA_HOME")) {
                        return splitString[1];
                    }
                }
            }
            return "ERROR";
        } catch (Exception e) {
            throw new DynasoarExceptionType();
        }
    }

    public String installCode(ServiceObject service) throws UnableToDeployException {
        try {
            return installCode(service.getId(), service.getCodeStoreLocation());
        } catch (DynasoarExceptionType dynoExceptionType) {
            dynoExceptionType.printStackTrace();
            throw new UnableToDeployException(dynoExceptionType.getDescription());
        }
    }
}
