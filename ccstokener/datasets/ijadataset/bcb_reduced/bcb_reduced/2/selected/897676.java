package com.sun.java.help.search;

import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;
import java.io.*;

/**
 *
 * @version	1.5	04/01/98
 * @author Jacek R. Ambroziak
 * @author Roger D. Brinkley
 * @author Eduardo Pelegri-Llopart
 */
class BtreeDictParameters extends BlockManagerParameters {

    private int id1;

    private String dirName;

    public BtreeDictParameters(URL fileName, int blockSize, int root, int freeID) {
        super(fileName, blockSize, root);
        id1 = freeID;
    }

    public BtreeDictParameters(Schema schema, String partName) throws Exception {
        super(schema, partName);
    }

    public boolean readState() {
        if (super.readState()) {
            setFreeID(integerParameter("id1"));
            return true;
        } else return false;
    }

    public void writeState() {
    }

    public int getFreeID() {
        return id1;
    }

    public final void setFreeID(int id) {
        id1 = id;
    }

    private void setDirName(String dirName) {
        this.dirName = dirName;
    }

    public static BtreeDictParameters create(URL dirName) {
        try {
            URL url = new URL(dirName, "TMAP");
            BtreeDictParameters bdp = null;
            bdp.setDirName(dirName.getFile());
            return bdp;
        } catch (java.net.MalformedURLException e) {
            System.out.println("Couldn't create " + dirName + File.separator + "TMAP");
        }
        return null;
    }

    public static BtreeDictParameters read(String dir, URL hsBase) throws Exception {
        URL baseURL = null, url, tmapURL = null;
        URLConnection connect;
        BufferedReader in;
        File file;
        int blockSize = -1;
        int rootPosition = -1;
        int freeID = -1;
        if (hsBase == null) {
            file = new File(dir);
            if (file.exists()) {
                if (File.separatorChar != '/') {
                    dir = dir.replace(File.separatorChar, '/');
                }
                if (dir.lastIndexOf(File.separatorChar) != dir.length() - 1) {
                    dir = dir.concat(File.separator);
                }
                debug("file:" + dir);
                baseURL = new URL("file", "", dir);
            } else {
                baseURL = new URL(dir);
            }
        }
        if (hsBase != null) {
            url = new URL(hsBase, dir + "/SCHEMA");
        } else {
            url = new URL(baseURL, "SCHEMA");
        }
        connect = url.openConnection();
        in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
        String line;
        do {
            line = in.readLine();
        } while (!line.startsWith("TMAP"));
        in.close();
        StringTokenizer tokens = new StringTokenizer(line, " =");
        tokens.nextToken();
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            if (token.equals("bs")) blockSize = Integer.parseInt(tokens.nextToken()); else if (token.equals("rt")) rootPosition = Integer.parseInt(tokens.nextToken()); else if (token.equals("id1")) freeID = Integer.parseInt(tokens.nextToken());
        }
        if (hsBase != null) {
            tmapURL = new URL(hsBase, dir + "/TMAP");
        } else {
            tmapURL = new URL(baseURL, "TMAP");
        }
        BtreeDictParameters bdp = null;
        if (hsBase == null) {
            bdp.setDirName(Utilities.URLDecoder(baseURL.getFile()));
        }
        return bdp;
    }

    public void updateSchema() {
        super.updateSchema("id1=" + id1 + " id2=1");
    }

    public void write() throws java.io.IOException {
        FileWriter out = new FileWriter(dirName + "/SCHEMA");
        out.write("JavaSearch 1.0\n");
        out.write("TMAP bs=2048 rt=" + root + " fl=-1 id1=" + id1 + " id2=1\n");
        out.close();
    }

    /**
   * For printf debugging.
   */
    private static boolean debugFlag = false;

    private static void debug(String str) {
        if (debugFlag) {
            System.out.println("BtreeDictParamters: " + str);
        }
    }
}
