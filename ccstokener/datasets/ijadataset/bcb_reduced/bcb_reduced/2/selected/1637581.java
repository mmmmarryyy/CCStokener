package com.iv.flash.gif;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Vector;

public class GifImage implements GifIO {

    public GifImage() {
    }

    public void read(URL url) throws IOException, GifException {
        InputStream fin = url.openStream();
        read(fin);
    }

    public void read(InputStream fin) throws IOException, GifException {
        d_data_blocks.clear();
        if (!fin.markSupported()) fin = new BufferedInputStream(fin);
        d_header.read(fin);
        d_logical_screen.read(fin);
        readData(fin);
        if (d_data_blocks.size() == 0) throw new GifException("No data blocks read.");
        readTrailer(fin);
    }

    public void write(OutputStream outs) throws IOException, GifException {
        throw new GifException("Not implemented Yet");
    }

    public GifTableBasedImage getFirstImage() throws GifException {
        for (int i = 0; i < d_data_blocks.size(); ++i) {
            if (d_data_blocks.get(i) instanceof GifTableBasedImage) return (GifTableBasedImage) d_data_blocks.get(i);
        }
        throw new GifException("Error, no table based image block.");
    }

    public GifTableBasedImage[] getAllImages() throws GifException {
        int length = 0;
        GifTableBasedImage[] blocks = new GifTableBasedImage[d_data_blocks.size()];
        for (int i = 0; i < d_data_blocks.size(); ++i) {
            if (d_data_blocks.get(i) instanceof GifTableBasedImage) {
                blocks[length] = (GifTableBasedImage) d_data_blocks.get(i);
                ++length;
            }
        }
        if (length == 0) {
            throw new GifException("Error, no table based image blocks.");
        }
        if (length < blocks.length) {
            GifTableBasedImage[] tmp = new GifTableBasedImage[length];
            System.arraycopy(blocks, 0, tmp, 0, length);
            blocks = tmp;
        }
        return blocks;
    }

    public int getBlockCount() {
        return d_data_blocks.size();
    }

    public GifBlock getBlock(int index) {
        return (GifBlock) d_data_blocks.get(index);
    }

    public GifPalette getPaletteFor(GifTableBasedImage image) {
        if (image.hasPalette()) return image.getPalette();
        if (d_logical_screen.hasPalette()) return d_logical_screen.getPalette();
        return GifPalette.getDefault();
    }

    void readBlock(InputStream ins, GifBlock block) throws IOException, GifException {
        block.read(ins);
        d_data_blocks.add(block);
    }

    void readData(InputStream ins) throws IOException, GifException {
        while (true) {
            switch(Gif.peek(ins)) {
                case 0x00:
                    ins.read();
                    break;
                case Gif.IMAGE_DESCRIPTOR:
                    GifTableBasedImage image = new GifTableBasedImage();
                    image.controlExtension(d_graphic_control_extension);
                    readBlock(ins, image);
                    d_graphic_control_extension = null;
                    break;
                case Gif.EXTENSION_INTRODUCTION:
                    ins.read();
                    break;
                case Gif.GRAPHIC_CONTROL_EXTENSION:
                    d_graphic_control_extension = new GifGraphicControlExtension();
                    d_graphic_control_extension.read(ins);
                    break;
                case Gif.COMMENT_EXTENSION:
                    readBlock(ins, new GifCommentExtension());
                    break;
                case Gif.PLAIN_TEXT_EXTENSION:
                    readBlock(ins, new GifPlainTextExtension());
                    break;
                case Gif.APPLICATION_EXTENSION:
                    readBlock(ins, new GifApplicationExtension());
                    break;
                case Gif.TRAILER:
                    return;
            }
        }
    }

    boolean readTrailer(InputStream ins) throws IOException {
        return Gif.unsignedByte(ins) == Gif.TRAILER;
    }

    public GifLogicalScreen getLogicalScreen() {
        return d_logical_screen;
    }

    GifHeader d_header = new GifHeader();

    GifLogicalScreen d_logical_screen = new GifLogicalScreen();

    GifGraphicControlExtension d_graphic_control_extension = null;

    private Vector d_data_blocks = new Vector();

    private byte[] d_buf = new byte[1024];
}

;
