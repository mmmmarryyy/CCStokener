package neembuu.mediafire;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import neembuu.directuploader.*;
import neembuu.directuploader.Uploader.ContentError;
import neembuu.mediafire.account.MFAccFSDataParser;
import neembuu.mediafire.account.MediafireAccountRoot;
import neembuu.mediafire.account.MediafireFileContainer;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import static neembuu.util.Constants.*;

/**
 *
 * @author Shashank Tulsyan
 */
public final class MediafireUploader extends AbstractUploader<MediafireFileContainer, MediafireUploadDestinationHost> implements Runnable {

    private MediafireFileContainer destinationDirectory;

    private MediafireFileContainer[] directories;

    private Object lock = new Object();

    private AbstractHttpClient httpClient;

    private Cookie ukey, user;

    private String quickKey = null;

    public static final String UPLOAD_URL = "http://www.mediafire.com/basicapi/douploadnonflash.php";

    public static final String POLL_UPLOAD_URL = "http://www.mediafire.com/basicapi/pollupload.php";

    public static final String GET_DIRECTORIES_KEY_URL = "http://www.mediafire.com//basicapi/getfolderkeys.php";

    public static final String FLASH_REFERER = "http://www.mediafire.com/myfiles.php";

    public long uploaded;

    public MediafireUploader() {
    }

    MediafireUploader(UploadOperation uploadOperation) throws IllegalArgumentException {
        this(uploadOperation, null, null);
    }

    MediafireUploader(UploadOperation uploadOperation, MediafireAccount account, MediafireFileContainer destinationDirectory) throws IllegalArgumentException {
        this.uploadOperation = uploadOperation;
        this.destinationDirectory = destinationDirectory;
        try {
            getBasicCookies();
        } catch (Exception any) {
        }
        HttpParams params = new BasicHttpParams();
        params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
        httpClient = new DefaultHttpClient(params);
    }

    public void executeUploadOperation() throws Exception {
        if (uploadOperation == null) throw new NullPointerException("Upload operation that is to be executed cannot be null");
        throwExceptionIfAlreadyRunning();
        started = true;
        LoginRequest loginRequest = getUploaderAccount().getLoginRequest(getHttpClient().getCookieStore());
        getHttpClient().execute(loginRequest.getPreLoginRequest()).getEntity().consumeContent();
        HttpResponse response = getHttpClient().execute(loginRequest.getHttpUriRequest());
        response.getEntity().consumeContent();
        getUploaderAccount().setLoginResponse(new HttpLoginResponse(response, getHttpClient().getCookieStore().getCookies()));
        for (Cookie cookie : getHttpClient().getCookieStore().getCookies()) {
            if (cookie.getName().equals("ukey") && cookie.getDomain().contains("mediafire")) {
                ukey = cookie;
            }
        }
        String uploaderkey = null;
        uploaderkey = getDestinationDirectory() == null ? null : getDestinationDirectory().getQuickKey();
        for (Cookie cookie : getHttpClient().getCookieStore().getCookies()) {
            if (cookie.getName().equals("user") && cookie.getDomain().contains("mediafire")) {
                user = cookie;
            }
        }
        if (user == null) {
            throw new RuntimeException("Could not initialize user cookie");
        }
        if (uploaderkey == null) {
        }
        getDirectories();
        MultipartEntity request = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        request.addPart("Filedata", new InputStreamBody(uploadOperation.getContentBeingUploaded().getInputStream(), "application/octet-stream", super.getDestinationFileName()));
        if (destinationDirectory == null) {
            throw new NullPointerException("wth");
        }
        System.out.println("destdir");
        System.out.println(destinationDirectory);
        System.out.println(destinationDirectory.getKey());
        HttpPost uploadRequest = new HttpPost(UPLOAD_URL + "?ukey=" + ukey.getValue() + "&user=" + user.getValue() + "&uploadkey=" + destinationDirectory.getKey() + "&filenum=0&uploader=0&MFU");
        uploadRequest.setEntity(request);
        uploadRequest.addHeader(new BasicHeader("Accept-Encoding", "gzip,deflate"));
        uploadRequest.addHeader(new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
        HttpResponse uploadResponse = getHttpClient().execute(uploadRequest);
        Object[] o = getUploadKey(uploadResponse);
        String uploadKey = (String) o[1];
        monitorUploadProgress(uploadKey);
    }

    @Override
    public ContentError setUploadOperation(UploadOperation uploadOperation) throws UploadingException {
        if (uploadOperation.getUploadFileName().endsWith("RAR") || uploadOperation.getUploadFileName().endsWith("rar") || uploadOperation.getUploadFileName().endsWith("ZIP") || uploadOperation.getUploadFileName().endsWith("zip")) {
            return ContentError.FILE_TYPE_NOT_SUPPORTED;
        }
        if (!uploadOperation.getContentBeingUploaded().canContentSizeBeDynamic()) {
            try {
                if (uploadOperation.getContentBeingUploaded().getFullSize() > 200 * MB) {
                    return ContentError.FILE_TOO_LARGE;
                }
            } catch (ContentLengthIsDynamicException clide) {
            }
        }
        super.setUploadOperation(uploadOperation);
        return ContentError.NONE;
    }

    @Override
    public final MediafireUploadDestinationHost getUploadDestination() {
        return MediafireUploadDestinationHost.getSingleton();
    }

    @Override
    public boolean supportsPauseAndResume() {
        return false;
    }

    @Override
    public int numberOfUploadersSupportedOnSameIP() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canUploadContentOfUnknownLength() {
        return false;
    }

    @Override
    public void pause() throws UploadingException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void resume() throws UploadingException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void stopAndFinalize() throws UploadingException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void run() {
        try {
            executeUploadOperation();
        } catch (Exception any) {
            any.printStackTrace();
        }
    }

    @Override
    public UploadStatus getUploadStatus() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MediafireFileContainer getDestinationDirectory() {
        return destinationDirectory;
    }

    @Override
    public void setDestinationDirectory(MediafireFileContainer destinationFile) {
        this.destinationDirectory = destinationFile;
    }

    private void throwExceptionIfAlreadyRunning() throws UploadingException {
        if (started) throw UploadingException.uploaderAlreadyRunning();
    }

    private void getBasicCookies() throws IOException {
        if (ukey != null) return;
        HttpGet getukey = new HttpGet(MediafireUploadDestinationHost.getSingleton().getHomePage());
        getHttpClient().execute(getukey).getEntity().consumeContent();
        for (Cookie cookie : getHttpClient().getCookieStore().getCookies()) {
            if (cookie.getName().equals("ukey") && cookie.getDomain().contains("mediafire")) {
                ukey = cookie;
                break;
            }
        }
        if (ukey == null) {
            System.out.println("ukey :(");
        }
    }

    private void finalizeUpload() {
    }

    private void getDirectories() throws IOException {
        if (user == null || ukey == null) {
            System.out.println("user and or ukey null");
        }
        if (directories != null) {
            if (directories.length != 0) {
                System.out.println("directories already present");
                return;
            }
        }
        HttpPost requestdirectories = new HttpPost(GET_DIRECTORIES_KEY_URL + "?ukey=" + ukey.getValue() + "&user=" + user.getValue());
        HttpResponse dirResponse = getHttpClient().execute(requestdirectories);
        String ds = EntityUtils.toString(dirResponse.getEntity());
        dirResponse.getEntity().consumeContent();
        getDirectories(ds);
    }

    private void getDirectories(String strXML) {
        strXML.contains("getuploadkey");
        LinkedList<MediafireFileContainer> dirs = MFAccFSDataParser.parseDirectoryList(strXML);
        directories = new MediafireFileContainer[dirs.size()];
        Iterator<MediafireFileContainer> it = dirs.iterator();
        for (int j = 0; it.hasNext(); j++) {
            MediafireFileContainer dir = it.next();
            if (dir.getName().contains(MediafireAccountRoot.DEFAULT_ROOT_NAME)) {
                MediafireAccountRoot root = new MediafireAccountRoot(getUploaderAccount(), dir.getKey());
                destinationDirectory = root;
                System.out.println("Root=" + destinationDirectory.getKey());
            }
            directories[j] = dir;
        }
    }

    private Object[] getUploadKey(HttpResponse response) {
        Object[] ret = new Object[2];
        String data = null;
        GZIPInputStream gzipis = null;
        try {
            gzipis = new GZIPInputStream(response.getEntity().getContent());
            StringBuilder sb = new StringBuilder();
            String line;
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(gzipis));
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } finally {
                gzipis.close();
            }
            data = sb.toString();
            System.out.println("++++data after ungziping+++");
            System.out.println(data);
            System.out.println("----data after ungziping---");
        } catch (IOException ioe) {
            System.out.println("gzip io exception");
            ioe.printStackTrace();
        }
        System.out.println("data=" + data);
        String checkFor = "nonFlashUploadComplete";
        data = data.substring(data.indexOf(checkFor) + checkFor.length());
        data = data.substring(0, data.indexOf(')'));
        data = data.substring(data.indexOf(',') + 1).trim();
        data = data.substring(data.indexOf(',') + 1).trim();
        data = data.substring(data.indexOf('\'') + 1, data.lastIndexOf('\''));
        System.out.println("key=" + data);
        ret[1] = data;
        return ret;
    }

    private void monitorUploadProgress(String uploadKey) throws IOException {
        HttpPost monitorRequest = new HttpPost(POLL_UPLOAD_URL + "?key=" + uploadKey + "&MFULConfig=jvjxdyyujlnnvegcrjfhmuvc82cy1nzy");
        String quickKey = null;
        boolean[] L = { false };
        while (quickKey == null) {
            HttpResponse hr = httpClient.execute(monitorRequest);
            String rep = EntityUtils.toString(hr.getEntity());
            hr.getEntity().consumeContent();
            System.out.println("rep=" + rep);
            quickKey = fetchQuickKey(rep, L);
            if (L[0] == true) break;
        }
        if (quickKey != null) this.quickKey = quickKey;
    }

    private String fetchQuickKey(String str, boolean[] L) {
        String QK = "<quickkey>", QKE = "</quickkey>";
        String lastDesc = "No more requests for this key";
        String DES = "<description>", DESE = "</description>";
        String s = str;
        s = s.substring(s.indexOf(DES) + DES.length());
        String ret = s.substring(0, s.indexOf(DESE)).trim();
        if (ret.equalsIgnoreCase(lastDesc)) L[0] = true;
        s = s.substring(s.indexOf(QK) + QK.length());
        ret = s.substring(0, s.indexOf(QKE)).trim();
        if (ret.length() > 0) return ret;
        return null;
    }

    private AbstractHttpClient getHttpClient() {
        return httpClient;
    }

    private void prepareHttpClient() {
    }

    public static void main(String[] args) throws Exception {
        UploadOperation toUpload = new UploadOperation(new UploadableFileContent(new java.io.File("j:\\Videos\\splits\\055. Sergej Vassiljevitsj Rachmaninoff - Piano Concerto No. 2 (Op. 18) - Moderato.mp3.001"), "Rachmaninoff.mp3.001"));
        MediafireUploader mediafireUploader = new MediafireUploader(toUpload);
        mediafireUploader.setUploaderAccount(MediafireAccount.TEST_ACCOUNT);
        new Thread(mediafireUploader).start();
    }

    @Override
    public void addUploadProgressListener(UploadProgressListener progressListener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
