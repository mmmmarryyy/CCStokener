package gnujatella.utils;

import java.io.*;
import java.net.*;
import gnujatella.event.*;

public class IOUtil {

    private IOUtil() {
    }

    public static int serializeIntLE(int value, byte[] outbuf, int offset) {
        outbuf[offset++] = (byte) (value);
        outbuf[offset++] = (byte) (value >> 8);
        outbuf[offset++] = (byte) (value >> 16);
        outbuf[offset++] = (byte) (value >> 24);
        return offset;
    }

    public static int deserializeIntLE(byte[] inbuf, int offset) {
        return (inbuf[offset + 3]) << 24 | (inbuf[offset + 2] & 0xff) << 16 | (inbuf[offset + 1] & 0xff) << 8 | (inbuf[offset] & 0xff);
    }

    public static int serializeShortLE(short value, byte[] outbuf, int offset) {
        outbuf[offset++] = (byte) (value);
        outbuf[offset++] = (byte) (value >> 8);
        return offset;
    }

    public static short deserializeShortLE(byte[] inbuf, int offset) {
        return (short) ((inbuf[offset + 1] & 0xff) << 8 | (inbuf[offset] & 0xff));
    }

    public static int serializeString(String str, byte[] outbuf, int offset) {
        for (int i = 0; i < str.length(); i++) {
            outbuf[offset] = (byte) str.charAt(i);
            offset++;
        }
        return offset;
    }

    public static int deserializeString(byte[] inbuf, int offset, StringBuffer outbuf) {
        int begin = offset;
        int maxLen = inbuf.length;
        while (offset < maxLen) {
            if (inbuf[offset] == 0) {
                break;
            }
            offset++;
        }
        if (offset - begin > 0) outbuf.append(new String(inbuf, begin, offset - begin));
        return offset;
    }

    public static int deserializeString(byte[] inbuf, int offset, int len, StringBuffer outbuf) {
        if (len > inbuf.length - offset) len = inbuf.length - offset;
        outbuf.append(new String(inbuf, offset, len));
        return offset + len;
    }

    public static int serializeIP(String ip, byte[] outbuf, int offset) {
        InetAddress inet = null;
        byte[] addrBuf = null;
        try {
            inet = InetAddress.getByName(ip);
            addrBuf = inet.getAddress();
        } catch (Exception e) {
            addrBuf = new byte[4];
            addrBuf[0] = (byte) '\0';
            addrBuf[1] = (byte) '\0';
            addrBuf[2] = (byte) '\0';
            addrBuf[3] = (byte) '\0';
        }
        outbuf[offset++] = addrBuf[0];
        outbuf[offset++] = addrBuf[1];
        outbuf[offset++] = addrBuf[2];
        outbuf[offset++] = addrBuf[3];
        return offset;
    }

    public static int deserializeIP(byte[] inbuf, int offset, StringBuffer outbuf) {
        int digit1 = inbuf[offset];
        int digit2 = inbuf[offset + 1];
        int digit3 = inbuf[offset + 2];
        int digit4 = inbuf[offset + 3];
        if (digit1 < 0) digit1 += 256;
        if (digit2 < 0) digit2 += 256;
        if (digit3 < 0) digit3 += 256;
        if (digit4 < 0) digit4 += 256;
        outbuf.append(digit1).append(".").append(digit2).append(".").append(digit3).append(".").append(digit4);
        return offset + 4;
    }

    public static int readToCRLF(InputStream is, byte[] buf, int bufLen, int offset) throws Exception {
        boolean isNearEnd = false;
        while (bufLen > 0) {
            int ch = is.read();
            if (ch == 0 || ch == -1) {
                throw new IOException("Connection closed by remote host.");
            }
            buf[offset++] = (byte) ch;
            bufLen--;
            if (isNearEnd) {
                if (ch == (int) '\n') {
                    return offset - 2;
                } else {
                    isNearEnd = false;
                }
            }
            if (ch == (int) '\n' || ch == (int) '\r') {
                isNearEnd = true;
            }
        }
        throw new Exception("Out of buffer");
    }

    public static int readToLF(InputStream is, byte[] buf, int bufLen, int offset) throws Exception {
        while (bufLen > 0) {
            int ch = is.read();
            if (ch == 0 || ch == -1) {
                throw new IOException("Connection closed by remote host.");
            }
            buf[offset++] = (byte) ch;
            bufLen--;
            if (ch == (int) '\n') {
                return offset - 1;
            }
        }
        throw new Exception("Out of buffer");
    }

    public static void downloadFile(URL url, File file, ProgressListener listener) throws IOException {
        int length = url.openConnection().getContentLength();
        int totalRead = 0;
        InputStream in = url.openStream();
        file.createNewFile();
        FileOutputStream out = new FileOutputStream(file);
        int read;
        byte[] buffer = new byte[4096];
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            totalRead += read;
            if (listener != null) listener.progressed(new ProgressEvent(listener, ((float) totalRead) / ((float) length)));
        }
        out.close();
        in.close();
    }

    public static void readUntil(LineNumberReader reader, String mark) {
        try {
            while (!reader.readLine().trim().equals(mark.trim())) ;
        } catch (IOException e) {
        }
    }

    public static boolean compareFiles(File file1, File file2) {
        if ((!file1.isFile()) || (!file2.isFile())) return false;
        if (file1.length() != file2.length()) return false;
        FileInputStream in1;
        FileInputStream in2;
        try {
            in1 = new FileInputStream(file1);
            in2 = new FileInputStream(file2);
        } catch (FileNotFoundException e) {
            return false;
        }
        try {
            int tmp;
            while ((tmp = in1.read()) != -1) if (tmp != in2.read()) return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static void deltree(File tree) throws IOException {
        if (tree.isDirectory()) {
            File[] files = tree.listFiles();
            for (int i = 0; i < files.length; i++) deltree(files[i]);
        }
        tree.delete();
    }
}
