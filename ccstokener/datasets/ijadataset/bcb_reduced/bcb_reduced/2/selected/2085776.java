package eu.digmap.thumbnails;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class ThumbnailMakerFactory {

    public static AbstractThumbnailMaker getThumbnailMaker(ThumbnailParams params) throws IOException {
        URL url = new URL(params.uri);
        URLConnection connection = url.openConnection();
        String type = connection.getContentType();
        if (type == null) {
            type = "text/plain";
        }
        params.connection = connection.getInputStream();
        AbstractThumbnailMaker thumbnailMaker = null;
        if (type.contains("image/") || isURLImageType(params.uri)) {
            thumbnailMaker = new ImageThumbnailMaker();
        } else if (type.contains("/pdf") || params.uri.toLowerCase().trim().endsWith(".pdf")) {
            thumbnailMaker = new PDFThumbnailMaker();
        } else {
            thumbnailMaker = new HTMLThumbnailMaker();
        }
        if (thumbnailMaker != null) {
            thumbnailMaker.setParams(params);
        }
        return thumbnailMaker;
    }

    private static boolean isURLImageType(String uri) {
        uri = uri.toLowerCase().trim();
        if (uri.endsWith(".jpeg")) return true;
        if (uri.endsWith(".jpg")) return true;
        if (uri.endsWith(".gif")) return true;
        if (uri.endsWith(".png")) return true;
        return false;
    }
}
