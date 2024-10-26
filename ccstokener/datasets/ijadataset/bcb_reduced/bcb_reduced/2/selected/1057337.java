package de.ulrich_fuchs.jtypeset.linebreak;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import com.lowagie.text.Image;
import de.ulrich_fuchs.jtypeset.FlowContext;
import de.ulrich_fuchs.jtypeset.LayoutingContext;

/**
 *
 * @author  ulrich
 */
public class ImageRenderer implements ContentRenderer {

    /** Creates a new instance of TextRenderer */
    public ImageRenderer() {
    }

    /** renders the text at the given position, y indicates the baseline */
    public void render(ParagraphElement cnt, double x, double y, Graphics2D g, LayoutingContext layoutingContext, FlowContext flowContext) {
        InlineImageContent ic = (InlineImageContent) cnt;
        try {
            URLConnection urlConn = ic.getUrl().openConnection();
            urlConn.setConnectTimeout(15000);
            ImageInputStream iis = ImageIO.createImageInputStream(urlConn.getInputStream());
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                System.out.println("loading image " + ic.getUrl());
                ImageReader reader = readers.next();
                reader.setInput(iis, true);
                if (flowContext.pdfContext == null) {
                    RenderedImage img = reader.readAsRenderedImage(0, null);
                    renderOnGraphics(img, x, y, ic, g, layoutingContext, flowContext);
                } else {
                    BufferedImage img = reader.read(0);
                    renderDirectPdf(img, x, y, ic, g, layoutingContext, flowContext);
                }
                reader.dispose();
            } else System.err.println("cannot render image " + ic.getUrl() + " - no suitable reader!");
        } catch (Exception exc) {
            System.err.println("cannot render image " + ic.getUrl() + " due to exception:");
            System.err.println(exc);
            exc.printStackTrace(System.err);
        }
    }

    protected void renderOnGraphics(RenderedImage img, double x, double y, InlineImageContent cnt, Graphics2D g, LayoutingContext layoutingContext, FlowContext flowContext) {
        ContentMetrics m = cnt.getMetrics(g, flowContext);
        double scaleX = m.width / (double) img.getWidth();
        double scaleY = (m.ascent + m.descent) / (double) img.getHeight();
        AffineTransform tl = AffineTransform.getTranslateInstance(x, y - m.ascent);
        AffineTransform rs = AffineTransform.getScaleInstance(scaleX, scaleY);
        tl.concatenate(rs);
        g.drawRenderedImage(img, tl);
    }

    protected void renderDirectPdf(BufferedImage bfImg, double x, double y, InlineImageContent cnt, Graphics2D g, LayoutingContext layoutingContext, FlowContext flowContext) {
        ContentMetrics m = cnt.getMetrics(g, flowContext);
        double inchWidth = m.width / 72.0;
        double inchHeight = (m.ascent + m.descent) / 72.0;
        int dpi = layoutingContext.getResolution();
        double pixWidth = inchWidth * dpi;
        double pixHeight = inchHeight * dpi;
        if (pixWidth > bfImg.getWidth()) pixWidth = bfImg.getWidth();
        if (pixHeight > bfImg.getHeight()) pixHeight = bfImg.getHeight();
        try {
            java.awt.Image awtImg;
            if (pixWidth < bfImg.getWidth() || pixHeight < bfImg.getHeight()) awtImg = bfImg.getScaledInstance((int) pixWidth, (int) pixHeight, bfImg.SCALE_SMOOTH); else awtImg = bfImg;
            Image pdfImg;
            BufferedImage bfImg2 = new BufferedImage((int) pixWidth, (int) pixHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D bfImg2graphics = bfImg2.createGraphics();
            bfImg2graphics.drawImage(awtImg, 0, 0, null);
            ImageWriter writer;
            if (bfImg.getType() != bfImg.TYPE_BYTE_INDEXED && bfImg.getType() != bfImg.TYPE_BYTE_GRAY && bfImg.getType() != bfImg.TYPE_BYTE_BINARY) {
                Iterator writers = ImageIO.getImageWritersByFormatName("jpg");
                writer = (ImageWriter) writers.next();
                ImageWriteParam iwparam = new JPEGImageWriteParam(Locale.getDefault());
                iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                iwparam.setCompressionQuality(0.98f);
            } else {
                Iterator writers = ImageIO.getImageWritersByFormatName("png");
                writer = (ImageWriter) writers.next();
            }
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(os);
            writer.setOutput(ios);
            writer.write(bfImg2);
            bfImg.flush();
            pdfImg = Image.getInstance(os.toByteArray());
            os.close();
            ios.flush();
            ios.close();
            writer.dispose();
            pdfImg.setDpi(dpi, dpi);
            pdfImg.scaleAbsolute((float) m.width, (float) (m.ascent + m.descent));
            int yItext = (int) (flowContext.pdfContext.doc.getPageSize().height() - (y + m.descent));
            x += flowContext.pdfContext.xTranslation;
            yItext += flowContext.pdfContext.yTranslation;
            pdfImg.setAbsolutePosition((int) x, yItext);
            flowContext.pdfContext.getWriter().getDirectContent().addImage(pdfImg);
        } catch (Exception exc) {
            System.err.println("cannot render loaded image due to exception:");
            System.err.println(exc);
            exc.printStackTrace(System.err);
        }
    }

    /** marks the text position, y indicates the baseline */
    public void markPosition(ParagraphElement cnt, double x, double y, Graphics2D g, FlowContext context) {
    }
}
