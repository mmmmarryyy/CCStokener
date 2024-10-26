package j2se.typestate.accesspath;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class URLExample1 {

    public static void main(String[] args) throws IOException {
        try {
            URL url = new URL("http://haven.watson.ibm.com:8080");
            URLConnection conn = url.openConnection();
            for (int i = 0; ; i++) {
                String headerName = conn.getHeaderFieldKey(i);
                String headerValue = conn.getHeaderField(i);
                System.out.println(headerName);
                System.out.println(headerValue);
                if (headerName == null && headerValue == null) {
                    break;
                }
                if (headerName == null) {
                }
            }
        } catch (Exception e) {
        }
    }
}
