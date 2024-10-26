package net.sf.f2s.service.impl;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;
import net.sf.f2s.service.AbstractHttpService;
import net.sf.f2s.service.AuthenticationService;
import net.sf.f2s.service.Authenticator;
import net.sf.f2s.service.AuthenticatorCapability;
import net.sf.f2s.service.AuthenticatorListener;
import net.sf.f2s.service.CapabilityMatrix;
import net.sf.f2s.service.Credential;
import net.sf.f2s.service.DownloadListener;
import net.sf.f2s.service.DownloadListener.TimerWaitReason;
import net.sf.f2s.service.DownloadService;
import net.sf.f2s.service.DownloadServiceException;
import net.sf.f2s.service.Downloader;
import net.sf.f2s.service.DownloaderCapability;
import net.sf.f2s.service.Service;
import net.sf.f2s.service.UploadListener;
import net.sf.f2s.service.UploadListenerContentBody;
import net.sf.f2s.service.UploadService;
import net.sf.f2s.service.Uploader;
import net.sf.f2s.service.UploaderCapability;
import net.sf.f2s.service.config.ServiceConfiguration;
import net.sf.f2s.service.config.ServiceConfigurationProperty;
import net.sf.f2s.service.impl.MegaUploadService.MegaUploadServiceConfiguration;
import net.sf.f2s.util.HttpClientUtils;
import net.sf.f2s.util.RegexpUtils;
import net.sf.f2s.util.ThreadUtils;
import net.sf.f2s.util.io.InputStreamFilterChain;
import net.sf.f2s.util.io.OutputStreamFilterChain;
import org.apache.commons.io.IOUtil;
import org.apache.commons.vfs.FileObject;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;

/**
 * This service handles login, upload and download to MegaUpload.com.
 * 
 * @author Rogiel
 * @since 1.0
 */
public class MegaUploadService extends AbstractHttpService<MegaUploadServiceConfiguration> implements Service, UploadService, DownloadService, AuthenticationService {

    private static final Pattern UPLOAD_URL_PATTERN = Pattern.compile("http://www([0-9]*)\\.megaupload\\.com/upload_done\\.php\\?UPLOAD_IDENTIFIER=[0-9]*");

    private static final Pattern DOWNLOAD_DIRECT_LINK_PATTERN = Pattern.compile("http://www([0-9]*)\\.megaupload\\.com/files/([A-Za-z0-9]*)/([A-Za-z0-9]*)");

    private static final Pattern DOWNLOAD_TIMER = Pattern.compile("count=([0-9]*);");

    private static final Pattern DOWNLOAD_FILESIZE = Pattern.compile("[0-9]*(\\.[0-9]*)? (K|M|G)B");

    private static final Pattern DOWNLOAD_URL_PATTERN = Pattern.compile("http://www\\.megaupload\\.com/\\?d=([A-Za-z0-9]*)");

    public MegaUploadService(final MegaUploadServiceConfiguration configuration) {
        super(configuration);
    }

    @Override
    public String getId() {
        return "megaupload";
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public Uploader getUploader(FileObject file, String description) {
        return new MegaUploadUploader(file, description);
    }

    @Override
    public long getMaximumFilesize() {
        return 1 * 1024 * 1024 * 1024;
    }

    @Override
    public CapabilityMatrix<UploaderCapability> getUploadCapabilities() {
        return new CapabilityMatrix<UploaderCapability>(UploaderCapability.UNAUTHENTICATED_UPLOAD, UploaderCapability.NON_PREMIUM_ACCOUNT_UPLOAD, UploaderCapability.PREMIUM_ACCOUNT_UPLOAD);
    }

    @Override
    public Downloader getDownloader(URL url, FileObject file) {
        return new MegaUploadDownloader(url, file);
    }

    @Override
    public boolean matchURL(URL url) {
        return false;
    }

    @Override
    public CapabilityMatrix<DownloaderCapability> getDownloadCapabilities() {
        return new CapabilityMatrix<DownloaderCapability>(DownloaderCapability.UNAUTHENTICATED_DOWNLOAD, DownloaderCapability.NON_PREMIUM_ACCOUNT_DOWNLOAD, DownloaderCapability.PREMIUM_ACCOUNT_DOWNLOAD);
    }

    @Override
    public Authenticator getAuthenticator(Credential credential) {
        return new MegaUploadAuthenticator(credential);
    }

    @Override
    public CapabilityMatrix<AuthenticatorCapability> getAuthenticationCapability() {
        return new CapabilityMatrix<AuthenticatorCapability>();
    }

    protected class MegaUploadUploader implements Uploader {

        private final FileObject file;

        private final String description;

        public MegaUploadUploader(FileObject file, String description) {
            this.file = file;
            this.description = description;
        }

        @Override
        public void upload(UploadListener listener, OutputStreamFilterChain filterChain) {
            try {
                final String body = HttpClientUtils.get(client, "http://www.megaupload.com/multiupload/");
                final String url = RegexpUtils.find(UPLOAD_URL_PATTERN, body);
                final HttpPost upload = new HttpPost(url);
                final MultipartEntity entity = new MultipartEntity();
                upload.setEntity(entity);
                entity.addPart("multifile_0", new UploadListenerContentBody(listener, filterChain, file.getName().getBaseName(), file.getContent().getSize(), file.getContent().getInputStream()));
                if (description == null || description.length() == 0) {
                    entity.addPart("multimessage_0", new StringBody(configuration.getDefaultUploadDescription()));
                } else {
                    entity.addPart("multimessage_0", new StringBody(description));
                }
                final String linkPage = HttpClientUtils.execute(client, upload);
                final String downloadUrl = RegexpUtils.find(DOWNLOAD_URL_PATTERN, linkPage);
                listener.downloadLink(new URL(downloadUrl));
                listener.complete(file);
            } catch (Exception e) {
            }
        }
    }

    protected class MegaUploadDownloader implements Downloader {

        private final URL url;

        private FileObject file;

        public MegaUploadDownloader(URL url, FileObject file) {
            this.url = url;
            this.file = file;
        }

        @Override
        public void download(DownloadListener listener, InputStreamFilterChain filterChain) throws DownloadServiceException {
            try {
                final HttpGet request = new HttpGet(url.toString());
                final HttpResponse response = client.execute(request);
                final String content = IOUtil.toString(response.getEntity().getContent());
                final String stringTimer = RegexpUtils.find(DOWNLOAD_TIMER, content, 1);
                int timer = 0;
                if (stringTimer != null && stringTimer.length() > 0) {
                    timer = Integer.parseInt(stringTimer);
                }
                if (timer > 0 && configuration.respectWaitTime()) {
                    listener.timer(timer * 1000, TimerWaitReason.DOWNLOAD_TIMER);
                    ThreadUtils.sleep(timer * 1000);
                }
                final String downloadUrl = RegexpUtils.find(DOWNLOAD_DIRECT_LINK_PATTERN, content, 0);
                if (downloadUrl != null && downloadUrl.length() > 0) {
                    final HttpGet downloadRequest = new HttpGet(downloadUrl);
                    final HttpResponse downloadResponse = client.execute(downloadRequest);
                    if (downloadResponse.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN) {
                        listener.timer(10 * 60 * 1000, TimerWaitReason.COOLDOWN);
                        ThreadUtils.sleep(10 * 60 * 1000);
                        download(listener, filterChain);
                    } else {
                    }
                } else {
                    throw new DownloadServiceException("Download link not found");
                }
            } catch (IOException e) {
                throw new DownloadServiceException(e);
            }
        }

        @Override
        public void resume(DownloadListener listener, InputStreamFilterChain filterChain) throws DownloadServiceException {
        }

        private String getDirectDownloadLink(DownloadListener listener) throws IOException {
            final String page = HttpClientUtils.get(client, url.toString());
            final int timer = Integer.parseInt(RegexpUtils.find(DOWNLOAD_TIMER, page, 1));
            return null;
        }
    }

    protected class MegaUploadAuthenticator implements Authenticator {

        private final Credential credential;

        public MegaUploadAuthenticator(Credential credential) {
            this.credential = credential;
        }

        @Override
        public void login(AuthenticatorListener listener) {
            try {
                final HttpPost login = new HttpPost("http://www.megaupload.com/?c=login");
                final MultipartEntity entity = new MultipartEntity();
                login.setEntity(entity);
                entity.addPart("login", new StringBody("1"));
                entity.addPart("username", new StringBody(credential.getUsername()));
                entity.addPart("password", new StringBody(credential.getPassword()));
                final String response = HttpClientUtils.execute(client, login);
                if (response.contains("Username and password do " + "not match. Please try again!")) {
                    listener.invalidCredentials(credential);
                    return;
                }
                listener.loginSuccessful(credential);
            } catch (IOException e) {
            }
        }

        @Override
        public void logout(AuthenticatorListener listener) {
            try {
                final HttpPost logout = new HttpPost("http://www.megaupload.com/?c=account");
                final MultipartEntity entity = new MultipartEntity();
                logout.setEntity(entity);
                entity.addPart("logout", new StringBody("1"));
                HttpClientUtils.execute(client, logout);
                listener.logout(credential);
                return;
            } catch (IOException e) {
            }
        }
    }

    public static interface MegaUploadServiceConfiguration extends ServiceConfiguration {

        @ServiceConfigurationProperty(defaultValue = "true")
        boolean respectWaitTime();

        @ServiceConfigurationProperty(defaultValue = "80")
        int getPreferedDownloadPort();

        @ServiceConfigurationProperty(defaultValue = "Uploaded by Free2Share")
        String getDefaultUploadDescription();
    }
}
