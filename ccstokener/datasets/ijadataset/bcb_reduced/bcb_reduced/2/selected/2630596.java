package org.lex.input.mouse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.lex.util.DirectoryTextureLoader;
import org.lex.util.TextureLoader;
import org.lex.util.UrlFactory;
import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.math.Vector2f;

public class Cursor {

    private static final Logger log = Logger.getLogger(Cursor.class.getName());

    private static final String cursorArchiveFile = ".car";

    private static final String cursorDescriptorFile = ".cursor";

    private static final String defaultDescriptorFile = "default.cursor";

    private static final String commentString = "#";

    private static final String delims = "\t, =";

    private static final String hotSpotXPrefix = "hotSpotOffset.x";

    private static final String hotSpotYPrefix = "hotSpotOffset.y";

    private static final String timePrefix = "time";

    private static final String imagePrefix = "image";

    private static Map<URL, Cursor> cursorCache = new HashMap<URL, Cursor>();

    /**
	 * @see net.mindgamer.risetothestars.common.mousemanager.Cursor#load(java.lang.String, java.net.URL, java.lang.String)
	 * 
	 * @param name the name to be givent to the cursor
	 * @param url the url pointing to the cursor file
	 * @return the loaded cursor, null if cursor could not be loaded
	 */
    public static Cursor load(URL url) {
        return load(url, null);
    }

    /**
	 * Use this method to load the cursor files. You have two alternatives:
	 * 
	 * 1) Use a folder and load ".cursor" files by pointing to them directly
	 * with a url. In this case the desciptor parameter will be ignored and
	 * should be null.
	 * 2) Use a zip file with ".car" extension by pointing to it with a url
	 * and specifying the descriptor file you are going to use. If you pass
	 * descriptor = null then this method will look for a file "default.cursor"
	 * inside the zip file and abort if the file "default.cursor" is not found.
	 * 
	 * For simplicity, subfolder and zipfiles with subfolders are not supported.
	 * 
	 * @param url the url to a cursor file (.car) or descriptor file (.cursor)
	 * @param descriptor the name of the descriptor file (with extension)
	 * @return the loaded cursor, null if unable to load
	 */
    public static Cursor load(URL url, String descriptor) {
        if (url == null) {
            log.log(Level.WARNING, "Trying to load a cursor with a null url.");
            return null;
        }
        String cursorFile = url.getFile();
        BufferedReader reader = null;
        int lineNumber = 0;
        try {
            DirectoryTextureLoader loader;
            URL cursorUrl;
            if (cursorFile.endsWith(cursorDescriptorFile)) {
                cursorUrl = url;
                Cursor cached = cursorCache.get(url);
                if (cached != null) return cached;
                reader = new BufferedReader(new InputStreamReader(url.openStream()));
                loader = new DirectoryTextureLoader(url, false);
            } else if (cursorFile.endsWith(cursorArchiveFile)) {
                loader = new DirectoryTextureLoader(url, true);
                if (descriptor == null) descriptor = defaultDescriptorFile;
                cursorUrl = loader.makeUrl(descriptor);
                Cursor cached = cursorCache.get(url);
                if (cached != null) return cached;
                ZipInputStream zis = new ZipInputStream(url.openStream());
                ZipEntry entry;
                boolean found = false;
                while ((entry = zis.getNextEntry()) != null) {
                    if (descriptor.equals(entry.getName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new IOException("Descriptor file \"" + descriptor + "\" was not found.");
                }
                reader = new BufferedReader(new InputStreamReader(zis));
            } else {
                log.log(Level.WARNING, "Invalid cursor fileName \"{0}\".", cursorFile);
                return null;
            }
            Cursor cursor = new Cursor();
            cursor.url = cursorUrl;
            List<Integer> delays = new ArrayList<Integer>();
            List<String> frameFileNames = new ArrayList<String>();
            Map<String, Texture> textureCache = new HashMap<String, Texture>();
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                int commentIndex = line.indexOf(commentString);
                if (commentIndex != -1) {
                    line = line.substring(0, commentIndex);
                }
                StringTokenizer tokens = new StringTokenizer(line, delims);
                if (!tokens.hasMoreTokens()) continue;
                String prefix = tokens.nextToken();
                if (prefix.equals(hotSpotXPrefix)) {
                    cursor.hotSpotOffset.x = Integer.valueOf(tokens.nextToken());
                } else if (prefix.equals(hotSpotYPrefix)) {
                    cursor.hotSpotOffset.y = Integer.valueOf(tokens.nextToken());
                } else if (prefix.equals(timePrefix)) {
                    delays.add(Integer.valueOf(tokens.nextToken()));
                    if (tokens.nextToken().equals(imagePrefix)) {
                        String file = tokens.nextToken("");
                        file = file.substring(file.indexOf('=') + 1);
                        file.trim();
                        frameFileNames.add(file);
                        if (textureCache.get(file) == null) {
                            textureCache.put(file, loader.loadTexture(file));
                        }
                    } else {
                        throw new NoSuchElementException();
                    }
                }
            }
            cursor.frameFileNames = frameFileNames.toArray(new String[0]);
            cursor.textureCache = textureCache;
            cursor.delays = new int[delays.size()];
            cursor.images = new Image[frameFileNames.size()];
            cursor.textures = new Texture[frameFileNames.size()];
            for (int i = 0; i < cursor.frameFileNames.length; i++) {
                cursor.textures[i] = textureCache.get(cursor.frameFileNames[i]);
                cursor.images[i] = cursor.textures[i].getImage();
                cursor.delays[i] = delays.get(i);
            }
            if (delays.size() == 1) cursor.delays = null;
            if (cursor.images.length == 0) {
                log.log(Level.WARNING, "The cursor has no animation frames.");
                return null;
            }
            cursor.width = cursor.images[0].getWidth();
            cursor.height = cursor.images[0].getHeight();
            cursorCache.put(cursor.url, cursor);
            return cursor;
        } catch (MalformedURLException mue) {
            log.log(Level.WARNING, "Unable to load cursor.", mue);
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Unable to load cursor.", ioe);
        } catch (NumberFormatException nfe) {
            log.log(Level.WARNING, "Numerical error while parsing the " + "file \"{0}\" at line {1}", new Object[] { url, lineNumber });
        } catch (IndexOutOfBoundsException ioobe) {
            log.log(Level.WARNING, "Error, \"=\" expected in the file \"{0}\" at line {1}", new Object[] { url, lineNumber });
        } catch (NoSuchElementException nsee) {
            log.log(Level.WARNING, "Error while parsing the file \"{0}\" at line {1}", new Object[] { url, lineNumber });
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                    log.log(Level.SEVERE, "Unable to close the steam.", ioe);
                }
            }
        }
        return null;
    }

    private URL url;

    /** from bottom left point of the cursor image */
    private Vector2f hotSpotOffset;

    private int width;

    private int height;

    private Texture[] textures;

    private Image[] images;

    private int[] delays;

    private int currentFrame;

    private float frameTime;

    private boolean frameUpdated;

    private String[] frameFileNames;

    private Map<String, Texture> textureCache;

    private Cursor parent;

    private int rotation;

    protected Cursor() {
        hotSpotOffset = new Vector2f();
    }

    public URL getUrl() {
        return url;
    }

    /**
	 * @return the "active" pixel on the cursor image, from bottom left
	 */
    public Vector2f getHotSpotOffset() {
        return hotSpotOffset;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getNumberOfFrames() {
        return textures.length;
    }

    public int[] getDelays() {
        return delays;
    }

    public Texture getTextureFrame(int i) {
        return textures[i];
    }

    public Image getImageFrame(int i) {
        return textures[i].getImage();
    }

    public Image[] getImages() {
        return images;
    }

    public void update(float time) {
        if (delays == null) return;
        int lastFrame = currentFrame;
        frameTime += time * 1000;
        while (frameTime >= delays[currentFrame]) {
            frameTime -= delays[currentFrame];
            currentFrame++;
            if (currentFrame >= textures.length) currentFrame = 0;
        }
        frameUpdated = (lastFrame != currentFrame);
    }

    public boolean isFrameUpdated() {
        return frameUpdated;
    }

    /**
	 * @return the index of the next frame
	 */
    public int getNextFrameIndex() {
        return currentFrame;
    }

    /**
	 * @return the next texture to be displayed after the update
	 */
    public Texture getNextTextureFrame() {
        return getTextureFrame(currentFrame);
    }

    /**
	 * This method should be called if you wish to restart the animation.
	 */
    public void restartAnimation() {
        frameTime = 0;
        currentFrame = 0;
    }

    private static class Rotation2d {

        private int degrees;

        private int x;

        private int y;

        private int a00, a01;

        private int a10, a11;

        public Rotation2d(int degrees) {
            if (degrees >= 360) degrees = degrees % 360; else if (degrees < 0) degrees = 360 - (degrees % 360);
            if (degrees % 90 != 0) {
                throw new IllegalArgumentException("Only the multiples of 90 degrees are allowed.");
            }
            this.degrees = degrees;
            switch(degrees) {
                case 0:
                    a00 = 1;
                    a01 = 0;
                    a10 = 0;
                    a11 = 1;
                    break;
                case 90:
                    a00 = 0;
                    a01 = -1;
                    a10 = 1;
                    a11 = 0;
                    break;
                case 180:
                    a00 = -1;
                    a01 = 0;
                    a10 = 0;
                    a11 = -1;
                    break;
                case 270:
                    a00 = 0;
                    a01 = 1;
                    a10 = -1;
                    a11 = 0;
                    break;
            }
        }

        public int getDegrees() {
            return degrees;
        }

        public void setVector(int x, int y) {
            this.x = x;
            this.y = y;
        }

        /**
		 * Will rotate the given vector around the origin.
		 */
        public void rotate() {
            int newx = a00 * x + a01 * y;
            int newy = a10 * x + a11 * y;
            x = newx;
            y = newy;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    /**
	 * Only the multiples of 90 degrees are supported.
	 * 
	 * Rotation of 0 degrees will return back the same instance. If there
	 * is already a rotated version of this cursor in the cache, the cached
	 * cursor is returned. You can have up to 4 rotated versions of the
	 * same cursor in the cache (0, 90, 180, 270).
	 * 
	 * @param degrees degrees for counterclockwise rotation
	 * @return a new cursor with hotspotOffset, images and textures rotated
	 */
    public Cursor getRotatedCursor(String name, int degrees) {
        if (parent != null) {
            return parent.getRotatedCursor(name, this.rotation + degrees);
        }
        try {
            Rotation2d rotation = new Rotation2d(degrees);
            URL newCursorUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "/" + rotation.getDegrees());
            Cursor cached = cursorCache.get(newCursorUrl);
            if (cached != null) return cached;
            Cursor cursor = new Cursor();
            cursor.url = newCursorUrl;
            cursor.currentFrame = 0;
            cursor.frameTime = 0;
            if (delays != null) {
                cursor.delays = new int[delays.length];
                for (int i = 0; i < delays.length; i++) {
                    cursor.delays[i] = delays[i];
                }
            }
            rotation.setVector(width, height);
            rotation.rotate();
            cursor.width = rotation.getX();
            cursor.height = rotation.getY();
            int xOffset = 0;
            if (cursor.width < 0) {
                cursor.width = -cursor.width;
                xOffset = cursor.width - 1;
            }
            int yOffset = 0;
            if (cursor.height < 0) {
                cursor.height = -cursor.height;
                yOffset = cursor.height - 1;
            }
            rotation.setVector((int) hotSpotOffset.x, (int) hotSpotOffset.y);
            rotation.rotate();
            cursor.hotSpotOffset.x = rotation.getX() + xOffset;
            cursor.hotSpotOffset.y = rotation.getY() + yOffset;
            cursor.images = new Image[images.length];
            cursor.textures = new Texture[images.length];
            TextureLoader textureLoader = new TextureLoader();
            URL tUrl = textures[0].getTextureKey().getLocation();
            UrlFactory urlFactory = new UrlFactory(tUrl.getProtocol(), tUrl.getHost(), tUrl.getPort(), tUrl.getFile().substring(0, tUrl.getFile().lastIndexOf('/') + 1) + rotation.getDegrees() + "/");
            Map<String, Texture> localCache = new HashMap<String, Texture>();
            for (Entry<String, Texture> entry : textureCache.entrySet()) {
                String fileName = entry.getKey();
                URL textureUrl = urlFactory.makeUrl(fileName);
                Texture texture = textureLoader.getImageFromCache(textureUrl);
                if (texture == null) {
                    Image image = entry.getValue().getImage();
                    IntBuffer data = image.getData(0).asIntBuffer();
                    ByteBuffer newImageData = ByteBuffer.allocateDirect(image.getData(0).capacity()).order(ByteOrder.nativeOrder());
                    IntBuffer rotatedData = newImageData.asIntBuffer();
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            rotation.setVector(x, y);
                            rotation.rotate();
                            int newx = rotation.getX() + xOffset;
                            int newy = rotation.getY() + yOffset;
                            rotatedData.put(newy * cursor.width + newx, data.get(y * width + x));
                        }
                    }
                    Image newImage = new Image(image.getFormat(), width, height, newImageData);
                    texture = textureLoader.loadTexture(textureUrl, newImage);
                }
                localCache.put(fileName, texture);
            }
            for (int i = 0; i < frameFileNames.length; i++) {
                cursor.textures[i] = localCache.get(frameFileNames[i]);
                cursor.images[i] = cursor.textures[i].getImage();
            }
            cursor.frameFileNames = null;
            cursor.textureCache = null;
            cursor.parent = this;
            cursor.rotation = rotation.getDegrees();
            cursorCache.put(newCursorUrl, cursor);
            return cursor;
        } catch (MalformedURLException mue) {
            log.log(Level.WARNING, "Unable to flip cursor.", mue);
        } catch (IllegalArgumentException iae) {
            log.log(Level.WARNING, "Unable to flip cursor.", iae);
        }
        return null;
    }
}
