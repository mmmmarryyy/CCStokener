package com.shuetech.groupon.session;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;
import java.util.UUID;
import javax.annotation.*;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.swing.ImageIcon;
import com.shuetech.general.data.EAOJPAHibernateBean;
import com.shuetech.groupon.entity.UploadedFile;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * Based on code from https://cwiki.apache.org/WICKET/uploaddownload.html
 * @author Shu
 * TODO: Future: Generalize to support all types of files, not just images
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class FileUploadBean implements FileUpload {

    protected static final int THUMBNAIL_SIZE = 150;

    @EJB
    private EAOJPAHibernateBean eao;

    public byte[] getFileContents(UploadedFile fileEntry) throws IOException {
        if (isUploadedFileAvailable(fileEntry)) {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            InputStream inStream = new FileInputStream(new File(fileEntry.getFileName()));
            copy(inStream, outStream);
            inStream.close();
            outStream.close();
            return outStream.toByteArray();
        } else {
            return createNotAvailableImage(fileEntry.getContentType());
        }
    }

    public Date getLastModifyTime(UploadedFile imageEntry) {
        File f = new File(imageEntry.getFileName());
        return new Date(f.lastModified());
    }

    public boolean isUploadedFileAvailable(UploadedFile fileEntry) {
        return (new File(fileEntry.getFileName()).exists() && new File(fileEntry.getThumbName()).exists());
    }

    public byte[] getThumbnail(UploadedFile imageEntry) throws IOException {
        if (isUploadedFileAvailable(imageEntry)) {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            InputStream inStream = new FileInputStream(new File(imageEntry.getThumbName()));
            copy(inStream, outStream);
            inStream.close();
            outStream.close();
            return outStream.toByteArray();
        } else {
            byte[] imageData = createNotAvailableImage(imageEntry.getContentType());
            return scaleImage(imageData, getThumbnailSize());
        }
    }

    /**
	 * Copies data from src into dst.
	 */
    private void copy(InputStream source, OutputStream destination) throws IOException {
        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = source.read(buf)) > 0) {
                destination.write(buf, 0, len);
            }
            source.close();
            destination.close();
        } catch (IOException ioe) {
            throw ioe;
        }
    }

    private File createImageFile(String suffix) throws IOException {
        UUID uuid = UUID.randomUUID();
        File file = new File(getFileUploadDir(), uuid.toString() + suffix);
        return file;
    }

    private File getFileUploadDir() throws IOException {
        String fileUploadDir = System.getProperty("jboss.server.data.dir") + "\\uploadedFiles";
        File dir = new File(fileUploadDir);
        if (!dir.exists()) dir.mkdir();
        return dir;
    }

    private byte[] createNotAvailableImage(String contentType) throws IOException {
        StringBuffer name = new StringBuffer("com/mysticcoders/resources/ImageNotAvailable.");
        if ("image/jpeg".equalsIgnoreCase(contentType)) {
            name.append("jpg");
        } else if ("image/png".equalsIgnoreCase(contentType)) {
            name.append("png");
        } else {
            name.append("gif");
        }
        URL url = getClass().getClassLoader().getResource(name.toString());
        InputStream in = url.openStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        in.close();
        out.close();
        return out.toByteArray();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void save(UploadedFile imageEntry, InputStream imageStream) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            copy(imageStream, baos);
            baos.close();
            byte[] imageData = baos.toByteArray();
            baos = null;
            String suffix = null;
            if ("image/gif".equalsIgnoreCase(imageEntry.getContentType())) {
                suffix = ".gif";
            } else if ("image/jpeg".equalsIgnoreCase(imageEntry.getContentType())) {
                suffix = ".jpeg";
            } else if ("image/png".equalsIgnoreCase(imageEntry.getContentType())) {
                suffix = ".png";
            }
            File newFile = createImageFile(suffix);
            OutputStream outStream = new FileOutputStream(newFile);
            outStream.write(imageData);
            outStream.close();
            imageEntry.setFileName(newFile.getAbsolutePath());
            newFile = createImageFile(".jpeg");
            byte[] thumbnailData = scaleImage(imageData, getThumbnailSize());
            outStream = new FileOutputStream(newFile);
            outStream.write(thumbnailData);
            outStream.close();
            imageEntry.setThumbName(newFile.getAbsolutePath());
            eao.persist(imageEntry);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private int getThumbnailSize() {
        return THUMBNAIL_SIZE;
    }

    private byte[] scaleImage(byte[] imageData, int maxSize) throws IOException {
        Image inImage = new ImageIcon(imageData).getImage();
        double scale = (double) maxSize / (double) inImage.getHeight(null);
        if (inImage.getWidth(null) > inImage.getHeight(null)) {
            scale = (double) maxSize / (double) inImage.getWidth(null);
        }
        int scaledW = (int) (scale * inImage.getWidth(null));
        int scaledH = (int) (scale * inImage.getHeight(null));
        BufferedImage outImage = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_RGB);
        AffineTransform tx = new AffineTransform();
        if (scale < 1.0d) {
            tx.scale(scale, scale);
        }
        Graphics2D g2d = outImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(inImage, tx, null);
        g2d.dispose();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(os);
        encoder.encode(outImage);
        os.close();
        return os.toByteArray();
    }
}
