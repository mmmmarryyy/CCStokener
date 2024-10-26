package services.crawler;

import java.io.IOException;
import java.net.URL;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class DocDownloader implements Downloader {

    public StringBuffer get(URL url) throws IOException {
        String contents = null;
        POIFSFileSystem poifsFileSystem;
        try {
            poifsFileSystem = new POIFSFileSystem(url.openStream());
            HWPFDocument docDocument = new HWPFDocument(poifsFileSystem);
            WordExtractor wordExtractor = new WordExtractor(docDocument);
            contents = new String(wordExtractor.getText().getBytes(), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new StringBuffer(contents);
    }
}
