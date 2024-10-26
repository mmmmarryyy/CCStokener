package iwork.icrafter.im;

import org.w3c.dom.*;
import java.io.*;
import java.net.*;
import java.util.*;
import iwork.icrafter.system.ICrafterException;
import iwork.icrafter.util.*;
import iwork.state.*;
import iwork.eheap2.*;
import org.xml.sax.*;

public class RemoteRepository {

    String repositoryLocation = null;

    long maxDownloadDelay = 24 * 60 * 60 * 1000;

    long lastDownloadTime = 0;

    String cacheFileName;

    EventHeap eheap;

    boolean available = false;

    String user;

    public RemoteRepository(String repLocation, String remoteGeneratorFileStr, EventHeap eh) {
        this("", repLocation, remoteGeneratorFileStr, eh);
    }

    public RemoteRepository(String userName, String repLocation, String remoteGeneratorFileStr, EventHeap eh) {
        repositoryLocation = repLocation;
        user = userName;
        if (!user.equals("")) cacheFileName = remoteGeneratorFileStr + "." + user; else cacheFileName = remoteGeneratorFileStr;
        File remoteGeneratorFile = new File(remoteGeneratorFileStr);
        lastDownloadTime = remoteGeneratorFile.lastModified();
        eheap = eh;
        try {
            HttpURLConnection connection = connect();
            if (connection != null) {
                connection.connect();
                System.err.println("Remote repository [" + repositoryLocation + "] available!");
                available = true;
                connection.disconnect();
            } else {
                System.err.println("Remote repository [" + repositoryLocation + "] not available");
            }
        } catch (Exception e) {
            System.err.println("Remote repository [" + repositoryLocation + "] not available");
        }
    }

    public synchronized Vector matchGenerators(ServiceSpec sspec, ApplianceSpec appl, UserSpec prefs) throws XMLException, ICrafterException, IOException, SAXException {
        if (available) update();
        File remoteGeneratorFile = new File(cacheFileName);
        NodeList gElems = parseGeneratorFile(remoteGeneratorFile);
        Matcher matcher = new Matcher(eheap);
        return matcher.selectGenerator(sspec, appl, prefs, gElems);
    }

    public synchronized boolean addGenerator(String[] gens) throws IOException {
        if (!available) return false;
        HttpURLConnection connection = connect();
        connection.setDoOutput(true);
        PrintWriter urlOut = new PrintWriter(connection.getOutputStream());
        String allGens = "<generators>";
        for (int i = 0; i < gens.length; i++) {
            allGens += gens[i];
        }
        allGens += "</generators>";
        String actionStr = URLEncoder.encode("AddGenerator");
        urlOut.println("action=" + actionStr + "&userName=" + user + "&generators=" + URLEncoder.encode(allGens));
        urlOut.close();
        int ret = connection.getResponseCode();
        if (ret / 100 == 2) if (update(true)) return true;
        return false;
    }

    public synchronized boolean clearGenerators() throws IOException {
        System.out.println("Clearing cache:" + cacheFileName);
        FileWriter fw = new FileWriter(cacheFileName);
        fw.write("<generators></generators>");
        fw.flush();
        fw.close();
        HttpURLConnection connection = connect();
        connection.setDoOutput(true);
        PrintWriter urlOut = new PrintWriter(connection.getOutputStream());
        urlOut.println("action=ClearGenerators&userName=" + URLEncoder.encode(user));
        urlOut.close();
        int ret = connection.getResponseCode();
        if (ret / 100 == 2) if (update(true)) return true;
        return false;
    }

    public boolean isServerAvailable() {
        return available;
    }

    private boolean update() throws IOException {
        return update(false);
    }

    private boolean update(boolean force) throws IOException {
        long currentTime = System.currentTimeMillis();
        if (!force) {
            if (lastDownloadTime + maxDownloadDelay >= currentTime) {
                return true;
            }
        }
        HttpURLConnection connection = connect();
        connection.setDoOutput(true);
        PrintWriter urlOut = new PrintWriter(connection.getOutputStream());
        String actionStr = URLEncoder.encode("GetGenList");
        urlOut.println("action=" + actionStr + "&testParam=test" + "&userName=" + user);
        urlOut.close();
        InputStream in = connection.getInputStream();
        byte[] genFileBytes = new byte[1000];
        File remoteGeneratorFile = new File(cacheFileName);
        if (remoteGeneratorFile.exists()) {
            Utils.debug("RemoteRepository", "remoteGeneratorFile" + "exists, deleting...");
            remoteGeneratorFile.delete();
        }
        FileOutputStream out = new FileOutputStream(remoteGeneratorFile);
        int bytesRead = in.read(genFileBytes);
        while (bytesRead != -1) {
            out.write(genFileBytes, 0, bytesRead);
            bytesRead = in.read(genFileBytes);
        }
        out.close();
        in.close();
        Utils.debug("RemoteRepository", "REMOTE gen file size: " + remoteGeneratorFile.length());
        lastDownloadTime = currentTime;
        return true;
    }

    private HttpURLConnection connect() throws MalformedURLException, IOException {
        HttpURLConnection connection = null;
        if (repositoryLocation == null) {
            Utils.debug("RemoteRepository", "repository Location unspecified");
            return null;
        }
        URL url = new URL(repositoryLocation);
        connection = (HttpURLConnection) url.openConnection();
        return connection;
    }

    private void debugConnection(URLConnection connection) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) Utils.debug("RemoteRepository", inputLine);
        in.close();
    }

    private NodeList parseGeneratorFile(File genFile) throws FileNotFoundException, IOException, SAXException {
        Element rootElem = XMLHelper.GetRootElement(genFile);
        if (rootElem == null) return null;
        NodeList gElems = XMLHelper.GetChildrenByTagName(rootElem, "generator");
        return gElems;
    }
}
