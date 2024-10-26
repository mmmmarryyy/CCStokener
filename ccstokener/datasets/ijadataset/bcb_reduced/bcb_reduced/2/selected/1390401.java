package org.gocha.inetools.http;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Тестирование http
 * @author gocha
 */
public class Test {

    public static void main(String[] args) {
        new Test().testPost();
    }

    public void testGet() {
        try {
            HttpRequest request = new HttpGetRequest("http://localhost/test/printer.php?do=1");
            try {
                HttpResponse response = request.execute();
                System.out.println("response:" + response.getResponseCode() + " " + response.getResponseMessage());
                System.out.println("==========");
                System.out.println("" + response.getResponseDataText());
            } catch (IOException ex) {
                Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void testPost() {
        try {
            WWWFormUrlEncoded data = new WWWFormUrlEncoded();
            data.add("a", "abc");
            data.add("b", "bcd");
            HttpRequest request = new HttpPostRequest("http://localhost/test/printer.php?do=1", data);
            request.getHeader().setUserAgent("Mozilla/5.0 (X11; U; Linux i686; ru; rv:1.9.2.3) Gecko/20100423 Ubuntu/10.04 (lucid) Firefox/3.6.3");
            try {
                HttpResponse response = request.execute();
                System.out.println("response:" + response.getResponseCode() + " " + response.getResponseMessage());
                System.out.println("==========");
                System.out.println("" + response.getResponseDataText());
            } catch (IOException ex) {
                Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
