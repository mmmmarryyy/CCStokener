package net.stickycode.deploy.grizzly;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WarTest {

    private Logger log = LoggerFactory.getLogger(getClass());

    File deployables = new File("/tmp");

    String repository = "https://nexus.stickycode.net/content/groups/public/";

    private HttpClient httpclient = new DefaultHttpClient();

    @Test
    public void gizzlyPlusTestWar() throws ClientProtocolException, IOException {
        downloadGrizzly();
        httpclient.getConnectionManager().shutdown();
    }

    private void downloadGrizzly() throws ClientProtocolException, IOException {
        String groupId = "com/sun/grizzly";
        String artifactId = "grizzly-http-servlet-deployer";
        String version = "1.9.18d";
        String type = "jar";
        download(groupId, artifactId, version, type);
    }

    private void download(String groupId, String artifactId, String version, String type) throws ClientProtocolException, IOException {
        String finalName = artifactId + "-" + version;
        File file = new File(deployables, groupId + "/" + artifactId + "/" + version + "/" + finalName + "." + type);
        if (file.exists()) {
            log.warn("Won't download {} found at {}", finalName, file.getAbsolutePath());
            return;
        }
        String url = repository + groupId + "/" + artifactId + "/" + version + "/" + finalName + "." + type;
        HttpGet get = new HttpGet(url);
        HttpResponse response = httpclient.execute(get);
        if (response.getStatusLine().getStatusCode() == 200) writeContent(get, response.getEntity(), file); else throw new RuntimeException("Failed to download " + url + " due to error " + response.getStatusLine());
    }

    private void writeContent(HttpGet get, HttpEntity entity, File file) throws IOException {
        try {
            writeToFile(entity, file);
        } catch (IOException e) {
        } catch (RuntimeException e) {
            get.abort();
        }
    }

    private void writeToFile(HttpEntity entity, File file) throws IOException {
        File tmp = File.createTempFile("sticky", ".part");
        log.info("tmp file {}", tmp);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmp));
        try {
            entity.writeTo(out);
        } finally {
            out.close();
            log.info("copy {} to {}", tmp, file);
            file.getParentFile().mkdirs();
            if (!tmp.renameTo(file)) throw new RuntimeException("failed to download " + file);
        }
    }
}
