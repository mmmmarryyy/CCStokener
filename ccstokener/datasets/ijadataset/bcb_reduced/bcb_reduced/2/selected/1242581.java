package org.maestroframework.maestro.widgets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import org.maestroframework.markup.Component;
import org.maestroframework.utils.StreamUtils;
import org.maestroframework.utils.StringUtils;

public class HTTPReaderComponent extends Component {

    private String urlString;

    public HTTPReaderComponent(String url) {
        super("");
        this.urlString = url;
    }

    @Override
    public void write(Writer writer, int depth) throws IOException {
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            InputStream in = conn.getInputStream();
            BufferedReader reader = new BufferedReader(StreamUtils.inputStreamToReader(in));
            String line = null;
            while ((line = reader.readLine()) != null) {
                StringUtils.encodeHTMLChars(line, writer);
                writer.write("\n");
            }
            reader.close();
            in.close();
        } catch (Exception e) {
            writer.write("couldn't include the href: " + e);
        }
    }
}
