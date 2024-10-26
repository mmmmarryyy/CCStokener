package com.ds;

import java.net.*;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: daniel
 * Date: 30/10/2009
 * Time: 07:44:32
 */
public class TorrentHandler {

    private static Logger logger = Logger.getLogger("rsstorrent");

    private static final String TORRENT_EXT = ".torrent";

    private static final int READ_TIMEOUT = 15 * 1000;

    public static void main(String[] args) throws IOException, URISyntaxException {
        execTorrent(new File("C:\\temp\\rss\\torrents\\Californication.S03E04.HDTV.XviD-SYS.[eztv].torrent"));
    }

    public static File downloadTorrentFile(String torrentURL, String folderToSave) throws IOException {
        logger.info("start to download torrent file: " + torrentURL);
        URL url = new URL(torrentURL);
        HttpURLConnection connection = (HttpURLConnection) (url.openConnection());
        connection.setReadTimeout(READ_TIMEOUT);
        InputStream is = connection.getInputStream();
        String contentType = connection.getContentType();
        if (!contentType.equals("application/x-bittorrent")) {
            logger.warning("TorrentHandler: contentType is not torrentFile file. contentType = " + contentType);
            throw new IllegalArgumentException("url is not of application/x-bittorrent type");
        }
        String filename = torrentURL.substring(torrentURL.lastIndexOf('/') + 1);
        if (!filename.endsWith(TORRENT_EXT)) {
            filename += TORRENT_EXT;
        }
        filename = URLDecoder.decode(filename, "UTF-8");
        File torrentFile = new File(folderToSave, filename);
        new File(folderToSave).mkdirs();
        byte[] buf = new byte[1024];
        FileOutputStream fos = new FileOutputStream(torrentFile);
        int n;
        while ((n = is.read(buf, 0, 1024)) > -1) fos.write(buf, 0, n);
        fos.close();
        is.close();
        return torrentFile;
    }

    public static void execTorrent(File file) throws IOException {
        String[] cmdarray = new String[] { "cmd", "/c", file.getAbsolutePath() };
        Runtime.getRuntime().exec(cmdarray);
    }
}
