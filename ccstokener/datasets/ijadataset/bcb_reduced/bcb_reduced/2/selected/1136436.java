package loengud.minu;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class Mingiklass {

    public static void main(String[] args) throws Exception {
        URL url = new URL("http://ipinfodb.com/ip_query.php?ip=74.125.77.99");
        URLConnection connection = url.openConnection();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println(br.readLine());
    }
}
