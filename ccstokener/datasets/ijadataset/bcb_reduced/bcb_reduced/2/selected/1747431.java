package se.vgregion.incidentreport.pivotaltracker;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author Patrik Bergström
 */
public class PivotalTrackerIT {

    @Test
    public void testHttpsConnection() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            String uri = "https://www.pivotaltracker.com";
            HttpGet get = new HttpGet(uri);
            HttpResponse execute = client.execute(get);
            byteArrayOutputStream = new ByteArrayOutputStream();
            execute.getEntity().writeTo(byteArrayOutputStream);
        } finally {
            byteArrayOutputStream.close();
        }
    }
}
