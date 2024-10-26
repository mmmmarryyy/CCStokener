package trb.fps.bmfont;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import javax.vecmath.Color4f;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import trb.jsg.RenderPass;
import trb.jsg.SceneGraph;
import trb.jsg.Shape;
import trb.jsg.Texture;
import trb.jsg.Unit;
import trb.jsg.VertexData;
import trb.jsg.View;
import trb.jsg.enums.BlendDstFunc;
import trb.jsg.enums.BlendSrcFunc;
import trb.jsg.enums.Format;
import trb.jsg.enums.TextureType;
import trb.jsg.renderer.Renderer;
import trb.jsg.util.TextureLoader;

/** Renders bitmap fonts. The font consists of 2 files: an image file or {@link TextureRegion} containing the glyphs and a file in
 * the AngleCode BMFont text format that describes where each glyph is on the image. Currently only a single image of glyphs is
 * supported.<br>
 * <br>
 * Text is drawn using a {@link SpriteBatch}. Text can be cached in a {@link BitmapFontCache} for faster rendering of static text,
 * which saves needing to compute the location of each glyph each frame.<br>
 * <br>
 * * The texture for a BitmapFont loaded from a file is managed. {@link #dispose()} must be called to free the texture when no
 * longer needed. A BitmapFont loaded using a {@link TextureRegion} is managed if the region's texture is managed. Disposing the
 * BitmapFont disposes the region's texture, which may not be desirable if the texture is still being used elsewhere.<br>
 * <br>
 * The code is based on Matthias Mann's TWL BitmapFont class. Thanks for sharing, Matthias! :)
 * @author Nathan Sweet
 * @author Matthias Mann */
public class BitmapFont {

    private final TextBounds textBounds = new TextBounds();

    private boolean integer = true;

    final BitmapFontData data;

    private final FloatList coords = new FloatList();

    private final FloatList texCoords = new FloatList();

    private final IntList indices = new IntList();

    /** Constructs a new BitmapFont from the given {@link BitmapFontData} and
	 * {@link TextureRegion}. If the TextureRegion is null, the image path is
	 * read from the BitmapFontData. The dispose() method will not dispose the
	 * texture of the region if the region is != null.
	 */
    public BitmapFont(BitmapFontData data, boolean integer) {
        this.integer = integer;
        this.data = data;
    }

    public VertexData createVertexData() {
        return new VertexData(coords.get(), null, null, 2, new float[][] { texCoords.get() }, indices.get());
    }

    /** Draws a string at the specified position.
     * @param x The x position for the left most character.
     * @param y The y position for the top of most capital letters in the font (the {@link #getCapHeight() cap height}).
     * @return The bounds of the rendered string (the height is the distance from y to the baseline). Note the same TextBounds
     *         instance is used for all methods that return TextBounds. */
    public TextBounds draw(CharSequence str, float x, float y) {
        return draw(str, x, y, 0, str.length());
    }

    /** Draws a substring at the specified position.
     * @param x The x position for the left most character.
     * @param y The y position for the top of most capital letters in the font (the {@link #getCapHeight() cap height}).
     * @param start The first character of the string to draw.
     * @param end The last character of the string to draw (exclusive).
     * @return The bounds of the rendered string (the height is the distance from y to the baseline). Note the same TextBounds
     *         instance is used for all methods that return TextBounds. */
    public TextBounds draw(CharSequence str, float x, float y, int start, int end) {
        y += data.ascent;
        float startX = x;
        Glyph lastGlyph = null;
        if (integer) {
            y = (int) y;
            x = (int) x;
        }
        while (start < end) {
            lastGlyph = data.getGlyph(str.charAt(start++));
            if (lastGlyph != null) {
                drawGlyph(lastGlyph, x, y);
                x += lastGlyph.xadvance;
                break;
            }
        }
        while (start < end) {
            char ch = str.charAt(start++);
            Glyph g = data.getGlyph(ch);
            if (g == null) {
                continue;
            }
            x += lastGlyph.getKerning(ch);
            if (integer) {
                x = (int) x;
            }
            lastGlyph = g;
            drawGlyph(lastGlyph, x, y);
            x += g.xadvance;
        }
        textBounds.width = x - startX;
        textBounds.height = data.capHeight;
        return textBounds;
    }

    void drawGlyph(Glyph lastGlyph, float x, float y) {
        x += lastGlyph.xoffset;
        y += lastGlyph.yoffset;
        float w = lastGlyph.width;
        float h = lastGlyph.height;
        float u = lastGlyph.u;
        float v = lastGlyph.v;
        float u2 = lastGlyph.u2;
        float v2 = lastGlyph.v2;
        System.out.println(u + " " + v + " " + u2 + " " + v2);
        int off = coords.size() / 3;
        coords.add(x, y, 0, x + w, y, 0, x + w, y + h, 0, x, y + h, 0);
        texCoords.add(u, v, u2, v, u2, v2, u, v2);
        indices.add(off + 0, off + 1, off + 2, off + 2, off + 3, off + 0);
    }

    /** Draws a string, which may contain newlines (\n), at the specified position.
     * @param x The x position for the left most character.
     * @param y The y position for the top of most capital letters in the font (the {@link #getCapHeight() cap height}).
     * @return The bounds of the rendered string (the height is the distance from y to the baseline of the last line). Note the
     *         same TextBounds instance is used for all methods that return TextBounds. */
    public TextBounds drawMultiLine(CharSequence str, float x, float y) {
        return drawMultiLine(str, x, y, 0, HAlignment.LEFT);
    }

    /** Draws a string, which may contain newlines (\n), at the specified position and alignment. Each line is aligned horizontally
     * within a rectangle of the specified width.
     * @param x The x position for the left most character.
     * @param y The y position for the top of most capital letters in the font (the {@link #getCapHeight() cap height}).
     * @return The bounds of the rendered string (the height is the distance from y to the baseline of the last line). Note the
     *         same TextBounds instance is used for all methods that return TextBounds. */
    public TextBounds drawMultiLine(CharSequence str, float x, float y, float alignmentWidth, HAlignment alignment) {
        float down = this.data.down;
        int start = 0;
        int numLines = 0;
        int length = str.length();
        float maxWidth = 0;
        while (start < length) {
            int lineEnd = indexOf(str, '\n', start);
            float xOffset = 0;
            if (alignment != HAlignment.LEFT) {
                float lineWidth = getBounds(str, start, lineEnd).width;
                xOffset = alignmentWidth - lineWidth;
                if (alignment == HAlignment.CENTER) {
                    xOffset = xOffset / 2;
                }
            }
            float lineWidth = draw(str, x + xOffset, y, start, lineEnd).width;
            maxWidth = Math.max(maxWidth, lineWidth);
            start = lineEnd + 1;
            y += down;
            numLines++;
        }
        textBounds.width = maxWidth;
        textBounds.height = data.capHeight + (numLines - 1) * data.lineHeight;
        return textBounds;
    }

    /** Draws a string, which may contain newlines (\n), with the specified position. Each line is automatically wrapped to keep it
     * within a rectangle of the specified width.
     * @param x The x position for the left most character.
     * @param y The y position for the top of most capital letters in the font (the {@link #getCapHeight() cap height}).
     * @return The bounds of the rendered string (the height is the distance from y to the baseline of the last line). Note the
     *         same TextBounds instance is used for all methods that return TextBounds. */
    public TextBounds drawWrapped(CharSequence str, float x, float y, float wrapWidth) {
        return drawWrapped(str, x, y, wrapWidth, HAlignment.LEFT);
    }

    /** Draws a string, which may contain newlines (\n), with the specified position. Each line is automatically wrapped to keep it
     * within a rectangle of the specified width, and aligned horizontally within that rectangle.
     * @param x The x position for the left most character.
     * @param y The y position for the top of most capital letters in the font (the {@link #getCapHeight() cap height}).
     * @return The bounds of the rendered string (the height is the distance from y to the baseline of the last line). Note the
     *         same TextBounds instance is used for all methods that return TextBounds. */
    public TextBounds drawWrapped(CharSequence str, float x, float y, float wrapWidth, HAlignment alignment) {
        if (wrapWidth <= 0) {
            wrapWidth = Integer.MAX_VALUE;
        }
        float down = this.data.down;
        int start = 0;
        int numLines = 0;
        int length = str.length();
        float maxWidth = 0;
        while (start < length) {
            int newLine = BitmapFont.indexOf(str, '\n', start);
            while (start < newLine) {
                if (!BitmapFont.isWhitespace(str.charAt(start))) {
                    break;
                }
                start++;
            }
            int lineEnd = start + computeVisibleGlyphs(str, start, newLine, wrapWidth);
            int nextStart = lineEnd + 1;
            if (lineEnd < newLine) {
                while (lineEnd > start) {
                    if (BitmapFont.isWhitespace(str.charAt(lineEnd))) {
                        break;
                    }
                    lineEnd--;
                }
                if (lineEnd == start) {
                    lineEnd = nextStart;
                } else {
                    nextStart = lineEnd;
                    while (lineEnd > start) {
                        if (!BitmapFont.isWhitespace(str.charAt(lineEnd - 1))) {
                            break;
                        }
                        lineEnd--;
                    }
                }
            } else {
                nextStart = lineEnd + 1;
            }
            if (lineEnd > start) {
                float xOffset = 0;
                if (alignment != HAlignment.LEFT) {
                    float lineWidth = getBounds(str, start, lineEnd).width;
                    xOffset = wrapWidth - lineWidth;
                    if (alignment == HAlignment.CENTER) {
                        xOffset /= 2;
                    }
                }
                float lineWidth = draw(str, x + xOffset, y, start, lineEnd).width;
                maxWidth = Math.max(maxWidth, lineWidth);
            }
            start = nextStart;
            y += down;
            numLines++;
        }
        textBounds.width = maxWidth;
        textBounds.height = data.capHeight + (numLines - 1) * data.lineHeight;
        return textBounds;
    }

    /** Returns the size of the specified string. The height is the distance from the top of most capital letters in the font (the
     * {@link #getCapHeight() cap height}) to the baseline. Note the same TextBounds instance is used for all methods that return
     * TextBounds. */
    public TextBounds getBounds(CharSequence str) {
        return getBounds(str, 0, str.length());
    }

    /** Returns the size of the specified substring. The height is the distance from the top of most capital letters in the font
     * (the {@link #getCapHeight() cap height}) to the baseline. Note the same TextBounds instance is used for all methods that
     * return TextBounds.
     * @param start The first character of the string.
     * @param end The last character of the string (exclusive). */
    public TextBounds getBounds(CharSequence str, int start, int end) {
        int width = 0;
        Glyph lastGlyph = null;
        while (start < end) {
            lastGlyph = data.getGlyph(str.charAt(start++));
            if (lastGlyph != null) {
                width = lastGlyph.xadvance;
                break;
            }
        }
        while (start < end) {
            char ch = str.charAt(start++);
            Glyph g = data.getGlyph(ch);
            if (g != null) {
                width += lastGlyph.getKerning(ch);
                lastGlyph = g;
                width += g.xadvance;
            }
        }
        textBounds.width = width;
        textBounds.height = data.capHeight;
        return textBounds;
    }

    /** Returns the size of the specified string, which may contain newlines. The height is the distance from the top of most
     * capital letters in the font (the {@link #getCapHeight() cap height}) to the baseline of the last line of text. Note the same
     * TextBounds instance is used for all methods that return TextBounds. */
    public TextBounds getMultiLineBounds(CharSequence str) {
        int start = 0;
        float maxWidth = 0;
        int numLines = 0;
        int length = str.length();
        while (start < length) {
            int lineEnd = indexOf(str, '\n', start);
            float lineWidth = getBounds(str, start, lineEnd).width;
            maxWidth = Math.max(maxWidth, lineWidth);
            start = lineEnd + 1;
            numLines++;
        }
        textBounds.width = maxWidth;
        textBounds.height = data.capHeight + (numLines - 1) * data.lineHeight;
        return textBounds;
    }

    /** Returns the size of the specified string, which may contain newlines and is wrapped to keep it within a rectangle of the
     * specified width. The height is the distance from the top of most capital letters in the font (the {@link #getCapHeight() cap
     * height}) to the baseline of the last line of text. Note the same TextBounds instance is used for all methods that return
     * TextBounds. */
    public TextBounds getWrappedBounds(CharSequence str, float wrapWidth) {
        if (wrapWidth <= 0) {
            wrapWidth = Integer.MAX_VALUE;
        }
        float down = this.data.down;
        int start = 0;
        int numLines = 0;
        int length = str.length();
        float maxWidth = 0;
        while (start < length) {
            int newLine = BitmapFont.indexOf(str, '\n', start);
            while (start < newLine) {
                if (!BitmapFont.isWhitespace(str.charAt(start))) {
                    break;
                }
                start++;
            }
            int lineEnd = start + computeVisibleGlyphs(str, start, newLine, wrapWidth);
            int nextStart = lineEnd + 1;
            if (lineEnd < newLine) {
                while (lineEnd > start) {
                    if (BitmapFont.isWhitespace(str.charAt(lineEnd))) {
                        break;
                    }
                    lineEnd--;
                }
                if (lineEnd == start) {
                    lineEnd = nextStart;
                } else {
                    nextStart = lineEnd;
                    while (lineEnd > start) {
                        if (!BitmapFont.isWhitespace(str.charAt(lineEnd - 1))) {
                            break;
                        }
                        lineEnd--;
                    }
                }
            }
            if (lineEnd > start) {
                float lineWidth = getBounds(str, start, lineEnd).width;
                maxWidth = Math.max(maxWidth, lineWidth);
            }
            start = nextStart;
            numLines++;
        }
        textBounds.width = maxWidth;
        textBounds.height = data.capHeight + (numLines - 1) * data.lineHeight;
        return textBounds;
    }

    /** Returns the number of glyphs from the substring that can be rendered in the specified width.
     * @param start The first character of the string.
     * @param end The last character of the string (exclusive). */
    public int computeVisibleGlyphs(CharSequence str, int start, int end, float availableWidth) {
        int index = start;
        int width = 0;
        Glyph lastGlyph = null;
        if (data.scaleX == 1) {
            for (; index < end; index++) {
                char ch = str.charAt(index);
                Glyph g = data.getGlyph(ch);
                if (g != null) {
                    if (lastGlyph != null) {
                        width += lastGlyph.getKerning(ch);
                    }
                    if (width + g.xadvance > availableWidth) {
                        break;
                    }
                    width += g.xadvance;
                    lastGlyph = g;
                }
            }
        } else {
            float scaleX = this.data.scaleX;
            for (; index < end; index++) {
                char ch = str.charAt(index);
                Glyph g = data.getGlyph(ch);
                if (g != null) {
                    if (lastGlyph != null) {
                        width += lastGlyph.getKerning(ch) * scaleX;
                    }
                    if (width + g.xadvance * scaleX > availableWidth) {
                        break;
                    }
                    width += g.xadvance * scaleX;
                    lastGlyph = g;
                }
            }
        }
        return index - start;
    }

    /** Makes the specified glyphs fixed width. This can be useful to make the numbers in a font fixed width. Eg, when horizontally
     * centering a score or loading percentage text, it will not jump around as different numbers are shown. */
    public void setFixedWidthGlyphs(CharSequence glyphs) {
        int maxAdvance = 0;
        for (int index = 0, end = glyphs.length(); index < end; index++) {
            Glyph g = data.getGlyph(glyphs.charAt(index));
            if (g != null && g.xadvance > maxAdvance) {
                maxAdvance = g.xadvance;
            }
        }
        for (int index = 0, end = glyphs.length(); index < end; index++) {
            Glyph g = data.getGlyph(glyphs.charAt(index));
            if (g == null) {
                continue;
            }
            g.xoffset += (maxAdvance - g.xadvance) / 2;
            g.xadvance = maxAdvance;
            g.kerning = null;
        }
    }

    /** @param character
     * @return whether the given character is contained in this font. */
    public boolean containsCharacter(char character) {
        return data.getGlyph(character) != null;
    }

    /** Specifies whether to use integer positions or not. Default is to use them so filtering doesn't kick in as badly.
     * @param use */
    public void setUseIntegerPositions(boolean use) {
        this.integer = use;
    }

    /** @return whether this font uses integer positions for drawing. */
    public boolean usesIntegerPositions() {
        return integer;
    }

    public BitmapFontData getData() {
        return data;
    }

    static int indexOf(CharSequence text, char ch, int start) {
        final int n = text.length();
        for (; start < n; start++) {
            if (text.charAt(start) == ch) {
                return start;
            }
        }
        return n;
    }

    static boolean isWhitespace(char c) {
        switch(c) {
            case '\n':
            case '\r':
            case '\t':
            case ' ':
                return true;
            default:
                return false;
        }
    }

    public static class TextBounds {

        public float width;

        public float height;

        public TextBounds() {
        }

        public TextBounds(TextBounds bounds) {
            set(bounds);
        }

        public void set(TextBounds bounds) {
            width = bounds.width;
            height = bounds.height;
        }
    }

    public static enum HAlignment {

        LEFT, CENTER, RIGHT
    }

    public static void main(String[] args) throws Exception {
        BufferedImage image = ImageIO.read(BitmapFont.class.getResource("Candara-38-Bold.png"));
        URL url = BitmapFontData.class.getResource("Candara-38-Bold.fnt");
        BitmapFontData bitmapFontData = new BitmapFontData(url.openStream(), true);
        BitmapFont font = new BitmapFont(bitmapFontData, true);
        font.drawMultiLine("Hello world\nthis is a\ntest!!!", 100, 100);
        VertexData vertexData = font.createVertexData();
        Display.setDisplayMode(new DisplayMode(640, 480));
        Display.create();
        RenderPass renderPass = new RenderPass();
        renderPass.setClearMask(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        renderPass.setClearColor(new Color4f(0.3f, 0.4f, 0.5f, 1f));
        renderPass.setView(View.createOrtho(0, 640, 0, 480, -1000, 1000));
        ByteBuffer[][] pixels = { { TextureLoader.getImageData(image) } };
        Texture texture = new Texture(TextureType.TEXTURE_2D, 4, image.getWidth(), image.getHeight(), 0, Format.BGRA, pixels, false, false);
        Shape shape = new Shape(vertexData);
        shape.getState().setUnit(0, new Unit(texture));
        shape.getState().setBlendEnabled(true);
        shape.getState().setBlendSrcFunc(BlendSrcFunc.SRC_ALPHA);
        shape.getState().setBlendDstFunc(BlendDstFunc.ONE_MINUS_SRC_ALPHA);
        renderPass.getRootNode().addShape(shape);
        Renderer renderer = new Renderer(new SceneGraph(renderPass));
        while (!Display.isCloseRequested()) {
            renderer.render();
            Display.update();
        }
        Display.destroy();
    }
}
