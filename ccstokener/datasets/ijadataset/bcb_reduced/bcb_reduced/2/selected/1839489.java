package net.sf.leechget.service.megaupload;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Pattern;
import net.sf.leechget.service.api.DownloadObject;
import net.sf.leechget.service.api.Downloader;
import net.sf.leechget.util.IOUtil;
import net.sf.leechget.util.htmlparser.HtmlParserUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.ParserException;

/**
 * @author Rogiel
 * 
 */
public class MegaUploadDownloader implements Downloader {

    private static final Pattern DOWNLOAD_LINK_PATTERN = Pattern.compile("http://www([0-9]{1,4})\\.megaupload\\.com/files/([aA-zZ0-9])*/.*");

    private final HttpClient client;

    private final URL url;

    public MegaUploadDownloader(final HttpClient client, final URL url) {
        this.client = client;
        this.url = url;
    }

    @Override
    public DownloadObject download() throws IOException {
        final HttpGet findLink = new HttpGet(url.toString());
        final HttpResponse response = this.client.execute(findLink);
        final String body = IOUtil.getString(response);
        LinkTag linkTag = null;
        try {
            linkTag = HtmlParserUtil.findLink(MegaUploadDownloader.DOWNLOAD_LINK_PATTERN, body);
        } catch (ParserException e) {
        }
        if (linkTag != null) {
            String link = linkTag.extractLink();
            final String filename = IOUtil.getBaseName(link).trim();
            link = link.replaceAll("&#[0-9]*;", "_");
            final URL url = new URL(link);
            final String newLink = link.replaceAll(Pattern.quote(IOUtil.getBaseName(url.getPath())), URLEncoder.encode(IOUtil.getBaseName(url.getPath()), "UTF-8"));
            final HttpGet download = new HttpGet(newLink);
            final HttpResponse downloadResponse = this.client.execute(download);
            final HttpEntity downloadEntity = downloadResponse.getEntity();
            final long filesize = downloadEntity.getContentLength();
            return createObject(downloadEntity.getContent(), filename, filesize);
        } else {
            throw new RuntimeException("No download link.");
        }
    }

    private DownloadObject createObject(final InputStream in, final String filename, final long filesize) {
        return new DownloadObject(new DownloaderInputStream(in, this), filename, filesize);
    }

    @Override
    public void end() throws IOException {
    }
}
