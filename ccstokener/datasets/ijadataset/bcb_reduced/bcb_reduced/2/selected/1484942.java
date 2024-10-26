package com.jogamp.opengl.util.texture.spi.awt;

import java.awt.Graphics;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import javax.imageio.*;
import javax.media.opengl.GLProfile;
import jogamp.opengl.Debug;
import com.jogamp.opengl.util.texture.*;
import com.jogamp.opengl.util.texture.awt.*;
import com.jogamp.opengl.util.texture.spi.*;

public class IIOTextureProvider implements TextureProvider {

    private static final boolean DEBUG = Debug.debug("TextureIO");

    public TextureData newTextureData(GLProfile glp, File file, int internalFormat, int pixelFormat, boolean mipmap, String fileSuffix) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            return null;
        }
        if (DEBUG) {
            System.out.println("TextureIO.newTextureData(): BufferedImage type for " + file + " = " + img.getType());
        }
        return new AWTTextureData(glp, internalFormat, pixelFormat, mipmap, img);
    }

    public TextureData newTextureData(GLProfile glp, InputStream stream, int internalFormat, int pixelFormat, boolean mipmap, String fileSuffix) throws IOException {
        BufferedImage img = ImageIO.read(stream);
        if (img == null) {
            return null;
        }
        if (DEBUG) {
            System.out.println("TextureIO.newTextureData(): BufferedImage type for stream = " + img.getType());
        }
        return new AWTTextureData(glp, internalFormat, pixelFormat, mipmap, img);
    }

    public TextureData newTextureData(GLProfile glp, URL url, int internalFormat, int pixelFormat, boolean mipmap, String fileSuffix) throws IOException {
        InputStream stream = url.openStream();
        try {
            return newTextureData(glp, stream, internalFormat, pixelFormat, mipmap, fileSuffix);
        } finally {
            stream.close();
        }
    }
}
