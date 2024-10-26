package org.fultest.test.qa.loader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.net.URL;

/**
 * @version $Id: $
 */
public class TextFileReader {

    private static final Log LOG = LogFactory.getLog(TextFileReader.class);

    private String separator = System.getProperty("line.separator");

    public String getContents(String fileUri) throws IOException {
        StringBuffer contents = new StringBuffer();
        if (fileUri != null && !fileUri.equals("")) {
            BufferedReader input = null;
            try {
                LOG.info("Reading:" + fileUri);
                URL url = getClass().getClassLoader().getResource(fileUri);
                if (url != null) {
                    InputStream stream = url.openStream();
                    input = new BufferedReader(new InputStreamReader(stream));
                    appendInputToContents(input, contents);
                } else {
                    LOG.error("Unable to locate file:" + fileUri + " in directory " + new File(".").getAbsolutePath());
                }
            } finally {
                if (input != null) {
                    input.close();
                }
            }
        }
        return contents.toString();
    }

    private void appendInputToContents(BufferedReader input, StringBuffer contents) throws IOException {
        String line;
        while ((line = input.readLine()) != null) {
            contents.append(line);
            if (input.ready()) {
                contents.append(separator);
            }
        }
        LOG.debug("read:" + contents.toString());
    }
}
