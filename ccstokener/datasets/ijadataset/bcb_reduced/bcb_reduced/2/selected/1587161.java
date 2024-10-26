package au.edu.diasb.annotation.dannotate;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import au.edu.diasb.chico.mvc.InvalidRequestParameterException;
import au.edu.diasb.chico.mvc.RequestFailureException;
import au.edu.diasb.chico.mvc.RequestUtils;

/**
 * The thumbnailer class creates thumbnails images for display in
 * image annotations.
 * 
 * @author xchernich
 * @author scrawley
 */
class ThumbNailer {

    private static final Logger LOGGER = Logger.getLogger(ThumbNailer.class);

    /**
	 * A wrapper for an AWT Area based on a polygon defining an "arrow" pointer with
	 * orientation and location set by the location of a target point within 
	 * a square bounding box.
	 * 
	 * @author xchernich
	 */
    class Arrow {

        int A_WIDTH = 15;

        int A_HEIGHT = 30;

        Area arrow = null;

        Arrow(Rectangle clip, Point target) {
            int length = clip.height / 3;
            int astart = length - A_HEIGHT;
            int barb = A_WIDTH / 3;
            int aseg = A_WIDTH - barb;
            this.arrow = new Area(new Polygon(new int[] { barb, barb, 0, A_WIDTH / 2, A_WIDTH, aseg, aseg }, new int[] { 0, astart, astart, length, astart, astart, 0 }, 7));
            int quad = 0;
            int angle = 0;
            switch(getQuadrent(clip, target)) {
                case 1:
                    quad = 2;
                    break;
                case 2:
                    quad = -1;
                    break;
                case 3:
                    quad = 1;
                    break;
                case 4:
                    angle = -30;
                    break;
                case 5:
                    angle = 30;
                    break;
                case 6:
                    quad = 2;
                    angle = 30;
                    break;
                case 7:
                    quad = 2;
                    angle = -30;
                    break;
            }
            AffineTransform atf = new AffineTransform();
            atf.translate(-A_WIDTH / 2, -length);
            this.arrow.transform(atf);
            if (quad > 0) {
                this.arrow.transform(AffineTransform.getQuadrantRotateInstance(quad));
            }
            if (angle != 0) {
                this.arrow.transform(AffineTransform.getRotateInstance(Math.toRadians(angle)));
            }
            atf = new AffineTransform();
            atf.translate(target.x, target.y);
            this.arrow.transform(atf);
        }

        /**
	     * Accessor for g2d renderable object
	     * @return The translated and rotated arrow
	     */
        Area getArea() {
            return this.arrow;
        }

        /**
	     * Returns a value indicating the rotation a downwards facing arrow should
	     * undergo to orient on the point in the rectangle. In general, the values are:
	     * 0: down
	     * 1: up
	     * 2: right
	     * 3: left
	     * 4: down-right
	     * 5: down-left
	     * 6: up-right
	     * 7: up-left
	     * @param clip The square box containing the target point
	     * @param target Point coordinates in the box
	     * @return orientation value per heuristic.
	     */
        private int getQuadrent(Rectangle clip, Point target) {
            int tnx = target.x - clip.x;
            int tny = target.y - clip.y;
            int mid = clip.width / 2;
            int lmg = mid / 2;
            int rmg = lmg + mid;
            int tmg = A_HEIGHT;
            int bmg = clip.height - A_HEIGHT;
            int quad = -1;
            if ((tnx >= lmg) && (tnx <= rmg)) {
                quad = (tny >= mid) ? 0 : 1;
            } else if ((tny >= tmg) && (tny <= bmg)) {
                quad = (tnx <= lmg) ? 3 : 2;
            } else if (tny >= bmg) {
                quad = (tnx <= lmg) ? 5 : 4;
            } else if (tny <= tmg) {
                quad = (tnx <= lmg) ? 7 : 6;
            }
            return quad;
        }
    }

    /**
	 * Read a remote image and create a thumbnail, writing it to the
	 * response output stream.
	 * 
	 * @param response
	 * @param urlString
	 * @param rect
	 * @throws RequestFailureException
	 * @throws IOException
	 */
    public void process(HttpServletResponse response, String urlString, String rect) throws RequestFailureException, IOException {
        Rectangle sourceRegion = validRegion(rect);
        URL url = new URL(urlString);
        URLConnection con = url.openConnection();
        String contentType = con.getContentType();
        String mimeType = contentType == null ? "" : RequestUtils.asMimeType(contentType);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Specified image mimeType is '" + mimeType + "'");
        }
        ImageReader reader = getImageReader(url, mimeType);
        InputStream is = null;
        OutputStream os = null;
        ImageWriter writer = null;
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Using image reader " + reader);
            }
            if (mimeType.isEmpty()) {
                mimeType = reader.getOriginatingProvider().getMIMETypes()[0];
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Inferred image mimeType is '" + mimeType + "'");
                }
            }
            is = con.getInputStream();
            reader.setInput(new MemoryCacheImageInputStream(is));
            writer = ImageIO.getImageWriter(reader);
            RenderedImage renderedImage = renderThumbnail(sourceRegion, reader);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(contentType == null ? mimeType : contentType);
            os = response.getOutputStream();
            MemoryCacheImageOutputStream mcos = new MemoryCacheImageOutputStream(os);
            writer.setOutput(mcos);
            writer.write(renderedImage);
            mcos.close();
        } finally {
            if (os != null) {
                os.close();
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                }
            }
            reader.dispose();
            if (writer != null) {
                writer.dispose();
            }
        }
    }

    /**
     * Construct the thumbnail image.  This handles both region
     * and point selections.  In the latter case we create a
     * thumbnail that consists of a rectangle around the image
     * and paint an arrow that indicates the point.
     * 
     * @param sourceRegion the selected region for the thumbnail
     * @param reader the image reader for the full image.
     * @return the rendered thumbnail
     * @throws IOException
     * @throws RequestFailureException
     */
    private RenderedImage renderThumbnail(Rectangle sourceRegion, ImageReader reader) throws IOException, RequestFailureException {
        RenderedImage renderedImage;
        try {
            ImageReadParam params = reader.getDefaultReadParam();
            params.setSourceRegion(sourceRegion);
            if ((sourceRegion.height > 1) && (sourceRegion.width > 1)) {
                renderedImage = reader.readAsRenderedImage(0, params);
            } else {
                BufferedImage imgBuf = reader.read(0);
                int clipSize = 200;
                clipSize = Math.min(clipSize, Math.min(imgBuf.getHeight(), imgBuf.getWidth()));
                Rectangle clip = new Rectangle(clipSize, clipSize);
                clip.x = sourceRegion.x - (int) (clipSize / 2);
                clip.y = sourceRegion.y - (int) (clipSize / 2);
                if ((clip.x < 0) || (clip.x + clipSize > imgBuf.getWidth())) {
                    clip.x = (clip.x < 0) ? 0 : imgBuf.getWidth() - clipSize;
                }
                if ((clip.y < 0) || (clip.y + clipSize > imgBuf.getHeight())) {
                    clip.y = (clip.y < 0) ? 0 : imgBuf.getHeight() - clipSize;
                }
                Graphics2D g2d = imgBuf.createGraphics();
                g2d.setPaint(Color.RED);
                g2d.setComposite(AlphaComposite.SrcIn);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Arrow arrow = new Arrow(clip, new Point(sourceRegion.x, sourceRegion.y));
                g2d.fill(arrow.getArea());
                g2d.dispose();
                renderedImage = imgBuf.getSubimage(clip.x, clip.y, clip.width, clip.height);
            }
        } catch (IllegalArgumentException ex) {
            throw new RequestFailureException(HttpServletResponse.SC_BAD_REQUEST, "Thumbnail image extraction failed", ex);
        }
        return renderedImage;
    }

    /**
	 * Figure out what kind of ImageReader to use to process the
	 * URL.  Use the supplied mimeType if possible, and fallback
	 * to guessing from the URL path suffix.
	 * 
	 * @param url the image URL
	 * @param mimeType a MIME type or {@literal null}
	 * @return the ImageReader to be used.
	 * @throws RequestFailureException
	 */
    private ImageReader getImageReader(URL url, String mimeType) throws RequestFailureException {
        Iterator<ImageReader> it = ImageIO.getImageReadersByMIMEType(mimeType);
        if (!it.hasNext()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Image mimeType not recognized - trying the file suffix method");
            }
            for (String suffix : ImageIO.getReaderFileSuffixes()) {
                if (url.getPath().endsWith("." + suffix)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Matched known image file suffix '" + suffix + "'");
                    }
                    it = ImageIO.getImageReadersBySuffix(suffix);
                    break;
                }
            }
            if (!it.hasNext()) {
                if (mimeType.isEmpty()) {
                    throw new RequestFailureException(HttpServletResponse.SC_BAD_REQUEST, "No image reader for URL path '" + url.getPath() + "'");
                } else {
                    throw new RequestFailureException(HttpServletResponse.SC_BAD_REQUEST, "No image reader for mimeType '" + mimeType + "'");
                }
            }
        }
        ImageReader reader = it.next();
        return reader;
    }

    /**
	 * Turn a String representing rectangle coordinates into a 
	 * {@link Rectangle} object
	 * 
	 * @param coords the input string
	 * @return the {@link Rectangle}
	 * @throws InvalidRequestParameterException
	 */
    private Rectangle validRegion(String coords) throws InvalidRequestParameterException {
        Pattern pat = Pattern.compile("^(\\d+?),(\\d+?),(\\d+?),(\\d+?)$");
        Matcher mat = pat.matcher(coords);
        if (!mat.matches()) {
            throw new InvalidRequestParameterException("Invalid rectangle: " + coords);
        }
        return new Rectangle(Integer.parseInt(mat.group(1)), Integer.parseInt(mat.group(2)), Integer.parseInt(mat.group(3)), Integer.parseInt(mat.group(4)));
    }
}
