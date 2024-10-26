package heisig.input;

import heisig.data.KanjiFrame;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Map;

public class DownloadStrokeOrderImages {

    private File rootDir;

    public static void main(String[] args) {
        KanjiFileReader reader = new KanjiFileReader();
        Map<Integer, KanjiFrame> kanjiMap = reader.read();
        DownloadStrokeOrderImages dsoi = new DownloadStrokeOrderImages(new File("strokes"));
        for (int m = 1; m <= kanjiMap.size(); m++) {
            KanjiFrame frame = kanjiMap.get(m);
            try {
                byte[] bytes = frame.getKanji().getBytes("UTF-16");
                int[] ints = new int[bytes.length];
                String s = "";
                for (int i = 2; i < bytes.length; i++) {
                    ints[i] = bytes[i] < 0 ? 256 + bytes[i] : bytes[i];
                    String hs = Integer.toHexString(ints[i]);
                    if (hs.length() == 1) {
                        hs = "0" + hs;
                    }
                    s += hs;
                }
                dsoi.download(frame.getHeisigNumber(), frame.getKeyword(), s);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    public DownloadStrokeOrderImages(File rootDir) {
        if (!rootDir.exists()) {
            rootDir.mkdir();
        }
        this.rootDir = rootDir;
    }

    public void download(int heisigNumber, String keyword, String utf16string) {
        String urlString = "http://www.bibiko.de/kanji/sod/" + utf16string + ".jpg";
        try {
            URL url = new URL(urlString);
            File imgFile = new File(rootDir.getAbsolutePath() + File.separator + String.valueOf(heisigNumber) + ".jpg");
            streamDownload(url, imgFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean streamDownload(URL url, File file) {
        try {
            InputStream in = url.openConnection().getInputStream();
            BufferedInputStream bis = new BufferedInputStream(in);
            OutputStream out = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(out);
            int chunkSize = 63 * 1024;
            byte[] ba = new byte[chunkSize];
            while (true) {
                int bytesRead = readBlocking(bis, ba, 0, chunkSize);
                if (bytesRead > 0) {
                    if (bos != null) bos.write(ba, 0, bytesRead);
                } else {
                    bos.close();
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error writing file " + file);
            return false;
        }
        System.out.println("OK writing file " + file);
        return true;
    }

    private int readBlocking(InputStream in, byte b[], int off, int len) throws IOException {
        int totalBytesRead = 0;
        while (totalBytesRead < len) {
            int bytesRead = in.read(b, off + totalBytesRead, len - totalBytesRead);
            if (bytesRead < 0) {
                break;
            }
            totalBytesRead += bytesRead;
        }
        return totalBytesRead;
    }
}
