package net.narusas.cafelibrary.bookfactories;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import net.narusas.cafelibrary.Book;
import net.narusas.util.lang.NFile;
import net.narusas.util.lang.NInputStream;

public class ImageDownloader {

    private static File root;

    static Logger logger = Logger.getLogger("log");

    static {
        root = new File("data");
        if (root.exists() == false) {
            root.mkdir();
        }
    }

    private String lastUrlString;

    private byte[] lastData;

    public void download(Book book) {
        logger.info(book + "�� Ŀ�� �̹��� �ٿ�ε带 �����մϴ�. ");
        logger.info("Ŀ�� �̹���(Large)�� �ٿ�ε带 �����մϴ�. ");
        downloadImage(book.getId(), book.getCoverLargeUrl(), "L");
        logger.info("Ŀ�� �̹���(Samll)�� �ٿ�ε带 �����մϴ�. ");
        downloadImage(book.getId(), book.getCoverSmallUrl(), "S");
        logger.info("����� �̹����� ���մϴ�.");
        creatThumbnail(book);
        book.imageDownloaded();
        book.getCoverImage();
    }

    private void creatThumbnail(Book book) {
        try {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("jpg");
            ImageReader reader = readers.next();
            Rectangle rect = new Rectangle(0, 0, 12, 17);
            File f = new File(root, book.getId() + "L" + ".jpg");
            ImageInputStream iis = ImageIO.createImageInputStream(f);
            reader.setInput(iis, true);
            ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceRegion(rect);
            BufferedImage bi = reader.read(0, param);
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter writer = writers.next();
            File target = new File(root, book.getId() + "T" + ".jpg");
            ImageOutputStream ios = ImageIO.createImageOutputStream(target);
            writer.setOutput(ios);
            writer.write(bi);
        } catch (Exception e) {
        }
    }

    private void downloadImage(long id, String urlString, String susfix) {
        if (urlString == null || "".equals(urlString)) {
            return;
        }
        String fileName = id + susfix;
        if (urlString.endsWith("jpg")) {
            fileName += ".jpg";
        } else if (urlString.endsWith("png")) {
            fileName += ".png";
        } else if (urlString.endsWith("gif")) {
            fileName += ".gif";
        }
        File target = new File(root, fileName);
        if (target.exists()) {
            return;
        }
        try {
            byte[] data = getData(urlString);
            NFile.write(target, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * @param urlString
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
    private byte[] getData(String urlString) throws MalformedURLException, IOException {
        byte[] data;
        if (canUseLast(urlString)) {
            data = lastData;
        } else {
            data = getNewData(urlString);
        }
        return data;
    }

    /**
	 * @param urlString
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
    private byte[] getNewData(String urlString) throws MalformedURLException, IOException {
        byte[] data;
        URL url = new URL(urlString);
        lastUrlString = urlString;
        InputStream in = url.openStream();
        data = NInputStream.readBytes(in);
        lastData = data;
        return data;
    }

    /**
	 * @param urlString
	 * @return
	 */
    private boolean canUseLast(String urlString) {
        return lastUrlString != null && lastUrlString.equals(urlString) && lastData != null && lastData.length != 0;
    }
}
