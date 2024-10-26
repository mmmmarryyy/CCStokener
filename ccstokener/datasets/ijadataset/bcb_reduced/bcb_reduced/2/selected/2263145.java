package data;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class SemaPropertyFiles {

    public void load() throws IOException {
        Properties props = new Properties();
        URL url = ClassLoader.getSystemResource("myprops.props");
        props.load(url.openStream());
    }
}
