import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 *
 * @author Shorteh
 */
public class POSTTest {

    private String data = null;

    public POSTTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void HTTPPostTest() {
        try {
            data = URLEncoder.encode("gebruikersNaam", "UTF-8") + "=" + URLEncoder.encode("Koen", "UTF-8") + "&" + URLEncoder.encode("wachtwoord", "UTF-8") + "=" + URLEncoder.encode("abc123", "UTF-8");
            URL url = new URL("http://127.0.0.1/formverwerking.php");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            wr.close();
            assertEquals("gebruikersNaam=Koen&wachtwoord=abc123", data);
            assertEquals("gebruikersNaam=Koen&wachtwoord=abc123".length(), data.length());
        } catch (Exception e) {
            fail("The POST data string wasn't put together as expected.");
        }
    }
}
