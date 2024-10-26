package test.it.hotel.model.sms;

import java.io.IOException;
import java.io.InputStream;
import junit.framework.TestCase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.RequestContent;
import org.apache.http.util.EntityUtils;
import test.it.hotel.model.abstrakt.BaseHotelTestCase;

public class TestSmsManager extends TestCase {

    public void testSendMessage() throws ClientProtocolException, IOException {
        String textMessage = "La%20sua%20prenotazione%20e60%20andata%20a%20buon%20fine";
        String customerPhoneNumber = "+393345730726";
        DefaultHttpClient httpclient = new DefaultHttpClient();
        String other = "http://smswizard.globalitalia.it/smsgateway/send.asp";
        String urlProva = other + "?" + "Account=sardricerche" + "&Password=v8LomdZT" + "&PhoneNumbers=+393345730726" + "&SMSData=" + textMessage + "&Recipients=1" + "&Sender=+393337589951" + "&ID=11762";
        HttpPost httpPost = new HttpPost(urlProva);
        HttpResponse response = httpclient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        String txt = EntityUtils.toString(entity);
    }

    public void testHttpPost() {
    }
}
