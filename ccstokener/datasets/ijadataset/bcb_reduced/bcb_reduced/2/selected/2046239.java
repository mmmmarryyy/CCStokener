package arcadeflex;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

public class UrlDownload {

    public static File fileUrl(String fAddress) {
        File out = null;
        OutputStream outStream = null;
        URLConnection uCon = null;
        InputStream is = null;
        try {
            URL Url;
            byte[] buf;
            int ByteRead, ByteWritten = 0;
            Url = new URL(fAddress);
            File dd = new File(System.getProperty("java.io.tmpdir") + "tmp");
            FileOutputStream asd = new FileOutputStream(dd);
            outStream = new BufferedOutputStream(asd);
            uCon = Url.openConnection();
            is = uCon.getInputStream();
            buf = new byte[1024];
            while ((ByteRead = is.read(buf)) != -1) {
                outStream.write(buf, 0, ByteRead);
                ByteWritten += ByteRead;
            }
            out = dd;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
                outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return out;
    }

    public static ByteBuffer getAsByteArray(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        InputStream in = connection.getInputStream();
        int contentLength = connection.getContentLength();
        ByteArrayOutputStream tmpOut;
        if (contentLength != -1) {
            tmpOut = new ByteArrayOutputStream(contentLength);
        } else {
            tmpOut = new ByteArrayOutputStream(16384);
        }
        byte[] buf = new byte[512];
        while (true) {
            int len = in.read(buf);
            if (len == -1) {
                break;
            }
            tmpOut.write(buf, 0, len);
        }
        in.close();
        tmpOut.close();
        byte[] array = tmpOut.toByteArray();
        return ByteBuffer.wrap(array);
    }
}
