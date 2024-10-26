package loengud.kaug.esimene;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class Peaklass {

    public static void main(String[] args) throws IOException {
        v2ljastaVeebileht("http://ipinfodb.com/ip_query.php?ip=74.125.77.99");
    }

    public static void v2ljastaVeebileht(String s) throws IOException {
        URL url = new URL(s);
        InputStream is = url.openConnection().getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
    }

    public static InputStream k2ivitaTrace(String serverMidaOtsida) throws IOException {
        Process p = Runtime.getRuntime().exec("tracert -d " + serverMidaOtsida);
        return p.getInputStream();
    }

    public static void tqqtleV2ljundit(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while (true) {
            line = br.readLine();
            if (line != null) {
                String ip = otsiIpaadress(line);
                if (ip != null) System.out.println(ip);
            } else break;
        }
    }

    public static String otsiIpaadress(String line) {
        int pos = line.lastIndexOf("ms");
        if (pos == -1) return null; else return line.substring(pos + 4);
    }
}
