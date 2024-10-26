package services.crawler;

import java.io.IOException;
import java.net.URL;
import com.asprise.util.pdf.PDFReader;

public class PdfDownloader implements Downloader {

    public StringBuffer get(URL url) throws IOException {
        StringBuffer pageContents = new StringBuffer();
        PDFReader reader = new PDFReader(url.openStream());
        reader.open();
        int pageCount = reader.getNumberOfPages();
        for (int i = 0; i < pageCount; i++) pageContents.append(new String(reader.extractTextFromPage(i).getBytes(), "UTF-8"));
        reader.close();
        return pageContents;
    }
}
