package test.net.jmatrix.eproperties.emptyvalue;

import java.io.InputStreamReader;
import java.net.URL;
import net.jmatrix.eproperties.EProperties;
import org.junit.*;

public class TestEmptyValue {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testEmptyValue() throws Exception {
        System.out.println("Test Empty Value...");
        EProperties props = new EProperties();
        URL url = this.getClass().getResource("emptyval.properties");
        System.out.println("Properties URL " + url);
        System.out.println("******************  LOADING URL  *************************");
        props.load(url);
        System.out.println("---list---");
        System.out.println(props.list());
        System.out.println("---list---");
        System.out.println("******************  LOADING Reader  *************************");
        EProperties p2 = new EProperties();
        p2.load(new InputStreamReader(url.openStream()));
        System.out.println("---list---");
        System.out.println(p2.list());
        System.out.println("---list---");
    }
}
