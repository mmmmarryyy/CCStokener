package spacewars.misc.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class Leitor {

    private ByteBuffer bytebuffer;

    public Leitor(URL url) {
        try {
            URLConnection c = url.openConnection();
            bytebuffer = ByteBuffer.allocate((int) c.getContentLength());
            InputStream in = new BufferedInputStream(c.getInputStream());
            ReadableByteChannel fc = Channels.newChannel(in);
            fc.read(bytebuffer);
            bytebuffer.position(0);
            fc.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Leitor(ByteBuffer in) {
        bytebuffer = in;
    }

    private int read() {
        int valor = bytebuffer.get();
        if (valor < 0) valor = valor + 256;
        return valor;
    }

    public byte[] leBytes(int qtos) {
        byte[] r = new byte[qtos];
        for (int i = 0; i < qtos; i++) r[i] = (byte) read();
        return r;
    }

    public void skip(long qto) {
        bytebuffer.position(bytebuffer.position() + (int) qto);
    }

    public boolean readBoolean8() {
        return (read() != 0);
    }

    public int readUnsignedInt8() {
        int valor = read();
        if (valor >= 0) return valor; else return valor + 256;
    }

    public int readInt8() {
        return (read());
    }

    public int readInt16() {
        return (read() << 0) + (read() << 8);
    }

    public long readInt32() {
        return ((long) read() << 0) + ((long) read() << 8) + ((long) read() << 16) + ((long) read() << 24);
    }

    public String readASCIIZ() {
        String s = "";
        int chr = read();
        while (chr != 0) {
            s = s + (char) chr;
            chr = read();
        }
        return s;
    }

    public float readFloat32() {
        return Float.intBitsToFloat((int) readInt32());
    }

    public boolean Acabou() {
        return bytebuffer.position() >= bytebuffer.capacity();
    }

    public int position() {
        return bytebuffer.position();
    }

    public void seek(int posicao) {
        bytebuffer.position(posicao);
    }
}
