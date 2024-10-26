package Gallery;

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Hashtable;

public class ConfigFile {

    private final int miMaxImageCount = 50;

    private int miImageCount;

    private String masImageFile[];

    private String masImageComment[];

    private Hashtable<String, String> mxCfgHash;

    private URL mUrlConfigFile;

    public ConfigFile(URL urlDocBase, String sConfigFile) {
        miImageCount = 0;
        masImageFile = new String[miMaxImageCount];
        masImageComment = new String[miMaxImageCount];
        mxCfgHash = new Hashtable<String, String>(20);
        if (sConfigFile == "" || sConfigFile == null) {
            sConfigFile = "../images/config.txt";
        }
        System.out.println("opening configuration file...");
        try {
            mUrlConfigFile = new URL(urlDocBase, sConfigFile);
            loadConfig(mUrlConfigFile);
        } catch (MalformedURLException e) {
            System.out.println("Invalid URL for config file: " + sConfigFile);
            System.out.println(e.getMessage());
            mUrlConfigFile = null;
        }
    }

    private void loadConfig(URL urlConfigFile) {
        try {
            InputStream is = urlConfigFile.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String sLine = br.readLine();
            while (sLine != null) {
                int iPos = sLine.indexOf('#');
                if (iPos != -1) {
                    sLine = sLine.substring(0, iPos);
                }
                sLine.trim();
                if (sLine.length() == 0) {
                    sLine = br.readLine();
                    continue;
                }
                String sProp;
                iPos = sLine.indexOf('=');
                if (iPos > 0 && (sProp = recognizeProperty(sLine.substring(0, iPos))) != null) {
                    addProperty(sProp, sLine.substring(iPos + 1, sLine.length()).trim());
                } else {
                    iPos = sLine.indexOf(';');
                    if (iPos == -1) {
                        addImage(sLine, null);
                    } else {
                        addImage(sLine.substring(0, iPos).trim(), sLine.substring(iPos + 1, sLine.length()).trim());
                    }
                }
                sLine = br.readLine();
            }
            br.close();
            isr.close();
            is.close();
        } catch (IOException e) {
            System.out.println("cannot read configuration from: " + mUrlConfigFile);
            System.out.println(e.getMessage());
        }
    }

    private String recognizeProperty(String sName) {
        sName = sName.trim();
        sName = sName.toUpperCase();
        if (sName.equals("SCROLLBAR") || sName.equals("NAVBAR") || sName.equals("TRANSITION") || sName.equals("HELP")) {
            return sName;
        }
        return null;
    }

    private boolean addProperty(String sName, String sValue) {
        sValue = sValue.trim();
        sValue = sValue.toLowerCase();
        System.out.println("Setting property: " + sName + " = " + sValue);
        mxCfgHash.put(sName, new String(sValue));
        return true;
    }

    public String getProperty(String sName) {
        String prop = mxCfgHash.get(sName);
        return prop == null ? "" : prop;
    }

    private void addImage(String sImageFile, String sImageComment) {
        if (miImageCount < miMaxImageCount) {
            masImageFile[miImageCount] = new String(sImageFile);
            masImageComment[miImageCount] = sImageComment == null ? "" : new String(sImageComment);
            miImageCount++;
        } else {
            System.out.println("maximal number of images loaded -> skip rest");
        }
    }

    public int getImageCount() {
        return miImageCount;
    }

    public int getMaxImageCount() {
        return miMaxImageCount;
    }

    public String getImageComment(int iIndex) {
        return masImageComment[iIndex];
    }

    public String getImageFile(int iIndex) {
        return masImageFile[iIndex];
    }

    public URL getConfigFile() {
        return mUrlConfigFile;
    }
}
