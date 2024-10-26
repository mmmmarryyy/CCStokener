package org.opennms.core.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

public class URLReader extends Reader {

    URL m_url = null;

    InputStream m_inputStream = null;

    InputStreamReader m_inputStreamReader = null;

    public URLReader(URL url) throws IOException {
        m_url = url;
        init();
    }

    private void init() throws IOException {
        m_inputStream = m_url.openStream();
        m_inputStreamReader = new InputStreamReader(m_inputStream);
    }

    @Override
    public void close() throws IOException {
        m_inputStreamReader.close();
        m_inputStream.close();
    }

    public String getEncoding() {
        return m_inputStreamReader.getEncoding();
    }

    @Override
    public int read() throws IOException {
        return m_inputStreamReader.read();
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return m_inputStreamReader.read(cbuf, off, len);
    }

    @Override
    public boolean ready() throws IOException {
        return m_inputStreamReader.ready();
    }

    /**
	 * Close the stream and re-initialize.
	 */
    @Override
    public void reset() throws IOException {
        this.close();
        this.init();
    }
}
