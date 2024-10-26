package bones.doc.file;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;

public class HttpClient {

    public static class Worker implements Runnable {

        URL url;

        public Worker(URL purl) {
            url = purl;
        }

        public void run() {
            try {
                for (int i = 0; i < 100; i++) {
                    URLConnection con = url.openConnection();
                    con.connect();
                    System.out.println(i + " " + url);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String readStream(InputStream in) {
        StringBuffer result = new StringBuffer();
        BufferedReader tmp = new BufferedReader(new InputStreamReader(in));
        char[] target = new char[65536];
        int nb = 0;
        try {
            while ((nb = tmp.read(target)) > 0) {
                result.append(target, 0, nb);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public static String saveStream(InputStream in, String file) {
        try {
            FileChannel fc = (new FileOutputStream(file)).getChannel();
        } catch (FileNotFoundException e1) {
            return null;
        }
        BufferedReader tmp = new BufferedReader(new InputStreamReader(in));
        char[] target = new char[65536];
        int nb = 0;
        try {
            while ((nb = tmp.read(target)) > 0) {
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void display(URL url) {
        URLConnection con;
        try {
            con = url.openConnection();
            con.connect();
            String s = readStream(con.getInputStream());
            System.out.println(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] argv) {
        URL[] urls = new URL[100];
        try {
            urls[0] = new URL("http://www.google.fr");
            urls[0] = new URL("http://www.renault-trucks.fr");
            urls[1] = new URL("http://www.renault-trucks.com.tr");
            urls[2] = new URL("http://www.renault-trucks.co.uk");
            urls[3] = new URL("http://www.renault-trucks.de");
            urls[4] = new URL("http://www.renault-trucks.it");
            urls[5] = new URL("http://www.renault-trucks.es");
            urls[6] = new URL("http://www.renault-trucks.pt");
            urls[7] = new URL("http://www.renault-trucks.nl");
            urls[8] = new URL("http://www.renault-trucks.ru");
            urls[9] = new URL("http://www.renault-trucks.ch");
            urls[10] = new URL("http://www.renault-trucks.be/J46PAYS/web/front/affich.jsp?codeWebSite=be&lang=fr");
            urls[11] = new URL("http://j46navig.renault-trucks.com/J46NAVIG/web/controller?command=InitContexte&codePays=322&rubrique=1_1");
            urls[12] = new URL("http://j46navig.renault-trucks.com/J46NAVIG/web/controller?command=InitContexte&codePays=322&rubrique=2_1");
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        for (int i = 0; i < 13; i++) (new Thread(new Worker(urls[i]))).start();
    }
}
