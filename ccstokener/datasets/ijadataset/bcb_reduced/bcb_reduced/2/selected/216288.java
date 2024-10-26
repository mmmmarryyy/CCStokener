package jyt.model.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import jyt.log.Category;
import jyt.main.control.SystemRegistry;

public class WGet {

    static Category log = Category.getInstance(WGet.class);

    String directory_prefix = null;

    private long previousBytes = 0;

    private long currentBytes = 0;

    private double currentSpeed = 0;

    private int percentageDownloaded = 0;

    private int num;

    /**
     * Creates a new WGet object.
     */
    public WGet(int num) {
        directory_prefix = System.getProperty("user.dir");
        this.num = num;
    }

    /**
     * -P
     *
     * @param directory_prefix DOCUMENT ME!
     */
    public void setDirectoryPrefix(String directory_prefix) {
        this.directory_prefix = directory_prefix;
    }

    /**
     * @param url The url of the resource to download
     * @param prefixSubstitute Regexp which shall be replaced
     * @param substituteReplacement Replacement of the regexp
     *
     * @return bytes of downloaded resource
     *
     * @throws IOException URL might not exist
     */
    public byte[] download(URL url, String prefixSubstitute, String substituteReplacement) throws IOException {
        log.debug(".download(): " + url + " " + prefixSubstitute + " " + substituteReplacement);
        return downloadUsingHttpClient(url, prefixSubstitute, substituteReplacement);
    }

    /**
     * DOCUMENT ME!
     *
     * @param url DOCUMENT ME!
     * @param prefixSubstitute DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public byte[] downloadUsingHttpClient(URL url, String prefixSubstitute, String substituteReplacement) {
        log.debug(".downloadUsingHttpClient(): " + url);
        byte[] sresponse = null;
        try {
            sresponse = getResource(url);
            File file = new File(createFileName(url, prefixSubstitute, substituteReplacement));
            saveToFile(file.getAbsolutePath(), sresponse);
            substitutePrefix(file.getAbsolutePath(), prefixSubstitute, substituteReplacement);
        } catch (MalformedURLException e) {
            log.error(".downloadUsingHttpClient(): ", e);
        } catch (FileNotFoundException e) {
            log.error(".downloadUsingHttpClient(): ", e);
        } catch (IOException e) {
            log.error(".downloadUsingHttpClient(): ", e);
        }
        List links = null;
        try {
            links = getLinks(url);
        } catch (IOException ioe) {
            log.error(".downloadUsingHttpClient(): ", ioe);
        }
        if (links != null) {
            Iterator iterator = links.iterator();
            while (iterator.hasNext()) {
                String link = (String) iterator.next();
                try {
                    URL child_url = new URL(URLUtil.complete(url.toString(), link));
                    byte[] child_sresponse = getResource(child_url);
                    saveToFile(createFileName(child_url, prefixSubstitute, substituteReplacement), child_sresponse);
                } catch (Exception e) {
                    log.error(".downloadUsingHttpClient(): ", e);
                }
            }
        }
        return sresponse;
    }

    /**
     *
     */
    public byte[] getResource(URL url) throws IOException {
        log.debug(".getResource(): " + url);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        long fileSize = httpConnection.getContentLength();
        InputStream in = httpConnection.getInputStream();
        byte[] buffer = new byte[4096];
        int bytes_read;
        int progressChunkSize = 100 / SystemRegistry.getReg().getListModel().size();
        int totalPercent;
        ByteArrayOutputStream bufferOut = new ByteArrayOutputStream();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new GetSpeed(), 0, 1000);
        while ((bytes_read = in.read(buffer)) != -1) {
            bufferOut.write(buffer, 0, bytes_read);
            currentBytes += bytes_read;
            percentageDownloaded = (int) ((currentBytes * 100) / fileSize);
            SystemRegistry.getReg().getView().getMainPanel().getCurrentProgress().setValue(percentageDownloaded);
            totalPercent = (int) ((progressChunkSize * num) + (percentageDownloaded * 0.01 * progressChunkSize));
            SystemRegistry.getReg().getView().getMainPanel().getTotalProgress().setValue(totalPercent);
        }
        timer.cancel();
        byte[] sresponse = bufferOut.toByteArray();
        httpConnection.disconnect();
        return sresponse;
    }

    public byte[] getStoppableResource(URL url, DownloadThread sourceThread) throws IOException {
        log.debug(".getResource(): " + url);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        long fileSize = httpConnection.getContentLength();
        InputStream in = httpConnection.getInputStream();
        byte[] buffer = new byte[4096];
        int bytes_read;
        int progressChunkSize = 100 / SystemRegistry.getReg().getListModel().size();
        int totalPercent;
        ByteArrayOutputStream bufferOut = new ByteArrayOutputStream();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new GetSpeed(), 0, 1000);
        while ((bytes_read = in.read(buffer)) != -1 && !sourceThread.getStopRequest()) {
            bufferOut.write(buffer, 0, bytes_read);
            currentBytes += bytes_read;
            percentageDownloaded = (int) ((currentBytes * 100) / fileSize);
            SystemRegistry.getReg().getView().getMainPanel().getCurrentProgress().setValue(percentageDownloaded);
            totalPercent = (int) ((progressChunkSize * num) + (percentageDownloaded * 0.01 * progressChunkSize));
            if (totalPercent != 0) {
                SystemRegistry.getReg().getView().getMainPanel().getTotalProgress().setValue(totalPercent);
            }
        }
        timer.cancel();
        byte[] sresponse = bufferOut.toByteArray();
        httpConnection.disconnect();
        return sresponse;
    }

    public byte[] getResourceProxy(URL url, ProxyDefintion proxyDef) throws IOException {
        log.debug(".getResource() with Proxy " + proxyDef.toString() + ": " + url);
        Authenticator.setDefault(new SimpleAuthenticator(proxyDef.getUser(), proxyDef.getPassword()));
        Properties systemProperties = System.getProperties();
        systemProperties.setProperty("http.proxyHost", proxyDef.getProxy());
        systemProperties.setProperty("http.proxyPort", proxyDef.getPort());
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        InputStream in = httpConnection.getInputStream();
        byte[] buffer = new byte[1024];
        int bytes_read;
        ByteArrayOutputStream bufferOut = new ByteArrayOutputStream();
        while ((bytes_read = in.read(buffer)) != -1) {
            bufferOut.write(buffer, 0, bytes_read);
        }
        byte[] sresponse = bufferOut.toByteArray();
        httpConnection.disconnect();
        return sresponse;
    }

    /**
     *
     */
    public List getLinks(URL url) throws IOException {
        log.debug(".getLinks(): Get links from " + url);
        List links = null;
        try {
            HTML html = new HTML(url.toString());
            links = html.getImageSrcs(false);
            links.addAll(html.getLinkHRefs(false));
        } catch (Exception e) {
            log.error(".getLinks() Exception 423432: ", e);
        }
        if (links != null) {
            log.debug(".getLinks(): Number of links found: " + links.size());
        }
        return links;
    }

    /**
     * Substitute prefix, e.g. "/lenya/blog/live/" by "/"
     *
     * @param filename Filename
     * @param prefixSubstitute Prefix which shall be replaced
     * @param substituteReplacement Prefix which is going to replace the original
     *
     * @throws IOException DOCUMENT ME!
     */
    public void substitutePrefix(String filename, String prefixSubstitute, String substituteReplacement) throws IOException {
        log.debug("Replace " + prefixSubstitute + " by " + substituteReplacement);
        SED.replaceAll(new File(filename), escapeSlashes(prefixSubstitute), escapeSlashes(substituteReplacement));
    }

    /**
     * Escape slashes
     *
     * @return String with escaped slashes
     */
    public String escapeSlashes(String string) {
        StringBuffer buffer = new StringBuffer("");
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) == '/') {
                buffer.append("\\/");
            } else {
                buffer.append(string.charAt(i));
            }
        }
        return buffer.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String toString() {
        return "-P: " + directory_prefix;
    }

    /**
     *
     */
    public void saveToFile(String filename, byte[] bytes) throws FileNotFoundException, IOException {
        File file = new File(filename);
        File parent = new File(file.getParent());
        if (!parent.exists()) {
            log.warn(".saveToFile(): Directory will be created: " + parent.getAbsolutePath());
            parent.mkdirs();
        }
        FileOutputStream out = new FileOutputStream(file.getAbsolutePath());
        out.write(bytes);
        out.close();
    }

    /**
     * @param url URL of resource, which has been downloaded and shall be saved
     * @return Absolute substituted filename
     */
    public String createFileName(URL url, String prefixSubstitute, String substituteReplacement) {
        File file = new File(directory_prefix + File.separator + url.getFile());
        return file.getAbsolutePath().replaceAll(prefixSubstitute, substituteReplacement);
    }

    /**
     *
     */
    public byte[] runProcess(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(command);
        java.io.InputStream in = process.getInputStream();
        byte[] buffer = new byte[1024];
        int bytes_read = 0;
        java.io.ByteArrayOutputStream baout = new java.io.ByteArrayOutputStream();
        while ((bytes_read = in.read(buffer)) != -1) {
            baout.write(buffer, 0, bytes_read);
        }
        if (baout.toString().length() > 0) {
            log.debug(".runProcess(): %%%InputStream:START" + baout.toString() + "END:InputStream%%%");
        }
        java.io.InputStream in_e = process.getErrorStream();
        java.io.ByteArrayOutputStream baout_e = new java.io.ByteArrayOutputStream();
        while ((bytes_read = in_e.read(buffer)) != -1) {
            baout_e.write(buffer, 0, bytes_read);
        }
        if (baout_e.toString().length() > 0) {
            log.error(".runProcess(): ###ErrorStream:START" + baout_e.toString() + "END:ErrorStream###");
        }
        return baout.toByteArray();
    }

    class GetSpeed extends TimerTask {

        public void run() {
            currentSpeed = ((currentBytes - previousBytes) / 1024.0);
            previousBytes = currentBytes;
            DecimalFormat df = new DecimalFormat("#.##");
            SystemRegistry.getReg().getView().getMainPanel().getDownloadSpeedLabel().setText(df.format(currentSpeed) + " KB/s");
        }
    }
}
